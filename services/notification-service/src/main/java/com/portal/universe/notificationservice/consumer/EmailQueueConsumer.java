package com.portal.universe.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

/**
 * SQS email-queue를 폴링하여 이메일을 발송하는 Worker.
 *
 * 실제 AWS 환경에서는 SES(Simple Email Service)로 발송하지만,
 * LocalStack 학습 환경에서는 로그로 대체한다.
 *
 * 처리 실패 시 삭제하지 않으면 VisibilityTimeout 후 재노출 → 3번 실패 시 DLQ 이동.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueueConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.email-queue-url}")
    private String emailQueueUrl;

    @Scheduled(fixedDelay = 5000) // 5초마다 폴링
    public void pollEmailQueue() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(emailQueueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5) // Long polling (비용 절감)
                .build());

        List<Message> messages = response.messages();
        if (messages.isEmpty()) {
            return;
        }

        log.info("Received {} email message(s) from SQS", messages.size());

        for (Message message : messages) {
            try {
                processEmailMessage(message);
                deleteMessage(message);
            } catch (Exception e) {
                log.error("Failed to process email message: {}, will retry", message.messageId(), e);
                // 삭제하지 않음 → VisibilityTimeout 후 재노출 → 3번 실패 시 DLQ
            }
        }
    }

    private void processEmailMessage(Message message) throws Exception {
        EmailMessage emailMessage = objectMapper.readValue(message.body(), EmailMessage.class);

        // TODO: 실제 환경에서는 SES로 이메일 발송
        // sesClient.sendEmail(...)
        log.info("[EMAIL] To: {} | Subject: {} | Body: {} | Link: {}",
                emailMessage.userId(),
                emailMessage.subject(),
                emailMessage.body(),
                emailMessage.link());
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(emailQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
        log.debug("Deleted email message from SQS: {}", message.messageId());
    }
}
