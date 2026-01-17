package com.portal.universe.shoppingservice.order.saga;

/**
 * Saga 실행 단계를 나타내는 열거형입니다.
 */
public enum SagaStep {

    /**
     * 1단계: 재고 예약
     */
    RESERVE_INVENTORY(1, "재고 예약", true),

    /**
     * 2단계: 결제 처리
     */
    PROCESS_PAYMENT(2, "결제 처리", true),

    /**
     * 3단계: 재고 차감 (결제 완료 후)
     */
    DEDUCT_INVENTORY(3, "재고 차감", true),

    /**
     * 4단계: 배송 생성
     */
    CREATE_DELIVERY(4, "배송 생성", true),

    /**
     * 5단계: 주문 확정
     */
    CONFIRM_ORDER(5, "주문 확정", false);

    private final int order;
    private final String description;
    private final boolean compensatable;

    SagaStep(int order, String description, boolean compensatable) {
        this.order = order;
        this.description = description;
        this.compensatable = compensatable;
    }

    public int getOrder() {
        return order;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 보상(롤백) 가능 여부를 반환합니다.
     */
    public boolean isCompensatable() {
        return compensatable;
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

    /**
     * 이전 단계를 반환합니다.
     */
    public SagaStep previous() {
        SagaStep[] steps = values();
        for (int i = 1; i < steps.length; i++) {
            if (steps[i] == this) {
                return steps[i - 1];
            }
        }
        return null; // 첫 번째 단계
    }
}
