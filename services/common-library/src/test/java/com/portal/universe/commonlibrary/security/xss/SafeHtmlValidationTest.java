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

@DisplayName("@SafeHtml 어노테이션 검증 테스트")
class SafeHtmlValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("허용된 태그만 있으면 통과")
    void validInputWithAllowedTags() {
        // given
        TestDto dto = new TestDto("<p>Hello <b>World</b></p>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("허용되지 않은 태그가 있으면 실패")
    void invalidInputWithDisallowedTags() {
        // given
        TestDto dto = new TestDto("<p>Hello <script>alert('XSS')</script></p>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("XSS 위험 패턴이 감지되었습니다");
    }

    @Test
    @DisplayName("허용되지 않은 일반 태그도 실패")
    void invalidInputWithOtherDisallowedTags() {
        // given
        TestDto dto = new TestDto("<p>Hello <div>World</div></p>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("허용되지 않은 HTML 태그가 포함되어 있습니다");
    }

    @Test
    @DisplayName("이벤트 핸들러가 있으면 실패")
    void invalidInputWithEventHandler() {
        // given
        TestDto dto = new TestDto("<p onclick='alert(1)'>Click</p>");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("커스텀 허용 태그 설정 테스트")
    void customAllowedTags() {
        // given
        CustomDto dto = new CustomDto("<p>Hello</p><img src='test.jpg'>");

        // when
        Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("null 값은 통과")
    void nullValueIsValid() {
        // given
        TestDto dto = new TestDto(null);

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    // 테스트용 DTO (기본 허용 태그 사용)
    record TestDto(@SafeHtml String content) {
    }

    // 테스트용 DTO (커스텀 허용 태그)
    record CustomDto(@SafeHtml(allowedTags = {"p", "img", "a"}) String content) {
    }
}
