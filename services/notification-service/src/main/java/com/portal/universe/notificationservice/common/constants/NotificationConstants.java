package com.portal.universe.notificationservice.common.constants;

public final class NotificationConstants {
    private NotificationConstants() {}

    // Kafka Topics
    public static final String TOPIC_USER_SIGNUP = "user-signup";
    public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
    public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
    public static final String TOPIC_COUPON_ISSUED = "shopping.coupon.issued";
    public static final String TOPIC_TIMEDEAL_STARTED = "shopping.timedeal.started";

    // Blog Topics
    public static final String TOPIC_BLOG_POST_LIKED = "blog.post.liked";
    public static final String TOPIC_BLOG_POST_COMMENTED = "blog.post.commented";
    public static final String TOPIC_BLOG_COMMENT_REPLIED = "blog.comment.replied";
    public static final String TOPIC_BLOG_USER_FOLLOWED = "blog.user.followed";

    // Prism Topics
    public static final String TOPIC_PRISM_TASK_COMPLETED = "prism.task.completed";
    public static final String TOPIC_PRISM_TASK_FAILED = "prism.task.failed";

    // WebSocket
    public static final String WS_QUEUE_NOTIFICATIONS = "/queue/notifications";

    // Redis
    public static final String REDIS_CHANNEL_PREFIX = "notification:";
}
