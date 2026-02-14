package com.portal.universe.shoppingsellerservice.coupon.dto;

import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0") BigDecimal minimumOrderAmount,
        @DecimalMin("0") BigDecimal maximumDiscountAmount,
        @NotNull @Min(1) Integer totalQuantity,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime expiresAt
) {
    public Coupon toEntity(Long sellerId) {
        return Coupon.builder()
                .sellerId(sellerId)
                .code(code)
                .name(name)
                .description(description)
                .discountType(discountType)
                .discountValue(discountValue)
                .minimumOrderAmount(minimumOrderAmount)
                .maximumDiscountAmount(maximumDiscountAmount)
                .totalQuantity(totalQuantity)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();
    }
}
