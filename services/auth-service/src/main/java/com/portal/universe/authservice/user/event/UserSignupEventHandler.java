package com.portal.universe.authservice.user.event;

import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 회원가입 완료 후 Kafka 이벤트를 발행하는 핸들러.
 * TransactionalEventListener(AFTER_COMMIT)를 사용하여
 * 트랜잭션이 성공적으로 커밋된 후에만 이벤트를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSignupEventHandler {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Publishing user signup event: userId={}, email={}", event.getUserId(), event.getEmail());
        avroKafkaTemplate.send(AuthTopics.USER_SIGNED_UP, event);
    }
}
