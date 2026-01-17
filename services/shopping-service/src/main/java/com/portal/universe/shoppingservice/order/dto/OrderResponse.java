package com.portal.universe.shoppingservice.order.dto;

import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 조회 응답 DTO입니다.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        String userId,
        OrderStatus status,
        String statusDescription,
        List<OrderItemResponse> items,
        int itemCount,
        int totalQuantity,
        BigDecimal totalAmount,
        AddressResponse shippingAddress,
        String cancelReason,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            addressResponse = AddressResponse.from(order.getShippingAddress());
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getStatus().getDescription(),
                itemResponses,
                order.getItems().size(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                addressResponse,
                order.getCancelReason(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
