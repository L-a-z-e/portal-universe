package com.portal.universe.shoppingservice.dto;

import jakarta.validation.constraints.*;

/**
 * Admin 상품 생성/수정 요청 DTO입니다.
 * Jakarta Validation을 통해 입력값 검증을 수행합니다.
 *
 * @param name 상품명 (필수, 1-200자)
 * @param description 상품 설명 (선택, 최대 2000자)
 * @param price 가격 (필수, 0보다 커야 함)
 * @param stock 재고 수량 (필수, 0 이상)
 */
public record AdminProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
        String name,

        @Size(max = 2000, message = "Product description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Product price is required")
        @Positive(message = "Product price must be greater than 0")
        Double price,

        @NotNull(message = "Product stock is required")
        @Min(value = 0, message = "Product stock must be non-negative")
        Integer stock
) {
}
