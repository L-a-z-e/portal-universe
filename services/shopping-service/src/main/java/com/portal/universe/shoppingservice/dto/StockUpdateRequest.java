package com.portal.universe.shoppingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin 재고 수정 요청 DTO입니다.
 *
 * @param stock 재고 수량 (필수, 0 이상)
 */
public record StockUpdateRequest(
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity must be non-negative")
        Integer stock
) {
}
