package com.portal.universe.notificationservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification 도메인")
class NotificationTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builder로 모든 필드를 설정하여 Notification을 생성한다")
        void should_createNotification_when_allFieldsProvided() {
            LocalDateTime now = LocalDateTime.now();

            Notification notification = Notification.builder()
                    .id(1L)
                    .userId("user-123")
                    .type(NotificationType.ORDER_CREATED)
                    .title("주문 접수")
                    .message("주문이 접수되었습니다")
                    .link("/shopping/orders/ORD-001")
                    .status(NotificationStatus.UNREAD)
                    .referenceId("ORD-001")
                    .referenceType("order")
                    .createdAt(now)
                    .readAt(null)
                    .build();

            assertThat(notification.getId()).isEqualTo(1L);
            assertThat(notification.getUserId()).isEqualTo("user-123");
            assertThat(notification.getType()).isEqualTo(NotificationType.ORDER_CREATED);
            assertThat(notification.getTitle()).isEqualTo("주문 접수");
            assertThat(notification.getMessage()).isEqualTo("주문이 접수되었습니다");
            assertThat(notification.getLink()).isEqualTo("/shopping/orders/ORD-001");
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNREAD);
            assertThat(notification.getReferenceId()).isEqualTo("ORD-001");
            assertThat(notification.getReferenceType()).isEqualTo("order");
            assertThat(notification.getCreatedAt()).isEqualTo(now);
            assertThat(notification.getReadAt()).isNull();
        }

        @Test
        @DisplayName("Builder로 필수 필드만 설정하여 Notification을 생성한다")
        void should_createNotification_when_onlyRequiredFields() {
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.BLOG_LIKE)
                    .title("좋아요")
                    .message("좋아요가 달렸습니다")
                    .build();

            assertThat(notification.getUserId()).isEqualTo("user-123");
            assertThat(notification.getType()).isEqualTo(NotificationType.BLOG_LIKE);
            assertThat(notification.getLink()).isNull();
            assertThat(notification.getReferenceId()).isNull();
            assertThat(notification.getReferenceType()).isNull();
        }
    }

    @Nested
    @DisplayName("@PrePersist - onCreate()")
    class OnCreateTest {

        @Test
        @DisplayName("status가 null이면 UNREAD로 설정된다")
        void should_setStatusUnread_when_statusIsNull() {
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .build();

            assertThat(notification.getStatus()).isNull();

            notification.onCreate();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        }

        @Test
        @DisplayName("createdAt이 null이면 현재 시간이 설정된다")
        void should_setCreatedAt_when_createdAtIsNull() {
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .build();

            assertThat(notification.getCreatedAt()).isNull();

            LocalDateTime before = LocalDateTime.now();
            notification.onCreate();
            LocalDateTime after = LocalDateTime.now();

            assertThat(notification.getCreatedAt()).isNotNull();
            assertThat(notification.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(notification.getCreatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("status가 이미 설정되어 있으면 변경하지 않는다")
        void should_notOverrideStatus_when_statusAlreadySet() {
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .status(NotificationStatus.READ)
                    .build();

            notification.onCreate();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
        }

        @Test
        @DisplayName("createdAt이 이미 설정되어 있으면 변경하지 않는다")
        void should_notOverrideCreatedAt_when_createdAtAlreadySet() {
            LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .createdAt(fixedTime)
                    .build();

            notification.onCreate();

            assertThat(notification.getCreatedAt()).isEqualTo(fixedTime);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTest {

        @Test
        @DisplayName("UNREAD 상태에서 READ로 전환되고 readAt이 설정된다")
        void should_transitionToRead_when_statusIsUnread() {
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .status(NotificationStatus.UNREAD)
                    .build();

            LocalDateTime before = LocalDateTime.now();
            notification.markAsRead();
            LocalDateTime after = LocalDateTime.now();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getReadAt()).isNotNull();
            assertThat(notification.getReadAt()).isAfterOrEqualTo(before);
            assertThat(notification.getReadAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("이미 READ 상태이면 상태와 readAt이 변경되지 않는다")
        void should_notChange_when_alreadyRead() {
            LocalDateTime originalReadAt = LocalDateTime.of(2025, 6, 1, 10, 0);
            Notification notification = Notification.builder()
                    .userId("user-123")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("시스템 알림")
                    .status(NotificationStatus.READ)
                    .readAt(originalReadAt)
                    .build();

            notification.markAsRead();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getReadAt()).isEqualTo(originalReadAt);
        }
    }
}
