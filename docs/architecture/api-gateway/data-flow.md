---
id: arch-data-flow
title: API Gateway Data Flow
type: architecture
status: current
created: 2026-02-06
updated: 2026-02-18
author: Laze
tags: [api-gateway, data-flow, jwt, rate-limiting, circuit-breaker, health-check]
related:
  - arch-system-overview
  - api-gateway-routing
  - api-gateway-security
---

# API Gateway Data Flow

## 개요

API Gateway의 주요 데이터 흐름을 설명합니다. 모든 요청은 필터 체인을 거쳐 처리되며, JWT 검증, Rate Limiting, Circuit Breaker 등이 순서대로 적용됩니다.

### 핵심 컴포넌트

- **JwtAuthenticationFilter**: HMAC-SHA256 JWT 검증 + Header Sanitization
- **TokenBlacklistChecker**: Redis 기반 로그아웃 토큰 확인
- **RateLimiterConfig**: Redis Token Bucket Rate Limiting
- **SecurityConfig**: RBAC 기반 접근 제어
- **ServiceHealthAggregator**: 병렬 Health Check + K8s enrichment

---

## 1. 인증된 API 요청 흐름

인증이 필요한 API 요청(예: `POST /api/v1/chat/stream`)의 전체 필터 체인 통과 과정입니다.

```mermaid
sequenceDiagram
    participant C as Client
    participant CORS as CorsWebFilter
    participant FWD as GlobalForwardedHeaders
    participant LOG as GlobalLoggingFilter
    participant JWT as JwtAuthenticationFilter
    participant BL as TokenBlacklistChecker
    participant SC as SecurityConfig (RBAC)
    participant RL as RequestRateLimiter
    participant CB as CircuitBreaker
    participant SVC as Backend Service
    participant SEC as SecurityHeadersFilter
    participant REDIS as Redis

    C->>CORS: POST /api/v1/chat/stream<br/>Authorization: Bearer eyJ...
    CORS->>CORS: CORS 헤더 검증
    CORS->>FWD: 통과

    FWD->>FWD: X-Forwarded-* 헤더 주입
    FWD->>LOG: 통과

    LOG->>LOG: API_REQUEST 로깅<br/>(IP, Path, Headers)
    LOG->>JWT: 통과

    Note over JWT: Header Sanitization
    JWT->>JWT: X-User-* 헤더 제거

    Note over JWT: JWT 검증
    JWT->>JWT: kid 추출 → 키 조회
    JWT->>JWT: HMAC-SHA256 서명 검증

    JWT->>REDIS: blacklist:{token} 존재?
    REDIS-->>JWT: false (유효)

    JWT->>JWT: Claims 파싱<br/>(sub, roles, effectiveRoles, memberships)
    JWT->>JWT: effectiveRoles claim 파싱 (없으면 roles fallback)
    JWT->>JWT: X-User-Id, X-User-Roles, X-User-Effective-Roles 헤더 설정
    JWT->>JWT: SecurityContext에 Authentication 저장

    JWT->>SC: 통과 (authenticated)
    SC->>SC: 경로별 권한 확인<br/>(anyExchange().authenticated())
    SC->>RL: 권한 확인 통과

    RL->>REDIS: Token Bucket 확인
    REDIS-->>RL: 허용 (remaining: 98)
    RL->>CB: 통과

    CB->>SVC: 요청 전달<br/>(+ X-User-Id 헤더)
    SVC-->>CB: 200 OK + Response Body

    CB-->>SEC: 응답 반환
    SEC->>SEC: 보안 헤더 추가<br/>(CSP, X-Frame-Options 등)

    SEC-->>LOG: 응답 반환
    LOG->>LOG: API_RESPONSE 로깅<br/>(Status: 200, Duration: 45ms)

    LOG-->>C: 200 OK + 보안 헤더
```

---

## 2. JWT 토큰 검증 상세 흐름

`JwtAuthenticationFilter`의 내부 검증 로직입니다.

