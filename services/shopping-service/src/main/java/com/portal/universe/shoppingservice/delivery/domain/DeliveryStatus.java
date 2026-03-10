package com.portal.universe.shoppingservice.delivery.domain;

/**
 * 배송 상태를 나타내는 열거형입니다.
 */
public enum DeliveryStatus {

    PREPARING("준비 중"),
    SHIPPED("발송됨"),
    IN_TRANSIT("배송 중"),
    DELIVERED("배송 완료"),
    RETURNED("반품"),
    CANCELLED("취소됨");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCancellable() {
        return this == PREPARING;
    }

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
