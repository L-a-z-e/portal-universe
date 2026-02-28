package com.portal.universe.paymentservice.intent.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.paymentservice.common.exception.PaymentErrorCode;
import com.portal.universe.paymentservice.intent.domain.PaymentIntent;
import com.portal.universe.paymentservice.intent.dto.CreateIntentRequest;
import com.portal.universe.paymentservice.intent.dto.IntentResponse;
import com.portal.universe.paymentservice.intent.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntentServiceImpl implements IntentService {

    private final PaymentIntentRepository intentRepository;

    @Value("${app.intent.expiry-minutes:30}")
    private long expiryMinutes;

    @Override
    @Transactional
    public IntentResponse createIntent(CreateIntentRequest request) {
        // 동일 주문에 대한 기존 Intent 확인
        intentRepository.findByOrderNumber(request.orderNumber())
                .ifPresent(existing -> {
                    if (existing.getStatus().isPayable()) {
                        throw new CustomBusinessException(PaymentErrorCode.INTENT_ALREADY_EXISTS);
                    }
                });

        PaymentIntent intent = PaymentIntent.builder()
                .orderNumber(request.orderNumber())
                .userId(request.userId())
                .amount(request.amount())
                .metadata(request.metadata())
                .expiryMinutes(expiryMinutes)
                .build();

        intent = intentRepository.save(intent);

        log.info("Payment intent created: {} (order: {}, amount: {})",
                intent.getIntentId(), request.orderNumber(), request.amount());

        return IntentResponse.from(intent);
    }

    @Override
    public IntentResponse getIntent(String intentId) {
        PaymentIntent intent = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.INTENT_NOT_FOUND));
        return IntentResponse.from(intent);
    }

    @Override
    @Transactional
    public void cancelIntent(String intentId, String userId) {
        PaymentIntent intent = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new CustomBusinessException(PaymentErrorCode.INTENT_NOT_FOUND));

        intent.validateOwnership(userId);
        intent.cancel();
        intentRepository.save(intent);

        log.info("Payment intent cancelled: {} (order: {})", intentId, intent.getOrderNumber());
    }
}
