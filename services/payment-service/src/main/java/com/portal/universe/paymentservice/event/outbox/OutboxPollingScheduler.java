package com.portal.universe.paymentservice.event.outbox;

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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingForUpdate(BATCH_SIZE);
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                SpecificRecord record = deserialize(event.getEventType(), event.getPayload());
                avroKafkaTemplate.send(event.getTopic(), event.getEventKey(), record).get();
                event.markPublished();

                log.debug("Outbox event published: id={}, topic={}, key={}",
                        event.getId(), event.getTopic(), event.getEventKey());
            } catch (Exception ex) {
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
        }
    }

    private SpecificRecord deserialize(String className, String json) throws Exception {
        Class<?> clazz = Class.forName(className);
        Schema schema = (Schema) clazz.getMethod("getClassSchema").invoke(null);
        SpecificDatumReader<SpecificRecord> reader = new SpecificDatumReader<>(schema);
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        return reader.read(null, decoder);
    }
}
