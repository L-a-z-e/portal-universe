package com.portal.universe.authservice.auth.event;

import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAssignedKafkaPublisher {

    private final ResilientKafkaPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleAssignedEvent event) {
        log.info("Publishing role assigned event: userId={}, roleKey={}", event.getUserId(), event.getRoleKey());
        kafkaPublisher.send(AuthTopics.ROLE_ASSIGNED, event.getUserId().toString(), event);
    }
}