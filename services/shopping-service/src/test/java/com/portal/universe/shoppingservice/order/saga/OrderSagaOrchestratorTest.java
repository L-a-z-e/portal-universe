package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private OrderSagaOrchestrator sagaOrchestrator;

    private Order createOrderWithItems(Long id, String orderNumber, String userId, OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(null)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "totalAmount", BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(order, "finalAmount", BigDecimal.valueOf(20000));

        List<OrderItem> items = new ArrayList<>();
        OrderItem item = OrderItem.builder()
                .productId(1L)
                .productName("Product A")
                .price(BigDecimal.valueOf(10000))
                .quantity(2)
                .build();
        ReflectionTestUtils.setField(item, "id", 1L);
        ReflectionTestUtils.setField(item, "subtotal", BigDecimal.valueOf(20000));
        items.add(item);
        ReflectionTestUtils.setField(order, "items", items);

        return order;
    }

    private SagaState createSagaState(Long id, Long orderId, String orderNumber,
                                       SagaStep currentStep, SagaStatus status, String completedSteps) {
        SagaState sagaState = SagaState.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .build();
        ReflectionTestUtils.setField(sagaState, "id", id);
        ReflectionTestUtils.setField(sagaState, "currentStep", currentStep);
        ReflectionTestUtils.setField(sagaState, "status", status);
        ReflectionTestUtils.setField(sagaState, "completedSteps", completedSteps);
        ReflectionTestUtils.setField(sagaState, "compensationAttempts", 0);
        return sagaState;
    }

    @Nested
    @DisplayName("startSaga")
    class StartSaga {

        @Test
        @DisplayName("should_startSaga_when_inventoryReserved")
        void should_startSaga_when_inventoryReserved() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);

            SagaState initialSaga = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.RESERVE_INVENTORY, SagaStatus.STARTED, "");
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(initialSaga);

            when(inventoryService.reserveStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1")))
                    .thenReturn(List.of());

            // when
            SagaState result = sagaOrchestrator.startSaga(order);

            // then
            assertThat(result).isNotNull();
            verify(inventoryService).reserveStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1"));
            verify(sagaStateRepository, atLeast(2)).save(any(SagaState.class));
        }

        @Test
        @DisplayName("should_compensateAndThrow_when_inventoryReserveFails")
        void should_compensateAndThrow_when_inventoryReserveFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);

            SagaState initialSaga = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.RESERVE_INVENTORY, SagaStatus.STARTED, "");
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(initialSaga);

            when(inventoryService.reserveStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1")))
                    .thenThrow(new CustomBusinessException(
                            com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode.INSUFFICIENT_STOCK));

            when(orderRepository.findByOrderNumberWithItems("ORD-001"))
                    .thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.startSaga(order))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("completeSagaAfterPayment")
    class CompleteSagaAfterPayment {

        @Test
        @DisplayName("should_completeSaga_when_allStepsSucceed")
        void should_completeSaga_when_allStepsSucceed() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.CONFIRMED);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.PROCESS_PAYMENT, SagaStatus.STARTED, "RESERVE_INVENTORY");

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(inventoryService.deductStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1")))
                    .thenReturn(List.of());
            when(deliveryService.createDelivery(order))
                    .thenReturn(mock(DeliveryResponse.class));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);

            // when
            sagaOrchestrator.completeSagaAfterPayment("ORD-001");

            // then
            verify(inventoryService).deductStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1"));
            verify(deliveryService).createDelivery(order);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should_throwException_when_sagaNotFound")
        void should_throwException_when_sagaNotFound() {
            // given
            when(sagaStateRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.completeSagaAfterPayment("ORD-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_compensateAndThrow_when_deductInventoryFails")
        void should_compensateAndThrow_when_deductInventoryFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.CONFIRMED);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.PROCESS_PAYMENT, SagaStatus.STARTED, "RESERVE_INVENTORY");

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(inventoryService.deductStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1")))
                    .thenThrow(new RuntimeException("Deduct failed"));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.completeSagaAfterPayment("ORD-001"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_compensateAndThrow_when_deliveryCreationFails")
        void should_compensateAndThrow_when_deliveryCreationFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.CONFIRMED);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.PROCESS_PAYMENT, SagaStatus.STARTED, "RESERVE_INVENTORY");

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(inventoryService.deductStockBatch(anyMap(), eq("ORDER"), eq("ORD-001"), eq("user1")))
                    .thenReturn(List.of());
            when(deliveryService.createDelivery(order)).thenThrow(new RuntimeException("Delivery creation failed"));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.completeSagaAfterPayment("ORD-001"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("compensate")
    class Compensate {

        @Test
        @DisplayName("should_releaseInventory_when_inventoryReserved")
        void should_releaseInventory_when_inventoryReserved() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.RESERVE_INVENTORY, SagaStatus.STARTED, "RESERVE_INVENTORY");

            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);
            when(inventoryService.releaseStockBatch(anyMap(), eq("ORDER_CANCEL"), eq("ORD-001"), eq("SYSTEM")))
                    .thenReturn(List.of());
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            sagaOrchestrator.compensate(sagaState, "Test error");

            // then
            verify(inventoryService).releaseStockBatch(anyMap(), eq("ORDER_CANCEL"), eq("ORD-001"), eq("SYSTEM"));
            verify(sagaStateRepository, atLeast(2)).save(any(SagaState.class));
        }

        @Test
        @DisplayName("should_cancelDelivery_when_deliveryCreated")
        void should_cancelDelivery_when_deliveryCreated() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.CONFIRMED);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.CONFIRM_ORDER, SagaStatus.STARTED,
                    "RESERVE_INVENTORY,DEDUCT_INVENTORY,CREATE_DELIVERY");

            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);
            when(inventoryService.releaseStockBatch(anyMap(), eq("ORDER_CANCEL"), eq("ORD-001"), eq("SYSTEM")))
                    .thenReturn(List.of());
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            sagaOrchestrator.compensate(sagaState, "Test error");

            // then
            verify(deliveryService).cancelDelivery(1L);
            verify(inventoryService).releaseStockBatch(anyMap(), eq("ORDER_CANCEL"), eq("ORD-001"), eq("SYSTEM"));
        }

        @Test
        @DisplayName("should_markAsFailed_when_orderNotFound")
        void should_markAsFailed_when_orderNotFound() {
            // given
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.RESERVE_INVENTORY, SagaStatus.STARTED, "");

            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.empty());
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);

            // when
            sagaOrchestrator.compensate(sagaState, "Test error");

            // then
            verify(sagaStateRepository, atLeast(2)).save(any(SagaState.class));
        }

        @Test
        @DisplayName("should_markCompensationFailed_when_maxAttemptsReached")
        void should_markCompensationFailed_when_maxAttemptsReached() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);
            SagaState sagaState = createSagaState(1L, 1L, "ORD-001",
                    SagaStep.RESERVE_INVENTORY, SagaStatus.STARTED, "RESERVE_INVENTORY");
            ReflectionTestUtils.setField(sagaState, "compensationAttempts", 2);

            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);
            when(inventoryService.releaseStockBatch(anyMap(), eq("ORDER_CANCEL"), eq("ORD-001"), eq("SYSTEM")))
                    .thenThrow(new RuntimeException("Release failed"));

            // when
            sagaOrchestrator.compensate(sagaState, "Test error");

            // then
            verify(sagaStateRepository, atLeast(2)).save(any(SagaState.class));
        }
    }
}
