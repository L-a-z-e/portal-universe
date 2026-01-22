# Spring Cloud Gateway 학습

## 학습 목표
- Spring Cloud Gateway의 아키텍처 이해
- Route, Predicate, Filter 개념 학습
- Portal Universe의 Gateway 설정 분석

---

## 1. Spring Cloud Gateway 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY (Port: 8080)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │                        REQUEST FLOW                                   │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│                                    ▼                                         │
│   ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌──────────────┐   │
│   │   CORS     │───►│   JWT      │───►│   Route    │───►│ Circuit      │   │
│   │   Filter   │    │   Filter   │    │   Match    │    │ Breaker      │   │
│   └────────────┘    └────────────┘    └────────────┘    └──────────────┘   │
│                                              │                    │          │
│                                              ▼                    ▼          │
│                                    ┌─────────────────────────────────────┐  │
│                                    │            FILTERS                   │  │
│                                    │  • StripPrefix                       │  │
│                                    │  • RewritePath                       │  │
│                                    │  • RequestSize                       │  │
│                                    └─────────────────────────────────────┘  │
│                                              │                               │
│                   ┌──────────────────────────┼──────────────────────────┐   │
│                   │                          │                          │   │
│                   ▼                          ▼                          ▼   │
│           ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│           │ Auth Service │          │ Blog Service │          │Shopping Svc  │
│           │   :8081      │          │   :8082      │          │   :8083      │
│           └──────────────┘          └──────────────┘          └──────────────┘
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 핵심 개념

### 2.1 Route

요청을 특정 서비스로 라우팅하는 규칙입니다.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: shopping-service-route      # 라우트 고유 ID
          uri: ${services.shopping.url}   # 대상 서비스 URL
          predicates:                      # 매칭 조건
            - Path=/api/shopping/**
          filters:                         # 적용할 필터
            - StripPrefix=2
            - name: CircuitBreaker
              args:
                name: shoppingCircuitBreaker
                fallbackUri: forward:/fallback/shopping
```

### 2.2 Predicate

요청을 라우트에 매칭하는 조건입니다.

| Predicate | 설명 | 예시 |
|-----------|------|------|
| `Path` | 경로 패턴 매칭 | `Path=/api/blog/**` |
| `Method` | HTTP 메서드 매칭 | `Method=GET,POST` |
| `Header` | 헤더 값 매칭 | `Header=X-Request-Id, \d+` |
| `Query` | 쿼리 파라미터 매칭 | `Query=name, .+` |
| `Host` | 호스트명 매칭 | `Host=**.portal.com` |
| `Cookie` | 쿠키 값 매칭 | `Cookie=session, .+` |

### 2.3 Filter

요청/응답을 변환하는 컴포넌트입니다.

| Filter | 설명 |
|--------|------|
| `StripPrefix` | 경로 앞부분 제거 |
| `RewritePath` | 경로 재작성 |
| `AddRequestHeader` | 요청 헤더 추가 |
| `RequestSize` | 요청 크기 제한 |
| `CircuitBreaker` | 서킷 브레이커 적용 |

---

## 3. Portal Universe 라우팅 설정

### 3.1 서비스별 라우트 맵

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ROUTING RULES                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  /auth-service/**  ──────────────────────────────────►  Auth Service        │
│  /api/auth/**      ──────────────────────────────────►  Auth Service        │
│  /api/users/**     ──────────────────────────────────►  Auth Service        │
│                                                                              │
│  /api/blog/**      ──► StripPrefix=2 ─────────────────►  Blog Service       │
│  /api/blog/file/** ──► StripPrefix=2, RequestSize=100MB ► Blog Service      │
│                                                                              │
│  /api/shopping/**  ──► StripPrefix=2 ─────────────────►  Shopping Service   │
│                                                                              │
│  /api/*/actuator/** ──► RewritePath ──────────────────►  각 서비스 Actuator │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 StripPrefix 동작

```
요청:   /api/shopping/products
        └─1─┘└───2───┘

StripPrefix=2 적용 후:
        /products

대상 서비스로 전달:
        http://shopping-service:8083/products
```

### 3.3 RewritePath 동작

```yaml
- id: auth-service-actuator
  uri: ${services.auth.url}
  predicates:
    - Path=/api/auth/actuator/**
  filters:
    - RewritePath=/api/auth/actuator/(?<segment>.*), /actuator/${segment}
```

```
요청:   /api/auth/actuator/health
재작성: /actuator/health
```

---

## 4. 환경별 서비스 URL 설정

### 4.1 application-local.yml

```yaml
services:
  auth:
    url: http://localhost:8081
  blog:
    url: http://localhost:8082
  shopping:
    url: http://localhost:8083
```

### 4.2 application-k8s.yml

```yaml
services:
  auth:
    url: http://auth-service:8081
  blog:
    url: http://blog-service:8082
  shopping:
    url: http://shopping-service:8083
```

---

## 5. 전역 필터

### 5.1 PreserveHostHeader

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: PreserveHostHeader
```

원본 Host 헤더를 유지하여 하위 서비스에 전달합니다.

### 5.2 X-Forwarded 헤더

```yaml
spring:
  cloud:
    gateway:
      x-forwarded:
        enabled: true
```

프록시 환경에서 원본 클라이언트 정보를 전달합니다:
- `X-Forwarded-For`: 클라이언트 IP
- `X-Forwarded-Proto`: 원본 프로토콜 (http/https)
- `X-Forwarded-Host`: 원본 호스트

---

## 6. Route 우선순위

```yaml
routes:
  - id: auth-service-actuator
    predicates:
      - Path=/api/auth/actuator/**
    order: 0                          # 높은 우선순위

  - id: auth-service-api
    predicates:
      - Path=/api/auth/**
    order: 5                          # 낮은 우선순위
```

| order 값 | 의미 |
|----------|------|
| 작은 값 | 높은 우선순위 (먼저 매칭) |
| 큰 값 | 낮은 우선순위 |
| 미지정 | 0 (기본값) |

---

## 7. 파일 업로드 처리

### 7.1 RequestSize 설정

```yaml
- id: blog-service-file-route
  uri: ${services.blog.url}
  predicates:
    - Path=/api/blog/file/**
  filters:
    - StripPrefix=2
    - name: RequestSize
      args:
        max-request-size: 100MB
```

### 7.2 Netty 설정

```yaml
server:
  netty:
    connection-timeout: 2s
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Route** | 요청을 서비스로 라우팅하는 규칙 |
| **Predicate** | 라우트 매칭 조건 (Path, Method 등) |
| **Filter** | 요청/응답 변환 (StripPrefix, RewritePath) |
| **default-filters** | 모든 라우트에 적용되는 전역 필터 |
| **order** | 라우트 우선순위 (낮을수록 높은 우선순위) |
| **uri** | 대상 서비스 URL (환경 변수로 관리) |

---

## 다음 학습

- [JWT 검증](./jwt-validation.md)
- [Circuit Breaker](./circuit-breaker.md)
- [Rate Limiting](./rate-limiting.md)
