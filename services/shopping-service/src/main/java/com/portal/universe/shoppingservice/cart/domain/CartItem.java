package com.portal.universe.shoppingservice.cart.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 장바구니 항목을 나타내는 JPA 엔티티입니다.
 * 상품 정보를 스냅샷으로 저장하여 가격 변동에 영향받지 않도록 합니다.
 */
@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_item_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_item_product_id", columnList = "product_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 장바구니
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 상품명 (스냅샷)
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 단가 (스냅샷)
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * 수량
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 장바구니에 추가된 시간
     */
    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Builder
    public CartItem(Cart cart, Long productId, String productName, BigDecimal price, Integer quantity) {
        validateQuantity(quantity);
        this.cart = cart;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * 수량을 변경합니다.
     *
     * @param newQuantity 새 수량
     */
    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    /**
     * 수량을 증가시킵니다.
     *
     * @param additionalQuantity 추가할 수량
     */
    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
        }
        this.quantity += additionalQuantity;
    }

    /**
     * 소계를 계산합니다 (단가 × 수량).
     *
     * @return 소계
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 가격 정보를 업데이트합니다 (관리자 또는 시스템에 의해).
     *
     * @param newPrice 새 가격
     * @param newProductName 새 상품명
     */
    public void updateProductInfo(BigDecimal newPrice, String newProductName) {
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.price = newPrice;
        }
        if (newProductName != null && !newProductName.isBlank()) {
            this.productName = newProductName;
        }
    }

    /**
     * 수량을 검증합니다.
     */
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
        }
    }
}
