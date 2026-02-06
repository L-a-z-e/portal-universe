# Refund Process (환불 처리)

## 개요

주문 취소 및 환불 처리 플로우를 설명합니다.
결제 상태에 따른 처리 분기와 재고 복원, PG사 환불 요청을 포함합니다.

## 환불 가능 조건

### Order Status

```java
public enum OrderStatus {
    PENDING("대기 중"),
    CONFIRMED("확정"),      // 취소 가능
    PAID("결제 완료"),       // 환불 가능
    SHIPPING("배송 중"),     // 환불 불가 (반품 처리 필요)
    DELIVERED("배송 완료"),  // 환불 불가 (반품 처리 필요)
    CANCELLED("취소됨"),
    REFUNDED("환불됨");

    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isRefundable() {
        return this == PAID;
    }
}
```

### Payment Status

```java
public enum PaymentStatus {
    PENDING("대기 중"),      // 취소 가능
    PROCESSING("처리 중"),   // 취소 가능
    COMPLETED("완료"),       // 환불 가능
    FAILED("실패"),
    CANCELLED("취소"),
    REFUNDED("환불");

    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    public boolean isRefundable() {
        return this == COMPLETED;
    }
}
```

## 환불 처리 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        환불 처리 흐름                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐                                               │
│  │ 환불 요청   │                                               │
│  └──────┬──────┘                                               │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────────────┐                                       │
│  │ 주문 상태 확인      │                                       │
│  └──────────┬──────────┘                                       │
│             │                                                   │
│    ┌────────┴────────┐                                         │
│    │                 │                                         │
│    ▼                 ▼                                         │
│ ┌──────────┐   ┌──────────┐                                   │
│ │ CONFIRMED│   │   PAID   │                                   │
│ │(결제 전) │   │(결제 후) │                                   │
│ └────┬─────┘   └────┬─────┘                                   │
│      │              │                                         │
│      ▼              ▼                                         │
│ ┌──────────┐   ┌──────────┐                                   │
│ │ 재고 해제│   │ PG 환불  │                                   │
│ └────┬─────┘   └────┬─────┘                                   │
│      │              │                                         │
│      │              ▼                                         │
│      │         ┌──────────┐                                   │
│      │         │ 재고 복원│                                   │
│      │         └────┬─────┘                                   │
│      │              │                                         │
│      ▼              ▼                                         │
│ ┌──────────────────────┐                                       │
│ │    주문 상태 변경     │                                       │
│ │ CANCELLED or REFUNDED│                                       │
│ └──────────────────────┘                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 주문 취소 (결제 전)

```java
@Transactional
public OrderResponse cancelOrder(String userId, String orderNumber, CancelOrderRequest request) {
    Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

    // 1. 본인 주문 확인
    if (!order.getUserId().equals(userId)) {
        throw new CustomBusinessException(ORDER_USER_MISMATCH);
    }

    // 2. 취소 가능 여부 확인
    if (!order.getStatus().isCancellable()) {
        throw new CustomBusinessException(ORDER_CANNOT_BE_CANCELLED);
    }

    // 3. Saga 상태 조회
    SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
            .orElse(null);

    // 4. 예약된 재고 해제
    try {
        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.releaseStockBatch(
                quantities,
                "ORDER_CANCEL",
                orderNumber,
                userId
        );
    } catch (Exception e) {
        log.error("Failed to release stock for order {}: {}", orderNumber, e.getMessage());
        // 재고 해제 실패해도 주문 취소는 진행
    }

    // 5. 주문 취소
    order.cancel(request.reason());
    Order savedOrder = orderRepository.save(order);

    // 6. Saga 상태 업데이트
    if (sagaState != null) {
        sagaState.markAsFailed("Order cancelled by user: " + request.reason());
        sagaStateRepository.save(sagaState);
    }

    log.info("Order cancelled: {} (user: {}, reason: {})",
            orderNumber, userId, request.reason());

    return OrderResponse.from(savedOrder);
}
```

## 결제 환불 (결제 후)

### PaymentService.refundPayment()

```java
@Transactional
public PaymentResponse refundPayment(String paymentNumber) {
    Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
            .orElseThrow(() -> new CustomBusinessException(PAYMENT_NOT_FOUND));

    return PaymentResponse.from(refundPaymentInternal(payment));
}

private Payment refundPaymentInternal(Payment payment) {
    // 1. 환불 가능 여부 확인
    if (!payment.getStatus().isRefundable()) {
        throw new CustomBusinessException(PAYMENT_REFUND_FAILED);
    }

    // 2. PG사 환불 요청
    PgResponse pgResponse = mockPGClient.refundPayment(
            payment.getPgTransactionId(),
            payment.getAmount()
    );

    if (pgResponse.success()) {
        // 3. 결제 상태 변경
        payment.refund(pgResponse.transactionId());
        paymentRepository.save(payment);

        // 4. 주문 환불 처리
        Order order = orderRepository.findByOrderNumber(payment.getOrderNumber())
                .orElse(null);
        if (order != null) {
            order.refund();
            orderRepository.save(order);
        }

        log.info("Payment refunded: {} (order: {})",
                payment.getPaymentNumber(), payment.getOrderNumber());
    } else {
        log.error("Failed to refund payment: {} (error: {})",
                payment.getPaymentNumber(), pgResponse.errorCode());
        throw new CustomBusinessException(PAYMENT_REFUND_FAILED);
    }

    return payment;
}
```