```mermaid
sequenceDiagram
    participant F as Filter
    participant JP as JwtProperties
    participant BL as TokenBlacklistChecker
    participant REDIS as Redis

    Note over F: 1. Header Sanitization
    F->>F: X-User-Id 제거
    F->>F: X-User-Roles 제거
    F->>F: X-User-Effective-Roles 제거
    F->>F: X-User-Memberships 제거
    F->>F: X-User-Nickname 제거
    F->>F: X-User-Name 제거

    Note over F: 2. 공개 경로 확인
    F->>F: skipJwtParsing prefix 매칭
    alt 공개 경로
        F-->>F: 검증 생략, chain.filter() 호출
    end

    Note over F: 3. Bearer Token 추출
    F->>F: Authorization 헤더 확인
    alt 헤더 없음 or Bearer 아님
        F-->>F: 토큰 없이 chain.filter()<br/>(SecurityConfig에서 접근 제어)
    end

    Note over F: 4. kid 추출
    F->>F: JWT 헤더 Base64 디코딩
    F->>F: "kid" 필드 읽기
    alt kid 없음
        F->>JP: currentKeyId 사용
    end

    Note over F: 5. 서명 검증
    F->>JP: keys.get(kid)
    JP-->>F: KeyConfig (secretKey, activatedAt, expiresAt)

    alt 키 만료됨
        F-->>F: 401 GW-A007 "Invalid token"
    end

    F->>F: HMAC-SHA256 서명 검증<br/>(jjwt parseSignedClaims)

    alt 서명 실패
        F-->>F: 401 GW-A007 "Invalid token"
    end
    alt 토큰 만료
        F-->>F: 401 GW-A006 "Token expired"
    end

    Note over F,REDIS: 6. Blacklist 확인
    F->>BL: isBlacklisted(token)
    BL->>REDIS: hasKey("blacklist:{token}")
    alt Redis 장애
        REDIS-->>BL: error
        BL-->>F: false (가용성 우선)
    else 정상
        REDIS-->>BL: true/false
    end

    alt Blacklisted
        BL-->>F: true
        F-->>F: 401 GW-A005 "Token revoked"
    end

    Note over F: 7. effectiveRoles 파싱
    F->>F: JWT effectiveRoles claim 파싱
    alt effectiveRoles claim 존재
        F->>F: claim 값 사용 (DAG 확장 역할)
    else claim 없음 (구형 JWT)
        F->>F: roles claim을 fallback으로 사용
    end

    Note over F: 8. Claims → 헤더 변환
    F->>F: sub → X-User-Id
    F->>F: roles[] → X-User-Roles (쉼표 구분, 직접 할당)
    F->>F: effectiveRoles[] → X-User-Effective-Roles (DAG 확장)
    F->>F: memberships{} → X-User-Memberships (JSON)
    F->>F: nickname → X-User-Nickname (URL encoded)
    F->>F: username → X-User-Name (URL encoded)

    Note over F: 9. SecurityContext 설정
    F->>F: UsernamePasswordAuthenticationToken 생성<br/>(authorities = effective roles)
    F->>F: ReactiveSecurityContextHolder에 저장
```

---

## 3. 공개 경로 요청 흐름

공개 경로의 3가지 카테고리별 동작 차이입니다.

```mermaid
sequenceDiagram
    participant C as Client
    participant JWT as JwtAuthenticationFilter
    participant SC as SecurityConfig
    participant SVC as Backend Service

    Note over C,SVC: Case 1: skipJwtParsing 경로<br/>/api/v1/auth/login (POST)
    C->>JWT: POST /api/v1/auth/login<br/>(토큰 없음)
    JWT->>JWT: skipJwtParsing 매칭 ✅
    JWT->>JWT: Header Sanitization만 수행
    JWT->>SC: JWT 검증 없이 통과
    SC->>SC: permitAll 매칭 ✅
    SC->>SVC: 인증 없이 전달

    Note over C,SVC: Case 2: permitAllGet 경로 (GET)<br/>/api/v1/blog/posts
    C->>JWT: GET /api/v1/blog/posts<br/>Authorization: Bearer eyJ...
    JWT->>JWT: skipJwtParsing 매칭 ❌
    JWT->>JWT: Bearer 토큰 발견 → JWT 검증 수행
    JWT->>JWT: 검증 성공 → X-User-Id 설정
    JWT->>SC: authenticated 상태
    SC->>SC: permitAllGet(GET) 매칭 ✅
    SC->>SVC: X-User-Id 포함 전달<br/>(로그인 사용자 = 본인 글 표시)

    Note over C,SVC: Case 3: permitAllGet 경로 (GET, 비로그인)<br/>/api/v1/blog/posts
    C->>JWT: GET /api/v1/blog/posts<br/>(토큰 없음)
    JWT->>JWT: skipJwtParsing 매칭 ❌
    JWT->>JWT: Authorization 헤더 없음 → 검증 생략
    JWT->>SC: anonymous 상태
    SC->>SC: permitAllGet(GET) 매칭 ✅
    SC->>SVC: X-User-Id 없이 전달<br/>(비로그인 = 공개 데이터만)

    Note over C,SVC: Case 4: permitAllGet 경로 (POST)<br/>/api/v1/blog/posts
    C->>JWT: POST /api/v1/blog/posts<br/>Authorization: Bearer eyJ...
    JWT->>JWT: JWT 검증 수행
    JWT->>SC: authenticated 상태
    SC->>SC: permitAllGet은 GET만<br/>POST는 authenticated() 적용
    SC->>SVC: 인증 확인 후 전달
```

