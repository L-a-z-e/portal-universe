package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationEvent;
import com.portal.universe.notificationservice.service.NotificationPushService;
import com.portal.universe.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "user-signup", groupId = "notification-group")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: {}", event);
        log.info("Sending welcome email to: {} ({})", event.name(), event.email());
    }

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.delivery.shipped", groupId = "notification-group")
    public void handleDeliveryShipped(NotificationEvent event) {
        log.info("Received delivery shipped event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.payment.completed", groupId = "notification-group")
    public void handlePaymentCompleted(NotificationEvent event) {
        log.info("Received payment completed event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.coupon.issued", groupId = "notification-group")
    public void handleCouponIssued(NotificationEvent event) {
        log.info("Received coupon issued event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.timedeal.started", groupId = "notification-group")
    public void handleTimeDealStarted(NotificationEvent event) {
        log.info("Received timedeal started event: userId={}", event.getUserId());
        createAndPush(event);
    }

    /**
     * 알림을 생성하고 푸시합니다.
     *
     * 중요: try-catch로 예외를 삼키지 않습니다.
     * 예외가 발생하면 KafkaConsumerConfig의 ErrorHandler가:
     * 1. 설정된 횟수만큼 재시도
     * 2. 재시도 실패 시 DLQ(Dead Letter Queue)로 이동
     *
     * @param event 알림 이벤트
     * @throws RuntimeException 처리 실패 시 (ErrorHandler가 처리)
     */
    private void createAndPush(NotificationEvent event) {
        log.debug("Creating notification for user: {}", event.getUserId());

        Notification notification = notificationService.create(
                event.getUserId(),
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getLink(),
                event.getReferenceId(),
                event.getReferenceType()
        );

        pushService.push(notification);

        log.info("Notification created and pushed: userId={}, type={}, notificationId={}",
                event.getUserId(), event.getType(), notification.getId());
    }
}