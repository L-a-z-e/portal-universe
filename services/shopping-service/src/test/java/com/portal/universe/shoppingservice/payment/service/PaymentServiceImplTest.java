package com.portal.universe.shoppingservice.payment.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.service.OrderService;
import com.portal.universe.shoppingservice.payment.domain.Payment;
import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import com.portal.universe.shoppingservice.payment.domain.PaymentStatus;
import com.portal.universe.shoppingservice.payment.dto.PaymentResponse;
import com.portal.universe.shoppingservice.payment.dto.ProcessPaymentRequest;
import com.portal.universe.shoppingservice.payment.pg.MockPGClient;
import com.portal.universe.shoppingservice.payment.pg.PgResponse;
import com.portal.universe.shoppingservice.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private MockPGClient mockPGClient;

    @Mock
    private ShoppingEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order createOrder(String userId, String orderNumber, OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(null)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "totalAmount", BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(order, "discountAmount", BigDecimal.ZERO);
        ReflectionTestUtils.setField(order, "finalAmount", BigDecimal.valueOf(10000));
        return order;
    }

    private Payment createPayment(String userId, String orderNumber, PaymentStatus status) {
        Payment payment = Payment.builder()
                .orderId(1L)
                .orderNumber(orderNumber)
                .userId(userId)
                .amount(BigDecimal.valueOf(10000))
                .paymentMethod(PaymentMethod.CARD)
                .build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        ReflectionTestUtils.setField(payment, "status", status);
        return payment;
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("should_processPayment_when_valid")
        void should_processPayment_when_valid() {
            // given
            String userId = "user1";
            Order order = createOrder(userId, "ORD-001", OrderStatus.CONFIRMED);
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());

            Payment payment = createPayment(userId, "ORD-001", PaymentStatus.PENDING);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            PgResponse pgResponse = PgResponse.success("PG-TX123");
            when(mockPGClient.processPayment(anyString(), any(BigDecimal.class), any(PaymentMethod.class), anyString()))
                    .thenReturn(pgResponse);

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001", PaymentMethod.CARD, "1234-5678-9012-3456", "12/26", "123");

            // when
            PaymentResponse result = paymentService.processPayment(userId, request);

            // then
            assertThat(result).isNotNull();
            verify(orderService).completeOrderAfterPayment("ORD-001");
            verify(eventPublisher).publishPaymentCompleted(any());
        }

        @Test
        @DisplayName("should_throwException_when_orderNotFound")
        void should_throwException_when_orderNotFound() {
            // given
            when(orderRepository.findByOrderNumberWithItems("ORD-999")).thenReturn(Optional.empty());

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-999", PaymentMethod.CARD, "1234", "12/26", "123");

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_userMismatch")
        void should_throwException_when_userMismatch() {
            // given
            Order order = createOrder("user1", "ORD-001", OrderStatus.CONFIRMED);
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001", PaymentMethod.CARD, "1234", "12/26", "123");

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment("user2", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_alreadyPaid")
        void should_throwException_when_alreadyPaid() {
            // given
            Order order = createOrder("user1", "ORD-001", OrderStatus.PAID);
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001", PaymentMethod.CARD, "1234", "12/26", "123");

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_pgFails")
        void should_throwException_when_pgFails() {
            // given
            String userId = "user1";
            Order order = createOrder(userId, "ORD-001", OrderStatus.CONFIRMED);
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());

            Payment payment = createPayment(userId, "ORD-001", PaymentStatus.PENDING);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            PgResponse pgResponse = PgResponse.failure("CARD_DECLINED", "Card was declined");
            when(mockPGClient.processPayment(anyString(), any(BigDecimal.class), any(PaymentMethod.class), anyString()))
                    .thenReturn(pgResponse);

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001", PaymentMethod.CARD, "1234", "12/26", "123");

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomBusinessException.class);
            verify(eventPublisher).publishPaymentFailed(any());
        }

        @Test
        @DisplayName("should_publishEvent_when_paymentCompleted")
        void should_publishEvent_when_paymentCompleted() {
            // given
            String userId = "user1";
            Order order = createOrder(userId, "ORD-001", OrderStatus.CONFIRMED);
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());

            Payment payment = createPayment(userId, "ORD-001", PaymentStatus.PENDING);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            PgResponse pgResponse = PgResponse.success("PG-TX999");
            when(mockPGClient.processPayment(anyString(), any(BigDecimal.class), any(PaymentMethod.class), anyString()))
                    .thenReturn(pgResponse);

            ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001", PaymentMethod.CARD, "1234", "12/26", "123");

            // when
            paymentService.processPayment(userId, request);

            // then
            verify(eventPublisher).publishPaymentCompleted(any());
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should_returnPayment_when_found")
        void should_returnPayment_when_found() {
            // given
            Payment payment = createPayment("user1", "ORD-001", PaymentStatus.COMPLETED);
            when(paymentRepository.findByPaymentNumber("PAY-001")).thenReturn(Optional.of(payment));

            // when
            PaymentResponse result = paymentService.getPayment("user1", "PAY-001");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(paymentRepository.findByPaymentNumber("PAY-999")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getPayment("user1", "PAY-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("should_cancelPayment_when_valid")
        void should_cancelPayment_when_valid() {
            // given
            Payment payment = createPayment("user1", "ORD-001", PaymentStatus.PENDING);
            when(paymentRepository.findByPaymentNumber("PAY-001")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            // when
            PaymentResponse result = paymentService.cancelPayment("user1", "PAY-001");

            // then
            assertThat(result).isNotNull();
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("should_throwException_when_cannotCancel")
        void should_throwException_when_cannotCancel() {
            // given
            Payment payment = createPayment("user1", "ORD-001", PaymentStatus.COMPLETED);
            when(paymentRepository.findByPaymentNumber("PAY-001")).thenReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment("user1", "PAY-001"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("should_refundPayment_when_valid")
        void should_refundPayment_when_valid() {
            // given
            Payment payment = createPayment("user1", "ORD-001", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(payment, "pgTransactionId", "PG-TX123");
            when(paymentRepository.findByPaymentNumber("PAY-001")).thenReturn(Optional.of(payment));

            PgResponse pgResponse = PgResponse.success("RF-PG-TX456");
            when(mockPGClient.refundPayment("PG-TX123", BigDecimal.valueOf(10000))).thenReturn(pgResponse);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            Order order = createOrder("user1", "ORD-001", OrderStatus.PAID);
            when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            PaymentResponse result = paymentService.refundPayment("PAY-001");

            // then
            assertThat(result).isNotNull();
            verify(mockPGClient).refundPayment("PG-TX123", BigDecimal.valueOf(10000));
        }
    }
}
