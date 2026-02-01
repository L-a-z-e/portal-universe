package com.portal.universe.shoppingservice.inventory.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryStreamService {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;

    private final Map<Long, Sinks.Many<InventoryUpdate>> productSinks = new ConcurrentHashMap<>();
    private final Map<Long, MessageListener> productListeners = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> subscriberCounts = new ConcurrentHashMap<>();

    public Flux<InventoryUpdate> subscribe(List<Long> productIds) {
        return Flux.merge(
                productIds.stream()
                        .map(this::subscribeToProduct)
                        .toList()
        );
    }

    private Flux<InventoryUpdate> subscribeToProduct(Long productId) {
        Sinks.Many<InventoryUpdate> sink = productSinks.computeIfAbsent(
                productId,
                id -> {
                    Sinks.Many<InventoryUpdate> newSink = Sinks.many().multicast().onBackpressureBuffer();
                    registerRedisListener(id, newSink);
                    return newSink;
                }
        );

        subscriberCounts.computeIfAbsent(productId, id -> new AtomicInteger(0)).incrementAndGet();

        return sink.asFlux()
                .doOnCancel(() -> handleClientDisconnect(productId));
    }

    private void registerRedisListener(Long productId, Sinks.Many<InventoryUpdate> sink) {
        String channel = "inventory:" + productId;

        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                String body = new String(message.getBody());
                InventoryUpdate update = objectMapper.readValue(body, InventoryUpdate.class);
                sink.tryEmitNext(update);
            } catch (Exception e) {
                log.error("Failed to parse inventory update message for product {}", productId, e);
            }
        };

        productListeners.put(productId, listener);
        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));
        log.debug("Registered Redis listener for inventory channel: {}", channel);
    }

    private void handleClientDisconnect(Long productId) {
        AtomicInteger count = subscriberCounts.get(productId);
        if (count != null && count.decrementAndGet() <= 0) {
            cleanupProduct(productId);
        }
        log.debug("Client disconnected from inventory stream for product {}", productId);
    }

    private void cleanupProduct(Long productId) {
        subscriberCounts.remove(productId);

        Sinks.Many<InventoryUpdate> sink = productSinks.remove(productId);
        if (sink != null) {
            sink.tryEmitComplete();
        }

        MessageListener listener = productListeners.remove(productId);
        if (listener != null) {
            String channel = "inventory:" + productId;
            redisMessageListenerContainer.removeMessageListener(listener, new ChannelTopic(channel));
            log.debug("Removed Redis listener for inventory channel: {}", channel);
        }
    }

    public void publishInventoryUpdate(InventoryUpdate update) {
        Sinks.Many<InventoryUpdate> sink = productSinks.get(update.getProductId());
        if (sink != null) {
            sink.tryEmitNext(update);
        }
    }
}
