package com.portal.universe.shoppingservice.inventory.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 재고 정보를 나타내는 JPA 엔티티입니다.
 * 상품별 가용 재고, 예약 재고, 전체 재고를 관리합니다.
 */
@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inventory_product_id", columnList = "product_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 ID (Product 테이블의 외래키)
     */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    /**
     * 가용 재고 (판매 가능한 수량)
     */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    /**
     * 예약 재고 (주문되어 결제 대기 중인 수량)
     */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /**
     * 전체 재고 (가용 + 예약)
     */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    /**
     * 낙관적 락을 위한 버전 (Pessimistic Lock의 백업용)
     */
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

    /**
     * 재고를 예약합니다 (주문 생성 시).
     * 가용 재고에서 예약 재고로 이동합니다.
     *
     * @param quantity 예약할 수량
     * @throws CustomBusinessException 재고가 부족한 경우
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.availableQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    /**
     * 예약된 재고를 실제로 차감합니다 (결제 완료 시).
     * 예약 재고와 전체 재고를 감소시킵니다.
     *
     * @param quantity 차감할 수량
     * @throws CustomBusinessException 예약된 재고가 부족한 경우
     */
    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_DEDUCTION_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    /**
     * 예약된 재고를 해제합니다 (주문 취소, 결제 실패 시).
     * 예약 재고에서 가용 재고로 복원합니다.
     *
     * @param quantity 해제할 수량
     * @throws CustomBusinessException 해제할 수량이 예약된 수량보다 많은 경우
     */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_RELEASE_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    /**
     * 재고를 추가합니다 (입고 시).
     * 가용 재고와 전체 재고를 증가시킵니다.
     *
     * @param quantity 추가할 수량
     * @throws CustomBusinessException 추가할 수량이 0 이하인 경우
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
    }

    /**
     * 반품으로 인한 재고 복원입니다.
     * 전체 재고와 가용 재고를 증가시킵니다.
     *
     * @param quantity 반품 수량
     */
    public void returnStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
    }

    /**
     * 관리자에 의한 재고 조정입니다.
     *
     * @param newAvailable 새로운 가용 재고
     * @param newReserved 새로운 예약 재고
     */
    public void adjust(int newAvailable, int newReserved) {
        if (newAvailable < 0 || newReserved < 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        this.availableQuantity = newAvailable;
        this.reservedQuantity = newReserved;
        this.totalQuantity = newAvailable + newReserved;
    }
}
