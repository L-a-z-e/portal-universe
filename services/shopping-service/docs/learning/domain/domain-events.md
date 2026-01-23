# Domain Events

## 1. 개요

Shopping Service는 Kafka를 통해 도메인 이벤트를 발행하여 다른 서비스(notification-service 등)와 느슨하게 결합된 통신을 수행합니다.

## 2. 이벤트 발행 구조

### ShoppingEventPublisher

```java
@Component
public class ShoppingEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, key={}, offset={}",
                    topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: topic={}, key={}", topic, key);
            }
        });
    }
}
```

## 3. 도메인 이벤트 목록

### OrderCreatedEvent

주문이 생성되었을 때 발행됩니다.

```java
public record OrderCreatedEvent(
    String orderNumber,          // 주문 번호
    String userId,               // 주문자 ID
    BigDecimal totalAmount,      // 총 주문 금액
    int itemCount,               // 상품 종류 수
    List<OrderItemInfo> items,   // 주문 항목 목록
    LocalDateTime createdAt      // 생성 시간
) {
    public record OrderItemInfo(
        Long productId,          // 상품 ID
        String productName,      // 상품명
        int quantity,            // 수량
        BigDecimal price         // 단가
    ) {}
}
```

**Topic:** `shopping.order.created`
**Key:** orderNumber
**발행 시점:** 주문 생성 완료 후

---

### OrderConfirmedEvent

주문이 확정(재고 예약 완료)되었을 때 발행됩니다.

```java
public record OrderConfirmedEvent(
    String orderNumber,          // 주문 번호
    String userId,               // 주문자 ID
    BigDecimal totalAmount,      // 총 금액
    LocalDateTime confirmedAt    // 확정 시간
) {}
```

**Topic:** `shopping.order.confirmed`
**Key:** orderNumber
**발행 시점:** 재고 예약 성공 후

---

### OrderCancelledEvent

주문이 취소되었을 때 발행됩니다.

```java
public record OrderCancelledEvent(
    String orderNumber,          // 주문 번호
    String userId,               // 주문자 ID
    BigDecimal totalAmount,      // 취소 금액
    String cancelReason,         // 취소 사유
    LocalDateTime cancelledAt    // 취소 시간
) {}
```

**Topic:** `shopping.order.cancelled`
**Key:** orderNumber
**발행 시점:** 주문 취소 완료 후

---

### PaymentCompletedEvent

결제가 완료되었을 때 발행됩니다.

```java
public record PaymentCompletedEvent(
    String paymentNumber,        // 결제 번호
    String orderNumber,          // 주문 번호
    String userId,               // 결제자 ID
    BigDecimal amount,           // 결제 금액
    String paymentMethod,        // 결제 수단
    String pgTransactionId,      // PG 거래 ID
    LocalDateTime paidAt         // 결제 완료 시간
) {}
```

**Topic:** `shopping.payment.completed`
**Key:** paymentNumber
**발행 시점:** 결제 성공 후

---

### PaymentFailedEvent

결제가 실패했을 때 발행됩니다.

```java
public record PaymentFailedEvent(
    String paymentNumber,        // 결제 번호
    String orderNumber,          // 주문 번호
    String userId,               // 결제자 ID
    BigDecimal amount,           // 시도한 금액
    String failureReason,        // 실패 사유
    LocalDateTime failedAt       // 실패 시간
) {}
```

**Topic:** `shopping.payment.failed`
**Key:** paymentNumber
**발행 시점:** 결제 실패 후

---

### InventoryReservedEvent

재고가 예약되었을 때 발행됩니다.

```java
public record InventoryReservedEvent(
    String orderNumber,                    // 주문 번호
    String userId,                         // 주문자 ID
    Map<Long, Integer> reservedQuantities, // 상품ID → 예약수량
    LocalDateTime reservedAt               // 예약 시간
) {}
```

