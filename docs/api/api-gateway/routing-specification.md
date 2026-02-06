---
id: api-gateway-routing-specification
title: API Gateway 라우팅 명세
type: api
status: current
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [api-gateway, routing, spring-cloud-gateway, endpoints]
related:
  - api-gateway-security-authentication
  - api-gateway-rate-limiting
  - api-gateway-resilience
---

# API Gateway 라우팅 명세

## 개요

API Gateway는 Portal Universe 플랫폼의 단일 진입점으로, Spring Cloud Gateway(WebFlux 기반)를 사용하여 클라이언트 요청을 적절한 백엔드 마이크로서비스로 라우팅합니다.

- **요청 라우팅**: 경로 패턴 및 HTTP 메서드 기반 요청 전달
- **JWT 인증**: HMAC Secret Key 기반 JWT 토큰 검증 (Multi-key 지원)
- **Rate Limiting**: Redis 기반 Token Bucket 알고리즘 (5개 Rate Limiter)
- **Circuit Breaker**: Resilience4j 기반 서비스별 장애 격리
- **CORS 처리**: 프론트엔드 크로스 오리진 요청 허용
- **보안 헤더**: CSP, HSTS, X-Frame-Options 등 자동 부여

> **소스 파일**: `services/api-gateway/src/main/resources/application.yml`

## 전역 필터 (Default Filters)

모든 라우트에 자동 적용되는 필터입니다.

| 필터 | 설명 |
|------|------|
| `PreserveHostHeader` | 원본 Host 헤더 보존 |
| `DedupeResponseHeader` | CORS 관련 중복 응답 헤더 제거 (RETAIN_UNIQUE) |
| `X-Forwarded` | X-Forwarded-For/Proto/Host 헤더 자동 추가 |

## 전체 라우팅 테이블

### Actuator Health Routes

| Route ID | Path | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|---------|--------------|-----------------|------|
| `auth-service-actuator-health` | `/api/v1/auth/actuator/health`, `/api/v1/auth/actuator/info` | auth-service | 0 | RewritePath | - | - | Public |
| `blog-service-actuator-health` | `/api/v1/blog/actuator/health`, `/api/v1/blog/actuator/info` | blog-service | 0 | RewritePath | - | - | Public |
| `shopping-service-actuator-health` | `/api/v1/shopping/actuator/health`, `/api/v1/shopping/actuator/info` | shopping-service | 0 | RewritePath | - | - | Public |

- RewritePath: `/api/v1/{service}/actuator/{segment}` -> `/actuator/{segment}`

### Auth Service Routes

| Route ID | Path | Method | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|--------|-------|---------|--------------|-----------------|------|
| `auth-service-login` | `/api/v1/auth/login` | POST | auth-service | 0 | - | strict + composite | authCB | Public |
| `auth-service-signup` | `/api/v1/users/signup` | POST | auth-service | 1 | - | signup + composite | authCB | Public |
| `auth-service-oauth2-authorization` | `/auth-service/oauth2/authorization/**` | ALL | auth-service | 2 | StripPrefix=1 | - | - | Public |
| `auth-service-oauth2-callback` | `/auth-service/login/oauth2/code/**` | ALL | auth-service | 3 | StripPrefix=1 | - | - | Public |
| `auth-service-api-prefixed` | `/auth-service/api/v1/auth/**` | ALL | auth-service | 4 | StripPrefix=1 | unauthenticated + IP | authCB | Public |
| `auth-service-profile` | `/auth-service/api/v1/profile/**` | ALL | auth-service | 5 | StripPrefix=1 | authenticated + user | authCB | Required |
| `auth-service-users` | `/api/v1/users/**` | ALL | auth-service | 6 | - | unauthenticated + IP | authCB | Public |
| `auth-service-api` | `/api/v1/auth/**` | ALL | auth-service | 7 | - | unauthenticated + IP | authCB | Public |

### Auth Service - RBAC/Membership/Seller Routes

| Route ID | Path | Target | Order | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|--------------|-----------------|------|
| `auth-service-admin` | `/api/v1/admin/rbac/**`, `/api/v1/admin/memberships/**`, `/api/v1/admin/seller/**` | auth-service | 8 | authenticated + user | authCB | SUPER_ADMIN / SHOPPING_ADMIN |
| `auth-service-memberships` | `/api/v1/memberships/**` | auth-service | 9 | authenticated + user | authCB | Required |
| `auth-service-seller` | `/api/v1/seller/**` | auth-service | 10 | authenticated + user | authCB | Required |
| `auth-service-permissions` | `/api/v1/permissions/**` | auth-service | 11 | authenticated + user | authCB | Required |

### Blog Service Routes

| Route ID | Path | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|---------|--------------|-----------------|------|
| `blog-service-file-route` | `/api/v1/blog/file/**` | blog-service | 8 | StripPrefix=3, RequestSize=100MB | authenticated + user | blogCB | Required |
| `blog-service-route` | `/api/v1/blog/**` | blog-service | 9 | StripPrefix=3 | unauthenticated + IP | blogCB | GET: Public, Others: Required |

### Shopping Service Routes

| Route ID | Path | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|---------|--------------|-----------------|------|
| `shopping-service-route` | `/api/v1/shopping/**` | shopping-service | - | StripPrefix=3 | unauthenticated + IP | shoppingCB | Partial (products/categories: Public) |

### Notification Service Routes

| Route ID | Path | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|---------|--------------|-----------------|------|
| `notification-service-websocket` | `/notification/ws/**` | notification-service | 49 | StripPrefix=1 | - | - | Public |
| `notification-service-route` | `/notification/api/v1/notifications/**`, `/api/v1/notifications/**` | notification-service | 50 | StripPrefix=1 | authenticated + user | - | Required |

