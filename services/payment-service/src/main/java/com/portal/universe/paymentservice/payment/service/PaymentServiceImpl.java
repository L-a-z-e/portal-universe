package com.portal.universe.paymentservice.payment.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.shopping.PaymentCancelledEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.PaymentFailedEvent;
import com.portal.universe.paymentservice.common.exception.PaymentErrorCode;
import com.portal.universe.paymentservice.event.PaymentEventPublisher;
import com.portal.universe.paymentservice.intent.domain.PaymentIntent;
import com.portal.universe.paymentservice.intent.repository.PaymentIntentRepository;
import com.portal.universe.paymentservice.payment.domain.Payment;
import com.portal.universe.paymentservice.payment.dto.ConfirmPaymentRequest;
import com.portal.universe.paymentservice.payment.dto.PaymentResponse;
import com.portal.universe.paymentservice.payment.pg.MockPGClient;
import com.portal.universe.paymentservice.payment.pg.PgResponse;
import com.portal.universe.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentIntentRepository intentRepository;
    private final MockPGClient mockPGClient;
    private final PaymentEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String userId, String intentId, ConfirmPaymentRequest request) {
        // 1. Intent 조회 및 검증
        PaymentIntent intent = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.INTENT_NOT_FOUND));

        intent.validateOwnership(userId);
        intent.validatePayable();

        // 중복 결제 방지
        paymentRepository.findByIntentId(intentId)
                .ifPresent(p -> {
                    throw new CustomBusinessException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
                });

        // 2. Intent 처리 시작
        intent.startProcessing();
        intentRepository.save(intent);

        // 3. Payment 생성
        Payment payment = Payment.builder()
                .intentId(intent.getIntentId())
                .orderNumber(intent.getOrderNumber())
                .userId(userId)
                .amount(intent.getAmount())
                .paymentMethod(request.paymentMethod())
                .build();

        payment = paymentRepository.save(payment);

        // 4. 결제 처리
        payment.startProcessing();

        PgResponse pgResponse = mockPGClient.processPayment(
                payment.getPaymentNumber(),
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                request.cardNumber()
        );

        // 5. 결과 처리
        if (pgResponse.success()) {
            payment.complete(pgResponse.transactionId(), pgResponse.rawResponse());
            paymentRepository.save(payment);

            intent.succeed();
            intentRepository.save(intent);

            log.info("Payment completed: {} (order: {}, amount: {})",
                    payment.getPaymentNumber(), intent.getOrderNumber(), payment.getAmount());

            // 결제 완료 이벤트 발행 (items는 shopping-service가 OrderSettlementCreatedEvent로 발행)
            eventPublisher.publishPaymentCompleted(PaymentCompletedEvent.newBuilder()
                    .setPaymentNumber(payment.getPaymentNumber())
                    .setOrderNumber(intent.getOrderNumber())
                    .setUserId(userId)
                    .setAmount(payment.getAmount())
                    .setPaymentMethod(payment.getPaymentMethod().name())
                    .setPgTransactionId(payment.getPgTransactionId())
                    .setPaidAt(java.time.Instant.now())
                    .setItems(Collections.emptyList())
                    .build());
        } else {
            payment.fail(pgResponse.errorCode() + ": " + pgResponse.message(), pgResponse.rawResponse());
            paymentRepository.save(payment);

            intent.fail();
            intentRepository.save(intent);

            log.warn("Payment failed: {} (order: {}, error: {})",
                    payment.getPaymentNumber(), intent.getOrderNumber(), pgResponse.errorCode());

            eventPublisher.publishPaymentFailed(PaymentFailedEvent.newBuilder()
                    .setPaymentNumber(payment.getPaymentNumber())
                    .setOrderNumber(intent.getOrderNumber())
                    .setUserId(userId)
                    .setAmount(payment.getAmount())
                    .setPaymentMethod(payment.getPaymentMethod().name())
                    .setFailureReason(pgResponse.errorCode() + ": " + pgResponse.message())
                    .setFailedAt(java.time.Instant.now())
                    .build());

            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_PROCESSING_FAILED);
        }

        return PaymentResponse.from(payment);
    }

    @Override
    public PaymentResponse getPayment(String userId, String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_USER_MISMATCH);
        }

        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String userId, String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_USER_MISMATCH);
        }

        payment.cancel("Cancelled by user");
        Payment saved = paymentRepository.save(payment);

        log.info("Payment cancelled: {} (user: {})", paymentNumber, userId);

        eventPublisher.publishPaymentCancelled(PaymentCancelledEvent.newBuilder()
                .setPaymentNumber(paymentNumber)
                .setOrderNumber(saved.getOrderNumber())
                .setUserId(userId)
                .setAmount(saved.getAmount())
                .setCancelReason("Cancelled by user")
                .setCancelledAt(java.time.Instant.now())
                .build());

        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return PaymentResponse.from(refundPaymentInternal(payment));
    }

    @Override
    @Transactional
    public void refundPaymentForCompensation(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElse(null);
        if (payment == null) {
            log.warn("No payment found for compensation: order={}", orderNumber);
            return;
        }
        if (!payment.getStatus().isRefundable()) {
            log.warn("Payment not refundable for compensation: order={}, status={}",
                    orderNumber, payment.getStatus());
            return;
        }

        refundPaymentInternal(payment);
    }

    private Payment refundPaymentInternal(Payment payment) {
        if (!payment.getStatus().isRefundable()) {
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_REFUND_FAILED);
        }

        PgResponse pgResponse = mockPGClient.refundPayment(
                payment.getPgTransactionId(),
                payment.getAmount()
        );

        if (pgResponse.success()) {
            payment.refund(pgResponse.transactionId());
            paymentRepository.save(payment);
            log.info("Payment refunded: {} (order: {})",
                    payment.getPaymentNumber(), payment.getOrderNumber());
        } else {
            log.error("Failed to refund payment: {} (error: {})",
                    payment.getPaymentNumber(), pgResponse.errorCode());
            throw new CustomBusinessException(PaymentErrorCode.PAYMENT_REFUND_FAILED);
        }

        return payment;
    }

}
