---
id: api-gateway-architecture-system-overview
title: API Gateway 시스템 아키텍처
type: architecture
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api-gateway, spring-cloud-gateway, security, oauth2, jwt]
related: []
---

# API Gateway 시스템 아키텍처

## 1. 시스템 개요

Portal Universe의 API Gateway는 Spring Cloud Gateway를 기반으로 구축된 중앙 진입점으로, 다음과 같은 핵심 역할을 수행합니다:

### 주요 역할

- **단일 진입점(Single Entry Point)**: 모든 클라이언트 요청을 받아 적절한 마이크로서비스로 라우팅
- **보안 게이트웨이**: JWT 기반 인증/인가 처리 및 OAuth2 Resource Server 역할
- **CORS 관리**: 프론트엔드 애플리케이션의 CORS 정책 중앙 관리
- **요청/응답 로깅**: 통합 로깅을 통한 API 호출 추적 및 모니터링
- **회로 차단(Circuit Breaker)**: Resilience4j를 통한 장애 격리 및 Fallback 처리
- **프로토콜 변환**: OIDC 인증 흐름 지원을 위한 헤더 변환

### 기술적 특징

- **비동기 논블로킹**: Spring WebFlux 기반으로 높은 처리량 달성
- **선언적 라우팅**: application.yml을 통한 간결한 라우팅 설정
- **필터 체인**: 요청 전/후 처리를 위한 확장 가능한 필터 아키텍처

