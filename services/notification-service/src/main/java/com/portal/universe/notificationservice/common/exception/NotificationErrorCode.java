package com.portal.universe.notificationservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 알림(Notification) 서비스에서 발생하는 비즈니스 예외에 대한 오류 코드를 정의하는 열거형 클래스입니다.
 * 각 오류 코드는 HTTP 상태, 고유 코드, 그리고 메시지를 포함합니다.
 */
@Getter
public enum NotificationErrorCode implements ErrorCode {

    /**
     * 요청한 ID에 해당하는 알림이 존재하지 않을 경우 발생합니다.
     */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "Notification not found"),

    /**
     * 알림 전송에 실패한 경우 발생합니다.
     */
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "Notification send failed"),

    /**
     * 유효하지 않은 알림 타입인 경우 발생합니다.
     */
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
