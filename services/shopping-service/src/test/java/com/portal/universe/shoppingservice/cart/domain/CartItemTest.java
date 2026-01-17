package com.portal.universe.shoppingservice.cart.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartItem 엔티티 단위 테스트입니다.
 */
class CartItemTest {

    @Test
    @DisplayName("장바구니 항목 생성 시 소계가 올바르게 계산된다")
    void createCartItemCalculatesSubtotal() {
        // given
        Cart cart = createCart();
        BigDecimal price = new BigDecimal("10000");
        int quantity = 3;

        // when
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(price)
                .quantity(quantity)
                .build();

        // then
        assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("수량을 변경하면 소계가 재계산된다")
    void updateQuantityRecalculatesSubtotal() {
        // given
        Cart cart = createCart();
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("5000"))
                .quantity(2)
                .build();

        // when
        item.updateQuantity(5);

        // then
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("수량을 증가시킬 수 있다")
    void increaseQuantity() {
        // given
        Cart cart = createCart();
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("5000"))
                .quantity(2)
                .build();

        // when
        item.increaseQuantity(3);

        // then
        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("0 이하의 수량으로 생성하면 예외가 발생한다")
    void createWithInvalidQuantityThrowsException() {
        // given
        Cart cart = createCart();

        // when & then
        assertThatThrownBy(() -> CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("10000"))
                .quantity(0)
                .build())
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
    }

    @Test
    @DisplayName("0 이하의 수량으로 업데이트하면 예외가 발생한다")
    void updateWithInvalidQuantityThrowsException() {
        // given
        Cart cart = createCart();
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("10000"))
                .quantity(1)
                .build();

        // when & then
        assertThatThrownBy(() -> item.updateQuantity(-1))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
    }

    @Test
    @DisplayName("0 이하의 수량을 증가시키면 예외가 발생한다")
    void increaseWithInvalidQuantityThrowsException() {
        // given
        Cart cart = createCart();
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("10000"))
                .quantity(1)
                .build();

        // when & then
        assertThatThrownBy(() -> item.increaseQuantity(0))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
    }

    @Test
    @DisplayName("상품 정보를 업데이트할 수 있다")
    void updateProductInfo() {
        // given
        Cart cart = createCart();
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Original Name")
                .price(new BigDecimal("10000"))
                .quantity(1)
                .build();

        // when
        item.updateProductInfo(new BigDecimal("15000"), "Updated Name");

        // then
        assertThat(item.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(item.getProductName()).isEqualTo("Updated Name");
    }

    private Cart createCart() {
        return Cart.builder()
                .userId("test-user")
                .build();
    }
}
