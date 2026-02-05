package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationEvent DTO")
class NotificationEventTest {

    @Test
    @DisplayName("Builder로 모든 필드를 설정하여 NotificationEvent를 생성한다")
    void should_createEvent_withBuilder() {
        NotificationEvent event = NotificationEvent.builder()
                .userId("user-123")
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/shopping/orders/ORD-001")
                .referenceId("ORD-001")
                .referenceType("order")
                .build();

        assertThat(event.getUserId()).isEqualTo("user-123");
        assertThat(event.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(event.getTitle()).isEqualTo("주문 접수");
        assertThat(event.getMessage()).isEqualTo("주문이 접수되었습니다");
        assertThat(event.getLink()).isEqualTo("/shopping/orders/ORD-001");
        assertThat(event.getReferenceId()).isEqualTo("ORD-001");
        assertThat(event.getReferenceType()).isEqualTo("order");
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTest {

        @Test
        @DisplayName("userId가 null이면 IllegalArgumentException이 발생한다")
        void should_throwException_when_userIdIsNull() {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(null)
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("메시지")
                    .build();

            assertThatThrownBy(event::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("userId is required");
        }

        @Test
        @DisplayName("userId가 blank이면 IllegalArgumentException이 발생한다")
        void should_throwException_when_userIdIsBlank() {
            NotificationEvent event = NotificationEvent.builder()
                    .userId("   ")
                    .type(NotificationType.SYSTEM)
                    .title("시스템")
                    .message("메시지")
                    .build();

            assertThatThrownBy(event::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("userId is required");
        }

        @Test
        @DisplayName("type이 null이면 IllegalArgumentException이 발생한다")
        void should_throwException_when_typeIsNull() {
            NotificationEvent event = NotificationEvent.builder()
                    .userId("user-123")
                    .type(null)
                    .title("제목")
                    .message("메시지")
                    .build();

            assertThatThrownBy(event::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("notification type is required");
        }

        @Test
        @DisplayName("title이 blank이면 type의 defaultMessage로 설정된다")
        void should_setDefaultTitle_when_titleIsBlank() {
            NotificationEvent event = NotificationEvent.builder()
                    .userId("user-123")
                    .type(NotificationType.ORDER_CREATED)
                    .title("")
                    .message("유효한 메시지")
                    .build();

            event.validate();

            assertThat(event.getTitle()).isEqualTo(NotificationType.ORDER_CREATED.getDefaultMessage());
        }

        @Test
        @DisplayName("message가 null이면 type의 defaultMessage로 설정된다")
        void should_setDefaultMessage_when_messageIsNull() {
            NotificationEvent event = NotificationEvent.builder()
                    .userId("user-123")
                    .type(NotificationType.BLOG_LIKE)
                    .title("유효한 제목")
                    .message(null)
                    .build();

            event.validate();

            assertThat(event.getMessage()).isEqualTo(NotificationType.BLOG_LIKE.getDefaultMessage());
        }
    }
}
