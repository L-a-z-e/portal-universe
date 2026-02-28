package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.SellerApprovedEvent;
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
public class SellerApprovedEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SellerApprovedEvent event) {
        avroKafkaTemplate.send(SellerTopics.SELLER_APPROVED, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SellerApprovedEvent: userId={}, sellerId={}",
                                event.getUserId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published SellerApprovedEvent: userId={}, sellerId={}, offset={}",
                                event.getUserId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
