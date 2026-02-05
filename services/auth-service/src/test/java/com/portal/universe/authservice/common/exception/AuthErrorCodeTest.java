package com.portal.universe.authservice.common.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthErrorCode Test")
class AuthErrorCodeTest {

    @Test
    @DisplayName("각 에러 코드는 올바른 HTTP 상태를 가진다")
    void should_haveCorrectHttpStatus_forEachCode() {
        assertThat(AuthErrorCode.EMAIL_ALREADY_EXISTS.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(AuthErrorCode.INVALID_CREDENTIALS.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(AuthErrorCode.USER_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(AuthErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(AuthErrorCode.MEMBERSHIP_EXPIRED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("모든 에러 코드 값은 유니크하다")
    void should_haveUniqueCodeValues() {
        Set<String> codes = Arrays.stream(AuthErrorCode.values())
                .map(AuthErrorCode::getCode)
                .collect(Collectors.toSet());

        assertThat(codes).hasSameSizeAs(AuthErrorCode.values());
    }

    @Test
    @DisplayName("모든 에러 코드에 비어있지 않은 메시지가 있다")
    void should_haveNonEmptyMessage() {
        for (AuthErrorCode errorCode : AuthErrorCode.values()) {
            assertThat(errorCode.getMessage())
                    .as("Error code %s should have a message", errorCode.name())
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("모든 에러 코드가 'A' 접두사를 가진다")
    void should_haveAuthPrefix() {
        for (AuthErrorCode errorCode : AuthErrorCode.values()) {
            assertThat(errorCode.getCode())
                    .as("Error code %s should start with 'A'", errorCode.name())
                    .startsWith("A");
        }
    }
}
