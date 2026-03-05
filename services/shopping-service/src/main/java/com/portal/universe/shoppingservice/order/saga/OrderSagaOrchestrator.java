package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.event.CloudWatchMetricsPublisher;
import com.portal.universe.shoppingservice.feign.SellerInventoryClient;
import com.portal.universe.shoppingservice.feign.dto.StockReserveRequest;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
    private final CloudWatchMetricsPublisher cloudWatchMetricsPublisher;
    private final SagaCompensationService sagaCompensationService;

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

            sagaCompensationService.compensate(sagaState, e.getMessage());
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

            sagaCompensationService.compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
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
