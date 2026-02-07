package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.AuthAuditLog;

import java.time.format.DateTimeFormatter;

public record AuditLogResponse(
        Long id,
        String eventType,
        String targetUserId,
        String actorUserId,
        String details,
        String ipAddress,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AuditLogResponse from(AuthAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEventType().name(),
                log.getTargetUserId(),
                log.getActorUserId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt() != null ? log.getCreatedAt().format(FORMATTER) : null
        );
    }
}
