package com.portal.universe.paymentservice.payment.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.paymentservice.common.exception.PaymentErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_number", columnList = "order_number"),
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_intent_id", columnList = "intent_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;

    @Column(name = "intent_id", nullable = false, length = 36)
    private String intentId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Builder
    public Payment(String intentId, String orderNumber, String userId,
                   BigDecimal amount, PaymentMethod paymentMethod) {
        this.paymentNumber = generatePaymentNumber();
        this.intentId = intentId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentMethod = paymentMethod;
    }

    public void startProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.PROCESSING;
    }

    public void complete(String pgTransactionId, String pgResponse) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.pgResponse = pgResponse;
        this.paidAt = Instant.now();
    }

    public void fail(String failureReason, String pgResponse) {
        if (this.status == PaymentStatus.COMPLETED || this.status == PaymentStatus.REFUNDED) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.pgResponse = pgResponse;
    }

    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
    }

    public void refund(String pgTransactionId) {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_REFUND_FAILED);
        }
        this.status = PaymentStatus.REFUNDED;
        this.pgTransactionId = pgTransactionId;
        this.refundedAt = Instant.now();
    }

    private static String generatePaymentNumber() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
