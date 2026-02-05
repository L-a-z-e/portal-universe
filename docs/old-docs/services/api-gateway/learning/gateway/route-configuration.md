# Route Configuration

Spring Cloud Gateway의 라우팅 설정 방법을 학습합니다.

## 개요

Spring Cloud Gateway는 **Predicate**와 **Filter**를 조합하여 라우팅 규칙을 정의합니다.

```
Request → Predicate 매칭 → Filter 적용 → Backend Service
```

## 핵심 구성 요소

### 1. Route

라우팅의 기본 단위로, 고유 ID, 목적지 URI, Predicate, Filter로 구성됩니다.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service-route        # 고유 식별자
          uri: http://blog-service:8082 # 목적지
          predicates:                    # 조건
            - Path=/api/blog/**
          filters:                       # 변환
            - StripPrefix=2
```

### 2. Predicate (조건)

요청이 라우트에 매칭되는지 판단하는 조건입니다.

| Predicate | 설명 | 예시 |
|-----------|------|------|
| `Path` | URL 경로 매칭 | `Path=/api/blog/**` |
| `Method` | HTTP 메서드 | `Method=GET,POST` |
| `Header` | 헤더 값 | `Header=X-Request-Id, \d+` |
| `Query` | 쿼리 파라미터 | `Query=category, java` |
| `Host` | 호스트명 | `Host=**.myhost.org` |
| `Cookie` | 쿠키 값 | `Cookie=session, abc123` |
| `After/Before` | 시간 기반 | `After=2025-01-01T00:00:00` |
| `Weight` | 가중치 라우팅 | `Weight=group1, 8` |

### 3. Filter (필터)

요청/응답을 변환하는 컴포넌트입니다.

| Filter | 설명 | 예시 |
|--------|------|------|
| `StripPrefix` | 경로 prefix 제거 | `StripPrefix=2` |
| `RewritePath` | 경로 재작성 | `RewritePath=/api/(?<seg>.*), /${seg}` |
| `AddRequestHeader` | 요청 헤더 추가 | `AddRequestHeader=X-Trace-Id, 123` |
| `AddResponseHeader` | 응답 헤더 추가 | `AddResponseHeader=X-Response-Time, 100ms` |
| `CircuitBreaker` | 서킷 브레이커 | `name=cb, fallbackUri=forward:/fallback` |
| `RequestSize` | 요청 크기 제한 | `max-request-size=100MB` |
| `Retry` | 재시도 설정 | `retries=3, statuses=BAD_GATEWAY` |

## Portal Universe 라우팅 구성

### 서비스별 URL 정의

환경별로 다른 URL을 사용하기 위해 변수를 활용합니다.

```yaml
# application-local.yml
services:
  auth:
    url: "http://localhost:8081"
  blog:
    url: "http://localhost:8082"
  shopping:
    url: "http://localhost:8083"

# application-kubernetes.yml
services:
  auth:
    url: "http://auth-service"      # K8s DNS
  blog:
    url: "http://blog-service"
  shopping:
    url: "http://shopping-service"
```

### Auth Service 라우팅

OAuth2 관련 경로와 REST API를 분리하여 라우팅합니다.

```yaml
routes:
  # OAuth2 소셜 로그인 시작
  - id: auth-service-oauth2-authorization
    uri: ${services.auth.url}
    predicates:
      - Path=/auth-service/oauth2/authorization/**
    filters:
      - StripPrefix=1                    # /auth-service 제거
    order: 0                             # 우선순위 높음

  # OAuth2 콜백
  - id: auth-service-oauth2-callback
    uri: ${services.auth.url}
    predicates:
      - Path=/auth-service/login/oauth2/code/**
    filters:
      - StripPrefix=1
    order: 1

  # REST API (CircuitBreaker 적용)
  - id: auth-service-api
    uri: ${services.auth.url}
    predicates:
      - Path=/api/auth/**
    filters:
      - name: CircuitBreaker
        args:
          name: authCircuitBreaker
          fallbackUri: forward:/fallback/auth
    order: 5
```

### Blog Service 라우팅

파일 업로드와 일반 API를 분리하여 설정합니다.

```yaml
routes:
  # 파일 업로드 (큰 Request Size 허용)
  - id: blog-service-file-route
    uri: ${services.blog.url}
    predicates:
      - Path=/api/blog/file/**
    filters:
      - StripPrefix=2                    # /api/blog 제거
      - name: CircuitBreaker
        args:
          name: blogCircuitBreaker
          fallbackUri: forward:/fallback/blog
      - name: RequestSize
        args:
          max-request-size: 100MB        # 파일 업로드용 크기 제한
    order: 1

  # 일반 API
  - id: blog-service-route
    uri: ${services.blog.url}
    predicates:
      - Path=/api/blog/**
    filters:
      - StripPrefix=2
      - name: CircuitBreaker
        args:
          name: blogCircuitBreaker
          fallbackUri: forward:/fallback/blog
    order: 2
```

### Actuator Health Check 라우팅

Status Page에서 각 서비스의 상태를 확인하기 위한 라우팅입니다.

```yaml
routes:
  # Auth Service Actuator
  - id: auth-service-actuator
    uri: ${services.auth.url}
    predicates:
      - Path=/api/auth/actuator/**
    filters:
      - RewritePath=/api/auth/actuator/(?<segment>.*), /actuator/${segment}
    order: 0                             # 가장 높은 우선순위
```

## 라우팅 우선순위

`order` 값이 낮을수록 먼저 평가됩니다.

```yaml
# 구체적인 경로가 먼저 매칭되어야 함
- id: blog-service-file-route
  predicates:
    - Path=/api/blog/file/**
  order: 1                               # 먼저 평가

- id: blog-service-route
  predicates:
    - Path=/api/blog/**
  order: 2                               # 나중에 평가
```

## 고급 Predicate 조합

### 복합 조건

여러 Predicate를 AND 조건으로 조합합니다.

```yaml
predicates:
  - Path=/api/admin/**
  - Method=POST,PUT,DELETE
  - Header=X-Admin-Token, ^[a-zA-Z0-9]+$
```

### 경로 변수 사용

정규식을 활용한 경로 매칭입니다.

```yaml
predicates:
  - Path=/api/users/{userId}/orders/{orderId}
filters:
  - RewritePath=/api/users/(?<userId>.*)/orders/(?<orderId>.*), /orders/${orderId}?user=${userId}
```

## 전역 필터 (Default Filters)

모든 라우트에 적용되는 필터입니다.

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: PreserveHostHeader       # 원본 Host 헤더 유지

      # X-Forwarded 헤더 자동 추가
      x-forwarded:
        enabled: true
```

## 테스트

```bash
# 라우팅 확인
curl -v http://localhost:8080/api/blog/posts

# 헤더 확인
curl -H "X-Request-Id: test-123" http://localhost:8080/api/auth/me

# Actuator 라우팅 확인
curl http://localhost:8080/api/blog/actuator/health
```

## 참고 자료

- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/how-it-works.html)
- Portal Universe: `services/api-gateway/src/main/resources/application.yml`
