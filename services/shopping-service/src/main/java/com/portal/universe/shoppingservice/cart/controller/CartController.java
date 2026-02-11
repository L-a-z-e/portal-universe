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

    /**
     * 현재 사용자의 장바구니를 조회합니다.
     *
     * @param user 인증된 사용자 정보
     * @return 장바구니 정보
     */
    @GetMapping
    public ApiResponse<CartResponse> getCart(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.getCart(user.uuid()));
    }

    /**
     * 장바구니에 상품을 추가합니다.
     *
     * @param request 추가할 상품 정보
     * @param user 인증된 사용자 정보
     * @return 업데이트된 장바구니 정보
     */
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.addItem(user.uuid(), request));
    }

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param itemId 항목 ID
     * @param request 변경할 수량 정보
     * @param user 인증된 사용자 정보
     * @return 업데이트된 장바구니 정보
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.updateItemQuantity(user.uuid(), itemId, request));
    }

    /**
     * 장바구니에서 항목을 제거합니다.
     *
     * @param itemId 항목 ID
     * @param user 인증된 사용자 정보
     * @return 업데이트된 장바구니 정보
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable Long itemId,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.removeItem(user.uuid(), itemId));
    }

    /**
     * 장바구니를 비웁니다.
     *
     * @param user 인증된 사용자 정보
     * @return 빈 장바구니 정보
     */
    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.clearCart(user.uuid()));
    }

    /**
     * 장바구니를 체크아웃합니다.
     * 체크아웃 후에는 주문 생성 API를 호출해야 합니다.
     *
     * @param user 인증된 사용자 정보
     * @return 체크아웃된 장바구니 정보
     */
    @PostMapping("/checkout")
    public ApiResponse<CartResponse> checkout(@CurrentUser AuthUser user) {
        return ApiResponse.success(cartService.checkout(user.uuid()));
    }
}
