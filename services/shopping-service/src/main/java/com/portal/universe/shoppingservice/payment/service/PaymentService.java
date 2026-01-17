package com.portal.universe.shoppingservice.payment.service;

import com.portal.universe.shoppingservice.payment.dto.PaymentResponse;
import com.portal.universe.shoppingservice.payment.dto.ProcessPaymentRequest;

/**
 * 결제 관리 서비스 인터페이스입니다.
 */
public interface PaymentService {

    /**
     * 결제를 처리합니다.
     *
     * @param userId 사용자 ID
     * @param request 결제 요청
     * @return 결제 결과
     */
    PaymentResponse processPayment(String userId, ProcessPaymentRequest request);

    /**
     * 결제 번호로 결제를 조회합니다.
     *
     * @param userId 사용자 ID (권한 확인용)
     * @param paymentNumber 결제 번호
     * @return 결제 정보
     */
    PaymentResponse getPayment(String userId, String paymentNumber);

    /**
     * 결제를 취소합니다.
     *
     * @param userId 사용자 ID
     * @param paymentNumber 결제 번호
     * @return 취소된 결제 정보
     */
    PaymentResponse cancelPayment(String userId, String paymentNumber);

    /**
     * 결제를 환불합니다 (관리자 전용).
     *
     * @param paymentNumber 결제 번호
     * @return 환불된 결제 정보
     */
    PaymentResponse refundPayment(String paymentNumber);
}
