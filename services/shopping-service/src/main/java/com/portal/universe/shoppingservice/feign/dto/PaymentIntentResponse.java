package com.portal.universe.shoppingservice.feign.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(
        String intentId,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String status,
        String expiresAt,
        String createdAt
) {}
