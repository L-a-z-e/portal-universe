# PaymentCompletedEvent 상세

## 개요

`PaymentCompletedEvent`는 결제가 성공적으로 완료되었을 때 발행되는 이벤트입니다. 이 이벤트는 Order Saga의 중요한 단계이며, 주문 확정과 배송 프로세스의 트리거 역할을 합니다.

## 이벤트 정의

```java
package com.portal.universe.common.event.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 시 발행되는 이벤트입니다.
 */
public record PaymentCompletedEvent(
    String paymentNumber,         // 결제 번호
    String orderNumber,           // 주문 번호
    String userId,                // 사용자 ID
    BigDecimal amount,            // 결제 금액
    String paymentMethod,         // 결제 수단
    String pgTransactionId,       // PG사 거래 ID
    LocalDateTime paidAt          // 결제 완료 시각
) {}
```

## 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `paymentNumber` | String | O | 결제 고유 번호 (예: "PAY-20250122-001") |
| `orderNumber` | String | O | 연관된 주문 번호 |
| `userId` | String | O | 결제한 사용자의 ID |
| `amount` | BigDecimal | O | 실제 결제된 금액 |
| `paymentMethod` | String | O | 결제 수단 (CARD, BANK_TRANSFER 등) |
| `pgTransactionId` | String | O | PG사에서 발급한 거래 ID |
| `paidAt` | LocalDateTime | O | 결제 완료 시각 |

## 결제 수단 (PaymentMethod)

```java
public enum PaymentMethod {
    CARD,           // 신용/체크카드
    BANK_TRANSFER,  // 실시간 계좌이체
    VIRTUAL_ACCOUNT,// 가상계좌
    KAKAO_PAY,      // 카카오페이
    NAVER_PAY,      // 네이버페이
    TOSS            // 토스
}
```

## 이벤트 발행

### 발행 시점

PG사로부터 결제 승인 응답을 받은 직후 발행됩니다:

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShoppingEventPublisher eventPublisher;
    private final PGClient pgClient;

    @Override
    @Transactional
    public PaymentResponse processPayment(String userId, ProcessPaymentRequest request) {
        // 1. 주문 검증
        Order order = orderRepository.findByOrderNumber(request.orderNumber())
            .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

        // 2. 결제 엔티티 생성
        Payment payment = Payment.create(order, request.paymentMethod());
        payment = paymentRepository.save(payment);

        // 3. PG사 결제 요청
        PgResponse pgResponse = pgClient.requestPayment(payment, request);

        if (pgResponse.isSuccess()) {
            // 4. 결제 완료 처리
            payment.complete(pgResponse.getTransactionId());
            paymentRepository.save(payment);

            // 5. 이벤트 발행
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getPaymentNumber(),
                order.getOrderNumber(),
                userId,
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                pgResponse.getTransactionId(),
                LocalDateTime.now()
            );
            eventPublisher.publishPaymentCompleted(event);

            return PaymentResponse.from(payment);
        } else {
            // 결제 실패 처리
            payment.fail(pgResponse.getErrorMessage());
            paymentRepository.save(payment);

            // PaymentFailedEvent 발행
            eventPublisher.publishPaymentFailed(PaymentFailedEvent.from(payment));

            throw new CustomBusinessException(PAYMENT_PROCESSING_FAILED);
        }
    }
}
```

## Topic 정보

| 속성 | 값 |
|------|-----|
| Topic Name | `shopping.payment.completed` |
| Partitions | 3 |
| Replication Factor | 1 (운영: 3 권장) |
| Message Key | paymentNumber |

## 이벤트 소비자

### 1. Notification Service

결제 완료 알림을 사용자에게 발송합니다:

```java
@KafkaListener(topics = "shopping.payment.completed", groupId = "notification-group")
public void handlePaymentCompleted(NotificationEvent event) {
    log.info("Received payment completed event: userId={}", event.getUserId());

    Notification notification = notificationService.create(
        event.getUserId(),
        NotificationType.PAYMENT_COMPLETED,
        "결제가 완료되었습니다",
        String.format("결제금액: %s원 / 결제수단: %s",
            event.getAmount(), event.getPaymentMethod()),
        "/orders/" + event.getOrderNumber(),
        event.getPaymentNumber(),
        "PAYMENT"
    );

    pushService.push(notification);
}
```

### 2. Order Service

주문 상태를 업데이트하고 후속 프로세스를 시작합니다:

```java
@KafkaListener(topics = "shopping.payment.completed", groupId = "order-group")
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    log.info("Processing payment completed: orderNumber={}", event.orderNumber());

    // 주문 상태 업데이트 및 Saga 다음 단계 실행
    orderService.completeOrderAfterPayment(event.orderNumber());
}
```

### 3. Delivery Service

배송 준비를 시작합니다:

```java
@KafkaListener(topics = "shopping.payment.completed", groupId = "delivery-group")
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    log.info("Creating delivery for order: {}", event.orderNumber());

    // 배송 정보 생성
    deliveryService.createDelivery(event.orderNumber());
}
```

## Saga 연계

PaymentCompletedEvent는 Order Saga의 중요한 체크포인트입니다:

```
[InventoryReservedEvent]
        │
        ▼
