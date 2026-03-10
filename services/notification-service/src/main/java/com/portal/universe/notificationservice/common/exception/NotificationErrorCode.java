package com.portal.universe.notificationservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Notification 서비스 오류 코드 정의.
 * N0XX: Notification
 */
@Getter
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "Notification not found"),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "Notification send failed"),
    INVALID_NOTIFICATION_TYPE(HttpStatus.BAD_REQUEST, "N003", "Invalid notification type");

    private final HttpStatus status;
    private final String code;
    private final String message;

    NotificationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
