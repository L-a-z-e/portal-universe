package com.portal.universe.blogservice.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlogErrorCode 테스트")
class BlogErrorCodeTest {

    @Test
    @DisplayName("should_haveUniqueCode")
    void should_haveUniqueCode() {
        // given
        BlogErrorCode[] errorCodes = BlogErrorCode.values();
        Set<String> codes = new HashSet<>();

        // when & then
        for (BlogErrorCode errorCode : errorCodes) {
            boolean added = codes.add(errorCode.getCode());
            assertThat(added)
                    .withFailMessage("Duplicate error code found: %s", errorCode.getCode())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("should_startWithB")
    void should_startWithB() {
        // given
        BlogErrorCode[] errorCodes = BlogErrorCode.values();

        // when & then
        for (BlogErrorCode errorCode : errorCodes) {
            assertThat(errorCode.getCode())
                    .withFailMessage("Error code %s should start with 'B'", errorCode.getCode())
                    .startsWith("B");
        }
    }

    @Test
    @DisplayName("should_haveValidHttpStatus")
    void should_haveValidHttpStatus() {
        // given
        BlogErrorCode[] errorCodes = BlogErrorCode.values();

        // when & then
        for (BlogErrorCode errorCode : errorCodes) {
            assertThat(errorCode.getStatus())
                    .withFailMessage("Error code %s has null HttpStatus", errorCode.getCode())
                    .isNotNull()
                    .isInstanceOf(HttpStatus.class);
        }
    }

    @Test
    @DisplayName("should_haveNonEmptyMessage")
    void should_haveNonEmptyMessage() {
        // given
        BlogErrorCode[] errorCodes = BlogErrorCode.values();

        // when & then
        for (BlogErrorCode errorCode : errorCodes) {
            assertThat(errorCode.getMessage())
                    .withFailMessage("Error code %s has empty message", errorCode.getCode())
                    .isNotNull()
                    .isNotBlank();
        }
    }
}
