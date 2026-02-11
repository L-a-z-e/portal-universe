package com.portal.universe.shoppingservice.payment.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.payment.dto.PaymentResponse;
import com.portal.universe.shoppingservice.payment.dto.ProcessPaymentRequest;
import com.portal.universe.shoppingservice.payment.service.PaymentService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 API를 제공하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제를 처리합니다.
     *
     * @param request 결제 요청
     * @param user 인증된 사용자 정보
     * @return 결제 결과
     */
    @PostMapping
    public ApiResponse<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(paymentService.processPayment(user.uuid(), request));
    }

    /**
     * 결제 번호로 결제를 조회합니다.
     *
     * @param paymentNumber 결제 번호
     * @param user 인증된 사용자 정보
     * @return 결제 정보
     */
    @GetMapping("/{paymentNumber}")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable String paymentNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(paymentService.getPayment(user.uuid(), paymentNumber));
    }

    /**
     * 결제를 취소합니다.
     *
     * @param paymentNumber 결제 번호
     * @param user 인증된 사용자 정보
     * @return 취소된 결제 정보
     */
    @PostMapping("/{paymentNumber}/cancel")
    public ApiResponse<PaymentResponse> cancelPayment(
            @PathVariable String paymentNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(paymentService.cancelPayment(user.uuid(), paymentNumber));
    }

    /**
     * 결제를 환불합니다 (관리자 전용).
     *
     * @param paymentNumber 결제 번호
     * @param user 인증된 관리자 정보
     * @return 환불된 결제 정보
     */
    @PostMapping("/{paymentNumber}/refund")
    public ApiResponse<PaymentResponse> refundPayment(
            @PathVariable String paymentNumber,
            @CurrentUser AuthUser user) {
        log.info("Payment refund requested: paymentNumber={}, adminId={}", paymentNumber, user.uuid());
        return ApiResponse.success(paymentService.refundPayment(paymentNumber));
    }
}
