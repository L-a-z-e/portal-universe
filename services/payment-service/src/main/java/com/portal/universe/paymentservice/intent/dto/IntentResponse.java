package com.portal.universe.paymentservice.intent.dto;

import com.portal.universe.paymentservice.intent.domain.PaymentIntent;

import java.math.BigDecimal;
import java.time.Instant;

public record IntentResponse(
        String intentId,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String status,
        Instant expiresAt,
        Instant createdAt
) {
    public static IntentResponse from(PaymentIntent intent) {
        return new IntentResponse(
                intent.getIntentId(),
                intent.getOrderNumber(),
                intent.getUserId(),
                intent.getAmount(),
                intent.getStatus().name(),
                intent.getExpiresAt(),
                intent.getCreatedAt()
        );
    }
}
