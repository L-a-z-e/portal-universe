package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

public final class OrderFixture {

    public static final String DEFAULT_USER_ID = "test-user-001";
    public static final String DEFAULT_ORDER_NUMBER = "ORD-20260310-001";

    private OrderFixture() {}

    public static Order create() {
        return builder().build();
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private Long id = 1L;
        private String userId = DEFAULT_USER_ID;
        private String orderNumber = DEFAULT_ORDER_NUMBER;
        private OrderStatus status = OrderStatus.PENDING;
        private BigDecimal totalAmount = BigDecimal.valueOf(10000);
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal finalAmount = BigDecimal.valueOf(10000);

        public OrderBuilder id(Long id) { this.id = id; return this; }
        public OrderBuilder userId(String userId) { this.userId = userId; return this; }
        public OrderBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public OrderBuilder status(OrderStatus status) { this.status = status; return this; }
        public OrderBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderBuilder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public OrderBuilder finalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; return this; }

        public Order build() {
            Order order = Order.builder()
                    .userId(userId)
                    .build();
            ReflectionTestUtils.setField(order, "id", id);
            ReflectionTestUtils.setField(order, "orderNumber", orderNumber);
            ReflectionTestUtils.setField(order, "status", status);
            ReflectionTestUtils.setField(order, "totalAmount", totalAmount);
            ReflectionTestUtils.setField(order, "discountAmount", discountAmount);
            ReflectionTestUtils.setField(order, "finalAmount", finalAmount);
            return order;
        }
    }

    public static OrderItem createItem(Order order, Long productId, int quantity, BigDecimal price) {
        return itemBuilder().order(order).productId(productId).quantity(quantity).price(price).build();
    }

    public static ItemBuilder itemBuilder() {
        return new ItemBuilder();
    }

    public static class ItemBuilder {
        private Long id = 1L;
        private Order order;
        private Long sellerId = 1L;
        private Long productId = 1L;
        private String productName = "Test Product";
        private BigDecimal price = BigDecimal.valueOf(10000);
        private int quantity = 1;
        private BigDecimal subtotal;

        public ItemBuilder id(Long id) { this.id = id; return this; }
        public ItemBuilder order(Order order) { this.order = order; return this; }
        public ItemBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public ItemBuilder productId(Long productId) { this.productId = productId; return this; }
        public ItemBuilder productName(String productName) { this.productName = productName; return this; }
        public ItemBuilder price(BigDecimal price) { this.price = price; return this; }
        public ItemBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public ItemBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }

        public OrderItem build() {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .sellerId(sellerId)
                    .productId(productId)
                    .productName(productName)
                    .price(price)
                    .quantity(quantity)
                    .build();
            ReflectionTestUtils.setField(item, "id", id);
            if (subtotal != null) {
                ReflectionTestUtils.setField(item, "subtotal", subtotal);
            }
            return item;
        }
    }
}
