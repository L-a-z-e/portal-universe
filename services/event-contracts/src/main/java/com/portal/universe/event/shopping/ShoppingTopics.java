package com.portal.universe.event.shopping;

public final class ShoppingTopics {

    public static final String ORDER_CREATED = "shopping.order.created";
    public static final String ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String PAYMENT_FAILED = "shopping.payment.failed";
    public static final String INVENTORY_RESERVED = "shopping.inventory.reserved";
    public static final String DELIVERY_SHIPPED = "shopping.delivery.shipped";
    public static final String COUPON_ISSUED = "shopping.coupon.issued";
    public static final String TIMEDEAL_STARTED = "shopping.timedeal.started";

    private ShoppingTopics() {}
}
