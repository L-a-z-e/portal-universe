package com.portal.universe.shoppingservice.cart.service;

import com.portal.universe.shoppingservice.cart.dto.*;

/**
 * 장바구니 관리 서비스 인터페이스입니다.
 */
public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, AddCartItemRequest request);

    CartResponse updateItemQuantity(String userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(String userId, Long itemId);

    CartResponse clearCart(String userId);

    CartResponse checkout(String userId);
}
