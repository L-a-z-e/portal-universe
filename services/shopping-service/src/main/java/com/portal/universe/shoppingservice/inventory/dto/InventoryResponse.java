package com.portal.universe.shoppingservice.inventory.dto;

import com.portal.universe.shoppingservice.inventory.domain.Inventory;

import java.time.LocalDateTime;

/**
 * 재고 조회 응답 DTO입니다.
 */
public record InventoryResponse(
        Long id,
        Long productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer totalQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getTotalQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
