package com.portal.universe.notificationservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationErrorCode")
class NotificationErrorCodeTest {

    @Test
    @DisplayName("NOTIFICATION_NOT_FOUND는 N001 코드와 404 상태를 갖는다")
    void should_haveCorrectValues_forNotFound() {
        NotificationErrorCode code = NotificationErrorCode.NOTIFICATION_NOT_FOUND;

        assertThat(code.getCode()).isEqualTo("N001");
        assertThat(code.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code.getMessage()).isEqualTo("Notification not found");
    }

    @Test
    @DisplayName("NOTIFICATION_SEND_FAILED는 N002 코드와 500 상태를 갖는다")
    void should_haveCorrectValues_forSendFailed() {
        NotificationErrorCode code = NotificationErrorCode.NOTIFICATION_SEND_FAILED;

        assertThat(code.getCode()).isEqualTo("N002");
        assertThat(code.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(code.getMessage()).isEqualTo("Notification send failed");
    }

    @Test
    @DisplayName("INVALID_NOTIFICATION_TYPE는 N003 코드와 400 상태를 갖는다")
    void should_haveCorrectValues_forInvalidType() {
        NotificationErrorCode code = NotificationErrorCode.INVALID_NOTIFICATION_TYPE;

        assertThat(code.getCode()).isEqualTo("N003");
        assertThat(code.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(code.getMessage()).isEqualTo("Invalid notification type");
    }

    @Test
    @DisplayName("ErrorCode 인터페이스를 구현한다")
    void should_implementErrorCodeInterface() {
        for (NotificationErrorCode errorCode : NotificationErrorCode.values()) {
            assertThat(errorCode).isInstanceOf(ErrorCode.class);
        }
    }

    @Test
    @DisplayName("총 3개의 에러 코드가 정의되어 있다")
    void should_haveThreeCodes() {
        assertThat(NotificationErrorCode.values()).hasSize(3);
    }
}
