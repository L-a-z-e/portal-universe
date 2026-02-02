package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;

public record CreateNotificationCommand(
    Long userId,
    NotificationType type,
    String title,
    String message,
    String link,
    String referenceId,
    String referenceType
) {
    public static CreateNotificationCommand from(NotificationEvent event) {
        return new CreateNotificationCommand(
            event.getUserId(),
            event.getType(),
            event.getTitle(),
            event.getMessage(),
            event.getLink(),
            event.getReferenceId(),
            event.getReferenceType()
        );
    }
}
