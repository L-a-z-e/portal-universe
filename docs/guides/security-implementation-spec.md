---
id: guide-security-implementation-spec
title: 보안 강화 구현 명세서
type: guide
status: current
created: 2026-01-23
updated: 2026-01-23
author: Laze
tags: [security, rate-limiting, audit, authentication, headers]
related:
  - ADR-004
  - ADR-005
  - guide-jwt-rbac-setup
---

# 보안 강화 구현 명세서

Portal Universe 프로젝트의 보안을 강화하기 위한 구현 명세서입니다. Rate Limiting, 보안 감사 로깅, 로그인 보안, 보안 헤더 설정에 대한 구현 가이드를 제공합니다.

## 목차

1. [Rate Limiting 구현](#1-rate-limiting-구현)
2. [보안 감사 로깅 구현](#2-보안-감사-로깅-구현)
3. [로그인 보안 강화 구현](#3-로그인-보안-강화-구현)
4. [보안 헤더 구현](#4-보안-헤더-구현)
5. [파일 위치 및 의존성](#5-파일-위치-및-의존성)

---

## 1. Rate Limiting 구현

API 과부하 방지 및 DDoS 공격 완화를 위한 요청 제한 구현 방법입니다.

### 1.1 Gateway 설정

**위치**: `services/api-gateway/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth Service
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 5      # 초당 토큰 재충전 속도
                redis-rate-limiter.burstCapacity: 10     # 버스트 허용량
                redis-rate-limiter.requestedTokens: 1    # 요청당 소비 토큰 수
                key-resolver: "#{@ipKeyResolver}"        # IP 기반 키 리졸버

        # Shopping Service (더 관대한 정책)
        - id: shopping-service
          uri: lb://shopping-service
          predicates:
            - Path=/api/v1/products/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 50
                key-resolver: "#{@userKeyResolver}"

  # Redis 연결 설정
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### 1.2 엔드포인트별 Rate Limit 정책

| 엔드포인트 | replenishRate | burstCapacity | 키 전략 | 이유 |
|-----------|---------------|---------------|---------|------|
| `/api/v1/auth/login` | 3 | 5 | IP | Brute-force 공격 방지 |
| `/api/v1/auth/register` | 2 | 3 | IP | 대량 계정 생성 방지 |
| `/api/v1/auth/password-reset` | 1 | 2 | IP | 악용 방지 |
| `/api/v1/products/**` | 20 | 50 | User | 일반 조회 트래픽 허용 |
| `/api/v1/orders/**` | 10 | 20 | User | 주문 API 보호 |
| `/api/v1/admin/**` | 30 | 60 | User | Admin 작업 여유 제공 |

### 1.3 KeyResolver 구현

**위치**: `services/api-gateway/src/main/java/...config/RateLimiterConfig.java`

```java
package com.portal.universe.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter 설정
 *
 * IP 기반 또는 User 기반 KeyResolver를 제공합니다.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * IP 주소 기반 KeyResolver
     *
     * X-Forwarded-For 헤더를 우선 사용하고, 없으면 RemoteAddress 사용
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }

            return Mono.just(
                exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress()
            );
        };
    }

    /**
     * 사용자 기반 KeyResolver
     *
     * JWT 토큰에서 사용자 ID를 추출하고, 인증되지 않은 경우 IP 사용
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 1. Authorization 헤더에서 JWT 추출 시도
            String auth = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                // JWT에서 userId 추출 (실제 구현 필요)
                String userId = extractUserIdFromToken(token);
                if (userId != null) {
                    return Mono.just("user:" + userId);
                }
            }

            // 2. Fallback to IP
            return ipKeyResolver().resolve(exchange);
        };
    }

    // JWT 파싱 로직은 common-library의 JwtTokenProvider 재사용
    private String extractUserIdFromToken(String token) {
        // TODO: JwtTokenProvider를 주입받아 사용
        return null;
    }
}
```

### 1.4 Rate Limit 초과 응답 처리

**위치**: `services/api-gateway/src/main/java/...filter/RateLimitExceededFilter.java`

```java
package com.portal.universe.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 초과 시 일관된 응답 포맷 제공
 */
@Component
public class RateLimitExceededFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(throwable -> {
            if (throwable.getMessage().contains("429")) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                // Retry-After 헤더 추가 (60초 후 재시도)
                exchange.getResponse().getHeaders().add("Retry-After", "60");

                String errorResponse = """
                    {
                      "success": false,
                      "error": {
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요.",
                        "retryAfter": 60
                      }
                    }
                    """;

                return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(errorResponse.getBytes())));
            }
            return Mono.error(throwable);
        });
    }

    @Override
    public int getOrder() {
        return -1; // 높은 우선순위
    }
}
```

### 1.5 의존성

**위치**: `services/api-gateway/pom.xml`

```xml
<!-- Rate Limiting을 위한 Redis Reactive -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