**Topic:** `shopping.inventory.reserved`
**Key:** orderNumber
**발행 시점:** 재고 예약 성공 후

---

### DeliveryShippedEvent

배송이 시작되었을 때 발행됩니다.

```java
public record DeliveryShippedEvent(
    String trackingNumber,       // 송장 번호
    String orderNumber,          // 주문 번호
    String userId,               // 수령인 ID
    String carrierName,          // 배송사명
    LocalDateTime shippedAt      // 발송 시간
) {}
```

**Topic:** `shopping.delivery.shipped`
**Key:** trackingNumber
**발행 시점:** 배송 발송 처리 후

## 4. Kafka Topic 설정

```java
public class KafkaConfig {
    // Order Events
    public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";

    // Payment Events
    public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";

    // Inventory Events
    public static final String TOPIC_INVENTORY_RESERVED = "shopping.inventory.reserved";

    // Delivery Events
    public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
}
```

## 5. 이벤트 흐름

### 정상 주문 흐름

```
┌─────────────────┐
│  Order Created  │
└────────┬────────┘
         │ OrderCreatedEvent
         v
┌─────────────────┐
│ Stock Reserved  │
└────────┬────────┘
         │ InventoryReservedEvent
         │ OrderConfirmedEvent
         v
┌─────────────────┐
│Payment Completed│
└────────┬────────┘
         │ PaymentCompletedEvent
         v
┌─────────────────┐
│Delivery Shipped │
└────────┬────────┘
         │ DeliveryShippedEvent
         v
       [완료]
```

### 실패 시 보상 이벤트

```
┌─────────────────┐
│ Payment Failed  │
└────────┬────────┘
         │ PaymentFailedEvent
         v
┌─────────────────┐
│ Stock Released  │ (보상 트랜잭션)
└────────┬────────┘
         │
         v
┌─────────────────┐
│Order Cancelled  │
└────────┬────────┘
         │ OrderCancelledEvent
         v
       [종료]
```

## 6. 이벤트 소비자 (Notification Service)

notification-service에서 이벤트를 소비하여 알림을 발송합니다:

```java
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 주문 확인 이메일/SMS 발송
    notificationService.sendOrderConfirmation(event.userId(), event.orderNumber());
}

@KafkaListener(topics = "shopping.payment.completed")
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    // 결제 완료 알림 발송
    notificationService.sendPaymentReceipt(event.userId(), event.paymentNumber());
}

@KafkaListener(topics = "shopping.delivery.shipped")
public void handleDeliveryShipped(DeliveryShippedEvent event) {
    // 배송 시작 알림 발송
    notificationService.sendShipmentNotification(event.userId(), event.trackingNumber());
}
```

## 7. 이벤트 설계 원칙

### 1. 불변성 (Immutability)
- 모든 이벤트는 `record`로 정의하여 불변
- 이벤트 발행 후 수정 불가

### 2. 자기 완결성 (Self-Contained)
- 이벤트만으로 필요한 정보 파악 가능
- 추가 조회 없이 처리 가능하도록 설계

### 3. 멱등성 고려
- 동일 이벤트가 중복 처리되어도 결과 동일
- Consumer는 중복 처리 대비 필요

### 4. Key 기반 파티셔닝
- 관련 이벤트가 동일 파티션에 저장되도록 Key 설정
- 주문 관련 이벤트: orderNumber를 Key로 사용

## 8. 소스 위치

- Publisher: `event/ShoppingEventPublisher.java`
- Event DTOs: `common-library/src/main/java/com/portal/universe/common/event/shopping/`
  - `OrderCreatedEvent.java`
  - `OrderConfirmedEvent.java`
  - `OrderCancelledEvent.java`
  - `PaymentCompletedEvent.java`
  - `PaymentFailedEvent.java`
  - `InventoryReservedEvent.java`
  - `DeliveryShippedEvent.java`
- Kafka Config: `common/config/KafkaConfig.java`
