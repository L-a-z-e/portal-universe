package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.event.seller.TimeDealCancelledEvent;
import com.portal.universe.event.seller.TimeDealCreatedEvent;
import com.portal.universe.event.seller.TimeDealUpdatedEvent;
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
public class TimeDealEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreated(TimeDealCreatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.TIMEDEAL_CREATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TimeDealCreatedEvent: timeDealId={}, sellerId={}",
                                event.getTimeDealId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published TimeDealCreatedEvent: timeDealId={}, sellerId={}, offset={}",
                                event.getTimeDealId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdated(TimeDealUpdatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.TIMEDEAL_UPDATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TimeDealUpdatedEvent: timeDealId={}, sellerId={}",
                                event.getTimeDealId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published TimeDealUpdatedEvent: timeDealId={}, sellerId={}, offset={}",
                                event.getTimeDealId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancelled(TimeDealCancelledEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.TIMEDEAL_CANCELLED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TimeDealCancelledEvent: timeDealId={}, sellerId={}",
                                event.getTimeDealId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published TimeDealCancelledEvent: timeDealId={}, sellerId={}, offset={}",
                                event.getTimeDealId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
