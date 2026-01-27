package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@NoXss 어노테이션 검증 테스트")
class NoXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("안전한 입력은 통과")
    void validInput() {
        // given
        TestDto dto = new TestDto("Hello World!");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("script 태그가 있으면 실패")
    void invalidInputWithScript() {
        // given
        TestDto dto = new TestDto("<script>alert('XSS')</script>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("HTML/Script 태그는 허용되지 않습니다");
    }

    @Test
    @DisplayName("이벤트 핸들러가 있으면 실패")
    void invalidInputWithEventHandler() {
        // given
        TestDto dto = new TestDto("<img onerror='alert(1)'>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("null 값은 통과 (@NotNull과 함께 사용)")
    void nullValueIsValid() {
        // given
        TestDto dto = new TestDto(null);

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열은 통과")
    void emptyStringIsValid() {
        // given
        TestDto dto = new TestDto("");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    // 테스트용 DTO
    record TestDto(@NoXss String value) {
    }
}
