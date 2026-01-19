package com.portal.universe.shoppingservice.coupon.dto;

import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponCreateRequest(
        @NotBlank(message = "Coupon code is required")
        @Size(max = 50, message = "Coupon code must be less than 50 characters")
        String code,

        @NotBlank(message = "Coupon name is required")
        @Size(max = 100, message = "Coupon name must be less than 100 characters")
        String name,

        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        BigDecimal discountValue,

        @DecimalMin(value = "0", message = "Minimum order amount must be non-negative")
        BigDecimal minimumOrderAmount,

        @DecimalMin(value = "0", message = "Maximum discount amount must be non-negative")
        BigDecimal maximumDiscountAmount,

        @NotNull(message = "Total quantity is required")
        @Min(value = 1, message = "Total quantity must be at least 1")
        Integer totalQuantity,

        @NotNull(message = "Start date is required")
        LocalDateTime startsAt,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDateTime expiresAt
) {
}
