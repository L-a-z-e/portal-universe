package com.portal.universe.shoppingservice.cart.dto;

import com.portal.universe.shoppingservice.cart.domain.CartItem;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 장바구니 항목 조회 응답 DTO입니다.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal,
        Instant addedAt
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getAddedAt()
        );
    }
}
