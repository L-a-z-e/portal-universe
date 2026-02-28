package com.portal.universe.paymentservice.payment.pg;

public record PgResponse(
        boolean success,
        String transactionId,
        String errorCode,
        String message,
        String rawResponse
) {}