### MockPGClient.refundPayment()

```java
public PgResponse refundPayment(String pgTransactionId, BigDecimal refundAmount) {
    log.info("MockPG: Refunding payment - txId: {}", pgTransactionId);

    MockTransaction transaction = transactions.get(pgTransactionId);

    // 1. 거래 존재 확인
    if (transaction == null) {
        return PgResponse.failure("TX_NOT_FOUND", "Transaction not found");
    }

    // 2. 거래 상태 확인
    if (!"COMPLETED".equals(transaction.status())) {
        return PgResponse.failure("INVALID_STATUS",
            "Cannot refund transaction in " + transaction.status() + " status");
    }

    // 3. 환불 금액 확인
    if (transaction.amount().compareTo(refundAmount) < 0) {
        return PgResponse.failure("INVALID_AMOUNT",
            "Refund amount exceeds original payment amount");
    }

    // 4. 환불 처리
    String refundTxId = "RF-" + generateTransactionId();
    transactions.put(pgTransactionId, new MockTransaction(
            transaction.transactionId(),
            transaction.paymentNumber(),
            transaction.amount(),
            transaction.method(),
            "REFUNDED"
    ));

    return PgResponse.success(refundTxId);
}
```

## 재고 복원

### 주문 취소 시 (예약 해제)

```java
// 예약 재고 → 가용 재고로 복원
inventoryService.releaseStockBatch(quantities, "ORDER_CANCEL", orderNumber, userId);
```

```java
@Transactional
public InventoryResponse releaseStock(Long productId, int quantity,
                                       String referenceType, String referenceId, String userId) {
    Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow(() -> new CustomBusinessException(INVENTORY_NOT_FOUND));

    int previousAvailable = inventory.getAvailableQuantity();
    int previousReserved = inventory.getReservedQuantity();

    // 예약 재고에서 가용 재고로 이동
    inventory.release(quantity);

    Inventory savedInventory = inventoryRepository.save(inventory);

    // 재고 이동 이력 기록
    recordMovement(savedInventory, MovementType.RELEASE, quantity,
            previousAvailable, savedInventory.getAvailableQuantity(),
            previousReserved, savedInventory.getReservedQuantity(),
            referenceType, referenceId, "Stock released due to cancellation", userId);

    return InventoryResponse.from(savedInventory);
}
```

### 환불 시 (재고 복원은 반품 처리 필요)

결제 완료 후 환불 시에는 재고가 이미 차감되었으므로,
반품 처리 시 별도로 `inventory.returnStock(quantity)`를 호출해야 합니다.

```java
public void returnStock(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(INVALID_STOCK_QUANTITY);
    }
    this.availableQuantity += quantity;
    this.totalQuantity += quantity;
}
```

## Order Entity 상태 변경 메서드

```java
@Entity
public class Order {

    /**
     * 주문을 취소합니다.
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 주문을 환불 처리합니다.
     */
    public void refund() {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.REFUNDED;
    }
}
```

## Payment Entity 상태 변경 메서드

```java
@Entity
public class Payment {

    /**
     * 결제를 취소합니다.
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(PAYMENT_CANNOT_BE_CANCELLED);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
    }

    /**
     * 결제를 환불합니다.
     */
    public void refund(String pgTransactionId) {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(PAYMENT_REFUND_FAILED);
        }
        this.status = PaymentStatus.REFUNDED;
        this.pgTransactionId = pgTransactionId;
        this.refundedAt = LocalDateTime.now();
    }
}
```

## Error Codes

| Code | 설명 |
|------|------|
| `ORDER_NOT_FOUND` | 주문을 찾을 수 없음 |
| `ORDER_USER_MISMATCH` | 주문자와 요청자 불일치 |
| `ORDER_CANNOT_BE_CANCELLED` | 취소할 수 없는 상태 |
| `PAYMENT_NOT_FOUND` | 결제 정보를 찾을 수 없음 |
| `PAYMENT_CANNOT_BE_CANCELLED` | 취소할 수 없는 결제 상태 |
| `PAYMENT_REFUND_FAILED` | 환불 처리 실패 |

## 부분 환불 (향후 구현)

현재는 전액 환불만 지원하며, 부분 환불은 향후 구현 예정입니다.

```java
// 예시: 부분 환불 구조
public PaymentResponse partialRefund(String paymentNumber, BigDecimal refundAmount) {
    Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
            .orElseThrow();

    // 이미 환불된 금액 확인
    BigDecimal totalRefunded = refundRepository.getTotalRefundedAmount(payment.getId());
    BigDecimal maxRefundable = payment.getAmount().subtract(totalRefunded);

    if (refundAmount.compareTo(maxRefundable) > 0) {
        throw new CustomBusinessException(REFUND_AMOUNT_EXCEEDED);
    }

    // 부분 환불 처리...
}
```

## 관련 파일

- `/order/service/OrderServiceImpl.java` - 주문 취소 로직
- `/payment/service/PaymentServiceImpl.java` - 결제 환불 로직
- `/payment/pg/MockPGClient.java` - PG 환불 요청
- `/inventory/service/InventoryServiceImpl.java` - 재고 해제
- `/order/domain/Order.java` - 주문 상태 변경
- `/payment/domain/Payment.java` - 결제 상태 변경
