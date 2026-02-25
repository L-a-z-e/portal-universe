package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;

/**
 * SQS email-queue에 전송할 이메일 메시지 DTO.
 * Kafka 구간은 Avro + Schema Registry, SQS 구간은 JSON (서비스 내부 통신).
 */
public record EmailMessage(
    String userId,
    String recipientEmail,
    NotificationType type,
    String subject,
    String body,
    String link
) {
    public static EmailMessage from(CreateNotificationCommand cmd) {
        return new EmailMessage(
            cmd.userId(),
            null, // 이메일 주소는 EmailWorker에서 사용자 조회로 획득
            cmd.type(),
            cmd.title(),
            cmd.message(),
            cmd.link()
        );
    }
}