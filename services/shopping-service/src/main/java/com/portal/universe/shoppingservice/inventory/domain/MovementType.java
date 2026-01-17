package com.portal.universe.shoppingservice.inventory.domain;

/**
 * 재고 이동 유형을 나타내는 열거형입니다.
 */
public enum MovementType {

    /**
     * 입고 - 재고 추가
     */
    INBOUND("입고"),

    /**
     * 예약 - 주문에 의한 재고 예약
     */
    RESERVE("예약"),

    /**
     * 해제 - 예약된 재고 해제 (주문 취소 등)
     */
    RELEASE("해제"),

    /**
     * 차감 - 결제 완료 후 실제 재고 차감
     */
    DEDUCT("차감"),

    /**
     * 반품 - 반품에 의한 재고 복원
     */
    RETURN("반품"),

    /**
     * 조정 - 관리자에 의한 수동 조정
     */
    ADJUSTMENT("조정"),

    /**
     * 초기화 - 최초 재고 설정
     */
    INITIAL("초기화");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
