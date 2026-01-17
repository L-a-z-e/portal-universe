package com.portal.universe.common.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 확정 시 발행되는 이벤트입니다.
 */
public record OrderConfirmedEvent(
        String orderNumber,
        String userId,
        BigDecimal totalAmount,
        String paymentNumber,
        LocalDateTime confirmedAt
) {}
