package com.portal.universe.commonlibrary.security.xss;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * XSS(Cross-Site Scripting) 방어를 위한 유틸리티 클래스입니다.
 * HTML 이스케이프, 태그 제거, 허용된 태그만 유지하는 기능을 제공합니다.
 */
public class XssUtils {

    /**
     * XSS 공격 패턴을 감지하는 정규식 목록
     */
    private static final Pattern[] XSS_PATTERNS = {
            // Script 태그
            Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            // 이벤트 핸들러
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // JavaScript 프로토콜
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            // iframe, embed, object
            Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
            // style 태그 및 expression
            Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            // 기타 위험 요소
            Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE)
    };

    /**
     * HTML 태그를 제거하는 정규식
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private XssUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * HTML 특수 문자를 이스케이프 처리합니다.
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
     *
     * @param input 원본 문자열
     * @return 태그가 제거된 문자열
     */
    public static String stripTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // HTML 태그 제거
        String result = HTML_TAG_PATTERN.matcher(input).replaceAll("");

        // HTML 엔티티 디코딩 (간단한 케이스만)
        return result.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/");
    }

    /**
     * 허용된 태그만 남기고 나머지는 제거합니다.
     * 블로그 게시글 등 제한적인 HTML을 허용해야 하는 경우 사용합니다.
     *
     * @param input       원본 문자열
     * @param allowedTags 허용할 태그 목록 (예: "p", "br", "b", "i")
     * @return 정제된 문자열
     */
    public static String sanitize(String input, String... allowedTags) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Set<String> allowedTagSet = Arrays.stream(allowedTags)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        String result = input;

        // XSS 위험 패턴 제거
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }

        // 허용되지 않은 태그 제거
        Pattern tagPattern = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*>");
        Matcher tagMatcher = tagPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (tagMatcher.find()) {
            String closingSlash = tagMatcher.group(1);
            String tagName = tagMatcher.group(2).toLowerCase();

            if (allowedTagSet.contains(tagName)) {
                // 허용된 태그의 경우, 속성은 제거하고 태그만 유지
                tagMatcher.appendReplacement(sb, "<" + closingSlash + tagName + ">");
            } else {
                // 허용되지 않은 태그는 제거
                tagMatcher.appendReplacement(sb, "");
            }
        }
        tagMatcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    /**
     * 입력값에 XSS 공격 패턴이 포함되어 있는지 검사합니다.
     *
     * @param input 검사할 문자열
     * @return XSS 패턴이 감지되면 true, 아니면 false
     */
    public static boolean containsXssPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 입력값이 안전한지 검사합니다.
     * XSS 패턴이 없으면 true를 반환합니다.
     *
     * @param input 검사할 문자열
     * @return 안전하면 true, 위험하면 false
     */
    public static boolean isSafe(String input) {
        return !containsXssPattern(input);
    }
}
