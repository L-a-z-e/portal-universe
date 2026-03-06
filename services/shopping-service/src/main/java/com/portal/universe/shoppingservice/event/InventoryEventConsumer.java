package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.seller.InventoryChangedEvent;
import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryRepository inventoryRepository;

    @KafkaListener(topics = SellerTopics.INVENTORY_CHANGED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onInventoryChanged(InventoryChangedEvent event) {
        log.info("Received InventoryChangedEvent: productId={}, changeType={}, available={}",
                event.getProductId(), event.getChangeType(), event.getAvailableQuantity());

        Inventory inventory = inventoryRepository.findByProductId(event.getProductId())
                .orElse(null);

        if (inventory == null) {
            inventory = Inventory.builder()
                    .productId(event.getProductId())
                    .initialQuantity(event.getAvailableQuantity())
                    .build();
            inventory.adjust(event.getAvailableQuantity(), event.getReservedQuantity());
            inventoryRepository.save(inventory);
            log.info("Created inventory read model from event: productId={}, available={}",
                    event.getProductId(), event.getAvailableQuantity());
        } else {
            inventory.adjust(event.getAvailableQuantity(), event.getReservedQuantity());
            log.info("Updated inventory read model: productId={}, available={}, reserved={}",
                    event.getProductId(), event.getAvailableQuantity(), event.getReservedQuantity());
        }
    }
}
