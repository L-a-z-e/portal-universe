package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka로부터 메시지를 수신(consume)하는 컨슈머 클래스입니다.
 */
@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    /**
     * 'user-signup' 토픽을 구독하고, 새로운 메시지가 들어오면 처리하는 리스너 메서드입니다.
     * groupId는 컨슈머 그룹을 식별하는 데 사용됩니다.
     *
     * @param event Kafka로부터 수신한 UserSignedUpEvent 메시지 객체
     */
    @KafkaListener(topics = "user-signup", groupId = "notification-group")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: {}", event);

        // 실제 프로덕션 환경에서는 이 부분에 이메일 발송, SMS 발송 등의 로직이 구현됩니다.
        // 현재는 로그를 남기는 것으로 대체합니다.
        log.info("Sending welcome email to: {} ({})", event.name(), event.email());
    }
}