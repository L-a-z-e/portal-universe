package com.portal.universe.common.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 시 발행되는 이벤트입니다.
 */
public record PaymentCompletedEvent(
        String paymentNumber,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        String pgTransactionId,
        LocalDateTime paidAt
) {}
