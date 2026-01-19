package com.portal.universe.notificationservice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // Order
    ORDER_CREATED("주문이 접수되었습니다"),
    ORDER_CONFIRMED("주문이 확정되었습니다"),
    ORDER_CANCELLED("주문이 취소되었습니다"),

    // Delivery
    DELIVERY_STARTED("상품이 발송되었습니다"),
    DELIVERY_IN_TRANSIT("상품이 배송 중입니다"),
    DELIVERY_COMPLETED("상품이 배송 완료되었습니다"),

    // Payment
    PAYMENT_COMPLETED("결제가 완료되었습니다"),
    PAYMENT_FAILED("결제가 실패했습니다"),
    REFUND_COMPLETED("환불이 완료되었습니다"),

    // Coupon
    COUPON_ISSUED("쿠폰이 발급되었습니다"),
    COUPON_EXPIRING("쿠폰이 곧 만료됩니다"),

    // TimeDeal
    TIMEDEAL_STARTING("타임딜이 곧 시작됩니다"),
    TIMEDEAL_STARTED("타임딜이 시작되었습니다"),

    // System
    SYSTEM("시스템 알림");

    private final String defaultMessage;
}
