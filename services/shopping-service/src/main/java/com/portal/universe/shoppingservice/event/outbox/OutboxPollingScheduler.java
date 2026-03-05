package com.portal.universe.shoppingservice.event.outbox;

import com.portal.universe.event.shopping.OrderCreatedEvent;
import com.portal.universe.shoppingservice.event.EventBridgePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;
    private final EventBridgePublisher eventBridgePublisher;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingForUpdate(BATCH_SIZE);
        if (pending.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (OutboxEvent event : pending) {
            try {
                SpecificRecord record = deserialize(event.getEventType(), event.getPayload());

                CompletableFuture<Void> future = avroKafkaTemplate.send(
                        event.getTopic(), event.getEventKey(), record)
                    .thenAccept(result -> {
                        event.markPublished();
                        publishToEventBridgeIfNeeded(record);
                        log.debug("Outbox event published: id={}, topic={}, key={}",
                                event.getId(), event.getTopic(), event.getEventKey());
                    })
                    .exceptionally(ex -> {
                        handlePublishFailure(event, ex);
                        return null;
                    });

                futures.add(future);
            } catch (Exception ex) {
                handlePublishFailure(event, ex);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void handlePublishFailure(OutboxEvent event, Throwable ex) {
        event.incrementRetry();
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.markFailed();
            log.error("Outbox event permanently failed: id={}, topic={}, key={}",
                    event.getId(), event.getTopic(), event.getEventKey(), ex);
        } else {
            log.warn("Outbox event publish failed (attempt {}/{}): id={}, topic={}",
                    event.getRetryCount(), MAX_RETRIES, event.getId(), event.getTopic());
        }
    }

    private SpecificRecord deserialize(String className, String json) throws Exception {
        Class<?> clazz = Class.forName(className);
        Schema schema = (Schema) clazz.getMethod("getClassSchema").invoke(null);
        SpecificDatumReader<SpecificRecord> reader = new SpecificDatumReader<>(schema);
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        return reader.read(null, decoder);
    }

    private void publishToEventBridgeIfNeeded(SpecificRecord record) {
        if (!(record instanceof OrderCreatedEvent oce)) return;

        try {
            eventBridgePublisher.publishOrderCreated(
                    oce.getOrderNumber().toString(),
                    oce.getUserId().toString(),
                    oce.getTotalAmount(),
                    oce.getItemCount(),
                    oce.getItems().stream()
                            .map(item -> Map.<String, Object>of(
                                    "productId", item.getProductId(),
                                    "productName", item.getProductName().toString(),
                                    "quantity", item.getQuantity(),
                                    "price", item.getPrice().intValue()))
                            .toList());
        } catch (Exception ex) {
            log.warn("EventBridge publish failed (non-blocking): {}", ex.getMessage());
        }
    }
}