## 2. 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                     Client Applications                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Portal Shell │  │Blog Frontend │  │Shopping FE   │         │
│  │  (Vue 3)     │  │  (Vue 3)     │  │  (React)     │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                 │                  │                   │
└─────────┼─────────────────┼──────────────────┼───────────────────┘
          │                 │                  │
          │  HTTP Requests (with JWT token)    │
          └─────────────────┼──────────────────┘
                            ▼
          ┌─────────────────────────────────────────┐
          │         API Gateway (Port 8080)         │
          │     Spring Cloud Gateway (WebFlux)      │
          ├─────────────────────────────────────────┤
          │                                         │
          │  ┌────────────────────────────────┐    │
          │  │   Pre-Filters (Request Path)   │    │
          │  │   - RequestPathLoggingFilter   │    │
          │  │   - CorsWebFilter              │    │
          │  │   - GlobalLoggingFilter        │    │
          │  └────────────────────────────────┘    │
          │              │                          │
          │              ▼                          │
          │  ┌────────────────────────────────┐    │
          │  │     SecurityConfig              │    │
          │  │  - JWT Validation (jwk-set-uri)│    │
          │  │  - Public/Private Path Split   │    │
          │  │  - CORS Configuration          │    │
          │  └────────────────────────────────┘    │
          │              │                          │
          │              ▼                          │
          │  ┌────────────────────────────────┐    │
          │  │      Route Predicates           │    │
          │  │  /api/v1/auth/**               │    │
          │  │  /api/v1/blog/**               │    │
          │  │  /api/v1/shopping/**           │    │
          │  │  /api/v1/notifications/**      │    │
          │  └────────────────────────────────┘    │
          │              │                          │
          │              ▼                          │
          │  ┌────────────────────────────────┐    │
          │  │   Gateway Filters               │    │
          │  │  - OidcPrefixGatewayFilter     │    │
          │  │  - Circuit Breaker Filter      │    │
          │  │  - RewritePath Filter          │    │
          │  └────────────────────────────────┘    │
          │              │                          │
          │              ▼                          │
          │  ┌────────────────────────────────┐    │
          │  │   Post-Filters                  │    │
          │  │  - GlobalLoggingFilter (resp)  │    │
          │  └────────────────────────────────┘    │
          │                                         │
          └──────────────┬──────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ Auth Service  │ │ Blog Service  │ │Shopping Svc   │
│   (Port 8081) │ │   (Port 8082) │ │   (Port 8083) │
│               │ │               │ │               │
│ - OAuth2      │ │ - MongoDB     │ │ - MySQL       │
│ - MySQL       │ │ - S3          │ │ - Feign       │
│ - Kafka       │ │               │ │               │
└───────────────┘ └───────────────┘ └───────────────┘
```

## 3. 핵심 컴포넌트

### 3.1 SecurityConfig.java

API Gateway의 보안 정책을 정의하는 핵심 컴포넌트입니다.

**주요 기능:**

- **공개 경로 처리 (Order 1)**:
  ```java
  /auth-service/**        // 인증 엔드포인트
  /api/users/**          // 사용자 공개 API
  /actuator/**           // 헬스체크 및 메트릭
  ```
  → `permitAll()` 적용, JWT 검증 없이 접근 가능

- **비공개 경로 처리 (Order 2)**:
  ```java
  /**                    // 기타 모든 경로
  ```
  → `authenticated()` 적용, JWT 인증 필수

- **JWT 검증 설정**:
  - `jwk-set-uri`: Auth Service에서 공개 키 가져오기
  - `issuer-uri`: 토큰 발급자 검증
  - Bearer Token 자동 파싱

- **CORS 설정**:
  - `FrontendProperties`에서 허용할 Origin 목록 주입
  - 프로필별 동적 CORS 정책 적용

**보안 흐름:**
```
Request → CORS 검증 → Public/Private 경로 판단 → JWT 검증 (비공개 경로) → 라우팅
```

### 3.2 GlobalLoggingFilter.java

모든 API 요청/응답을 로깅하는 전역 필터입니다.

**실행 순서:** `Ordered.HIGHEST_PRECEDENCE + 2`

**로깅 내용:**
- **요청 정보**:
  - HTTP Method, Request URI
  - Client IP
  - User-Agent
  - Authorization Header (마스킹)

- **응답 정보**:
  - HTTP Status Code
  - 처리 시간 (ms)

**특징:**
- WebFlux의 `Mono` 체인을 활용한 비동기 로깅
- 민감 정보(JWT 토큰) 자동 마스킹
- Micrometer를 통한 메트릭 수집 연동 가능

### 3.3 OidcPrefixGatewayFilterFactory.java

OIDC(OpenID Connect) 인증 흐름을 지원하기 위한 커스텀 필터입니다.

**역할:**
- `X-Forwarded-Prefix` 헤더 추가
- OIDC 콜백 URL을 Gateway 경로로 리다이렉트
- Auth Service와 프론트엔드 간 인증 흐름 중계

**적용 대상:** `/api/v1/auth/**` 경로

**사용 예시:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          filters:
            - OidcPrefix
```

### 3.4 FallbackController.java

Resilience4j Circuit Breaker가 열렸을 때 실행되는 Fallback 핸들러입니다.

**주요 Fallback 엔드포인트:**
```java
/fallback/auth          → Auth Service 장애 시
/fallback/blog          → Blog Service 장애 시
/fallback/shopping      → Shopping Service 장애 시
/fallback/notification  → Notification Service 장애 시
```

**응답 형식:**
```json
{
  "success": false,
  "code": "GATEWAY_ERROR",
  "message": "[서비스명] is temporarily unavailable. Please try again later.",
  "data": null
}
```

**Circuit Breaker 설정:**
- Failure Rate Threshold: 50%
- Wait Duration in Open State: 10초
- Sliding Window Size: 10 requests

### 3.5 FrontendProperties.java

프론트엔드 애플리케이션의 URL을 프로필별로 관리하는 설정 클래스입니다.

**설정 예시 (application-local.yml):**
```yaml
frontend:
  urls:
    - http://localhost:30000  # Portal Shell
    - http://localhost:30001  # Blog Frontend
    - http://localhost:30002  # Shopping Frontend
```

**사용처:**
- SecurityConfig의 CORS allowedOrigins
- Redirect URL 검증

## 4. 필터 체인 순서

API Gateway의 필터는 다음 순서로 실행됩니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Request Processing                          │
└─────────────────────────────────────────────────────────────────┘

1. requestPathLoggingFilter (Ordered.HIGHEST_PRECEDENCE)
   ↓ 요청 경로 및 메서드 로깅

2. CorsWebFilter (HIGHEST_PRECEDENCE + 1)
   ↓ CORS Preflight 처리 (OPTIONS), Origin 검증

3. GlobalLoggingFilter (HIGHEST_PRECEDENCE + 2)
   ↓ 요청 상세 정보 로깅 (IP, User-Agent, Header)

4. Security Filters (Spring Security)
   ↓ JWT 토큰 추출 및 검증
   ↓ 공개/비공개 경로 판단
   ↓ SecurityContext 설정

5. Gateway Route Predicates
   ↓ Path, Method, Header 기반 라우팅 규칙 평가

6. Gateway Filters (per-route)
   ↓ OidcPrefixGatewayFilter (auth 경로)
   ↓ CircuitBreakerFilter (모든 경로)
   ↓ RewritePathFilter (경로 변환)

7. Load Balancer Client Filter
   ↓ Eureka를 통한 서비스 인스턴스 선택

8. NettyRoutingFilter
   ↓ 실제 HTTP 요청 전송 (WebFlux Reactor Netty)

┌─────────────────────────────────────────────────────────────────┐
│                     Response Processing                          │
└─────────────────────────────────────────────────────────────────┘

1. NettyRoutingFilter
   ↑ 백엔드 서비스로부터 응답 수신

2. Gateway Filters (reverse order)
   ↑ 응답 헤더 변환 등

3. GlobalLoggingFilter
   ↑ 응답 상태 코드, 처리 시간 로깅

4. CORS 응답 헤더 추가
   ↑ Access-Control-Allow-* 헤더

5. Client에게 응답 반환
```

### 필터 우선순위 설정 이유

| Order | 필터 | 이유 |
|-------|------|------|
| `HIGHEST_PRECEDENCE` | requestPathLoggingFilter | 모든 요청을 가장 먼저 기록하여 디버깅 용이 |
| `HIGHEST_PRECEDENCE + 1` | CorsWebFilter | CORS Preflight은 보안 검증 전에 처리 필요 |
| `HIGHEST_PRECEDENCE + 2` | GlobalLoggingFilter | CORS 통과 후 상세 로깅 |
| (default) | Security Filters | 인증/인가는 라우팅 전에 처리 |

## 5. 보안 흐름

### 5.1 공개 경로 (Public Endpoints)

**허용 대상:**
```
/auth-service/**         → 로그인, 회원가입, 토큰 발급
/api/users/**           → 사용자 조회 (읽기 전용)
/actuator/**            → 헬스체크, 메트릭
/.well-known/**         → OIDC Discovery
```

**흐름:**
```
Client → Gateway → Public Path SecurityFilterChain (Order 1)
                → permitAll() → Routing → Backend Service
```

### 5.2 비공개 경로 (Protected Endpoints)

**대상:** 공개 경로를 제외한 모든 경로

**흐름:**
```
Client (with JWT) → Gateway
    ↓
    CORS 검증
    ↓
    JWT 추출 (Authorization: Bearer <token>)
    ↓
    JWT 검증 (jwk-set-uri 통해 공개 키 가져오기)
    ↓
    - 서명 검증 (RS256)
    - 만료 시간 검증 (exp claim)
    - 발급자 검증 (iss claim = issuer-uri)
    - 대상 검증 (aud claim)
    ↓
    SecurityContext 설정 (Authentication 객체 생성)
    ↓
    Private Path SecurityFilterChain (Order 2)
    ↓
    authenticated() 통과 → Routing → Backend Service
```

### 5.3 JWT 검증 상세

**jwk-set-uri:**
```
http://auth-service:8081/.well-known/jwks.json
```
→ Auth Service의 RSA 공개 키 세트를 가져와 JWT 서명 검증

**issuer-uri:**
```
http://auth-service:8081
```
→ JWT의 `iss` claim이 이 값과 일치하는지 검증

**JWT Payload 예시:**
```json
{
  "sub": "user123",
  "iss": "http://auth-service:8081",
  "aud": ["api-gateway"],
  "exp": 1737234567,
  "iat": 1737230967,
  "scope": ["read", "write"]
}
```

### 5.4 에러 처리

**401 Unauthorized:**
- JWT 토큰 없음
- JWT 만료
- 서명 불일치
- 잘못된 형식

**403 Forbidden:**
- 인증은 성공했으나 권한 부족
- Scope 불일치

**Gateway 응답 형식:**
```json
{
  "timestamp": "2026-01-18T10:30:00.000Z",
  "path": "/api/v1/blog/posts",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

## 6. 기술 스택

### Core Framework

| 기술 | 버전 | 역할 |
|------|------|------|
| Spring Boot | 3.5.5 | 애플리케이션 기반 프레임워크 |
| Spring Cloud | 2025.0.0 | 마이크로서비스 인프라 |
| Java | 17 | 런타임 환경 |

### Gateway & Routing

| 기술 | 설명 |
|------|------|
| Spring Cloud Gateway | WebFlux 기반 비동기 게이트웨이 |
| Reactor Netty | HTTP 클라이언트/서버 엔진 |
| Project Reactor | Reactive Streams 구현 |

### Security

| 기술 | 역할 |
|------|------|
| Spring Security | 인증/인가 프레임워크 |
| OAuth2 Resource Server | JWT 검증 및 Bearer Token 처리 |
| JJWT (nimbus-jose-jwt) | JWT 파싱 및 서명 검증 |

### Service Discovery

| 기술 | 설명 |
|------|------|
| Eureka Client | 서비스 레지스트리 연동 (현재 비활성화 옵션 있음) |

### Resilience

| 기술 | 역할 |
|------|------|
| Resilience4j | Circuit Breaker, Retry, Rate Limiter |
| Spring Cloud CircuitBreaker | Resilience4j 추상화 레이어 |

### Observability

| 기술 | 역할 |
|------|------|
| Micrometer | 메트릭 수집 추상화 |
| Prometheus | 메트릭 저장소 (Micrometer Registry) |
| Zipkin | 분산 추적 (Distributed Tracing) |
| Spring Boot Actuator | 헬스체크, 메트릭 엔드포인트 |

### Configuration

| 기술 | 설명 |
|------|------|
| Spring Cloud Config Client | Config Service 연동 |
| Git Config Repository | 외부 설정 저장소 |

### 의존성 버전 관리

**build.gradle:**
```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
}
```

## 7. 라우팅 규칙

### 7.1 라우팅 테이블

| 경로 | 대상 서비스 | 포트 | 설명 |
|------|------------|------|------|
| `/api/v1/auth/**` | auth-service | 8081 | 인증/인가 API |
| `/api/v1/blog/**` | blog-service | 8082 | 블로그 CRUD |
| `/api/v1/shopping/**` | shopping-service | 8083 | 전자상거래 |
| `/api/v1/notifications/**` | notification-service | 8084 | 알림 |

### 7.2 경로 변환 (RewritePath)

**예시:**
```
Client Request:  /api/v1/blog/posts
     ↓
Gateway Rewrite: /posts
     ↓
Blog Service:    /posts (실제 엔드포인트)
```

**설정 (application.yml):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service
          uri: lb://blog-service
          predicates:
            - Path=/api/v1/blog/**
          filters:
            - RewritePath=/api/v1/blog(?<segment>/?.*), ${segment}
            - name: CircuitBreaker
              args:
                name: blogCircuitBreaker
                fallbackUri: forward:/fallback/blog
```

## 8. 배포 및 운영

### 8.1 프로필별 설정

**local:**
- Config Server: localhost:8888
- Eureka: localhost:8761
- Frontend URLs: localhost:30000-30002

**docker:**
- Config Server: config-service:8888
- Eureka: eureka-server:8761
- Frontend URLs: host.docker.internal:30000-30002

**k8s:**
- Config Server: config-service.default.svc.cluster.local:8888
- Eureka: eureka-server.default.svc.cluster.local:8761
- Frontend URLs: 프로덕션 도메인

### 8.2 Health Check

**엔드포인트:** `/actuator/health`

**응답 예시:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "reactiveGateway": {"status": "UP"},
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "authCircuitBreaker": "CLOSED",
        "blogCircuitBreaker": "CLOSED"
      }
    }
  }
}
```

### 8.3 메트릭

**Prometheus 엔드포인트:** `/actuator/prometheus`

**주요 메트릭:**
- `spring_cloud_gateway_requests_total`: 총 요청 수
- `spring_cloud_gateway_requests_seconds`: 요청 처리 시간
- `resilience4j_circuitbreaker_state`: Circuit Breaker 상태
- `jvm_memory_used_bytes`: JVM 메모리 사용량

### 8.4 분산 추적

**Zipkin 연동:**
- Gateway에서 생성된 Trace ID가 모든 백엔드 서비스로 전파
- `X-B3-TraceId`, `X-B3-SpanId` 헤더 자동 추가
- Zipkin UI에서 전체 요청 흐름 시각화

## 9. 확장 포인트

### 9.1 커스텀 필터 추가

**방법 1: Global Filter**
```java
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 전처리
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 후처리
        }));
    }

    @Override
    public int getOrder() {
        return -1; // 우선순위 설정
    }
}
```

**방법 2: Gateway Filter Factory**
```java
@Component
public class CustomGatewayFilterFactory extends AbstractGatewayFilterFactory<CustomGatewayFilterFactory.Config> {
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 필터 로직
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // 설정 프로퍼티
    }
}
```

### 9.2 커스텀 라우팅 로직

**Route Locator Bean 정의:**
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("custom_route", r -> r
            .path("/custom/**")
            .and()
            .header("X-Custom-Header", "value")
            .filters(f -> f
                .addRequestHeader("X-Gateway", "portal-universe")
                .circuitBreaker(c -> c.setFallbackUri("forward:/fallback/custom")))
            .uri("lb://custom-service"))
        .build();
}
```

### 9.3 인증 전략 확장

**OAuth2 Scope 기반 권한 검증:**
```java
http.authorizeExchange(exchanges -> exchanges
    .pathMatchers("/api/v1/admin/**").hasAuthority("SCOPE_admin")
    .pathMatchers("/api/v1/user/**").hasAuthority("SCOPE_user")
    .anyExchange().authenticated()
);
```

## 10. 트러블슈팅

### 10.1 CORS 에러

**증상:** 브라우저 콘솔에서 CORS 에러 발생

**원인:**
- `FrontendProperties.urls`에 프론트엔드 URL 미등록
- OPTIONS 요청이 보안 필터에서 차단

**해결:**
```yaml
frontend:
  urls:
    - http://localhost:30000
    - https://portal-universe.com
```

### 10.2 JWT 검증 실패

**증상:** 401 Unauthorized, "Invalid token" 메시지

**체크리스트:**
- Auth Service의 jwk-set-uri가 접근 가능한가?
- JWT의 `iss` claim이 issuer-uri와 일치하는가?
- JWT가 만료되지 않았는가? (`exp` claim)
- 서명 알고리즘이 RS256인가?

**로그 확인:**
```bash
kubectl logs -f api-gateway-pod | grep "JWT"
```

### 10.3 Circuit Breaker 열림

**증상:** Fallback 응답 반환, "Service temporarily unavailable"

**원인:**
- 백엔드 서비스 장애
- 네트워크 지연
- Failure Rate Threshold 초과 (기본 50%)

**복구:**
- 백엔드 서비스 재시작
- 10초 대기 후 Circuit Breaker 자동 Half-Open
- Actuator로 Circuit Breaker 상태 확인:
  ```bash
  curl http://localhost:8080/actuator/circuitbreakers
  ```

### 10.4 라우팅 실패

**증상:** 404 Not Found, "No matching route"

**디버깅:**
```bash
# 등록된 라우트 확인
curl http://localhost:8080/actuator/gateway/routes

# 특정 경로 매칭 테스트
curl -v http://localhost:8080/api/v1/blog/posts
```

**주의사항:**
- Path Predicate의 대소문자 구분
- RewritePath 정규식 오류
- Eureka에 서비스 미등록

## 11. 참고 자료

### 공식 문서
- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Resilience4j User Guide](https://resilience4j.readme.io/docs)

### 프로젝트 내부 문서
- [API Gateway 설정 파일](../config/README.md)
- [보안 정책 가이드](../security/oauth2-flow.md)
- [운영 가이드](../../../docs/runbooks/api-gateway-operations.md)

### 관련 서비스 문서
- [Auth Service 아키텍처](../../auth-service/docs/architecture/oauth2-server.md)
- [Config Service 설정 관리](../../config-service/docs/architecture/centralized-config.md)

---

**문서 버전:** 1.0.0
**최종 검토:** 2026-01-18
**다음 리뷰 예정:** 2026-04-18
