# notification-refactoring Plan

> **Feature**: notification-refactoring
> **Date**: 2026-02-02
> **Scope**: notification-service 전체 점검 및 구조적 개선
> **Level**: Dynamic (Backend + Frontend + Cross-service)

## 1. 현재 상태 진단

### 1.1 Architecture Overview

```
                     ┌─────────────────┐
                     │   API Gateway   │
                     │  (port 8080)    │
                     └────────┬────────┘
                              │ ⚠️ notification route 미등록!
                              │
┌──────────────┐    ┌─────────▼─────────┐    ┌──────────────────┐
│ auth-service │    │ notification-svc  │    │   Frontend       │
│  (8081)      │    │   (8084)          │    │  (portal-shell)  │
│              │    │                   │    │                  │
│ Kafka:       │    │ Kafka Consumer:   │    │ ⚠️ 알림 UI 없음  │
│ user-signup ─┼───►│ user-signup       │    │ ⚠️ WebSocket 미연결│
│              │    │ order.created     │    └──────────────────┘
└──────────────┘    │ delivery.shipped  │
                    │ payment.completed │
┌──────────────┐    │ coupon.issued  ⚠️ │
│ shopping-svc │    │ timedeal.started⚠️│
│  (8083)      │    │                   │
│              │    │ WebSocket(STOMP)  │
│ Kafka:       │    │ Redis Pub/Sub    │
│ order.created┼───►│                   │
│ order.confirm┼─X  │ REST API:         │
│ order.cancel ┼─X  │ /api/v1/notif...  │
│ payment.done ┼───►│                   │
│ payment.fail ┼─X  │ MySQL + Flyway   │
│ delivery.ship┼───►│                   │
│ inv.reserved ┼─X  └───────────────────┘
│              │
│ ⚠️ coupon.issued  미발행!  │
│ ⚠️ timedeal.started 미발행!│
└──────────────┘

┌──────────────┐
│ blog-service │
│  (8082)      │
│ ⚠️ Kafka 미사용 │
│ → 알림 이벤트 없음│
└──────────────┘
```

### 1.2 Quality Score: 62/100

### 1.3 핵심 문제 분류

#### A. 끊어진 연결 (Cross-service Gaps)

| # | Issue | Severity | Detail |
|---|-------|----------|--------|
| A1 | **Gateway route 미등록** | Critical | 문서에 `/api/v1/notifications/**` route 명시되어 있으나 실제 `application.yml` routes에 미등록. Frontend에서 REST API 접근 불가 |
| A2 | **Shopping 이벤트 3개 미연결** | High | shopping-service가 `order.confirmed`, `order.cancelled`, `payment.failed` 발행하지만 notification consumer에 handler 없음. NotificationType enum에는 해당 타입 정의됨 |
| A3 | **Shopping 이벤트 2개 미발행** | High | notification consumer가 `shopping.coupon.issued`, `shopping.timedeal.started` 구독하지만 shopping-service에서 해당 토픽 발행 코드 없음 (dead consumer) |
| A4 | **Blog 이벤트 완전 부재** | Medium | blog-service에 Kafka producer 없음. 댓글 알림, 좋아요 알림, 시리즈 업데이트 등 블로그 알림 기능 불가 |
| A5 | **Frontend 알림 UI 없음** | High | portal-shell에 알림 벨 아이콘, 알림 패널, WebSocket 연결 코드 없음. 백엔드만 존재하고 사용자에게 전달되지 않음 |

#### B. Dead Code & Unused

| # | Item | Location | Detail |
|---|------|----------|--------|
| B1 | `handleUserSignup` stub | `NotificationConsumer.java:22-26` | 로그만 찍고 알림 생성하지 않음 |
| B2 | `NotificationType.defaultMessage` | `NotificationType.java:35` | 한국어 기본 메시지 정의되었으나 어디서도 호출 안 됨 |
| B3 | `NOTIFICATION_SEND_FAILED` (N002) | `NotificationErrorCode.java:22` | throw하는 코드 없음 |
| B4 | `INVALID_NOTIFICATION_TYPE` (N003) | `NotificationErrorCode.java:27` | throw하는 코드 없음 |
| B5 | `pushToAll()` broadcast | `NotificationPushService.java:43-46` | 호출하는 코드 없음 |
| B6 | Feign Client 설정 | `application-*.yml` 3개 파일 | notification-service는 다른 서비스를 호출하지 않음 |
| B7 | Spring Cloud BOM | `build.gradle:27-30, 86-90` | Spring Cloud 의존성 실사용 없음 |
| B8 | `coupon.issued` consumer | `NotificationConsumer.java:46` | shopping-service에서 발행하지 않는 토픽 구독 |
| B9 | `timedeal.started` consumer | `NotificationConsumer.java:52` | shopping-service에서 발행하지 않는 토픽 구독 |

#### C. Hardcoded Values

