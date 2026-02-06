# 📝 Security Audit Logging 학습

> 보안 이벤트를 추적하고 기록하여 침입 탐지와 포렌식을 지원하는 기법

**난이도**: ⭐⭐⭐ (중급)
**학습 시간**: 45분
**실습 시간**: 30분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Security Audit Logging의 필요성 이해하기
- [ ] AOP 기반 감사 로깅 구현하기
- [ ] 보안 이벤트 분류 및 기록하기
- [ ] 감사 로그를 활용한 침입 탐지하기

---

## 1️⃣ 왜 Security Audit Logging이 필요한가?

### 보안 사고 대응

```
🔴 침입 사고 발생

Without Audit Log:
❌ 언제 침입했는지 모름
❌ 어떤 데이터에 접근했는지 모름
❌ 어떻게 침입했는지 모름
→ 피해 범위 파악 불가
→ 재발 방지 불가

With Audit Log:
✅ 2026-01-23 15:23:45 비정상 로그인 탐지
✅ IP: 203.0.113.45 (러시아)
✅ 접근한 데이터: 사용자 10명의 개인정보
✅ 침입 경로: 탈취된 관리자 계정
→ 즉시 계정 차단, 비밀번호 리셋
→ 영향받은 사용자 10명 알림
→ 취약점 패치
```

### 규정 준수 (Compliance)

```
📋 GDPR (유럽 개인정보보호법)
- Article 30: 처리 활동 기록 의무
- 누가, 언제, 무엇을, 왜 처리했는지 기록

📋 PCI DSS (신용카드 정보보호 표준)
- Requirement 10: 모든 접근 추적 및 모니터링
- 로그 최소 1년 보관

📋 개인정보보호법 (한국)
- 제29조: 안전성 확보 조치
- 접속 기록 최소 6개월 보관
```

### 내부자 위협 탐지

```
🔍 이상 행동 탐지

Case 1: 대량 데이터 조회
- 평소 하루 10건 조회하던 직원
- 갑자기 1000건 조회
→ 퇴사 전 정보 유출 시도 의심

Case 2: 비정상 시간 접근
- 평일 09:00-18:00만 접근하던 직원
- 주말 새벽 03:00 접근
→ 권한 남용 의심

Case 3: 권한 밖 접근 시도
- 일반 사용자가 관리자 API 호출
→ 권한 우회 시도
```

---

## 2️⃣ 감사 로그에 기록할 정보

### Who (누가)

```java
{
  "userId": "user-123",           // 사용자 ID
  "email": "admin@example.com",   // 이메일
  "roles": ["ROLE_ADMIN"],        // 역할
  "username": "admin"             // 사용자명
}
```

### When (언제)

```java
{
  "timestamp": "2026-01-23T15:23:45.123Z",  // ISO 8601 형식
  "timezone": "Asia/Seoul"                   // 시간대
}
```

### What (무엇을)

```java
{
  "eventType": "DATA_ACCESS",        // 이벤트 유형
  "resource": "/api/users/123",      // 접근 리소스
  "action": "READ",                  // 수행 동작
  "targetEntity": "User",            // 대상 엔티티
  "targetId": "123"                  // 대상 ID
}
```

### Where (어디서)

```java
{
  "ipAddress": "192.168.1.100",      // IP 주소
  "country": "KR",                   // 국가 코드
  "userAgent": "Mozilla/5.0...",     // 브라우저/클라이언트
  "deviceType": "Desktop"            // 디바이스 유형
}
```

### How (어떻게)

```java
{
  "endpoint": "GET /api/users/123",  // API 엔드포인트
  "method": "GET",                   // HTTP 메서드
  "statusCode": 200,                 // 응답 상태
  "executionTime": 125               // 실행 시간 (ms)
}
```

### Why (왜 - 선택적)

```java
{
  "reason": "Routine admin check",   // 접근 사유
  "requestTicket": "JIRA-1234"       // 승인 티켓
}
```

---

## 3️⃣ 프로젝트 구현: AOP 기반

### @AuditLog 어노테이션

```java
// services/common-library/.../security/audit/AuditLog.java

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 이벤트 유형
     */
    SecurityAuditEventType eventType();

    /**
     * 이벤트 설명
     */
    String description() default "";

    /**
     * 대상 리소스 SpEL 표현식
     * 예: "#request.userId"
     */
    String targetResource() default "";
}
```

### SecurityAuditEventType

