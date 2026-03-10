package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

public final class CartFixture {

    public static final String DEFAULT_USER_ID = "test-user-001";

    private CartFixture() {}

    public static Cart create() {
        return builder().build();
    }

    public static CartBuilder builder() {
        return new CartBuilder();
    }

    public static class CartBuilder {
        private Long id = 1L;
        private String userId = DEFAULT_USER_ID;
        private CartStatus status = CartStatus.ACTIVE;

        public CartBuilder id(Long id) { this.id = id; return this; }
        public CartBuilder userId(String userId) { this.userId = userId; return this; }
        public CartBuilder status(CartStatus status) { this.status = status; return this; }

        public Cart build() {
            Cart cart = Cart.builder().userId(userId).build();
            ReflectionTestUtils.setField(cart, "id", id);
            ReflectionTestUtils.setField(cart, "status", status);
            return cart;
        }
    }

    public static CartItem createItem(Cart cart, Long productId, String productName, BigDecimal price, int quantity) {
        CartItem item = CartItem.builder()
                .cart(cart)
                .sellerId(1L)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", 10L);
        return item;
    }
}
