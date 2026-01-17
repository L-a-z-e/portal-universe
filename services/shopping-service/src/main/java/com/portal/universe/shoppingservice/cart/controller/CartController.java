package com.portal.universe.shoppingservice.cart.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.cart.dto.*;
import com.portal.universe.shoppingservice.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/shopping/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 현재 사용자의 장바구니를 조회합니다.
     *
     * @param jwt 인증 정보
     * @return 장바구니 정보
     */
    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.getCart(userId));
    }

    /**
     * 장바구니에 상품을 추가합니다.
     *
     * @param request 추가할 상품 정보
     * @param jwt 인증 정보
     * @return 업데이트된 장바구니 정보
     */
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.addItem(userId, request));
    }

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param itemId 항목 ID
     * @param request 변경할 수량 정보
     * @param jwt 인증 정보
     * @return 업데이트된 장바구니 정보
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.updateItemQuantity(userId, itemId, request));
    }

    /**
     * 장바구니에서 항목을 제거합니다.
     *
     * @param itemId 항목 ID
     * @param jwt 인증 정보
     * @return 업데이트된 장바구니 정보
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.removeItem(userId, itemId));
    }

    /**
     * 장바구니를 비웁니다.
     *
     * @param jwt 인증 정보
     * @return 빈 장바구니 정보
     */
    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.clearCart(userId));
    }

    /**
     * 장바구니를 체크아웃합니다.
     * 체크아웃 후에는 주문 생성 API를 호출해야 합니다.
     *
     * @param jwt 인증 정보
     * @return 체크아웃된 장바구니 정보
     */
    @PostMapping("/checkout")
    public ApiResponse<CartResponse> checkout(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ApiResponse.success(cartService.checkout(userId));
    }
}