```java
public enum SecurityAuditEventType {

    // 인증 관련
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    LOGIN_FAILED("로그인 실패"),
    PASSWORD_CHANGE("비밀번호 변경"),

    // 접근 관련
    ACCESS_GRANTED("접근 허용"),
    ACCESS_DENIED("접근 거부"),

    // 데이터 관련
    DATA_READ("데이터 조회"),
    DATA_CREATE("데이터 생성"),
    DATA_UPDATE("데이터 수정"),
    DATA_DELETE("데이터 삭제"),

    // 권한 관련
    PERMISSION_GRANTED("권한 부여"),
    PERMISSION_REVOKED("권한 회수"),
    ROLE_CHANGED("역할 변경"),

    // 시스템 관련
    SYSTEM_CONFIG_CHANGE("시스템 설정 변경"),
    SECURITY_ALERT("보안 경고"),
    SUSPICIOUS_ACTIVITY("의심스러운 활동");

    private final String description;
}
```

### AuditLogAspect

```java
// AuditLogAspect.java

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SecurityAuditService auditService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object logAuditEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog)
            throws Throwable {

        // 1. 요청 컨텍스트 수집
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = attributes != null
            ? attributes.getRequest()
            : null;

        // 2. 사용자 정보 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "anonymous";

        // 3. 메서드 실행
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // 4. Audit 이벤트 생성
            SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(auditLog.eventType())
                .userId(userId)
                .ipAddress(getClientIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .endpoint(request != null ? request.getMethod() + " " + request.getRequestURI() : null)
                .targetResource(evaluateTargetResource(auditLog, joinPoint))
                .success(exception == null)
                .executionTime(executionTime)
                .timestamp(LocalDateTime.now())
                .build();

            // 5. 비동기 저장
            auditService.saveAuditEvent(event);
        }
    }

    /**
     * SpEL로 targetResource 평가
     */
    private String evaluateTargetResource(AuditLog auditLog, ProceedingJoinPoint joinPoint) {
        if (auditLog.targetResource().isEmpty()) {
            return null;
        }

        try {
            Expression expression = parser.parseExpression(auditLog.targetResource());
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 메서드 파라미터를 SpEL 변수로 등록
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            return expression.getValue(context, String.class);
        } catch (Exception e) {
            log.warn("Failed to evaluate targetResource: {}", auditLog.targetResource(), e);
            return null;
        }
    }
}
```

---

## 4️⃣ 사용 예시

