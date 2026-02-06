# Notification Types

## 개요

알림 유형(Notification Type)은 알림의 목적과 성격을 분류하는 열거형입니다. 각 유형은 고유한 의미와 기본 메시지를 가지며, 클라이언트에서 알림을 적절하게 표시하는 데 활용됩니다.

## NotificationType Enum

```java
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // Order
    ORDER_CREATED("주문이 접수되었습니다"),
    ORDER_CONFIRMED("주문이 확정되었습니다"),
    ORDER_CANCELLED("주문이 취소되었습니다"),

    // Delivery
    DELIVERY_STARTED("상품이 발송되었습니다"),
    DELIVERY_IN_TRANSIT("상품이 배송 중입니다"),
    DELIVERY_COMPLETED("상품이 배송 완료되었습니다"),

    // Payment
    PAYMENT_COMPLETED("결제가 완료되었습니다"),
    PAYMENT_FAILED("결제가 실패했습니다"),
    REFUND_COMPLETED("환불이 완료되었습니다"),

    // Coupon
    COUPON_ISSUED("쿠폰이 발급되었습니다"),
    COUPON_EXPIRING("쿠폰이 곧 만료됩니다"),

    // TimeDeal
    TIMEDEAL_STARTING("타임딜이 곧 시작됩니다"),
    TIMEDEAL_STARTED("타임딜이 시작되었습니다"),

    // System
    SYSTEM("시스템 알림");

    private final String defaultMessage;
}
```

## 유형별 상세

### 주문 관련 (Order)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Order Notification Flow                       │
│                                                                      │
│  주문 접수          주문 확정           주문 취소                     │
│  ┌─────────┐      ┌─────────┐        ┌─────────┐                    │
│  │ ORDER   │─────▶│ ORDER   │───X───▶│ ORDER   │                    │
│  │ CREATED │      │CONFIRMED│        │CANCELLED│                    │
│  └─────────┘      └─────────┘        └─────────┘                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `ORDER_CREATED` | 주문이 접수되었습니다 | 주문 완료 | 높음 |
| `ORDER_CONFIRMED` | 주문이 확정되었습니다 | 결제 검증 후 | 높음 |
| `ORDER_CANCELLED` | 주문이 취소되었습니다 | 취소 처리 시 | 높음 |

### 배송 관련 (Delivery)

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Delivery Notification Flow                     │
│                                                                      │
│  발송 완료         배송 중              배송 완료                     │
│  ┌─────────┐     ┌─────────┐         ┌─────────┐                    │
│  │DELIVERY │────▶│DELIVERY │────────▶│DELIVERY │                    │
│  │ STARTED │     │IN_TRANSIT│        │COMPLETED│                    │
│  └─────────┘     └─────────┘         └─────────┘                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `DELIVERY_STARTED` | 상품이 발송되었습니다 | 발송 처리 시 | 중간 |
| `DELIVERY_IN_TRANSIT` | 상품이 배송 중입니다 | 배송사 연동 | 낮음 |
| `DELIVERY_COMPLETED` | 상품이 배송 완료되었습니다 | 배송 완료 시 | 높음 |

### 결제 관련 (Payment)

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `PAYMENT_COMPLETED` | 결제가 완료되었습니다 | 결제 성공 | 높음 |
| `PAYMENT_FAILED` | 결제가 실패했습니다 | 결제 실패 | 높음 |
| `REFUND_COMPLETED` | 환불이 완료되었습니다 | 환불 처리 완료 | 높음 |

### 쿠폰 관련 (Coupon)

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `COUPON_ISSUED` | 쿠폰이 발급되었습니다 | 쿠폰 발급 | 중간 |
| `COUPON_EXPIRING` | 쿠폰이 곧 만료됩니다 | 만료 7일 전 | 중간 |

### 타임딜 관련 (TimeDeal)

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `TIMEDEAL_STARTING` | 타임딜이 곧 시작됩니다 | 시작 1시간 전 | 중간 |
| `TIMEDEAL_STARTED` | 타임딜이 시작되었습니다 | 타임딜 시작 | 높음 |

