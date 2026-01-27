package com.portal.universe.commonlibrary.security.xss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XssUtils 테스트")
class XssUtilsTest {

    @Test
    @DisplayName("HTML 이스케이프 처리")
    void escape() {
        // given
        String input = "<script>alert('XSS')</script>";

        // when
        String result = XssUtils.escape(input);

        // then
        assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;");
    }

    @Test
    @DisplayName("HTML 태그 제거")
    void stripTags() {
        // given
        String input = "<p>Hello <b>World</b></p>";

        // when
        String result = XssUtils.stripTags(input);

        // then
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("허용된 태그만 유지")
    void sanitize() {
        // given
        String input = "<p>Hello</p><script>alert('XSS')</script><b>World</b>";

        // when
        String result = XssUtils.sanitize(input, "p", "b");

        // then
        assertThat(result)
                .contains("<p>Hello</p>")
                .contains("<b>World</b>")
                .doesNotContain("script");
    }

    @Test
    @DisplayName("script 태그 탐지")
    void detectScriptTag() {
        // given
        String input = "<script>alert('XSS')</script>";

        // when
        boolean result = XssUtils.containsXssPattern(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이벤트 핸들러 탐지")
    void detectEventHandler() {
        // given
        String input = "<img src='x' onerror='alert(1)'>";

        // when
        boolean result = XssUtils.containsXssPattern(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("javascript: 프로토콜 탐지")
    void detectJavascriptProtocol() {
        // given
        String input = "<a href='javascript:alert(1)'>Click</a>";

        // when
        boolean result = XssUtils.containsXssPattern(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("안전한 입력은 통과")
    void safeInput() {
        // given
        String input = "Hello World! 안녕하세요 123";

        // when
        boolean result = XssUtils.isSafe(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("null과 빈 문자열 처리")
    void handleNullAndEmpty() {
        assertThat(XssUtils.escape(null)).isNull();
        assertThat(XssUtils.escape("")).isEmpty();
        assertThat(XssUtils.stripTags(null)).isNull();
        assertThat(XssUtils.containsXssPattern(null)).isFalse();
        assertThat(XssUtils.isSafe(null)).isTrue();
    }
}
