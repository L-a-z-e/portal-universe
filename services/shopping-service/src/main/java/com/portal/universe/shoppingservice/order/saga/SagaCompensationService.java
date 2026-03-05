package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.event.CloudWatchMetricsPublisher;
import com.portal.universe.shoppingservice.feign.PaymentIntentFeignClient;
import com.portal.universe.shoppingservice.feign.SellerInventoryClient;
import com.portal.universe.shoppingservice.feign.dto.StockReserveRequest;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Saga 보상 로직을 담당하는 서비스입니다.
 * OrderSagaOrchestrator에서 분리하여 self-invocation으로 인한
 * REQUIRES_NEW 무시 문제를 해결합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCompensationService {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final SellerInventoryClient sellerInventoryClient;
    private final DeliveryService deliveryService;
    private final PaymentIntentFeignClient paymentIntentFeignClient;
    private final CloudWatchMetricsPublisher cloudWatchMetricsPublisher;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * 완료된 Saga 단계들을 역순으로 보상합니다.
     * 순수 보상 로직만 포함 — 주문/Saga 상태 변경은 호출자가 관리합니다.
     */
    public void compensateSagaSteps(Order order, SagaState sagaState) {
        String sagaId = sagaState.getSagaId();
        String orderNumber = order.getOrderNumber();

        boolean deductCompleted = sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY);
        Map<Long, Integer> quantities = buildQuantityMap(order);
        StockReserveRequest stockRequest = new StockReserveRequest(orderNumber, quantities);

        // 역순 보상: DELIVERY → DEDUCT → PAYMENT → RESERVE
        if (sagaState.isStepCompleted(SagaStep.CREATE_DELIVERY)) {
            deliveryService.cancelDelivery(order.getId());
            log.info("Saga {} - Delivery cancelled for order {}", sagaId, orderNumber);
        }

        if (deductCompleted) {
            sellerInventoryClient.restoreStock(stockRequest);
            log.info("Saga {} - Deducted inventory restored for order {}", sagaId, orderNumber);
        }

        if (sagaState.isStepCompleted(SagaStep.PROCESS_PAYMENT)) {
            paymentIntentFeignClient.refundForCompensation(orderNumber);
            log.info("Saga {} - Payment refunded via payment-service for order {}", sagaId, orderNumber);
        }

        // RESERVE 보상은 DEDUCT가 미완료일 때만 (DEDUCT 완료 시 reserved는 이미 0)
        if (!deductCompleted && sagaState.isStepCompleted(SagaStep.RESERVE_INVENTORY)) {
            sellerInventoryClient.releaseStock(stockRequest);
            log.info("Saga {} - Reserved inventory released for order {}", sagaId, orderNumber);
        }
    }

    /**
     * Saga 내부 실패 시 보상을 수행합니다.
     * compensateSagaSteps()로 단계 보상 + 주문 취소 + Saga 상태 관리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(SagaState sagaState, String errorMessage) {
        log.info("Starting compensation for saga {}: {}", sagaState.getSagaId(), errorMessage);

        sagaState.startCompensation(errorMessage);
        sagaStateRepository.save(sagaState);

        cloudWatchMetricsPublisher.publishCompensation(
                sagaState.getOrderNumber(), sagaState.getCompensationAttempts());

        Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
                .orElse(null);

        if (order == null) {
            sagaState.markAsFailed("Order not found during compensation");
            sagaStateRepository.save(sagaState);
            return;
        }

        try {
            compensateSagaSteps(order, sagaState);

            if (order.getStatus().isCancellable()) {
                order.cancel("Saga compensation: " + errorMessage);
                orderRepository.save(order);
            }

            sagaState.markAsFailed(errorMessage);
            sagaStateRepository.save(sagaState);

            log.info("Saga {} compensation completed", sagaState.getSagaId());

        } catch (Exception e) {
            log.error("Saga {} - Compensation failed: {}", sagaState.getSagaId(), e.getMessage());
            sagaState.incrementCompensationAttempts();

            if (sagaState.getCompensationAttempts() >= MAX_COMPENSATION_ATTEMPTS) {
                sagaState.markAsCompensationFailed(e.getMessage());
                log.error("Saga {} - Max compensation attempts reached, requires manual intervention",
                        sagaState.getSagaId());
            }

            sagaStateRepository.save(sagaState);
        }
    }

    Map<Long, Integer> buildQuantityMap(Order order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));
    }
}