---

## 4. Rate Limiting 흐름

Redis Token Bucket 알고리즘 기반의 속도 제한 처리 흐름입니다.

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as Gateway Route
    participant KR as KeyResolver
    participant RL as RedisRateLimiter
    participant REDIS as Redis
    participant SVC as Backend Service
    participant RLH as RateLimitHeaderFilter

    C->>GW: POST /api/v1/auth/login

    Note over GW,KR: 1. Key 결정
    GW->>KR: compositeKeyResolver.resolve()
    KR->>KR: IP 추출 (X-Forwarded-For)
    KR->>KR: Path 추출 (/api/v1/auth/login)
    KR-->>GW: "192.168.1.1:/api/v1/auth/login"

    Note over GW,REDIS: 2. Token Bucket 확인
    GW->>RL: strictRedisRateLimiter 적용<br/>(1 req/s, burst 5)
    RL->>REDIS: Lua Script 실행<br/>(EVALSHA token_bucket)
    REDIS-->>RL: {allowed: true, remaining: 4}

    alt 허용
        RL-->>GW: allowed=true
        GW->>SVC: 요청 전달
        SVC-->>GW: 200 OK
        GW-->>C: 200 OK<br/>X-RateLimit-Remaining: 4<br/>X-RateLimit-Replenish-Rate: 1<br/>X-RateLimit-Burst-Capacity: 5
    else 거부 (remaining = 0)
        RL-->>GW: allowed=false
        GW-->>C: 429 Too Many Requests<br/>Retry-After: 60<br/>X-RateLimit-Remaining: 0
    end

    Note over RLH: 3. 응답 후 로깅
    RLH->>RLH: Rate Limit 헤더 로깅<br/>(Remaining, Rate, Capacity)
