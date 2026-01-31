package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
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
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * Saga를 시작합니다 (주문 생성 시 호출).
     * 재고 예약 단계까지만 실행합니다.
     *
     * @param order 생성된 주문
     * @return Saga 상태
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
            // Step 1: Reserve Inventory
            executeReserveInventory(order, sagaState);
            sagaState.proceedToNextStep();
            sagaStateRepository.save(sagaState);

            log.info("Saga {} - Inventory reserved successfully for order: {}",
                    sagaState.getSagaId(), order.getOrderNumber());

            return sagaState;

        } catch (Exception e) {
            log.error("Saga {} - Failed at step {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());
            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * 결제 완료 후 나머지 Saga 단계를 실행합니다.
     *
     * @param orderNumber 주문 번호
     */
    @Transactional
    public void completeSagaAfterPayment(String orderNumber) {
        SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.SAGA_NOT_FOUND));

        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        log.info("Continuing saga {} after payment for order: {}", sagaState.getSagaId(), orderNumber);

        try {
            // Step 3: Deduct Inventory (결제 완료 후)
            executeDeductInventory(order, sagaState);
            sagaState.proceedToNextStep();

            // Step 4: Create Delivery
            executeCreateDelivery(order, sagaState);
            sagaState.proceedToNextStep();

            // Step 5: Confirm Order
            order.markAsPaid();
            orderRepository.save(order);

            // Saga 완료
            sagaState.complete();
            sagaStateRepository.save(sagaState);

            log.info("Saga {} completed successfully for order: {}", sagaState.getSagaId(), orderNumber);

        } catch (Exception e) {
            log.error("Saga {} - Failed after payment at step {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());
            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * Saga 보상(롤백)을 수행합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(SagaState sagaState, String errorMessage) {
        log.info("Starting compensation for saga {}: {}", sagaState.getSagaId(), errorMessage);

        sagaState.startCompensation(errorMessage);
        sagaStateRepository.save(sagaState);

        Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
                .orElse(null);

        if (order == null) {
            sagaState.markAsFailed("Order not found during compensation");
            sagaStateRepository.save(sagaState);
            return;
        }

        try {
            // 완료된 단계들을 역순으로 보상
            if (sagaState.isStepCompleted(SagaStep.CREATE_DELIVERY)) {
                deliveryService.cancelDelivery(order.getId());
                log.info("Saga {} - Delivery cancelled for order {}", sagaState.getSagaId(), order.getOrderNumber());
            }

            if (sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY)) {
                // 재고 차감 보상: 이미 차감된 재고는 복원 불가 (반품 처리 필요)
                log.warn("Saga {} - Deducted inventory cannot be auto-restored, requires manual intervention",
                        sagaState.getSagaId());
            }

            if (sagaState.isStepCompleted(SagaStep.RESERVE_INVENTORY)) {
                compensateReserveInventory(order, sagaState);
            }

            // 주문 취소
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

    /**
     * Step 1: 재고 예약 실행
     */
    private void executeReserveInventory(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: RESERVE_INVENTORY", sagaState.getSagaId());

        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.reserveStockBatch(
                quantities,
                "ORDER",
                order.getOrderNumber(),
                order.getUserId()
        );
    }

    /**
     * Step 3: 재고 차감 실행
     */
    private void executeDeductInventory(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: DEDUCT_INVENTORY", sagaState.getSagaId());

        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.deductStockBatch(
                quantities,
                "ORDER",
                order.getOrderNumber(),
                order.getUserId()
        );
    }

    /**
     * Step 4: 배송 생성 실행
     */
    private void executeCreateDelivery(Order order, SagaState sagaState) {
        log.debug("Saga {} - Executing step: CREATE_DELIVERY", sagaState.getSagaId());
        deliveryService.createDelivery(order);
    }

    /**
     * Step 1 보상: 재고 예약 해제
     */
    private void compensateReserveInventory(Order order, SagaState sagaState) {
        log.debug("Saga {} - Compensating step: RESERVE_INVENTORY", sagaState.getSagaId());

        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.releaseStockBatch(
                quantities,
                "ORDER_CANCEL",
                order.getOrderNumber(),
                "SYSTEM"
        );
    }
}
