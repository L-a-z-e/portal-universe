package com.portal.universe.shoppingservice.delivery.domain;

/**
 * 배송 상태를 나타내는 열거형입니다.
 */
public enum DeliveryStatus {

    /**
     * 준비 중 - 출고 준비
     */
    PREPARING("준비 중"),

    /**
     * 발송됨 - 택배사에 인계
     */
    SHIPPED("발송됨"),

    /**
     * 배송 중 - 이동 중
     */
    IN_TRANSIT("배송 중"),

    /**
     * 배송 완료 - 수령 완료
     */
    DELIVERED("배송 완료"),

    /**
     * 반품 - 반품 처리
     */
    RETURNED("반품"),

    /**
     * 취소됨 - 배송 취소
     */
    CANCELLED("취소됨");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 취소 가능 여부를 반환합니다.
     */
    public boolean isCancellable() {
        return this == PREPARING;
    }

    /**
     * 다음 상태로의 전환 가능 여부를 반환합니다.
     */
    public boolean canTransitionTo(DeliveryStatus next) {
        return switch (this) {
            case PREPARING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == IN_TRANSIT;
            case IN_TRANSIT -> next == DELIVERED || next == RETURNED;
            case DELIVERED -> next == RETURNED;
            default -> false;
        };
    }
}
