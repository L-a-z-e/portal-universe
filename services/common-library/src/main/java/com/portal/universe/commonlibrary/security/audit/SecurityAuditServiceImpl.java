package com.portal.universe.commonlibrary.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 보안 감사 로그 서비스의 기본 구현체입니다.
 * SLF4J를 통해 JSON 형식으로 로그를 출력합니다.
 */
@Slf4j
@Service
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final ObjectMapper objectMapper;

    public SecurityAuditServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void log(SecurityAuditEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("SECURITY_AUDIT: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SecurityAuditEvent: {}", event, e);
        }
    }

    @Override
    public void logLoginSuccess(String userId, String username, String ip, String userAgent) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.LOGIN_SUCCESS)
                .userId(userId)
                .username(username)
                .ipAddress(ip)
                .userAgent(userAgent)
                .success(true)
                .build();

        log(event);
    }

    @Override
    public void logLoginFailure(String username, String ip, String reason) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.LOGIN_FAILURE)
                .username(username)
                .ipAddress(ip)
                .success(false)
                .errorMessage(reason)
                .build();

        log(event);
    }

    @Override
    public void logLogout(String userId, String ip) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.LOGOUT)
                .userId(userId)
                .ipAddress(ip)
                .success(true)
                .build();

        log(event);
    }

    @Override
    public void logAccessDenied(String userId, String uri, String requiredRole) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.ACCESS_DENIED)
                .userId(userId)
                .requestUri(uri)
                .success(false)
                .build();

        event.addDetail("requiredRole", requiredRole);
        log(event);
    }

    @Override
    public void logAdminAction(String adminId, String action, String targetResource) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.ADMIN_ACTION)
                .userId(adminId)
                .success(true)
                .build();

        event.addDetail("action", action);
        event.addDetail("targetResource", targetResource);
        log(event);
    }

    @Override
    public void logTokenRefresh(String userId, String ip) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.TOKEN_REFRESH)
                .userId(userId)
                .ipAddress(ip)
                .success(true)
                .build();

        log(event);
    }

    @Override
    public void logPasswordChanged(String userId, String ip) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.PASSWORD_CHANGED)
                .userId(userId)
                .ipAddress(ip)
                .success(true)
                .build();

        log(event);
    }

    @Override
    public void logSensitiveDataAccess(String userId, String resourceType, String resourceId) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.SENSITIVE_DATA_ACCESS)
                .userId(userId)
                .success(true)
                .build();

        event.addDetail("resourceType", resourceType);
        event.addDetail("resourceId", resourceId);
        log(event);
    }
}