| # | File | Line | Value | Should Be |
|---|------|------|-------|-----------|
| C1 | `KafkaConsumerConfig` | 58-59 | retry interval `1000L`, max `3L` | application.yml 설정값 |
| C2 | `NotificationConsumer` | 22-52 | Topic name 6개 하드코딩 | 상수 클래스 또는 설정값 |
| C3 | `NotificationConsumer` | 22 | Group ID `"notification-group"` | application.yml과 이중 관리 |
| C4 | `WebSocketConfig` | 25-26 | endpoint, CORS `"*"` | 환경변수 기반 |
| C5 | `NotificationRedisSubscriber` | 22 | Redis channel prefix `"notification:"` | 상수 또는 설정값 |
| C6 | `NotificationPushService` | 28, 44 | WebSocket destination path | 상수 클래스 |

#### D. Code Quality

| # | Issue | Severity | Detail |
|---|-------|----------|--------|
| D1 | **Consumer handler 5중 복사** | Medium | `handleOrderCreated` 등 5개 메서드가 `log + createAndPush(event)` 완전 동일 패턴 |
| D2 | **create() 파라미터 7개** | Medium | `NotificationService.create()` 파라미터가 과다. Command DTO 필요 |
| D3 | **Event validation 없음** | High | Kafka consumer에서 userId/type/title/message null 체크 없이 DB 저장 |
| D4 | **Idempotency 미구현** | High | 동일 이벤트 재처리 시 중복 알림 생성됨. `referenceId + referenceType` unique 체크 필요 |
| D5 | **delete() silent success** | Low | 존재하지 않는 ID 삭제 시 예외 없이 성공 |
| D6 | **Entity `@AllArgsConstructor`** | Low | `@Builder`로 충분, public all-args constructor 불필요 |
| D7 | **`createdAt` timing** | Low | `@Builder.Default` 대신 `@PrePersist` 권장 |

#### E. Security

| # | Issue | Severity | Detail |
|---|-------|----------|--------|
| E1 | **WebSocket CORS wildcard** | Critical | `setAllowedOriginPatterns("*")` - 모든 origin 허용 |
| E2 | **trusted.packages `"*"`** | Critical | application-local/docker.yml에서 모든 Java 패키지 역직렬화 허용 (RCE 취약점) |
| E3 | **X-User-Id 미검증** | Medium | 헤더값 범위 검증 없음, 직접 접근 시 임의 userId 설정 가능 |

#### F. Missing Capabilities

| # | Item | Detail |
|---|------|--------|
| F1 | **테스트 코드 전무** | `src/test/` 디렉토리 자체가 없음 |
| F2 | **DLQ consumer 없음** | `.DLT` 토픽에 쌓인 실패 메시지 처리 로직 없음 |
| F3 | **logback 중복** | Docker/K8s 프로파일 appender 설정 60줄 중복 |
| F4 | **ObjectMapper 충돌 가능** | `redisObjectMapper` Bean이 Spring 기본 ObjectMapper를 override할 수 있음 |

---

## 2. 개선 계획 (Phase별)

### Phase 1: Critical Security & Dead Code 정리

**목표**: 보안 취약점 해결 + dead code 제거로 코드베이스 정화

| # | Task | Files |
|---|------|-------|
| 1-1 | `trusted.packages` 제한 (`"*"` → `"com.portal.universe.*"`) | `application-local.yml`, `application-docker.yml`, `application-kubernetes.yml` |
| 1-2 | WebSocket CORS 환경변수화 | `WebSocketConfig.java` |
| 1-3 | 미사용 Feign 설정 제거 | `application-*.yml` 3개 |
| 1-4 | Spring Cloud BOM 제거 | `build.gradle` |
| 1-5 | Dead consumer 제거 (`coupon.issued`, `timedeal.started`) | `NotificationConsumer.java` |
| 1-6 | `handleUserSignup` stub → 실제 welcome 알림 구현 또는 제거 | `NotificationConsumer.java` |
| 1-7 | `pushToAll()` dead method 제거 | `NotificationPushService.java` |
| 1-8 | `@AllArgsConstructor` 제거 | `Notification.java` |

### Phase 2: Code Quality 개선

**목표**: 코드 복잡도 감소, DRY 원칙, hardcoding 제거

| # | Task | Files |
|---|------|-------|
| 2-1 | Topic name 상수화 (`NotificationTopics.java`) | 신규 + `NotificationConsumer.java` |
| 2-2 | Consumer handler 통합 (5중 복사 → 단일 generic) | `NotificationConsumer.java` |
| 2-3 | `create()` 파라미터 축소 → `CreateNotificationCommand` DTO | `NotificationService.java`, `NotificationServiceImpl.java` |
| 2-4 | Event validation 추가 (userId, type, title 필수 체크) | `NotificationConsumer.java` 또는 신규 validator |
| 2-5 | Idempotency: `referenceId + referenceType` unique 체크 | `NotificationRepository.java`, `NotificationServiceImpl.java` |
| 2-6 | Hardcoded retry/WebSocket 상수 → 설정값 이동 | `KafkaConsumerConfig.java`, `NotificationPushService.java` |
| 2-7 | `defaultMessage` 활용 (message null일 때 fallback) | `NotificationServiceImpl.java` |
| 2-8 | `redisObjectMapper` → `@Qualifier` 명시 | `RedisConfig.java` |
| 2-9 | logback-spring.xml 중복 제거 | `logback-spring.xml` |
| 2-10 | `createdAt` → `@PrePersist` 전환 | `Notification.java` |

