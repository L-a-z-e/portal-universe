package com.portal.universe.authservice.password;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResult Test")
class ValidationResultTest {

    @Test
    @DisplayName("success()는 valid=true, 빈 에러 목록을 반환한다")
    void should_beValid_when_success() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("failure(List)는 valid=false, 에러 목록을 반환한다")
    void should_beInvalid_when_failureWithList() {
        ValidationResult result = ValidationResult.failure(List.of("error1", "error2"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsExactly("error1", "error2");
    }

    @Test
    @DisplayName("failure(String)는 valid=false, 단일 에러를 반환한다")
    void should_beInvalid_when_failureWithString() {
        ValidationResult result = ValidationResult.failure("single error");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsExactly("single error");
    }

    @Test
    @DisplayName("getFirstError()는 첫 번째 에러를 반환한다")
    void should_returnFirstError() {
        ValidationResult result = ValidationResult.failure(List.of("first", "second"));

        assertThat(result.getFirstError()).isEqualTo("first");
    }

    @Test
    @DisplayName("성공 결과의 getFirstError()는 null을 반환한다")
    void should_returnNull_when_successGetFirstError() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.getFirstError()).isNull();
    }
}
