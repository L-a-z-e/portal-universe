package com.portal.universe.shoppingservice.coupon.dto;

import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponResponse(
        Long id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Integer totalQuantity,
        Integer issuedQuantity,
        Integer remainingQuantity,
        CouponStatus status,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .remainingQuantity(coupon.getRemainingQuantity())
                .status(coupon.getStatus())
                .startsAt(coupon.getStartsAt())
                .expiresAt(coupon.getExpiresAt())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
