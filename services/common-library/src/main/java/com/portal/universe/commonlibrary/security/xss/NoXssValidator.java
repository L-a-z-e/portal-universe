package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link NoXss} 어노테이션의 검증 로직을 구현하는 클래스입니다.
 * XSS 공격 패턴이 포함되어 있는지 검사합니다.
 */
public class NoXssValidator implements ConstraintValidator<NoXss, String> {

    @Override
    public void initialize(NoXss constraintAnnotation) {
        // 초기화 로직 (필요시 구현)
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

        // XSS 패턴이 없으면 유효
        return XssUtils.isSafe(value);
    }
}