```

### Rate Limiter 적용 매핑

| 엔드포인트 | Rate Limiter | Key Resolver |
|-----------|-------------|-------------|
| `POST /api/v1/auth/login` | strict (1/s, burst 5) | composite (IP:path) |
| `POST /api/v1/users/signup` | signup (1/s, burst 3) | composite (IP:path) |
| `/auth-service/api/v1/auth/**` | unauthenticated (1/s, burst 30) | ip |
| `/api/v1/shopping/**` | unauthenticated (1/s, burst 30) | ip |
| `/api/v1/chat/**` (authenticated) | authenticated (2/s, burst 100) | user |
| `/api/v1/prism/**` (authenticated) | authenticated (2/s, burst 100) | user |

---

## 5. Circuit Breaker 상태 전이

Resilience4j Circuit Breaker의 상태 전이와 Fallback 처리 흐름입니다.

```mermaid
sequenceDiagram
    participant C as Client
    participant CB as CircuitBreaker
    participant SVC as Backend Service
    participant FB as FallbackController

    Note over CB: 상태: CLOSED (정상)
    rect rgb(200, 250, 200)
        C->>CB: 요청 1-10 (정상)
        CB->>SVC: 전달
        SVC-->>CB: 200 OK
        CB-->>C: 200 OK
    end

    Note over CB: 실패 발생 시작
    rect rgb(250, 220, 200)
        C->>CB: 요청 11-20 (일부 실패)
        CB->>SVC: 전달
        SVC-->>CB: 503 / Timeout

        Note over CB: Sliding Window (20건)<br/>실패율 >= 50%
        CB->>CB: 상태 → OPEN
    end

    Note over CB: 상태: OPEN (차단)
    rect rgb(250, 200, 200)
        C->>CB: 요청 21
        CB->>FB: fallbackUri 호출<br/>(forward:/fallback/{service})
        FB-->>CB: GW00x 에러 응답
        CB-->>C: 503 Service Unavailable<br/>{"error":{"code":"GW001",...}}
    end

    Note over CB: 10초 대기 후 자동 전환
    Note over CB: 상태: HALF_OPEN (시험)
    rect rgb(250, 250, 200)
        C->>CB: 요청 22-26 (5건 허용)
        CB->>SVC: 전달 (시험 요청)
        alt 성공
            SVC-->>CB: 200 OK
            CB->>CB: 상태 → CLOSED
            CB-->>C: 200 OK
        else 실패
            SVC-->>CB: 503 / Timeout
            CB->>CB: 상태 → OPEN (다시 차단)
            CB->>FB: fallback
            FB-->>C: GW00x 에러
        end
    end
```

### 서비스별 타임아웃

```mermaid
gantt
    title Circuit Breaker Timeout 비교
    dateFormat X
    axisFormat %s초

    section 기본 (5s)
    auth-service     :0, 5
    blog-service     :0, 5
    shopping-service :0, 5

    section AI 서비스
    prism-service    :0, 60
    chatbot-service  :0, 120
```

---

## 6. Health Aggregation 흐름

`ServiceHealthAggregator`의 병렬 Health Check 및 Kubernetes enrichment 흐름입니다.

```mermaid
sequenceDiagram
    participant C as Client / Status Page
    participant HC as ServiceHealthController
    participant SHA as ServiceHealthAggregator
    participant HE as HealthEndpoint (Self)
    participant WC as WebClient
    participant K8S as Kubernetes API
    participant AS as auth-service
    participant BS as blog-service
    participant SS as shopping-service
    participant NS as notification-service
    participant PS as prism-service
    participant CS as chatbot-service

    C->>HC: GET /api/health/services

    HC->>SHA: aggregateHealth()

    Note over SHA: 병렬 Health Check (Flux.flatMap)

    par Gateway Self-Check
        SHA->>HE: healthEndpoint.health()
        HE-->>SHA: UP (5ms)
    and Auth Service
        SHA->>WC: GET http://auth:8081/actuator/health
        WC->>AS: 요청
        AS-->>WC: {"status": "UP"}
        WC-->>SHA: up (45ms)
    and Blog Service
        SHA->>WC: GET http://blog:8082/actuator/health
        WC->>BS: 요청
        BS-->>WC: {"status": "UP"}
        WC-->>SHA: up (32ms)
    and Shopping Service
        SHA->>WC: GET http://shopping:8083/actuator/health
        WC->>SS: 요청
        SS-->>WC: {"status": "UP"}
        WC-->>SHA: up (28ms)
    and Notification Service
        SHA->>WC: GET http://notification:8084/actuator/health
        WC->>NS: 요청 (타임아웃 3초)
        NS-->>WC: timeout
        WC-->>SHA: down (3000ms)
    and Prism Service
        SHA->>WC: GET http://prism:8085/api/v1/health
        WC->>PS: 요청
        PS-->>WC: {"success": true, "data": {"status": "ok"}}
        WC-->>SHA: up (15ms)
    and Chatbot Service
        SHA->>WC: GET http://chatbot:8086/api/v1/chat/health
        WC->>CS: 요청
        CS-->>WC: {"status": "UP"}
        WC-->>SHA: up (20ms)
    end

    Note over SHA,K8S: K8s Enrichment (kubernetes 프로필만)
    opt Kubernetes 프로필 활성화
        par 각 서비스 K8s 정보 조회
            SHA->>K8S: GET deployments/{name}
            K8S-->>SHA: replicas: 2, readyReplicas: 2
            SHA->>K8S: GET pods?label=app={name}
            K8S-->>SHA: Pod 목록 (name, phase, ready, restarts)
        end
    end

    Note over SHA: Overall Status 결정
    SHA->>SHA: 1개 이상 down → "degraded"
    SHA-->>HC: ServiceHealthResponse

    HC-->>C: 200 OK<br/>{"overallStatus": "degraded", ...}
```

### Overall Status 결정 로직

| 조건 | Overall Status |
|------|---------------|
| 모든 서비스 UP | `up` |
| 모든 서비스 DOWN | `down` |
| 혼합 (일부 UP, 일부 DOWN) | `degraded` |
| 서비스 목록 비어있음 | `unknown` |

### Health 응답 형식 호환성

| 형식 | 예시 | 대상 서비스 |
|------|------|-----------|
| Spring Boot Actuator | `{"status": "UP"}` | auth, blog, shopping, notification |
| Custom (NestJS/Python) | `{"success": true, "data": {"status": "ok"}}` | prism, chatbot |

---

## 참고 자료

- [System Overview](./system-overview.md) - 컴포넌트 상세 설명
- [Routing Specification](../../api/api-gateway/routing-specification.md) - 라우팅 규칙 상세
- [Security & Authentication](../../api/api-gateway/security-authentication.md) - JWT 검증 상세
- [Rate Limiting](../../api/api-gateway/rate-limiting.md) - Rate Limiting 설정 상세
- [Resilience](../../api/api-gateway/resilience.md) - Circuit Breaker 설정 상세

---

## 변경 이력

| 날짜 | 작성자 | 변경 내용 |
|------|--------|-----------|
| 2026-02-06 | Laze | 코드베이스 기준 신규 작성 (24개 Java 파일, application.yml 검증) |
| 2026-02-18 | Laze | RoleHierarchyResolver 제거 → JWT effectiveRoles claim 직접 파싱 (ADR-044) |