---

## 2. 보안 감사 로깅 구현

보안 관련 이벤트를 기록하고 추적하기 위한 감사 로깅 시스템입니다.

### 2.1 SecurityAuditEvent 엔티티

**위치**: `services/common-library/src/main/java/...security/audit/entity/SecurityAuditEvent.java`

```java
package com.portal.universe.common.security.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "security_audit_events", indexes = {
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID eventId;

    /**
     * 이벤트 유형
     *
     * - LOGIN_SUCCESS: 로그인 성공
     * - LOGIN_FAILURE: 로그인 실패
     * - LOGOUT: 로그아웃
     * - PASSWORD_CHANGE: 비밀번호 변경
     * - PASSWORD_RESET_REQUEST: 비밀번호 재설정 요청
     * - ACCOUNT_LOCKED: 계정 잠금
     * - PERMISSION_DENIED: 권한 거부
     * - SENSITIVE_DATA_ACCESS: 민감 데이터 접근
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(length = 100)
    private String userId;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String requestUri;

    @Column(length = 10)
    private String requestMethod;

    /**
     * 추가 상세 정보 (JSON)
     *
     * 예시:
     * {
     *   "reason": "Invalid credentials",
     *   "attemptCount": 3,
     *   "resource": "/api/v1/admin/users"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        PASSWORD_CHANGE,
        PASSWORD_RESET_REQUEST,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        PERMISSION_DENIED,
        SENSITIVE_DATA_ACCESS,
        TOKEN_REFRESH,
        TWO_FACTOR_ENABLED,
        TWO_FACTOR_DISABLED
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
```

### 2.2 SecurityAuditService 인터페이스

**위치**: `services/common-library/src/main/java/...security/audit/service/SecurityAuditService.java`

```java
package com.portal.universe.common.security.audit.service;

import com.portal.universe.common.security.audit.entity.SecurityAuditEvent;
import com.portal.universe.common.security.audit.entity.SecurityAuditEvent.EventType;

import java.util.Map;

/**
 * 보안 감사 로깅 서비스
 */
public interface SecurityAuditService {

    /**
     * 보안 이벤트 로깅 (일반)
     */
    void logEvent(SecurityAuditEvent event);

    /**
     * 로그인 성공 로깅
     */
    void logLoginSuccess(String userId, String ipAddress, String userAgent);

    /**
     * 로그인 실패 로깅
     */
    void logLoginFailure(String username, String ipAddress, String userAgent, String reason);

    /**
     * 로그아웃 로깅
     */
    void logLogout(String userId, String ipAddress);

    /**
     * 권한 거부 로깅
     */
    void logPermissionDenied(String userId, String ipAddress, String requestUri, String requiredRole);

    /**
     * 계정 잠금 로깅
     */
    void logAccountLocked(String userId, String ipAddress, int attemptCount);

    /**
     * 민감 데이터 접근 로깅
     */
    void logSensitiveDataAccess(String userId, String ipAddress, String resource);

    /**
     * 커스텀 이벤트 로깅
     */
    void logCustomEvent(EventType eventType, String userId, String ipAddress, Map<String, Object> details);
}
```

### 2.3 SecurityAuditServiceImpl 구현체

**위치**: `services/common-library/src/main/java/...security/audit/service/impl/SecurityAuditServiceImpl.java`

