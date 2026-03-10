package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.event.CloudWatchMetricsPublisher;
import com.portal.universe.shoppingservice.feign.SellerInventoryClient;
import com.portal.universe.shoppingservice.feign.dto.StockReserveRequest;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import com.portal.universe.shoppingservice.support.fixture.OrderFixture;
import com.portal.universe.shoppingservice.support.fixture.SagaStateFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private SellerInventoryClient sellerInventoryClient;

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private CloudWatchMetricsPublisher cloudWatchMetricsPublisher;

    @Mock
    private SagaCompensationService sagaCompensationService;

    @InjectMocks
    private OrderSagaOrchestrator sagaOrchestrator;

    private Order createOrderWithItems(Long id, String orderNumber, String userId, OrderStatus status) {
        Order order = OrderFixture.builder()
                .id(id).orderNumber(orderNumber).userId(userId).status(status)
                .totalAmount(BigDecimal.valueOf(20000)).finalAmount(BigDecimal.valueOf(20000))
                .build();
        OrderItem item = OrderFixture.itemBuilder()
                .order(order).sellerId(1L).productId(1L).productName("Product A")
                .price(BigDecimal.valueOf(10000)).quantity(2)
                .subtotal(BigDecimal.valueOf(20000)).build();
        order.getItems().add(item);
        return order;
    }

    @Nested
    @DisplayName("startSaga")
    class StartSaga {

        @Test
        @DisplayName("should_startSaga_when_inventoryReserved")
        void should_startSaga_when_inventoryReserved() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);

            SagaState initialSaga = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.RESERVE_INVENTORY).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("").build();
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(initialSaga);

            // when
            SagaState result = sagaOrchestrator.startSaga(order);

            // then
            assertThat(result).isNotNull();
            verify(sellerInventoryClient).reserveStock(any(StockReserveRequest.class));
            verify(sagaStateRepository, atLeast(2)).save(any(SagaState.class));
        }

        @Test
        @DisplayName("should_compensateAndThrow_when_inventoryReserveFails")
        void should_compensateAndThrow_when_inventoryReserveFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);

            SagaState initialSaga = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.RESERVE_INVENTORY).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("").build();
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(initialSaga);

            doThrow(new RuntimeException("Reserve failed"))
                    .when(sellerInventoryClient).reserveStock(any(StockReserveRequest.class));

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.startSaga(order))
                    .isInstanceOf(CustomBusinessException.class);

            verify(sagaCompensationService).compensate(any(SagaState.class), eq("Reserve failed"));
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
            SagaState sagaState = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.PROCESS_PAYMENT).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("RESERVE_INVENTORY").build();

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(deliveryService.createDelivery(order))
                    .thenReturn(mock(DeliveryResponse.class));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(sagaState);

            // when
            sagaOrchestrator.completeSagaAfterPayment("ORD-001");

            // then
            verify(sellerInventoryClient).deductStock(any(StockReserveRequest.class));
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
            SagaState sagaState = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.PROCESS_PAYMENT).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("RESERVE_INVENTORY").build();

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            doThrow(new RuntimeException("Deduct failed"))
                    .when(sellerInventoryClient).deductStock(any(StockReserveRequest.class));

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.completeSagaAfterPayment("ORD-001"))
                    .isInstanceOf(CustomBusinessException.class);

            verify(sagaCompensationService).compensate(any(SagaState.class), eq("Deduct failed"));
        }

        @Test
        @DisplayName("should_compensateAndThrow_when_deliveryCreationFails")
        void should_compensateAndThrow_when_deliveryCreationFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.CONFIRMED);
            SagaState sagaState = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.PROCESS_PAYMENT).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("RESERVE_INVENTORY").build();

            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(sagaState));
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(deliveryService.createDelivery(order)).thenThrow(new RuntimeException("Delivery creation failed"));

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.completeSagaAfterPayment("ORD-001"))
                    .isInstanceOf(CustomBusinessException.class);

            verify(sagaCompensationService).compensate(any(SagaState.class), eq("Delivery creation failed"));
        }
    }

    @Nested
    @DisplayName("compensate delegation")
    class CompensateDelegation {

        @Test
        @DisplayName("should_delegateToSagaCompensationService_when_sagaFails")
        void should_delegateToSagaCompensationService_when_sagaFails() {
            // given
            Order order = createOrderWithItems(1L, "ORD-001", "user1", OrderStatus.PENDING);

            SagaState initialSaga = SagaStateFixture.builder()
                    .id(1L).orderId(1L).orderNumber("ORD-001")
                    .currentStep(SagaStep.RESERVE_INVENTORY).status(SagaStatus.STARTED)
                    .completedStepsFromCsv("").build();
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(initialSaga);

            doThrow(new RuntimeException("Reserve failed"))
                    .when(sellerInventoryClient).reserveStock(any(StockReserveRequest.class));

            // when & then
            assertThatThrownBy(() -> sagaOrchestrator.startSaga(order))
                    .isInstanceOf(CustomBusinessException.class);

            verify(sagaCompensationService).compensate(any(SagaState.class), eq("Reserve failed"));
        }
    }
}
