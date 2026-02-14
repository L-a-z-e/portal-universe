package com.portal.universe.shoppingsellerservice.inventory.dto;

import com.portal.universe.shoppingsellerservice.inventory.domain.Inventory;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer totalQuantity,
        LocalDateTime updatedAt
) {
    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getTotalQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
