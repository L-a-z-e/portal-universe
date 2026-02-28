package com.portal.universe.authservice.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 이벤트 발행 실패 시 지수 백오프로 재시도하는 퍼블리셔.
 * AFTER_COMMIT 핸들러에서 사용하며, 최대 3회 재시도 후 CRITICAL 로그를 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientKafkaPublisher {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000L;

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    public void send(String topic, SpecificRecord event) {
        send(topic, null, event);
    }

    public void send(String topic, String key, SpecificRecord event) {
        doSend(topic, key, event, 1);
    }

    private void doSend(String topic, String key, SpecificRecord event, int attempt) {
        avroKafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        if (attempt > 1) {
                            log.info("Event published on retry (attempt {}): topic={}, key={}",
                                    attempt, topic, key);
                        } else {
                            log.info("Event published: topic={}, key={}, offset={}",
                                    topic, key, result.getRecordMetadata().offset());
                        }
                    } else if (attempt < MAX_RETRIES + 1) {
                        long delayMs = BASE_DELAY_MS * attempt;
                        log.warn("Failed to publish event (attempt {}/{}), retrying in {}ms: topic={}, key={}, error={}",
                                attempt, MAX_RETRIES, delayMs, topic, key, ex.getMessage());
                        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                                .execute(() -> doSend(topic, key, event, attempt + 1));
                    } else {
                        log.error("CRITICAL: Event publish failed after {} attempts. topic={}, key={}, error={}",
                                MAX_RETRIES, topic, key, ex.getMessage(), ex);
                    }
                });
    }
}