### 로그인 이벤트

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/api/auth/login")
    @AuditLog(
        eventType = SecurityAuditEventType.LOGIN,
        description = "User login attempt",
        targetResource = "#request.email"
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**기록되는 로그**:
```json
{
  "eventType": "LOGIN",
  "userId": "user-123",
  "email": "admin@example.com",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
  "endpoint": "POST /api/auth/login",
  "targetResource": "admin@example.com",
  "success": true,
  "timestamp": "2026-01-23T15:23:45.123Z",
  "executionTime": 125
}
```

### 데이터 접근 이벤트

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/api/users/{id}")
    @AuditLog(
        eventType = SecurityAuditEventType.DATA_READ,
        description = "User profile access",
        targetResource = "#id"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id) {

        UserResponse user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/api/users/{id}")
    @AuditLog(
        eventType = SecurityAuditEventType.DATA_UPDATE,
        description = "User profile update",
        targetResource = "#id"
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {

        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
```

### 권한 변경 이벤트

```java
@Service
@RequiredArgsConstructor
public class AdminService {

    @AuditLog(
        eventType = SecurityAuditEventType.ROLE_CHANGED,
        description = "User role modification",
        targetResource = "#userId"
    )
    public void changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.setRole(newRole);
        userRepository.save(user);
    }
}
```

---

## 5️⃣ 비동기 저장

### 성능 영향 최소화

```java
@Service
@RequiredArgsConstructor
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Async  // 비동기 실행
    public void saveAuditEvent(SecurityAuditEvent event) {
        // 이벤트 발행
        eventPublisher.publishEvent(event);
    }

    @EventListener
    @Async
    public void handleAuditEvent(SecurityAuditEvent event) {
        try {
            // 1. DB에 저장 (JPA)
            AuditLog auditLog = AuditLog.from(event);
            auditLogRepository.save(auditLog);

            // 2. 검색 엔진에 인덱싱 (Elasticsearch - 선택적)
            elasticsearchService.index(event);

            // 3. 실시간 모니터링 (Kafka - 선택적)
            if (event.getEventType() == SecurityAuditEventType.SECURITY_ALERT) {
                kafkaTemplate.send("security-alerts", event);
            }

        } catch (Exception e) {
            log.error("Failed to save audit event", e);
            // 감사 로그 실패로 비즈니스 로직에 영향 주면 안됨
        }
    }
}
```

---

## ✍️ 실습 과제

### 과제 1: Audit Log 확인 (기초)

로그인하고 DB에서 감사 로그를 확인하세요.

```sql
-- 로그인 이벤트 조회
SELECT *
FROM audit_log
WHERE event_type = 'LOGIN'
ORDER BY timestamp DESC
LIMIT 10;

-- 특정 사용자의 활동 조회
SELECT *
FROM audit_log
WHERE user_id = 'user-123'
ORDER BY timestamp DESC;

-- 실패한 로그인 시도 조회
SELECT *
FROM audit_log
WHERE event_type = 'LOGIN'
  AND success = false
ORDER BY timestamp DESC;
```

### 과제 2: 의심스러운 활동 탐지 (중급)

SQL 쿼리로 의심스러운 패턴을 찾아보세요.

```sql
-- 1. 같은 IP에서 여러 계정 로그인
SELECT ip_address, COUNT(DISTINCT user_id) as account_count
FROM audit_log
WHERE event_type = 'LOGIN'
  AND timestamp > NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(DISTINCT user_id) > 5;

-- 2. 단기간 대량 데이터 조회
SELECT user_id, COUNT(*) as access_count
FROM audit_log
WHERE event_type = 'DATA_READ'
  AND timestamp > NOW() - INTERVAL '10 minutes'
GROUP BY user_id
HAVING COUNT(*) > 100;

-- 3. 비정상 시간대 접근 (새벽 2-5시)
SELECT *
FROM audit_log
WHERE EXTRACT(HOUR FROM timestamp) BETWEEN 2 AND 5
  AND event_type IN ('DATA_READ', 'DATA_UPDATE', 'DATA_DELETE');
```

### 과제 3: 커스텀 Audit Event (고급)

새로운 보안 이벤트 타입을 추가하고 사용하세요.

```java
// 1. EventType 추가
public enum SecurityAuditEventType {
    // ...기존 타입들...

    // 새로 추가
    FILE_DOWNLOAD("파일 다운로드"),
    FILE_UPLOAD("파일 업로드"),
    EXPORT_DATA("데이터 내보내기"),
    BULK_DELETE("대량 삭제");
}

// 2. 적용
@PostMapping("/api/files/download")
@AuditLog(
    eventType = SecurityAuditEventType.FILE_DOWNLOAD,
    description = "File download",
    targetResource = "#fileId"
)
public ResponseEntity<Resource> downloadFile(@RequestParam String fileId) {
    // ...
}
```

---

## 🔍 더 알아보기

### SIEM (Security Information and Event Management)

감사 로그를 중앙 집중화하고 분석

```
┌────────────────┐
│  Auth Service  │──┐
└────────────────┘  │
┌────────────────┐  │  ┌──────────────┐
│  Blog Service  │──┼─→│     SIEM     │
└────────────────┘  │  │ (Elasticsearch│
┌────────────────┐  │  │   + Kibana)  │
│Shopping Service│──┘  └──────────────┘
└────────────────┘        │
                          ▼
                   ┌──────────────┐
                   │   Dashboard  │
                   │   Alerting   │
                   └──────────────┘
```

### 로그 보관 정책

```
Tier 1: Hot Storage (최근 30일)
- SSD
- 빠른 검색
- 실시간 대시보드

Tier 2: Warm Storage (31-90일)
- HDD
- 일반 검색
- 월간 리포트

Tier 3: Cold Storage (91일-1년)
- Object Storage (S3)
- 아카이브
- 규정 준수

Tier 4: 삭제 (1년 초과)
```

### 개인정보 마스킹

```java
@EventListener
public void handleAuditEvent(SecurityAuditEvent event) {
    // 개인정보 마스킹
    if (event.getTargetResource() != null &&
        event.getTargetResource().contains("@")) {
        // 이메일 마스킹: user@example.com → u***@e***.com
        event.setTargetResource(maskEmail(event.getTargetResource()));
    }

    // IP 마스킹: 192.168.1.100 → 192.168.*.*
    if (event.getIpAddress() != null) {
        event.setIpAddress(maskIp(event.getIpAddress()));
    }
}
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Security Audit Logging의 필요성을 설명할 수 있다
- [ ] @AuditLog 어노테이션을 사용할 수 있다
- [ ] AOP 기반 감사 로깅의 동작 원리를 이해한다
- [ ] 의심스러운 활동 패턴을 SQL로 조회할 수 있다
- [ ] 비동기 저장의 중요성을 이해한다

---

## 📚 참고 자료

- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [NIST Special Publication 800-92: Guide to Computer Security Log Management](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-92.pdf)
- [Spring Boot AOP](https://docs.spring.io/spring-framework/reference/core/aop.html)

---

**이전**: [Login Security](./03-login-security.md)
**다음**: [Password Policy 학습하기](./05-password-policy.md) →
