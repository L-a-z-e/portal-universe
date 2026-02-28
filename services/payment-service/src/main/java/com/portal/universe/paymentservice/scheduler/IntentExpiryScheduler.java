package com.portal.universe.paymentservice.scheduler;

import com.portal.universe.paymentservice.intent.domain.PaymentIntent;
import com.portal.universe.paymentservice.intent.domain.PaymentIntentStatus;
import com.portal.universe.paymentservice.intent.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentExpiryScheduler {

    private final PaymentIntentRepository intentRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOverdueIntents() {
        List<PaymentIntent> expired = intentRepository.findExpiredIntents(
                PaymentIntentStatus.REQUIRES_PAYMENT, Instant.now());

        if (expired.isEmpty()) return;

        for (PaymentIntent intent : expired) {
            intent.expire();
            log.info("Payment intent expired: {} (order: {})", intent.getIntentId(), intent.getOrderNumber());
        }

        intentRepository.saveAll(expired);
        log.info("Expired {} payment intents", expired.size());
    }
}
