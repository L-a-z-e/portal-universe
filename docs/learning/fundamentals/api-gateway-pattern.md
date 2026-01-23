# API Gateway Pattern

마이크로서비스 아키텍처에서 API Gateway의 역할, 핵심 기능, 그리고 Portal Universe의 실제 구현을 학습합니다.

---

## 목차

1. [API Gateway 개요](#1-api-gateway-개요)
2. [핵심 기능](#2-핵심-기능)
3. [API Gateway 패턴 유형](#3-api-gateway-패턴-유형)
4. [Portal Universe Spring Cloud Gateway 분석](#4-portal-universe-spring-cloud-gateway-분석)
5. [실제 코드 예시](#5-실제-코드-예시)
6. [모범 사례 및 주의사항](#6-모범-사례-및-주의사항)
7. [관련 문서](#7-관련-문서)

---

## 1. API Gateway 개요

### 1.1 정의

API Gateway는 마이크로서비스 아키텍처의 **단일 진입점(Single Entry Point)**으로, 클라이언트와 백엔드 서비스 사이에서 요청을 라우팅하고 관리하는 역할을 합니다.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client Applications                           │
│                 (Web Browser, Mobile App, External API)                 │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            API Gateway                                  │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌──────────┐ ┌─────────────┐  │
│  │ Routing │ │   Auth   │ │Rate Limit  │ │  Logging │ │Circuit Break│  │
│  └─────────┘ └──────────┘ └────────────┘ └──────────┘ └─────────────┘  │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Auth Service   │     │  Blog Service   │     │Shopping Service │
│   (Port 8081)   │     │   (Port 8082)   │     │   (Port 8083)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### 1.2 왜 API Gateway가 필요한가?

#### 문제: API Gateway 없이 직접 통신

```
┌─────────┐    ┌─────────────────┐
│ Client  │───▶│  Auth Service   │  문제점:
│         │    └─────────────────┘  - 클라이언트가 모든 서비스 URL을 알아야 함
│         │───▶┌─────────────────┐  - 각 서비스마다 인증 로직 중복
│         │    │  Blog Service   │  - CORS 설정 분산
│         │    └─────────────────┘  - 클라이언트-서비스 강결합
│         │───▶┌─────────────────┐  - 프로토콜 변환 어려움
│         │    │Shopping Service │
└─────────┘    └─────────────────┘
```

#### 해결: API Gateway를 통한 통합 진입점

```
┌─────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Client  │───▶│   API Gateway   │───▶│  Auth Service   │
│         │    │                 │───▶│  Blog Service   │
│         │    │ - 단일 URL      │───▶│Shopping Service │
│         │    │ - 통합 인증     │    └─────────────────┘
│         │    │ - 요청 라우팅   │
└─────────┘    └─────────────────┘
```

### 1.3 주요 장점

| 장점 | 설명 |
|------|------|
| **단순화된 클라이언트** | 클라이언트는 Gateway URL만 알면 됨 |
| **보안 강화** | 인증/인가를 Gateway에서 중앙 집중 처리 |
| **Cross-Cutting Concerns** | 로깅, 모니터링, Rate Limiting 일괄 적용 |
| **프로토콜 변환** | 외부(HTTP) → 내부(gRPC, WebSocket) 변환 가능 |
| **서비스 추상화** | 내부 서비스 구조 변경이 클라이언트에 영향 없음 |
| **API 집계** | 여러 서비스 응답을 조합하여 단일 응답 생성 |

---

## 2. 핵심 기능

### 2.1 라우팅 (Routing)

클라이언트 요청을 적절한 백엔드 서비스로 전달하는 기능입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Routing Rules                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  /api/auth/**     ──────────────▶  Auth Service (8081)          │
│                                                                 │
│  /api/blog/**     ──────────────▶  Blog Service (8082)          │
│                                                                 │
│  /api/shopping/** ──────────────▶  Shopping Service (8083)      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**라우팅 유형:**
- **Path-based**: URL 경로로 라우팅 (가장 일반적)
- **Host-based**: 도메인으로 라우팅 (`api.blog.com` → Blog Service)
- **Header-based**: 특정 헤더 값으로 라우팅 (A/B 테스트)
- **Weight-based**: 가중치 기반 (Canary 배포)

### 2.2 인증 및 인가 (Authentication & Authorization)

```
┌───────────────────────────────────────────────────────────────────────┐
│                      Authentication Flow                              │
└───────────────────────────────────────────────────────────────────────┘

┌─────────┐  1. Request + JWT   ┌─────────────────┐
│ Client  │────────────────────▶│   API Gateway   │
└─────────┘                     │                 │
                                │ 2. Validate JWT │
     ┌──────────────────────────│    (HMAC)       │
     │                          └────────┬────────┘
     │                                   │
     │ 4. Response                       │ 3. Forward + X-User-Id
     │                                   ▼
     │                          ┌─────────────────┐
     └──────────────────────────│ Backend Service │
                                └─────────────────┘
```

**인증 처리 방식:**

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **Token Relay** | JWT를 검증 후 그대로 전달 | 서비스가 사용자 정보 직접 접근 | 토큰 크기 증가 |
| **Token Exchange** | Gateway가 내부 토큰으로 변환 | 외부/내부 토큰 분리 | 복잡도 증가 |
| **Header Enrichment** | 검증 후 사용자 정보를 헤더에 추가 | 백엔드 서비스 단순화 | 헤더 신뢰 필요 |

### 2.3 Rate Limiting

서비스 보호를 위한 요청 제한 기능입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Rate Limiting Algorithms                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Token Bucket (토큰 버킷)                                         │
│     ┌─────────────────────────────────┐                             │
│     │  Bucket: [●●●●●○○○○○]           │  ● = Token available        │
│     │  Refill: 10 tokens/second       │  ○ = Empty slot             │
│     │  Request consumes 1 token       │                             │
│     └─────────────────────────────────┘                             │
│                                                                     │
│  2. Leaky Bucket (누출 버킷)                                         │
│     ┌─────────────────────────────────┐                             │
│     │  ▼ Incoming (variable rate)     │                             │
│     │  │  ┌─────────────┐             │                             │
│     │  └─▶│   Bucket    │             │                             │
│     │     └──────┬──────┘             │                             │
│     │            │ Outgoing (fixed)   │                             │
│     │            ▼                    │                             │
│     └─────────────────────────────────┘                             │
│                                                                     │
│  3. Fixed Window (고정 윈도우)                                       │
│     ┌─────────────────────────────────┐                             │
│     │  [Window 1: 100 req] [Window 2: 100 req]                      │
│     │  |-----1분-----|-----1분-----|                                 │
│     └─────────────────────────────────┘                             │
│                                                                     │
│  4. Sliding Window (슬라이딩 윈도우)                                  │
│     ┌─────────────────────────────────┐                             │
│     │        ╔═══════════════╗        │  현재 시점 기준으로           │
│     │  ──────╫───────────────╫──────  │  윈도우가 이동               │
│     │        ╚═══════════════╝        │                             │
│     └─────────────────────────────────┘                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.4 Load Balancing

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Load Balancing Strategies                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Round Robin          Weighted Round Robin      Least Connections   │
│  ┌─────────────┐      ┌─────────────────┐      ┌─────────────────┐  │
│  │ Req1 → S1   │      │ S1 (w=3): ●●●   │      │ S1: 5 conns     │  │
│  │ Req2 → S2   │      │ S2 (w=1): ●     │      │ S2: 2 conns ◀── │  │
│  │ Req3 → S3   │      │ S3 (w=2): ●●    │      │ S3: 8 conns     │  │
│  │ Req4 → S1   │      │                 │      │                 │  │
│  └─────────────┘      └─────────────────┘      └─────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.5 Circuit Breaker

장애 서비스로부터 시스템을 보호하는 패턴입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Circuit Breaker State Machine                     │
└─────────────────────────────────────────────────────────────────────┘

                          실패율 < 임계값
                    ┌─────────────────────┐
                    │                     │
                    ▼                     │
              ┌───────────┐               │
              │  CLOSED   │───────────────┘
              │ (정상 상태)│
              └─────┬─────┘
                    │
                    │ 실패율 >= 임계값 (50%)
                    ▼
              ┌───────────┐
              │   OPEN    │ ──────────────────▶ 즉시 실패 응답
              │ (차단 상태)│                     (Fallback)
              └─────┬─────┘
                    │
                    │ 대기 시간 경과 (10초)
                    ▼
              ┌───────────┐
              │ HALF-OPEN │ ──────────────────▶ 일부 요청만 허용
              │(테스트 상태)│                     (5개)
              └─────┬─────┘
                    │
        ┌───────────┴───────────┐
        │                       │
   성공 비율 충족            실패 발생
        │                       │
        ▼                       ▼
   ┌─────────┐            ┌─────────┐
   │ CLOSED  │            │  OPEN   │
   └─────────┘            └─────────┘
```

---

## 3. API Gateway 패턴 유형

### 3.1 Edge Gateway (기본 패턴)

모든 외부 트래픽의 단일 진입점으로 동작합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Edge Gateway Pattern                        │
└─────────────────────────────────────────────────────────────────────┘

┌──────────┐     ┌──────────┐     ┌──────────┐
│   Web    │     │  Mobile  │     │ Partner  │
│  Client  │     │   App    │     │   API    │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │
     └────────────────┼────────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  Edge Gateway │  ← 모든 클라이언트가 동일한 Gateway 사용
              │               │
              │ - Auth/AuthZ  │
              │ - Rate Limit  │
              │ - Logging     │
              └───────┬───────┘
                      │
       ┌──────────────┼──────────────┐
       │              │              │
       ▼              ▼              ▼
  [Service A]    [Service B]    [Service C]
```

**특징:**
- 간단한 구조
- 모든 클라이언트가 동일한 API 사용
- 클라이언트별 최적화 어려움

### 3.2 Backend for Frontend (BFF) 패턴

클라이언트 유형별로 최적화된 Gateway를 제공합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BFF Pattern                                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────┐              ┌──────────┐              ┌──────────┐
│   Web    │              │  Mobile  │              │ Partner  │
│  Client  │              │   App    │              │   API    │
└────┬─────┘              └────┬─────┘              └────┬─────┘
     │                         │                         │
     ▼                         ▼                         ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Web BFF    │         │ Mobile BFF  │         │Partner BFF  │
│             │         │             │         │             │
│- 풍부한 UI  │         │- 최소 데이터│         │- 표준 REST  │
│- SSR 지원   │         │- 오프라인   │         │- API 버저닝 │
│- WebSocket  │         │- 푸시 알림  │         │- Rate Limit │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       └───────────────────────┼───────────────────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
           [Service A]    [Service B]    [Service C]
```

**장점:**
- 클라이언트별 최적화된 API
- 독립적인 배포 및 스케일링
- 관심사 분리

**단점:**
- 코드 중복 가능성
- 관리 복잡도 증가

### 3.3 Gateway Aggregation 패턴

여러 서비스 응답을 조합하여 단일 응답을 생성합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Gateway Aggregation Pattern                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────┐  GET /dashboard
│ Client  │──────────────────────▶┌─────────────────────┐
└─────────┘                       │    API Gateway      │
                                  │    (Aggregator)     │
                                  └──────────┬──────────┘
                                             │
                  ┌──────────────────────────┼──────────────────────────┐
                  │ 병렬 호출                │                          │
                  │                          │                          │
                  ▼                          ▼                          ▼
           ┌─────────────┐           ┌─────────────┐           ┌─────────────┐
           │User Service │           │Order Service│           │Stats Service│
           │             │           │             │           │             │
           │GET /users/1 │           │GET /orders  │           │GET /stats   │
           └──────┬──────┘           └──────┬──────┘           └──────┬──────┘
                  │                         │                         │
                  └─────────────────────────┼─────────────────────────┘
                                            │
                                            ▼
                                  ┌─────────────────────┐
                                  │   응답 조합         │
                                  │  {                  │
                                  │    user: {...},     │
                                  │    orders: [...],   │
                                  │    stats: {...}     │
                                  │  }                  │
                                  └─────────────────────┘
```

### 3.4 Gateway Offloading 패턴

공통 기능을 Gateway로 오프로딩하여 서비스를 단순화합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Gateway Offloading Pattern                        │
└─────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │           API Gateway               │
                    │                                     │
 오프로딩 기능 ────▶│  ✓ SSL/TLS Termination             │
                    │  ✓ Authentication                   │
                    │  ✓ IP Whitelisting                  │
                    │  ✓ Rate Limiting                    │
                    │  ✓ Logging & Monitoring             │
                    │  ✓ Response Caching                 │
                    │  ✓ Response Compression (gzip)      │
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       │
                                       │ HTTP (내부 통신)
                                       ▼
                    ┌─────────────────────────────────────┐
                    │        Backend Services             │
                    │                                     │
 서비스는 핵심     │  ✓ 비즈니스 로직에만 집중            │
 로직에만 집중 ───▶│  ✓ 단순한 구조                       │
                    │  ✓ 빠른 개발                        │
                    │                                     │
                    └─────────────────────────────────────┘
```

---

## 4. Portal Universe Spring Cloud Gateway 분석

### 4.1 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Portal Universe Gateway Architecture             │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│  Portal Shell    │  (Vue 3, :30000)
│  (Frontend Host) │
└────────┬─────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                      API Gateway (:8080)                           │
│                                                                    │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐       │
│  │ GlobalLogging  │  │   CORS Filter  │  │    JWT Auth    │       │
│  │    Filter      │  │                │  │    Filter      │       │
│  └────────────────┘  └────────────────┘  └────────────────┘       │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    Route Configuration                       │ │
│  │  /api/auth/**     → Auth Service (:8081)                     │ │
│  │  /api/blog/**     → Blog Service (:8082)                     │ │
│  │  /api/shopping/** → Shopping Service (:8083)                 │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                   Resilience4j Circuit Breaker               │ │
│  │  authCircuitBreaker | blogCircuitBreaker | shoppingCircuit   │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
└────────────────────────────┬───────────────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
   │Auth Service │    │Blog Service │    │  Shopping   │
   │   (:8081)   │    │   (:8082)   │    │  Service    │
   │             │    │             │    │   (:8083)   │
   │- JWT 발급   │    │- MongoDB    │    │- MySQL      │
   │- OAuth2     │    │- 검색(ES)   │    │- Redis Lock │
   └─────────────┘    └─────────────┘    └─────────────┘
```

### 4.2 Filter Chain 실행 순서

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Filter Chain Flow                              │
└─────────────────────────────────────────────────────────────────────┘

  Request                                                    Response
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 1. RequestPathLoggingFilter (HIGHEST_PRECEDENCE)                  │
│    - 모든 요청 경로와 메서드를 DEBUG 로깅                          │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 2. CorsWebFilter (HIGHEST_PRECEDENCE + 1)                         │
│    - CORS Preflight(OPTIONS) 요청 처리                            │
│    - Access-Control-* 헤더 추가                                   │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 3. GlobalLoggingFilter (HIGHEST_PRECEDENCE + 2)                   │
│    - 요청/응답 INFO 로깅                                          │
│    - 처리 시간 측정                                               │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 4. JwtAuthenticationFilter (AUTHENTICATION)                       │
│    - JWT 토큰 추출 및 검증                                        │
│    - X-User-Id, X-User-Roles 헤더 추가                            │
│    - SecurityContext에 인증 정보 설정                              │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 5. SecurityWebFilterChain                                         │
│    - 경로별 접근 제어 (permitAll / authenticated / hasRole)        │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 6. Gateway Filters (Route-specific)                               │
│    - StripPrefix: /api/blog/** → /**                             │
│    - RewritePath: 경로 재작성                                     │
│    - CircuitBreaker: 장애 격리                                    │
└───────────────────────────────────────────────────────────────────┘
    │                                                            ▲
    ▼                                                            │
┌───────────────────────────────────────────────────────────────────┐
│ 7. Backend Service                                                │
└───────────────────────────────────────────────────────────────────┘
```

### 4.3 경로별 접근 제어 정책

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Access Control Matrix                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ PUBLIC (인증 불필요)                                         │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ /auth-service/**        OAuth2 로그인 플로우                 │   │
│  │ /api/auth/**            인증 API (로그인, 토큰 갱신)         │   │
│  │ /api/users/**           회원가입, 프로필 조회                │   │
│  │ /api/blog/** (GET)      블로그 글 조회                       │   │
│  │ /api/shopping/products  상품 목록/상세 조회                  │   │
│  │ /api/shopping/categories 카테고리 조회                       │   │
│  │ /actuator/**            Health Check (모니터링)              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ ADMIN ONLY (관리자 권한 필요)                                │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ /api/shopping/admin/**  관리자 전용 API                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ AUTHENTICATED (로그인 필요)                                  │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ 그 외 모든 경로                                              │   │
│  │ - /api/blog/** (POST, PUT, DELETE)                          │   │
│  │ - /api/shopping/cart/**                                     │   │
│  │ - /api/shopping/orders/**                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. 실제 코드 예시

### 5.1 Route Configuration (application.yml)

```yaml
spring:
  cloud:
    gateway:
      # 전역 필터
      default-filters:
        - name: PreserveHostHeader

      # X-Forwarded 헤더 활성화
      x-forwarded:
        enabled: true

      routes:
        # ========== Auth Service ==========
        # OAuth2 소셜 로그인 시작
        - id: auth-service-oauth2-authorization
          uri: ${services.auth.url}
          predicates:
            - Path=/auth-service/oauth2/authorization/**
          filters:
            - StripPrefix=1
          order: 0

        # OAuth2 콜백
        - id: auth-service-oauth2-callback
          uri: ${services.auth.url}
          predicates:
            - Path=/auth-service/login/oauth2/code/**
          filters:
            - StripPrefix=1
          order: 1

        # Auth REST API
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

        # ========== Blog Service ==========
        # 파일 업로드 (큰 요청 크기 허용)
        - id: blog-service-file-route
          uri: ${services.blog.url}
          predicates:
            - Path=/api/blog/file/**
          filters:
            - StripPrefix=2    # /api/blog 제거
            - name: CircuitBreaker
              args:
                name: blogCircuitBreaker
                fallbackUri: forward:/fallback/blog
            - name: RequestSize
              args:
                max-request-size: 100MB
          order: 1

        # Blog 일반 API
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

        # ========== Shopping Service ==========
        - id: shopping-service-route
          uri: ${services.shopping.url}
          predicates:
            - Path=/api/shopping/**
          filters:
            - StripPrefix=2
            - name: CircuitBreaker
              args:
                name: shoppingCircuitBreaker
                fallbackUri: forward:/fallback/shopping
```

### 5.2 JWT Authentication Filter

```java
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtConfig jwtConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 공개 경로는 JWT 검증 생략
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // 토큰이 없으면 SecurityConfig의 접근 제어에 위임
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // JWT 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            log.debug("JWT validated for user: {}, roles: {}", userId, roles);

            // X-User-Id 헤더 추가 (하위 서비스에서 사용)
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles != null ? roles : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? List.of(new SimpleGrantedAuthority(roles))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth-service/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/users/") ||
               path.startsWith("/actuator/") ||
               path.equals("/api/shopping/products") ||
               path.startsWith("/api/shopping/products/");
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
```

### 5.3 Security Configuration

```java
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtConfig);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:30000",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Preflight 캐시 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authorize -> authorize
                        // 공개 경로
                        .pathMatchers("/auth-service/**", "/api/auth/**", "/api/users/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                        .pathMatchers("/api/shopping/products", "/api/shopping/products/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        // 관리자 전용
                        .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
```

### 5.4 Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: count_based
        sliding-window-size: 20          # 최근 20개 요청 기준
        failure-rate-threshold: 50       # 실패율 50% 이상 시 OPEN
        wait-duration-in-open-state: 10s # OPEN 상태 유지 시간
        permitted-number-of-calls-in-half-open-state: 5  # HALF-OPEN에서 테스트 요청 수
        automatic-transition-from-open-to-half-open-enabled: true

    instances:
      authCircuitBreaker:
        base-config: default
      blogCircuitBreaker:
        base-config: default
      shoppingCircuitBreaker:
        base-config: default

  timelimiter:
    configs:
      default:
        timeout-duration: 5s  # 타임아웃 5초
    instances:
      authCircuitBreaker:
        base-config: default
```

### 5.5 Fallback Controller

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/auth")
    public Mono<Map<String, Object>> authServiceFallback() {
        return Mono.just(Map.of(
            "success", false,
            "error", Map.of(
                "code", "AUTH_SERVICE_UNAVAILABLE",
                "message", "인증 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요."
            )
        ));
    }

    @GetMapping("/fallback/blog")
    public Mono<Map<String, Object>> blogServiceFallback() {
        return Mono.just(Map.of(
            "success", false,
            "error", Map.of(
                "code", "BLOG_SERVICE_UNAVAILABLE",
                "message", "블로그 서비스가 일시적으로 사용 불가합니다."
            )
        ));
    }

    @GetMapping("/fallback/shopping")
    public Mono<Map<String, Object>> shoppingServiceFallback() {
        return Mono.just(Map.of(
            "success", false,
            "error", Map.of(
                "code", "SHOPPING_SERVICE_UNAVAILABLE",
                "message", "쇼핑 서비스가 일시적으로 사용 불가합니다."
            )
        ));
    }
}
```

### 5.6 Global Logging Filter

```java
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class GlobalLoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestPath = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request, exchange);

        log.info("API_REQUEST - Method: {}, Path: {}, IP: {}",
                method, requestPath, clientIp);

        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("API_RESPONSE - Path: {}, Status: {}, Duration: {}ms",
                            requestPath, response.getStatusCode(), duration);
                })
        );
    }

    private String getClientIp(ServerHttpRequest request, ServerWebExchange exchange) {
        XForwardedRemoteAddressResolver resolver =
            XForwardedRemoteAddressResolver.maxTrustedIndex(1);
        InetSocketAddress remoteAddress = resolver.resolve(exchange);

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "Unknown";
    }
}
```

---

## 6. 모범 사례 및 주의사항

### 6.1 모범 사례

#### Route 설계

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Route Design Best Practices                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ✓ DO                                                               │
│  ─────────────────────────────────────────────────────────────────  │
│  • 구체적인 경로를 먼저 정의 (order 값으로 우선순위 제어)            │
│  • 서비스별 일관된 prefix 사용 (/api/auth, /api/blog)               │
│  • Actuator 경로는 별도로 분리하여 관리                             │
│  • 파일 업로드 등 특수 경로는 RequestSize 설정 추가                 │
│                                                                     │
│  ✗ DON'T                                                            │
│  ─────────────────────────────────────────────────────────────────  │
│  • 와일드카드 라우트를 상위에 배치 (다른 라우트를 덮어씀)           │
│  • 하드코딩된 서비스 URL 사용 (환경 변수 활용)                      │
│  • 민감한 내부 경로 외부 노출                                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 보안 설계

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Security Best Practices                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Defense in Depth (심층 방어)                                    │
│     ┌─────────────────────────────────────────────────────────┐    │
│     │  Gateway    │  인증, Rate Limit, CORS                    │    │
│     │  Service    │  비즈니스 권한 검증, 입력 검증              │    │
│     │  Database   │  Row-level Security, Encryption            │    │
│     └─────────────────────────────────────────────────────────┘    │
│                                                                     │
│  2. Zero Trust (제로 트러스트)                                      │
│     - 내부 서비스 간 통신도 검증                                    │
│     - X-User-Id 헤더만 신뢰하지 말고 서비스에서도 재검증            │
│                                                                     │
│  3. Secret 관리                                                     │
│     - JWT Secret Key는 환경 변수로 주입                             │
│     - 모든 서비스가 동일한 키를 사용해야 함                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Circuit Breaker 설계

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Circuit Breaker Best Practices                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 서비스별 독립 Circuit Breaker                                   │
│     - authCircuitBreaker, blogCircuitBreaker 등                    │
│     - 한 서비스 장애가 다른 서비스에 영향 X                         │
│                                                                     │
│  2. 적절한 임계값 설정                                              │
│     - 너무 낮은 실패율: 일시적 오류에도 회로 열림                   │
│     - 너무 높은 실패율: 장애 격리 지연                              │
│     - 권장: 50% 실패율, 20개 요청 윈도우                            │
│                                                                     │
│  3. Fallback 응답 설계                                              │
│     - 사용자 친화적인 에러 메시지                                   │
│     - 캐시된 데이터 반환 (가능한 경우)                              │
│     - 부분 응답 제공 (Aggregation 패턴 시)                          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 주의사항

#### 성능 고려사항

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Performance Considerations                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ⚠️ Gateway는 모든 요청을 거치므로 SPOF(Single Point of Failure)   │
│                                                                     │
│  해결책:                                                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Load Balancer                            │   │
│  │                         │                                   │   │
│  │      ┌──────────────────┼──────────────────┐                │   │
│  │      │                  │                  │                │   │
│  │      ▼                  ▼                  ▼                │   │
│  │  [Gateway 1]       [Gateway 2]       [Gateway 3]           │   │
│  │  (Active)          (Active)          (Active)              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  • 다중 Gateway 인스턴스 배포 (최소 2개 이상)                       │
│  • Kubernetes에서 HPA(Horizontal Pod Autoscaler) 적용              │
│  • Netty 기반 비동기 처리로 높은 처리량 확보                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Anti-Patterns

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Anti-Patterns                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ❌ Fat Gateway (비대한 Gateway)                                    │
│     - Gateway에 비즈니스 로직 포함                                  │
│     - 데이터베이스 직접 접근                                        │
│     - 복잡한 데이터 변환 로직                                       │
│     → 라우팅, 인증, 공통 기능만 담당해야 함                         │
│                                                                     │
│  ❌ Single Giant Gateway                                            │
│     - 모든 서비스를 하나의 Gateway로 처리                           │
│     - 수백 개의 라우트 규칙                                         │
│     → 도메인별 BFF 또는 Gateway 분리 고려                           │
│                                                                     │
│  ❌ Synchronous Aggregation                                         │
│     - 여러 서비스 동기 호출 후 응답 조합                            │
│     - 가장 느린 서비스가 전체 응답 시간 결정                        │
│     → 병렬 호출 또는 비동기 패턴 사용                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 모니터링 및 관측성

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Observability Setup                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Metrics (메트릭)                                                │
│     - 요청 수, 응답 시간, 에러율                                    │
│     - Circuit Breaker 상태                                         │
│     - Route별 트래픽 분석                                           │
│                                                                     │
│  2. Logging (로깅)                                                  │
│     - 분산 추적 ID (traceId, spanId)                                │
│     - 요청/응답 로깅 (민감 정보 제외)                               │
│     - 에러 상세 로깅                                                │
│                                                                     │
│  3. Tracing (추적)                                                  │
│     - Zipkin/Jaeger 연동                                            │
│     - Gateway → Service 호출 추적                                   │
│     - 병목 구간 식별                                                │
│                                                                     │
│  Portal Universe 로그 패턴:                                         │
│  "%5p [${spring.application.name},%X{traceId},%X{spanId}]"         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. 관련 문서

### Portal Universe 내부 문서

| 문서 | 설명 | 경로 |
|------|------|------|
| Spring Cloud Gateway | Route, Predicate, Filter 상세 | [gateway/spring-cloud-gateway.md](../../services/api-gateway/docs/learning/gateway/spring-cloud-gateway.md) |
| Circuit Breaker | Resilience4j 상태 전이, 설정 | [gateway/circuit-breaker.md](../../services/api-gateway/docs/learning/gateway/circuit-breaker.md) |
| JWT Validation | JWT 검증, 접근 제어 상세 | [gateway/jwt-validation.md](../../services/api-gateway/docs/learning/gateway/jwt-validation.md) |
| Architecture Overview | API Gateway 시스템 개요 | [api-gateway/docs/architecture/](../../services/api-gateway/docs/architecture/system-overview.md) |
| Routing Specification | 라우팅 규칙 명세 | [api-gateway/docs/api/](../../services/api-gateway/docs/api/routing-specification.md) |

### 외부 참고 자료

| 자료 | 설명 |
|------|------|
| [Spring Cloud Gateway Docs](https://docs.spring.io/spring-cloud-gateway/reference/index.html) | 공식 문서 |
| [Resilience4j Docs](https://resilience4j.readme.io/docs/circuitbreaker) | Circuit Breaker 라이브러리 |
| [API Gateway Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/microservices/design/gateway) | 아키텍처 패턴 가이드 |
| [BFF Pattern - Sam Newman](https://samnewman.io/patterns/architectural/bff/) | BFF 패턴 원작자 설명 |

---

## 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Key Takeaways                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. API Gateway는 마이크로서비스의 단일 진입점                       │
│     - 라우팅, 인증, Rate Limiting, Circuit Breaker 담당             │
│                                                                     │
│  2. Portal Universe는 Spring Cloud Gateway + WebFlux 사용           │
│     - 비동기/논블로킹 처리로 높은 처리량                            │
│     - Resilience4j로 서비스별 장애 격리                             │
│                                                                     │
│  3. 보안은 Gateway에서 1차 검증, 서비스에서 2차 검증                 │
│     - JWT 검증 → X-User-Id 헤더 추가 → 서비스 전달                  │
│                                                                     │
│  4. Gateway는 얇게 유지                                              │
│     - Cross-Cutting Concerns만 처리                                 │
│     - 비즈니스 로직은 서비스에서 처리                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

*마지막 업데이트: 2025-01*
