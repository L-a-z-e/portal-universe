package com.portal.universe.event.seller;

/**
 * Seller 도메인 Kafka topic 상수.
 *
 * <p>SSOT: {@code services/event-contracts/schemas/com/portal/universe/event/seller/*.avsc}</p>
 */
public final class SellerTopics {

    public static final String SELLER_APPROVED = "seller.approved";

    public static final String COUPON_CREATED = "seller.coupon.created";
    public static final String COUPON_UPDATED = "seller.coupon.updated";
    public static final String COUPON_DELETED = "seller.coupon.deleted";

    public static final String TIMEDEAL_CREATED = "seller.timedeal.created";
    public static final String TIMEDEAL_UPDATED = "seller.timedeal.updated";
    public static final String TIMEDEAL_CANCELLED = "seller.timedeal.cancelled";

    public static final String QUEUE_ACTIVATED = "seller.queue.activated";
    public static final String QUEUE_DEACTIVATED = "seller.queue.deactivated";

    public static final String PRODUCT_CREATED = "seller.product.created";
    public static final String PRODUCT_UPDATED = "seller.product.updated";
    public static final String PRODUCT_DELETED = "seller.product.deleted";

    private SellerTopics() {}
}
