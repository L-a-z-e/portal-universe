# 주문 전체 흐름

## 학습 목표
- 주문 생성부터 완료까지 전체 프로세스 이해
- Saga 패턴 적용 방법 학습
- 각 단계별 도메인 이벤트 발행 시점 파악

---

## 1. 주문 흐름 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ORDER LIFECYCLE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐         │
│    │ 장바구니  │────▶│ 주문생성  │────▶│ 재고예약  │────▶│  결제    │         │
│    │ checkout │     │ PENDING  │     │ Saga #1  │     │ Saga #2  │         │
│    └──────────┘     └──────────┘     └──────────┘     └──────────┘         │
│                                                             │               │
│                                                             ▼               │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐         │
│    │ 배송완료  │◀────│ 배송시작  │◀────│ 배송생성  │◀────│ 재고확정  │         │
│    │DELIVERED │     │ SHIPPING │     │ Saga #4  │     │ Saga #3  │         │
│    └──────────┘     └──────────┘     └──────────┘     └──────────┘         │
│                                                                              │
│    [취소 가능]                          [환불 가능]                          │
│    PENDING ───────────────────────────▶ CANCELLED                           │
│    CONFIRMED ─────────────────────────▶ CANCELLED                           │
│    PAID ──────────────────────────────▶ REFUNDED                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 상세 흐름

### Phase 1: 장바구니 → 주문 생성

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 1: 장바구니 체크아웃 & 주문 생성                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client                   OrderService                Cart          │
│    │                          │                        │            │
│    │ POST /orders             │                        │            │
│    │────────────────────────▶│                        │            │
│    │                          │                        │            │
│    │                          │ findByUserId(ACTIVE)   │            │
│    │                          │───────────────────────▶│            │
│    │                          │◀───────────────────────│            │
│    │                          │                        │            │
│    │                          │ cart.checkout()        │            │
│    │                          │───────────────────────▶│            │
│    │                          │     CHECKED_OUT        │            │
│    │                          │◀───────────────────────│            │
│    │                          │                        │            │
│    │                          │                                     │
│    │                          │ Order.create()                      │
│    │                          │  • orderNumber 생성                 │
│    │                          │  • status = PENDING                 │
│    │                          │  • cartItems → orderItems           │
│    │                          │  • totalAmount 계산                 │
│    │                          │                                     │
│    │                          │ orderRepository.save()              │
│    │                          │                                     │
│    │◀────────────────────────│                                     │
│    │   OrderResponse          │                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**코드 예시:**
```java
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    public OrderResponse createOrder(OrderCreateRequest request, String userId) {
        // 1. 장바구니 조회 및 체크아웃
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseThrow(() -> new CustomBusinessException(CART_NOT_FOUND));
        cart.checkout();

        // 2. 주문 생성
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(userId)
            .status(OrderStatus.PENDING)
            .shippingAddress(Address.from(request))
            .build();

        // 3. 장바구니 항목 → 주문 항목
        cart.getItems().forEach(cartItem ->
            order.addItem(
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getPrice(),
                cartItem.getQuantity()
            )
        );

        // 4. 쿠폰 적용 (있으면)
        if (request.getUserCouponId() != null) {
            applyCoupon(order, request.getUserCouponId());
        }

        Order saved = orderRepository.save(order);

        // 5. Saga 시작
        sagaOrchestrator.startSaga(saved);

        return OrderResponse.from(saved);
    }
}
```

