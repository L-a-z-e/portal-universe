package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String referenceId;
    private String referenceType;
}
