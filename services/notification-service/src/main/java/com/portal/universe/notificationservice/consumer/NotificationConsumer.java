package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import com.portal.universe.event.prism.PrismTaskCompletedEvent;
import com.portal.universe.event.prism.PrismTaskFailedEvent;
import com.portal.universe.event.shopping.*;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.converter.NotificationEventConverter;
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
    private final NotificationEventConverter converter;

    @KafkaListener(topics = NotificationConstants.TOPIC_USER_SIGNUP,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: userId={}", event.userId());
        NotificationEvent notifEvent = NotificationEvent.builder()
                .userId(event.userId())
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message(event.name() + "님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();
        handleNotificationEvent(notifEvent);
    }

    // ===== Shopping Domain Events =====

    @KafkaListener(topics = NotificationConstants.TOPIC_ORDER_CREATED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: orderNumber={}", event.orderNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process order created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_ORDER_CANCELLED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order cancelled event: orderNumber={}", event.orderNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process order cancelled event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_PAYMENT_COMPLETED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event: paymentNumber={}", event.paymentNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process payment completed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_PAYMENT_FAILED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment failed event: paymentNumber={}", event.paymentNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process payment failed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_DELIVERY_SHIPPED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleDeliveryShipped(DeliveryShippedEvent event) {
        log.info("Received delivery shipped event: trackingNumber={}", event.trackingNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process delivery shipped event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_COUPON_ISSUED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleCouponIssued(CouponIssuedEvent event) {
        log.info("Received coupon issued event: couponCode={}", event.couponCode());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process coupon issued event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_TIMEDEAL_STARTED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleTimeDealStarted(TimeDealStartedEvent event) {
        log.info("Received timedeal started event: id={}", event.timeDealId());
        // TimeDeal은 broadcast (특정 userId 없음) → 현재 구조에서는 skip
        // 향후 구독/관심 기능 추가 시 구현
        log.info("TimeDeal broadcast notification not yet implemented (no subscriber model)");
    }

    // ===== Blog Domain Events =====

    @KafkaListener(topics = NotificationConstants.TOPIC_BLOG_POST_LIKED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePostLiked(PostLikedEvent event) {
        log.info("Received post liked event: postId={}, likerId={}", event.postId(), event.likerId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process post liked event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_BLOG_POST_COMMENTED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("Received comment created event: postId={}, commenterId={}", event.postId(), event.commenterId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process comment created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_BLOG_COMMENT_REPLIED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleCommentReplied(CommentRepliedEvent event) {
        log.info("Received comment replied event: postId={}, replierId={}, parentCommentId={}",
                event.postId(), event.replierId(), event.parentCommentId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process comment replied event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_BLOG_USER_FOLLOWED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserFollowed(UserFollowedEvent event) {
        log.info("Received user followed event: followeeId={}, followerId={}",
                event.followeeId(), event.followerId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process user followed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===== Prism Domain Events =====

    @KafkaListener(topics = NotificationConstants.TOPIC_PRISM_TASK_COMPLETED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePrismTaskCompleted(PrismTaskCompletedEvent event) {
        log.info("Received prism task completed event: taskId={}, userId={}", event.taskId(), event.userId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process prism task completed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = NotificationConstants.TOPIC_PRISM_TASK_FAILED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePrismTaskFailed(PrismTaskFailedEvent event) {
        log.info("Received prism task failed event: taskId={}, userId={}", event.taskId(), event.userId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process prism task failed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleNotificationEvent(NotificationEvent event) {
        event.validate();
        createAndPushNotification(CreateNotificationCommand.from(event));
    }

    /**
     * 알림을 생성하고 실시간 푸시합니다.
     */
    private void createAndPushNotification(CreateNotificationCommand cmd) {
        Notification notification = notificationService.create(cmd);
        pushService.push(notification);
        log.info("Notification created and pushed: userId={}, type={}, id={}",
                cmd.userId(), cmd.type(), notification.getId());
    }
}
