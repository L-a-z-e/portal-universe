package com.portal.universe.shoppingservice.delivery.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.domain.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryTest {

    private Address createTestAddress() {
        return Address.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipCode("12345")
                .address1("서울시 강남구")
                .address2("101호")
                .build();
    }

    private Delivery createTestDelivery() {
        return Delivery.builder()
                .orderId(1L)
                .orderNumber("ORD-001")
                .shippingAddress(createTestAddress())
                .carrier("CJ대한통운")
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create delivery with PREPARING status")
        void should_create_delivery_with_preparing_status() {
            Delivery delivery = createTestDelivery();

            assertThat(delivery.getOrderId()).isEqualTo(1L);
            assertThat(delivery.getOrderNumber()).isEqualTo("ORD-001");
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
            assertThat(delivery.getCarrier()).isEqualTo("CJ대한통운");
            assertThat(delivery.getShippingAddress()).isNotNull();
            assertThat(delivery.getEstimatedDeliveryDate()).isEqualTo(LocalDate.now().plusDays(3));
            assertThat(delivery.getHistories()).isEmpty();
        }

        @Test
        @DisplayName("should generate tracking number starting with TRK-")
        void should_generate_tracking_number() {
            Delivery delivery = createTestDelivery();

            assertThat(delivery.getTrackingNumber()).isNotNull();
            assertThat(delivery.getTrackingNumber()).startsWith("TRK-");
            assertThat(delivery.getTrackingNumber()).hasSize(16); // "TRK-" + 12 chars
        }

        @Test
        @DisplayName("should set default carrier when null")
        void should_set_default_carrier_when_null() {
            Delivery delivery = Delivery.builder()
                    .orderId(1L)
                    .orderNumber("ORD-001")
                    .shippingAddress(createTestAddress())
                    .carrier(null)
                    .build();

            assertThat(delivery.getCarrier()).isEqualTo("기본택배");
        }
    }

    @Nested
    @DisplayName("ship")
    class ShipTest {

        @Test
        @DisplayName("should change status to SHIPPED from PREPARING")
        void should_change_status_to_shipped() {
            Delivery delivery = createTestDelivery();

            delivery.ship("물류센터", "출고 완료");

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
            assertThat(delivery.getHistories()).hasSize(1);
            assertThat(delivery.getHistories().get(0).getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
            assertThat(delivery.getHistories().get(0).getLocation()).isEqualTo("물류센터");
            assertThat(delivery.getHistories().get(0).getDescription()).isEqualTo("출고 완료");
        }
    }

    @Nested
    @DisplayName("transit")
    class TransitTest {

        @Test
        @DisplayName("should change status to IN_TRANSIT from SHIPPED")
        void should_change_status_to_in_transit() {
            Delivery delivery = createTestDelivery();
            delivery.ship("물류센터", "출고");

            delivery.transit("강남 허브", "배송 중");

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
            assertThat(delivery.getHistories()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deliver")
    class DeliverTest {

        @Test
        @DisplayName("should change status to DELIVERED and set actual delivery date")
        void should_change_status_to_delivered() {
            Delivery delivery = createTestDelivery();
            delivery.ship("물류센터", "출고");
            delivery.transit("강남 허브", "배송 중");

            delivery.deliver("수령인 주소", "배송 완료");

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(delivery.getActualDeliveryDate()).isEqualTo(LocalDate.now());
            assertThat(delivery.getHistories()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("should cancel delivery when status is PREPARING")
        void should_cancel_when_preparing() {
            Delivery delivery = createTestDelivery();

            delivery.cancel("고객 요청");

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
            assertThat(delivery.getHistories()).hasSize(1);
            assertThat(delivery.getHistories().get(0).getDescription()).isEqualTo("고객 요청");
        }

        @Test
        @DisplayName("should throw exception when cancelling non-cancellable delivery")
        void should_throw_when_not_cancellable() {
            Delivery delivery = createTestDelivery();
            delivery.ship("물류센터", "출고");

            assertThatThrownBy(() -> delivery.cancel("고객 요청"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTest {

        @Test
        @DisplayName("should add history when status changes")
        void should_add_history_on_status_change() {
            Delivery delivery = createTestDelivery();

            delivery.updateStatus(DeliveryStatus.SHIPPED, "물류센터", "출고 완료");

            assertThat(delivery.getHistories()).hasSize(1);
            DeliveryHistory history = delivery.getHistories().get(0);
            assertThat(history.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
            assertThat(history.getLocation()).isEqualTo("물류센터");
            assertThat(history.getDescription()).isEqualTo("출고 완료");
            assertThat(history.getDelivery()).isEqualTo(delivery);
        }

        @Test
        @DisplayName("should throw exception on invalid status transition")
        void should_throw_on_invalid_transition() {
            Delivery delivery = createTestDelivery();

            assertThatThrownBy(() ->
                    delivery.updateStatus(DeliveryStatus.DELIVERED, "주소", "배달"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should track multiple status transitions with history")
        void should_track_multiple_transitions() {
            Delivery delivery = createTestDelivery();

            delivery.ship("물류센터", "출고");
            delivery.transit("강남", "배송 중");
            delivery.deliver("수령인", "배송 완료");

            assertThat(delivery.getHistories()).hasSize(3);
            assertThat(delivery.getHistories().get(0).getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
            assertThat(delivery.getHistories().get(1).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
            assertThat(delivery.getHistories().get(2).getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        }
    }

    @Nested
    @DisplayName("trackingNumber uniqueness")
    class TrackingNumberTest {

        @Test
        @DisplayName("should generate unique tracking numbers for different deliveries")
        void should_generate_unique_tracking_numbers() {
            Delivery delivery1 = createTestDelivery();
            Delivery delivery2 = createTestDelivery();

            assertThat(delivery1.getTrackingNumber())
                    .isNotEqualTo(delivery2.getTrackingNumber());
        }
    }
}
