package com.portal.universe.paymentservice.payment.dto;

import com.portal.universe.paymentservice.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String cardNumber,
        String cardExpiry,
        String cardCvv
) {}
