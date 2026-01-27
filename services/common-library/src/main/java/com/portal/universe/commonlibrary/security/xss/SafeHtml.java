package com.portal.universe.commonlibrary.security.xss;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 허용된 HTML 태그만 포함하는지 검증하는 어노테이션입니다.
 * 블로그 게시글처럼 제한적인 HTML 마크업이 필요한 경우 사용합니다.
 *
 * <p>사용 예시:</p>
 * <pre>
 * public record PostCreateRequest(
 *     &#64;SafeHtml(allowedTags = {"p", "br", "b", "i", "a", "img"})
 *     String content
 * ) {}
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeHtmlValidator.class)
@Documented
public @interface SafeHtml {

    /**
     * 검증 실패 시 반환될 메시지
     */
    String message() default "허용되지 않은 HTML 태그가 포함되어 있습니다";

    /**
     * 허용할 HTML 태그 목록
     * 기본값: 안전한 텍스트 포맷 태그만 허용
     */
    String[] allowedTags() default {"p", "br", "b", "i", "u", "strong", "em", "ul", "ol", "li"};

    /**
     * 검증 그룹 지정
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드 타입 지정
     */
    Class<? extends Payload>[] payload() default {};
}
