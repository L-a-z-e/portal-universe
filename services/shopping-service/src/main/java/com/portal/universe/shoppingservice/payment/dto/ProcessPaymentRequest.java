package com.portal.universe.shoppingservice.payment.dto;

import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 처리 요청 DTO입니다.
 */
public record ProcessPaymentRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        // 카드 정보 (실제로는 암호화하여 전송)
        String cardNumber,
        String cardExpiry,
        String cardCvv
) {
}
