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
import java.time.Instant;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Builder
    public CartItem(Cart cart, Long sellerId, Long productId, String productName, BigDecimal price, Integer quantity) {
        validateQuantity(quantity);
        this.cart = cart;
        this.sellerId = sellerId;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
        }
        this.quantity += additionalQuantity;
    }

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

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
        }
    }
}
