package com.portal.universe.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailQueueService")
class EmailQueueServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    private EmailQueueService emailQueueService;

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/email-queue";

    @BeforeEach
    void setUp() {
        emailQueueService = new EmailQueueService(sqsClient, objectMapper);
        ReflectionTestUtils.setField(emailQueueService, "emailQueueUrl", QUEUE_URL);
    }

    @Nested
    @DisplayName("enqueueIfEmailWorthy")
    class EnqueueIfEmailWorthy {

        @Test
        @DisplayName("should send to SQS when notification type is email-worthy")
        void should_send_to_sqs_when_email_worthy() throws JsonProcessingException {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.ORDER_CREATED, "주문 접수",
                    "주문이 접수되었습니다", "/orders/123", "ORD-123", "order"
            );
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":true}");
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-001").build());

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
            verify(sqsClient).sendMessage(captor.capture());
            assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(captor.getValue().messageBody()).isEqualTo("{\"test\":true}");
        }

        @Test
        @DisplayName("should skip SQS when notification type is not email-worthy")
        void should_skip_when_not_email_worthy() {
            // given - BLOG_LIKE is not email-worthy
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.BLOG_LIKE, "좋아요",
                    "좋아요가 달렸습니다", "/blog/1", "LIKE-001", "like"
            );

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should send for PAYMENT_COMPLETED type")
        void should_send_for_payment_completed() throws JsonProcessingException {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.PAYMENT_COMPLETED, "결제 완료",
                    "결제가 완료되었습니다", "/orders/1", "PAY-001", "payment"
            );
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-002").build());

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should send for PASSWORD_RESET_REQUESTED type")
        void should_send_for_password_reset() throws JsonProcessingException {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.PASSWORD_RESET_REQUESTED, "비밀번호 재설정",
                    "비밀번호 재설정이 요청되었습니다", null, null, null
            );
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-003").build());

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should not throw when JSON serialization fails")
        void should_not_throw_when_serialization_fails() throws JsonProcessingException {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.ORDER_CANCELLED, "주문 취소",
                    "주문이 취소되었습니다", null, "ORD-001", "order"
            );
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // when - should not throw
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should not throw when SQS call fails")
        void should_not_throw_when_sqs_fails() throws JsonProcessingException {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.SYSTEM, "시스템 알림",
                    "메시지", null, null, null
            );
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenThrow(new RuntimeException("SQS unavailable"));

            // when - should not throw
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then - exception was caught
            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should skip for BLOG_COMMENT type")
        void should_skip_for_blog_comment() {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.BLOG_COMMENT, "댓글",
                    "댓글이 달렸습니다", "/blog/1", "CMT-001", "comment"
            );

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("should skip for TIMEDEAL_STARTED type")
        void should_skip_for_timedeal_started() {
            // given
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "user-001", NotificationType.TIMEDEAL_STARTED, "타임딜",
                    "타임딜이 시작되었습니다", "/deals/1", "TD-001", "timedeal"
            );

            // when
            emailQueueService.enqueueIfEmailWorthy(cmd);

            // then
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }
    }
}
