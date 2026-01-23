package com.portal.universe.commonlibrary.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * @AuditLog 어노테이션이 적용된 메서드에 대해 보안 감사 로그를 자동으로 기록하는 Aspect입니다.
 * 메서드 실행 전후에 로그를 기록하며, 예외 발생 시에도 실패 이벤트를 로깅합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SecurityAuditService securityAuditService;

    /**
     * @AuditLog 어노테이션이 적용된 메서드를 가로채어 감사 로그를 기록합니다.
     *
     * @param joinPoint 실행 중인 메서드에 대한 정보
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(com.portal.universe.commonlibrary.security.audit.AuditLog)")
    public Object logAuditEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        // 요청 정보 추출
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = getClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String requestUri = request != null ? request.getRequestURI() : null;
        String requestMethod = request != null ? request.getMethod() : null;

        // 사용자 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : "anonymous";

        // 이벤트 생성
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(auditLog.eventType())
                .userId(userId)
                .username(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestUri(requestUri)
                .requestMethod(requestMethod)
                .build();

        // description이 있으면 details에 추가
        if (!auditLog.description().isEmpty()) {
            event.addDetail("description", auditLog.description());
        }

        // 메서드 정보 추가
        event.addDetail("method", method.getName());
        event.addDetail("class", method.getDeclaringClass().getSimpleName());

        try {
            // 메서드 실행
            Object result = joinPoint.proceed();

            // 성공 로그
            event.setSuccess(true);
            securityAuditService.log(event);

            return result;
        } catch (Throwable throwable) {
            // 실패 로그
            event.setSuccess(false);
            event.setErrorMessage(throwable.getMessage());
            securityAuditService.log(event);

            throw throwable;
        }
    }

    /**
     * 현재 HTTP 요청을 가져옵니다.
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Failed to get HttpServletRequest: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * X-Forwarded-For 헤더를 우선 확인하며, 없으면 RemoteAddr을 사용합니다.
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For는 쉼표로 구분된 여러 IP를 포함할 수 있음 (첫 번째 IP가 원본 클라이언트)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}
