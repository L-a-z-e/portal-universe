package com.portal.universe.event.shopping;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 시 발행되는 이벤트입니다.
 */
public record CouponIssuedEvent(
        String userId,
        String couponCode,
        String couponName,
        String discountType,
        int discountValue,
        LocalDateTime expiresAt
) {}
