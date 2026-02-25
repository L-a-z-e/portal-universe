package com.portal.universe.authservice.auth.event;

import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 역할 할당 트랜잭션 커밋 후 Kafka로 이벤트를 발행하는 핸들러.
 * 외부 마이크로서비스가 역할 할당 이벤트를 구독할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAssignedKafkaPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleAssignedEvent event) {
        log.info("Publishing role assigned event: userId={}, roleKey={}", event.getUserId(), event.getRoleKey());
        avroKafkaTemplate.send(AuthTopics.ROLE_ASSIGNED, event.getUserId().toString(), event);
    }
}
