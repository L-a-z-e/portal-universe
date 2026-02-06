# notification-refactoring Design

> **Feature**: notification-refactoring
> **Plan**: [notification-refactoring.plan.md](../../01-plan/features/notification-refactoring.plan.md)
> **Date**: 2026-02-02
> **Scope**: Phase 1 (Security + Dead Code) + Phase 2 (Code Quality) + Phase 3 (Cross-service 연결)

---

## 1. 변경 파일 맵

### notification-service 내부

| File | Action | Phase |
|------|--------|:-----:|
| `consumer/NotificationConsumer.java` | **Rewrite** - generic handler 통합, event validation, 누락 토픽 추가 | 1+2+3 |
| `service/NotificationService.java` | **Modify** - `create()` 시그니처 변경 | 2 |
| `service/NotificationServiceImpl.java` | **Modify** - Command DTO, idempotency, defaultMessage fallback | 2 |
| `service/NotificationPushService.java` | **Modify** - `pushToAll()` 제거, 상수 추출 | 1+2 |
| `domain/Notification.java` | **Modify** - `@AllArgsConstructor` 제거, `@PrePersist` | 1+2 |
| `dto/NotificationEvent.java` | **Modify** - validation 메서드 추가 | 2 |
| `dto/CreateNotificationCommand.java` | **New** - Service 파라미터 축소용 Command DTO | 2 |
| `common/config/WebSocketConfig.java` | **Modify** - CORS 환경변수화 | 1 |
| `common/config/RedisConfig.java` | **Modify** - `@Qualifier` 명시 | 2 |
| `common/config/KafkaConsumerConfig.java` | **Modify** - retry 설정 외부화 | 2 |
| `common/config/NotificationRedisSubscriber.java` | **Modify** - 상수 추출 | 2 |
| `common/constants/NotificationConstants.java` | **New** - Topic, WebSocket path, Redis prefix 상수 | 2 |
| `repository/NotificationRepository.java` | **Modify** - idempotency 쿼리 추가 | 2 |
| `common/exception/NotificationErrorCode.java` | **Keep** - N002, N003은 validation에서 사용하게 됨 | 2 |
| `application-local.yml` | **Modify** - trusted.packages 제한, Feign 제거, CORS 추가 | 1 |
| `application-docker.yml` | **Modify** - 동일 | 1 |
| `application-kubernetes.yml` | **Modify** - 동일 | 1 |
| `build.gradle` | **Modify** - Spring Cloud BOM 제거 | 1 |
| `logback-spring.xml` | **Modify** - 중복 appender 통합 | 2 |

### Cross-service

| File | Action | Phase |
|------|--------|:-----:|
| `api-gateway/.../application.yml` | **Modify** - notification route 추가 | 3 |
| `shopping-service/.../CouponService(Impl).java` | **Modify** - coupon.issued 이벤트 발행 추가 | 3 |
| `shopping-service/.../TimeDealService(Impl).java` | **Modify** - timedeal.started 이벤트 발행 추가 | 3 |
| `shopping-service/.../KafkaConfig.java` | **Modify** - 2개 토픽 추가 | 3 |
| `common-library/.../event/shopping/` | **New** - `CouponIssuedEvent.java`, `TimeDealStartedEvent.java` record | 3 |

### Test (신규)

| File | Phase |
|------|:-----:|
| `src/test/java/.../service/NotificationServiceImplTest.java` | 2 |
| `src/test/java/.../consumer/NotificationConsumerTest.java` | 2 |
| `src/test/java/.../controller/NotificationControllerTest.java` | 2 |

---

## 2. 상세 설계

### 2.1 Phase 1: Security + Dead Code 정리

#### 2.1.1 `trusted.packages` 제한

**AS-IS** (`application-local.yml:70`):
```yaml
spring.json.trusted.packages: "*"
```

**TO-BE**:
```yaml
spring.json.trusted.packages: "com.portal.universe.*"
```

> 동일하게 `application-docker.yml`, `application-kubernetes.yml` 적용.
> `KafkaConsumerConfig.java:85`는 이미 `"com.portal.universe.*"`로 설정되어 있어 변경 불필요.

#### 2.1.2 WebSocket CORS 환경변수화

