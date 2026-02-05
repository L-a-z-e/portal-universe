package com.portal.universe.notificationservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRedisSubscriber")
class NotificationRedisSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;

    private NotificationRedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        subscriber = new NotificationRedisSubscriber(messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("Redis 메시지를 수신하여 WebSocket으로 전달한다")
    void should_forwardMessageToWebSocket() throws Exception {
        NotificationResponse notification = NotificationResponse.builder()
                .id(1L)
                .userId("user-123")
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .status(NotificationStatus.UNREAD)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(notification);
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-123";

        subscriber.onMessage(jsonMessage, channel);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("채널에서 userId를 정확히 추출한다")
    void should_extractUserIdFromChannel() throws Exception {
        NotificationResponse notification = NotificationResponse.builder()
                .id(2L)
                .userId("user-abc-def-456")
                .type(NotificationType.BLOG_LIKE)
                .title("좋아요")
                .message("메시지")
                .status(NotificationStatus.UNREAD)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(notification);
        String channel = "notification:user-abc-def-456";

        subscriber.onMessage(jsonMessage, channel);

        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(
                userIdCaptor.capture(),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );

        assertThat(userIdCaptor.getValue()).isEqualTo("user-abc-def-456");
    }

    @Test
    @DisplayName("JSON 메시지를 NotificationResponse로 역직렬화한다")
    void should_deserializeJsonToNotificationResponse() throws Exception {
        NotificationResponse original = NotificationResponse.builder()
                .id(3L)
                .userId("user-789")
                .type(NotificationType.PAYMENT_COMPLETED)
                .title("결제 완료")
                .message("결제가 완료되었습니다")
                .status(NotificationStatus.UNREAD)
                .link("/shopping/orders/ORD-001")
                .referenceId("PAY-001")
                .referenceType("payment")
                .build();

        String jsonMessage = objectMapper.writeValueAsString(original);
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-789";

        subscriber.onMessage(jsonMessage, channel);

        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                anyString(),
                anyString(),
                captor.capture()
        );

        NotificationResponse captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo(3L);
        assertThat(captured.getType()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
        assertThat(captured.getTitle()).isEqualTo("결제 완료");
    }

    @Test
    @DisplayName("올바른 WebSocket queue 경로로 전달한다")
    void should_sendToCorrectQueue() throws Exception {
        NotificationResponse notification = NotificationResponse.builder()
                .id(4L)
                .userId("user-queue-test")
                .type(NotificationType.SYSTEM)
                .title("시스템")
                .message("테스트")
                .status(NotificationStatus.UNREAD)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(notification);
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-queue-test";

        subscriber.onMessage(jsonMessage, channel);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(
                anyString(),
                queueCaptor.capture(),
                any()
        );

        assertThat(queueCaptor.getValue()).isEqualTo("/queue/notifications");
    }

    @Test
    @DisplayName("notification: 접두사가 없는 채널은 무시한다")
    void should_ignoreNonNotificationChannel() {
        String channel = "other-channel:user-123";

        subscriber.onMessage("{}", channel);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("잘못된 JSON이 전달되면 예외 없이 처리한다")
    void should_handleInvalidJson_gracefully() {
        String invalidJson = "this is not json";
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-123";

        assertThatCode(() -> subscriber.onMessage(invalidJson, channel))
                .doesNotThrowAnyException();

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("역직렬화 실패 시 예외를 삼키고 로그를 남긴다")
    void should_catchDeserializationError() {
        // 타입이 맞지 않는 JSON
        String badJson = "{\"id\": \"not-a-number\"}";
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-456";

        assertThatCode(() -> subscriber.onMessage(badJson, channel))
                .doesNotThrowAnyException();

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("정상 처리 시 debug 로그 레벨로 기록한다 (예외 없이 완료)")
    void should_completeWithoutException_onValidMessage() throws Exception {
        NotificationResponse notification = NotificationResponse.builder()
                .id(10L)
                .userId("user-debug")
                .type(NotificationType.BLOG_FOLLOW)
                .title("팔로워")
                .message("새 팔로워")
                .status(NotificationStatus.UNREAD)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(notification);
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + "user-debug";

        assertThatCode(() -> subscriber.onMessage(jsonMessage, channel))
                .doesNotThrowAnyException();

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("user-debug"),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );
    }
}
