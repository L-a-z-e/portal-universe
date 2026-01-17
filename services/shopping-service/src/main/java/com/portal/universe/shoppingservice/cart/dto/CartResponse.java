package com.portal.universe.shoppingservice.cart.dto;

import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 장바구니 조회 응답 DTO입니다.
 */
public record CartResponse(
        Long id,
        String userId,
        CartStatus status,
        List<CartItemResponse> items,
        int itemCount,
        int totalQuantity,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getStatus(),
                itemResponses,
                cart.getItemCount(),
                cart.getTotalQuantity(),
                cart.getTotalAmount(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}
