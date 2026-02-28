package com.portal.universe.authservice.user.event;

import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSignupEventHandler {

    private final ResilientKafkaPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Publishing user signup event: userId={}, email={}", event.getUserId(), event.getEmail());
        kafkaPublisher.send(AuthTopics.USER_SIGNED_UP, event);
    }
}
