package com.portal.universe.shoppingsellerservice.inventory.dto;

import com.portal.universe.shoppingsellerservice.inventory.domain.MovementType;
import com.portal.universe.shoppingsellerservice.inventory.domain.StockMovement;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        MovementType movementType,
        Integer quantity,
        Integer previousAvailable,
        Integer afterAvailable,
        Integer previousReserved,
        Integer afterReserved,
        String referenceType,
        String referenceId,
        String reason,
        String performedBy,
        LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProductId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousAvailable(),
                movement.getAfterAvailable(),
                movement.getPreviousReserved(),
                movement.getAfterReserved(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getReason(),
                movement.getPerformedBy(),
                movement.getCreatedAt()
        );
    }
}
