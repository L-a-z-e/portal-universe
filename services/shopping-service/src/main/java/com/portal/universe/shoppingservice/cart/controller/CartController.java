package com.portal.universe.shoppingservice.cart.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.cart.dto.*;
import com.portal.universe.shoppingservice.cart.service.CartService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.getCart(user.uuid()));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.addItem(user.uuid(), request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.updateItemQuantity(user.uuid(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable Long itemId,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.removeItem(user.uuid(), itemId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.clearCart(user.uuid()));
    }

    @PostMapping("/checkout")
    public ApiResponse<CartResponse> checkout(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.checkout(user.uuid()));
    }
}
