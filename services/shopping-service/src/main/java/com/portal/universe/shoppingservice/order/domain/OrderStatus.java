package com.portal.universe.shoppingservice.order.domain;

/**
 * 주문 상태를 나타내는 열거형입니다.
 */
public enum OrderStatus {

    /**
     * 대기 중 - 주문 생성됨, 결제 대기
     */
    PENDING("대기 중"),

    /**
     * 확정됨 - 재고 예약 완료
     */
    CONFIRMED("확정됨"),

    /**
     * 결제 완료 - 결제 성공
     */
    PAID("결제 완료"),

    /**
     * 배송 중 - 출고 완료
     */
    SHIPPING("배송 중"),

    /**
     * 배송 완료 - 수령 완료
     */
    DELIVERED("배송 완료"),

    /**
     * 취소됨 - 주문 취소
     */
    CANCELLED("취소됨"),

    /**
     * 환불됨 - 결제 환불 완료
     */
    REFUNDED("환불됨");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 주문 취소 가능 여부를 반환합니다.
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED || this == PAID;
    }

    /**
     * 환불 가능 여부를 반환합니다.
     */
    public boolean isRefundable() {
        return this == PAID || this == SHIPPING;
    }
}
