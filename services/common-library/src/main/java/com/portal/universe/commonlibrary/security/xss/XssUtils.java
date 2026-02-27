package com.portal.universe.commonlibrary.security.xss;

import org.owasp.html.PolicyFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * XSS(Cross-Site Scripting) 방어를 위한 유틸리티 클래스입니다.
 *
 * <ul>
 *   <li>{@link #escape(String)}: 출력 인코딩 (HTML 특수문자 → entity)</li>
 *   <li>{@link #stripTags(String)}: 모든 HTML 태그 제거 → 순수 텍스트</li>
 *   <li>{@link #sanitize(String, String...)}: OWASP 기반 — 허용 태그만 남기고 위험 속성/프로토콜 제거</li>
 *   <li>{@link #containsXssPattern(String)}: HTML 태그 포함 여부 탐지</li>
 * </ul>
 */
public class XssUtils {

    /**
     * HTML 태그 탐지 패턴.
     * &lt; 뒤에 알파벳, /, !가 오는 경우만 태그로 인식합니다.
     * 수학 표현식 {@code 5 < 10 > 3} 같은 오탐을 방지합니다.
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[a-zA-Z/!][^>]*>");

    private XssUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * HTML 특수 문자를 이스케이프 처리합니다.
     * 출력 단계에서 XSS를 방지하는 용도로 사용합니다.
     *
     * @param input 원본 문자열
     * @return 이스케이프 처리된 문자열
     */
    public static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * 모든 HTML 태그를 제거하고 순수 텍스트만 반환합니다.
     * HTML entity는 디코딩하지 않습니다 (이중 디코딩 공격 방지).
     *
     * @param input 원본 문자열
     * @return 태그가 제거된 문자열
     */
    public static String stripTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return HTML_TAG_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * OWASP HTML Sanitizer를 사용하여 허용된 태그만 남기고 나머지를 제거합니다.
     *
     * <p>DOM 기반 화이트리스트 방식으로 동작합니다:</p>
     * <ul>
     *   <li>허용되지 않은 태그 제거</li>
     *   <li>위험한 속성 제거 (on* 이벤트 핸들러 등)</li>
     *   <li>URL 프로토콜 화이트리스트 (http, https만 허용 — javascript:, data: 차단)</li>
     *   <li>HTML entity 인코딩 우회 자동 방어 (파서 레벨 처리)</li>
     * </ul>
     *
     * @param input       원본 문자열
     * @param allowedTags 허용할 태그 목록 (예: "p", "br", "b", "i", "a")
     * @return 정제된 HTML 문자열
     */
    public static String sanitize(String input, String... allowedTags) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Set<String> allowedTagSet = Arrays.stream(allowedTags)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        PolicyFactory policy = HtmlSanitizePolicy.buildPolicy(allowedTagSet);
        return policy.sanitize(input);
    }

    /**
     * 입력값에 HTML 태그가 포함되어 있는지 검사합니다.
     * {@code @NoXss} 어노테이션의 검증에 사용됩니다.
     *
     * @param input 검사할 문자열
     * @return HTML 태그가 감지되면 true
     */
    public static boolean containsXssPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return HTML_TAG_PATTERN.matcher(input).find();
    }

    /**
     * 입력값이 안전한지 검사합니다 (HTML 태그 미포함).
     *
     * @param input 검사할 문자열
     * @return HTML 태그가 없으면 true
     */
    public static boolean isSafe(String input) {
        return !containsXssPattern(input);
    }
}
