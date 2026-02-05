# Event Handlers

## 개요

Event Handler는 Kafka 토픽에서 수신한 도메인 이벤트를 처리하는 컴포넌트입니다. Notification Service의 `NotificationConsumer`는 쇼핑 도메인의 다양한 이벤트를 수신하여 알림을 생성합니다.

## 이벤트 핸들러 구조

### NotificationConsumer 클래스

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created event: userId={}", event.getUserId());
        createAndPush(event);
    }

    // ... 다른 핸들러들
}
```

## 이벤트 유형별 핸들러

### 1. 주문 이벤트 (Order)

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(NotificationEvent event) {
    log.info("Received order created event: userId={}", event.getUserId());
    createAndPush(event);
}
```

**토픽:** `shopping.order.created`
**알림 유형:** `ORDER_CREATED`
**발생 시점:** 사용자가 주문을 완료했을 때

### 2. 배송 이벤트 (Delivery)

```java
@KafkaListener(topics = "shopping.delivery.shipped", groupId = "notification-group")
public void handleDeliveryShipped(NotificationEvent event) {
    log.info("Received delivery shipped event: userId={}", event.getUserId());
    createAndPush(event);
}
```

**토픽:** `shopping.delivery.shipped`
**알림 유형:** `DELIVERY_STARTED`
**발생 시점:** 상품이 발송되었을 때

### 3. 결제 이벤트 (Payment)

```java
@KafkaListener(topics = "shopping.payment.completed", groupId = "notification-group")
public void handlePaymentCompleted(NotificationEvent event) {
    log.info("Received payment completed event: userId={}", event.getUserId());
    createAndPush(event);
}
```

**토픽:** `shopping.payment.completed`
**알림 유형:** `PAYMENT_COMPLETED`
**발생 시점:** 결제가 성공적으로 처리되었을 때

### 4. 쿠폰 이벤트 (Coupon)

```java
@KafkaListener(topics = "shopping.coupon.issued", groupId = "notification-group")
public void handleCouponIssued(NotificationEvent event) {
    log.info("Received coupon issued event: userId={}", event.getUserId());
    createAndPush(event);
}
```

**토픽:** `shopping.coupon.issued`
**알림 유형:** `COUPON_ISSUED`
**발생 시점:** 사용자에게 쿠폰이 발급되었을 때

### 5. 타임딜 이벤트 (TimeDeal)

```java
@KafkaListener(topics = "shopping.timedeal.started", groupId = "notification-group")
public void handleTimeDealStarted(NotificationEvent event) {
    log.info("Received timedeal started event: userId={}", event.getUserId());
    createAndPush(event);
}
```

**토픽:** `shopping.timedeal.started`
**알림 유형:** `TIMEDEAL_STARTED`
**발생 시점:** 타임딜이 시작되었을 때

### 6. 회원 가입 이벤트 (User Signup)

```java
@KafkaListener(topics = "user-signup", groupId = "notification-group")
public void handleUserSignup(UserSignedUpEvent event) {
    log.info("Received user signup event: {}", event);
    log.info("Sending welcome email to: {} ({})", event.name(), event.email());
    // 환영 이메일 발송 로직
}
```

**토픽:** `user-signup`
**발생 시점:** 새로운 사용자가 회원 가입했을 때

## 이벤트 토픽 매핑

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Topic → Handler Mapping                       │
├─────────────────────────┬───────────────────────────────────────────┤
│ Topic                   │ Handler Method                             │
├─────────────────────────┼───────────────────────────────────────────┤
│ user-signup             │ handleUserSignup()                         │
│ shopping.order.created  │ handleOrderCreated()                       │
│ shopping.delivery.shipped│ handleDeliveryShipped()                   │
│ shopping.payment.completed│ handlePaymentCompleted()                 │
│ shopping.coupon.issued  │ handleCouponIssued()                       │
│ shopping.timedeal.started│ handleTimeDealStarted()                   │
└─────────────────────────┴───────────────────────────────────────────┘
```

## 이벤트 처리 파이프라인

### createAndPush 메서드

```java
/**
 * 알림을 생성하고 푸시합니다.
 *
 * 중요: try-catch로 예외를 삼키지 않습니다.
 * 예외가 발생하면 KafkaConsumerConfig의 ErrorHandler가:
 * 1. 설정된 횟수만큼 재시도
 * 2. 재시도 실패 시 DLQ(Dead Letter Queue)로 이동
 */
private void createAndPush(NotificationEvent event) {
    log.debug("Creating notification for user: {}", event.getUserId());

    // 1. 알림 생성 및 DB 저장
    Notification notification = notificationService.create(
            event.getUserId(),
            event.getType(),
            event.getTitle(),
            event.getMessage(),
            event.getLink(),
            event.getReferenceId(),
            event.getReferenceType()
    );

    // 2. 실시간 푸시 (WebSocket + Redis)
    pushService.push(notification);

    log.info("Notification created and pushed: userId={}, type={}, notificationId={}",
            event.getUserId(), event.getType(), notification.getId());
}
```

### 처리 흐름 다이어그램

```
Kafka Event
    │
    ▼
