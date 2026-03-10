package com.portal.universe.shoppingservice.cart.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Builder
    public Cart(String userId) {
        this.userId = userId;
        this.status = CartStatus.ACTIVE;
    }

    public CartItem addItem(Long sellerId, Long productId, String productName, BigDecimal price, int quantity) {
        validateActive();

        // 이미 같은 상품이 있는지 확인
        Optional<CartItem> existingItem = findItemByProductId(productId);
        if (existingItem.isPresent()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
        }

        CartItem cartItem = CartItem.builder()
                .cart(this)
                .sellerId(sellerId)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();

        this.items.add(cartItem);
        return cartItem;
    }

    public CartItem updateItemQuantity(Long itemId, int newQuantity) {
        validateActive();

        CartItem item = findItemById(itemId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

        item.updateQuantity(newQuantity);
        return item;
    }

    public void removeItem(Long itemId) {
        validateActive();

        CartItem item = findItemById(itemId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

        this.items.remove(item);
    }

    public void clear() {
        validateActive();
        this.items.clear();
    }

    public void checkout() {
        validateActive();

        if (this.items.isEmpty()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
        }

        this.status = CartStatus.CHECKED_OUT;
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount() {
        return items.size();
    }

    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public Optional<CartItem> findItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private Optional<CartItem> findItemById(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();
    }

    private void validateActive() {
        if (this.status != CartStatus.ACTIVE) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ALREADY_CHECKED_OUT);
        }
    }
}
