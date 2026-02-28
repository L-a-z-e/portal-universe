package com.portal.universe.blogservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * DB 트랜잭션 커밋 후에만 Kafka 이벤트를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogKafkaEventListener {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKafkaPublish(KafkaPublishEvent event) {
        avroKafkaTemplate.send(event.topic(), event.key(), event.payload())
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Blog event published: topic={}, key={}, offset={}",
                                event.topic(), event.key(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish blog event: topic={}, key={}, error={}",
                                event.topic(), event.key(), ex.getMessage());
                    }
                });
    }
}