┌─────────────────────┐
│ @KafkaListener      │
│ handleXXX()         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ createAndPush()     │
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
┌─────────┐  ┌─────────────┐
│ create()│  │             │
│(DB Save)│  │             │
└────┬────┘  │             │
     │       │             │
     ▼       ▼             │
┌─────────────────┐        │
│ push()          │        │
│ ├─ WebSocket    │        │
│ └─ Redis Pub/Sub│        │
└─────────────────┘        │
                           │
     예외 발생 시           │
           │               │
           ▼               │
┌─────────────────────┐    │
│ ErrorHandler        │◄───┘
│ ├─ 재시도 (3회)     │
│ └─ DLQ 이동         │
└─────────────────────┘
```

## NotificationEvent DTO

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long userId;           // 대상 사용자 ID
    private NotificationType type; // 알림 유형
    private String title;          // 알림 제목
    private String message;        // 알림 내용
    private String link;           // 연결 링크 (선택)
    private String referenceId;    // 참조 ID (주문번호 등)
    private String referenceType;  // 참조 타입 (ORDER, COUPON 등)
}
```

## 이벤트 처리 설계 원칙

### 1. 멱등성 (Idempotency)

```java
// referenceId를 사용한 중복 체크 예시
public Notification create(NotificationEvent event) {
    // 동일한 referenceId로 이미 알림이 있는지 확인
    Optional<Notification> existing = repository.findByUserIdAndReferenceIdAndReferenceType(
        event.getUserId(),
        event.getReferenceId(),
        event.getReferenceType()
    );

    if (existing.isPresent()) {
        log.warn("Duplicate notification detected, skipping: {}", event.getReferenceId());
        return existing.get();
    }

    // 새 알림 생성
    return repository.save(notification);
}
```

### 2. 예외 전파

```java
// 잘못된 예시: 예외를 삼킴
private void createAndPush(NotificationEvent event) {
    try {
        // 처리 로직
    } catch (Exception e) {
        log.error("Failed", e);  // 예외가 사라짐 → 재시도 불가
    }
}

// 올바른 예시: 예외 전파
private void createAndPush(NotificationEvent event) {
    // 예외를 catch하지 않음
    // ErrorHandler가 재시도 및 DLQ 처리
    Notification notification = notificationService.create(...);
    pushService.push(notification);
}
```

### 3. 로깅 전략

```java
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(NotificationEvent event) {
    // 1. 수신 로그 (INFO)
    log.info("Received order created event: userId={}", event.getUserId());

    // 2. 처리 중 상세 로그 (DEBUG)
    log.debug("Creating notification for user: {}", event.getUserId());

    // 3. 완료 로그 (INFO)
    log.info("Notification created and pushed: userId={}, type={}, id={}",
            event.getUserId(), event.getType(), notification.getId());
}
```

## 핸들러 확장 가이드

### 새 이벤트 핸들러 추가

```java
// 1. 새 토픽 핸들러 추가
@KafkaListener(topics = "shopping.wishlist.item-on-sale", groupId = "notification-group")
public void handleWishlistItemOnSale(NotificationEvent event) {
    log.info("Received wishlist item on sale event: userId={}", event.getUserId());
    createAndPush(event);
}
```

```java
// 2. NotificationType에 새 유형 추가
public enum NotificationType {
    // 기존 유형들...

    // Wishlist
    WISHLIST_ITEM_ON_SALE("관심 상품이 할인 중입니다");
}
```

### 이벤트별 커스텀 처리

```java
@KafkaListener(topics = "shopping.order.cancelled")
public void handleOrderCancelled(NotificationEvent event) {
    log.info("Received order cancelled event: userId={}", event.getUserId());

    // 커스텀 처리: 환불 안내 메시지 추가
    NotificationEvent enrichedEvent = NotificationEvent.builder()
            .userId(event.getUserId())
            .type(NotificationType.ORDER_CANCELLED)
            .title(event.getTitle())
            .message(event.getMessage() + " 환불은 3-5영업일 내 처리됩니다.")
            .link(event.getLink())
            .referenceId(event.getReferenceId())
            .referenceType(event.getReferenceType())
            .build();

    createAndPush(enrichedEvent);
}
```

## 테스트 전략

### 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPushService pushService;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    void handleOrderCreated_shouldCreateAndPushNotification() {
        // Given
        NotificationEvent event = NotificationEvent.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 완료")
                .message("주문이 완료되었습니다.")
                .build();

        Notification notification = mock(Notification.class);
        when(notificationService.create(any())).thenReturn(notification);

        // When
        consumer.handleOrderCreated(event);

        // Then
        verify(notificationService).create(any());
        verify(pushService).push(notification);
    }
}
```

## 관련 문서

- [consumer-architecture.md](./consumer-architecture.md) - Consumer 아키텍처
- [consumer-error-handling.md](./consumer-error-handling.md) - 에러 처리
- [../notification/notification-types.md](../notification/notification-types.md) - 알림 유형
