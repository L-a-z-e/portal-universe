---
id: common-library-security-audit-log-setup
title: 보안 감사 로그 설정 가이드
type: guide
status: current
created: 2026-01-23
updated: 2026-01-30
author: Laze
tags: [common-library, security, audit, logging, logback]
---

# 보안 감사 로그 설정 가이드

**난이도**: ⭐⭐⭐ | **예상 시간**: 30분 | **카테고리**: Development

## 개요
common-library에서 제공하는 보안 감사 로깅 모듈을 각 서비스에서 활용하는 방법을 설명합니다.

## 1. 의존성 추가
서비스의 `build.gradle`에 common-library 의존성이 추가되어 있는지 확인합니다.

```gradle
dependencies {
    implementation project(':common-library')
}
```

## 2. 구현 코드

### SecurityAuditEvent 엔티티

**위치**: `services/common-library/src/main/java/...security/audit/entity/SecurityAuditEvent.java`

```java
@Entity
@Table(name = "security_audit_events", indexes = {
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class SecurityAuditEvent {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID eventId;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum EventType {
        LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT,
        PASSWORD_CHANGE, PASSWORD_RESET_REQUEST,
        ACCOUNT_LOCKED, ACCOUNT_UNLOCKED,
        PERMISSION_DENIED, SENSITIVE_DATA_ACCESS,
        TOKEN_REFRESH, TWO_FACTOR_ENABLED, TWO_FACTOR_DISABLED
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) { timestamp = LocalDateTime.now(); }
    }
}
```

### SecurityAuditService 인터페이스

**위치**: `services/common-library/src/main/java/...security/audit/service/SecurityAuditService.java`

```java
public interface SecurityAuditService {
    void logEvent(SecurityAuditEvent event);
    void logLoginSuccess(String userId, String ipAddress, String userAgent);
    void logLoginFailure(String username, String ipAddress, String userAgent, String reason);
    void logLogout(String userId, String ipAddress);
    void logPermissionDenied(String userId, String ipAddress, String requestUri, String requiredRole);
    void logAccountLocked(String userId, String ipAddress, int attemptCount);
    void logSensitiveDataAccess(String userId, String ipAddress, String resource);
    void logCustomEvent(EventType eventType, String userId, String ipAddress, Map<String, Object> details);
}
```

### @AuditLog 어노테이션 (AOP)

**위치**: `services/common-library/src/main/java/...security/audit/annotation/AuditLog.java`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    EventType eventType();
    String description() default "";
}
```

**SecurityAuditAspect** (`services/common-library/src/main/java/...security/audit/aspect/SecurityAuditAspect.java`):

```java
@Slf4j @Aspect @Component @RequiredArgsConstructor
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

            auditService.logCustomEvent(auditLog.eventType(), userId, ipAddress, details);
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }
}
```

### DB 스키마

```sql
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

## 3. Logback 설정

### 2.1. 별도 로그 파일 분리
`src/main/resources/logback-spring.xml`에 보안 감사 로그 전용 appender를 추가합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 기본 콘솔 appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 보안 감사 로그 전용 파일 appender -->
    <appender name="SECURITY_AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security-audit.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 일별 로그 파일 생성 -->
            <fileNamePattern>logs/security-audit.%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 90일 보관 -->
            <maxHistory>90</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- SecurityAuditServiceImpl의 로그만 SECURITY_AUDIT_FILE로 전송 -->
    <logger name="com.portal.universe.commonlibrary.security.audit.SecurityAuditServiceImpl"
            level="INFO"
            additivity="false">
        <appender-ref ref="SECURITY_AUDIT_FILE"/>
    </logger>

    <!-- 루트 로거 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

### 2.2. JSON 포맷 로그 (선택 사항)
Logstash나 ELK Stack과 연동할 경우 JSON 포맷을 사용할 수 있습니다.

**의존성 추가:**
```gradle
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

**Logback 설정:**
```xml
<appender name="SECURITY_AUDIT_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/security-audit.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>eventId</includeMdcKeyName>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/security-audit.%d{yyyy-MM-dd}.json</fileNamePattern>
        <maxHistory>90</maxHistory>
    </rollingPolicy>
</appender>
```

## 3. 사용 방법

### 3.1. 직접 호출 방식
컨트롤러나 서비스에서 직접 `SecurityAuditService`를 주입받아 사용합니다.

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponse response = authService.login(request);

            // 로그인 성공 로그
            securityAuditService.logLoginSuccess(
                    response.getUserId(),
                    request.getUsername(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BadCredentialsException e) {
            // 로그인 실패 로그
            securityAuditService.logLoginFailure(
                    request.getUsername(),
                    ipAddress,
                    "Invalid credentials"
            );
            throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

### 3.2. @AuditLog 어노테이션 방식 (AOP)
메서드에 어노테이션을 추가하여 자동으로 로그를 기록합니다.

```java
@Service
@RequiredArgsConstructor
public class UserAdminService {

    @AuditLog(
        eventType = SecurityAuditEventType.ADMIN_ACTION,
        description = "사용자 권한 변경"
    )
    public void updateUserRole(String userId, String newRole) {
        // 권한 변경 로직
    }

    @AuditLog(
        eventType = SecurityAuditEventType.ACCOUNT_LOCKED,
        description = "계정 잠금"
    )
    public void lockAccount(String userId) {
        // 계정 잠금 로직
    }