┌───────────────────────────────────────┐
│ Payment Processing                    │
│ - PG사 결제 요청                        │
│ - 결제 승인 대기                        │
└───────────────────────────────────────┘
        │
        ├─── 성공 ──→ [PaymentCompletedEvent]
        │                    │
        │                    ▼
        │            ┌───────────────────┐
        │            │ Delivery Creation │
        │            │ - 배송 정보 생성    │
        │            │ - 출고 준비        │
        │            └───────────────────┘
        │
        └─── 실패 ──→ [PaymentFailedEvent]
                             │
                             ▼
                     ┌───────────────────┐
                     │ Compensation      │
                     │ - 재고 예약 해제   │
                     │ - 주문 취소 처리   │
                     └───────────────────┘
```

## PaymentFailedEvent (실패 케이스)

결제가 실패했을 때 발행되는 이벤트입니다:

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

### 실패 사유 예시

| 코드 | 설명 |
|------|------|
| `INSUFFICIENT_BALANCE` | 잔액 부족 |
| `CARD_EXPIRED` | 카드 유효기간 만료 |
| `CARD_BLOCKED` | 카드 정지 |
| `PG_CONNECTION_ERROR` | PG사 연결 오류 |
| `TIMEOUT` | 결제 시간 초과 |
| `USER_CANCELLED` | 사용자 취소 |

## 페이로드 예시

### PaymentCompletedEvent

```json
{
  "paymentNumber": "PAY-20250122-001",
  "orderNumber": "ORD-20250122-001",
  "userId": "user-12345",
  "amount": 150000.00,
  "paymentMethod": "CARD",
  "pgTransactionId": "PG_TXN_20250122143000_ABC123",
  "paidAt": "2025-01-22T14:35:00"
}
```

### PaymentFailedEvent

```json
{
  "paymentNumber": "PAY-20250122-002",
  "orderNumber": "ORD-20250122-002",
  "userId": "user-67890",
  "amount": 250000.00,
  "paymentMethod": "CARD",
  "failureReason": "INSUFFICIENT_BALANCE",
  "failedAt": "2025-01-22T14:40:00"
}
```

## 에러 처리

### 발행 실패 시

결제 완료 이벤트는 매우 중요하므로 동기 발행을 고려합니다:

```java
public void publishPaymentCompleted(PaymentCompletedEvent event) {
    try {
        // 동기 발행 - 실패 시 예외 발생
        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event)
            .get(10, TimeUnit.SECONDS);

        log.info("PaymentCompletedEvent published: {}", event.paymentNumber());
    } catch (Exception e) {
        log.error("Failed to publish PaymentCompletedEvent: {}", event.paymentNumber(), e);

        // Outbox 테이블에 저장하여 나중에 재시도
        outboxRepository.save(OutboxEvent.from(event));

        // 알림 전송
        alertService.critical("PAYMENT_EVENT_PUBLISH_FAILED", event.paymentNumber());
    }
}
```

### 소비 실패 시

재시도 후 DLQ로 이동:

```
shopping.payment.completed.DLT  (Dead Letter Topic)
```

## 보상 트랜잭션 (PaymentFailedEvent 처리)

```java
@KafkaListener(topics = "shopping.payment.failed", groupId = "order-group")
public void handlePaymentFailed(PaymentFailedEvent event) {
    log.info("Processing payment failure: orderNumber={}", event.orderNumber());

    // 1. 주문 상태를 결제 실패로 변경
    orderService.markPaymentFailed(event.orderNumber(), event.failureReason());

    // 2. 재고 예약 해제 (Saga 보상)
    inventoryService.releaseReservation(event.orderNumber());

    // 3. 사용자에게 알림
    notificationService.notifyPaymentFailed(event.userId(), event.orderNumber());
}
```

## 모니터링 포인트

1. **결제 성공률**: 성공/실패 이벤트 비율
2. **평균 결제 시간**: 결제 요청부터 완료까지 소요 시간
3. **결제 수단별 통계**: 각 결제 수단의 사용 빈도
4. **실패 사유 분석**: 주요 실패 원인 파악

### Grafana 대시보드 예시

```promql
# 결제 성공률
sum(rate(payment_completed_total[5m])) /
sum(rate(payment_attempted_total[5m])) * 100

# 결제 수단별 사용량
sum by (payment_method) (rate(payment_completed_total[1h]))
```

## 관련 이벤트

| 이벤트 | 관계 | 설명 |
|--------|------|------|
| `OrderCreatedEvent` | 선행 | 주문 생성 시 발행 |
| `InventoryReservedEvent` | 선행 | 재고 예약 완료 시 발행 |
| `PaymentFailedEvent` | 대안 | 결제 실패 시 발행 |
| `DeliveryShippedEvent` | 후속 | 배송 시작 시 발행 |

## 관련 문서

- [Shopping Events](./shopping-events.md) - 전체 이벤트 목록
- [OrderCreatedEvent](./order-created-event.md) - 주문 생성 이벤트
- [DLQ Handling](./dlq-handling.md) - 실패 이벤트 처리
