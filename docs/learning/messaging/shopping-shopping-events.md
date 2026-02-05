# Shopping Service 이벤트 정의

## 개요

Shopping Service는 Kafka를 활용한 Event-Driven Architecture를 채택하여 서비스 간 느슨한 결합(Loose Coupling)을 달성합니다. 이 문서에서는 쇼핑 도메인에서 발생하는 모든 이벤트의 정의와 페이로드 구조를 설명합니다.

## 이벤트 설계 원칙

### 1. Event Naming Convention

```
{도메인}.{엔티티}.{동작}
```

예시:
- `shopping.order.created`
- `shopping.payment.completed`
- `shopping.delivery.shipped`

### 2. 이벤트 페이로드 원칙

- **Immutability**: Java Record를 사용하여 불변 객체로 정의
- **Self-contained**: 이벤트 소비자가 필요한 정보를 모두 포함
- **Timestamp**: 이벤트 발생 시각 항상 포함
- **Identifier**: 추적을 위한 고유 식별자 포함

## 이벤트 목록

### Topic 정의

```java
// KafkaConfig.java
public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
public static final String TOPIC_INVENTORY_RESERVED = "shopping.inventory.reserved";
public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
```

### 이벤트 흐름도

```
[주문 생성] ──→ OrderCreatedEvent
     │
     ▼
[재고 예약] ──→ InventoryReservedEvent
     │
     ▼
[결제 요청] ──→ PaymentCompletedEvent / PaymentFailedEvent
     │
     ▼
[배송 시작] ──→ DeliveryShippedEvent
     │
     ▼
[주문 확정] ──→ OrderConfirmedEvent
```

## 이벤트 페이로드 상세

### 1. OrderCreatedEvent

주문이 생성되었을 때 발행되는 이벤트입니다.

```java
public record OrderCreatedEvent(
    String orderNumber,           // 주문 번호 (예: "ORD-20250122-001")
    String userId,                // 사용자 ID
    BigDecimal totalAmount,       // 총 금액
    int itemCount,                // 주문 항목 수
    List<OrderItemInfo> items,    // 주문 상품 목록
    LocalDateTime createdAt       // 주문 생성 시각
) {
    public record OrderItemInfo(
        Long productId,           // 상품 ID
        String productName,       // 상품명
        int quantity,             // 수량
        BigDecimal price          // 단가
    ) {}
}
```

**발행 시점**: 주문이 성공적으로 생성된 직후

**소비자**:
- `notification-service`: 주문 확인 알림 발송
- 재고 관리 시스템: 재고 예약 시작

### 2. PaymentCompletedEvent

결제가 성공적으로 완료되었을 때 발행되는 이벤트입니다.

```java
public record PaymentCompletedEvent(
    String paymentNumber,         // 결제 번호
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    BigDecimal amount,            // 결제 금액
    String paymentMethod,         // 결제 수단 (CARD, BANK_TRANSFER 등)
    String pgTransactionId,       // PG사 거래 ID
    LocalDateTime paidAt          // 결제 완료 시각
) {}
```

**발행 시점**: PG사로부터 결제 승인 응답을 받은 후

**소비자**:
- `notification-service`: 결제 완료 알림 발송
- `shopping-service`: 주문 상태 업데이트

### 3. PaymentFailedEvent

결제가 실패했을 때 발행되는 이벤트입니다.

```java
public record PaymentFailedEvent(
    String paymentNumber,         // 결제 번호
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    BigDecimal amount,            // 결제 시도 금액
    String paymentMethod,         // 결제 수단
    String failureReason,         // 실패 사유
    LocalDateTime failedAt        // 실패 시각
) {}
```

**발행 시점**: 결제 처리 중 실패가 발생했을 때

**소비자**:
- `notification-service`: 결제 실패 알림 발송
- `shopping-service`: Saga 보상 트랜잭션 시작 (재고 해제 등)

### 4. InventoryReservedEvent

재고가 성공적으로 예약되었을 때 발행되는 이벤트입니다.

```java
public record InventoryReservedEvent(
    String orderNumber,                       // 주문 번호
    String userId,                            // 사용자 ID
    Map<Long, Integer> reservedQuantities,    // 상품ID -> 예약 수량
    LocalDateTime reservedAt                  // 예약 시각
) {}
```

**발행 시점**: 주문의 모든 상품에 대한 재고 예약이 완료되었을 때

### 5. DeliveryShippedEvent

배송이 시작되었을 때 발행되는 이벤트입니다.

```java
public record DeliveryShippedEvent(
    String trackingNumber,        // 운송장 번호
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    String carrier,               // 택배사
    LocalDateTime shippedAt       // 발송 시각
) {}
```

**발행 시점**: 상품이 출고되어 배송이 시작되었을 때

**소비자**:
- `notification-service`: 배송 시작 알림 발송

### 6. OrderConfirmedEvent

주문이 최종 확정되었을 때 발행되는 이벤트입니다.

```java
public record OrderConfirmedEvent(
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    LocalDateTime confirmedAt     // 확정 시각
) {}
```

### 7. OrderCancelledEvent

주문이 취소되었을 때 발행되는 이벤트입니다.

```java
public record OrderCancelledEvent(
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    String cancelReason,          // 취소 사유
    LocalDateTime cancelledAt     // 취소 시각
) {}
```

## 이벤트 패키지 구조

```
services/common-library/
└── src/main/java/com/portal/universe/common/event/
    └── shopping/
        ├── OrderCreatedEvent.java
        ├── OrderConfirmedEvent.java
        ├── OrderCancelledEvent.java
        ├── PaymentCompletedEvent.java
        ├── PaymentFailedEvent.java
        ├── InventoryReservedEvent.java
        └── DeliveryShippedEvent.java
```

## Best Practices

### 1. Event Versioning

이벤트 스키마가 변경될 경우:

```java
// V1 (기존)
public record OrderCreatedEvent(...) {}

// V2 (신규 - 새 필드 추가 시 기본값 제공)
public record OrderCreatedEventV2(
    ...기존 필드...,
    String source  // 새 필드 - 기본값 처리 필요
) {}
```

### 2. Idempotency

소비자는 동일한 이벤트가 중복 수신되어도 안전하게 처리해야 합니다:

```java
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 이미 처리된 주문인지 확인
    if (notificationRepository.existsByOrderNumber(event.orderNumber())) {
        log.info("Duplicate event ignored: {}", event.orderNumber());
        return;
    }
    // 처리 로직
}
```

### 3. Event Enrichment vs Event Sourcing

Portal Universe는 **Event Notification** 패턴을 사용합니다:
- 이벤트에 필요한 데이터를 모두 포함
- 소비자가 추가 API 호출 없이 처리 가능
- 단, 민감한 정보는 제외하고 필요시 별도 조회

## 관련 문서

- [Event Producer](./event-producer.md) - 이벤트 발행 방법
- [Event Consumer](./event-consumer.md) - 이벤트 소비 방법
- [DLQ Handling](./dlq-handling.md) - 실패한 이벤트 처리