    @AuditLog(
        eventType = SecurityAuditEventType.SENSITIVE_DATA_ACCESS,
        description = "개인정보 조회"
    )
    public UserDetail getUserDetail(String userId) {
        // 민감 정보 조회 로직
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
```

### 3.3. 커스텀 이벤트 로깅
복잡한 상황에서는 `SecurityAuditEvent`를 직접 생성하여 로깅할 수 있습니다.

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SecurityAuditService securityAuditService;

    public void processPayment(PaymentRequest request, String userId) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEventType.SENSITIVE_DATA_ACCESS)
                .userId(userId)
                .ipAddress(getCurrentUserIp())
                .success(true)
                .build();

        event.addDetail("paymentAmount", request.getAmount());
        event.addDetail("paymentMethod", request.getMethod());
        event.addDetail("orderId", request.getOrderId());

        securityAuditService.log(event);

        // 결제 처리 로직...
    }
}
```

## 4. 로그 예시

### 4.1. 로그인 성공
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "LOGIN_SUCCESS",
  "timestamp": "2026-01-23T15:30:45.123",
  "userId": "user-123",
  "username": "john.doe",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "requestUri": "/api/v1/auth/login",
  "requestMethod": "POST",
  "details": {},
  "success": true,
  "errorMessage": null
}
```

### 4.2. 접근 거부
```json
{
  "eventId": "660e9511-f39c-52e5-b827-557766551111",
  "eventType": "ACCESS_DENIED",
  "timestamp": "2026-01-23T15:35:12.456",
  "userId": "user-456",
  "username": "user-456",
  "ipAddress": "192.168.1.101",
  "userAgent": "Mozilla/5.0...",
  "requestUri": "/api/v1/admin/users",
  "requestMethod": "GET",
  "details": {
    "requiredRole": "ROLE_ADMIN"
  },
  "success": false,
  "errorMessage": null
}
```

### 4.3. 관리자 작업
```json
{
  "eventId": "770f0622-g40d-63f6-c938-668877662222",
  "eventType": "ADMIN_ACTION",
  "timestamp": "2026-01-23T16:00:00.789",
  "userId": "admin-789",
  "username": "admin-789",
  "ipAddress": "192.168.1.50",
  "userAgent": "Mozilla/5.0...",
  "requestUri": "/api/v1/admin/users/user-123/role",
  "requestMethod": "PUT",
  "details": {
    "action": "updateUserRole",
    "targetResource": "user-123",
    "description": "사용자 권한 변경",
    "method": "updateUserRole",
    "class": "UserAdminService"
  },
  "success": true,
  "errorMessage": null
}
```

## 5. 보안 고려사항

### 5.1. 민감 정보 노출 방지
- 비밀번호, 토큰 원문 등은 로그에 기록하지 않습니다.
- 필요시 마스킹 처리를 적용합니다.

```java
event.addDetail("creditCard", maskCreditCard(cardNumber)); // "****-****-****-1234"
```

### 5.2. 로그 파일 보안
- 로그 파일에 적절한 권한 설정 (읽기 권한 제한)
- 로그 파일 암호화 고려
- 정기적인 로그 백업 및 보관

### 5.3. GDPR/개인정보보호법 준수
- 개인정보는 법적 보관 기간만 유지 (예: 90일)
- 필요시 개인정보 익명화 또는 가명화

## 6. 모니터링 및 알람

### 6.1. 의심스러운 활동 탐지
- 짧은 시간에 여러 번의 로그인 실패 (무차별 대입 공격 의심)
- 비정상적인 시간대의 관리자 작업
- 여러 IP에서 동일 계정 접근

### 6.2. Prometheus + Grafana 연동 (선택 사항)
보안 이벤트를 메트릭으로 변환하여 시각화할 수 있습니다.

```java
@Component
@RequiredArgsConstructor
public class SecurityMetricsCollector {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void onSecurityEvent(SecurityAuditEvent event) {
        meterRegistry.counter("security.audit.events",
                "type", event.getEventType().name(),
                "success", String.valueOf(event.isSuccess())
        ).increment();
    }
}
```

## 7. 문제 해결

### Q1. 로그가 기록되지 않습니다.
- `SecurityAuditServiceImpl`이 Spring Bean으로 등록되었는지 확인
- Logback 설정의 logger name과 패키지 경로가 일치하는지 확인
- 로그 레벨이 INFO 이상인지 확인

### Q2. @AuditLog가 동작하지 않습니다.
- `@EnableAspectJAutoProxy` 어노테이션이 활성화되었는지 확인 (Spring Boot는 기본 활성화)
- AOP 프록시 제약: 같은 클래스 내부 메서드 호출은 AOP가 적용되지 않음
- AspectJ 의존성이 있는지 확인

### Q3. 로그 파일이 너무 큽니다.
- Rolling 정책을 조정하여 파일 크기 제한 추가
- 보관 기간 단축 (`maxHistory` 값 조정)
- 불필요한 이벤트 유형 로깅 제거

## 8. 참고 자료
- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [Logback Configuration](http://logback.qos.ch/manual/configuration.html)
- [Spring AOP](https://docs.spring.io/spring-framework/reference/core/aop.html)
