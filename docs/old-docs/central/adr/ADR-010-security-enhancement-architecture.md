---
id: ADR-010
title: Security Enhancement Architecture
type: adr
status: proposed
created: 2026-01-23
updated: 2026-01-23
author: Architect Agent
decision_date: null
reviewers: []
tags: [security, rate-limiting, audit-logging, redis, kafka, gateway]
related:
  - ADR-003
  - ADR-008
---

# ADR-010: Security Enhancement Architecture

## 메타데이터

| 항목 | 내용 |
|------|------|
| **상태** | Proposed |
| **작성일** | 2026-01-23 |
| **작성자** | Architect Agent |

---

## Context (배경)

### 현재 보안 현황

Portal Universe는 JWT 기반 인증(ADR-008), RBAC 기반 인가(ADR-003)를 구현하여 기본적인 보안 체계를 갖추고 있습니다. 그러나 운영 환경에서 발생할 수 있는 다양한 보안 위협에 대응하기 위한 추가적인 보안 강화가 필요합니다.

```
현재 아키텍처:
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│  API Gateway │────▶│   Services   │
│  (Vue/React) │     │    (:8080)   │     │ (8081-8083)  │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Redis     │
                     │ (Token Store)│
                     └──────────────┘
```

### 식별된 보안 위협

| 위협 유형 | 현재 상태 | 위험도 |
|----------|----------|--------|
| **DDoS / Brute Force** | 미구현 (Rate Limiting 없음) | 높음 |
| **보안 감사 추적** | 기본 로그만 존재 | 중간 |
| **로그인 공격** | 실패 횟수 제한 없음 | 높음 |
| **보안 헤더** | 부분적 적용 | 중간 |
| **XSS/CSRF** | 기본 설정 | 중간 |

### 개선 필요성

1. **규제 준수**: 개인정보보호법, 정보보안 감사 대응
2. **서비스 안정성**: DDoS 공격 방어, 리소스 보호
3. **침해 대응**: 보안 사고 발생 시 추적 및 분석 능력
4. **사용자 보호**: 계정 탈취 공격 방어

---

## Decision (결정)

다음 4가지 보안 강화 아키텍처를 채택합니다.

---

### 1. Rate Limiting 아키텍처

#### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Request                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway (:8080)                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              RateLimiterFilter (Gateway Filter)           │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  1. Key 추출: IP + UserID (인증된 경우)              │  │  │
│  │  │  2. Redis 조회: Sliding Window Counter              │  │  │
│  │  │  3. 허용 여부 판단                                   │  │  │
│  │  │  4. 허용 → 요청 전달 / 거부 → 429 응답              │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
    ┌──────────────────┐         ┌──────────────────┐
    │      Redis       │         │  Backend Service │
    │  (Rate Counter)  │         │                  │
    └──────────────────┘         └──────────────────┘
```

#### 1.2 Sliding Window 알고리즘

Fixed Window의 경계 시점 burst 문제를 해결하기 위해 **Sliding Window Log** 또는 **Sliding Window Counter** 알고리즘을 채택합니다.

```
Sliding Window Counter 방식:

시간축: ──────────────────────────────────────────▶
        │◀── 이전 윈도우 ──▶│◀── 현재 윈도우 ──▶│
        │     (60초)        │     (60초)        │
        │                   │        ▲          │
        │  count: 40        │  현재시점 (30초)  │
        │                   │  count: 20        │
        └───────────────────┴───────────────────┘

