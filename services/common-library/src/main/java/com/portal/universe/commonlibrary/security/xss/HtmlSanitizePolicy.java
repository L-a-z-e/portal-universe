package com.portal.universe.commonlibrary.security.xss;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OWASP Java HTML Sanitizer 기반 정책 팩토리.
 *
 * <p>{@link PolicyFactory}는 {@code @ThreadSafe} + {@code @Immutable}이므로
 * static final 또는 캐시에서 안전하게 재사용됩니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * // 모든 태그 제거 (순수 텍스트)
 * String text = HtmlSanitizePolicy.TEXT_ONLY.sanitize(untrustedHtml);
 *
 * // 허용 태그만 남기기
 * PolicyFactory policy = HtmlSanitizePolicy.buildPolicy(Set.of("p", "b", "a"));
 * String safe = policy.sanitize(untrustedHtml);
 * </pre>
 */
public class HtmlSanitizePolicy {

    /** 모든 태그 제거 — 순수 텍스트만 반환 */
    public static final PolicyFactory TEXT_ONLY = new HtmlPolicyBuilder().toFactory();

    /** 태그 세트별 PolicyFactory 캐시 (동일 태그 조합은 재사용) */
    private static final ConcurrentHashMap<Set<String>, PolicyFactory> POLICY_CACHE =
            new ConcurrentHashMap<>();

    /**
     * 태그별 자동 허용 속성.
     * a, img 등 URL 속성을 가지는 태그는 프로토콜 화이트리스트(http, https)도 적용됩니다.
     */
    private static final Map<String, String[]> TAG_ATTRIBUTES = Map.of(
            "a", new String[]{"href", "title", "target"},
            "img", new String[]{"src", "alt", "title", "width", "height"}
    );

    private HtmlSanitizePolicy() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 허용 태그 목록으로 PolicyFactory를 생성하거나 캐시에서 반환합니다.
     *
     * <p>a, img 태그가 포함된 경우 자동으로:</p>
     * <ul>
     *   <li>안전한 속성만 허용 (href, src, alt 등)</li>
     *   <li>URL 프로토콜은 http, https만 허용 (javascript:, data: 등 차단)</li>
     *   <li>a 태그에 rel="nofollow noopener" 자동 추가</li>
     * </ul>
     *
     * @param allowedTags 허용할 태그 이름 세트 (소문자)
     * @return 재사용 가능한 PolicyFactory
     */
    public static PolicyFactory buildPolicy(Set<String> allowedTags) {
        return POLICY_CACHE.computeIfAbsent(Set.copyOf(allowedTags), HtmlSanitizePolicy::createPolicy);
    }

    private static PolicyFactory createPolicy(Set<String> allowedTags) {
        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();

        builder.allowElements(allowedTags.toArray(String[]::new));

        for (String tag : allowedTags) {
            String[] attrs = TAG_ATTRIBUTES.get(tag);
            if (attrs != null) {
                builder.allowAttributes(attrs).onElements(tag);
            }
        }

        builder.allowUrlProtocols("https", "http");

        if (allowedTags.contains("a")) {
            builder.requireRelNofollowOnLinks();
        }

        return builder.toFactory();
    }
}
