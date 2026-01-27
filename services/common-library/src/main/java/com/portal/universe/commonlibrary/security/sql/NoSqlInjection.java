package com.portal.universe.commonlibrary.security.sql;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SQL Injection 공격 패턴이 포함되지 않았는지 검증하는 어노테이션입니다.
 * 검색어, 정렬 필드 등 동적 쿼리에 사용되는 파라미터에 적용합니다.
 *
 * <p>사용 예시:</p>
 * <pre>
 * public record SearchRequest(
 *     &#64;NoSqlInjection
 *     String keyword,
 *
 *     &#64;NoSqlInjection
 *     String sortField
 * ) {}
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSqlInjectionValidator.class)
@Documented
public @interface NoSqlInjection {

    /**
     * 검증 실패 시 반환될 메시지
     */
    String message() default "SQL Injection 위험 패턴이 감지되었습니다";

    /**
     * 검증 그룹 지정
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드 타입 지정
     */
    Class<? extends Payload>[] payload() default {};
}
