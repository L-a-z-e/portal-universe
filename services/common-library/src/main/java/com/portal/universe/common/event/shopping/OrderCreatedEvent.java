package com.portal.universe.common.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 시 발행되는 이벤트입니다.
 */
public record OrderCreatedEvent(
        String orderNumber,
        String userId,
        BigDecimal totalAmount,
        int itemCount,
        List<OrderItemInfo> items,
        LocalDateTime createdAt
) {
    public record OrderItemInfo(
            Long productId,
            String productName,
            int quantity,
            BigDecimal price
    ) {}
}
