package com.portal.universe.shoppingservice.order.dto;

import com.portal.universe.shoppingservice.order.domain.OrderItem;

import java.math.BigDecimal;

/**
 * 주문 항목 조회 응답 DTO입니다.
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
