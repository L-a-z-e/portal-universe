package com.portal.universe.common.event.shopping;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 재고 예약 시 발행되는 이벤트입니다.
 */
public record InventoryReservedEvent(
        String orderNumber,
        String userId,
        Map<Long, Integer> reservedQuantities, // productId -> quantity
        LocalDateTime reservedAt
) {}