### Phase 2: Saga - 재고 예약

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 2: Saga Step 1 - 재고 예약                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  SagaOrchestrator        InventoryService         Inventory         │
│        │                       │                      │             │
│        │ Step 1: RESERVE       │                      │             │
│        │──────────────────────▶│                      │             │
│        │                       │                      │             │
│        │                       │ 각 상품별 재고 조회    │             │
│        │                       │─────────────────────▶│             │
│        │                       │                      │             │
│        │                       │ inventory.reserve(qty)│            │
│        │                       │─────────────────────▶│             │
│        │                       │  • availableQty -= qty│            │
│        │                       │  • reservedQty += qty │            │
│        │                       │◀─────────────────────│             │
│        │                       │                      │             │
│        │                       │ StockMovement 생성    │             │
│        │                       │  • type = RESERVE    │             │
│        │                       │  • referenceType = ORDER           │
│        │                       │  • referenceId = orderNumber       │
│        │                       │                                    │
│        │◀──────────────────────│                                    │
│        │   Reserved OK         │                                    │
│        │                       │                                    │
│        │ SagaState 업데이트     │                                    │
│        │  • completedSteps += "RESERVE_INVENTORY"                   │
│        │                                                             │
└─────────────────────────────────────────────────────────────────────┘
```

**보상 트랜잭션 (실패 시):**
```java
// Saga 보상: 재고 예약 해제
private void compensateReserveInventory(Order order) {
    order.getItems().forEach(item -> {
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
            .orElseThrow();

        inventory.release(item.getQuantity());

        stockMovementRepository.save(StockMovement.release(
            inventory,
            item.getQuantity(),
            "ORDER",
            order.getOrderNumber(),
            "주문 취소로 인한 예약 해제"
        ));
    });
}
```

### Phase 3: Saga - 결제 처리

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 3: Saga Step 2 - 결제 처리                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  SagaOrchestrator     PaymentService          PG Gateway            │
│        │                    │                      │                │
│        │ Step 2: PAYMENT    │                      │                │
│        │───────────────────▶│                      │                │
│        │                    │                      │                │
│        │                    │ Payment.create()     │                │
│        │                    │  • status = PENDING  │                │
│        │                    │                      │                │
│        │                    │ payment.startProcessing()             │
│        │                    │  • status = PROCESSING                │
│        │                    │                      │                │
│        │                    │ PG 결제 요청          │                │
│        │                    │─────────────────────▶│                │
│        │                    │                      │                │
│        │                    │◀─────────────────────│                │
│        │                    │   PG Response        │                │
│        │                    │                      │                │
│        │                    │ [성공]                │                │
│        │                    │ payment.complete(pgTransactionId)    │
│        │                    │  • status = COMPLETED │               │
│        │                    │  • paidAt = now()    │                │
│        │                    │                      │                │
│        │                    │ [실패]               │                │
│        │                    │ payment.fail(reason) │                │
│        │                    │  • status = FAILED   │                │
│        │                    │  ─▶ Saga 보상 시작   │                │
│        │                    │                      │                │
│        │◀───────────────────│                      │                │
│        │  Payment Result    │                      │                │
│        │                                                             │
│        │ SagaState 업데이트                                          │
│        │  • completedSteps += "PROCESS_PAYMENT"                     │
│        │                                                             │
│        │ 이벤트 발행                                                  │
│        │  • PaymentCompletedEvent                                   │
│        │  OR PaymentFailedEvent                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Phase 4: Saga - 재고 확정 & 배송 생성

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 4: Saga Step 3 & 4 - 재고 확정 & 배송 생성                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [Step 3: DEDUCT_INVENTORY]                                         │
│                                                                      │
│  InventoryService                                                    │
│        │                                                             │
│        │ inventory.deduct(qty)                                       │
│        │  • reservedQty -= qty                                      │
│        │  • totalQty -= qty                                         │
│        │                                                             │
│        │ StockMovement 생성                                          │
│        │  • type = DEDUCT                                           │
│        │  • referenceType = PAYMENT                                 │
│                                                                      │
│  [Step 4: CREATE_DELIVERY]                                          │
│                                                                      │
│  DeliveryService                                                     │
│        │                                                             │
│        │ Delivery.create()                                          │
│        │  • trackingNumber 생성                                     │
│        │  • status = PREPARING                                      │
│        │  • address = order.shippingAddress                         │
│        │                                                             │
│        │ DeliveryHistory 생성                                        │
│        │  • status = PREPARING                                      │
│        │  • description = "배송 준비 중"                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Phase 5: Saga - 주문 확정

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 5: Saga Step 5 - 주문 확정                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  SagaOrchestrator                 Order                             │
│        │                            │                               │
│        │ Step 5: CONFIRM_ORDER      │                               │
│        │───────────────────────────▶│                               │
│        │                            │                               │
│        │                            │ order.confirm()               │
│        │                            │  • status = CONFIRMED         │
│        │                            │                               │
│        │◀───────────────────────────│                               │
│        │                            │                               │
│        │ SagaState 완료             │                               │
│        │  • status = COMPLETED      │                               │
│        │  • completedAt = now()     │                               │
│        │                                                             │
│        │ 이벤트 발행                                                  │
│        │  • OrderConfirmedEvent                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Saga 상태 관리

### 3.1 SagaState 엔티티

```java
@Entity
public class SagaState {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String sagaId;           // SAGA-XXXXXXXX

    private Long orderId;
    private String orderNumber;
    private String currentStep;       // 현재 단계

    @Enumerated(EnumType.STRING)
    private SagaStatus status;        // STARTED, COMPLETED, COMPENSATING, FAILED

