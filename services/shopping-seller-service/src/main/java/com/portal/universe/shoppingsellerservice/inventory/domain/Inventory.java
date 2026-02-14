package com.portal.universe.shoppingsellerservice.inventory.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Inventory(Long productId, Integer initialQuantity) {
        this.productId = productId;
        this.availableQuantity = initialQuantity != null ? initialQuantity : 0;
        this.reservedQuantity = 0;
        this.totalQuantity = this.availableQuantity;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(SellerErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.availableQuantity < quantity) {
            throw new CustomBusinessException(SellerErrorCode.INSUFFICIENT_STOCK);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(SellerErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(SellerErrorCode.STOCK_DEDUCTION_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(SellerErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(SellerErrorCode.STOCK_RELEASE_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(SellerErrorCode.INVALID_STOCK_QUANTITY);
        }
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
    }
}
