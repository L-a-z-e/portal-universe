package com.portal.universe.shoppingservice.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 재고 이동 이력을 기록하는 JPA 엔티티입니다.
 * 모든 재고 변경 작업에 대한 감사 추적(Audit Trail)을 제공합니다.
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movement_inventory_id", columnList = "inventory_id"),
        @Index(name = "idx_stock_movement_product_id", columnList = "product_id"),
        @Index(name = "idx_stock_movement_reference", columnList = "reference_type, reference_id"),
        @Index(name = "idx_stock_movement_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 재고 엔티티 ID
     */
    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    /**
     * 상품 ID (빠른 조회를 위해 비정규화)
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 이동 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /**
     * 이동 수량 (양수: 증가, 음수: 감소)
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 이동 전 가용 재고
     */
    @Column(name = "previous_available", nullable = false)
    private Integer previousAvailable;

    /**
     * 이동 후 가용 재고
     */
    @Column(name = "after_available", nullable = false)
    private Integer afterAvailable;

    /**
     * 이동 전 예약 재고
     */
    @Column(name = "previous_reserved", nullable = false)
    private Integer previousReserved;

    /**
     * 이동 후 예약 재고
     */
    @Column(name = "after_reserved", nullable = false)
    private Integer afterReserved;

    /**
     * 참조 유형 (ORDER, PAYMENT, RETURN, ADMIN 등)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * 참조 ID (주문번호, 결제번호 등)
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /**
     * 이동 사유
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * 작업 수행자 (사용자 ID 또는 시스템)
     */
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
