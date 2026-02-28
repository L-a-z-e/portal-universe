package com.portal.universe.paymentservice.payment.dto;

import com.portal.universe.paymentservice.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentNumber,
        String intentId,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String pgTransactionId,
        Instant paidAt,
        Instant refundedAt,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentNumber(),
                payment.getIntentId(),
                payment.getOrderNumber(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod().name(),
                payment.getPgTransactionId(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt()
        );
    }
}