```java
package com.portal.universe.common.security.audit.service.impl;

import com.portal.universe.common.security.audit.entity.SecurityAuditEvent;
import com.portal.universe.common.security.audit.entity.SecurityAuditEvent.EventType;
import com.portal.universe.common.security.audit.repository.SecurityAuditEventRepository;
import com.portal.universe.common.security.audit.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final SecurityAuditEventRepository repository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(SecurityAuditEvent event) {
        try {
            repository.save(event);
            log.info("Security event logged: type={}, userId={}, ip={}",
                event.getEventType(), event.getUserId(), event.getIpAddress());
        } catch (Exception e) {
            log.error("Failed to log security event: {}", event, e);
        }
    }

    @Override
    public void logLoginSuccess(String userId, String ipAddress, String userAgent) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.LOGIN_SUCCESS)
            .userId(userId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logLoginFailure(String username, String ipAddress, String userAgent, String reason) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.LOGIN_FAILURE)
            .userId(username)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .details(Map.of("reason", reason))
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logLogout(String userId, String ipAddress) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.LOGOUT)
            .userId(userId)
            .ipAddress(ipAddress)
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logPermissionDenied(String userId, String ipAddress, String requestUri, String requiredRole) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.PERMISSION_DENIED)
            .userId(userId)
            .ipAddress(ipAddress)
            .requestUri(requestUri)
            .details(Map.of("requiredRole", requiredRole))
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logAccountLocked(String userId, String ipAddress, int attemptCount) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.ACCOUNT_LOCKED)
            .userId(userId)
            .ipAddress(ipAddress)
            .details(Map.of("attemptCount", attemptCount))
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logSensitiveDataAccess(String userId, String ipAddress, String resource) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(EventType.SENSITIVE_DATA_ACCESS)
            .userId(userId)
            .ipAddress(ipAddress)
            .details(Map.of("resource", resource))
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }

    @Override
    public void logCustomEvent(EventType eventType, String userId, String ipAddress, Map<String, Object> details) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(eventType)
            .userId(userId)
            .ipAddress(ipAddress)
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();
        logEvent(event);
    }
}
```

### 2.4 AOP를 이용한 자동 로깅

**위치**: `services/common-library/src/main/java/...security/audit/annotation/AuditLog.java`

```java
package com.portal.universe.common.security.audit.annotation;

import com.portal.universe.common.security.audit.entity.SecurityAuditEvent.EventType;

import java.lang.annotation.*;

/**
 * 보안 감사 로깅 어노테이션
 *
 * 메서드에 적용 시 실행 결과를 자동으로 로깅합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    EventType eventType();
    String description() default "";
}
```

**위치**: `services/common-library/src/main/java/...security/audit/aspect/SecurityAuditAspect.java`

```java
package com.portal.universe.common.security.audit.aspect;

import com.portal.universe.common.security.audit.annotation.AuditLog;
import com.portal.universe.common.security.audit.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAuditAspect {

    private final SecurityAuditService auditService;

    @AfterReturning("@annotation(auditLog)")
    public void logAfterMethod(JoinPoint joinPoint, AuditLog auditLog) {
        try {
            HttpServletRequest request = getCurrentRequest();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            String userId = auth != null ? auth.getName() : "anonymous";
            String ipAddress = getClientIp(request);

            Map<String, Object> details = new HashMap<>();
            details.put("method", joinPoint.getSignature().getName());
            details.put("description", auditLog.description());

            auditService.logCustomEvent(
                auditLog.eventType(),
                userId,
                ipAddress,
                details
            );
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder
            .currentRequestAttributes())
            .getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
```

### 2.5 사용 예시

```java
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final SecurityAuditService auditService;

    @AuditLog(eventType = EventType.SENSITIVE_DATA_ACCESS, description = "Admin user list accessed")
    public List<UserResponse> getAllUsers() {
        // 사용자 목록 조회
        return userRepository.findAll();
    }

    public void deleteUser(Long userId, String adminIp) {
        // 사용자 삭제 로직
        userRepository.deleteById(userId);

        // 수동 로깅
        auditService.logCustomEvent(
            EventType.SENSITIVE_DATA_ACCESS,
            "admin",
            adminIp,
            Map.of("action", "delete_user", "targetUserId", userId)
        );
    }
}
```

---

## 3. 로그인 보안 강화 구현

Brute-force 공격 방지를 위한 로그인 시도 제한 및 계정 잠금 기능입니다.

### 3.1 LoginAttemptService 인터페이스