**AS-IS** (`WebSocketConfig.java:26`):
```java
.setAllowedOriginPatterns("*")
```

**TO-BE**:
```java
@Value("${app.websocket.allowed-origins:*}")
private String[] allowedOrigins;

// ...
.setAllowedOriginPatterns(allowedOrigins)
```

`application-local.yml` 추가:
```yaml
app:
  websocket:
    allowed-origins: "*"
```

`application-docker.yml` / `application-kubernetes.yml`:
```yaml
app:
  websocket:
    allowed-origins: "https://localhost:30000,https://portal-shell:30000"
```

#### 2.1.3 Feign 설정 제거

`application-local.yml`, `application-docker.yml`, `application-kubernetes.yml`에서 아래 블록 삭제:
```yaml
feign:
  client:
    config:
      ...
```

`services:` 블록도 notification-service에서 불필요 (다른 서비스를 호출하지 않음) → 삭제.

#### 2.1.4 Spring Cloud BOM 제거

**AS-IS** (`build.gradle:27-30, 85-90`):
```groovy
ext {
    set('springCloudVersion', "2025.0.0")
}
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

**TO-BE**: 해당 블록 전체 삭제.

#### 2.1.5 Dead Consumer/Code 정리

| Item | Action |
|------|--------|
| `handleCouponIssued` | Phase 3에서 shopping-service 이벤트 발행 후 복원. Phase 1에서는 **유지하되 주석으로 미발행 상태 표기** |
| `handleTimeDealStarted` | 동일 |
| `handleUserSignup` | 실제 SYSTEM 타입 welcome 알림 생성하도록 구현 |
| `pushToAll()` | 삭제 (호출처 없음) |
| `@AllArgsConstructor` | `Notification.java`에서 삭제 (`@Builder` + `@NoArgsConstructor` 충분) |

#### 2.1.6 `handleUserSignup` 구현

**AS-IS**: 로그만 출력
```java
@KafkaListener(topics = "user-signup", groupId = "notification-group")
public void handleUserSignup(UserSignedUpEvent event) {
    log.info("Received user signup event: {}", event);
    log.info("Sending welcome email to: {} ({})", event.name(), event.email());
}
```

**TO-BE**: NotificationEvent로 변환하여 알림 생성
```java
@KafkaListener(topics = NotificationConstants.TOPIC_USER_SIGNUP,
               groupId = "${spring.kafka.consumer.group-id}")
public void handleUserSignup(UserSignedUpEvent event) {
    log.info("Received user signup event: userId={}", event.userId());
    NotificationEvent notifEvent = NotificationEvent.builder()
            .userId(event.userId())
            .type(NotificationType.SYSTEM)
            .title("환영합니다!")
            .message(event.name() + "님, Portal Universe에 가입해주셔서 감사합니다.")
            .build();
    handleNotificationEvent(notifEvent);
}
```

---

### 2.2 Phase 2: Code Quality

#### 2.2.1 상수 클래스 (`NotificationConstants.java`)

```java
package com.portal.universe.notificationservice.common.constants;

public final class NotificationConstants {
    private NotificationConstants() {}

    // Kafka Topics
    public static final String TOPIC_USER_SIGNUP = "user-signup";
    public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
    public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
    public static final String TOPIC_COUPON_ISSUED = "shopping.coupon.issued";
    public static final String TOPIC_TIMEDEAL_STARTED = "shopping.timedeal.started";

    // WebSocket
    public static final String WS_QUEUE_NOTIFICATIONS = "/queue/notifications";

