package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateNotificationCommand DTO")
class CreateNotificationCommandTest {

    @Test
    @DisplayName("record 생성자로 모든 필드를 설정한다")
    void should_createCommand_withAllFields() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                "user-123",
                NotificationType.ORDER_CREATED,
                "주문 접수",
                "주문이 접수되었습니다",
                "/shopping/orders/ORD-001",
                "ORD-001",
                "order"
        );

        assertThat(command.userId()).isEqualTo("user-123");
        assertThat(command.type()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(command.title()).isEqualTo("주문 접수");
        assertThat(command.message()).isEqualTo("주문이 접수되었습니다");
        assertThat(command.link()).isEqualTo("/shopping/orders/ORD-001");
        assertThat(command.referenceId()).isEqualTo("ORD-001");
        assertThat(command.referenceType()).isEqualTo("order");
    }

    @Test
    @DisplayName("from(NotificationEvent)로 이벤트에서 커맨드를 생성한다")
    void should_createCommand_fromNotificationEvent() {
        NotificationEvent event = NotificationEvent.builder()
                .userId("user-456")
                .type(NotificationType.BLOG_LIKE)
                .title("좋아요")
                .message("좋아요가 달렸습니다")
                .link("/blog/post-123")
                .referenceId("like-001")
                .referenceType("like")
                .build();

        CreateNotificationCommand command = CreateNotificationCommand.from(event);

        assertThat(command.userId()).isEqualTo("user-456");
        assertThat(command.type()).isEqualTo(NotificationType.BLOG_LIKE);
        assertThat(command.title()).isEqualTo("좋아요");
        assertThat(command.message()).isEqualTo("좋아요가 달렸습니다");
        assertThat(command.link()).isEqualTo("/blog/post-123");
        assertThat(command.referenceId()).isEqualTo("like-001");
        assertThat(command.referenceType()).isEqualTo("like");
    }

    @Test
    @DisplayName("link가 null인 경우 null로 생성된다")
    void should_allowNullLink() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                "user-123",
                NotificationType.SYSTEM,
                "시스템",
                "시스템 알림",
                null,
                null,
                null
        );

        assertThat(command.link()).isNull();
    }

    @Test
    @DisplayName("referenceId가 null인 경우 null로 생성된다")
    void should_allowNullReferenceId() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                "user-123",
                NotificationType.SYSTEM,
                "시스템",
                "시스템 알림",
                null,
                null,
                null
        );

        assertThat(command.referenceId()).isNull();
        assertThat(command.referenceType()).isNull();
    }

    @Test
    @DisplayName("from()으로 생성된 커맨드는 이벤트의 모든 필드를 매핑한다")
    void should_mapAllFields_fromEvent() {
        NotificationEvent event = NotificationEvent.builder()
                .userId("user-789")
                .type(NotificationType.PRISM_TASK_COMPLETED)
                .title("AI 태스크 완료")
                .message("태스크가 완료되었습니다")
                .link("/prism/boards/1/tasks/5")
                .referenceId("5")
                .referenceType("task")
                .build();

        CreateNotificationCommand command = CreateNotificationCommand.from(event);

        assertThat(command.userId()).isEqualTo(event.getUserId());
        assertThat(command.type()).isEqualTo(event.getType());
        assertThat(command.title()).isEqualTo(event.getTitle());
        assertThat(command.message()).isEqualTo(event.getMessage());
        assertThat(command.link()).isEqualTo(event.getLink());
        assertThat(command.referenceId()).isEqualTo(event.getReferenceId());
        assertThat(command.referenceType()).isEqualTo(event.getReferenceType());
    }
}
