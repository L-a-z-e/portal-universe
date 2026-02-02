package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.common.event.shopping.CouponIssuedEvent;
import com.portal.universe.common.event.shopping.TimeDealStartedEvent;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
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

    @KafkaListener(topics = NotificationConstants.TOPIC_USER_SIGNUP,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: userId={}", event.userId());
        NotificationEvent notifEvent = NotificationEvent.builder()
                .userId(Long.parseLong(event.userId()))
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message(event.name() + "님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();
        handleNotificationEvent(notifEvent);
    }

    @KafkaListener(topics = {
        NotificationConstants.TOPIC_ORDER_CREATED,
        NotificationConstants.TOPIC_ORDER_CONFIRMED,
        NotificationConstants.TOPIC_ORDER_CANCELLED,
        NotificationConstants.TOPIC_DELIVERY_SHIPPED,
        NotificationConstants.TOPIC_PAYMENT_COMPLETED,
        NotificationConstants.TOPIC_PAYMENT_FAILED
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void handleShoppingEvent(NotificationEvent event) {
        log.info("Received shopping event: userId={}, type={}", event.getUserId(), event.getType());
        handleNotificationEvent(event);
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_COUPON_ISSUED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleCouponIssued(CouponIssuedEvent event) {
        log.info("Received coupon issued event: couponCode={}", event.couponCode());
        NotificationEvent notifEvent = NotificationEvent.builder()
                .userId(event.userId())
                .type(NotificationType.COUPON_ISSUED)
                .title(NotificationType.COUPON_ISSUED.getDefaultMessage())
                .message(event.couponName() + " 쿠폰이 발급되었습니다.")
                .link("/shopping/coupons")
                .referenceId(event.couponCode())
                .referenceType("coupon")
                .build();
        handleNotificationEvent(notifEvent);
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_TIMEDEAL_STARTED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleTimeDealStarted(TimeDealStartedEvent event) {
        log.info("Received timedeal started event: id={}", event.timeDealId());
        // TimeDeal은 broadcast (특정 userId 없음) → 현재 구조에서는 skip
        // 향후 구독/관심 기능 추가 시 구현
        log.info("TimeDeal broadcast notification not yet implemented (no subscriber model)");
    }

    private void handleNotificationEvent(NotificationEvent event) {
        event.validate();

        Notification notification = notificationService.create(
                CreateNotificationCommand.from(event));

        pushService.push(notification);

        log.info("Notification created and pushed: userId={}, type={}, id={}",
                event.getUserId(), event.getType(), notification.getId());
    }
}