### Chatbot Service Routes

| Route ID | Path | Method | Target | Order | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|--------|-------|--------------|-----------------|------|
| `chatbot-service-health` | `/api/v1/chat/health` | ALL | chatbot-service | 0 | - | - | Public |
| `chatbot-service-stream` | `/api/v1/chat/stream` | POST | chatbot-service | 1 | authenticated + user | chatbotCB | Required |
| `chatbot-service-documents` | `/api/v1/chat/documents/**` | ALL | chatbot-service | 2 | authenticated + user | chatbotCB | Required |
| `chatbot-service-route` | `/api/v1/chat/**` | ALL | chatbot-service | 3 | authenticated + user | chatbotCB | Required |

### Prism Service Routes

| Route ID | Path | Target | Order | Filters | Rate Limiter | Circuit Breaker | Auth |
|----------|------|--------|-------|---------|--------------|-----------------|------|
| `prism-service-health` | `/api/v1/prism/health`, `/api/v1/prism/ready` | prism-service | 0 | RewritePath | - | - | Public |
| `prism-service-sse` | `/api/v1/prism/sse/**` | prism-service | 1 | RewritePath | - | - | Required |
| `prism-service-route` | `/api/v1/prism/**` | prism-service | - | RewritePath | authenticated + user | prismCB | Required |

- Prism RewritePath: `/api/v1/prism/{segment}` -> `/api/v1/{segment}`

## 라우팅 우선순위

라우팅은 `order` 값이 낮은 순서대로 평가됩니다. 동일 서비스 내에서 구체적인 경로가 먼저 매칭되도록 설계되어 있습니다.

**핵심 원칙**:
1. 특정 엔드포인트(예: `/api/v1/auth/login`) → order 0
2. 기능별 그룹(예: `/api/v1/admin/**`) → order 8~11
3. 범용 catch-all(예: `/api/v1/auth/**`) → order 7

## CORS 설정

> **소스 파일**: `SecurityConfig.java` - `corsWebFilter()`

| 설정 항목 | 값 |
|-----------|-----|
| **허용 Origin** | `http://localhost:30000`, `https://localhost:30000`, `http://localhost:8080`, `https://portal-universe:30000` |
| **허용 메서드** | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| **허용 헤더** | `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`, `Cache-Control` |
| **Credentials** | `true` |
| **Max Age** | `3600` (1시간, Preflight 캐싱) |

Preflight 요청(OPTIONS)은 Gateway 단계에서 처리되어 각 마이크로서비스의 CORS 부담을 덜어줍니다.

```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:30000
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, Accept, Origin, X-Requested-With, Cache-Control
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

## 환경별 서비스 URL

### Local 환경 (`application-local.yml`)

| 서비스 | URL |
|--------|-----|
| api-gateway | `http://localhost:8080` |
| auth-service | `http://localhost:8081` |
| blog-service | `http://localhost:8082` |
| shopping-service | `http://localhost:8083` |
| notification-service | `http://localhost:8084` |
| prism-service | `http://localhost:8085` |
| chatbot-service | `http://localhost:8086` |

### Docker Compose 환경 (`application-docker.yml`)

| 서비스 | URL |
|--------|-----|
| api-gateway | `http://api-gateway:8080` |
| auth-service | `http://auth-service:8081` |
| blog-service | `http://blog-service:8082` |
| shopping-service | `http://shopping-service:8083` |
| notification-service | `http://notification-service:8084` |
| prism-service | `http://prism-service:8085` |
| chatbot-service | `http://chatbot-service:8086` |

### Kubernetes 환경 (`application-kubernetes.yml`)

Docker와 동일한 서비스 이름 사용 (K8s Service DNS 해결).

## 요청/응답 예시

### 로그인 (Rate Limited)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john_doe", "password": "SecurePass123!"}'
```

**성공 (200)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImtleS1kZWZhdWx0In0...",
    "refreshToken": "...",
    "tokenType": "Bearer"
  },
  "error": null
}
```

### 블로그 게시글 조회 (Public GET)

```bash
curl -X GET http://localhost:8080/api/v1/blog/posts/507f1f77bcf86cd799439011
```

**성공 (200)**:
```json
{
  "success": true,
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "title": "Spring Cloud Gateway 설정",
    "content": "..."
  },
  "error": null
}
```

### 인증 실패

```bash
curl -X POST http://localhost:8080/api/v1/blog/posts \
  -H "Content-Type: application/json" \
  -d '{"title": "New Post"}'
```

**실패 (401)**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A001",
    "message": "Authentication required"
  }
}
```

### 파일 업로드 (100MB 제한)

```bash
curl -X POST http://localhost:8080/api/v1/blog/file/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@image.png"
```

## 라우팅 설정 파일

```
services/api-gateway/src/main/resources/
├── application.yml              # 공통 라우트, Circuit Breaker, JWT, 보안 설정
├── application-local.yml        # Local 서비스 URL, 완화된 Rate Limiting
├── application-docker.yml       # Docker DNS URL, Zipkin 분산 추적
└── application-kubernetes.yml   # K8s DNS URL, Liveness/Readiness Probe
```

## 관련 문서

- [보안 및 인증](./security-authentication.md) - JWT, RBAC, Token Blacklist, 보안 헤더
- [Rate Limiting](./rate-limiting.md) - 5개 Rate Limiter, Key Resolver, 429 응답
- [장애 복원력](./resilience.md) - Circuit Breaker, Fallback, Timeout
- [헬스체크 및 모니터링](./health-monitoring.md) - Health API, Prometheus, Zipkin
- [에러 코드 레퍼런스](./error-reference.md) - 전체 에러 코드 및 트러블슈팅
