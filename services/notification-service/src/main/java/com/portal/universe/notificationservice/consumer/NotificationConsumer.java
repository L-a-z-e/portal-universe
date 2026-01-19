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

    private void createAndPush(NotificationEvent event) {
        try {
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
        } catch (Exception e) {
            log.error("Failed to create and push notification for user: {}", event.getUserId(), e);
        }
    }
}