**위치**: `services/auth-service/src/main/java/...security/service/LoginAttemptService.java`

```java
package com.portal.universe.authservice.security.service;

/**
 * 로그인 시도 추적 및 제한 서비스
 */
public interface LoginAttemptService {

    /**
     * 로그인 실패 기록
     *
     * @param key IP 주소 또는 username
     */
    void recordFailure(String key);

    /**
     * 로그인 성공 시 초기화
     *
     * @param key IP 주소 또는 username
     */
    void recordSuccess(String key);

    /**
     * 잠금 상태 확인
     *
     * @param key IP 주소 또는 username
     * @return 잠금 여부
     */
    boolean isBlocked(String key);

    /**
     * 현재 시도 횟수 조회
     *
     * @param key IP 주소 또는 username
     * @return 시도 횟수
     */
    int getAttemptCount(String key);

    /**
     * 수동 초기화
     *
     * @param key IP 주소 또는 username
     */
    void reset(String key);

    /**
     * 잠금 해제 시간 조회 (초)
     *
     * @param key IP 주소 또는 username
     * @return 남은 시간 (초), 잠금되지 않았으면 0
     */
    long getLockDuration(String key);
}
```

### 3.2 LoginAttemptServiceImpl 구현체

**위치**: `services/auth-service/src/main/java/...security/service/impl/LoginAttemptServiceImpl.java`

```java
package com.portal.universe.authservice.security.service.impl;

import com.portal.universe.authservice.security.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String ATTEMPT_PREFIX = "login_attempt:";
    private static final String LOCK_PREFIX = "login_lock:";

    // 잠금 정책
    private static final int MAX_ATTEMPTS_LEVEL_1 = 3;  // 첫 번째 임계값
    private static final int MAX_ATTEMPTS_LEVEL_2 = 5;  // 두 번째 임계값
    private static final int MAX_ATTEMPTS_LEVEL_3 = 10; // 세 번째 임계값

    private static final long LOCK_DURATION_LEVEL_1 = 60;     // 1분
    private static final long LOCK_DURATION_LEVEL_2 = 300;    // 5분
    private static final long LOCK_DURATION_LEVEL_3 = 1800;   // 30분

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void recordFailure(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;

        // 현재 시도 횟수 조회
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        int currentAttempts = (attempts != null) ? attempts : 0;
        currentAttempts++;

        // 시도 횟수 저장 (24시간 TTL)
        redisTemplate.opsForValue().set(attemptKey, currentAttempts, Duration.ofHours(24));

        // 잠금 정책 적용
        applyLockPolicy(key, currentAttempts);

        log.warn("Login failure recorded for key={}, attempts={}", key, currentAttempts);
    }

    @Override
    public void recordSuccess(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        String lockKey = LOCK_PREFIX + key;

        redisTemplate.delete(attemptKey);
        redisTemplate.delete(lockKey);

        log.info("Login success, reset attempts for key={}", key);
    }

    @Override
    public boolean isBlocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    @Override
    public int getAttemptCount(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        return (attempts != null) ? attempts : 0;
    }

    @Override
    public void reset(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        String lockKey = LOCK_PREFIX + key;

        redisTemplate.delete(attemptKey);
        redisTemplate.delete(lockKey);

        log.info("Manually reset attempts for key={}", key);
    }

    @Override
    public long getLockDuration(String key) {
        String lockKey = LOCK_PREFIX + key;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }

    /**
     * 시도 횟수에 따른 잠금 정책 적용
     */
    private void applyLockPolicy(String key, int attempts) {
        String lockKey = LOCK_PREFIX + key;
        long lockDuration = 0;

        if (attempts >= MAX_ATTEMPTS_LEVEL_3) {
            lockDuration = LOCK_DURATION_LEVEL_3; // 30분
        } else if (attempts >= MAX_ATTEMPTS_LEVEL_2) {
            lockDuration = LOCK_DURATION_LEVEL_2; // 5분
        } else if (attempts >= MAX_ATTEMPTS_LEVEL_1) {
            lockDuration = LOCK_DURATION_LEVEL_1; // 1분
        }

        if (lockDuration > 0) {
            redisTemplate.opsForValue().set(lockKey, true, Duration.ofSeconds(lockDuration));
            log.warn("Account locked for key={}, duration={}s", key, lockDuration);
        }
    }
}
```

