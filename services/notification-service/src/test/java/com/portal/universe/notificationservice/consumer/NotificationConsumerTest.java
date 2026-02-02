package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.dto.NotificationEvent;
import com.portal.universe.notificationservice.service.NotificationPushService;
import com.portal.universe.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPushService pushService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    @DisplayName("should_createWelcomeNotification_when_userSignup")
    void should_createWelcomeNotification_when_userSignup() {
        // given
        UserSignedUpEvent event = new UserSignedUpEvent("1", "hong@test.com", "홍길동");

        Notification notification = Notification.builder()
                .userId(1L)
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message("홍길동님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();

        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleUserSignup(event);

        // then
        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).create(captor.capture());

        CreateNotificationCommand captured = captor.getValue();
        assertThat(captured.userId()).isEqualTo(1L);
        assertThat(captured.type()).isEqualTo(NotificationType.SYSTEM);
        assertThat(captured.title()).isEqualTo("환영합니다!");
        assertThat(captured.message()).contains("홍길동");

        verify(pushService).push(notification);
    }

    @Test
    @DisplayName("should_createNotification_when_shoppingEvent")
    void should_createNotification_when_shoppingEvent() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .referenceId("ORD-001")
                .referenceType("order")
                .build();

        Notification notification = Notification.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .build();

        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleShoppingEvent(event);

        // then
        verify(notificationService).create(any(CreateNotificationCommand.class));
        verify(pushService).push(notification);
    }

    @Test
    @DisplayName("should_throwException_when_invalidEvent")
    void should_throwException_when_invalidEvent() {
        // given - userId가 null인 이벤트
        NotificationEvent event = NotificationEvent.builder()
                .userId(null)
                .type(NotificationType.ORDER_CREATED)
                .title("주문")
                .message("테스트")
                .build();

        // when & then
        assertThatThrownBy(() -> notificationConsumer.handleShoppingEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");

        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("should_useDefaultMessage_when_titleIsBlank")
    void should_useDefaultMessage_when_titleIsBlank() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("")
                .message("")
                .build();

        Notification notification = Notification.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title(NotificationType.ORDER_CREATED.getDefaultMessage())
                .message(NotificationType.ORDER_CREATED.getDefaultMessage())
                .build();

        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleShoppingEvent(event);

        // then
        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).create(captor.capture());

        CreateNotificationCommand captured = captor.getValue();
        assertThat(captured.title()).isEqualTo(NotificationType.ORDER_CREATED.getDefaultMessage());
        assertThat(captured.message()).isEqualTo(NotificationType.ORDER_CREATED.getDefaultMessage());
    }
}
