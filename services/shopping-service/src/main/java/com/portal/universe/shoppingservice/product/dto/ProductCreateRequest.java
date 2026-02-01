package com.portal.universe.shoppingservice.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 새로운 상품 생성을 요청할 때 사용하는 DTO입니다.
 *
 * @param name 상품명
 * @param description 상품 설명
 * @param price 가격
 * @param stock 재고
 */
public record ProductCreateRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,

        @NotNull(message = "Product price is required")
        @Positive(message = "Product price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Product stock is required")
        @Min(value = 0, message = "Product stock must be non-negative")
        Integer stock
) {
}