### 3.3 Redis 키 구조

| 키 패턴 | 값 타입 | TTL | 설명 |
|---------|---------|-----|------|
| `login_attempt:{ip}:{username}` | Integer | 24h | 로그인 실패 시도 횟수 |
| `login_lock:{ip}:{username}` | Boolean | 동적 | 계정 잠금 플래그 |

**예시**:
```
login_attempt:192.168.1.100:john@example.com = 3
login_lock:192.168.1.100:john@example.com = true (TTL: 60s)
```

### 3.4 잠금 정책 테이블

| 실패 횟수 | 잠금 시간 | 적용 시점 |
|----------|----------|----------|
| 1-2회 | 없음 | - |
| 3-4회 | 1분 (60초) | 3회째 실패 시 |
| 5-9회 | 5분 (300초) | 5회째 실패 시 |
| 10회 이상 | 30분 (1800초) | 10회째 실패 시 |

### 3.5 AuthService 통합

**위치**: `services/auth-service/src/main/java/...service/impl/AuthServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService auditService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String key = ipAddress + ":" + request.getEmail();

        // 1. 잠금 상태 확인
        if (loginAttemptService.isBlocked(key)) {
            long remainingSeconds = loginAttemptService.getLockDuration(key);
            auditService.logLoginFailure(request.getEmail(), ipAddress, userAgent,
                "Account locked for " + remainingSeconds + " seconds");

            throw new CustomBusinessException(
                AuthErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
                Map.of("retryAfter", remainingSeconds)
            );
        }

        // 2. 사용자 조회 및 비밀번호 검증
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                loginAttemptService.recordFailure(key);
                auditService.logLoginFailure(request.getEmail(), ipAddress, userAgent, "User not found");
                return new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(key);
            auditService.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Invalid password");
            throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 로그인 성공
        loginAttemptService.recordSuccess(key);
        auditService.logLoginSuccess(user.getUserId(), ipAddress, userAgent);

        // 4. JWT 토큰 생성 및 반환
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
```

### 3.6 에러 코드 추가

**위치**: `services/auth-service/src/main/java/...exception/AuthErrorCode.java`

```java
public enum AuthErrorCode implements ErrorCode {
    ACCOUNT_TEMPORARILY_LOCKED("A010", "계정이 일시적으로 잠겼습니다. {retryAfter}초 후 다시 시도해주세요.", HttpStatus.FORBIDDEN),
    // ... 기타 에러 코드
}
```

---

## 4. 보안 헤더 구현

XSS, Clickjacking, MIME Sniffing 등 웹 보안 위협을 방지하기 위한 HTTP 보안 헤더 설정입니다.

### 4.1 적용할 보안 헤더 목록

| 헤더 | 값 | 목적 |
|------|-----|------|
| `X-Content-Type-Options` | `nosniff` | MIME Sniffing 방지 |
| `X-Frame-Options` | `DENY` | Clickjacking 방지 |
| `X-XSS-Protection` | `1; mode=block` | 레거시 XSS 필터 활성화 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 강제 |
| `Content-Security-Policy` | (아래 참조) | XSS 및 데이터 주입 공격 방지 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referrer 정보 제한 |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=()` | 브라우저 기능 제한 |

### 4.2 SecurityHeadersFilter 구현

**위치**: `services/api-gateway/src/main/java/...filter/SecurityHeadersFilter.java`

```java
package com.portal.universe.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway 레벨 보안 헤더 필터
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // X-Content-Type-Options
            headers.add("X-Content-Type-Options", "nosniff");

            // X-Frame-Options
            headers.add("X-Frame-Options", "DENY");

            // X-XSS-Protection
            headers.add("X-XSS-Protection", "1; mode=block");

            // Strict-Transport-Security (HTTPS 환경에서만)
            if (exchange.getRequest().getURI().getScheme().equals("https")) {
                headers.add("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
            }

            // Content-Security-Policy
            headers.add("Content-Security-Policy", buildCspPolicy());

            // Referrer-Policy
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions-Policy
            headers.add("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=()");
        }));
    }

    @Override
    public int getOrder() {
        return -2; // RateLimitExceededFilter 다음으로 높은 우선순위
    }

    /**
     * Content Security Policy 생성
     */
    private String buildCspPolicy() {
        return String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // TODO: 'unsafe-inline', 'unsafe-eval' 제거
            "style-src 'self' 'unsafe-inline'",                // TODO: 'unsafe-inline' 제거
            "img-src 'self' data: https:",
            "font-src 'self' data:",
            "connect-src 'self' https://api.portal-universe.com",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'"
        );
    }
}
```

### 4.3 CSP 정책 상세 설명

```text
Content-Security-Policy:
  default-src 'self';                     # 기본적으로 같은 origin만 허용
  script-src 'self' 'unsafe-inline';      # 스크립트: 같은 origin + 인라인 (임시)
  style-src 'self' 'unsafe-inline';       # 스타일: 같은 origin + 인라인 (임시)
  img-src 'self' data: https:;            # 이미지: 같은 origin + data URI + HTTPS
  font-src 'self' data:;                  # 폰트: 같은 origin + data URI
  connect-src 'self' https://api.portal-universe.com;  # API 호출 허용
  frame-ancestors 'none';                 # iframe 임베딩 금지
  base-uri 'self';                        # <base> 태그 제한
  form-action 'self';                     # 폼 제출 제한
