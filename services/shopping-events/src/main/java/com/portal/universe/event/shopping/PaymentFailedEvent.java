package com.portal.universe.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 실패 시 발행되는 이벤트입니다.
 */
public record PaymentFailedEvent(
        String paymentNumber,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        String failureReason,
        LocalDateTime failedAt
) {}
