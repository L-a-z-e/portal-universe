package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * {@link SafeHtml} 어노테이션의 검증 로직을 구현하는 클래스입니다.
 *
 * <p>OWASP Java HTML Sanitizer를 사용하여 DOM 기반 화이트리스트 검증을 수행합니다.
 * {@link HtmlChangeListener}로 제거된 태그/속성을 감지하여, 위험한 콘텐츠 포함 여부를 판단합니다.</p>
 *
 * <p>방어 범위:</p>
 * <ul>
 *   <li>허용되지 않은 태그 (script, iframe 등)</li>
 *   <li>위험한 속성 (on* 이벤트 핸들러, style 등)</li>
 *   <li>위험한 URL 프로토콜 (javascript:, data:, vbscript:)</li>
 *   <li>HTML entity 인코딩 우회 ({@code &#106;avascript:} 등)</li>
 * </ul>
 */
public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, String> {

    private Set<String> allowedTags;

    @Override
    public void initialize(SafeHtml constraintAnnotation) {
        this.allowedTags = Arrays.stream(constraintAnnotation.allowedTags())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        PolicyFactory policy = HtmlSanitizePolicy.buildPolicy(allowedTags);
        AtomicBoolean hasUnsafeContent = new AtomicBoolean(false);

        policy.sanitize(value, new HtmlChangeListener<Void>() {
            @Override
            public void discardedTag(Void ctx, String elementName) {
                hasUnsafeContent.set(true);
            }

            @Override
            public void discardedAttributes(Void ctx, String tagName, String... attributeNames) {
                hasUnsafeContent.set(true);
            }
        }, null);

        if (hasUnsafeContent.get()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "안전하지 않은 HTML 콘텐츠가 포함되어 있습니다"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
