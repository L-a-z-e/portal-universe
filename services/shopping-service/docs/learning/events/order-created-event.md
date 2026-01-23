# OrderCreatedEvent 상세

## 개요

`OrderCreatedEvent`는 주문이 성공적으로 생성되었을 때 발행되는 이벤트입니다. 이 이벤트는 주문 생성 Saga의 시작점이며, 다양한 후속 프로세스의 트리거 역할을 합니다.

## 이벤트 정의

```java
package com.portal.universe.common.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 시 발행되는 이벤트입니다.
 */
public record OrderCreatedEvent(
    String orderNumber,           // 주문 번호
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

## 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderNumber` | String | O | 주문 고유 번호 (예: "ORD-20250122-001") |
| `userId` | String | O | 주문한 사용자의 ID |
| `totalAmount` | BigDecimal | O | 주문 총 금액 |
| `itemCount` | int | O | 주문 항목 수 |
| `items` | List | O | 주문 상품 상세 목록 |
| `createdAt` | LocalDateTime | O | 주문 생성 시각 |

### OrderItemInfo 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `productId` | Long | O | 상품 ID |
| `productName` | String | O | 상품명 |
| `quantity` | int | O | 주문 수량 |
| `price` | BigDecimal | O | 상품 단가 |

## 이벤트 발행

### 발행 시점

주문이 성공적으로 생성된 직후 발행됩니다:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingEventPublisher eventPublisher;

    @Override
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 1. 주문 생성
        Order order = Order.create(userId, cart, request.shippingAddress().toEntity());
        order = orderRepository.save(order);

        // 2. 이벤트 발행
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        eventPublisher.publishOrderCreated(event);

        return OrderResponse.from(order);
    }
}
```

### 이벤트 생성 헬퍼 메서드

```java
public record OrderCreatedEvent(...) {

    public static OrderCreatedEvent from(Order order) {
        List<OrderItemInfo> items = order.getItems().stream()
            .map(item -> new OrderItemInfo(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
            ))
            .toList();

        return new OrderCreatedEvent(
            order.getOrderNumber(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getItems().size(),
            items,
            LocalDateTime.now()
        );
    }
}
```

## Topic 정보

| 속성 | 값 |
|------|-----|
| Topic Name | `shopping.order.created` |
| Partitions | 3 |
| Replication Factor | 1 (운영: 3 권장) |
| Message Key | orderNumber |

### Topic 설정

```java
@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name("shopping.order.created")
            .partitions(3)
            .replicas(1)
            .build();
}
```

## 이벤트 소비자

### 1. Notification Service

주문 확인 알림을 사용자에게 발송합니다:

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(NotificationEvent event) {
    log.info("Received order created event: userId={}", event.getUserId());

    Notification notification = notificationService.create(
        event.getUserId(),
        NotificationType.ORDER_CREATED,
        "주문이 완료되었습니다",
        String.format("주문번호: %s, 금액: %s원", event.getOrderNumber(), event.getAmount()),
        "/orders/" + event.getOrderNumber(),
        event.getOrderNumber(),
        "ORDER"
    );

    pushService.push(notification);
}
```

### 2. Inventory Service (내부)

재고 예약 프로세스를 시작합니다:

```java
// OrderSagaOrchestrator 내부에서 처리
@Override
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 1. 주문 생성
    Order order = createOrderEntity(userId, request);

    // 2. Saga 시작 - 재고 예약
    sagaOrchestrator.startOrderSaga(order);

    // 3. 이벤트 발행
    eventPublisher.publishOrderCreated(OrderCreatedEvent.from(order));

    return OrderResponse.from(order);
}
```

### 3. Analytics Service (잠재적)

주문 통계 및 분석용 데이터 수집:

```java
@KafkaListener(topics = "shopping.order.created", groupId = "analytics-group")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 주문 통계 업데이트
    orderAnalytics.recordOrder(
        event.userId(),
        event.totalAmount(),
        event.itemCount(),
        event.createdAt()
    );
}
```

## Saga 연계

OrderCreatedEvent는 Order Saga의 시작점입니다:

```
[OrderCreatedEvent]
        │
        ▼
┌───────────────────────────────────────┐
│ Step 1: Inventory Reservation         │
│ - 각 상품의 재고 예약                   │
│ - 실패 시: Saga 중단                   │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│ Step 2: Payment Processing            │
│ - 결제 대기 상태로 전환                 │
│ - 사용자의 결제 요청 대기               │
└───────────────────────────────────────┘
        │
        ▼
[InventoryReservedEvent]
```

## 페이로드 예시

```json
{
  "orderNumber": "ORD-20250122-001",
  "userId": "user-12345",
  "totalAmount": 150000.00,
  "itemCount": 2,
  "items": [
    {
      "productId": 101,
      "productName": "무선 이어폰",
      "quantity": 1,
      "price": 89000.00
    },
    {
      "productId": 205,
      "productName": "충전 케이블",
      "quantity": 2,
      "price": 30500.00
    }
  ],
  "createdAt": "2025-01-22T14:30:00"
}
```

## 에러 처리

### 발행 실패 시

```java
public void publishOrderCreated(OrderCreatedEvent event) {
    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(TOPIC_ORDER_CREATED, event.orderNumber(), event);

    future.whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Failed to publish OrderCreatedEvent: orderNumber={}",
                event.orderNumber(), ex);

            // 알림 전송 (Slack, PagerDuty 등)
            alertService.sendAlert(
                "ORDER_EVENT_PUBLISH_FAILED",
                "주문 이벤트 발행 실패: " + event.orderNumber()
            );
        }
    });
}
```

### 소비 실패 시

ErrorHandler에 의해 재시도 후 DLQ로 이동:

```
shopping.order.created.DLT  (Dead Letter Topic)
```

## 모니터링 포인트

1. **발행 지연 시간**: 주문 생성부터 이벤트 발행까지의 시간
2. **Consumer Lag**: 처리 대기 중인 이벤트 수
3. **처리 실패율**: DLQ로 이동한 이벤트 비율
4. **평균 처리 시간**: 이벤트 처리에 소요되는 시간

## Best Practices

1. **필요한 정보만 포함**: 민감 정보(카드번호 등)는 제외
2. **불변 객체 사용**: Java Record로 immutability 보장
3. **버전 관리**: 스키마 변경 시 하위 호환성 유지
4. **멱등성**: 소비자는 중복 이벤트 처리에 안전해야 함

## 관련 이벤트

| 이벤트 | 관계 | 설명 |
|--------|------|------|
| `InventoryReservedEvent` | 후속 | 재고 예약 완료 시 발행 |
| `PaymentCompletedEvent` | 후속 | 결제 완료 시 발행 |
| `OrderCancelledEvent` | 대안 | 주문 취소 시 발행 |

## 관련 문서

- [Shopping Events](./shopping-events.md) - 전체 이벤트 목록
- [PaymentCompletedEvent](./payment-completed-event.md) - 결제 완료 이벤트
- [DLQ Handling](./dlq-handling.md) - 실패 이벤트 처리
