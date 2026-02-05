---
id: api-gateway-security-authentication
title: API Gateway 보안 및 인증
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api-gateway, security, jwt, rbac, authentication, authorization]
related:
  - api-gateway-routing-specification
  - api-gateway-error-reference
---

# API Gateway 보안 및 인증

## 개요

API Gateway는 모든 인바운드 요청에 대해 다층 보안을 적용합니다.

1. **Header Injection 방어** - 외부에서 주입된 X-User-* 헤더 Strip
2. **JWT 토큰 검증** - HMAC Multi-key 서명 검증
3. **Token Blacklist** - Redis 기반 로그아웃 토큰 차단
4. **RBAC 접근 제어** - 경로별 권한 검증
5. **User Context 전파** - 하위 서비스로 사용자 정보 헤더 전달
6. **보안 응답 헤더** - CSP, HSTS, X-Frame-Options 등

## JWT 인증

### 검증 방식

API Gateway는 **HMAC Secret Key** 방식으로 JWT 토큰을 검증합니다. Auth Service와 동일한 secret key를 공유하며, 다중 키(Key Rotation)를 지원합니다.

> **소스 파일**: `JwtAuthenticationFilter.java`, `JwtProperties.java`

```
요청 흐름:
1. Authorization: Bearer <token> 헤더 추출
2. JWT 헤더에서 kid (Key ID) 추출
3. kid에 해당하는 secret key로 서명 검증
4. 만료, 서명 오류 시 401 응답
5. Token Blacklist 체크 (Redis)
6. Claims에서 사용자 정보 추출 → X-User-* 헤더 설정
```

### Multi-key JWT 지원

```yaml
# application.yml
jwt:
  current-key-id: ${JWT_CURRENT_KEY_ID:key-default}
  keys:
    key-default:
      secret-key: ${JWT_SECRET_KEY:...minimum-32-characters...}
      activated-at: 2026-01-01T00:00:00
      # expires-at: null (만료 없음)
```

| 필드 | 설명 |
|------|------|
| `current-key-id` | 현재 활성 키 ID (kid가 없는 토큰에 기본 사용) |
| `keys.<id>.secret-key` | HMAC-SHA256 서명 비밀키 (최소 256비트/32바이트) |
| `keys.<id>.activated-at` | 키 활성화 시점 |
| `keys.<id>.expires-at` | 키 만료 시점 (null이면 만료 없음) |

**Key Rotation 절차**:
1. 새 키를 `keys` 맵에 추가 (예: `key-2026-02`)
2. Auth Service에서 새 키로 토큰 발급 시작
3. `current-key-id`를 새 키로 변경
4. 이전 키의 `expires-at` 설정 (기존 토큰 만료 대기)

### JWT 토큰 요구사항

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImtleS1kZWZhdWx0In0...
```

| 항목 | 값 |
|------|-----|
| **헤더** | `Authorization` |
| **형식** | `Bearer <access_token>` |
| **발급처** | auth-service |
| **서명 알고리즘** | HMAC-SHA256 (HS256) |
| **키 식별** | JWT 헤더의 `kid` 클레임 |

### JWT Claims 구조

```json
{
  "sub": "user-uuid",
  "kid": "key-default",
  "roles": ["ROLE_USER", "ROLE_SELLER"],
  "memberships": {"shopping": "PREMIUM"},
  "nickname": "홍길동",
  "username": "hong_gildong",
  "exp": 1738857600,
  "iat": 1738856700
}
```

## Token Blacklist

로그아웃된 토큰은 Redis에 블랙리스트로 등록되어 재사용을 차단합니다.

> **소스 파일**: `TokenBlacklistChecker.java`

| 항목 | 값 |
|------|-----|
| **Redis 키 패턴** | `blacklist:{accessToken}` |
| **저장소** | ReactiveRedisTemplate |
| **장애 정책** | Redis 장애 시 토큰 허용 (가용성 우선) |

```
검증 흐름:
1. JWT 서명 검증 통과
2. Redis에서 blacklist:{token} 키 존재 여부 확인
3. 존재하면 → 401 (Token revoked, GW-A005)
4. 미존재하면 → 인증 진행
5. Redis 연결 실패 → 토큰 허용 (가용성 우선)
```

## Header Injection 방어

외부에서 주입된 X-User-* 헤더를 사전에 제거하여 권한 위조를 방지합니다.

> **소스 파일**: `JwtAuthenticationFilter.java:127-134`

**제거 대상 헤더** (모든 요청에서 Strip):
- `X-User-Id`
- `X-User-Roles`
- `X-User-Memberships`
- `X-User-Nickname`
- `X-User-Name`

JWT 검증 성공 후 Claims에서 추출한 값으로 다시 설정됩니다.

## User Context Headers

JWT 검증 성공 시 하위 서비스로 전달되는 헤더입니다.

| 헤더 | 소스 (JWT Claim) | 설명 | 인코딩 |
|------|------------------|------|--------|
| `X-User-Id` | `sub` | 사용자 UUID | Plain |
| `X-User-Roles` | `roles` | 쉼표 구분 권한 목록 | Plain |
| `X-User-Memberships` | `memberships` | 서비스별 멤버십 | JSON |
| `X-User-Nickname` | `nickname` | 닉네임 | URL Encoded |
| `X-User-Name` | `username` | 사용자명 | URL Encoded |

**예시**:
```http
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Roles: ROLE_USER,ROLE_SELLER
X-User-Memberships: {"shopping":"PREMIUM"}
X-User-Nickname: %ED%99%8D%EA%B8%B8%EB%8F%99
X-User-Name: hong_gildong
```

## RBAC 접근 제어

> **소스 파일**: `SecurityConfig.java:116-148`

### 권한 체계

```mermaid
graph TD
    A[SUPER_ADMIN] --> B[SHOPPING_ADMIN]
    A --> C[BLOG_ADMIN]
    B --> D[SELLER]
    A --> E[All Protected Paths]
    B --> F["/api/v1/shopping/admin/**"]
    B --> G["/api/v1/admin/seller/**"]
    C --> H["/api/v1/blog/admin/**"]
    D --> I["/api/v1/shopping/seller/**"]
