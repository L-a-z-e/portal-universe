package com.portal.universe.paymentservice.intent.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.paymentservice.common.exception.PaymentErrorCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents", indexes = {
        @Index(name = "idx_intent_order_number", columnList = "order_number"),
        @Index(name = "idx_intent_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentIntent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intent_id", nullable = false, unique = true, length = 36)
    private String intentId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentIntentStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder
    public PaymentIntent(String orderNumber, String userId, BigDecimal amount,
                         String metadata, long expiryMinutes) {
        this.intentId = UUID.randomUUID().toString();
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentIntentStatus.REQUIRES_PAYMENT;
        this.metadata = metadata;
        this.expiresAt = Instant.now().plus(Duration.ofMinutes(expiryMinutes));
    }

    public void startProcessing() {
        if (!this.status.isPayable()) {
            throw new CustomBusinessException(PaymentErrorCode.INTENT_NOT_PAYABLE);
        }
        this.status = PaymentIntentStatus.PROCESSING;
    }

    public void succeed() {
        if (this.status != PaymentIntentStatus.PROCESSING) {
            throw new CustomBusinessException(PaymentErrorCode.INTENT_NOT_PAYABLE);
        }
        this.status = PaymentIntentStatus.SUCCEEDED;
    }

    public void fail() {
        this.status = PaymentIntentStatus.FAILED;
    }

    public void expire() {
        if (this.status == PaymentIntentStatus.REQUIRES_PAYMENT) {
            this.status = PaymentIntentStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(PaymentErrorCode.INTENT_CANNOT_BE_CANCELLED);
        }
        this.status = PaymentIntentStatus.CANCELLED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public void validateOwnership(String userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomBusinessException(PaymentErrorCode.INTENT_USER_MISMATCH);
        }
    }

    public void validatePayable() {
        if (isExpired()) {
            expire();
            throw new CustomBusinessException(PaymentErrorCode.INTENT_EXPIRED);
        }
        if (!this.status.isPayable()) {
            throw new CustomBusinessException(PaymentErrorCode.INTENT_NOT_PAYABLE);
        }
    }
}
