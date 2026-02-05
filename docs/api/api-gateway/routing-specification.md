---
id: api-gateway-api-routing-specification
title: API Gateway 라우팅 명세
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter-agent
tags: [api-gateway, routing, spring-cloud-gateway, endpoints]
related: []
---

# API Gateway 라우팅 명세

## 개요

API Gateway는 Portal Universe 플랫폼의 단일 진입점으로, Spring Cloud Gateway(WebFlux 기반)를 사용하여 클라이언트 요청을 적절한 백엔드 마이크로서비스로 라우팅합니다. 주요 역할은 다음과 같습니다:

- **요청 라우팅**: 경로 패턴 기반으로 요청을 대상 서비스로 전달
- **JWT 인증**: OAuth2 Resource Server로 동작하여 JWT 토큰 검증
- **CORS 처리**: 프론트엔드 애플리케이션의 크로스 오리진 요청 허용
- **공통 보안**: 공개/비공개 엔드포인트 구분 및 접근 제어
- **로드 밸런싱**: 서비스 인스턴스 간 부하 분산 (Kubernetes 환경)

## 라우팅 규칙

### 전체 라우팅 테이블

| 경로 패턴 | 대상 서비스 | 인증 필요 | 설명 |
|-----------|-------------|-----------|------|
| `/api/v1/auth/**` | auth-service:8081 | No | 인증 API (로그인, 회원가입, 토큰 발급) |
| `/api/v1/blog/**` | blog-service:8082 | Yes | 블로그 CRUD API (게시글, 댓글, 태그, 시리즈) |
| `/api/v1/shopping/**` | shopping-service:8083 | Yes | 쇼핑 API (상품 조회, 주문 관리) |
| `/api/v1/notifications/**` | notification-service:8084 | Yes | 알림 API (사용자 알림 조회) |
| `/auth-service/**` | auth-service:8081 | No | OAuth2 Authorization Server 엔드포인트 |
| `/api/users/**` | auth-service:8081 | No | 사용자 공개 API (회원가입, 프로필 조회) |
| `/actuator/**` | self (gateway) | No | 헬스체크, 메트릭 (Prometheus) |

### 라우팅 우선순위

라우팅은 **먼저 매칭되는 패턴**이 적용됩니다. Spring Cloud Gateway는 다음 순서로 평가합니다:

1. 구체적인 경로 (`/api/v1/auth/**`)
2. 일반적인 경로 (`/api/**`)
3. 기본 경로 (`/**`)

## 공개 경로 (Public Endpoints)

다음 경로는 JWT 토큰 없이 접근 가능합니다:

### 1. OAuth2 Authorization Server 엔드포인트
```
/auth-service/oauth2/authorize
/auth-service/oauth2/token
/auth-service/oauth2/jwks
/auth-service/.well-known/oauth-authorization-server
/auth-service/.well-known/openid-configuration
```

**용도**: OAuth2/OIDC 프로토콜 엔드포인트. 프론트엔드의 oidc-client-ts가 이 경로를 통해 인증 흐름을 진행합니다.

### 2. 사용자 공개 API
```
POST   /api/users/register     # 회원가입
GET    /api/users/{id}         # 사용자 프로필 조회 (공개 정보)
POST   /api/v1/auth/login      # 로그인 (내부적으로 토큰 발급)
POST   /api/v1/auth/refresh    # 리프레시 토큰 갱신
```

**용도**: 인증되지 않은 사용자도 접근할 수 있는 기본 기능.

### 3. 헬스체크 및 메트릭
```
GET    /actuator/health        # 서비스 상태 확인
GET    /actuator/prometheus    # Prometheus 메트릭
GET    /actuator/info          # 애플리케이션 정보
```

**용도**: 모니터링 시스템(Prometheus, Grafana)과 Kubernetes Liveness/Readiness Probe가 사용.

## 인증 필요 경로 (Protected Endpoints)

위 공개 경로를 제외한 **모든 경로**는 JWT 토큰이 필요합니다.

### JWT 토큰 요구사항

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

- **헤더**: `Authorization`
- **형식**: `Bearer <access_token>`
- **발급처**: auth-service의 OAuth2 Authorization Server
- **유효기간**: 기본 1시간 (설정 가능)
- **검증 방식**: API Gateway가 JWK Set을 통해 서명 검증

### 토큰 검증 실패 시 응답

**401 Unauthorized** (토큰 없음 또는 만료):
```json
{
  "timestamp": "2026-01-18T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/blog/posts"
}
```

**403 Forbidden** (권한 부족):
```json
{
  "timestamp": "2026-01-18T14:23:45.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/users"
}
```

## CORS 설정

### 허용 설정

