package com.portal.universe.shoppingservice.payment.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.service.OrderService;
import com.portal.universe.shoppingservice.payment.domain.Payment;
import com.portal.universe.shoppingservice.payment.dto.PaymentResponse;
import com.portal.universe.shoppingservice.payment.dto.ProcessPaymentRequest;
import com.portal.universe.shoppingservice.payment.pg.MockPGClient;
import com.portal.universe.shoppingservice.payment.pg.PgResponse;
import com.portal.universe.shoppingservice.payment.repository.PaymentRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 결제 관리 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final MockPGClient mockPGClient;
    private final ShoppingEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentResponse processPayment(String userId, ProcessPaymentRequest request) {
        // 1. 주문 조회 및 검증
        Order order = orderRepository.findByOrderNumberWithItems(request.orderNumber())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // 주문자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_USER_MISMATCH);
        }

        // 주문 상태 확인 (CONFIRMED 상태여야 결제 가능)
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            if (order.getStatus() == OrderStatus.PAID) {
                throw new CustomBusinessException(ShoppingErrorCode.ORDER_ALREADY_PAID);
            }
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }

        // 이미 결제가 존재하는지 확인
        paymentRepository.findByOrderNumber(request.orderNumber())
                .ifPresent(p -> {
                    throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
                });

        // 2. 결제 생성 (쿠폰 적용된 최종 금액으로 결제)
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(userId)
                .amount(order.getFinalAmount())
                .paymentMethod(request.paymentMethod())
                .build();

        payment = paymentRepository.save(payment);

        // 3. 결제 처리 시작
        payment.startProcessing();

        // 4. PG사 결제 요청
        PgResponse pgResponse = mockPGClient.processPayment(
                payment.getPaymentNumber(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                request.cardNumber()
        );

        // 5. 결제 결과 처리
        if (pgResponse.success()) {
            payment.complete(pgResponse.transactionId(), pgResponse.rawResponse());
            paymentRepository.save(payment);

            // 6. 주문 완료 처리 (Saga 나머지 단계)
            try {
                orderService.completeOrderAfterPayment(order.getOrderNumber());
            } catch (Exception e) {
                // 주문 완료 실패 시 결제 환불
                log.error("Failed to complete order after payment, initiating refund: {}", e.getMessage());
                refundPaymentInternal(payment);
                throw new CustomBusinessException(ShoppingErrorCode.ORDER_CREATION_FAILED);
            }

            log.info("Payment completed successfully: {} (order: {}, amount: {})",
                    payment.getPaymentNumber(), order.getOrderNumber(), payment.getAmount());

            // 결제 완료 이벤트 발행
            eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    payment.getPaymentNumber(),
                    order.getOrderNumber(),
                    userId,
                    payment.getAmount(),
                    payment.getPaymentMethod().name(),
                    payment.getPgTransactionId(),
                    LocalDateTime.now()
            ));
        } else {
            payment.fail(pgResponse.errorCode() + ": " + pgResponse.message(), pgResponse.rawResponse());
            paymentRepository.save(payment);

            log.warn("Payment failed: {} (order: {}, error: {})",
                    payment.getPaymentNumber(), order.getOrderNumber(), pgResponse.errorCode());

            // 결제 실패 이벤트 발행
            eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                    payment.getPaymentNumber(),
                    order.getOrderNumber(),
                    userId,
                    payment.getAmount(),
                    payment.getPaymentMethod().name(),
                    pgResponse.errorCode() + ": " + pgResponse.message(),
                    LocalDateTime.now()
            ));

            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_PROCESSING_FAILED);
        }

        return PaymentResponse.from(payment);
    }

    @Override
    public PaymentResponse getPayment(String userId, String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제인지 확인
        if (!payment.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_USER_MISMATCH);
        }

        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String userId, String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제인지 확인
        if (!payment.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_USER_MISMATCH);
        }

        // 취소 가능 여부 확인
        if (!payment.getStatus().isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }

        payment.cancel("Cancelled by user");
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled: {} (user: {})", paymentNumber, userId);
        return PaymentResponse.from(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PAYMENT_NOT_FOUND));

        return PaymentResponse.from(refundPaymentInternal(payment));
    }

    /**
     * 결제 환불을 수행합니다.
     */
    private Payment refundPaymentInternal(Payment payment) {
        if (!payment.getStatus().isRefundable()) {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }

        // PG사 환불 요청
        PgResponse pgResponse = mockPGClient.refundPayment(
                payment.getPgTransactionId(),
                payment.getAmount()
        );

        if (pgResponse.success()) {
            payment.refund(pgResponse.transactionId());
            paymentRepository.save(payment);

            // 주문 환불 처리
            Order order = orderRepository.findByOrderNumber(payment.getOrderNumber())
                    .orElse(null);
            if (order != null) {
                order.refund();
                orderRepository.save(order);
            }

            log.info("Payment refunded: {} (order: {})", payment.getPaymentNumber(), payment.getOrderNumber());
        } else {
            log.error("Failed to refund payment: {} (error: {})", payment.getPaymentNumber(), pgResponse.errorCode());
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }

        return payment;
    }
}
