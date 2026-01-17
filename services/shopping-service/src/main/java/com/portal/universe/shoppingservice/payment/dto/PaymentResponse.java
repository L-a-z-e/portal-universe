package com.portal.universe.shoppingservice.payment.dto;

import com.portal.universe.shoppingservice.payment.domain.Payment;
import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import com.portal.universe.shoppingservice.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 조회 응답 DTO입니다.
 */
public record PaymentResponse(
        Long id,
        String paymentNumber,
        String orderNumber,
        String userId,
        BigDecimal amount,
        PaymentStatus status,
        String statusDescription,
        PaymentMethod paymentMethod,
        String paymentMethodDescription,
        String pgTransactionId,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getOrderNumber(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getStatus().getDescription(),
                payment.getPaymentMethod(),
                payment.getPaymentMethod().getDescription(),
                payment.getPgTransactionId(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
