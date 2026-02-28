package com.portal.universe.commonlibrary.security.xss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XssUtils 테스트")
class XssUtilsTest {

    @Nested
    @DisplayName("escape()")
    class Escape {

        @Test
        @DisplayName("HTML 특수문자를 entity로 변환")
        void escapesSpecialChars() {
            String input = "<script>alert('XSS')</script>";
            String result = XssUtils.escape(input);
            assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;");
        }

        @Test
        @DisplayName("null과 빈 문자열은 그대로 반환")
        void handlesNullAndEmpty() {
            assertThat(XssUtils.escape(null)).isNull();
            assertThat(XssUtils.escape("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("stripTags()")
    class StripTags {

        @Test
        @DisplayName("HTML 태그를 제거하고 텍스트만 반환")
        void stripsAllTags() {
            String input = "<p>Hello <b>World</b></p>";
            String result = XssUtils.stripTags(input);
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("이중 디코딩 공격 방어 — entity를 디코딩하지 않음")
        void preventsDoubleDecodeAttack() {
            String input = "&lt;script&gt;alert('XSS')&lt;/script&gt;";
            String result = XssUtils.stripTags(input);
            // entity가 그대로 유지되어야 함 (디코딩하면 <script> 태그가 복원됨)
            assertThat(result).isEqualTo("&lt;script&gt;alert('XSS')&lt;/script&gt;");
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("null 처리")
        void handlesNull() {
            assertThat(XssUtils.stripTags(null)).isNull();
        }
    }

    @Nested
    @DisplayName("sanitize() — OWASP 기반")
    class Sanitize {

        @Test
        @DisplayName("허용된 태그만 남기고 나머지 제거")
        void keepsAllowedTags() {
            String input = "<p>Hello</p><script>alert('XSS')</script><b>World</b>";
            String result = XssUtils.sanitize(input, "p", "b");
            assertThat(result).contains("<p>Hello</p>");
            assertThat(result).contains("<b>World</b>");
            assertThat(result).doesNotContain("script");
            assertThat(result).doesNotContain("alert");
        }

        @Test
        @DisplayName("javascript: 프로토콜 차단")
        void blocksJavascriptProtocol() {
            String input = "<a href=\"javascript:alert(1)\">Click</a>";
            String result = XssUtils.sanitize(input, "a");
            assertThat(result).doesNotContain("javascript");
        }

        @Test
        @DisplayName("HTML entity 인코딩 우회 차단 — &#106;avascript:")
        void blocksEntityEncodedJavascript() {
            String input = "<a href=\"&#106;avascript:alert(1)\">Click</a>";
            String result = XssUtils.sanitize(input, "a");
            assertThat(result).doesNotContain("javascript");
            assertThat(result).doesNotContain("&#106;avascript");
        }

        @Test
        @DisplayName("on* 이벤트 핸들러 속성 제거")
        void stripsEventHandlers() {
            String input = "<p onclick=\"alert(1)\">Hello</p>";
            String result = XssUtils.sanitize(input, "p");
            assertThat(result).contains("<p>");
            assertThat(result).doesNotContain("onclick");
        }

        @Test
        @DisplayName("img onerror 이벤트 제거")
        void stripsImgOnerror() {
            String input = "<img src=\"x\" onerror=\"alert(1)\">";
            String result = XssUtils.sanitize(input, "img");
            assertThat(result).doesNotContain("onerror");
        }

        @Test
        @DisplayName("data: 프로토콜 차단")
        void blocksDataProtocol() {
            String input = "<a href=\"data:text/html,<script>alert(1)</script>\">Click</a>";
            String result = XssUtils.sanitize(input, "a");
            assertThat(result).doesNotContain("data:");
        }

        @Test
        @DisplayName("style 속성 제거")
        void stripsStyleAttribute() {
            String input = "<p style=\"background:url('javascript:alert(1)')\">Text</p>";
            String result = XssUtils.sanitize(input, "p");
            assertThat(result).doesNotContain("style");
            assertThat(result).doesNotContain("javascript");
        }

        @Test
        @DisplayName("null과 빈 문자열 처리")
        void handlesNullAndEmpty() {
            assertThat(XssUtils.sanitize(null, "p")).isNull();
            assertThat(XssUtils.sanitize("", "p")).isEmpty();
        }
    }

    @Nested
    @DisplayName("containsXssPattern()")
    class ContainsXssPattern {

        @ParameterizedTest
        @ValueSource(strings = {
                "<script>alert(1)</script>",
                "<img src=x>",
                "<a href='test'>link</a>",
                "<div>content</div>",
                "<!DOCTYPE html>"
        })
        @DisplayName("HTML 태그가 포함된 입력은 true")
        void detectsHtmlTags(String input) {
            assertThat(XssUtils.containsXssPattern(input)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Hello World! 안녕하세요 123",
                "5 < 10 > 3",
                "price: $5.99 & tax",
                "&lt;script&gt;",
                ""
        })
        @DisplayName("HTML 태그가 없는 입력은 false")
        void acceptsSafeInput(String input) {
            assertThat(XssUtils.containsXssPattern(input)).isFalse();
        }

        @Test
        @DisplayName("null은 false")
        void handlesNull() {
            assertThat(XssUtils.containsXssPattern(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isSafe()")
    class IsSafe {

        @Test
        @DisplayName("안전한 평문은 true")
        void safeInput() {
            assertThat(XssUtils.isSafe("Hello World! 안녕하세요 123")).isTrue();
        }

        @Test
        @DisplayName("HTML 태그 포함은 false")
        void unsafeInput() {
            assertThat(XssUtils.isSafe("<b>bold</b>")).isFalse();
        }

        @Test
        @DisplayName("null은 true")
        void handlesNull() {
            assertThat(XssUtils.isSafe(null)).isTrue();
        }
    }
}
