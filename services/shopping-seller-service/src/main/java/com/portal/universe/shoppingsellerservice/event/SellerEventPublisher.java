package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.shopping.InventoryReservedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellerEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        avroKafkaTemplate.send(ShoppingTopics.INVENTORY_RESERVED, event.getOrderNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryReservedEvent: {}", event.getOrderNumber(), ex);
                    } else {
                        log.info("Published InventoryReservedEvent: {}", event.getOrderNumber());
                    }
                });
    }
}
