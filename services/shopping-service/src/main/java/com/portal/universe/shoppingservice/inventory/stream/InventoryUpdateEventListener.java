package com.portal.universe.shoppingservice.inventory.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryUpdateEventListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryUpdate(InventoryUpdate event) {
        String channel = "inventory:" + event.getProductId();
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, jsonPayload);
            log.debug("Published inventory update for product {}: available={}, reserved={}",
                    event.getProductId(), event.getAvailable(), event.getReserved());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize inventory update for product {}", event.getProductId(), e);
        } catch (Exception e) {
            log.error("Failed to publish inventory update to Redis for product {}", event.getProductId(), e);
        }
    }
}
