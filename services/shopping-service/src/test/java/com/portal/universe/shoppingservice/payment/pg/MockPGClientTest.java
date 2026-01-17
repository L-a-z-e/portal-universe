package com.portal.universe.shoppingservice.payment.pg;

import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockPGClient 단위 테스트입니다.
 */
class MockPGClientTest {

    private MockPGClient mockPGClient;

    @BeforeEach
    void setUp() {
        mockPGClient = new MockPGClient();
    }

    @Test
    @DisplayName("결제 처리는 성공 또는 실패 응답을 반환한다")
    void processPaymentReturnsResponse() {
        // given
        String paymentNumber = "PAY-TEST0001";
        BigDecimal amount = new BigDecimal("50000");
        PaymentMethod method = PaymentMethod.CARD;
        String cardNumber = "****-****-****-1234";

        // when
        PgResponse response = mockPGClient.processPayment(paymentNumber, amount, method, cardNumber);

        // then
        assertThat(response).isNotNull();
        // 응답은 성공 또는 실패 중 하나
        if (response.success()) {
            assertThat(response.transactionId()).isNotBlank();
            assertThat(response.transactionId()).startsWith("PG-");
            assertThat(response.errorCode()).isNull();
            assertThat(response.message()).isNotNull();
        } else {
            assertThat(response.transactionId()).isNull();
            assertThat(response.errorCode()).isNotBlank();
            assertThat(response.message()).isNotBlank();
        }
    }

    @Test
    @DisplayName("결제 취소는 존재하지 않는 거래 ID로 요청 시 실패한다")
    void cancelNonExistentTransactionFails() {
        // given
        String nonExistentTxId = "PG-NONEXISTENT";

        // when
        PgResponse response = mockPGClient.cancelPayment(nonExistentTxId);

        // then
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("TX_NOT_FOUND");
    }

    @Test
    @DisplayName("환불은 존재하지 않는 거래 ID로 요청 시 실패한다")
    void refundNonExistentTransactionFails() {
        // given
        String nonExistentTxId = "PG-NONEXISTENT";
        BigDecimal refundAmount = new BigDecimal("10000");

        // when
        PgResponse response = mockPGClient.refundPayment(nonExistentTxId, refundAmount);

        // then
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("TX_NOT_FOUND");
    }

    @Test
    @DisplayName("성공한 결제를 취소할 수 있다")
    void cancelSuccessfulPayment() {
        // given - 먼저 성공적인 결제를 수행
        String paymentNumber = "PAY-TEST0002";
        BigDecimal amount = new BigDecimal("50000");
        PaymentMethod method = PaymentMethod.CARD;

        // 결제가 성공할 때까지 재시도 (90% 성공률이므로 몇 번 안에 성공함)
        PgResponse paymentResponse = null;
        for (int i = 0; i < 20; i++) {
            paymentResponse = mockPGClient.processPayment(paymentNumber + i, amount, method, "****");
            if (paymentResponse.success()) {
                break;
            }
        }

        // 테스트를 위해 성공한 결제가 있어야 함
        assertThat(paymentResponse.success()).isTrue();
        String transactionId = paymentResponse.transactionId();

        // when
        PgResponse cancelResponse = mockPGClient.cancelPayment(transactionId);

        // then
        assertThat(cancelResponse.success()).isTrue();
    }

    @Test
    @DisplayName("성공한 결제를 환불할 수 있다")
    void refundSuccessfulPayment() {
        // given - 먼저 성공적인 결제를 수행
        String paymentNumber = "PAY-TEST0003";
        BigDecimal amount = new BigDecimal("50000");
        PaymentMethod method = PaymentMethod.CARD;

        // 결제가 성공할 때까지 재시도
        PgResponse paymentResponse = null;
        for (int i = 0; i < 20; i++) {
            paymentResponse = mockPGClient.processPayment(paymentNumber + i, amount, method, "****");
            if (paymentResponse.success()) {
                break;
            }
        }

        assertThat(paymentResponse.success()).isTrue();
        String transactionId = paymentResponse.transactionId();

        // when - 부분 환불
        BigDecimal refundAmount = new BigDecimal("30000");
        PgResponse refundResponse = mockPGClient.refundPayment(transactionId, refundAmount);

        // then
        assertThat(refundResponse.success()).isTrue();
        assertThat(refundResponse.transactionId()).startsWith("RF-");
    }

    @Test
    @DisplayName("환불 금액이 원래 결제 금액을 초과하면 실패한다")
    void refundExceedingAmountFails() {
        // given - 먼저 성공적인 결제를 수행
        String paymentNumber = "PAY-TEST0004";
        BigDecimal amount = new BigDecimal("50000");
        PaymentMethod method = PaymentMethod.CARD;

        PgResponse paymentResponse = null;
        for (int i = 0; i < 20; i++) {
            paymentResponse = mockPGClient.processPayment(paymentNumber + i, amount, method, "****");
            if (paymentResponse.success()) {
                break;
            }
        }

        assertThat(paymentResponse.success()).isTrue();
        String transactionId = paymentResponse.transactionId();

        // when - 원래 금액보다 큰 금액으로 환불 시도
        BigDecimal excessiveRefundAmount = new BigDecimal("100000");
        PgResponse refundResponse = mockPGClient.refundPayment(transactionId, excessiveRefundAmount);

        // then
        assertThat(refundResponse.success()).isFalse();
        assertThat(refundResponse.errorCode()).isEqualTo("INVALID_AMOUNT");
    }
}
