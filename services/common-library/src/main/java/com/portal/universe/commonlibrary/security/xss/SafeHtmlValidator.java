package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link SafeHtml} 어노테이션의 검증 로직을 구현하는 클래스입니다.
 * 허용된 HTML 태그만 포함되어 있는지, XSS 위험 요소가 없는지 검사합니다.
 */
public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, String> {

    private Set<String> allowedTags;
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*>");

    @Override
    public void initialize(SafeHtml constraintAnnotation) {
        this.allowedTags = Arrays.stream(constraintAnnotation.allowedTags())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 값은 @NotNull, @NotBlank 등 다른 검증 어노테이션에서 처리
        if (value == null) {
            return true;
        }

        // 빈 문자열은 통과
        if (value.isEmpty()) {
            return true;
        }

        // XSS 위험 패턴이 있으면 실패
        if (XssUtils.containsXssPattern(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "XSS 위험 패턴이 감지되었습니다 (script, iframe, event handler 등)"
            ).addConstraintViolation();
            return false;
        }

        // 허용되지 않은 태그가 있는지 검사
        Matcher matcher = TAG_PATTERN.matcher(value);
        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            if (!allowedTags.contains(tagName)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("허용되지 않은 HTML 태그가 포함되어 있습니다: <%s>", tagName)
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
