# Shopping Service 아키텍처

## 도메인 구조

```
shopping-service/
├── common/
│   ├── config/       # SecurityConfig, JpaConfig, KafkaConfig
│   ├── domain/       # Address (Value Object)
│   └── exception/    # ShoppingErrorCode
├── product/          # 상품 (Product)
├── cart/             # 장바구니 (Cart, CartItem)
├── order/            # 주문 (Order, OrderItem, Saga)
├── payment/          # 결제 (Payment, MockPGClient)
├── delivery/         # 배송 (Delivery, DeliveryHistory)
├── inventory/        # 재고 (Inventory, StockMovement)
└── event/            # ShoppingEventPublisher
```

## 데이터 모델

### Product

```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private boolean active;
}
```

### Cart & CartItem

```java
@Entity
public class Cart {
    @Id @GeneratedValue
    private Long id;
    private String userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    private List<CartItem> items;

    private CartStatus status;  // ACTIVE, CHECKED_OUT, ORDERED
}

@Entity
public class CartItem {
    @Id @GeneratedValue
    private Long id;
    private Long productId;
    private int quantity;
    private BigDecimal priceAtAddTime;
}
```

### Order

```java
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;
    private String orderNumber;
    private String userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @Embedded
    private Address shippingAddress;

    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
```

## 주문 상태 흐름

```
     ┌────────────┐
     │  PENDING   │ (주문 생성)
     └─────┬──────┘
           │ processPayment()
           ▼
     ┌────────────┐
     │   PAID     │ (결제 완료)
     └─────┬──────┘
           │ startDelivery()
           ▼
     ┌────────────┐
     │ DELIVERING │ (배송 중)
     └─────┬──────┘
           │ complete()
           ▼
     ┌────────────┐
     │ COMPLETED  │ (배송 완료)
     └────────────┘

  ※ PENDING, PAID 상태에서 취소 가능 → CANCELLED
```

## Saga 패턴 (주문 처리)

```
┌─────────────────────────────────────────────────────────┐
│                  OrderSagaOrchestrator                   │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│ 1. 재고 확인   │ ──▶ │ 2. 결제 처리   │ ──▶ │ 3. 주문 확정   │
│ (Inventory)   │     │ (Payment)      │     │ (Order)        │
└───────┬───────┘     └───────┬───────┘     └───────┬───────┘
        │                     │                     │
        │ Fail ──────────────▶│ Rollback ──────────▶│ Rollback
        │                     │                     │
        ▼                     ▼                     ▼
   재고 복구           결제 취소/환불          주문 취소
```

### SagaState

```java
@Entity
public class SagaState {
    @Id @GeneratedValue
    private Long id;
    private String orderId;
    private SagaStatus status;        // STARTED, COMPLETED, FAILED, COMPENSATED
    private SagaStep currentStep;     // RESERVE_INVENTORY, PROCESS_PAYMENT, CONFIRM_ORDER
    private String failureReason;
    private LocalDateTime createdAt;
}
```

## 결제 처리

### Mock PG Client

```java
@Component
public class MockPGClient {

    public PgResponse processPayment(String orderId, BigDecimal amount, PaymentMethod method) {
        // 테스트용: 90% 성공, 10% 실패
        boolean success = random.nextDouble() < 0.9;

        return PgResponse.builder()
            .transactionId(UUID.randomUUID().toString())
            .success(success)
            .message(success ? "Payment processed" : "Payment failed")
            .build();
    }
}
```

### 결제 수단

| 메서드 | 설명 |
|--------|------|
| CREDIT_CARD | 신용카드 |
| BANK_TRANSFER | 계좌이체 |
| VIRTUAL_ACCOUNT | 가상계좌 |
| KAKAO_PAY | 카카오페이 |
| NAVER_PAY | 네이버페이 |

## 재고 관리

### 동시성 제어

```java
@Transactional
public void reserveStock(Long productId, int quantity) {
    Inventory inventory = inventoryRepository
        .findByProductIdWithLock(productId)  // PESSIMISTIC_WRITE
        .orElseThrow();

    inventory.reserve(quantity);

    // 재고 이동 이력 기록
    stockMovementRepository.save(StockMovement.of(
        productId,
        -quantity,
        MovementType.RESERVATION,
        "Order reservation"
    ));
}
```

### 재고 이동 타입

| 타입 | 설명 |
|------|------|
| PURCHASE | 입고 |
| SALE | 판매 |
| RESERVATION | 예약 (주문) |
| RELEASE | 예약 해제 (취소) |
| ADJUSTMENT | 수동 조정 |

## 이벤트 발행

```java
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        kafkaTemplate.send("order-created", OrderCreatedEvent.from(order));
    }

    public void publishOrderCancelled(Order order) {
        kafkaTemplate.send("order-cancelled", OrderCancelledEvent.from(order));
    }
}
```

## 에러 코드

| 코드 | 설명 |
|------|------|
| S001 | 상품 없음 |
| S002 | 재고 부족 |
| S003 | 장바구니 비어있음 |
| S004 | 주문 없음 |
| S005 | 결제 실패 |
| S006 | 취소 불가 상태 |
| S007 | 권한 없음 |