    // Redis
    public static final String REDIS_CHANNEL_PREFIX = "notification:";
}
```

#### 2.2.2 Consumer 리팩토링 (`NotificationConsumer.java`)

**핵심 변경**: 5중 복사 handler → 공통 `handleNotificationEvent()` + 개별 `@KafkaListener`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = NotificationConstants.TOPIC_USER_SIGNUP,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: userId={}", event.userId());
        NotificationEvent notifEvent = NotificationEvent.builder()
                .userId(event.userId())
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message(event.name() + "님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();
        handleNotificationEvent(notifEvent);
    }

    @KafkaListener(topics = {
        NotificationConstants.TOPIC_ORDER_CREATED,
        NotificationConstants.TOPIC_ORDER_CONFIRMED,
        NotificationConstants.TOPIC_ORDER_CANCELLED,
        NotificationConstants.TOPIC_DELIVERY_SHIPPED,
        NotificationConstants.TOPIC_PAYMENT_COMPLETED,
        NotificationConstants.TOPIC_PAYMENT_FAILED,
        NotificationConstants.TOPIC_COUPON_ISSUED,
        NotificationConstants.TOPIC_TIMEDEAL_STARTED
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void handleShoppingEvent(NotificationEvent event) {
        log.info("Received shopping event: userId={}, type={}", event.getUserId(), event.getType());
        handleNotificationEvent(event);
    }

    private void handleNotificationEvent(NotificationEvent event) {
        event.validate(); // throws IllegalArgumentException if invalid

        Notification notification = notificationService.create(
                CreateNotificationCommand.from(event));

        pushService.push(notification);

        log.info("Notification created and pushed: userId={}, type={}, id={}",
                event.getUserId(), event.getType(), notification.getId());
    }
}
```

> **설계 결정**: shopping 토픽을 하나의 `@KafkaListener`로 묶는다. 이유:
> - 모든 shopping 이벤트가 동일한 `NotificationEvent` DTO를 사용
> - 처리 로직이 완전히 동일 (`validate → create → push`)
> - `user-signup`만 다른 DTO(`UserSignedUpEvent`)를 사용하므로 별도 listener

#### 2.2.3 Event Validation (`NotificationEvent.java`)

```java
public void validate() {
    if (userId == null || userId <= 0) {
        throw new IllegalArgumentException("userId is required and must be positive");
    }
    if (type == null) {
        throw new IllegalArgumentException("notification type is required");
    }
    if (title == null || title.isBlank()) {
        // fallback to defaultMessage
        this.title = type.getDefaultMessage();
    }
    if (message == null || message.isBlank()) {
        this.message = type.getDefaultMessage();
    }
}
```

> `IllegalArgumentException`은 `KafkaConsumerConfig`에서 not-retryable로 설정되어 있으므로 즉시 DLQ로 이동됨.

#### 2.2.4 Command DTO (`CreateNotificationCommand.java`)

```java
package com.portal.universe.notificationservice.dto;

public record CreateNotificationCommand(
    Long userId,
    NotificationType type,
    String title,
    String message,
    String link,
    String referenceId,
    String referenceType
) {
    public static CreateNotificationCommand from(NotificationEvent event) {
        return new CreateNotificationCommand(
            event.getUserId(),
            event.getType(),
            event.getTitle(),
            event.getMessage(),
            event.getLink(),
            event.getReferenceId(),
            event.getReferenceType()
        );
    }
}
```

#### 2.2.5 Service 시그니처 변경

**AS-IS** (`NotificationService.java`):
```java
Notification create(Long userId, NotificationType type, String title, String message,
                    String link, String referenceId, String referenceType);
```

**TO-BE**:
```java
Notification create(CreateNotificationCommand command);
```

#### 2.2.6 Idempotency (`NotificationRepository.java`)

추가 쿼리:
```java
boolean existsByReferenceIdAndReferenceTypeAndUserId(
    String referenceId, String referenceType, Long userId);
```

`NotificationServiceImpl.create()` 변경:
```java
@Override
@Transactional
public Notification create(CreateNotificationCommand cmd) {
    // Idempotency: referenceId + referenceType + userId 중복 체크
    if (cmd.referenceId() != null && cmd.referenceType() != null) {
        boolean exists = notificationRepository
                .existsByReferenceIdAndReferenceTypeAndUserId(
                    cmd.referenceId(), cmd.referenceType(), cmd.userId());
        if (exists) {
            log.info("Duplicate notification skipped: ref={}/{}, userId={}",
                    cmd.referenceType(), cmd.referenceId(), cmd.userId());
            return notificationRepository
                    .findByReferenceIdAndReferenceTypeAndUserId(
                        cmd.referenceId(), cmd.referenceType(), cmd.userId())
                    .orElseThrow();
        }
    }

    Notification notification = Notification.builder()
            .userId(cmd.userId())
            .type(cmd.type())
            .title(cmd.title())
            .message(cmd.message())
            .link(cmd.link())
            .referenceId(cmd.referenceId())
            .referenceType(cmd.referenceType())
            .build();

    return notificationRepository.save(notification);
}
```

