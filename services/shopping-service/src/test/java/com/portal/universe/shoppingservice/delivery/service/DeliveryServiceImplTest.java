package com.portal.universe.shoppingservice.delivery.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.repository.DeliveryRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.support.fixture.DeliveryFixture;
import com.portal.universe.shoppingservice.support.fixture.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Nested
    @DisplayName("createDelivery")
    class CreateDelivery {

        @Test
        @DisplayName("should_createDelivery_when_valid")
        void should_createDelivery_when_valid() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.PAID).build();
            when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.empty());
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));

            // when
            DeliveryResponse result = deliveryService.getDeliveryByTrackingNumber("TRK-ABC123", "user-1");

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
            assertThatThrownBy(() -> deliveryService.getDeliveryByTrackingNumber("TRK-999", "user-1"))
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
            when(deliveryRepository.findByOrderNumberWithHistories("ORD-001"))
                    .thenReturn(Optional.of(delivery));

            // when
            DeliveryResponse result = deliveryService.getDeliveryByOrderNumber("ORD-001", "user-1");

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
            assertThatThrownBy(() -> deliveryService.getDeliveryByOrderNumber("ORD-999", "user-1"))
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(
                    DeliveryStatus.SHIPPED, "Warehouse", "Package shipped"
            );

            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.PAID).build();
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
            when(deliveryRepository.findByTrackingNumberWithHistories("TRK-ABC123"))
                    .thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(
                    DeliveryStatus.SHIPPED, "Warehouse", "Package shipped"
            );

            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.PAID).build();
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.PREPARING).build();
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
            Delivery delivery = DeliveryFixture.builder()
                    .id(1L).trackingNumber("TRK-ABC123").orderNumber("ORD-001")
                    .userId("user-1").carrier("Test Carrier").status(DeliveryStatus.SHIPPED).build();
            when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

            // when
            deliveryService.cancelDelivery(1L);

            // then
            verify(deliveryRepository, never()).save(any(Delivery.class));
        }
    }
}
