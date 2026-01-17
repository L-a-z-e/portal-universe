package com.portal.universe.shoppingservice.payment.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 정보를 나타내는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_number", columnList = "payment_number", unique = true),
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제 번호 (내부 관리용)
     */
    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;

    /**
     * 주문 ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 주문 번호
     */
    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    /**
     * 사용자 ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 결제 금액
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * 결제 수단
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * PG사 거래 ID
     */
    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    /**
     * PG사 응답 (JSON)
     */
    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    /**
     * 실패 사유
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * 결제 완료 일시
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 환불 일시
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Payment(Long orderId, String orderNumber, String userId, BigDecimal amount, PaymentMethod paymentMethod) {
        this.paymentNumber = generatePaymentNumber();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentMethod = paymentMethod;
    }

    /**
     * 결제를 처리 중 상태로 변경합니다.
     */
    public void startProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.PROCESSING;
    }

    /**
     * 결제를 완료합니다.
     */
    public void complete(String pgTransactionId, String pgResponse) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.pgResponse = pgResponse;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 결제를 실패 처리합니다.
     */
    public void fail(String failureReason, String pgResponse) {
        if (this.status == PaymentStatus.COMPLETED || this.status == PaymentStatus.REFUNDED) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.pgResponse = pgResponse;
    }

    /**
     * 결제를 취소합니다.
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
    }

    /**
     * 결제를 환불합니다.
     */
    public void refund(String pgTransactionId) {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }
        this.status = PaymentStatus.REFUNDED;
        this.pgTransactionId = pgTransactionId;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 결제 번호를 생성합니다.
     * 형식: PAY-XXXXXXXX (UUID의 앞 8자리)
     */
    private static String generatePaymentNumber() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