### 시스템 알림 (System)

| Type | 기본 메시지 | 발생 시점 | 우선순위 |
|------|-----------|----------|---------|
| `SYSTEM` | 시스템 알림 | 공지, 점검 등 | 가변 |

## 이벤트-알림 매핑

### Kafka Topic → NotificationType

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Event to Notification Mapping                     │
│                                                                      │
│  Kafka Topic                    │  NotificationType                  │
│  ───────────────────────────────┼─────────────────────────────────── │
│  shopping.order.created         │  ORDER_CREATED                    │
│  shopping.delivery.shipped      │  DELIVERY_STARTED                 │
│  shopping.payment.completed     │  PAYMENT_COMPLETED                │
│  shopping.coupon.issued         │  COUPON_ISSUED                    │
│  shopping.timedeal.started      │  TIMEDEAL_STARTED                 │
│  user-signup                    │  (환영 이메일, 별도 처리)          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Consumer 매핑 예시

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    @KafkaListener(topics = "shopping.order.created")
    public void handleOrderCreated(NotificationEvent event) {
        // event.type = ORDER_CREATED
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.delivery.shipped")
    public void handleDeliveryShipped(NotificationEvent event) {
        // event.type = DELIVERY_STARTED
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.payment.completed")
    public void handlePaymentCompleted(NotificationEvent event) {
        // event.type = PAYMENT_COMPLETED
        createAndPush(event);
    }
}
```

## 클라이언트 활용

### UI 아이콘 매핑

```javascript
const notificationIcons = {
    // Order
    ORDER_CREATED: 'shopping-cart',
    ORDER_CONFIRMED: 'check-circle',
    ORDER_CANCELLED: 'x-circle',

    // Delivery
    DELIVERY_STARTED: 'truck',
    DELIVERY_IN_TRANSIT: 'truck',
    DELIVERY_COMPLETED: 'package-check',

    // Payment
    PAYMENT_COMPLETED: 'credit-card',
    PAYMENT_FAILED: 'alert-triangle',
    REFUND_COMPLETED: 'refresh-cw',

    // Coupon
    COUPON_ISSUED: 'gift',
    COUPON_EXPIRING: 'clock',

    // TimeDeal
    TIMEDEAL_STARTING: 'zap',
    TIMEDEAL_STARTED: 'zap',

    // System
    SYSTEM: 'bell'
};
```

### 색상 테마

```javascript
const notificationColors = {
    // 성공/긍정
    ORDER_CREATED: 'blue',
    ORDER_CONFIRMED: 'green',
    PAYMENT_COMPLETED: 'green',
    DELIVERY_COMPLETED: 'green',
    COUPON_ISSUED: 'purple',
    REFUND_COMPLETED: 'green',

    // 경고
    COUPON_EXPIRING: 'orange',
    TIMEDEAL_STARTING: 'orange',

    // 알림/정보
    DELIVERY_STARTED: 'blue',
    DELIVERY_IN_TRANSIT: 'blue',
    TIMEDEAL_STARTED: 'indigo',
    SYSTEM: 'gray',

    // 오류/취소
    ORDER_CANCELLED: 'red',
    PAYMENT_FAILED: 'red'
};
```

### 알림 그룹화

```javascript
const notificationGroups = {
    ORDER: ['ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_CANCELLED'],
    DELIVERY: ['DELIVERY_STARTED', 'DELIVERY_IN_TRANSIT', 'DELIVERY_COMPLETED'],
    PAYMENT: ['PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'REFUND_COMPLETED'],
    PROMOTION: ['COUPON_ISSUED', 'COUPON_EXPIRING', 'TIMEDEAL_STARTING', 'TIMEDEAL_STARTED'],
    SYSTEM: ['SYSTEM']
};

// 그룹별 필터링
function filterByGroup(notifications, group) {
    const types = notificationGroups[group];
    return notifications.filter(n => types.includes(n.type));
}
```

## 알림 설정

### 사용자 선호 설정 (예시)

```java
@Entity
public class NotificationPreference {

    @Id
    private Long userId;

    // 유형별 활성화 여부
    private boolean orderEnabled = true;
    private boolean deliveryEnabled = true;
    private boolean paymentEnabled = true;
    private boolean couponEnabled = true;
    private boolean timedealEnabled = false;
    private boolean systemEnabled = true;

    // 채널별 설정
    private boolean pushEnabled = true;
    private boolean emailEnabled = false;
    private boolean smsEnabled = false;

    public boolean isEnabled(NotificationType type) {
        return switch (type) {
            case ORDER_CREATED, ORDER_CONFIRMED, ORDER_CANCELLED -> orderEnabled;
            case DELIVERY_STARTED, DELIVERY_IN_TRANSIT, DELIVERY_COMPLETED -> deliveryEnabled;
            case PAYMENT_COMPLETED, PAYMENT_FAILED, REFUND_COMPLETED -> paymentEnabled;
            case COUPON_ISSUED, COUPON_EXPIRING -> couponEnabled;
            case TIMEDEAL_STARTING, TIMEDEAL_STARTED -> timedealEnabled;
            case SYSTEM -> systemEnabled;
        };
    }
}
```

## 유형 확장 가이드

### 새 알림 유형 추가

```java
// 1. Enum에 새 유형 추가
public enum NotificationType {
    // 기존 유형들...

    // 새로운 유형
    WISHLIST_PRICE_DROP("관심 상품의 가격이 인하되었습니다"),
    REVIEW_REQUEST("구매하신 상품의 리뷰를 작성해주세요"),
    POINT_EXPIRING("포인트가 곧 만료됩니다");

    private final String defaultMessage;
}
```

```java
// 2. 새 토픽 핸들러 추가
@KafkaListener(topics = "shopping.wishlist.price-drop")
public void handleWishlistPriceDrop(NotificationEvent event) {
    createAndPush(event);
}
```

```javascript
// 3. 클라이언트 UI 매핑 추가
notificationIcons.WISHLIST_PRICE_DROP = 'heart';
notificationColors.WISHLIST_PRICE_DROP = 'pink';
```

## 우선순위 시스템

### 우선순위 분류

```java
public enum NotificationPriority {
    CRITICAL,  // 결제 실패, 주문 취소
    HIGH,      // 주문 완료, 배송 완료
    MEDIUM,    // 쿠폰 발급, 타임딜
    LOW        // 배송 중, 정보성 알림
}

public NotificationPriority getPriority(NotificationType type) {
    return switch (type) {
        case PAYMENT_FAILED, ORDER_CANCELLED -> NotificationPriority.CRITICAL;
        case ORDER_CREATED, ORDER_CONFIRMED, PAYMENT_COMPLETED,
             DELIVERY_COMPLETED, TIMEDEAL_STARTED -> NotificationPriority.HIGH;
        case COUPON_ISSUED, COUPON_EXPIRING, TIMEDEAL_STARTING,
             DELIVERY_STARTED, REFUND_COMPLETED -> NotificationPriority.MEDIUM;
        case DELIVERY_IN_TRANSIT, SYSTEM -> NotificationPriority.LOW;
    };
}
```

## Best Practices

1. **명확한 네이밍** - 도메인_동작 형식 (ORDER_CREATED)
2. **기본 메시지 제공** - 최소한의 정보 보장
3. **그룹화 가능하게** - 관련 유형끼리 접두사 공유
4. **확장 고려** - 새 유형 추가 시 영향 최소화
5. **우선순위 정의** - 중요도에 따른 처리 차별화
6. **다국어 지원 준비** - 메시지 키 기반 설계

## 관련 문서

- [notification-domain.md](./notification-domain.md) - 알림 도메인 모델
- [../kafka/event-handlers.md](../kafka/event-handlers.md) - 이벤트 핸들러
