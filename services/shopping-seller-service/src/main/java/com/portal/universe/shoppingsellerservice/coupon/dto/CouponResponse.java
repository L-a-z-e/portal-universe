package com.portal.universe.shoppingsellerservice.coupon.dto;

import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        Long sellerId,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Integer totalQuantity,
        Integer issuedQuantity,
        CouponStatus status,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getSellerId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getStatus(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
