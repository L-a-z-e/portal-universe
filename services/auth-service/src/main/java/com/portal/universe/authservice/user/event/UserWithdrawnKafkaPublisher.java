package com.portal.universe.authservice.user.event;

import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawnKafkaPublisher {

    private final ResilientKafkaPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawnEvent event) {
        log.info("Publishing user withdrawn event: userId={}", event.getUserId());
        kafkaPublisher.send(AuthTopics.USER_WITHDRAWN, event.getUserId().toString(), event);
    }
}
