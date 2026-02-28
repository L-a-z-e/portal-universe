package com.portal.universe.paymentservice.payment.controller;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import com.portal.universe.paymentservice.common.exception.PaymentErrorCode;
import com.portal.universe.paymentservice.intent.dto.CreateIntentRequest;
import com.portal.universe.paymentservice.intent.dto.IntentResponse;
import com.portal.universe.paymentservice.intent.service.IntentService;
import com.portal.universe.paymentservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final IntentService intentService;
    private final PaymentService paymentService;

    @Value("${app.internal.token}")
    private String expectedToken;

    /**
     * Intent 생성 (shopping-service → payment-service)
     */
    @PostMapping("/intents")
    public ApiResponse<IntentResponse> createIntent(
            @RequestHeader(AuthConstants.Headers.INTERNAL_TOKEN) String token,
            @Valid @RequestBody CreateIntentRequest request) {
        validateInternalToken(token);
        return ApiResponse.success(intentService.createIntent(request));
    }

    /**
     * Saga 보상 환불 (shopping-service → payment-service)
     */
    @PostMapping("/refund/{orderNumber}")
    public ApiResponse<Void> refundForCompensation(
            @RequestHeader(AuthConstants.Headers.INTERNAL_TOKEN) String token,
            @PathVariable String orderNumber) {
        validateInternalToken(token);
        paymentService.refundPaymentForCompensation(orderNumber);
        return ApiResponse.success(null);
    }

    private void validateInternalToken(String token) {
        if (!expectedToken.equals(token)) {
            throw new CustomBusinessException(PaymentErrorCode.INTERNAL_TOKEN_INVALID);
        }
    }
}
