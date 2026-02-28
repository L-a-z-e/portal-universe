package com.portal.universe.shoppingservice.feign.dto;

import java.math.BigDecimal;

public record CreatePaymentIntentRequest(
        String orderNumber,
        String userId,
        BigDecimal amount,
        String metadata
) {}
