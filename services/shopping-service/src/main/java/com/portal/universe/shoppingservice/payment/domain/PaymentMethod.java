package com.portal.universe.shoppingservice.payment.domain;

/**
 * 결제 수단을 나타내는 열거형입니다.
 */
public enum PaymentMethod {

    /**
     * 신용카드/체크카드
     */
    CARD("카드"),

    /**
     * 계좌이체
     */
    BANK_TRANSFER("계좌이체"),

    /**
     * 가상계좌
     */
    VIRTUAL_ACCOUNT("가상계좌"),

    /**
     * 휴대폰 결제
     */
    MOBILE("휴대폰"),

    /**
     * 포인트 결제
     */
    POINTS("포인트");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
