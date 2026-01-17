package com.portal.universe.shoppingservice.payment.pg;

import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock PG (Payment Gateway) 클라이언트입니다.
 * 실제 PG사 연동 대신 시뮬레이션을 제공합니다.
 *
 * 특징:
 * - 90% 성공률 시뮬레이션
 * - 랜덤 실패 시나리오
 * - 거래 내역 메모리 저장
 */
@Slf4j
@Component
public class MockPGClient {

    private static final double SUCCESS_RATE = 0.90; // 90% 성공률
    private final Random random = new Random();

    // Mock 거래 저장소 (실제로는 PG사 서버에 저장됨)
    private final Map<String, MockTransaction> transactions = new ConcurrentHashMap<>();

    /**
     * 결제를 처리합니다.
     *
     * @param paymentNumber 결제 번호
     * @param amount 결제 금액
     * @param method 결제 수단
     * @param cardNumber 카드번호 (마스킹된 형태)
     * @return PG 응답
     */
    public PgResponse processPayment(String paymentNumber, BigDecimal amount, PaymentMethod method, String cardNumber) {
        log.info("MockPG: Processing payment {} - amount: {}, method: {}", paymentNumber, amount, method);

        // 결제 처리 시뮬레이션 (실제로는 네트워크 지연이 있음)
        simulateProcessingDelay();

        // 랜덤 성공/실패 결정
        if (shouldSucceed()) {
            String transactionId = generateTransactionId();

            // 거래 내역 저장
            transactions.put(transactionId, new MockTransaction(
                    transactionId,
                    paymentNumber,
                    amount,
                    method,
                    "COMPLETED"
            ));

            log.info("MockPG: Payment {} succeeded with txId: {}", paymentNumber, transactionId);
            return PgResponse.success(transactionId);
        } else {
            String errorCode = generateRandomErrorCode();
            String errorMessage = getErrorMessage(errorCode);

            log.warn("MockPG: Payment {} failed - error: {} ({})", paymentNumber, errorCode, errorMessage);
            return PgResponse.failure(errorCode, errorMessage);
        }
    }

    /**
     * 결제를 취소합니다.
     *
     * @param pgTransactionId PG 거래 ID
     * @return PG 응답
     */
    public PgResponse cancelPayment(String pgTransactionId) {
        log.info("MockPG: Cancelling payment - txId: {}", pgTransactionId);

        MockTransaction transaction = transactions.get(pgTransactionId);
        if (transaction == null) {
            return PgResponse.failure("TX_NOT_FOUND", "Transaction not found");
        }

        if (!"COMPLETED".equals(transaction.status())) {
            return PgResponse.failure("INVALID_STATUS", "Cannot cancel transaction in " + transaction.status() + " status");
        }

        // 취소 처리
        transactions.put(pgTransactionId, new MockTransaction(
                transaction.transactionId(),
                transaction.paymentNumber(),
                transaction.amount(),
                transaction.method(),
                "CANCELLED"
        ));

        log.info("MockPG: Payment cancelled - txId: {}", pgTransactionId);
        return PgResponse.success(pgTransactionId);
    }

    /**
     * 결제를 환불합니다.
     *
     * @param pgTransactionId PG 거래 ID
     * @param refundAmount 환불 금액
     * @return PG 응답
     */
    public PgResponse refundPayment(String pgTransactionId, BigDecimal refundAmount) {
        log.info("MockPG: Refunding payment - txId: {}, amount: {}", pgTransactionId, refundAmount);

        MockTransaction transaction = transactions.get(pgTransactionId);
        if (transaction == null) {
            return PgResponse.failure("TX_NOT_FOUND", "Transaction not found");
        }

        if (!"COMPLETED".equals(transaction.status())) {
            return PgResponse.failure("INVALID_STATUS", "Cannot refund transaction in " + transaction.status() + " status");
        }

        if (transaction.amount().compareTo(refundAmount) < 0) {
            return PgResponse.failure("INVALID_AMOUNT", "Refund amount exceeds original payment amount");
        }

        // 환불 처리
        String refundTxId = "RF-" + generateTransactionId();
        transactions.put(pgTransactionId, new MockTransaction(
                transaction.transactionId(),
                transaction.paymentNumber(),
                transaction.amount(),
                transaction.method(),
                "REFUNDED"
        ));

        log.info("MockPG: Payment refunded - original txId: {}, refund txId: {}", pgTransactionId, refundTxId);
        return PgResponse.success(refundTxId);
    }

    /**
     * 성공 여부를 결정합니다.
     */
    private boolean shouldSucceed() {
        return random.nextDouble() < SUCCESS_RATE;
    }

    /**
     * 처리 지연을 시뮬레이션합니다.
     */
    private void simulateProcessingDelay() {
        try {
            // 100ms ~ 500ms 랜덤 지연
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * PG 거래 ID를 생성합니다.
     */
    private String generateTransactionId() {
        return "PG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * 랜덤 에러 코드를 생성합니다.
     */
    private String generateRandomErrorCode() {
        String[] errorCodes = {
                "CARD_DECLINED",
                "INSUFFICIENT_FUNDS",
                "INVALID_CARD",
                "EXPIRED_CARD",
                "NETWORK_ERROR",
                "TIMEOUT",
                "LIMIT_EXCEEDED"
        };
        return errorCodes[random.nextInt(errorCodes.length)];
    }

    /**
     * 에러 코드에 해당하는 메시지를 반환합니다.
     */
    private String getErrorMessage(String errorCode) {
        return switch (errorCode) {
            case "CARD_DECLINED" -> "Card was declined by the issuer";
            case "INSUFFICIENT_FUNDS" -> "Insufficient funds in the account";
            case "INVALID_CARD" -> "Invalid card number";
            case "EXPIRED_CARD" -> "Card has expired";
            case "NETWORK_ERROR" -> "Network communication error";
            case "TIMEOUT" -> "Request timed out";
            case "LIMIT_EXCEEDED" -> "Transaction limit exceeded";
            default -> "Unknown error occurred";
        };
    }

    /**
     * Mock 거래 레코드
     */
    private record MockTransaction(
            String transactionId,
            String paymentNumber,
            BigDecimal amount,
            PaymentMethod method,
            String status
    ) {}
}