| 설정 항목 | 값 |
|-----------|-----|
| **허용 Origin** | `http://localhost:30000` (portal-shell)<br>`http://localhost:8080` (gateway)<br>`https://portal-universe:30000` (production) |
| **허용 메서드** | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH` |
| **허용 헤더** | `*` (모든 헤더) |
| **노출 헤더** | `Authorization`, `Content-Type` |
| **Credentials** | `true` (쿠키 및 인증 정보 허용) |
| **Max Age** | `3600` (1시간, Preflight 캐싱) |

### Preflight 요청 처리

브라우저는 다음 조건에서 OPTIONS 요청을 먼저 보냅니다:

- 요청 메서드가 `GET`, `HEAD`, `POST` 외의 것 (`PUT`, `DELETE` 등)
- 커스텀 헤더 사용 (`Authorization`)
- `Content-Type`이 `application/json`

API Gateway는 이러한 Preflight 요청에 자동으로 응답합니다:

```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:30000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

## 요청/응답 예시

### 예시 1: 공개 API - 회원가입

**요청**:
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "displayName": "John Doe"
  }'
```

**응답** (200 OK):
```json
{
  "success": true,
  "code": "A000",
  "message": "User registered successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "displayName": "John Doe",
    "createdAt": "2026-01-18T14:23:45.123Z"
  }
}
```

### 예시 2: 보호된 API - 블로그 게시글 조회

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/blog/posts/507f1f77bcf86cd799439011 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**응답** (200 OK):
```json
{
  "success": true,
  "code": "B000",
  "message": "Post retrieved successfully",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "title": "Spring Boot 3.5.5 Migration Guide",
    "content": "...",
    "author": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "username": "john_doe",
      "displayName": "John Doe"
    },
    "tags": ["spring-boot", "java", "migration"],
    "createdAt": "2026-01-15T10:30:00.000Z",
    "updatedAt": "2026-01-18T12:00:00.000Z"
  }
}
```

### 예시 3: 인증 실패 - 토큰 없음

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/blog/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Post",
    "content": "Content here"
  }'
```

**응답** (401 Unauthorized):
```json
{
  "timestamp": "2026-01-18T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/blog/posts"
}
```

### 예시 4: 헬스체크

**요청**:
```bash
curl -X GET http://localhost:8080/actuator/health
```

**응답** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 예시 5: OAuth2 토큰 발급

**요청**:
```bash
curl -X POST http://localhost:8080/auth-service/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=" \
  -d "grant_type=authorization_code" \
  -d "code=SplxlOBeZQQYbYS6WxSbIA" \
  -d "redirect_uri=http://localhost:30000/callback"
```

**응답** (200 OK):
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "tGzv3JOkF0XG5Qx2TlKWIA...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile email"
}
```

## 환경별 서비스 URL

### Local 환경
```yaml
auth-service: http://localhost:8081
blog-service: http://localhost:8082
shopping-service: http://localhost:8083
notification-service: http://localhost:8084
```

### Docker Compose 환경
```yaml
auth-service: http://auth-service:8081
blog-service: http://blog-service:8082
shopping-service: http://shopping-service:8083
notification-service: http://notification-service:8084
```

### Kubernetes 환경
```yaml
auth-service: http://auth-service.default.svc.cluster.local:8081
blog-service: http://blog-service.default.svc.cluster.local:8082
shopping-service: http://shopping-service.default.svc.cluster.local:8083
notification-service: http://notification-service.default.svc.cluster.local:8084
```

## 라우팅 설정 파일

API Gateway의 라우팅 규칙은 다음 파일에서 관리됩니다:

```
services/api-gateway/src/main/resources/
├── application.yml           # 공통 설정
├── application-local.yml     # 로컬 환경
├── application-docker.yml    # Docker Compose 환경
└── application-k8s.yml       # Kubernetes 환경
```

**예시 라우팅 설정** (application.yml):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${AUTH_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/auth/**,/auth-service/**,/api/users/**
          filters:
            - RewritePath=/auth-service/(?<segment>.*), /${segment}

        - id: blog-service
          uri: ${BLOG_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/api/v1/blog/**
          filters:
            - TokenRelay=

        - id: shopping-service
          uri: ${SHOPPING_SERVICE_URL:http://localhost:8083}
          predicates:
            - Path=/api/v1/shopping/**
          filters:
            - TokenRelay=

        - id: notification-service
          uri: ${NOTIFICATION_SERVICE_URL:http://localhost:8084}
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - TokenRelay=
```

## 참고사항

### 1. 타임아웃 설정
- **Connect Timeout**: 3초
- **Response Timeout**: 30초
- **Read Timeout**: 60초

### 2. 재시도 정책
- 일시적 네트워크 오류 시 최대 3회 재시도
- 재시도 가능한 HTTP 메서드: GET, HEAD, OPTIONS
- 재시도 불가능한 메서드: POST, PUT, DELETE, PATCH

### 3. Circuit Breaker (Resilience4j)
- 실패율 50% 이상 시 Circuit Open
- Half-Open 상태 전환: 10초 후
- 슬라이딩 윈도우: 100 요청

### 4. Rate Limiting
- 현재 미적용 (향후 Redis 기반 구현 예정)

## 관련 문서

- [OAuth2 인증 가이드](../../auth-service/docs/guides/oauth2-guide.md)
- [Spring Cloud Gateway 설정](../guides/gateway-configuration.md)
- [보안 정책](../../common/docs/security/security-policy.md)
- [모니터링 가이드](../../../monitoring/docs/monitoring-guide.md)
