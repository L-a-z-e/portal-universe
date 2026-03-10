package com.portal.universe.shoppingservice.cart.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.dto.AddCartItemRequest;
import com.portal.universe.shoppingservice.cart.dto.CartResponse;
import com.portal.universe.shoppingservice.cart.dto.UpdateCartItemRequest;
import com.portal.universe.shoppingservice.cart.repository.CartRepository;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.support.fixture.CartFixture;
import com.portal.universe.shoppingservice.support.fixture.InventoryFixture;
import com.portal.universe.shoppingservice.support.fixture.ProductFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart createCartWithItem(String userId) {
        Cart cart = CartFixture.builder().userId(userId).build();
        CartItem item = CartFixture.createItem(cart, 1L, "Test Product", BigDecimal.valueOf(5000), 2);
        cart.getItems().add(item);
        return cart;
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("should_returnExistingCart_when_cartExists")
        void should_returnExistingCart_when_cartExists() {
            // given
            Cart cart = CartFixture.builder().userId("user1").build();
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));

            // when
            CartResponse result = cartService.getCart("user1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo("user1");
        }

        @Test
        @DisplayName("should_createNewCart_when_noActiveCart")
        void should_createNewCart_when_noActiveCart() {
            // given
            Cart newCart = CartFixture.builder().userId("user1").build();
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of());
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

            // when
            CartResponse result = cartService.getCart("user1");

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("should_addItem_when_validRequest")
        void should_addItem_when_validRequest() {
            // given
            Cart cart = CartFixture.builder().userId("user1").build();
            Product product = ProductFixture.builder()
                    .id(2L).name("Test Product").price(BigDecimal.valueOf(5000)).build();
            Inventory inventory = InventoryFixture.builder()
                    .productId(2L).availableQuantity(100).build();

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product));
            when(inventoryRepository.findByProductIds(List.of(2L))).thenReturn(List.of(inventory));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            AddCartItemRequest request = new AddCartItemRequest(2L, 3);

            // when
            CartResponse result = cartService.addItem("user1", request);

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("should_throwException_when_productNotFound")
        void should_throwException_when_productNotFound() {
            // given
            Cart cart = CartFixture.builder().userId("user1").build();
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            AddCartItemRequest request = new AddCartItemRequest(999L, 1);

            // when & then
            assertThatThrownBy(() -> cartService.addItem("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_exceedsStock")
        void should_throwException_when_exceedsStock() {
            // given
            Cart cart = CartFixture.builder().userId("user1").build();
            Product product = ProductFixture.builder()
                    .id(1L).name("Test Product").price(BigDecimal.valueOf(5000)).build();
            Inventory inventory = InventoryFixture.builder()
                    .productId(1L).availableQuantity(2).build();

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(inventoryRepository.findByProductIds(List.of(1L))).thenReturn(List.of(inventory));

            AddCartItemRequest request = new AddCartItemRequest(1L, 10);

            // when & then
            assertThatThrownBy(() -> cartService.addItem("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateItemQuantity")
    class UpdateItemQuantity {

        @Test
        @DisplayName("should_updateQuantity_when_valid")
        void should_updateQuantity_when_valid() {
            // given
            Cart cart = createCartWithItem("user1");
            Inventory inventory = InventoryFixture.builder()
                    .productId(1L).availableQuantity(100).build();

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductIds(List.of(1L))).thenReturn(List.of(inventory));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            UpdateCartItemRequest request = new UpdateCartItemRequest(5);

            // when
            CartResponse result = cartService.updateItemQuantity("user1", 10L, request);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_itemNotFound")
        void should_throwException_when_itemNotFound() {
            // given
            Cart cart = createCartWithItem("user1");
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));

            UpdateCartItemRequest request = new UpdateCartItemRequest(5);

            // when & then
            assertThatThrownBy(() -> cartService.updateItemQuantity("user1", 999L, request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_exceedsStock")
        void should_throwException_when_exceedsStock() {
            // given
            Cart cart = createCartWithItem("user1");
            Inventory inventory = InventoryFixture.builder()
                    .productId(1L).availableQuantity(2).build();

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductIds(List.of(1L))).thenReturn(List.of(inventory));

            UpdateCartItemRequest request = new UpdateCartItemRequest(10);

            // when & then
            assertThatThrownBy(() -> cartService.updateItemQuantity("user1", 10L, request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("should_removeItem_when_valid")
        void should_removeItem_when_valid() {
            // given
            Cart cart = createCartWithItem("user1");
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            // when
            CartResponse result = cartService.removeItem("user1", 10L);

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("should_throwException_when_itemNotFound")
        void should_throwException_when_itemNotFound() {
            // given
            Cart cart = createCartWithItem("user1");
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));

            // when & then
            assertThatThrownBy(() -> cartService.removeItem("user1", 999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("should_clearCart_when_valid")
        void should_clearCart_when_valid() {
            // given
            Cart cart = createCartWithItem("user1");
            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            // when
            CartResponse result = cartService.clearCart("user1");

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("should_checkout_when_valid")
        void should_checkout_when_valid() {
            // given
            Cart cart = createCartWithItem("user1");
            Inventory inventory = InventoryFixture.builder()
                    .productId(1L).availableQuantity(100).build();

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductIds(List.of(1L))).thenReturn(List.of(inventory));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            // when
            CartResponse result = cartService.checkout("user1");

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }
}
