package com.portal.universe.shoppingservice.cart.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import com.portal.universe.shoppingservice.cart.dto.AddCartItemRequest;
import com.portal.universe.shoppingservice.cart.dto.CartResponse;
import com.portal.universe.shoppingservice.cart.dto.UpdateCartItemRequest;
import com.portal.universe.shoppingservice.cart.repository.CartRepository;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    private Cart createActiveCart(String userId) {
        Cart cart = Cart.builder().userId(userId).build();
        ReflectionTestUtils.setField(cart, "id", 1L);
        return cart;
    }

    private Cart createCartWithItem(String userId) {
        Cart cart = createActiveCart(userId);
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(1L)
                .productName("Test Product")
                .price(BigDecimal.valueOf(5000))
                .quantity(2)
                .build();
        ReflectionTestUtils.setField(item, "id", 10L);
        cart.getItems().add(item);
        return cart;
    }

    private Product createProduct(Long id) {
        Product product = Product.builder()
                .name("Test Product")
                .description("desc")
                .price(BigDecimal.valueOf(5000))
                .stock(100)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Inventory createInventory(Long productId, int available) {
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .initialQuantity(available)
                .build();
        ReflectionTestUtils.setField(inventory, "id", 1L);
        return inventory;
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("should_returnExistingCart_when_cartExists")
        void should_returnExistingCart_when_cartExists() {
            // given
            Cart cart = createActiveCart("user1");
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
            Cart newCart = createActiveCart("user1");
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
            Cart cart = createActiveCart("user1");
            Product product = createProduct(2L);
            Inventory inventory = createInventory(2L, 100);

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product));
            when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));
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
            Cart cart = createActiveCart("user1");
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
            Cart cart = createActiveCart("user1");
            Product product = createProduct(1L);
            Inventory inventory = createInventory(1L, 2);

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

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
            Inventory inventory = createInventory(1L, 100);

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
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
            Inventory inventory = createInventory(1L, 2);

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

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
            Inventory inventory = createInventory(1L, 100);

            when(cartRepository.findActiveCartWithItems("user1")).thenReturn(List.of(cart));
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            // when
            CartResponse result = cartService.checkout("user1");

            // then
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }
}