```

**주의사항**:
- `'unsafe-inline'` 및 `'unsafe-eval'`은 보안상 취약하므로 **프로덕션 환경에서는 제거**해야 합니다.
- Vue 3, React 18 빌드 시 인라인 스크립트를 제거하고 nonce 또는 hash 기반 CSP를 적용하세요.

### 4.4 환경별 CSP 정책 관리

**위치**: `services/api-gateway/src/main/resources/application.yml`

```yaml
security:
  headers:
    csp:
      enabled: true
      policy:
        # Local 환경: 관대한 정책
        local: "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';"

        # Docker/K8s 환경: 엄격한 정책
        production: "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: https:; connect-src 'self' https://api.portal-universe.com; frame-ancestors 'none';"
```

**SecurityHeadersFilter 수정**:

```java
@Component
@ConfigurationProperties(prefix = "security.headers.csp")
@Data
public class CspConfig {
    private boolean enabled = true;
    private Map<String, String> policy = new HashMap<>();
}

@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    private final CspConfig cspConfig;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private String buildCspPolicy() {
        if (!cspConfig.isEnabled()) {
            return "";
        }

        return cspConfig.getPolicy()
            .getOrDefault(activeProfile, cspConfig.getPolicy().get("local"));
    }
}
```

---

## 5. 파일 위치 및 의존성

### 5.1 파일 위치 맵

| 컴포넌트 | 파일 위치 | 비고 |
|---------|----------|------|
| **Rate Limiting** | | |
| RateLimiterConfig | `api-gateway/.../config/RateLimiterConfig.java` | KeyResolver 정의 |
| RateLimitExceededFilter | `api-gateway/.../filter/RateLimitExceededFilter.java` | 429 응답 처리 |
| application.yml | `api-gateway/src/main/resources/application.yml` | Route 설정 |
| **보안 감사 로깅** | | |
| SecurityAuditEvent | `common-library/.../security/audit/entity/SecurityAuditEvent.java` | 엔티티 |
| SecurityAuditService | `common-library/.../security/audit/service/SecurityAuditService.java` | 인터페이스 |
| SecurityAuditServiceImpl | `common-library/.../security/audit/service/impl/SecurityAuditServiceImpl.java` | 구현체 |
| @AuditLog | `common-library/.../security/audit/annotation/AuditLog.java` | 어노테이션 |
| SecurityAuditAspect | `common-library/.../security/audit/aspect/SecurityAuditAspect.java` | AOP Aspect |
| **로그인 보안** | | |
| LoginAttemptService | `auth-service/.../security/service/LoginAttemptService.java` | 인터페이스 |
| LoginAttemptServiceImpl | `auth-service/.../security/service/impl/LoginAttemptServiceImpl.java` | 구현체 |
| AuthServiceImpl | `auth-service/.../service/impl/AuthServiceImpl.java` | 통합 적용 |
| AuthErrorCode | `auth-service/.../exception/AuthErrorCode.java` | 에러 코드 |
| **보안 헤더** | | |
| SecurityHeadersFilter | `api-gateway/.../filter/SecurityHeadersFilter.java` | WebFilter |
| CspConfig | `api-gateway/.../config/CspConfig.java` | CSP 설정 |

### 5.2 의존성 요구사항

#### API Gateway (`services/api-gateway/pom.xml`)

```xml
<dependencies>
    <!-- Rate Limiting -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>

    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
