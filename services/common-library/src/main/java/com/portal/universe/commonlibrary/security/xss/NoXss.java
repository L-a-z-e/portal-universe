package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * XSS(Cross-Site Scripting) 공격 패턴이 포함되지 않았는지 검증하는 어노테이션입니다.
 * 일반 텍스트 입력 필드에 사용하며, HTML 태그 및 스크립트가 허용되지 않습니다.
 *
 * <p>사용 예시:</p>
 * <pre>
 * public record UserRequest(
 *     &#64;NoXss
 *     &#64;NotBlank
 *     String username,
 *
 *     &#64;NoXss
 *     String comment
 * ) {}
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoXssValidator.class)
@Documented
public @interface NoXss {

    /**
     * 검증 실패 시 반환될 메시지
     */
    String message() default "HTML/Script 태그는 허용되지 않습니다";

    /**
     * 검증 그룹 지정
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드 타입 지정
     */
    Class<? extends Payload>[] payload() default {};
}
