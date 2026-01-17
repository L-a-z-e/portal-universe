package com.portal.universe.shoppingservice.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 재고 수정 요청 DTO입니다.
 */
public record InventoryUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be non-negative")
        Integer quantity,

        String reason
) {
}
