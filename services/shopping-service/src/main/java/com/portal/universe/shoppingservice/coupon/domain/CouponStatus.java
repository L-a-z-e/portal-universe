package com.portal.universe.shoppingservice.coupon.domain;

public enum CouponStatus {
    ACTIVE,     // 발급 가능
    EXHAUSTED,  // 소진됨
    EXPIRED,    // 만료됨
    INACTIVE    // 비활성화
}
