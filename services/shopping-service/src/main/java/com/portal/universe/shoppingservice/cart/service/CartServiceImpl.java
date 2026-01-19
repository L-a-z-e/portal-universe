package com.portal.universe.shoppingservice.cart.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import com.portal.universe.shoppingservice.cart.dto.*;
import com.portal.universe.shoppingservice.cart.repository.CartRepository;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 장바구니 관리 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = getOrCreateActiveCart(userId);
        return CartResponse.from(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String userId, AddCartItemRequest request) {
        Cart cart = getOrCreateActiveCart(userId);

        // 상품 정보 조회
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 재고 확인
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
        }

        // 이미 장바구니에 있는 상품인 경우 수량 증가
        cart.findItemByProductId(request.productId())
                .ifPresentOrElse(
                        existingItem -> {
                            int newQuantity = existingItem.getQuantity() + request.quantity();
                            if (inventory.getAvailableQuantity() < newQuantity) {
                                throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
                            }
                            existingItem.updateQuantity(newQuantity);
                        },
                        () -> cart.addItem(
                                request.productId(),
                                product.getName(),
                                BigDecimal.valueOf(product.getPrice()),
                                request.quantity()
                        )
                );

        Cart savedCart = cartRepository.save(cart);
        log.info("Added item to cart for user {}: product {} x {}", userId, request.productId(), request.quantity());
        return CartResponse.from(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getActiveCartWithItems(userId);

        // 항목 찾기
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

        // 재고 확인
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
        }

        cart.updateItemQuantity(itemId, request.quantity());

        Cart savedCart = cartRepository.save(cart);
        log.info("Updated cart item quantity for user {}: item {} to {}", userId, itemId, request.quantity());
        return CartResponse.from(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userId, Long itemId) {
        Cart cart = getActiveCartWithItems(userId);
        cart.removeItem(itemId);

        Cart savedCart = cartRepository.save(cart);
        log.info("Removed item from cart for user {}: item {}", userId, itemId);
        return CartResponse.from(savedCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(String userId) {
        Cart cart = getActiveCartWithItems(userId);
        cart.clear();

        Cart savedCart = cartRepository.save(cart);
        log.info("Cleared cart for user {}", userId);
        return CartResponse.from(savedCart);
    }

    @Override
    @Transactional
    public CartResponse checkout(String userId) {
        Cart cart = getActiveCartWithItems(userId);

        // 모든 항목의 재고 확인
        for (CartItem item : cart.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
            }
        }

        cart.checkout();

        Cart savedCart = cartRepository.save(cart);
        log.info("Checked out cart for user {}: {} items, total {}", userId, cart.getItemCount(), cart.getTotalAmount());
        return CartResponse.from(savedCart);
    }

    /**
     * 사용자의 활성 장바구니를 조회하거나, 없으면 새로 생성합니다.
     */
    @Transactional
    protected Cart getOrCreateActiveCart(String userId) {
        return cartRepository.findActiveCartWithItems(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * 사용자의 활성 장바구니를 조회합니다 (항목 포함).
     */
    private Cart getActiveCartWithItems(String userId) {
        return cartRepository.findActiveCartWithItems(userId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));
    }
}
