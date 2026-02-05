---
id: common-library-security-audit-module
title: 보안 감사 로깅 모듈 아키텍처
type: architecture
status: current
created: 2026-01-23
updated: 2026-01-30
author: Laze
tags: [common-library, security, audit, logging, architecture]
---

# 보안 감사 로깅 모듈

## 개요
Portal Universe의 모든 마이크로서비스에서 사용할 수 있는 통합 보안 감사 로깅 모듈입니다.
로그인, 권한 변경, 민감 데이터 접근 등 보안 관련 주요 이벤트를 표준화된 형식으로 기록합니다.

## 주요 기능
- **표준화된 이벤트 타입**: 13가지 보안 이벤트 유형 (로그인, 권한, 관리자 작업 등)
- **자동 메타데이터 수집**: IP 주소, User-Agent, 요청 URI 등 자동 추출
- **JSON 로그 출력**: 구조화된 로그 형식 (ELK Stack 연동 용이)
- **AOP 기반 어노테이션**: `@AuditLog`로 간편한 로깅
- **유연한 확장**: 커스텀 이벤트 추가 가능

## 모듈 구성

### 1. 핵심 클래스

| 클래스명 | 역할 | 위치 |
|---------|------|------|
| `SecurityAuditEventType` | 이벤트 유형 정의 (enum) | `security/audit/` |
| `SecurityAuditEvent` | 이벤트 데이터 클래스 | `security/audit/` |
| `SecurityAuditService` | 로깅 서비스 인터페이스 | `security/audit/` |
| `SecurityAuditServiceImpl` | 로깅 서비스 구현체 | `security/audit/` |
| `@AuditLog` | AOP 어노테이션 | `security/audit/` |
| `AuditLogAspect` | AOP Aspect 구현 | `security/audit/` |

### 2. 이벤트 유형 (SecurityAuditEventType)

#### 인증 관련
- `LOGIN_SUCCESS`: 로그인 성공
- `LOGIN_FAILURE`: 로그인 실패
- `LOGOUT`: 로그아웃
- `TOKEN_REFRESH`: JWT 토큰 갱신
- `TOKEN_REVOKED`: JWT 토큰 폐기

#### 계정 관련
- `PASSWORD_CHANGED`: 비밀번호 변경
- `ACCOUNT_LOCKED`: 계정 잠금
- `ACCOUNT_UNLOCKED`: 계정 잠금 해제

#### 권한 관련
- `ACCESS_DENIED`: 접근 거부
- `PERMISSION_CHANGED`: 권한 변경

#### 데이터 관련
- `SENSITIVE_DATA_ACCESS`: 민감한 데이터 접근

#### 관리자 작업
- `ADMIN_ACTION`: 관리자 작업

## 사용 방법

### 방법 1: 직접 호출
```java
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final SecurityAuditService securityAuditService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponse response = authService.login(request);
            securityAuditService.logLoginSuccess(
                response.getUserId(), request.getUsername(), ipAddress, userAgent
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BadCredentialsException e) {
            securityAuditService.logLoginFailure(
                request.getUsername(), ipAddress, "Invalid credentials"
            );
            throw e;
        }
    }
}
```

### 방법 2: @AuditLog 어노테이션 (권장)
```java
@Service
public class UserAdminService {

    @AuditLog(
        eventType = SecurityAuditEventType.ADMIN_ACTION,
        description = "사용자 권한 변경"
    )
    public void updateUserRole(String userId, String newRole) {
        // 권한 변경 로직 - 메서드 실행 전후 자동 로깅됨
    }
}
```

### 방법 3: 커스텀 이벤트
```java
SecurityAuditEvent event = SecurityAuditEvent.builder()
    .eventType(SecurityAuditEventType.SENSITIVE_DATA_ACCESS)
    .userId(userId)
    .ipAddress(ipAddress)
    .success(true)
    .build();

event.addDetail("resourceType", "CreditCard");
event.addDetail("resourceId", "card-123");

securityAuditService.log(event);
```

## 로그 출력 예시

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "LOGIN_SUCCESS",
  "timestamp": "2026-01-23T15:30:45.123",
  "userId": "user-123",
  "username": "john.doe",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
  "requestUri": "/api/v1/auth/login",
  "requestMethod": "POST",
  "details": {},
  "success": true,
  "errorMessage": null
}
```

## 보안 고려사항

### DO
- 로그인/로그아웃 이벤트 기록
- 권한 변경 및 관리자 작업 기록
- 민감 데이터 접근 기록
- 반복적인 실패 시도 기록 (무차별 대입 공격 탐지)

### DON'T
- 비밀번호, JWT 토큰 원문 기록
- 신용카드 번호, 주민등록번호 전체 기록 (마스킹 필수)
- 과도하게 많은 이벤트 기록 (성능 및 스토리지 고려)

### 데이터 마스킹 예시
```java
event.addDetail("creditCard", maskCreditCard(cardNumber)); // "****-****-****-1234"
event.addDetail("email", maskEmail(email)); // "jo**@example.com"
```

## 확장 방법

### 새로운 이벤트 유형 추가
1. `SecurityAuditEventType` enum에 새 값 추가
2. `SecurityAuditService` 인터페이스에 편의 메서드 추가 (선택 사항)
3. 구현체에 메서드 구현

```java
// SecurityAuditEventType.java
public enum SecurityAuditEventType {
    // 기존 이벤트들...
    DATA_EXPORT,
    TWO_FACTOR_AUTH_ENABLED
}
```

## 모니터링 연동

### Prometheus 메트릭 수집
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

### Grafana 대시보드
- 시간당 로그인 실패 횟수
- 서비스별 접근 거부 횟수
- 관리자 작업 빈도

## 문제 해결

| 문제 | 해결 방법 |
|-----|---------|
| 로그가 기록되지 않음 | Logback 설정 확인, 로그 레벨 확인 |
| @AuditLog가 동작하지 않음 | AOP 의존성 확인, 프록시 제약 확인 |
| 로그 파일이 너무 큼 | Rolling 정책 조정, 보관 기간 단축 |
| IP 주소가 잘못 기록됨 | X-Forwarded-For 헤더 확인, Proxy 설정 확인 |

## 관련 문서
- [설정 가이드](../guides/security-audit-log-setup.md)
- [Security Module 가이드](../guides/security-module.md)

## 버전 히스토리
- **v1.0.0** (2026-01-23): 초기 릴리스
  - 13가지 이벤트 유형 지원
  - AOP 기반 @AuditLog 어노테이션
  - JSON 로그 출력
