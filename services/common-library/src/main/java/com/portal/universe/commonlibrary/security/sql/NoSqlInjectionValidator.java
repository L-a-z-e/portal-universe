package com.portal.universe.commonlibrary.security.sql;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link NoSqlInjection} 어노테이션의 검증 로직을 구현하는 클래스입니다.
 * SQL Injection 공격 패턴이 포함되어 있는지 검사합니다.
 */
public class NoSqlInjectionValidator implements ConstraintValidator<NoSqlInjection, String> {

    @Override
    public void initialize(NoSqlInjection constraintAnnotation) {
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

        // SQL Injection 패턴이 없으면 유효
        return SqlInjectionUtils.isSafe(value);
    }
}
