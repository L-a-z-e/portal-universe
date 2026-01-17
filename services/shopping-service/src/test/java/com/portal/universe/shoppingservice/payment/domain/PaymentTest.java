package com.portal.universe.shoppingservice.payment.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Payment 엔티티 단위 테스트입니다.
 */
class PaymentTest {

    @Nested
    @DisplayName("결제 생성 테스트")
    class CreatePaymentTest {

        @Test
        @DisplayName("새 결제 생성 시 PENDING 상태로 초기화되고 결제번호가 생성된다")
        void createNewPayment() {
            // given
            Long orderId = 1L;
            String orderNumber = "ORD-20240101-ABCD1234";
            String userId = "user-123";
            BigDecimal amount = new BigDecimal("50000");
            PaymentMethod method = PaymentMethod.CARD;

            // when
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod(method)
                    .build();

            // then
            assertThat(payment.getOrderId()).isEqualTo(orderId);
            assertThat(payment.getOrderNumber()).isEqualTo(orderNumber);
            assertThat(payment.getUserId()).isEqualTo(userId);
            assertThat(payment.getAmount()).isEqualByComparingTo(amount);
            assertThat(payment.getPaymentMethod()).isEqualTo(method);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getPaymentNumber()).startsWith("PAY-");
            assertThat(payment.getPaymentNumber()).hasSize(12); // PAY-XXXXXXXX = 12자
        }
    }

    @Nested
    @DisplayName("결제 처리 상태 전이 테스트")
    class PaymentStatusTransitionTest {

        @Test
        @DisplayName("PENDING 결제를 처리 시작하면 PROCESSING 상태로 변경된다")
        void startProcessingFromPending() {
            // given
            Payment payment = createPayment();

            // when
            payment.startProcessing();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("PROCESSING 결제를 완료하면 COMPLETED 상태로 변경된다")
        void completeFromProcessing() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();
            String pgTransactionId = "PG-TRANSACTION-123";
            String pgResponse = "{\"status\": \"success\"}";

            // when
            payment.complete(pgTransactionId, pgResponse);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPgTransactionId()).isEqualTo(pgTransactionId);
            assertThat(payment.getPgResponse()).isEqualTo(pgResponse);
            assertThat(payment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING이 아닌 결제를 처리 시작하면 예외가 발생한다")
        void startProcessingFromNonPendingThrowsException() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();

            // when & then
            assertThatThrownBy(payment::startProcessing)
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("PROCESSING이 아닌 결제를 완료하면 예외가 발생한다")
        void completeFromNonProcessingThrowsException() {
            // given
            Payment payment = createPayment(); // PENDING

            // when & then
            assertThatThrownBy(() -> payment.complete("tx-123", "{}"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("결제 실패 테스트")
    class PaymentFailureTest {

        @Test
        @DisplayName("PENDING 결제를 실패 처리할 수 있다")
        void failPendingPayment() {
            // given
            Payment payment = createPayment();
            String failureReason = "카드 한도 초과";
            String pgResponse = "{\"error\": \"limit_exceeded\"}";

            // when
            payment.fail(failureReason, pgResponse);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
            assertThat(payment.getPgResponse()).isEqualTo(pgResponse);
        }

        @Test
        @DisplayName("PROCESSING 결제를 실패 처리할 수 있다")
        void failProcessingPayment() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();

            // when
            payment.fail("네트워크 오류", "{}");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("COMPLETED 결제는 실패 처리할 수 없다")
        void failCompletedPaymentThrowsException() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();
            payment.complete("tx-123", "{}");

            // when & then
            assertThatThrownBy(() -> payment.fail("오류", "{}"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("결제 취소 테스트")
    class PaymentCancelTest {

        @Test
        @DisplayName("PENDING 결제를 취소할 수 있다")
        void cancelPendingPayment() {
            // given
            Payment payment = createPayment();

            // when
            payment.cancel("고객 요청");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getFailureReason()).isEqualTo("고객 요청");
        }

        @Test
        @DisplayName("PROCESSING 결제를 취소할 수 있다")
        void cancelProcessingPayment() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();

            // when
            payment.cancel("시스템 오류");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("COMPLETED 결제는 취소할 수 없다")
        void cancelCompletedPaymentThrowsException() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();
            payment.complete("tx-123", "{}");

            // when & then
            assertThatThrownBy(() -> payment.cancel("취소 요청"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }
    }

    @Nested
    @DisplayName("결제 환불 테스트")
    class PaymentRefundTest {

        @Test
        @DisplayName("COMPLETED 결제를 환불할 수 있다")
        void refundCompletedPayment() {
            // given
            Payment payment = createPayment();
            payment.startProcessing();
            payment.complete("tx-123", "{}");
            String refundTxId = "RF-tx-456";

            // when
            payment.refund(refundTxId);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getPgTransactionId()).isEqualTo(refundTxId);
            assertThat(payment.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING 결제는 환불할 수 없다")
        void refundPendingPaymentThrowsException() {
            // given
            Payment payment = createPayment();

            // when & then
            assertThatThrownBy(() -> payment.refund("RF-123"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }

        @Test
        @DisplayName("FAILED 결제는 환불할 수 없다")
        void refundFailedPaymentThrowsException() {
            // given
            Payment payment = createPayment();
            payment.fail("오류", "{}");

            // when & then
            assertThatThrownBy(() -> payment.refund("RF-123"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }
    }

    private Payment createPayment() {
        return Payment.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-ABCD1234")
                .userId("test-user")
                .amount(new BigDecimal("10000"))
                .paymentMethod(PaymentMethod.CARD)
                .build();
    }
}
