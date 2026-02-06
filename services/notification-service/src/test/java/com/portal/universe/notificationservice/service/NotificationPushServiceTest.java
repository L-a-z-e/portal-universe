package com.portal.universe.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.domain.Notification;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper redisObjectMapper;

    private NotificationPushService pushService;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        pushService = new NotificationPushService(messagingTemplate, redisTemplate, redisObjectMapper);
    }

    private Notification createTestNotification() {
        return Notification.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/orders/123")
                .status(NotificationStatus.UNREAD)
                .referenceId("ORD-123")
                .referenceType("order")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should_pushViaWebSocket_when_pushCalled")
    void should_pushViaWebSocket_when_pushCalled() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{}");

        // when
        pushService.push(notification);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_USER_ID),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("should_sendToCorrectDestination_when_push")
    void should_sendToCorrectDestination_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{}");

        // when
        pushService.push(notification);

        // then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(
                anyString(), destinationCaptor.capture(), any(NotificationResponse.class));

        assertThat(destinationCaptor.getValue()).isEqualTo("/queue/notifications");
    }

    @Test
    @DisplayName("should_sendNotificationResponsePayload_when_push")
    void should_sendNotificationResponsePayload_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{}");

        // when
        pushService.push(notification);

        // then
        ArgumentCaptor<NotificationResponse> payloadCaptor =
                ArgumentCaptor.forClass(NotificationResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                anyString(), anyString(), payloadCaptor.capture());

        NotificationResponse payload = payloadCaptor.getValue();
        assertThat(payload.getId()).isEqualTo(1L);
        assertThat(payload.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(payload.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(payload.getTitle()).isEqualTo("주문 접수");
    }

    @Test
    @DisplayName("should_publishToRedisChannel_when_push")
    void should_publishToRedisChannel_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        String expectedJson = "{\"id\":1}";
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn(expectedJson);

        // when
        pushService.push(notification);

        // then
        verify(redisTemplate).convertAndSend(anyString(), eq(expectedJson));
    }

    @Test
    @DisplayName("should_useCorrectRedisChannel_when_push")
    void should_useCorrectRedisChannel_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{}");

        // when
        pushService.push(notification);

        // then
        String expectedChannel = NotificationConstants.REDIS_CHANNEL_PREFIX + TEST_USER_ID;
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), anyString());

        assertThat(channelCaptor.getValue()).isEqualTo(expectedChannel);
        assertThat(channelCaptor.getValue()).isEqualTo("notification:" + TEST_USER_ID);
    }

    @Test
    @DisplayName("should_serializeResponseAsJson_when_push")
    void should_serializeResponseAsJson_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{\"serialized\":true}");

        // when
        pushService.push(notification);

        // then
        verify(redisObjectMapper).writeValueAsString(any(NotificationResponse.class));
    }

    @Test
    @DisplayName("should_handleJsonSerializationError_when_push")
    void should_handleJsonSerializationError_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willThrow(new JsonProcessingException("Serialization failed") {});

        // when - should not throw
        pushService.push(notification);

        // then - WebSocket should still be called, Redis convertAndSend should not be called
        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_USER_ID),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("should_pushWebSocketEvenIfRedisFails_when_push")
    void should_pushWebSocketEvenIfRedisFails_when_push() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willThrow(new JsonProcessingException("Redis error") {});

        // when
        pushService.push(notification);

        // then - WebSocket push happened before Redis attempt
        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_USER_ID),
                eq(NotificationConstants.WS_QUEUE_NOTIFICATIONS),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("should_passUserIdToConvertAndSendToUser_when_push")
    void should_passUserIdToConvertAndSendToUser_when_push() throws JsonProcessingException {
        // given
        String differentUserId = "user-abc-123";
        Notification notification = Notification.builder()
                .id(2L)
                .userId(differentUserId)
                .type(NotificationType.BLOG_LIKE)
                .title("좋아요")
                .message("좋아요가 달렸습니다")
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();

        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{}");

        // when
        pushService.push(notification);

        // then
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(
                userCaptor.capture(), anyString(), any(NotificationResponse.class));

        assertThat(userCaptor.getValue()).isEqualTo(differentUserId);
    }

    @Test
    @DisplayName("should_useRedisObjectMapper_when_serializing")
    void should_useRedisObjectMapper_when_serializing() throws JsonProcessingException {
        // given
        Notification notification = createTestNotification();
        given(redisObjectMapper.writeValueAsString(any(NotificationResponse.class)))
                .willReturn("{\"test\":true}");

        // when
        pushService.push(notification);

        // then - verify that specifically the redisObjectMapper mock was called
        verify(redisObjectMapper).writeValueAsString(any(NotificationResponse.class));
    }
}