</dependencies>
```

#### Common Library (`services/common-library/pom.xml`)

```xml
<dependencies>
    <!-- Audit Logging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- AOP for @AuditLog -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Async Support -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

#### Auth Service (`services/auth-service/pom.xml`)

```xml
<dependencies>
    <!-- Login Attempt Tracking -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Common Library (SecurityAuditService 포함) -->
    <dependency>
        <groupId>com.portal.universe</groupId>
        <artifactId>common-library</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

### 5.3 데이터베이스 스키마

**보안 감사 이벤트 테이블**:

```sql
-- MySQL
CREATE TABLE security_audit_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    request_uri VARCHAR(100),
    request_method VARCHAR(10),
    details JSON,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_ip_address (ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.4 Redis 설정

**공통 Redis 설정** (`docker-compose.yml` 또는 `k8s/redis.yaml`):

```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
  command: redis-server --appendonly yes
```

**Redis 연결 정보** (`application.yml`):

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

---

## 구현 체크리스트

### Rate Limiting
- [ ] `RateLimiterConfig` 클래스 생성
- [ ] `ipKeyResolver`, `userKeyResolver` Bean 정의
- [ ] `application.yml`에 Route별 Rate Limit 설정
- [ ] `RateLimitExceededFilter` 구현
- [ ] Redis 연결 테스트
- [ ] 엔드포인트별 정책 검증

### 보안 감사 로깅
- [ ] `SecurityAuditEvent` 엔티티 생성
- [ ] 데이터베이스 테이블 생성 (마이그레이션 스크립트)
- [ ] `SecurityAuditService` 인터페이스 및 구현체 작성
- [ ] `@AuditLog` 어노테이션 및 Aspect 구현
- [ ] `@Async` 설정 확인
- [ ] 테스트 케이스 작성

### 로그인 보안 강화
- [ ] `LoginAttemptService` 인터페이스 및 구현체 작성
- [ ] Redis 키 구조 설계
- [ ] 잠금 정책 구현 (3단계)
- [ ] `AuthServiceImpl`에 통합
- [ ] `AuthErrorCode.ACCOUNT_TEMPORARILY_LOCKED` 추가
- [ ] 테스트 케이스 작성 (실패 시나리오)

### 보안 헤더
- [ ] `SecurityHeadersFilter` 구현
- [ ] CSP 정책 정의
- [ ] 환경별 CSP 설정 (`CspConfig`)
- [ ] HTTPS 환경에서 HSTS 적용 확인
- [ ] 브라우저 개발자 도구에서 헤더 검증

### 통합 테스트
- [ ] Rate Limiting 초과 시 429 응답 확인
- [ ] 로그인 실패 3회 시 1분 잠금 확인
- [ ] 보안 이벤트 데이터베이스 저장 확인
- [ ] 보안 헤더 모든 응답에 포함 확인
- [ ] 부하 테스트 (JMeter, Gatling 등)

---

## 관련 문서

- [JWT RBAC 설정 가이드](./jwt-rbac-setup.md)
- [환경 변수 설정 가이드](./environment-variables.md)
- [ADR-004: JWT RBAC 자동 설정 전략](../adr/ADR-004-jwt-rbac-auto-configuration.md)
- [ADR-005: 민감 데이터 관리 전략](../adr/ADR-005-sensitive-data-management.md)

---

## 참고 자료

### Spring Cloud Gateway Rate Limiting
- [Spring Cloud Gateway Rate Limiter](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-requestratelimiter-gatewayfilter-factory)
- [Redis Rate Limiter](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-redis-ratelimiter)

### Content Security Policy
- [MDN - CSP](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [CSP Evaluator](https://csp-evaluator.withgoogle.com/)

### Security Headers
- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [Security Headers Best Practices](https://securityheaders.com/)

---

**최종 업데이트**: 2026-01-23
**작성자**: Laze
**문서 상태**: Current
