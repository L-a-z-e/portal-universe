package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 생성을 위한 Saga Orchestrator입니다.
 *
 * Saga 실행 흐름:
 * 1. RESERVE_INVENTORY - 재고 예약
 * 2. PROCESS_PAYMENT - 결제 처리 (OrderService에서 별도 호출)
 * 3. DEDUCT_INVENTORY - 재고 차감
 * 4. CREATE_DELIVERY - 배송 생성
 * 5. CONFIRM_ORDER - 주문 확정
 *
 * 실패 시 보상(Compensation):
 * - 역순으로 완료된 단계들을 롤백
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final SellerInventoryClient sellerInventoryClient;
    private final DeliveryService deliveryService;
    private final PaymentIntentFeignClient paymentIntentFeignClient;
    private final CloudWatchMetricsPublisher cloudWatchMetricsPublisher;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * Saga를 시작합니다 (주문 생성 시 호출).
     * 재고 예약 단계까지만 실행합니다.
     */
    @Transactional
    public SagaState startSaga(Order order) {
        log.info("Starting saga for order: {}", order.getOrderNumber());

        SagaState sagaState = SagaState.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .build();

        sagaState = sagaStateRepository.save(sagaState);

        try {
            executeReserveInventory(order, sagaState);
            sagaState.proceedToNextStep();
            sagaStateRepository.save(sagaState);

            log.info("Saga {} - Inventory reserved successfully for order: {}",
                    sagaState.getSagaId(), order.getOrderNumber());

            return sagaState;

        } catch (Exception e) {
            log.error("Saga {} - Failed at step {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());

            cloudWatchMetricsPublisher.publishOrderFailure(order.getOrderNumber(),
                    sagaState.getCurrentStep().name());

            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * 결제 완료 후 나머지 Saga 단계를 실행합니다.
     */
    @Transactional
    public void completeSagaAfterPayment(String orderNumber) {
        SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.SAGA_NOT_FOUND));

        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        log.info("Continuing saga {} after payment for order: {}", sagaState.getSagaId(), orderNumber);

        try {
            executeDeductInventory(order, sagaState);
            sagaState.proceedToNextStep();

            executeCreateDelivery(order, sagaState);
            sagaState.proceedToNextStep();

            order.markAsPaid();
            orderRepository.save(order);

            sagaState.complete();
            sagaStateRepository.save(sagaState);

            cloudWatchMetricsPublisher.publishOrderSuccess(orderNumber, order.getTotalAmount());

            log.info("Saga {} completed successfully for order: {}", sagaState.getSagaId(), orderNumber);

        } catch (Exception e) {
            log.error("Saga {} - Failed after payment at step {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());

            cloudWatchMetricsPublisher.publishOrderFailure(orderNumber,
                    sagaState.getCurrentStep().name());

            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * 완료된 Saga 단계들을 역순으로 보상합니다.
     * 순수 보상 로직만 포함 — 주문/Saga 상태 변경은 호출자가 관리합니다.
     *
     * cancelOrder()와 compensate() 양쪽에서 재사용됩니다.
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

    private void executeReserveInventory(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: RESERVE_INVENTORY via Feign", sagaState.getSagaId());
        sellerInventoryClient.reserveStock(new StockReserveRequest(order.getOrderNumber(), buildQuantityMap(order)));
    }

    private void executeDeductInventory(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: DEDUCT_INVENTORY via Feign", sagaState.getSagaId());
        sellerInventoryClient.deductStock(new StockReserveRequest(order.getOrderNumber(), buildQuantityMap(order)));
    }

    private void executeCreateDelivery(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: CREATE_DELIVERY", sagaState.getSagaId());
        deliveryService.createDelivery(order);
    }

    private Map<Long, Integer> buildQuantityMap(Order order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));
    }
}
