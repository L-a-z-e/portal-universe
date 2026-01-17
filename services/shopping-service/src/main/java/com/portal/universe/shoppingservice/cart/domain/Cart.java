package com.portal.universe.shoppingservice.cart.domain;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 장바구니를 나타내는 JPA 엔티티입니다.
 * 사용자당 하나의 활성 장바구니만 가질 수 있습니다.
 */
@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_user_status", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 ID (auth-service의 사용자 ID)
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 장바구니 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status;

    /**
     * 장바구니 항목 목록
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Cart(String userId) {
        this.userId = userId;
        this.status = CartStatus.ACTIVE;
    }

    /**
     * 장바구니에 상품을 추가합니다.
     *
     * @param productId 상품 ID
     * @param productName 상품명
     * @param price 단가
     * @param quantity 수량
     * @return 추가된 장바구니 항목
     */
    public CartItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
        validateActive();

        // 이미 같은 상품이 있는지 확인
        Optional<CartItem> existingItem = findItemByProductId(productId);
        if (existingItem.isPresent()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
        }

        CartItem cartItem = CartItem.builder()
                .cart(this)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();

        this.items.add(cartItem);
        return cartItem;
    }

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param itemId 항목 ID
     * @param newQuantity 새 수량
     * @return 업데이트된 장바구니 항목
     */
    public CartItem updateItemQuantity(Long itemId, int newQuantity) {
        validateActive();

        CartItem item = findItemById(itemId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

        item.updateQuantity(newQuantity);
        return item;
    }

    /**
     * 장바구니에서 항목을 제거합니다.
     *
     * @param itemId 항목 ID
     */
    public void removeItem(Long itemId) {
        validateActive();

        CartItem item = findItemById(itemId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

        this.items.remove(item);
    }

    /**
     * 장바구니를 비웁니다.
     */
    public void clear() {
        validateActive();
        this.items.clear();
    }

    /**
     * 장바구니를 체크아웃 상태로 변경합니다.
     */
    public void checkout() {
        validateActive();

        if (this.items.isEmpty()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
        }

        this.status = CartStatus.CHECKED_OUT;
    }

    /**
     * 장바구니 총액을 계산합니다.
     *
     * @return 총액
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 장바구니 항목 수를 반환합니다.
     *
     * @return 항목 수
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * 장바구니 총 수량을 반환합니다 (모든 항목의 수량 합계).
     *
     * @return 총 수량
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * 상품 ID로 항목을 찾습니다.
     */
    public Optional<CartItem> findItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * 항목 ID로 항목을 찾습니다.
     */
    private Optional<CartItem> findItemById(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();
    }

    /**
     * 장바구니가 활성 상태인지 검증합니다.
     */
    private void validateActive() {
        if (this.status != CartStatus.ACTIVE) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ALREADY_CHECKED_OUT);
        }
    }
}
