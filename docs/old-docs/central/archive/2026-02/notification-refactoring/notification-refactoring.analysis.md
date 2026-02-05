# notification-refactoring Gap Analysis

> **Feature**: notification-refactoring
> **Design**: [notification-refactoring.design.md](../../02-design/features/notification-refactoring.design.md)
> **Date**: 2026-02-02
> **Match Rate**: 97.0% (32/33 items)

---

## 1. Summary

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 97.0% | Pass |
| Architecture Compliance | 100% | Pass |
| Convention Compliance | 100% | Pass |
| Test Coverage (structural) | 100% | Pass |
| **Overall** | **97.0%** | **Pass** |

---

## 2. Design Items Analysis

### Phase 1: Security + Dead Code (7 items)

| # | Design Item | Status | File:Line | Notes |
|:-:|-------------|:------:|-----------|-------|
| 1 | `trusted.packages` 제한 (`application-local.yml`) | Match | `application-local.yml:12` | `"com.portal.universe.*"` 정확히 적용 |
| 2 | `trusted.packages` 제한 (`application-docker.yml`) | Match | `application-docker.yml:16` | 동일 |
| 3 | `trusted.packages` 제한 (`application-kubernetes.yml`) | Match | `application-kubernetes.yml:16` | 동일 |
| 4 | WebSocket CORS 환경변수화 | Match | `WebSocketConfig.java:14` | `@Value` + `String[]` + 프로파일별 설정 |
| 5 | Feign/services/Spring Cloud BOM 제거 | Match | `build.gradle`, `application-*.yml` | 3개 yml + build.gradle 정리 완료 |
| 6 | `pushToAll()` 제거 + `handleUserSignup` 구현 | Match | `NotificationPushService.java`, `NotificationConsumer.java:28` | 정확히 매치 |
| 7 | `@AllArgsConstructor` 제거 | **Partial** | `Notification.java:16` | `PRIVATE` access로 변환 (완전 제거 아닌 캡슐화) |

### Phase 2: Code Quality (15 items)

| # | Design Item | Status | File:Line | Notes |
|:-:|-------------|:------:|-----------|-------|
| 8 | `NotificationConstants.java` 생성 | Match | `NotificationConstants.java` | 9 topics + WS + Redis 상수 |
| 9 | `CreateNotificationCommand` record | Match | `CreateNotificationCommand.java` | `from(NotificationEvent)` factory 포함 |
| 10 | `NotificationService.create()` 시그니처 변경 | Match | `NotificationService.java:10` | 7-param → Command DTO |
| 11 | `NotificationServiceImpl` idempotency | Match | `NotificationServiceImpl.java:29-46` | referenceId+type+userId 체크 |
| 12 | `NotificationRepository` idempotency 쿼리 | Match | `NotificationRepository.java:33-35` | exists + find 쿼리 |
| 13 | `NotificationEvent.validate()` | Match | `NotificationEvent.java:24-35` | defaultMessage fallback 포함 |
| 14 | Consumer 통합 (`handleShoppingEvent`) | Match | `NotificationConsumer.java:39-50` | 6 topics 통합 listener |
| 15 | `handleUserSignup` 구현 | Match | `NotificationConsumer.java:28-37` | `Long.parseLong()` 변환 적용 |
| 16 | `@PrePersist` + JPA index | Match | `Notification.java:55-63`, `:12` | `onCreate()` + `idx_notification_ref` |
| 17 | KafkaConsumerConfig retry 외부화 | Match | `KafkaConsumerConfig.java:53-57` | `@Value` 2개 |
| 18 | `RedisConfig @Qualifier` | Match | `RedisConfig.java:19` | `@Bean("redisObjectMapper")` |
| 19 | `PushService` 상수 + `@Qualifier` | Match | `NotificationPushService.java:19,30,35` | 상수 + qualifier 적용 |
| 20 | `RedisSubscriber` 상수 + `@Qualifier` | Match | `NotificationRedisSubscriber.java:17,23,29` | 상수 + qualifier 적용 |
| 21 | `logback-spring.xml` 중복 제거 | Match | `logback-spring.xml` | 183줄 → 94줄 |
| 22 | `groupId` SpEL 전환 | Match | `NotificationConsumer.java:25,41` | `${spring.kafka.consumer.group-id}` |

