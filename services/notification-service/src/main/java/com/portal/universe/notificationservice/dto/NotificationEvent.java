package com.portal.universe.notificationservice.dto;

import com.portal.universe.notificationservice.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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

    public void validate() {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required and must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("notification type is required");
        }
        if (title == null || title.isBlank()) {
            this.title = type.getDefaultMessage();
        }
        if (message == null || message.isBlank()) {
            this.message = type.getDefaultMessage();
        }
    }
}
