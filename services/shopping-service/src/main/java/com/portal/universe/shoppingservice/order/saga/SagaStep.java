package com.portal.universe.shoppingservice.order.saga;

/**
 * Saga 실행 단계를 나타내는 열거형입니다.
 */
public enum SagaStep {

    /**
     * 1단계: 재고 예약
     */
    RESERVE_INVENTORY(1, "재고 예약"),

    /**
     * 2단계: 결제 처리
     */
    PROCESS_PAYMENT(2, "결제 처리"),

    /**
     * 3단계: 재고 차감 (결제 완료 후)
     */
    DEDUCT_INVENTORY(3, "재고 차감"),

    /**
     * 4단계: 배송 생성
     */
    CREATE_DELIVERY(4, "배송 생성"),

    /**
     * 5단계: 주문 확정
     */
    CONFIRM_ORDER(5, "주문 확정");

    private final int order;
    private final String description;

    SagaStep(int order, String description) {
        this.order = order;
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 다음 단계를 반환합니다.
     */
    public SagaStep next() {
        SagaStep[] steps = values();
        for (int i = 0; i < steps.length - 1; i++) {
            if (steps[i] == this) {
                return steps[i + 1];
            }
        }
        return null; // 마지막 단계
    }

}
