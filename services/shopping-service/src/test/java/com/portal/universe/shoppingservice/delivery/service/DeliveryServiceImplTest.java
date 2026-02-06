package com.portal.universe.shoppingservice.delivery.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.domain.Address;
import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.repository.DeliveryRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShoppingEventPublisher eventPublisher;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private Order createOrder(String userId, String orderNumber) {
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(null)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "totalAmount", BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(order, "finalAmount", BigDecimal.valueOf(10000));
        return order;
    }

    private Delivery createDelivery(Long id, String trackingNumber, String orderNumber, DeliveryStatus status) {
        Delivery delivery = Delivery.builder()
                .orderId(1L)
                .orderNumber(orderNumber)
                .shippingAddress(null)
                .carrier("Test Carrier")
                .build();
        ReflectionTestUtils.setField(delivery, "id", id);
        ReflectionTestUtils.setField(delivery, "trackingNumber", trackingNumber);
        ReflectionTestUtils.setField(delivery, "status", status);
        return delivery;
    }

    @Nested
    @DisplayName("createDelivery")
    class CreateDelivery {

        @Test
        @DisplayName("should_createDelivery_when_valid")
        void should_createDelivery_when_valid() {
            // given
            Order order = createOrder("user1", "ORD-001");
            when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.empty());
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            // when
            DeliveryResponse result = deliveryService.createDelivery(order);

            // then
            assertThat(result).isNotNull();
            verify(deliveryRepository).save(any(Delivery.class));
        }
    }

    @Nested
    @DisplayName("getDeliveryByTrackingNumber")
    class GetDeliveryByTrackingNumber {

        @Test
        @DisplayName("should_returnDelivery_when_found")
        void should_returnDelivery_when_found() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));

            // when
            DeliveryResponse result = deliveryService.getDeliveryByTrackingNumber("TRK-ABC123");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-999"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryByTrackingNumber("TRK-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getDeliveryByOrderNumber")
    class GetDeliveryByOrderNumber {

        @Test
        @DisplayName("should_returnDelivery_when_found")
        void should_returnDelivery_when_found() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.findByOrderNumberWithHistories("ORD-001"))
                    .thenReturn(Optional.of(delivery));

            // when
            DeliveryResponse result = deliveryService.getDeliveryByOrderNumber("ORD-001");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(deliveryRepository.findByOrderNumberWithHistories("ORD-999"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.getDeliveryByOrderNumber("ORD-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateDeliveryStatus")
    class UpdateDeliveryStatus {

        @Test
        @DisplayName("should_updateStatus_when_valid")
        void should_updateStatus_when_valid() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(
                    DeliveryStatus.SHIPPED, "Warehouse", "Package shipped"
            );

            Order order = createOrder("user1", "ORD-001");
            when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

            // when
            DeliveryResponse result = deliveryService.updateDeliveryStatus("TRK-ABC123", request);

            // then
            assertThat(result).isNotNull();
            verify(deliveryRepository).save(any(Delivery.class));
        }

        @Test
        @DisplayName("should_publishEvent_when_statusChangedToShipped")
        void should_publishEvent_when_statusChangedToShipped() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(
                    DeliveryStatus.SHIPPED, "Warehouse", "Package shipped"
            );

            Order order = createOrder("user1", "ORD-001");
            when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

            // when
            deliveryService.updateDeliveryStatus("TRK-ABC123", request);

            // then
            verify(eventPublisher).publishDeliveryShipped(any());
        }
    }

    @Nested
    @DisplayName("cancelDelivery")
    class CancelDelivery {

        @Test
        @DisplayName("should_cancelDelivery_when_preparing")
        void should_cancelDelivery_when_preparing() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.PREPARING);
            when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            // when
            deliveryService.cancelDelivery(1L);

            // then
            verify(deliveryRepository).save(any(Delivery.class));
        }

        @Test
        @DisplayName("should_notCancel_when_alreadyShipped")
        void should_notCancel_when_alreadyShipped() {
            // given
            Delivery delivery = createDelivery(1L, "TRK-ABC123", "ORD-001", DeliveryStatus.SHIPPED);
            when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

            // when
            deliveryService.cancelDelivery(1L);

            // then
            verify(deliveryRepository, never()).save(any(Delivery.class));
        }
    }
}
