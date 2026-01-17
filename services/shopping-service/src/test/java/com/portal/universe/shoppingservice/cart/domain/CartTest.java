package com.portal.universe.shoppingservice.cart.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cart 엔티티 단위 테스트입니다.
 */
class CartTest {

    @Nested
    @DisplayName("장바구니 생성 테스트")
    class CreateCartTest {

        @Test
        @DisplayName("새 장바구니 생성 시 ACTIVE 상태로 초기화된다")
        void createNewCart() {
            // given
            String userId = "user-123";

            // when
            Cart cart = Cart.builder()
                    .userId(userId)
                    .build();

            // then
            assertThat(cart.getUserId()).isEqualTo(userId);
            assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
            assertThat(cart.getItems()).isEmpty();
            assertThat(cart.getItemCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("장바구니 항목 추가 테스트")
    class AddItemTest {

        @Test
        @DisplayName("장바구니에 상품을 추가할 수 있다")
        void addItemToCart() {
            // given
            Cart cart = createActiveCart();
            Long productId = 1L;
            String productName = "Test Product";
            BigDecimal price = new BigDecimal("10000");
            int quantity = 2;

            // when
            CartItem item = cart.addItem(productId, productName, price, quantity);

            // then
            assertThat(cart.getItemCount()).isEqualTo(1);
            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getProductName()).isEqualTo(productName);
            assertThat(item.getPrice()).isEqualByComparingTo(price);
            assertThat(item.getQuantity()).isEqualTo(quantity);
        }

        @Test
        @DisplayName("여러 상품을 추가하면 총액이 합산된다")
        void addMultipleItemsCalculatesTotalAmount() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product 1", new BigDecimal("10000"), 2); // 20000
            cart.addItem(2L, "Product 2", new BigDecimal("5000"), 3);  // 15000

            // when
            BigDecimal totalAmount = cart.getTotalAmount();

            // then
            assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("35000"));
            assertThat(cart.getTotalQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("이미 있는 상품을 추가하면 예외가 발생한다")
        void addDuplicateItemThrowsException() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product", new BigDecimal("10000"), 1);

            // when & then
            assertThatThrownBy(() -> cart.addItem(1L, "Same Product", new BigDecimal("10000"), 1))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("체크아웃된 장바구니에 상품을 추가하면 예외가 발생한다")
        void addItemToCheckedOutCartThrowsException() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product", new BigDecimal("10000"), 1);
            cart.checkout();

            // when & then
            assertThatThrownBy(() -> cart.addItem(2L, "Another Product", new BigDecimal("5000"), 1))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.CART_ALREADY_CHECKED_OUT);
        }
    }

    @Nested
    @DisplayName("장바구니 체크아웃 테스트")
    class CheckoutTest {

        @Test
        @DisplayName("장바구니를 체크아웃하면 상태가 CHECKED_OUT으로 변경된다")
        void checkoutChangesStatus() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product", new BigDecimal("10000"), 1);

            // when
            cart.checkout();

            // then
            assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        }

        @Test
        @DisplayName("빈 장바구니를 체크아웃하면 예외가 발생한다")
        void checkoutEmptyCartThrowsException() {
            // given
            Cart cart = createActiveCart();

            // when & then
            assertThatThrownBy(cart::checkout)
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.CART_EMPTY);
        }
    }

    @Nested
    @DisplayName("장바구니 비우기 테스트")
    class ClearCartTest {

        @Test
        @DisplayName("장바구니를 비우면 모든 항목이 제거된다")
        void clearRemovesAllItems() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product 1", new BigDecimal("10000"), 1);
            cart.addItem(2L, "Product 2", new BigDecimal("5000"), 2);

            // when
            cart.clear();

            // then
            assertThat(cart.getItems()).isEmpty();
            assertThat(cart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("상품 찾기 테스트")
    class FindItemTest {

        @Test
        @DisplayName("상품 ID로 항목을 찾을 수 있다")
        void findItemByProductId() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product 1", new BigDecimal("10000"), 1);
            cart.addItem(2L, "Product 2", new BigDecimal("5000"), 2);

            // when
            var foundItem = cart.findItemByProductId(2L);

            // then
            assertThat(foundItem).isPresent();
            assertThat(foundItem.get().getProductName()).isEqualTo("Product 2");
        }

        @Test
        @DisplayName("존재하지 않는 상품 ID로 찾으면 빈 Optional을 반환한다")
        void findNonExistentItemReturnsEmpty() {
            // given
            Cart cart = createActiveCart();
            cart.addItem(1L, "Product 1", new BigDecimal("10000"), 1);

            // when
            var foundItem = cart.findItemByProductId(999L);

            // then
            assertThat(foundItem).isEmpty();
        }
    }

    private Cart createActiveCart() {
        return Cart.builder()
                .userId("test-user")
                .build();
    }
}
