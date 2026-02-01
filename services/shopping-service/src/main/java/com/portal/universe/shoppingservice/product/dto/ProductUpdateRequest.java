package com.portal.universe.shoppingservice.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 상품 정보 수정을 요청할 때 사용하는 DTO입니다.
 *
 * @param name 수정할 상품명
 * @param description 수정할 상품 설명
 * @param price 수정할 가격
 * @param stock 수정할 재고
 */
public record ProductUpdateRequest(
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
