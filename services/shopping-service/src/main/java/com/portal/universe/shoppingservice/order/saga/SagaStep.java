package com.portal.universe.shoppingservice.order.saga;

/**
 * Saga 실행 단계를 나타내는 열거형입니다.
 */
public enum SagaStep {

    RESERVE_INVENTORY(1, "재고 예약"),
    PROCESS_PAYMENT(2, "결제 처리"),
    DEDUCT_INVENTORY(3, "재고 차감"),
    CREATE_DELIVERY(4, "배송 생성"),
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
