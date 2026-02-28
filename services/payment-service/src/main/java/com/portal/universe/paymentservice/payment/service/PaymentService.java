package com.portal.universe.paymentservice.payment.service;

import com.portal.universe.paymentservice.payment.dto.ConfirmPaymentRequest;
import com.portal.universe.paymentservice.payment.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse confirmPayment(String userId, String intentId, ConfirmPaymentRequest request);

    PaymentResponse getPayment(String userId, String paymentNumber);

    PaymentResponse cancelPayment(String userId, String paymentNumber);

    PaymentResponse refundPayment(String paymentNumber);

    void refundPaymentForCompensation(String orderNumber);
}
