package com.portal.universe.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailQueueConsumer")
class EmailQueueConsumerTest {

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private EmailQueueConsumer emailQueueConsumer;

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/email-queue";

    @BeforeEach
    void setUp() {
        emailQueueConsumer = new EmailQueueConsumer(sqsClient, objectMapper);
        ReflectionTestUtils.setField(emailQueueConsumer, "emailQueueUrl", QUEUE_URL);
    }

    @Nested
    @DisplayName("pollEmailQueue")
    class PollEmailQueue {

        @Test
        @DisplayName("should process and delete message when valid email message received")
        void should_process_and_delete_valid_message() {
            // given
            String messageBody = """
                    {"userId":"user-001","recipientEmail":null,"type":"ORDER_CREATED","subject":"주문 접수","body":"주문이 접수되었습니다","link":"/orders/123"}
                    """;
            Message message = Message.builder()
                    .messageId("msg-001")
                    .receiptHandle("receipt-001")
                    .body(messageBody.trim())
                    .build();

            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        }

        @Test
        @DisplayName("should not delete message when processing fails")
        void should_not_delete_when_processing_fails() {
            // given - invalid JSON body
            Message message = Message.builder()
                    .messageId("msg-002")
                    .receiptHandle("receipt-002")
                    .body("invalid json")
                    .build();

            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        }

        @Test
        @DisplayName("should do nothing when no messages received")
        void should_do_nothing_when_no_messages() {
            // given
            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        }

        @Test
        @DisplayName("should use correct queue URL and long polling settings")
        void should_use_correct_queue_settings() {
            // given
            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
            verify(sqsClient).receiveMessage(captor.capture());
            assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(captor.getValue().maxNumberOfMessages()).isEqualTo(10);
            assertThat(captor.getValue().waitTimeSeconds()).isEqualTo(5);
        }

        @Test
        @DisplayName("should process multiple messages and delete each on success")
        void should_process_multiple_messages() {
            // given
            String body1 = """
                    {"userId":"u1","recipientEmail":null,"type":"ORDER_CREATED","subject":"s1","body":"b1","link":null}
                    """;
            String body2 = """
                    {"userId":"u2","recipientEmail":null,"type":"PAYMENT_COMPLETED","subject":"s2","body":"b2","link":null}
                    """;
            Message msg1 = Message.builder().messageId("m1").receiptHandle("r1").body(body1.trim()).build();
            Message msg2 = Message.builder().messageId("m2").receiptHandle("r2").body(body2.trim()).build();

            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(List.of(msg1, msg2)).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
        }

        @Test
        @DisplayName("should continue processing remaining messages when one fails")
        void should_continue_on_partial_failure() {
            // given - first message invalid, second valid
            Message badMsg = Message.builder().messageId("bad").receiptHandle("r-bad").body("bad json").build();
            String validBody = """
                    {"userId":"u1","recipientEmail":null,"type":"SYSTEM","subject":"s","body":"b","link":null}
                    """;
            Message goodMsg = Message.builder().messageId("good").receiptHandle("r-good").body(validBody.trim()).build();

            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(List.of(badMsg, goodMsg)).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then - only the good message should be deleted
            verify(sqsClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
        }

        @Test
        @DisplayName("should delete with correct receipt handle")
        void should_delete_with_correct_receipt_handle() {
            // given
            String body = """
                    {"userId":"u1","recipientEmail":null,"type":"ORDER_CREATED","subject":"s","body":"b","link":null}
                    """;
            Message message = Message.builder()
                    .messageId("msg-x")
                    .receiptHandle("handle-abc-123")
                    .body(body.trim())
                    .build();

            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

            // when
            emailQueueConsumer.pollEmailQueue();

            // then
            ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
            verify(sqsClient).deleteMessage(captor.capture());
            assertThat(captor.getValue().receiptHandle()).isEqualTo("handle-abc-123");
            assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
        }
    }
}