    private String completedSteps;    // "STEP1,STEP2,STEP3"
    private String lastErrorMessage;
    private int compensationAttempts; // 보상 시도 횟수 (max 3)

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

### 3.2 Saga 단계 정의

```java
public enum SagaStep {
    RESERVE_INVENTORY(true),      // 보상 가능
    PROCESS_PAYMENT(false),       // 외부 서비스 (보상 불가)
    DEDUCT_INVENTORY(true),       // 보상 가능
    CREATE_DELIVERY(true),        // 보상 가능
    CONFIRM_ORDER(false);         // 보상 불필요

    private final boolean compensatable;
}
```

### 3.3 보상 트랜잭션 흐름

```
[에러 발생: DEDUCT_INVENTORY 단계에서]

Completed Steps: RESERVE_INVENTORY, PROCESS_PAYMENT
                      ↓
                 보상 필요 단계 확인
                      ↓
           ┌─────────────────────────┐
           │ PROCESS_PAYMENT         │
           │   → 환불 요청 (PG)       │
           │   (보상 불가, 수동 처리)  │
           └─────────────────────────┘
                      ↓
           ┌─────────────────────────┐
           │ RESERVE_INVENTORY       │
           │   → inventory.release() │
           │   → StockMovement 기록  │
           └─────────────────────────┘
                      ↓
              SagaState.status = FAILED
```

---

## 4. 이벤트 발행 시점

### 4.1 이벤트 타임라인

```
주문 생성 (PENDING)
    │
    └─▶ OrderCreatedEvent
            {orderNumber, userId, items, totalAmount}

결제 완료 (COMPLETED)
    │
    └─▶ PaymentCompletedEvent
            {paymentNumber, orderNumber, amount, paymentMethod, paidAt}

결제 실패 (FAILED)
    │
    └─▶ PaymentFailedEvent
            {paymentNumber, orderNumber, failureReason}

주문 확정 (CONFIRMED)
    │
    └─▶ OrderConfirmedEvent
            {orderNumber, userId, confirmedAt}

배송 시작 (SHIPPING)
    │
    └─▶ DeliveryShippedEvent
            {trackingNumber, orderNumber, carrier, estimatedDeliveryDate}

주문 취소 (CANCELLED)
    │
    └─▶ OrderCancelledEvent
            {orderNumber, userId, cancelReason, cancelledAt}
```

### 4.2 ShoppingEventPublisher

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publish("shopping.order.created", event.orderNumber(), event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publish("shopping.payment.completed", event.paymentNumber(), event);
    }

    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        publish("shopping.delivery.shipped", event.trackingNumber(), event);
    }

    private void publish(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, key={}", topic, key);
            } else {
                log.error("Event publish failed: topic={}, error={}", topic, ex.getMessage());
            }
        });
    }
}
```

---

## 5. 취소 및 환불 처리

### 5.1 주문 취소 가능 상태

| 현재 상태 | 취소 가능 | 결과 상태 | 처리 |
|-----------|----------|----------|------|
| PENDING | ✓ | CANCELLED | 재고 예약 해제 |
| CONFIRMED | ✓ | CANCELLED | 재고 예약 해제 |
| PAID | ✓ | REFUNDED | 환불 + 재고 복구 |
| SHIPPING | △ | REFUNDED | PG 환불 필요 |
| DELIVERED | ✗ | - | 반품 절차 필요 |

### 5.2 취소 흐름

```java
@Transactional
public void cancelOrder(String orderNumber, String reason) {
    Order order = orderRepository.findByOrderNumber(orderNumber)
        .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

    // 1. 취소 가능 상태 확인
    order.validateCancellable();

    // 2. 재고 복구
    order.getItems().forEach(item -> {
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
            .orElseThrow();

        if (order.getStatus() == OrderStatus.PAID) {
            // 결제 완료 상태: 재고 반환
            inventory.returnStock(item.getQuantity());
        } else {
            // 예약 상태: 예약 해제
            inventory.release(item.getQuantity());
        }
    });

    // 3. 결제 환불 (결제된 경우)
    if (order.getStatus() == OrderStatus.PAID) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
            .orElseThrow();
        paymentService.refund(payment);
    }

    // 4. 쿠폰 복구 (적용된 경우)
    if (order.getAppliedUserCouponId() != null) {
        UserCoupon userCoupon = userCouponRepository
            .findById(order.getAppliedUserCouponId())
            .orElseThrow();
        userCoupon.restore();
    }

    // 5. 주문 취소
    order.cancel(reason);

    // 6. 이벤트 발행
    eventPublisher.publishOrderCancelled(OrderCancelledEvent.from(order));
}
```

---

## 6. 핵심 정리

| 단계 | 상태 변화 | 이벤트 | 보상 |
|------|----------|--------|------|
| 주문 생성 | → PENDING | OrderCreatedEvent | - |
| 재고 예약 | (Saga #1) | InventoryReservedEvent | release() |
| 결제 처리 | → (PROCESSING) → COMPLETED | PaymentCompletedEvent | refund() |
| 재고 확정 | (Saga #3) | - | addStock() |
| 배송 생성 | (Saga #4) | - | cancel() |
| 주문 확정 | → CONFIRMED | OrderConfirmedEvent | - |
| 배송 시작 | → SHIPPING | DeliveryShippedEvent | - |
| 배송 완료 | → DELIVERED | - | - |

---

## 다음 학습

- [재고 동시성 제어](./inventory-concurrency.md)
- [결제 연동](./payment-integration.md)
- [Saga 패턴 심화](../../patterns/saga-pattern-deep-dive.md)
