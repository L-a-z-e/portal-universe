# Portal Universe - Security Enhancement Plan

## 문서 정보
- **버전**: 1.0
- **작성일**: 2026-01-23
- **작성자**: Security Agent
- **상태**: Draft

---

## 목차
1. [현재 보안 상태](#1-현재-보안-상태)
2. [취약점 분석](#2-취약점-분석)
3. [위협 모델링 (STRIDE)](#3-위협-모델링-stride)
4. [보안 정책 정의](#4-보안-정책-정의)
5. [구현 우선순위 및 일정](#5-구현-우선순위-및-일정)
6. [테스트 전략](#6-테스트-전략)
7. [모니터링 및 감사](#7-모니터링-및-감사)

---

## 1. 현재 보안 상태

### 1.1 인증 및 인가

| 구성 요소 | 현재 상태 | 평가 |
|-----------|-----------|------|
| **JWT 기반 인증** | HMAC-SHA256, Access 15분/Refresh 7일 | ✅ 양호 |
| **토큰 블랙리스트** | Redis 기반 로그아웃/토큰 무효화 | ✅ 양호 |
| **OAuth2 소셜 로그인** | Google, Naver, Kakao 지원 | ✅ 양호 |
| **API Gateway JWT 검증** | 모든 요청에 대한 토큰 검증 | ✅ 양호 |
| **RBAC** | `@PreAuthorize` 기반 권한 제어 | ✅ 양호 |

### 1.2 네트워크 보안

| 구성 요소 | 현재 상태 | 평가 |
|-----------|-----------|------|
| **K8s NetworkPolicy** | Pod 간 통신 제한 | ✅ 양호 |
| **TLS/HTTPS** | (상태 확인 필요) | ⚠️ 확인 필요 |
| **CORS 설정** | API Gateway 적용 | ✅ 양호 |

### 1.3 프론트엔드 보안

| 구성 요소 | 현재 상태 | 평가 |
|-----------|-----------|------|
| **Access Token** | 메모리 저장 (XSS 방어) | ✅ 양호 |
| **Refresh Token** | localStorage 저장 | ⚠️ 개선 필요 |
| **Route Guard** | RequireRole 컴포넌트 | ✅ 양호 |
| **보안 헤더** | (미구현) | ❌ 위험 |

### 1.4 데이터 보안

| 구성 요소 | 현재 상태 | 평가 |
|-----------|-----------|------|
| **비밀번호 해싱** | BCrypt | ✅ 양호 |
| **민감 정보 암호화** | (상태 확인 필요) | ⚠️ 확인 필요 |
| **환경 변수 관리** | `.env` 파일 사용 | ⚠️ 개선 필요 |
| **Secret 관리** | K8s Secrets (상태 확인 필요) | ⚠️ 확인 필요 |

---

## 2. 취약점 분석

### 2.1 Critical (긴급 대응 필요)

#### C1. Rate Limiting 미구현
- **위험도**: Critical
- **영향**: Brute Force 공격, DDoS 취약
- **현상**:
  - 로그인 API 무제한 시도 가능
  - 회원가입 API 스팸 등록 가능
  - 모든 엔드포인트 Rate Limit 없음
- **공격 시나리오**:
  ```
  공격자 → /api/auth/login (무한 반복)
  → 비밀번호 무차별 대입 (Brute Force)
  → 계정 탈취
  ```
- **영향 범위**: 전체 서비스
- **대응 방안**: Spring Cloud Gateway Rate Limiter 적용

#### C2. 보안 감사 로깅 부재
- **위험도**: Critical
- **영향**: 보안 사고 추적 불가, 규제 준수 실패
- **현상**:
  - 로그인 실패 이벤트 미기록
  - 권한 위반 시도 미기록
  - 민감한 작업(비밀번호 변경, 개인정보 수정) 감사 로그 없음
- **공격 시나리오**:
  ```
  공격자 → 관리자 계정 탈취 시도
  → 실패해도 로그 없음
  → 반복 시도 후 성공
  → 추적 불가능
  ```
- **영향 범위**: 전체 서비스, 규제 준수(GDPR, 개인정보보호법)
- **대응 방안**: Spring Security AuditEvent, ELK Stack 연동

### 2.2 High (빠른 대응 필요)

#### H1. 로그인 실패 제한 없음
- **위험도**: High
- **영향**: 계정 탈취, 서비스 부하
- **현상**:
  - 동일 계정에 무한 로그인 시도 가능
  - 실패 횟수 제한 없음
  - 계정 잠금 메커니즘 없음
- **대응 방안**: Redis 기반 실패 카운터 + 계정 일시 잠금

#### H2. JWT Secret 관리 개선 필요
- **위험도**: High
- **영향**: 토큰 위조 가능
- **현상**:
  - `.env` 파일에 평문 저장
  - Secret Rotation 메커니즘 없음
  - 서비스별 Secret 분리 안됨
- **대응 방안**: K8s Secrets + Secret Rotation + 서비스별 분리

#### H3. Refresh Token localStorage 저장
- **위험도**: High
- **영향**: XSS 공격 시 토큰 탈취
- **현상**:
  - Refresh Token이 JavaScript로 접근 가능
  - XSS 공격 시 7일간 유효한 토큰 노출
- **대응 방안**: HttpOnly Cookie + SameSite 속성

#### H4. 보안 헤더 미설정
- **위험도**: High
- **영향**: XSS, Clickjacking, MIME Sniffing 공격 취약
- **현상**:
  - `Content-Security-Policy` 미설정
  - `X-Frame-Options` 미설정
  - `X-Content-Type-Options` 미설정
  - `Strict-Transport-Security` 미설정
- **대응 방안**: Spring Security Headers 설정

### 2.3 Medium (계획적 대응)

#### M1. 비밀번호 복잡도 정책 미흡
- **위험도**: Medium
- **영향**: 약한 비밀번호로 계정 생성 가능
- **현상**:
  - 최소 길이만 검증 (8자)
  - 복잡도 요구사항 없음 (숫자, 특수문자 조합)
  - 흔한 비밀번호 필터링 없음
- **대응 방안**: Passay 라이브러리 + 복잡도 정책

#### M2. Session Timeout 설정 부재
- **위험도**: Medium
- **영향**: 방치된 세션 악용 가능
- **현상**:
  - Access Token 15분 (적절)
  - Refresh Token 7일 (검토 필요)
  - 절대적 만료 시간 없음 (Sliding Session)
- **대응 방안**: Absolute Timeout 추가 (예: 24시간)

#### M3. API 입력 검증 불완전
- **위험도**: Medium
- **영향**: SQL Injection, NoSQL Injection
- **현상**:
  - `@Valid` 사용하지만 모든 엔드포인트 적용 안됨
  - 커스텀 검증 로직 부재
- **대응 방안**: 전체 엔드포인트 검증 점검

#### M4. CORS 설정 과도하게 개방적
- **위험도**: Medium
- **영향**: CSRF 공격 가능성
- **현상**:
  - 개발 환경에서 `allowedOrigins: "*"` 사용 가능성
  - Production 환경 검증 필요
- **대응 방안**: 환경별 명시적 Origin 리스트

---

## 3. 위협 모델링 (STRIDE)

### 3.1 Spoofing (위장)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **JWT 토큰 위조** | HMAC-SHA256 서명 검증 | Secret 관리 미흡 | Secret Rotation, K8s Secrets | High |
| **소셜 로그인 위장** | OAuth2 Provider 검증 | - | - | - |
| **사용자 신원 도용** | 비밀번호 해싱 (BCrypt) | 2FA 없음 | 2FA 도입 검토 | Low |

**위험 시나리오**:
```
공격자 → JWT Secret 탈취 (환경 변수 노출)
→ 위조 토큰 생성 (admin 권한)
→ 관리자 API 호출
→ 시스템 장악
```

**완화 방안**:
- JWT Secret을 K8s Secret으로 이동
- Secret Rotation 정책 수립 (30일 주기)
- 로그인 시 디바이스 지문(fingerprint) 검증 추가

### 3.2 Tampering (변조)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **API 요청 변조** | JWT 서명 검증, HTTPS | TLS 상태 확인 필요 | TLS 1.3 강제, HSTS | High |
| **Database 변조** | JPA Auditing, 트랜잭션 | 감사 로그 부재 | DB Audit Trigger | Medium |
| **Frontend 변조** | CSP 미설정 | XSS 취약 | CSP 헤더 추가 | High |

**위험 시나리오**:
```
공격자 → XSS 취약점 발견
→ JavaScript 주입
→ 사용자 요청 변조 (가격 조작)
→ 부정 주문
```

**완화 방안**:
- Content-Security-Policy 헤더 설정
- 입력 검증 강화 (모든 엔드포인트)
- DB 트리거 기반 변경 이력 추적

### 3.3 Repudiation (부인)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **작업 부인** | JPA Auditing (createdBy/modifiedBy) | 감사 로그 부재 | 보안 이벤트 로깅 | Critical |
| **로그인 부인** | 로그 없음 | 추적 불가 | Spring Security AuditEvent | Critical |
| **주문 부인** | 주문 이력 보관 | 디지털 서명 없음 | - | Low |

**위험 시나리오**:
```
사용자 → 불법 행위 수행 (악의적 주문 취소 반복)
→ "내가 한 게 아니다" 주장
→ 로그 없음
→ 입증 불가
```

**완화 방안**:
- ELK Stack을 통한 중앙 로깅
- 보안 이벤트 별도 저장 (변조 불가능한 방식)
- 타임스탬프 + IP 주소 + User Agent 기록

### 3.4 Information Disclosure (정보 노출)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **토큰 노출** | Access Token 메모리, Refresh Token localStorage | Refresh Token XSS 취약 | HttpOnly Cookie | High |
| **에러 메시지 노출** | GlobalExceptionHandler | Stack Trace 노출 가능성 | Production에서 상세 에러 숨김 | Medium |
| **민감 정보 로깅** | (확인 필요) | 비밀번호, 토큰 로그 가능성 | 로그 마스킹 정책 | Medium |
| **CORS Misconfiguration** | 설정됨 | 과도한 허용 가능성 | 엄격한 Origin 제한 | Medium |

**위험 시나리오**:
```
공격자 → XSS 공격 성공
→ localStorage에서 Refresh Token 탈취
→ 7일간 유효한 토큰으로 API 호출
→ 사용자 데이터 탈취
```

**완화 방안**:
- Refresh Token을 HttpOnly Cookie로 이동
- 에러 응답에서 민감한 정보 제거
- 로깅 시 자동 마스킹 (비밀번호, 카드번호, 토큰)

### 3.5 Denial of Service (서비스 거부)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **Brute Force 공격** | 없음 | 무제한 로그인 시도 가능 | Rate Limiting + 계정 잠금 | Critical |
| **DDoS** | K8s HPA | Rate Limiting 없음 | API Gateway Rate Limiter | Critical |
| **Resource Exhaustion** | 없음 | 무제한 API 호출 가능 | Rate Limiting + Circuit Breaker | Critical |

**위험 시나리오**:
```
공격자 → 로그인 API에 초당 1000회 요청
→ DB 과부하
→ 정상 사용자 로그인 불가
→ 서비스 다운
```

**완화 방안**:
- Spring Cloud Gateway Rate Limiter (Token Bucket 알고리즘)
- Redis 기반 분산 Rate Limiting
- Resilience4j Circuit Breaker
- K8s 리소스 쿼터 설정

### 3.6 Elevation of Privilege (권한 상승)

| 위협 | 현재 대응 | 취약점 | 개선 방안 | 우선순위 |
|------|-----------|--------|-----------|----------|
| **수직 권한 상승** | `@PreAuthorize`, Route Guard | 누락된 엔드포인트 가능성 | 전체 엔드포인트 감사 | High |
| **수평 권한 상승** | Resource Owner 검증 | 일부 엔드포인트 미구현 | 전체 엔드포인트 감사 | High |
| **JWT 역할 조작** | 서명 검증 | Secret 노출 시 위험 | Secret 관리 강화 | High |

**위험 시나리오**:
```
일반 사용자 → Admin API 직접 호출
→ @PreAuthorize 누락된 엔드포인트 발견
→ 관리자 권한으로 작업 수행
→ 데이터 변조
```

**완화 방안**:
- 모든 엔드포인트에 권한 검증 추가
- Default Deny 정책 (명시적으로 허용되지 않으면 거부)
- 정기적인 권한 검증 감사
- 통합 테스트로 권한 검증 자동화

---

## 4. 보안 정책 정의

### 4.1 인증 정책 (Authentication Policy)

#### 4.1.1 비밀번호 정책
```yaml
Password Complexity Requirements:
  - Minimum Length: 10 characters (현재 8자에서 강화)
  - Character Types Required:
    - Lowercase letters (a-z): 최소 1개
    - Uppercase letters (A-Z): 최소 1개
    - Numbers (0-9): 최소 1개
    - Special characters (!@#$%^&*): 최소 1개
  - Forbidden Patterns:
    - Common passwords (e.g., "password123")
    - Sequential characters (e.g., "abc123", "qwerty")
    - Repeated characters (e.g., "aaaaaa")
    - User information (username, email prefix)
  - Password History: 최근 3개 비밀번호 재사용 금지
  - Password Expiration: 90일 (선택적)
```

#### 4.1.2 계정 잠금 정책
```yaml
Account Lockout Policy:
  - Failed Login Threshold: 5회
  - Lockout Duration: 30분 (자동 해제)
  - Lockout Reset: 성공 로그인 시 카운터 초기화
  - IP-based Lockout: 동일 IP에서 10회 실패 시 1시간 차단
  - Notification: 계정 잠금 시 이메일 알림
```

#### 4.1.3 세션 관리 정책
```yaml
Session Management:
  - Access Token Lifetime: 15분 (현재 유지)
  - Refresh Token Lifetime: 7일 (현재 유지)
  - Absolute Session Timeout: 24시간 (신규)
  - Idle Timeout: 2시간 (신규)
  - Concurrent Session Limit: 3개 디바이스 (신규)
  - Token Storage:
    - Access Token: 메모리 (현재 유지)
    - Refresh Token: HttpOnly Cookie + SameSite=Strict (변경)
```

### 4.2 접근 제어 정책 (Access Control Policy)

#### 4.2.1 RBAC 규칙
```yaml
Roles:
  - ROLE_USER:
    - Read own profile
    - Read public resources
    - Create orders
    - Read own orders

  - ROLE_ADMIN:
    - All ROLE_USER permissions
    - CRUD products
    - Read all orders
    - Manage users (except delete)

  - ROLE_SUPER_ADMIN:
    - All ROLE_ADMIN permissions
    - Delete users
    - Manage system settings
    - Access audit logs

Default Deny:
  - 모든 엔드포인트는 기본적으로 인증 필요
  - 명시적으로 허용된 경로만 public 접근 가능
  - Public Endpoints:
    - POST /api/auth/login
    - POST /api/auth/register
    - POST /api/auth/oauth2/**
    - GET /api/products (목록 조회)
    - GET /api/products/{id} (상세 조회)
    - GET /actuator/health
```

#### 4.2.2 Resource Owner 검증
```yaml
Horizontal Access Control:
  - 주문 조회: 본인 주문만 조회 가능
  - 프로필 수정: 본인 프로필만 수정 가능
  - 리뷰 작성: 구매한 상품만 리뷰 작성 가능
  - 예외: ROLE_ADMIN은 모든 리소스 접근 가능

Implementation:
  - Service Layer에서 검증
  - currentUser.email == resource.ownerEmail
  - 실패 시 403 Forbidden + UNAUTHORIZED_ACCESS 에러 코드
```

### 4.3 로깅 정책 (Logging Policy)

#### 4.3.1 보안 이벤트 로깅
```yaml
Security Events to Log:
  Authentication:
    - Login Success: email, timestamp, IP, User-Agent
    - Login Failure: email (if provided), timestamp, IP, reason
    - Logout: email, timestamp, reason (manual/forced/expired)
    - Password Change: email, timestamp, IP
    - Password Reset Request: email, timestamp, IP
    - Account Locked: email, timestamp, reason, lockout duration

  Authorization:
    - Access Denied (403): email, endpoint, timestamp, IP
    - Role Assignment Changed: target user, new roles, changed by, timestamp

  Token Management:
    - Token Issued: email, token type, expiration, IP
    - Token Refreshed: email, timestamp, IP
    - Token Revoked: email, timestamp, reason
    - Token Blacklisted: token ID, reason, timestamp

  Data Access:
    - Sensitive Data Read: email, resource type, resource ID, timestamp
    - Sensitive Data Modified: email, resource type, resource ID, changes, timestamp
    - Sensitive Data Deleted: email, resource type, resource ID, timestamp

Log Format:
  {
    "timestamp": "2026-01-23T10:30:45.123Z",
    "event_type": "LOGIN_SUCCESS",
    "user": {
      "email": "user@example.com",
      "uuid": "550e8400-e29b-41d4-a716-446655440000"
    },
    "metadata": {
      "ip": "192.168.1.100",
      "user_agent": "Mozilla/5.0...",
      "session_id": "abc123"
    },
    "result": "SUCCESS"
  }
```

#### 4.3.2 민감 정보 마스킹
```yaml
Masking Rules:
  - Password: 전체 마스킹 (********)
  - Email: 부분 마스킹 (u***@example.com)
  - Phone: 중간 마스킹 (010-****-1234)
  - Credit Card: 중간 마스킹 (**** **** **** 1234)
  - JWT Token: 마지막 8자만 표시 (생략...abc123)
  - SSN/주민번호: 절대 로깅 금지

Implementation:
  - Logback Masking Pattern
  - Spring AOP for method logging
  - 실수로 로깅되더라도 자동 마스킹
```

#### 4.3.3 로그 보관 정책
```yaml
Log Retention:
  - Application Logs: 30일
  - Security Audit Logs: 1년
  - Access Logs: 90일
  - Error Logs: 90일

Log Storage:
  - Development: 로컬 파일 (7일)
  - Staging: ELK Stack (30일)
  - Production: ELK Stack (보관 정책 준수)

Log Access Control:
  - Application Logs: DevOps 팀
  - Security Audit Logs: Security 팀, Compliance 팀
  - 개인정보 포함 로그: 접근 시 별도 승인 필요
```

### 4.4 Rate Limiting 정책

#### 4.4.1 엔드포인트별 제한
```yaml
Rate Limits (per IP address):
  Authentication Endpoints:
    - POST /api/auth/login:
      - Rate: 5 requests per minute
      - Burst: 10 requests
      - Penalty: 403 + 1분 차단

    - POST /api/auth/register:
      - Rate: 3 requests per hour
      - Burst: 5 requests
      - Penalty: 403 + 1시간 차단

    - POST /api/auth/refresh:
      - Rate: 10 requests per minute
      - Burst: 20 requests

    - POST /api/auth/password-reset:
      - Rate: 3 requests per hour
      - Burst: 5 requests

  Public API:
    - GET /api/products:
      - Rate: 100 requests per minute
      - Burst: 200 requests

    - GET /api/products/{id}:
      - Rate: 100 requests per minute
      - Burst: 200 requests

  Authenticated API:
    - General Endpoints:
      - Rate: 1000 requests per minute (per user)
      - Burst: 2000 requests

    - POST /api/orders:
      - Rate: 10 requests per minute
      - Burst: 20 requests

    - Admin Endpoints:
      - Rate: 500 requests per minute
      - Burst: 1000 requests

Global Rate Limit:
  - Per IP: 10,000 requests per hour
  - Per User: 50,000 requests per hour
```

#### 4.4.2 구현 방법
```yaml
Implementation:
  - Layer: API Gateway (Spring Cloud Gateway)
  - Algorithm: Token Bucket
  - Storage: Redis (분산 환경)
  - Response Headers:
    - X-RateLimit-Limit: 요청 제한
    - X-RateLimit-Remaining: 남은 요청 수
    - X-RateLimit-Reset: 리셋 시간 (Unix timestamp)
  - Error Response (429 Too Many Requests):
    {
      "success": false,
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "요청 한도를 초과했습니다. 1분 후 다시 시도해주세요.",
      "metadata": {
        "retry_after": 60
      }
    }
```

### 4.5 암호화 정책

#### 4.5.1 데이터 암호화
```yaml
Encryption at Rest:
  Database:
    - Sensitive Columns: AES-256-GCM
      - 실명 (real_name)
      - 전화번호 (phone_number)
      - 소셜 로그인 토큰 (social_accounts.access_token)
    - Key Management: K8s Secrets + External KMS (향후)

  File Storage:
    - 프로필 이미지: 암호화 불필요 (public)
    - 업로드 파일: 암호화 필요 시 S3 SSE-KMS

Encryption in Transit:
  - Internal Service Communication: TLS 1.3
  - External API: TLS 1.3 + HSTS
  - Database Connection: TLS 필수

Key Management:
  - JWT Secret: K8s Secrets (256-bit random)
  - Encryption Keys: K8s Secrets (향후 AWS KMS 또는 HashiCorp Vault)
  - Key Rotation: 90일 주기 (JWT Secret)
```

#### 4.5.2 해싱 정책
```yaml
Password Hashing:
  - Algorithm: BCrypt
  - Work Factor: 12 (2^12 iterations)
  - Salt: 자동 생성 (BCrypt 내장)

Other Hashing:
  - Session ID: SHA-256
  - File Checksum: SHA-256
  - API Key: SHA-256 (필요 시)
```

---

## 5. 구현 우선순위 및 일정

### 5.1 Phase 1: Critical (Week 1-2, 즉시 착수)

| 작업 | 담당 | 예상 기간 | 의존성 | 상태 |
|------|------|-----------|--------|------|
| **Rate Limiting 구현** | Backend | 3일 | Redis 설정 | Planned |
| **보안 감사 로깅** | Backend | 3일 | ELK Stack 연동 | Planned |
| **로그인 실패 제한** | Backend | 2일 | Redis 설정 | Planned |
| **JWT Secret을 K8s Secrets로 이동** | DevOps | 1일 | - | Planned |
| **Refresh Token을 HttpOnly Cookie로 변경** | Backend + Frontend | 2일 | - | Planned |

**총 기간**: 2주 (병렬 진행 시)

#### 5.1.1 Rate Limiting 구현

**목표**: API Gateway에서 Token Bucket 알고리즘 기반 Rate Limiting 적용

**구현 단계**:
1. Redis Rate Limiter 의존성 추가
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
   </dependency>
   ```

2. API Gateway에 Rate Limiter 설정
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: auth-service
             uri: lb://auth-service
             predicates:
               - Path=/api/auth/**
             filters:
               - name: RequestRateLimiter
                 args:
                   redis-rate-limiter:
                     replenishRate: 5  # 초당 5개 토큰 추가
                     burstCapacity: 10  # 최대 10개 토큰 보유
                     requestedTokens: 1  # 요청당 1개 토큰 소비
                   key-resolver: "#{@ipKeyResolver}"
   ```

3. KeyResolver 구현
   ```java
   @Bean
   public KeyResolver ipKeyResolver() {
       return exchange -> Mono.just(
           exchange.getRequest()
                   .getRemoteAddress()
                   .getAddress()
                   .getHostAddress()
       );
   }
   ```

4. 테스트
   - JMeter로 부하 테스트 (100 concurrent users)
   - 429 응답 확인
   - Redis에 저장된 토큰 버킷 상태 확인

**완료 조건**:
- [ ] Redis Rate Limiter 동작 확인
- [ ] 엔드포인트별 제한 적용 완료
- [ ] 429 에러 응답 테스트 통과
- [ ] 부하 테스트 결과 문서화

#### 5.1.2 보안 감사 로깅

**목표**: 모든 보안 이벤트를 ELK Stack에 중앙 집중식 로깅

**구현 단계**:
1. Spring Security AuditEvent 설정
   ```java
   @Service
   public class SecurityAuditService {
       private final ApplicationEventPublisher publisher;

       public void auditLoginSuccess(String email, String ip) {
           publisher.publishEvent(new AuditEvent(
               email,
               "LOGIN_SUCCESS",
               Map.of("ip", ip, "timestamp", Instant.now())
           ));
       }

       public void auditLoginFailure(String email, String ip, String reason) {
           publisher.publishEvent(new AuditEvent(
               email,
               "LOGIN_FAILURE",
               Map.of("ip", ip, "reason", reason, "timestamp", Instant.now())
           ));
       }
   }
   ```

2. Logback JSON 포맷 설정
   ```xml
   <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
       <encoder class="net.logstash.logback.encoder.LogstashEncoder">
           <fieldNames>
               <timestamp>timestamp</timestamp>
               <message>message</message>
               <logger>logger</logger>
               <level>level</level>
           </fieldNames>
       </encoder>
   </appender>
   ```

3. ELK Stack 연동
   - Filebeat으로 로그 수집
   - Logstash로 파싱 및 필터링
   - Elasticsearch에 저장
   - Kibana 대시보드 생성

4. 민감 정보 마스킹
   ```xml
   <appender name="MASKED" class="ch.qos.logback.core.ConsoleAppender">
       <encoder class="net.logstash.logback.encoder.LogstashEncoder">
           <provider class="net.logstash.logback.composite.loggingevent.MaskingJsonProvider">
               <maskPattern>password=.*</maskPattern>
               <maskPattern>token=.*</maskPattern>
           </provider>
       </encoder>
   </appender>
   ```

**완료 조건**:
- [ ] SecurityAuditService 구현 완료
- [ ] 모든 보안 이벤트 로깅 적용
- [ ] ELK Stack 연동 완료
- [ ] Kibana 대시보드 생성
- [ ] 민감 정보 마스킹 테스트 통과

#### 5.1.3 로그인 실패 제한

**목표**: Redis 기반 실패 카운터 + 계정 일시 잠금

**구현 단계**:
1. Redis에 실패 카운터 저장
   ```java
   @Service
   public class LoginAttemptService {
       private final RedisTemplate<String, Integer> redisTemplate;
       private static final int MAX_ATTEMPTS = 5;
       private static final int LOCKOUT_DURATION = 30; // 분

       public void loginFailed(String email) {
           String key = "login:failed:" + email;
           Integer attempts = redisTemplate.opsForValue().get(key);
           if (attempts == null) {
               attempts = 0;
           }
           attempts++;
           redisTemplate.opsForValue().set(key, attempts,
               Duration.ofMinutes(LOCKOUT_DURATION));

           if (attempts >= MAX_ATTEMPTS) {
               lockAccount(email);
           }
       }

       public boolean isAccountLocked(String email) {
           String key = "login:locked:" + email;
           return Boolean.TRUE.equals(redisTemplate.hasKey(key));
       }

       private void lockAccount(String email) {
           String key = "login:locked:" + email;
           redisTemplate.opsForValue().set(key, true,
               Duration.ofMinutes(LOCKOUT_DURATION));
           // 이메일 알림 전송
       }
   }
   ```

2. AuthenticationService에 통합
   ```java
   public TokenResponse login(LoginRequest request) {
       if (loginAttemptService.isAccountLocked(request.getEmail())) {
           throw new CustomBusinessException(
               AuthErrorCode.ACCOUNT_LOCKED
           );
       }

       try {
           // 로그인 로직
           loginAttemptService.loginSucceeded(request.getEmail());
           return tokenResponse;
       } catch (BadCredentialsException e) {
           loginAttemptService.loginFailed(request.getEmail());
           throw new CustomBusinessException(
               AuthErrorCode.INVALID_CREDENTIALS
           );
       }
   }
   ```

3. 테스트
   - 5회 실패 후 계정 잠금 확인
   - 30분 후 자동 해제 확인
   - 성공 로그인 시 카운터 리셋 확인

**완료 조건**:
- [ ] LoginAttemptService 구현 완료
- [ ] Redis 저장 로직 테스트 통과
- [ ] 계정 잠금 테스트 통과
- [ ] 이메일 알림 전송 테스트 통과

### 5.2 Phase 2: High (Week 3-4)

| 작업 | 담당 | 예상 기간 | 의존성 | 상태 |
|------|------|-----------|--------|------|
| **보안 헤더 설정** | Backend | 1일 | - | Planned |
| **JWT Secret Rotation 구현** | Backend + DevOps | 3일 | K8s Secrets | Planned |
| **권한 검증 전수 감사** | Backend | 3일 | - | Planned |
| **TLS/HTTPS 강제** | DevOps | 2일 | SSL 인증서 | Planned |

**총 기간**: 2주 (병렬 진행 시)

#### 5.2.1 보안 헤더 설정

**구현 단계**:
1. API Gateway SecurityConfig
   ```java
   @Configuration
   public class SecurityConfig {
       @Bean
       public SecurityWebFilterChain springSecurityFilterChain(
           ServerHttpSecurity http) {
           http.headers(headers -> headers
               .contentSecurityPolicy(csp -> csp
                   .policyDirectives("default-src 'self'; " +
                       "script-src 'self' 'unsafe-inline'; " +
                       "style-src 'self' 'unsafe-inline'; " +
                       "img-src 'self' data: https:;"))
               .frameOptions(frameOptions -> frameOptions
                   .mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
               .xssProtection(xss -> xss
                   .headerValue(XXssProtectionServerHttpHeadersWriter
                       .HeaderValue.ENABLED_MODE_BLOCK))
               .contentTypeOptions(ServerHttpSecurity
                   .HeaderSpec.ContentTypeOptionsSpec::disable)
               .hsts(hsts -> hsts
                   .maxAge(Duration.ofDays(365))
                   .includeSubdomains(true)
                   .preload(true))
           );
           return http.build();
       }
   }
   ```

2. Frontend Nginx 설정 (추가 레이어)
   ```nginx
   add_header Content-Security-Policy "default-src 'self'";
   add_header X-Frame-Options "DENY";
   add_header X-Content-Type-Options "nosniff";
   add_header Referrer-Policy "strict-origin-when-cross-origin";
   add_header Permissions-Policy "geolocation=(), microphone=()";
   ```

**완료 조건**:
- [ ] 모든 보안 헤더 설정 완료
- [ ] SecurityHeaders.com 스캔 A+ 등급

### 5.3 Phase 3: Medium (Week 5-6)

| 작업 | 담당 | 예상 기간 | 의존성 | 상태 |
|------|------|-----------|--------|------|
| **비밀번호 복잡도 정책** | Backend | 2일 | - | Planned |
| **Absolute Session Timeout** | Backend | 2일 | - | Planned |
| **민감 정보 암호화** | Backend | 3일 | KMS 설정 | Planned |
| **CORS 설정 강화** | Backend | 1일 | - | Planned |

**총 기간**: 2주

### 5.4 Phase 4: Low (Week 7-8, 선택적)

| 작업 | 담당 | 예상 기간 | 의존성 | 상태 |
|------|------|-----------|--------|------|
| **2FA (Two-Factor Authentication)** | Backend + Frontend | 5일 | TOTP 라이브러리 | Future |
| **보안 침투 테스트** | Security Team | 3일 | 모든 Phase 완료 | Future |
| **보안 문서화** | All | 2일 | - | Future |

---

## 6. 테스트 전략

### 6.1 단위 테스트

```java
@SpringBootTest
class SecurityConfigTest {

    @Test
    @DisplayName("Rate Limiting: 5회 초과 요청 시 429 반환")
    void rateLimiting_ExceedLimit_Returns429() {
        // Given: 5회 로그인 시도
        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/api/auth/login", request, TokenResponse.class);
        }

        // When: 6번째 요청
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/auth/login", request, ApiResponse.class);

        // Then: 429 Too Many Requests
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("계정 잠금: 5회 실패 시 30분 잠금")
    void accountLockout_FiveFailures_LocksFor30Minutes() {
        // Given: 5회 로그인 실패
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class,
                () -> authService.login(wrongPasswordRequest));
        }

        // When: 6번째 시도
        CustomBusinessException exception = assertThrows(
            CustomBusinessException.class,
            () -> authService.login(correctPasswordRequest)
        );

        // Then: 계정 잠금 에러
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_LOCKED);
    }
}
```

### 6.2 통합 테스트

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Test
    @DisplayName("보안 헤더: CSP 헤더 응답 확인")
    void securityHeaders_ResponseContainsCsp() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Security-Policy"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("권한 검증: USER 역할로 Admin API 호출 시 403")
    @WithMockUser(roles = "USER")
    void authorization_UserRole_AccessAdminApi_Returns403() throws Exception {
        mockMvc.perform(post("/api/shopping/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":1000}"))
            .andExpect(status().isForbidden());
    }
}
```

### 6.3 보안 테스트

#### 6.3.1 OWASP ZAP 스캔
```bash
# Docker로 OWASP ZAP 실행
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t http://localhost:8080 \
  -r zap-report.html

# 결과 분석
# - High/Medium 취약점 0개 목표
# - Low 취약점 최소화
```

#### 6.3.2 침투 테스트 체크리스트
- [ ] SQL Injection 테스트 (SQLMap)
- [ ] XSS 테스트 (XSStrike)
- [ ] CSRF 테스트
- [ ] JWT 토큰 위조 시도
- [ ] Rate Limiting 우회 시도
- [ ] 권한 상승 시도 (수직/수평)
- [ ] Brute Force 공격 시뮬레이션

### 6.4 자동화 테스트

```yaml
# .github/workflows/security-tests.yml
name: Security Tests

on:
  push:
    branches: [main, dev]
  pull_request:
    branches: [main]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run OWASP Dependency Check
        run: |
          mvn dependency-check:check

      - name: Run Trivy Vulnerability Scanner
        run: |
          docker run --rm -v $(pwd):/workspace aquasec/trivy \
            filesystem /workspace --exit-code 1 --severity HIGH,CRITICAL

      - name: Run Security Tests
        run: |
          mvn test -Dtest=*SecurityTest
```

---

## 7. 모니터링 및 감사

### 7.1 보안 메트릭

#### 7.1.1 Prometheus Metrics
```yaml
Security Metrics:
  - auth_login_attempts_total{status="success|failure"}
  - auth_login_failures_by_ip{ip="x.x.x.x"}
  - auth_account_locked_total
  - api_rate_limit_exceeded_total{endpoint="/api/auth/login"}
  - jwt_token_issued_total{type="access|refresh"}
  - jwt_token_expired_total
  - authorization_denied_total{role="USER|ADMIN",endpoint="/api/**"}
```

#### 7.1.2 Grafana 대시보드
```
Dashboard: Security Overview
  - Panel 1: Login Success/Failure Rate (시간대별)
  - Panel 2: Top 10 Failed Login IPs
  - Panel 3: Account Lockout Events
  - Panel 4: Rate Limit Exceeded by Endpoint
  - Panel 5: Authorization Denied by Role
  - Panel 6: Active Sessions Count
```

### 7.2 알림 정책

```yaml
Prometheus Alerting Rules:
  - alert: HighLoginFailureRate
    expr: rate(auth_login_attempts_total{status="failure"}[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "높은 로그인 실패율 감지"
      description: "5분간 초당 10회 이상 로그인 실패"

  - alert: BruteForceAttack
    expr: sum(rate(auth_login_failures_by_ip[5m])) by (ip) > 20
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Brute Force 공격 의심"
      description: "IP {{ $labels.ip }}에서 1분간 20회 이상 로그인 실패"

  - alert: MultipleAccountLockouts
    expr: increase(auth_account_locked_total[10m]) > 5
    for: 1m
    labels:
      severity: warning
    annotations:
      summary: "다수 계정 잠금 발생"
      description: "10분간 5개 이상 계정 잠금"

  - alert: UnauthorizedAccessAttempt
    expr: rate(authorization_denied_total[5m]) > 5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "무단 접근 시도 증가"
      description: "5분간 초당 5회 이상 권한 위반"
```

### 7.3 보안 감사 절차

#### 7.3.1 일일 감사
```
Daily Security Audit (자동화):
  1. 전날 로그인 실패 TOP 10 IP 분석
  2. 계정 잠금 이벤트 리뷰
  3. 권한 위반 시도 리뷰
  4. 비정상적인 API 호출 패턴 탐지
  5. 보고서 자동 생성 (Kibana Reporting)
```

#### 7.3.2 주간 감사
```
Weekly Security Audit (수동):
  1. 모든 ADMIN 역할 사용자 활동 리뷰
  2. 새로 추가된 권한 리뷰
  3. Rate Limiting 정책 효과성 분석
  4. 보안 메트릭 트렌드 분석
  5. 취약점 스캔 결과 리뷰
```

#### 7.3.3 월간 감사
```
Monthly Security Audit:
  1. 전체 권한 체계 리뷰
  2. JWT Secret Rotation 실행
  3. 보안 정책 준수 여부 점검
  4. 침투 테스트 실시
  5. 보안 교육 실시
  6. 보안 개선 사항 식별 및 백로그 추가
```

---

## 8. 완료 기준 (Definition of Done)

### 8.1 Phase 1 완료 기준
- [ ] Rate Limiting이 모든 주요 엔드포인트에 적용됨
- [ ] 부하 테스트에서 429 응답 정상 동작 확인
- [ ] 보안 감사 로깅이 ELK Stack에 저장됨
- [ ] Kibana 대시보드로 로그인 실패, 권한 위반 조회 가능
- [ ] 5회 로그인 실패 시 계정 30분 잠금 확인
- [ ] JWT Secret이 K8s Secrets에 저장됨
- [ ] Refresh Token이 HttpOnly Cookie로 전송됨
- [ ] 모든 테스트 통과 (단위/통합/보안)

### 8.2 Phase 2 완료 기준
- [ ] SecurityHeaders.com 스캔 결과 A+ 등급
- [ ] CSP 헤더로 XSS 공격 차단 확인
- [ ] JWT Secret Rotation 메커니즘 동작 확인
- [ ] 모든 엔드포인트 권한 검증 감사 완료
- [ ] TLS 1.3 강제, HTTP → HTTPS 리다이렉션 확인

### 8.3 Phase 3 완료 기준
- [ ] 비밀번호 복잡도 정책 적용 (10자, 대소문자+숫자+특수문자)
- [ ] 흔한 비밀번호 필터링 동작 확인
- [ ] Absolute Session Timeout 24시간 적용
- [ ] 민감 정보 암호화 완료 (실명, 전화번호)
- [ ] CORS 설정 환경별 분리 (dev: 허용적, prod: 엄격)

---

## 9. 위험 관리

### 9.1 구현 위험

| 위험 | 영향 | 가능성 | 완화 방안 |
|------|------|--------|-----------|
| **Rate Limiting으로 정상 사용자 차단** | High | Medium | - 제한값 넉넉하게 설정 (점진적 강화)<br>- 화이트리스트 메커니즘 추가<br>- 실시간 모니터링 및 조정 |
| **Refresh Token Cookie 변경으로 호환성 문제** | High | Low | - 기존 localStorage 토큰 마이그레이션 로직<br>- Feature Flag로 점진적 적용 |
| **보안 헤더로 기능 동작 안함** | Medium | Medium | - CSP 정책 단계적 강화<br>- Staging 환경에서 충분히 테스트 |
| **JWT Secret Rotation 중 서비스 다운타임** | High | Low | - Blue-Green Deployment<br>- Grace Period 설정 (이전 Secret도 유효) |
| **로그 저장 공간 부족** | Medium | Medium | - 로그 보관 정책 엄격히 준수<br>- 로그 압축 및 아카이빙 |

### 9.2 운영 위험

| 위험 | 영향 | 가능성 | 완화 방안 |
|------|------|--------|-----------|
| **계정 잠금으로 인한 고객 불만** | Medium | High | - 이메일 알림으로 잠금 사유 설명<br>- 자동 해제 시간 명시<br>- 고객 지원팀 해제 권한 부여 |
| **보안 정책으로 UX 저하** | Medium | Medium | - 사용자 피드백 수집 및 개선<br>- 보안과 UX 균형 조정 |
| **과도한 알림으로 알림 피로** | Low | High | - 알림 임계값 조정<br>- 중요한 이벤트만 알림 |

---

## 10. 참고 자료

### 10.1 내부 문서
- `/docs/architecture/auth-system-design.md` - 인증 시스템 설계
- `/docs/adr/ADR-003-authorization-strategy.md` - 권한 검증 전략
- `/docs/runbooks/secret-rotation.md` - Secret Rotation 절차

### 10.2 외부 자료
- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Best Current Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [NIST Digital Identity Guidelines](https://pages.nist.gov/800-63-3/)

---

## 11. 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 2026-01-23 | 1.0 | 초안 작성 | Security Agent |

---

**문서 승인**:
- Security Team Lead: ___________________
- DevOps Lead: ___________________
- Backend Lead: ___________________
- Product Owner: ___________________

**다음 리뷰 예정일**: 2026-02-23
