package com.portal.universe.shoppingservice.cart.domain;

/**
 * 장바구니 상태를 나타내는 열거형입니다.
 */
public enum CartStatus {

    /**
     * 활성 상태 - 상품 추가/수정/삭제 가능
     */
    ACTIVE("활성"),

    /**
     * 체크아웃 완료 - 주문 생성됨
     */
    CHECKED_OUT("체크아웃"),

    /**
     * 포기됨 - 일정 기간 미사용
     */
    ABANDONED("포기"),

    /**
     * 병합됨 - 다른 장바구니와 병합됨
     */
    MERGED("병합");

    private final String description;

    CartStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
