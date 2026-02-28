package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.ProductCreatedEvent;
import com.portal.universe.event.seller.ProductDeletedEvent;
import com.portal.universe.event.seller.ProductUpdatedEvent;
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
public class ProductEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreated(ProductCreatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.PRODUCT_CREATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductCreatedEvent: productId={}, sellerId={}",
                                event.getProductId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published ProductCreatedEvent: productId={}, sellerId={}, offset={}",
                                event.getProductId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdated(ProductUpdatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.PRODUCT_UPDATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductUpdatedEvent: productId={}, sellerId={}",
                                event.getProductId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published ProductUpdatedEvent: productId={}, sellerId={}, offset={}",
                                event.getProductId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleted(ProductDeletedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.PRODUCT_DELETED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductDeletedEvent: productId={}, sellerId={}",
                                event.getProductId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published ProductDeletedEvent: productId={}, sellerId={}, offset={}",
                                event.getProductId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