#### 2.2.7 Entity 개선 (`Notification.java`)

```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_status", columnList = "user_id, status"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_notification_ref", columnList = "reference_id, reference_type, user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Notification {
    // ... fields same as before but without @AllArgsConstructor

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = NotificationStatus.UNREAD;
        }
    }
}
```

> `@Builder` 사용을 위해 `@AllArgsConstructor`를 package-private으로 생성하는 private constructor 패턴:
> ```java
> @Builder
> private Notification(Long id, Long userId, ...) { ... }
> ```

#### 2.2.8 Retry 설정 외부화 (`KafkaConsumerConfig.java`)

```java
@Value("${app.kafka.retry.interval-ms:1000}")
private long retryIntervalMs;

@Value("${app.kafka.retry.max-attempts:3}")
private long maxRetryAttempts;
```

`application.yml`에 기본값 추가:
```yaml
app:
  kafka:
    retry:
      interval-ms: 1000
      max-attempts: 3
```

#### 2.2.9 RedisConfig `@Qualifier`

```java
@Bean("redisObjectMapper")
public ObjectMapper redisObjectMapper() { ... }
```

주입처에서:
```java
@Qualifier("redisObjectMapper")
private final ObjectMapper redisObjectMapper;
```

#### 2.2.10 PushService 상수 적용

```java
public void push(Notification notification) {
    NotificationResponse response = NotificationResponse.from(notification);

    messagingTemplate.convertAndSendToUser(
            notification.getUserId().toString(),
            NotificationConstants.WS_QUEUE_NOTIFICATIONS,
            response
    );

    String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + notification.getUserId();
    // ...
}
// pushToAll() 메서드 삭제
```

#### 2.2.11 logback-spring.xml 중복 제거

Docker/Kubernetes 프로파일의 공통 appender를 `base` 프로파일로 추출:
```xml
<!-- 공통 JSON appender (profile 외부) -->
<appender name="CONSOLE_JSON" class="...">...</appender>
<appender name="FILE_JSON" class="...">...</appender>

<springProfile name="docker | kubernetes">
    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON" />
        <appender-ref ref="FILE_JSON" />
    </root>
</springProfile>
```

---

### 2.3 Phase 3: Cross-service 연결 복구

#### 2.3.1 Gateway Route 등록

`api-gateway/.../application.yml`의 `routes:` 섹션에 추가:

```yaml
# ========== Notification Service ==========
- id: notification-service
  uri: ${services.notification.url}
  predicates:
    - Path=/api/v1/notifications/**
  filters:
    - name: RequestRateLimiter
      args:
        rate-limiter: "#{@authenticatedRedisRateLimiter}"
        key-resolver: "#{@userIdKeyResolver}"
  order: 50
```

> `TokenRelay`가 아닌 `RequestRateLimiter` + `userIdKeyResolver` 사용.
> Gateway의 `GatewayAuthenticationFilter`가 JWT → `X-User-Id` 변환을 처리.

#### 2.3.2 Shopping 누락 Consumer 추가

Phase 2에서 단일 `@KafkaListener`로 통합했으므로, topics 배열에 이미 포함:
```java
NotificationConstants.TOPIC_ORDER_CONFIRMED,
NotificationConstants.TOPIC_ORDER_CANCELLED,
NotificationConstants.TOPIC_PAYMENT_FAILED,
```

이 토픽들은 shopping-service가 이미 발행 중이므로 consumer 추가만으로 연결 완료.

#### 2.3.3 common-library 이벤트 DTO 추가

```java
// services/common-library/.../event/shopping/CouponIssuedEvent.java
package com.portal.universe.common.event.shopping;

public record CouponIssuedEvent(
    Long userId,
    String couponCode,
    String couponName,
    String discountType,    // PERCENTAGE or FIXED
    int discountValue,
    java.time.LocalDateTime expiresAt
) {}
```

