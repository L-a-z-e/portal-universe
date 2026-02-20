package com.portal.universe.notificationservice.consumer;

import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserSignedUpEvent;
import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import com.portal.universe.event.drive.DriveTopics;
import com.portal.universe.event.drive.FileUploadedEvent;
import com.portal.universe.event.drive.FileDeletedEvent;
import com.portal.universe.event.drive.FolderCreatedEvent;
import com.portal.universe.event.prism.PrismTopics;
import com.portal.universe.event.prism.PrismTaskCompletedEvent;
import com.portal.universe.event.prism.PrismTaskFailedEvent;
import com.portal.universe.event.shopping.*;
import com.portal.universe.event.shopping.ShoppingTopics;
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

    @KafkaListener(topics = AuthTopics.USER_SIGNED_UP,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: userId={}", event.getUserId());
        NotificationEvent notifEvent = NotificationEvent.builder()
                .userId(event.getUserId())
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message(event.getName() + "님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();
        handleNotificationEvent(notifEvent);
    }

    // ===== Shopping Domain Events =====

    @KafkaListener(topics = ShoppingTopics.ORDER_CREATED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: orderNumber={}", event.getOrderNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process order created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order cancelled event: orderNumber={}", event.getOrderNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process order cancelled event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event: paymentNumber={}", event.getPaymentNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process payment completed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.PAYMENT_FAILED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment failed event: paymentNumber={}", event.getPaymentNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process payment failed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.DELIVERY_SHIPPED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleDeliveryShipped(DeliveryShippedEvent event) {
        log.info("Received delivery shipped event: trackingNumber={}", event.getTrackingNumber());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process delivery shipped event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.COUPON_ISSUED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleCouponIssued(CouponIssuedEvent event) {
        log.info("Received coupon issued event: couponCode={}", event.getCouponCode());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process coupon issued event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = ShoppingTopics.TIMEDEAL_STARTED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleTimeDealStarted(TimeDealStartedEvent event) {
        log.info("Received timedeal started event: id={}", event.getTimeDealId());
        // TimeDeal은 broadcast (특정 userId 없음) → 현재 구조에서는 skip
        // 향후 구독/관심 기능 추가 시 구현
        log.info("TimeDeal broadcast notification not yet implemented (no subscriber model)");
    }

    // ===== Blog Domain Events =====

    @KafkaListener(topics = BlogTopics.POST_LIKED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handlePostLiked(PostLikedEvent event) {
        log.info("Received post liked event: postId={}, likerId={}", event.getPostId(), event.getLikerId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process post liked event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = BlogTopics.POST_COMMENTED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("Received comment created event: postId={}, commenterId={}", event.getPostId(), event.getCommenterId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process comment created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = BlogTopics.COMMENT_REPLIED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleCommentReplied(CommentRepliedEvent event) {
        log.info("Received comment replied event: postId={}, replierId={}, parentCommentId={}",
                event.getPostId(), event.getReplierId(), event.getParentCommentId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process comment replied event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = BlogTopics.USER_FOLLOWED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleUserFollowed(UserFollowedEvent event) {
        log.info("Received user followed event: followeeId={}, followerId={}",
                event.getFolloweeId(), event.getFollowerId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process user followed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===== Drive Domain Events =====

    @KafkaListener(topics = DriveTopics.FILE_UPLOADED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleFileUploaded(FileUploadedEvent event) {
        log.info("Received file uploaded event: fileId={}, userId={}", event.getFileId(), event.getUserId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process file uploaded event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = DriveTopics.FILE_DELETED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleFileDeleted(FileDeletedEvent event) {
        log.info("Received file deleted event: fileId={}, userId={}", event.getFileId(), event.getUserId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process file deleted event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = DriveTopics.FOLDER_CREATED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handleFolderCreated(FolderCreatedEvent event) {
        log.info("Received folder created event: folderId={}, userId={}", event.getFolderId(), event.getUserId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process folder created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===== Prism Domain Events =====

    @KafkaListener(topics = PrismTopics.TASK_COMPLETED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handlePrismTaskCompleted(PrismTaskCompletedEvent event) {
        log.info("Received prism task completed event: taskId={}, userId={}", event.getTaskId(), event.getUserId());
        try {
            CreateNotificationCommand cmd = converter.convert(event);
            createAndPushNotification(cmd);
        } catch (Exception e) {
            log.error("Failed to process prism task completed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = PrismTopics.TASK_FAILED,
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "avroKafkaListenerContainerFactory")
    public void handlePrismTaskFailed(PrismTaskFailedEvent event) {
        log.info("Received prism task failed event: taskId={}, userId={}", event.getTaskId(), event.getUserId());
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

    private void createAndPushNotification(CreateNotificationCommand cmd) {
        Notification notification = notificationService.create(cmd);
        pushService.push(notification);
        log.info("Notification created and pushed: userId={}, type={}, id={}",
                cmd.userId(), cmd.type(), notification.getId());
    }
}
