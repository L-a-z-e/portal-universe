package com.portal.universe.paymentservice.intent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateIntentRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotBlank(message = "User ID is required")
        String userId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String metadata
) {}