```

### 경로별 접근 권한

| 경로 패턴 | 필요 권한 |
|-----------|-----------|
| `/api/v1/admin/**` (seller 제외) | `ROLE_SUPER_ADMIN` |
| `/api/v1/admin/seller/**` | `ROLE_SHOPPING_ADMIN` 또는 `ROLE_SUPER_ADMIN` |
| `/api/v1/shopping/admin/**` | `ROLE_SHOPPING_ADMIN` 또는 `ROLE_SUPER_ADMIN` |
| `/api/v1/blog/admin/**` | `ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` |
| `/api/v1/shopping/seller/**` | `ROLE_SELLER` 또는 `ROLE_SHOPPING_ADMIN` 또는 `ROLE_SUPER_ADMIN` |
| 기타 보호 경로 | 인증된 사용자 (authenticated) |

### 권한 부족 시 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A002",
    "message": "Access denied"
  }
}
```

## 공개 경로 (Public Paths)

> **소스 파일**: `PublicPathProperties.java`, `application.yml` - `gateway.public-paths`

공개 경로는 3가지 카테고리로 관리됩니다.

### 1. permit-all (모든 메서드 허용)

인증 없이 모든 HTTP 메서드로 접근 가능한 경로입니다.

| 경로 | 용도 |
|------|------|
| `/notification/ws/**` | WebSocket 연결 |
| `/auth-service/**` | OAuth2 엔드포인트 |
| `/api/v1/auth/**` | 인증 API (로그인, 리프레시) |
| `/api/v1/users/**` | 사용자 API (회원가입) |
| `/api/v1/shopping/products`, `/api/v1/shopping/products/**` | 상품 조회 |
| `/api/v1/shopping/categories`, `/api/v1/shopping/categories/**` | 카테고리 조회 |
| `/api/v1/shopping/coupons` | 쿠폰 목록 |
| `/api/v1/shopping/time-deals`, `/api/v1/shopping/time-deals/**` | 타임딜 |
| `/api/v1/shopping/inventory/batch`, `/api/v1/shopping/inventory/stream` | 재고 조회 |
| `/api/v1/prism/health`, `/api/v1/prism/ready` | Prism 헬스체크 |
| `/api/v1/chat/health` | Chatbot 헬스체크 |
| `/api/health/**` | 통합 Health API |
| `/actuator/**` | Actuator 엔드포인트 |
| `/api/v1/*/actuator/**` | 서비스별 Actuator 프록시 |

### 2. permit-all-get (GET만 허용)

GET 요청만 인증 없이 허용되는 경로입니다. POST/PUT/DELETE는 인증 필요.

| 경로 | 용도 |
|------|------|
| `/api/v1/blog/**` | 블로그 글 조회 (GET) |
| `/api/v1/memberships/tiers/**` | 멤버십 등급 조회 |
| `/api/v1/shopping/search/products` | 상품 검색 |
| `/api/v1/shopping/search/suggest` | 검색 자동완성 |
| `/api/v1/shopping/search/popular` | 인기 검색어 |

### 3. skip-jwt-parsing (JWT 파싱 생략)

JWT 토큰이 있어도 파싱을 건너뛰는 경로입니다. 불필요한 Redis 조회를 방지합니다.

> 참고: `permit-all-get` 경로(예: `/api/v1/blog/**`)는 JWT가 있으면 파싱하여 사용자 정보를 추출합니다 (로그인 사용자의 읽기 추적 등).

| Prefix | 용도 |
|--------|------|
| `/auth-service/` | OAuth2 경로 |
| `/api/v1/auth/` | 인증 API |
| `/api/v1/users/` | 사용자 API |
| `/api/health/` | Health API |
| `/actuator/` | Actuator |
| `/api/v1/shopping/products` | 상품 조회 |
| `/api/v1/shopping/categories` | 카테고리 조회 |
| `/api/v1/prism/health`, `/api/v1/prism/ready` | Prism 헬스 |
| `/api/v1/chat/health` | Chatbot 헬스 |

## 보안 응답 헤더

> **소스 파일**: `SecurityHeadersFilter.java`, `SecurityHeadersProperties.java`

Gateway는 모든 응답에 보안 헤더를 추가합니다.

| 헤더 | 기본값 | 설명 |
|------|--------|------|
| `X-Content-Type-Options` | `nosniff` | MIME 타입 스니핑 방지 |
| `X-Frame-Options` | `DENY` | Clickjacking 방지 |
| `X-XSS-Protection` | `1; mode=block` | XSS 필터 활성화 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referrer 정보 제한 |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=()` | 브라우저 기능 제한 |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' 'unsafe-inline'; ...` | CSP 정책 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 강제 (HTTPS only) |

### Content Security Policy

```
default-src 'self';
script-src 'self' 'unsafe-inline';
style-src 'self' 'unsafe-inline';
img-src 'self' data: https:;
font-src 'self' https://fonts.gstatic.com;
connect-src 'self'
```

| 설정 | 값 | 설명 |
|------|-----|------|
| `enabled` | `true` | CSP 적용 여부 |
| `report-only` | `false` | 위반 시 차단 (보고만 하지 않음) |

### HSTS 설정

| 설정 | 값 | 설명 |
|------|-----|------|
| `enabled` | `true` | HSTS 적용 여부 |
| `https-only` | `true` | HTTPS 요청에만 적용 |
| `max-age` | `31536000` (1년) | 브라우저 HTTPS 강제 기간 |
| `include-sub-domains` | `true` | 서브도메인 포함 |
| `preload` | `false` | Preload 리스트 미등록 |

**환경별 차이**: Local 환경(`application-local.yml`)에서는 HSTS가 비활성화됩니다.

### Cache-Control (인증 경로)

인증 관련 경로에는 브라우저 캐싱을 방지하는 헤더가 추가됩니다.

```http
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Expires: 0
```

**적용 경로 패턴**:
- `/api/v1/auth/**`
- `/auth-service/**`
- `/api/v1/users/**`
- `/api/v1/*/profile/**`

## 필터 실행 순서

```
1. RequestPathLoggingFilter  (HIGHEST_PRECEDENCE)     - 요청 로깅
2. CorsWebFilter             (HIGHEST_PRECEDENCE + 1) - CORS Preflight 처리
3. SecurityHeadersFilter      (HIGHEST_PRECEDENCE)     - 보안 응답 헤더 (beforeCommit)
4. JwtAuthenticationFilter    (AUTHENTICATION)         - JWT 검증 + User Context
5. SecurityFilterChain        (Spring Security)        - RBAC 접근 제어
6. Route-level Filters        (per route)              - Rate Limiter, Circuit Breaker
7. RateLimitHeaderFilter      (LOWEST_PRECEDENCE)      - Rate Limit 헤더 로깅
```

## 소스 파일 참조

| 파일 | 역할 |
|------|------|
| `config/SecurityConfig.java` | CORS, RBAC, 필터 체인 구성 |
| `filter/JwtAuthenticationFilter.java` | JWT 검증, Blacklist, Header Injection 방어 |
| `config/JwtProperties.java` | Multi-key JWT 설정 |
| `config/PublicPathProperties.java` | 공개 경로 3카테고리 관리 |
| `config/SecurityHeadersFilter.java` | 보안 응답 헤더 7종 |
| `config/SecurityHeadersProperties.java` | 보안 헤더 설정 Properties |
| `service/TokenBlacklistChecker.java` | Redis Token Blacklist 체크 |
| `security/CustomAuthenticationEntryPoint.java` | 401 응답 (A001) |
| `security/CustomAccessDeniedHandler.java` | 403 응답 (A002) |

## 관련 문서

- [라우팅 명세](./routing-specification.md) - 전체 라우트 테이블
- [에러 코드 레퍼런스](./error-reference.md) - A001, A002, GW-A005~A007
