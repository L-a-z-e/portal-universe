package com.portal.universe.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Set;

/**
 * 이메일 발송이 필요한 알림을 SQS email-queue에 전송하는 서비스.
 *
 * 모든 알림이 이메일 대상은 아니다.
 * 주문/결제/배송 등 중요 알림만 이메일로 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.email-queue-url}")
    private String emailQueueUrl;

    /**
     * 이메일 발송 대상 알림 타입.
     * 주문/결제/배송 등 사용자가 반드시 인지해야 하는 알림만 포함.
     */
    private static final Set<NotificationType> EMAIL_WORTHY_TYPES = Set.of(
            NotificationType.ORDER_CREATED,
            NotificationType.ORDER_CANCELLED,
            NotificationType.PAYMENT_COMPLETED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.DELIVERY_STARTED,
            NotificationType.SYSTEM
    );

    public void enqueueIfEmailWorthy(CreateNotificationCommand cmd) {
        if (!EMAIL_WORTHY_TYPES.contains(cmd.type())) {
            return;
        }

        EmailMessage emailMessage = EmailMessage.from(cmd);
        try {
            String messageBody = objectMapper.writeValueAsString(emailMessage);

            SendMessageResponse response = sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(emailQueueUrl)
                    .messageBody(messageBody)
                    .build());

            log.info("Email queued - userId: {}, type: {}, messageId: {}",
                    cmd.userId(), cmd.type(), response.messageId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email message: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to send message to SQS: {}", e.getMessage(), e);
        }
    }
}