### Phase 3: Cross-service (7 items)

| # | Design Item | Status | File:Line | Notes |
|:-:|-------------|:------:|-----------|-------|
| 23 | Gateway notification route | Match | `api-gateway/application.yml:331-341` | `RequestRateLimiter` + order 50 |
| 24 | `CouponIssuedEvent` record | Match | `common-library/.../CouponIssuedEvent.java` | 6 fields |
| 25 | `TimeDealStartedEvent` record | Match | `common-library/.../TimeDealStartedEvent.java` | 4 fields |
| 26 | Shopping `KafkaConfig` 토픽 2개 | Match | `KafkaConfig.java:38-39` | + NewTopic beans |
| 27 | `ShoppingEventPublisher` 메서드 2개 | Match | `ShoppingEventPublisher.java:72-85` | `publishCouponIssued`, `publishTimeDealStarted` |
| 28 | `handleCouponIssued` typed consumer | Match | `NotificationConsumer.java:52-66` | `CouponIssuedEvent` → `NotificationEvent` 변환 |
| 29 | `handleTimeDealStarted` placeholder | Match | `NotificationConsumer.java:68-75` | broadcast skip 로그 |

### Tests + DB Migration (4 items)

| # | Design Item | Status | File:Line | Notes |
|:-:|-------------|:------:|-----------|-------|
| 30 | `NotificationServiceImplTest` | Match | 4 tests | 생성, 중복 skip, null ref, unread count |
| 31 | `NotificationConsumerTest` | Match | 4 tests | signup, shopping, invalid, defaultMessage |
| 32 | `NotificationControllerTest` | Match | 4 tests | 목록, count, readAll, delete |
| 33 | V2 Flyway migration | Match | `V2__Add_notification_reference_index.sql` | `idx_notification_ref` |

---

## 3. Gaps

### Gap 1: `@AllArgsConstructor` access level (Partial Match)

| Field | Value |
|-------|-------|
| Design | `@AllArgsConstructor` 완전 제거 |
| Implementation | `@AllArgsConstructor(access = AccessLevel.PRIVATE)` |
| File | `Notification.java:16` |
| Impact | Low |
| Reason | Lombok `@Builder` + `@NoArgsConstructor` 조합에서 `@AllArgsConstructor` 없이는 컴파일 에러 발생. `PRIVATE` access로 캡슐화 유지하면서 `@Builder` 동작 보장. 설계보다 나은 구현. |

---

## 4. Design Document Inaccuracies (Non-implementation gaps)

| Item | Design States | Reality | Impact |
|------|---------------|---------|--------|
| `handleUserSignup` userId type | `event.userId()` as Long | `UserSignedUpEvent.userId()` returns `String`; impl uses `Long.parseLong()` | None (correctly handled) |
| Gateway key-resolver | `userIdKeyResolver` | Gateway uses `userKeyResolver` consistently | None (follows existing convention) |

---

## 5. Conclusion

**Match Rate: 97.0% -- 추가 iteration 불필요.**

유일한 gap은 `@AllArgsConstructor` access level이며, 이는 설계의 의도(캡슐화)를 더 잘 만족하는 구현입니다. Lombok의 기술적 제약(`@Builder` + `@NoArgsConstructor` 조합)으로 인해 완전 제거 대신 `PRIVATE` access를 적용한 것은 올바른 판단입니다.

모든 핵심 설계 항목(security fixes, consumer 통합, idempotency, constants 추출, cross-service 연결, tests)이 정확하게 구현되었습니다.
