package com.portal.universe.shoppingservice.payment.domain;

/**
 * 결제 상태를 나타내는 열거형입니다.
 */
public enum PaymentStatus {

    /**
     * 대기 중 - 결제 생성됨
     */
    PENDING("대기 중"),

    /**
     * 처리 중 - PG사에 요청 중
     */
    PROCESSING("처리 중"),

    /**
     * 완료 - 결제 성공
     */
    COMPLETED("완료"),

    /**
     * 실패 - 결제 실패
     */
    FAILED("실패"),

    /**
     * 취소됨 - 결제 취소
     */
    CANCELLED("취소"),

    /**
     * 환불됨 - 결제 환불
     */
    REFUNDED("환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 취소 가능 여부를 반환합니다.
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * 환불 가능 여부를 반환합니다.
     */
    public boolean isRefundable() {
        return this == COMPLETED;
    }
}