```java
// services/common-library/.../event/shopping/TimeDealStartedEvent.java
package com.portal.universe.common.event.shopping;

public record TimeDealStartedEvent(
    Long timeDealId,
    String timeDealName,
    java.time.LocalDateTime startsAt,
    java.time.LocalDateTime endsAt
) {}
```

#### 2.3.4 Shopping Service 이벤트 발행

**shopping-service KafkaConfig.java에 토픽 추가**:
```java
public static final String TOPIC_COUPON_ISSUED = "shopping.coupon.issued";
public static final String TOPIC_TIMEDEAL_STARTED = "shopping.timedeal.started";

@Bean
public NewTopic topicCouponIssued() {
    return TopicBuilder.name(TOPIC_COUPON_ISSUED).partitions(3).replicas(1).build();
}
@Bean
public NewTopic topicTimeDealStarted() {
    return TopicBuilder.name(TOPIC_TIMEDEAL_STARTED).partitions(3).replicas(1).build();
}
```

**ShoppingEventPublisher.java에 메서드 추가**:
```java
public void publishCouponIssued(CouponIssuedEvent event) {
    publishEvent(KafkaConfig.TOPIC_COUPON_ISSUED, event.couponCode(), event);
}
public void publishTimeDealStarted(TimeDealStartedEvent event) {
    publishEvent(KafkaConfig.TOPIC_TIMEDEAL_STARTED, String.valueOf(event.timeDealId()), event);
}
```

**CouponService에서 발행 호출**: 쿠폰 발급(claim) 시점에 이벤트 발행
**TimeDealScheduler에서 발행 호출**: 타임딜 시작 시점에 이벤트 발행

> **Note**: 쿠폰/타임딜 이벤트는 `NotificationEvent` DTO가 아닌 common-library의 typed event를 사용.
> notification consumer에서 이 이벤트를 `NotificationEvent`로 변환하는 별도 listener가 필요.

#### 2.3.5 Typed Event Consumer 추가

coupon/timedeal 이벤트는 `NotificationEvent`가 아닌 typed record이므로 별도 handler:

```java
@KafkaListener(topics = NotificationConstants.TOPIC_COUPON_ISSUED,
               groupId = "${spring.kafka.consumer.group-id}")
public void handleCouponIssued(CouponIssuedEvent event) {
    log.info("Received coupon issued event: couponCode={}", event.couponCode());
    NotificationEvent notifEvent = NotificationEvent.builder()
            .userId(event.userId())
            .type(NotificationType.COUPON_ISSUED)
            .title(NotificationType.COUPON_ISSUED.getDefaultMessage())
            .message(event.couponName() + " 쿠폰이 발급되었습니다.")
            .link("/shopping/coupons")
            .referenceId(event.couponCode())
            .referenceType("coupon")
            .build();
    handleNotificationEvent(notifEvent);
}

@KafkaListener(topics = NotificationConstants.TOPIC_TIMEDEAL_STARTED,
               groupId = "${spring.kafka.consumer.group-id}")
public void handleTimeDealStarted(TimeDealStartedEvent event) {
    log.info("Received timedeal started event: id={}", event.timeDealId());
    // TimeDeal은 broadcast (특정 userId 없음) → 현재 구조에서는 skip
    // 향후 구독/관심 기능 추가 시 구현
    log.info("TimeDeal broadcast notification not yet implemented (no subscriber model)");
}
```

---

## 3. DB Migration

### V2: idempotency 인덱스 추가

```sql
-- V2__Add_notification_reference_index.sql
CREATE INDEX idx_notification_ref
ON notifications (reference_id, reference_type, user_id);
```

> 별도 unique constraint는 걸지 않음 (application level에서 체크).
> 이유: `referenceId`가 null인 경우가 많으므로 unique constraint가 부적절.

---

## 4. 구현 순서 (Commit 전략)

### Commit 1: `chore(notification): remove dead code and spring cloud bom`
- Spring Cloud BOM 제거 (`build.gradle`)
- Feign 설정 제거 (`application-*.yml` 3개)
- `@AllArgsConstructor` 제거 (`Notification.java`)
- `pushToAll()` 제거 (`NotificationPushService.java`)

