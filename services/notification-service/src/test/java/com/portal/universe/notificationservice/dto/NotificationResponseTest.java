package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationResponse DTO")
class NotificationResponseTest {

    @Test
    @DisplayName("from()으로 Notification 엔티티를 NotificationResponse로 변환한다")
    void should_convertFromNotification() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 7, 1, 10, 0);
        LocalDateTime readAt = LocalDateTime.of(2025, 7, 1, 11, 0);

        Notification notification = Notification.builder()
                .id(1L)
                .userId("user-123")
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/shopping/orders/ORD-001")
                .status(NotificationStatus.READ)
                .referenceId("ORD-001")
                .referenceType("order")
                .createdAt(createdAt)
                .readAt(readAt)
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(response.getTitle()).isEqualTo("주문 접수");
        assertThat(response.getMessage()).isEqualTo("주문이 접수되었습니다");
        assertThat(response.getLink()).isEqualTo("/shopping/orders/ORD-001");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(response.getReferenceId()).isEqualTo("ORD-001");
        assertThat(response.getReferenceType()).isEqualTo("order");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getReadAt()).isEqualTo(readAt);
    }

    @Test
    @DisplayName("from()에서 null인 optional 필드는 null로 매핑된다")
    void should_mapNullFields_when_optionalFieldsAreNull() {
        Notification notification = Notification.builder()
                .id(2L)
                .userId("user-456")
                .type(NotificationType.SYSTEM)
                .title("시스템 알림")
                .message("시스템 메시지")
                .status(NotificationStatus.UNREAD)
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.getLink()).isNull();
        assertThat(response.getReferenceId()).isNull();
        assertThat(response.getReferenceType()).isNull();
        assertThat(response.getReadAt()).isNull();
    }

    @Test
    @DisplayName("status가 READ이고 readAt이 설정된 경우 그대로 매핑된다")
    void should_preserveReadStatus_when_readAtIsSet() {
        LocalDateTime readAt = LocalDateTime.of(2025, 7, 15, 14, 30);

        Notification notification = Notification.builder()
                .id(3L)
                .userId("user-789")
                .type(NotificationType.BLOG_LIKE)
                .title("좋아요")
                .message("좋아요가 달렸습니다")
                .status(NotificationStatus.READ)
                .readAt(readAt)
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(response.getReadAt()).isEqualTo(readAt);
    }

    @Test
    @DisplayName("Builder로 직접 NotificationResponse를 생성할 수 있다")
    void should_createResponse_withBuilder() {
        NotificationResponse response = NotificationResponse.builder()
                .id(10L)
                .userId("user-abc")
                .type(NotificationType.PAYMENT_COMPLETED)
                .title("결제 완료")
                .message("결제가 완료되었습니다")
                .status(NotificationStatus.UNREAD)
                .build();

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo("user-abc");
        assertThat(response.getType()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
    }

    @Test
    @DisplayName("NoArgsConstructor로 빈 인스턴스를 생성할 수 있다")
    void should_createEmptyResponse_withNoArgsConstructor() {
        NotificationResponse response = new NotificationResponse();

        assertThat(response.getId()).isNull();
        assertThat(response.getUserId()).isNull();
        assertThat(response.getType()).isNull();
        assertThat(response.getStatus()).isNull();
    }
}
