package com.portal.universe.common.event.shopping;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 배송 발송 시 발행되는 이벤트입니다.
 */
public record DeliveryShippedEvent(
        String trackingNumber,
        String orderNumber,
        String userId,
        String carrier,
        LocalDate estimatedDeliveryDate,
        LocalDateTime shippedAt
) {}