### Phase 3: 끊어진 연결 복구

**목표**: Shopping 이벤트 완전 연결, Gateway route 등록

| # | Task | Files | Cross-service |
|---|------|-------|---------------|
| 3-1 | Gateway에 notification REST route 등록 | `api-gateway/.../application.yml` | api-gateway |
| 3-2 | `order.confirmed` consumer 추가 | `NotificationConsumer.java` | - |
| 3-3 | `order.cancelled` consumer 추가 | `NotificationConsumer.java` | - |
| 3-4 | `payment.failed` consumer 추가 | `NotificationConsumer.java` | - |
| 3-5 | Shopping coupon 이벤트 발행 구현 | `shopping-service` 쿠폰 서비스 | shopping-service |
| 3-6 | Shopping timedeal 이벤트 발행 구현 | `shopping-service` 타임딜 서비스 | shopping-service |
| 3-7 | consumer `coupon.issued`, `timedeal.started` 복원 (Phase 1에서 제거 후) | `NotificationConsumer.java` | - |

### Phase 4: Frontend 알림 UI (별도 PDCA 권장)

> **Note**: 이 Phase는 scope이 크므로 별도 `notification-frontend` PDCA로 분리 권장

| # | Task | Detail |
|---|------|--------|
| 4-1 | portal-shell에 WebSocket 연결 서비스 | STOMP client 연결, 자동 재연결 |
| 4-2 | 알림 벨 아이콘 + 읽지 않은 개수 badge | 사이드바에 추가 |
| 4-3 | 알림 드롭다운/패널 | 알림 목록, 읽음 처리, 전체 읽음 |
| 4-4 | 알림 상세 페이지 | 전체 알림 목록, 페이징, 필터링 |
| 4-5 | Toast/Push 알림 | WebSocket 수신 시 실시간 토스트 |

### Phase 5: Blog 이벤트 연동 (별도 PDCA 권장)

> **Note**: blog-service에 Kafka producer 추가 필요. 별도 `blog-notification` PDCA로 분리 권장

| # | Event | Trigger |
|---|-------|---------|
| 5-1 | `blog.comment.created` | 내 게시글에 댓글 |
| 5-2 | `blog.like.toggled` | 내 게시글에 좋아요 |
| 5-3 | `blog.post.published` | 팔로우 작가 새 글 |

### Phase 6: 테스트 작성 (모든 Phase와 병행)

| # | Test Type | Target |
|---|-----------|--------|
| 6-1 | Unit Test | `NotificationServiceImpl` |
| 6-2 | Unit Test | `NotificationConsumer` (event validation, idempotency) |
| 6-3 | Integration Test | `NotificationController` (MockMvc) |
| 6-4 | Kafka Test | Consumer + DLQ 동작 검증 |

---

## 3. 우선순위 및 실행 전략

### 이번 PDCA 범위 (Phase 1 + 2 + 3)

```
Phase 1 (Security + Dead Code)     → 기반 정리
Phase 2 (Code Quality)             → 구조 개선
Phase 3 (Cross-service 연결 복구)   → 기능 완성
Phase 6 (테스트)                    → 품질 보증
```

### 별도 PDCA (후속)

- `notification-frontend`: Phase 4 (Frontend 알림 UI)
- `blog-notification`: Phase 5 (Blog 이벤트 연동)

---

## 4. 핵심 원칙

| 원칙 | 적용 |
|------|------|
| **하드코딩 금지** | 모든 상수/설정값을 설정 파일 또는 상수 클래스로 이동 |
| **땜질 금지** | dead code는 주석이 아닌 삭제, stub은 구현 또는 제거 |
| **구조적 접근** | Consumer 중복 → generic handler, 파라미터 과다 → Command DTO |
| **원인-결과 추적** | 각 변경에 대해 "왜" 필요한지 명확히 문서화 |
| **Backward compatible** | Gateway route 추가, consumer 추가는 기존 동작에 영향 없음 |

---

## 5. 성공 기준

| Metric | Target |
|--------|--------|
| Quality Score | 62 → 85+ |
| Dead code items | 9 → 0 |
| Hardcoded values | 6 → 0 |
| Security issues | 3 → 0 |
| 끊어진 이벤트 연결 | 5 → 0 |
| Gateway route | 미등록 → 등록 완료 |
| Test coverage | 0% → 주요 로직 커버 |
| Consumer idempotency | 없음 → 구현 |

---

## 6. 영향받는 서비스

| Service | 변경 사항 |
|---------|----------|
| **notification-service** | 전면 리팩토링 (Phase 1-2-3) |
| **api-gateway** | notification route 등록 (Phase 3-1) |
| **shopping-service** | coupon/timedeal 이벤트 발행 추가 (Phase 3-5, 3-6) |
| **common-library** | 필요시 새 이벤트 DTO 추가 |
