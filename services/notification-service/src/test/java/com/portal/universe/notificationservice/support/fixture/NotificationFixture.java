package com.portal.universe.notificationservice.support.fixture;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public final class NotificationFixture {

    public static final String DEFAULT_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private NotificationFixture() {}

    public static Notification create() {
        return builder().build();
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        private Long id;
        private String userId = DEFAULT_USER_ID;
        private NotificationType type = NotificationType.ORDER_CREATED;
        private String title = "주문 접수";
        private String message = "주문이 접수되었습니다";
        private String link = "/orders/123";
        private NotificationStatus status = NotificationStatus.UNREAD;
        private String referenceId = "ORD-123";
        private String referenceType = "order";
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime readAt;

        public NotificationBuilder id(Long id) { this.id = id; return this; }
        public NotificationBuilder userId(String userId) { this.userId = userId; return this; }
        public NotificationBuilder type(NotificationType type) { this.type = type; return this; }
        public NotificationBuilder title(String title) { this.title = title; return this; }
        public NotificationBuilder message(String message) { this.message = message; return this; }
        public NotificationBuilder link(String link) { this.link = link; return this; }
        public NotificationBuilder status(NotificationStatus status) { this.status = status; return this; }
        public NotificationBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public NotificationBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public NotificationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public NotificationBuilder readAt(LocalDateTime readAt) { this.readAt = readAt; return this; }

        public Notification build() {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .link(link)
                    .status(status)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .createdAt(createdAt)
                    .readAt(readAt)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(notification, "id", id);
            }
            return notification;
        }

        public NotificationResponse buildResponse() {
            return NotificationResponse.builder()
                    .id(id)
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .link(link)
                    .status(status)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .createdAt(createdAt)
                    .readAt(readAt)
                    .build();
        }
    }
}
