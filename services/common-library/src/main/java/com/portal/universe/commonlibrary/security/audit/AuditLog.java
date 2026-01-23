package com.portal.universe.commonlibrary.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드에 보안 감사 로그를 자동으로 기록하도록 하는 어노테이션입니다.
 * AOP를 통해 메서드 실행 전후에 로그가 기록됩니다.
 *
 * <p>사용 예시:
 * <pre>
 * {@code
 * @AuditLog(eventType = SecurityAuditEventType.ADMIN_ACTION, description = "사용자 권한 변경")
 * public void updateUserRole(String userId, String newRole) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 기록할 보안 감사 이벤트 유형
     */
    SecurityAuditEventType eventType();

    /**
     * 이벤트에 대한 상세 설명 (선택 사항)
     */
    String description() default "";
}
