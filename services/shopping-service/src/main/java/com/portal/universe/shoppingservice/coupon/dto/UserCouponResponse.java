package com.portal.universe.shoppingservice.coupon.dto;

import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingservice.coupon.domain.UserCoupon;
import com.portal.universe.shoppingservice.coupon.domain.UserCouponStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record UserCouponResponse(
        Long id,
        Long couponId,
        String couponCode,
        String couponName,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        UserCouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        Long usedOrderId
) {
    public static UserCouponResponse from(UserCoupon userCoupon) {
        return UserCouponResponse.builder()
                .id(userCoupon.getId())
                .couponId(userCoupon.getCoupon().getId())
                .couponCode(userCoupon.getCoupon().getCode())
                .couponName(userCoupon.getCoupon().getName())
                .discountType(userCoupon.getCoupon().getDiscountType())
                .discountValue(userCoupon.getCoupon().getDiscountValue())
                .minimumOrderAmount(userCoupon.getCoupon().getMinimumOrderAmount())
                .maximumDiscountAmount(userCoupon.getCoupon().getMaximumDiscountAmount())
                .status(userCoupon.getStatus())
                .issuedAt(userCoupon.getIssuedAt())
                .expiresAt(userCoupon.getExpiresAt())
                .usedAt(userCoupon.getUsedAt())
                .usedOrderId(userCoupon.getUsedOrderId())
                .build();
    }
}
