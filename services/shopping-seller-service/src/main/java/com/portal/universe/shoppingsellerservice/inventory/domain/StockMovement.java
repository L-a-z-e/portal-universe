package com.portal.universe.shoppingsellerservice.inventory.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_available", nullable = false)
    private Integer previousAvailable;

    @Column(name = "after_available", nullable = false)
    private Integer afterAvailable;

    @Column(name = "previous_reserved", nullable = false)
    private Integer previousReserved;

    @Column(name = "after_reserved", nullable = false)
    private Integer afterReserved;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 500)
    private String reason;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Builder
    public StockMovement(Long inventoryId, Long productId, MovementType movementType,
                         Integer quantity, Integer previousAvailable, Integer afterAvailable,
                         Integer previousReserved, Integer afterReserved,
                         String referenceType, String referenceId, String reason, String performedBy) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousAvailable = previousAvailable;
        this.afterAvailable = afterAvailable;
        this.previousReserved = previousReserved;
        this.afterReserved = afterReserved;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.performedBy = performedBy;
    }
}
