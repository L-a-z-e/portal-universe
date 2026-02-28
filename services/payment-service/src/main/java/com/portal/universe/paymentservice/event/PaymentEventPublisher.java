package com.portal.universe.paymentservice.event;

import com.portal.universe.event.shopping.PaymentCancelledEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.PaymentFailedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.paymentservice.event.outbox.OutboxEvent;
import com.portal.universe.paymentservice.event.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        saveOutbox(ShoppingTopics.PAYMENT_COMPLETED, event.getPaymentNumber().toString(), event);
    }

    public void publishPaymentCancelled(PaymentCancelledEvent event) {
        saveOutbox(ShoppingTopics.PAYMENT_CANCELLED, event.getPaymentNumber().toString(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        saveOutbox(ShoppingTopics.PAYMENT_FAILED, event.getPaymentNumber().toString(), event);
    }

    private void saveOutbox(String topic, String key, SpecificRecord event) {
        outboxEventRepository.save(OutboxEvent.create(topic, key, event));
    }
}
