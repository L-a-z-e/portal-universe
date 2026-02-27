package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Nested
    @DisplayName("기본 허용 태그")
    class DefaultAllowedTags {

        @Test
        @DisplayName("허용된 태그만 있으면 통과")
        void validInputWithAllowedTags() {
            TestDto dto = new TestDto("<p>Hello <b>World</b></p>");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("허용되지 않은 태그가 있으면 실패")
        void invalidInputWithDisallowedTags() {
            TestDto dto = new TestDto("<p>Hello <script>alert('XSS')</script></p>");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("허용되지 않은 일반 태그도 실패")
        void invalidInputWithOtherDisallowedTags() {
            TestDto dto = new TestDto("<p>Hello <div>World</div></p>");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("null 값은 통과")
        void nullValueIsValid() {
            TestDto dto = new TestDto(null);
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열은 통과")
        void emptyStringIsValid() {
            TestDto dto = new TestDto("");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("순수 텍스트는 통과")
        void plainTextIsValid() {
            TestDto dto = new TestDto("Hello World! 안녕하세요");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("커스텀 허용 태그 (a, img 포함)")
    class CustomAllowedTags {

        @Test
        @DisplayName("허용 태그 + 안전한 속성은 통과")
        void validCustomTags() {
            CustomDto dto = new CustomDto("<p>Hello</p><img src=\"test.jpg\" alt=\"test\">");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("a 태그 + https 링크 통과")
        void validHttpsLink() {
            CustomDto dto = new CustomDto("<a href=\"https://example.com\">Link</a>");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("XSS 공격 벡터 차단")
    class XssAttackVectors {

        @Test
        @DisplayName("이벤트 핸들러가 있으면 실패")
        void rejectsEventHandler() {
            TestDto dto = new TestDto("<p onclick='alert(1)'>Click</p>");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("javascript: 프로토콜 차단")
        void rejectsJavascriptProtocol() {
            CustomDto dto = new CustomDto("<a href=\"javascript:alert(1)\">Click</a>");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("HTML entity 인코딩 우회 차단 — &#106;avascript:")
        void rejectsEntityEncodedJavascript() {
            CustomDto dto = new CustomDto("<a href=\"&#106;avascript:alert(1)\">Click</a>");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("data: 프로토콜 차단")
        void rejectsDataProtocol() {
            CustomDto dto = new CustomDto("<a href=\"data:text/html,<script>alert(1)</script>\">Click</a>");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("img onerror 이벤트 차단")
        void rejectsImgOnerror() {
            CustomDto dto = new CustomDto("<img src=\"x\" onerror=\"alert(1)\">");
            Set<ConstraintViolation<CustomDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "<script>alert(1)</script>",
                "<iframe src='evil.com'></iframe>",
                "<object data='evil.swf'></object>",
                "<embed src='evil.swf'>",
                "<meta http-equiv='refresh' content='0;url=evil.com'>",
                "<base href='evil.com'>"
        })
        @DisplayName("위험한 태그 전면 차단")
        void rejectsDangerousTags(String dangerousHtml) {
            TestDto dto = new TestDto(dangerousHtml);
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("style 속성의 javascript 차단")
        void rejectsStyleJavascript() {
            TestDto dto = new TestDto("<p style=\"background:url('javascript:alert(1)')\">Text</p>");
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
        }
    }

    // 기본 허용 태그: p, br, b, i, u, strong, em, ul, ol, li
    record TestDto(@SafeHtml String content) {
    }

    // 커스텀 허용 태그: a, img 포함
    record CustomDto(@SafeHtml(allowedTags = {"p", "img", "a"}) String content) {
    }
}