### Commit 2: `fix(notification): restrict trusted packages and cors`
- `trusted.packages` 제한 (`application-local.yml`, `application-docker.yml`)
- WebSocket CORS 환경변수화 (`WebSocketConfig.java`, `application-*.yml`)

### Commit 3: `refactor(notification): extract constants and command dto`
- `NotificationConstants.java` 신규
- `CreateNotificationCommand.java` 신규 (record)
- `NotificationService.create()` 시그니처 변경
- `NotificationServiceImpl` Command DTO 적용
- `NotificationPushService` 상수 적용
- `NotificationRedisSubscriber` 상수 적용
- `RedisConfig` `@Qualifier` 명시
- `KafkaConsumerConfig` retry 설정 외부화
- `Notification.java` `@PrePersist` 전환 + idempotency 인덱스
- `NotificationRepository` idempotency 쿼리 추가

### Commit 4: `refactor(notification): consolidate consumers and add validation`
- `NotificationConsumer.java` 전면 리팩토링
  - 5중 복사 → 단일 handler
  - `handleUserSignup` 구현
  - Event validation 추가
  - Idempotency 적용
  - `groupId` SpEL 전환
  - 상수 적용

### Commit 5: `refactor(notification): deduplicate logback profiles`
- `logback-spring.xml` Docker/K8s 공통 appender 추출

### Commit 6: `feat(notification): add gateway route for notification api`
- `api-gateway/application.yml` route 추가

### Commit 7: `feat(common): add coupon and timedeal event dtos`
- `common-library` CouponIssuedEvent, TimeDealStartedEvent record 추가

### Commit 8: `feat(shopping): publish coupon issued and timedeal started events`
- `KafkaConfig` 토픽 추가
- `ShoppingEventPublisher` 메서드 추가
- 쿠폰/타임딜 서비스에서 발행 호출

### Commit 9: `feat(notification): add missing shopping event consumers`
- `order.confirmed`, `order.cancelled`, `payment.failed` consumer
- `coupon.issued`, `timedeal.started` typed event consumer
- V2 Flyway migration (idempotency index)

### Commit 10: `test(notification): add unit and integration tests`
- `NotificationServiceImplTest`
- `NotificationConsumerTest`
- `NotificationControllerTest`

---

## 5. 검증 체크리스트

### Build
- [ ] `./gradlew :services:notification-service:build` 성공
- [ ] `./gradlew :services:common-library:build` 성공
- [ ] `./gradlew :services:shopping-service:build` 성공
- [ ] `./gradlew :services:api-gateway:build` 성공

### 기능 검증 (Docker 환경)
- [ ] notification-service 정상 기동
- [ ] Kafka consumer 정상 구독 (consumer group lag = 0)
- [ ] `POST /api/v1/shopping/products` → order.created → 알림 생성 확인
- [ ] `GET /api/v1/notifications` via Gateway → 200 응답
- [ ] `GET /api/v1/notifications/unread/count` → 정확한 개수
- [ ] `PUT /api/v1/notifications/{id}/read` → READ 상태 전환
- [ ] WebSocket `/ws/notifications` 연결 성공
- [ ] 동일 이벤트 재전송 → 중복 알림 미생성 (idempotency)
- [ ] 잘못된 이벤트 (userId null) → DLQ 이동

### Security
- [ ] `trusted.packages` 제한 확인
- [ ] WebSocket CORS 제한 확인 (docker/k8s 환경)

### Test
- [ ] Unit test 전체 통과
- [ ] 커버리지: Service/Consumer/Controller 주요 경로

---

## 6. Scope 제외

| Item | 이유 | 후속 PDCA |
|------|------|-----------|
| Frontend 알림 UI | 별도 scope (portal-shell Vue + WebSocket) | `notification-frontend` |
| Blog 이벤트 연동 | blog-service Kafka producer 추가 필요 | `blog-notification` |
| DLQ Consumer | 운영 모니터링 관점, 별도 설계 필요 | `notification-monitoring` |
| Email 발송 | 외부 서비스 연동 필요 (SES, SMTP) | `notification-email` |
| 알림 설정 (구독/차단) | 사용자별 알림 preference 설계 필요 | `notification-preferences` |
