package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.InventoryChangedEvent;
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
public class InventoryEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryChanged(InventoryChangedEvent event) {
        String key = String.valueOf(event.getProductId());
        avroKafkaTemplate.send(SellerTopics.INVENTORY_CHANGED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryChangedEvent: productId={}, changeType={}",
                                event.getProductId(), event.getChangeType(), ex);
                    } else {
                        log.info("Published InventoryChangedEvent: productId={}, changeType={}, available={}, offset={}",
                                event.getProductId(), event.getChangeType(), event.getAvailableQuantity(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
