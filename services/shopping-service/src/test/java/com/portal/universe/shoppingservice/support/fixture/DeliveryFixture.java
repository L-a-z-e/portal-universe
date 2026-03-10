package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import org.springframework.test.util.ReflectionTestUtils;

public final class DeliveryFixture {

    private DeliveryFixture() {}

    public static Delivery create() {
        return builder().build();
    }

    public static DeliveryBuilder builder() {
        return new DeliveryBuilder();
    }

    public static class DeliveryBuilder {
        private Long id = 1L;
        private Long orderId = 1L;
        private String orderNumber = "ORD-20260310-001";
        private String userId = "test-user-001";
        private DeliveryStatus status = DeliveryStatus.PREPARING;
        private String carrier = "CJ대한통운";
        private String trackingNumber;

        public DeliveryBuilder id(Long id) { this.id = id; return this; }
        public DeliveryBuilder orderId(Long orderId) { this.orderId = orderId; return this; }
        public DeliveryBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public DeliveryBuilder userId(String userId) { this.userId = userId; return this; }
        public DeliveryBuilder status(DeliveryStatus status) { this.status = status; return this; }
        public DeliveryBuilder carrier(String carrier) { this.carrier = carrier; return this; }
        public DeliveryBuilder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }

        public Delivery build() {
            Delivery delivery = Delivery.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .carrier(carrier)
                    .build();
            ReflectionTestUtils.setField(delivery, "id", id);
            ReflectionTestUtils.setField(delivery, "status", status);
            if (trackingNumber != null) {
                ReflectionTestUtils.setField(delivery, "trackingNumber", trackingNumber);
            }
            return delivery;
        }
    }
}
