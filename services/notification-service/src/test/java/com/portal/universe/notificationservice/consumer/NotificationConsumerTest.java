package com.portal.universe.notificationservice.consumer;

import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.event.shopping.OrderCreatedEvent;
import com.portal.universe.notificationservice.converter.NotificationEventConverter;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPushService pushService;

    @Mock
    private NotificationEventConverter converter;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    @DisplayName("should_createWelcomeNotification_when_userSignup")
    void should_createWelcomeNotification_when_userSignup() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        UserSignedUpEvent event = new UserSignedUpEvent(userId, "hong@test.com", "홍길동");

        Notification notification = Notification.builder()
                .userId(userId)
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
        assertThat(captured.userId()).isEqualTo(userId);
        assertThat(captured.type()).isEqualTo(NotificationType.SYSTEM);
        assertThat(captured.title()).isEqualTo("환영합니다!");
        assertThat(captured.message()).contains("홍길동");

        verify(pushService).push(notification);
    }

    @Test
    @DisplayName("should_createNotification_when_orderCreated")
    void should_createNotification_when_orderCreated() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        OrderCreatedEvent event = new OrderCreatedEvent(
                "ORD-001", userId, new java.math.BigDecimal("50000"), 2, java.util.List.of(), java.time.LocalDateTime.now()
        );

        CreateNotificationCommand cmd = new CreateNotificationCommand(
                userId, NotificationType.ORDER_CREATED, "주문이 접수되었습니다",
                "2개 상품, 50,000원 결제 대기중", "/shopping/orders/ORD-001", "ORD-001", "order"
        );

        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("주문이 접수되었습니다")
                .message("2개 상품, 50,000원 결제 대기중")
                .build();

        given(converter.convert(event)).willReturn(cmd);
        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleOrderCreated(event);

        // then
        verify(converter).convert(event);
        verify(notificationService).create(cmd);
        verify(pushService).push(notification);
    }
}