가중치 계산:
- 이전 윈도우 비율: (60 - 30) / 60 = 0.5
- 현재 요청 수: 40 * 0.5 + 20 = 40
- 제한: 100 req/min → 허용
```

#### 1.3 Rate Limit 정책

| 엔드포인트 패턴 | IP 제한 | User 제한 | 윈도우 | 비고 |
|----------------|---------|----------|--------|------|
| `POST /api/auth/login` | 10 req | - | 1분 | 로그인 시도 |
| `POST /api/auth/refresh` | 30 req | 30 req | 1분 | 토큰 갱신 |
| `GET /api/**` | 300 req | 600 req | 1분 | 일반 조회 |
| `POST /api/**` | 60 req | 120 req | 1분 | 생성/수정 |
| `DELETE /api/**` | 30 req | 60 req | 1분 | 삭제 |

#### 1.4 Redis Key 구조

```
rate_limit:ip:{ip_address}:{endpoint_pattern}:{window_timestamp}
rate_limit:user:{user_id}:{endpoint_pattern}:{window_timestamp}

예시:
rate_limit:ip:192.168.1.100:login:1705900800 → "8"
rate_limit:user:550e8400-e29b:api_get:1705900800 → "45"

TTL: window_size * 2 (2분)
```

#### 1.5 429 응답 표준화

```json
{
  "success": false,
  "code": "C429",
  "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
  "data": null,
  "meta": {
    "retryAfter": 30,
    "limit": 100,
    "remaining": 0,
    "resetAt": "2026-01-23T10:30:00Z"
  }
}
```

**응답 헤더**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705900830
Retry-After: 30
```

---

### 2. 보안 감사 로깅 아키텍처

#### 2.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                     Microservices Layer                         │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐           │
│  │Auth Service │   │Blog Service │   │Shopping Svc │           │
│  │   (:8081)   │   │   (:8082)   │   │   (:8083)   │           │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘           │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │                                     │
│  ┌────────────────────────▼────────────────────────────────┐   │
│  │              @AuditLog Aspect (common-library)          │   │
│  │  - 메서드 실행 전/후 감사 이벤트 생성                    │   │
│  │  - 비동기 처리 (별도 스레드풀)                           │   │
│  └────────────────────────┬────────────────────────────────┘   │
└───────────────────────────┼─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Kafka Cluster                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               Topic: security.audit.events              │   │
│  │               Partitions: 6 (by service-id)             │   │
│  │               Retention: 90 days                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Elasticsearch│ │  PostgreSQL  │ │   Grafana    │
    │ (Full-text)  │ │ (Long-term)  │ │  (Dashboard) │
    └──────────────┘ └──────────────┘ └──────────────┘
```

#### 2.2 감사 로그 이벤트 분류

| 카테고리 | 이벤트 유형 | 심각도 | 예시 |
|---------|-----------|--------|------|
| **AUTH** | LOGIN_SUCCESS | INFO | 로그인 성공 |
| **AUTH** | LOGIN_FAILURE | WARN | 로그인 실패 |
| **AUTH** | LOGOUT | INFO | 로그아웃 |
| **AUTH** | TOKEN_REFRESH | INFO | 토큰 갱신 |
| **AUTH** | PASSWORD_CHANGE | INFO | 비밀번호 변경 |
| **ACCESS** | RESOURCE_ACCESS | INFO | 리소스 조회 |
| **ACCESS** | UNAUTHORIZED_ACCESS | WARN | 권한 없는 접근 시도 |
| **DATA** | DATA_CREATE | INFO | 데이터 생성 |
| **DATA** | DATA_UPDATE | INFO | 데이터 수정 |
| **DATA** | DATA_DELETE | WARN | 데이터 삭제 |
| **ADMIN** | USER_ROLE_CHANGE | WARN | 사용자 권한 변경 |
| **ADMIN** | SYSTEM_CONFIG_CHANGE | CRITICAL | 시스템 설정 변경 |

#### 2.3 감사 로그 JSON 스키마

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-23T10:15:30.123Z",
  "service": "auth-service",
  "category": "AUTH",
  "eventType": "LOGIN_FAILURE",
  "severity": "WARN",
  "actor": {
    "userId": null,
    "email": "attacker@example.com",
    "ip": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "sessionId": null
  },
  "target": {
    "type": "USER_ACCOUNT",
    "id": "user@portal.com",
    "name": "User Account"
  },
  "action": {
    "method": "POST",
    "endpoint": "/api/auth/login",
    "parameters": {
      "email": "user@portal.com"
    }
  },
  "result": {
    "status": "FAILURE",
    "errorCode": "A001",
    "message": "Invalid password",
    "duration": 245
  },
  "context": {
    "traceId": "abc123def456",
    "spanId": "span789",
    "attemptCount": 5
  }
}
```

#### 2.4 common-library 감사 모듈 구조

```
services/common-library/
└── src/main/java/com/portal/universe/common/
    └── audit/
        ├── annotation/
        │   └── AuditLog.java              # @AuditLog 어노테이션
        ├── aspect/
        │   └── AuditLogAspect.java        # AOP Aspect
        ├── event/
        │   ├── AuditEvent.java            # 이벤트 DTO
        │   ├── AuditEventType.java        # 이벤트 유형 Enum
        │   └── AuditSeverity.java         # 심각도 Enum
        ├── publisher/
        │   └── KafkaAuditPublisher.java   # Kafka 발행
        └── config/
            └── AuditAutoConfiguration.java # 자동 설정
```

#### 2.5 사용 예시

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @AuditLog(
        category = AuditCategory.AUTH,
        eventType = "LOGIN_ATTEMPT",
        includeRequestBody = false,  // 비밀번호 제외
        includeResponseBody = false
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody LoginRequest request) {
        // 로그인 로직
    }

    @AuditLog(
        category = AuditCategory.ADMIN,
        eventType = "USER_ROLE_CHANGE",
        severity = AuditSeverity.WARN
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @PathVariable String userId,
            @RequestBody RoleChangeRequest request) {
        // 권한 변경 로직
    }
}
```

---

### 3. 로그인 보안 아키텍처

#### 3.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                      Login Request Flow                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Auth Service (:8081)                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  LoginAttemptService                      │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  1. 계정 잠금 상태 확인 (Redis)                      │  │  │
│  │  │  2. 잠금 → 423 Locked 응답                          │  │  │
│  │  │  3. 미잠금 → 로그인 시도 횟수 확인                   │  │  │
│  │  │  4. 지연 시간 계산 (점진적 지연)                     │  │  │
│  │  │  5. 지연 적용 후 인증 진행                          │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                             │                                   │
│                             ▼                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Authentication                           │  │
│  │  - BCrypt 비밀번호 검증                                   │  │
│  │  - 성공 → 카운터 초기화, JWT 발급                        │  │
│  │  - 실패 → 카운터 증가, 잠금 여부 판단                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │      Redis       │
                    │  - 실패 카운터   │
                    │  - 잠금 상태     │
                    │  - 지연 시간     │
                    └──────────────────┘
```

#### 3.2 점진적 지연 정책 (Progressive Delay)

| 실패 횟수 | 지연 시간 | 누적 시간 | 비고 |
|----------|----------|----------|------|
| 1회 | 0초 | 0초 | 즉시 재시도 가능 |
| 2회 | 1초 | 1초 | |
| 3회 | 2초 | 3초 | |
| 4회 | 4초 | 7초 | |
| 5회 | 8초 | 15초 | 경고 메시지 표시 |
| 6-9회 | 16초 | 31초-79초 | |
| 10회 | 계정 잠금 | - | 30분 잠금 |

```
지연 시간 계산식: delay = 2^(failCount - 2) 초 (최대 16초)
```

#### 3.3 계정 잠금 정책

| 조건 | 잠금 기간 | 해제 방법 |
|------|----------|----------|
| 10회 연속 실패 | 30분 | 자동 해제 |
| 20회 연속 실패 | 2시간 | 자동 해제 |
| 30회 연속 실패 | 24시간 | 관리자 또는 이메일 인증 |
| IP 기반 다계정 공격 | 1시간 | 자동 해제 |

#### 3.4 Redis Key 구조

```
login_attempt:{email}:count → "5"
login_attempt:{email}:locked_until → "1705901400" (Unix timestamp)
login_attempt:{email}:last_attempt → "1705900800"

login_attempt:ip:{ip}:count → "50"  # IP 기반 카운터 (다계정 공격 탐지)

TTL 정책:
- count: 30분 (마지막 시도 후)
- locked_until: 잠금 해제 시간까지
- IP count: 1시간
```

#### 3.5 응답 표준화

**로그인 실패 (지연 적용)**:
```json
{
  "success": false,
  "code": "A001",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "data": null,
  "meta": {
    "attemptCount": 5,
    "remainingAttempts": 5,
    "nextRetryAfter": 8
  }
}
```

**계정 잠금**:
```json
{
  "success": false,
  "code": "A002",
  "message": "계정이 일시적으로 잠금되었습니다.",
  "data": null,
  "meta": {
    "lockedUntil": "2026-01-23T11:00:00Z",
    "unlockMethod": "AUTOMATIC",
    "supportEmail": "support@portal.com"
  }
}
```
HTTP Status: 423 Locked

#### 3.6 계정 잠금 해제 전략

```
┌─────────────────────────────────────────────────────────────────┐
│                    Account Unlock Strategies                    │
└─────────────────────────────────────────────────────────────────┘

1. 자동 해제 (Time-based)
   ┌────────────┐         ┌────────────┐
   │ 잠금 발생  │ ──30분──▶ │ 자동 해제  │
   └────────────┘         └────────────┘

2. 이메일 인증 해제
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │ 해제 요청  │───▶│ 이메일 발송 │───▶│ 링크 클릭  │───▶ 해제
   └────────────┘    │ (6자리 코드)│    │ (5분 유효) │
                     └────────────┘    └────────────┘

3. 관리자 해제
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │ 사용자 문의 │───▶│ 관리자 확인 │───▶│ 수동 해제  │
   └────────────┘    │ (신원 확인) │    └────────────┘
                     └────────────┘

4. 비밀번호 재설정
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │ 재설정 요청 │───▶│ 이메일 인증 │───▶│ 비밀번호   │───▶ 자동 해제
   └────────────┘    └────────────┘    │ 변경 완료  │
                                       └────────────┘
```

---

### 4. 보안 헤더 아키텍처

#### 4.1 Gateway 레벨 보안 헤더

```
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (:8080)                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              SecurityHeadersFilter (Global)               │  │
│  │                                                           │  │
│  │  Response Headers:                                        │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ X-Content-Type-Options: nosniff                     │  │  │
│  │  │ X-Frame-Options: DENY                               │  │  │
│  │  │ X-XSS-Protection: 1; mode=block                     │  │  │
│  │  │ Strict-Transport-Security: max-age=31536000;        │  │  │
│  │  │                            includeSubDomains        │  │  │
│  │  │ Referrer-Policy: strict-origin-when-cross-origin    │  │  │
│  │  │ Permissions-Policy: geolocation=(), camera=()       │  │  │
│  │  │ Content-Security-Policy: <서비스별 정책>            │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.2 보안 헤더 상세

| 헤더 | 값 | 목적 |
|------|-----|------|
| `X-Content-Type-Options` | `nosniff` | MIME 타입 스니핑 방지 |
| `X-Frame-Options` | `DENY` | Clickjacking 방지 |
| `X-XSS-Protection` | `1; mode=block` | XSS 필터 활성화 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 강제 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referer 정보 제한 |
| `Permissions-Policy` | `geolocation=(), camera=(), microphone=()` | 브라우저 기능 제한 |
| `Cache-Control` | `no-store` (민감 데이터) | 캐시 제어 |

#### 4.3 서비스별 CSP (Content-Security-Policy) 정책

**API 응답 (기본 정책)**:
```
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
```

**Portal Shell (Vue 3 Host)**:
```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net;
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
  font-src 'self' https://fonts.gstatic.com;
  img-src 'self' data: https:;
  connect-src 'self' http://localhost:* https://api.portal.com;
  frame-src 'self' http://localhost:30001 http://localhost:30002;
  base-uri 'self';
  form-action 'self';
```

**Blog Frontend (Remote)**:
```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'unsafe-inline';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  connect-src 'self' http://localhost:8080;
```

**Shopping Frontend (React Remote)**:
```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'unsafe-inline' 'unsafe-eval';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https: blob:;
  connect-src 'self' http://localhost:8080 https://payment.example.com;
```

#### 4.4 CSP 정책 매핑 전략

```
┌─────────────────────────────────────────────────────────────────┐
│                    CSP Policy Mapping                           │
└─────────────────────────────────────────────────────────────────┘

요청 경로 기반 CSP 적용:

  /api/**              → API 기본 정책 (가장 제한적)
  /                    → Portal Shell 정책
  /blog/**             → Blog Frontend 정책
  /shopping/**         → Shopping Frontend 정책

┌──────────────────────────────────────────────────────────────┐
│ Gateway SecurityHeadersFilter                                 │
│                                                               │
│ if (path.startsWith("/api")) {                               │
│     response.addHeader("CSP", apiPolicy);                    │
│ } else if (path.startsWith("/blog")) {                       │
│     response.addHeader("CSP", blogPolicy);                   │
│ } else if (path.startsWith("/shopping")) {                   │
│     response.addHeader("CSP", shoppingPolicy);               │
│ } else {                                                      │
│     response.addHeader("CSP", shellPolicy);                  │
│ }                                                             │
└──────────────────────────────────────────────────────────────┘
```

---

## Consequences (영향)

### 긍정적 영향

| 영역 | 효과 |
|------|------|
| **DDoS 방어** | Rate Limiting으로 서비스 안정성 확보 |
| **규정 준수** | 감사 로그로 개인정보보호법/ISMS 대응 |
| **침해 대응** | 상세 로그로 보안 사고 추적 및 분석 가능 |
| **계정 보호** | 점진적 지연/잠금으로 무차별 대입 공격 방어 |
| **브라우저 보안** | 보안 헤더로 XSS, Clickjacking 등 방어 |
| **통합 관리** | common-library 모듈로 일관된 보안 적용 |

### 부정적 영향 (트레이드오프)

| 영역 | 영향 | 완화 방안 |
|------|------|----------|
| **성능** | Redis 조회 증가 (Rate Limit, Login Attempt) | Redis Cluster, 로컬 캐시 |
| **복잡도** | 새로운 컴포넌트 추가 | 자동 설정, 문서화 |
| **사용자 경험** | 로그인 지연, 잠금 발생 | 명확한 안내 메시지 |
| **운영 부하** | 로그 저장 공간 증가 | 보관 정책, 압축 |
| **Redis 의존성** | Redis 장애 시 보안 기능 저하 | Redis HA, Fallback 정책 |

### 리스크 분석

| 리스크 | 가능성 | 영향 | 대응 |
|--------|--------|------|------|
| Redis 장애 | 낮음 | 높음 | Sentinel/Cluster, Circuit Breaker |
| 로그 유실 | 낮음 | 중간 | Kafka 복제, 재전송 |
| 오탐 (정상 사용자 차단) | 중간 | 중간 | 화이트리스트, 임계값 조정 |
| CSP 호환성 문제 | 중간 | 낮음 | Report-Only 모드 선행 |

---

## Implementation Roadmap

### Phase 1: Rate Limiting (1주)
- Gateway Rate Limiter Filter 구현
- Redis 연동
- 429 응답 표준화

### Phase 2: 로그인 보안 (1주)
- LoginAttemptService 구현
- 점진적 지연 로직
- 계정 잠금/해제 기능

### Phase 3: 보안 감사 로깅 (2주)
- common-library 감사 모듈
- Kafka Producer/Consumer
- ELK 연동

### Phase 4: 보안 헤더 (3일)
- Gateway 보안 헤더 필터
- 서비스별 CSP 정책
- 테스트 및 검증

---

## 참고 자료

- [OWASP Rate Limiting](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [MDN Content-Security-Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [ADR-003: Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)
- [ADR-008: JWT Stateless + Redis](./ADR-008-jwt-stateless-redis.md)

---

**최종 업데이트**: 2026-01-23
**작성자**: Architect Agent
