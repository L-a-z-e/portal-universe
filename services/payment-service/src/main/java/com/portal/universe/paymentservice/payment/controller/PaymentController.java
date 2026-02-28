package com.portal.universe.paymentservice.payment.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import com.portal.universe.paymentservice.intent.dto.IntentResponse;
import com.portal.universe.paymentservice.intent.service.IntentService;
import com.portal.universe.paymentservice.payment.dto.ConfirmPaymentRequest;
import com.portal.universe.paymentservice.payment.dto.PaymentResponse;
import com.portal.universe.paymentservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final IntentService intentService;

    /**
     * Intent 조회 (프론트엔드용)
     */
    @GetMapping("/intents/{intentId}")
    public ApiResponse<IntentResponse> getIntent(@PathVariable String intentId) {
        return ApiResponse.success(intentService.getIntent(intentId));
    }

    /**
     * 결제 확인 (Intent 기반)
     */
    @PostMapping("/intents/{intentId}/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(
            @RequestHeader(AuthConstants.Headers.USER_ID) String userId,
            @PathVariable String intentId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ApiResponse.success(paymentService.confirmPayment(userId, intentId, request));
    }

    /**
     * 결제 조회
     */
    @GetMapping("/payments/{paymentNumber}")
    public ApiResponse<PaymentResponse> getPayment(
            @RequestHeader(AuthConstants.Headers.USER_ID) String userId,
            @PathVariable String paymentNumber) {
        return ApiResponse.success(paymentService.getPayment(userId, paymentNumber));
    }

    /**
     * 결제 취소
     */
    @PostMapping("/payments/{paymentNumber}/cancel")
    public ApiResponse<PaymentResponse> cancelPayment(
            @RequestHeader(AuthConstants.Headers.USER_ID) String userId,
            @PathVariable String paymentNumber) {
        return ApiResponse.success(paymentService.cancelPayment(userId, paymentNumber));
    }

    /**
     * 관리자 환불
     */
    @PostMapping("/payments/{paymentNumber}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentResponse> refundPayment(@PathVariable String paymentNumber) {
        return ApiResponse.success(paymentService.refundPayment(paymentNumber));
    }
}
