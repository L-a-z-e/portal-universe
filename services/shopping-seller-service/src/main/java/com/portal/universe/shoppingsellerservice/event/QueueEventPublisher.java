package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.QueueActivatedEvent;
import com.portal.universe.event.seller.QueueDeactivatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivated(QueueActivatedEvent event) {
        String key = event.getEventType() + ":" + event.getEventId();
        avroKafkaTemplate.send(SellerTopics.QUEUE_ACTIVATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish QueueActivatedEvent: eventType={}, eventId={}",
                                event.getEventType(), event.getEventId(), ex);
                    } else {
                        log.info("Published QueueActivatedEvent: eventType={}, eventId={}, offset={}",
                                event.getEventType(), event.getEventId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeactivated(QueueDeactivatedEvent event) {
        String key = event.getEventType() + ":" + event.getEventId();
        avroKafkaTemplate.send(SellerTopics.QUEUE_DEACTIVATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish QueueDeactivatedEvent: eventType={}, eventId={}",
                                event.getEventType(), event.getEventId(), ex);
                    } else {
                        log.info("Published QueueDeactivatedEvent: eventType={}, eventId={}, offset={}",
                                event.getEventType(), event.getEventId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
