package com.portal.universe.shoppingservice.inventory.dto;

import com.portal.universe.shoppingservice.inventory.domain.MovementType;
import com.portal.universe.shoppingservice.inventory.domain.StockMovement;

import java.time.LocalDateTime;

/**
 * 재고 이동 이력 응답 DTO입니다.
 */
public record StockMovementResponse(
        Long id,
        Long productId,
        MovementType movementType,
        String movementTypeDescription,
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
                movement.getMovementType().getDescription(),
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
