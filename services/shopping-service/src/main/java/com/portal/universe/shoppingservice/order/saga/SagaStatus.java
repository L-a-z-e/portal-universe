package com.portal.universe.shoppingservice.order.saga;

/**
 * Saga 실행 상태를 나타내는 열거형입니다.
 */
public enum SagaStatus {

    /**
     * 시작됨 - Saga 실행 중
     */
    STARTED("실행 중"),

    /**
     * 완료됨 - 모든 단계 성공
     */
    COMPLETED("완료"),

    /**
     * 보상 중 - 롤백 진행 중
     */
    COMPENSATING("보상 처리 중"),

    /**
     * 실패 - Saga 실패 (보상 완료)
     */
    FAILED("실패"),

    /**
     * 보상 실패 - 롤백도 실패 (수동 개입 필요)
     */
    COMPENSATION_FAILED("보상 실패");

    private final String description;

    SagaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
