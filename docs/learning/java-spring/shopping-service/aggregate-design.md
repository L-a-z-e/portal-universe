# Aggregate Design

## 1. 개요

Aggregate는 데이터 변경의 단위로 취급되는 연관 객체의 묶음입니다. 각 Aggregate는 Root Entity를 가지며, 외부에서는 Root를 통해서만 접근합니다.

## 2. Shopping Service의 Aggregate 구조

```
┌────────────────────────────────────────┐
│            Order Aggregate             │
│  ┌─────────────────────────────────┐   │
│  │  Order (Aggregate Root)         │   │
│  │  ├── OrderItem (Entity)         │   │
│  │  │   └── productId, price, qty  │   │
│  │  ├── OrderItem                  │   │
│  │  └── Address (Value Object)     │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│            Cart Aggregate              │
│  ┌─────────────────────────────────┐   │
│  │  Cart (Aggregate Root)          │   │
│  │  ├── CartItem (Entity)          │   │
│  │  │   └── productId, price, qty  │   │
│  │  └── CartItem                   │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│           Inventory Aggregate          │
│  ┌─────────────────────────────────┐   │
│  │  Inventory (Aggregate Root)     │   │
│  │  └── productId 참조 (Not Entity) │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │  StockMovement (별도 Entity)    │   │
│  │  └── inventoryId 참조           │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│           Payment Aggregate            │
│  ┌─────────────────────────────────┐   │
│  │  Payment (Aggregate Root)       │   │
│  │  └── orderId 참조 (Not Entity)  │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘
```

## 3. Aggregate별 설계

### Order Aggregate

```java
@Entity
public class Order {  // Aggregate Root
    @Id
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    private Address shippingAddress;

    // 비즈니스 메서드 - Aggregate 내부 조작
    public OrderItem addItem(Long productId, String productName,
                             BigDecimal price, int quantity) {
        OrderItem item = OrderItem.builder()
            .order(this)
            .productId(productId)
            .productName(productName)
            .price(price)
            .quantity(quantity)
            .build();
        this.items.add(item);
        recalculateTotalAmount();
        return item;
    }

    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }
}
```

**트랜잭션 경계:**
- Order와 OrderItem은 하나의 트랜잭션에서 처리
- Order 저장 시 OrderItem도 함께 저장 (Cascade)

**외부 참조:**
- `userId`: String (auth-service의 사용자 ID)
- `appliedUserCouponId`: Long (Coupon Aggregate 참조)

### Cart Aggregate

```java
@Entity
public class Cart {  // Aggregate Root
    @Id
    private Long id;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public CartItem addItem(Long productId, String productName,
                            BigDecimal price, int quantity) {
        validateActive();

        Optional<CartItem> existing = findItemByProductId(productId);
        if (existing.isPresent()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
        }

        CartItem item = CartItem.builder()
            .cart(this)
            .productId(productId)
            .productName(productName)
            .price(price)
            .quantity(quantity)
            .build();

        this.items.add(item);
        return item;
    }

    public void checkout() {
        validateActive();
        if (this.items.isEmpty()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
        }
        this.status = CartStatus.CHECKED_OUT;
    }
}
```

**특징:**
- CartItem은 Cart를 통해서만 생성/수정/삭제
- CartItemRepository 직접 사용 X

### Inventory Aggregate

```java
@Entity
public class Inventory {  // Aggregate Root
    @Id
    private Long id;

    private Long productId;  // 외부 참조 (ID만)
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;

    @Version
    private Long version;  // 낙관적 락

    public void reserve(int quantity) {
        if (this.availableQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void deduct(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_DEDUCTION_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    public void release(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_RELEASE_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }
}
```

**트랜잭션 경계:**
- 비관적 락 사용 (`findByProductIdWithLock`)
- StockMovement는 별도 트랜잭션에서 기록 (감사 로그)

## 4. Aggregate 간 참조 규칙

### ID로만 참조 (느슨한 결합)

```java
// Good - ID로 참조
@Entity
public class Payment {
    @Column(name = "order_id", nullable = false)
    private Long orderId;  // Order의 ID만 저장

    @Column(name = "order_number", nullable = false)
    private String orderNumber;  // 조회 편의를 위해 저장
}

// Bad - Entity 직접 참조
@Entity
public class Payment {
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;  // X - 다른 Aggregate 직접 참조
}
```

### 이유

1. **독립적 트랜잭션**: 각 Aggregate는 독립적으로 저장 가능
2. **서비스 분리 가능**: 마이크로서비스로 분리 시 용이
3. **데드락 방지**: Aggregate 간 락 충돌 최소화

## 5. 트랜잭션 경계

### 원칙: 하나의 트랜잭션 = 하나의 Aggregate

```java
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 1. Order Aggregate 생성 및 저장
    Order order = Order.builder()
        .userId(userId)
        .shippingAddress(address)
        .build();

    // Order Aggregate 내부 조작
    for (var item : request.items()) {
        order.addItem(item.productId(), item.productName(),
                      item.price(), item.quantity());
    }

    orderRepository.save(order);

    // 2. 다른 Aggregate는 별도 트랜잭션 또는 이벤트로 처리
    sagaOrchestrator.startSaga(order);  // Inventory 예약은 Saga로

    return OrderResponse.from(order);
}
```

### 여러 Aggregate 수정이 필요한 경우

**Saga 패턴 사용:**

```java
@Transactional
public void startSaga(Order order) {
    SagaState saga = SagaState.builder()
        .orderId(order.getId())
        .orderNumber(order.getOrderNumber())
        .build();

    sagaStateRepository.save(saga);

    try {
        // Step 1: Inventory Aggregate
        executeReserveInventory(order, saga);

        // Step 2: (결제 후) Inventory 차감
        // Step 3: Delivery 생성
    } catch (Exception e) {
        compensate(saga, e.getMessage());
    }
}
```

## 6. Cascade 설정

### Aggregate 내부

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items;
```

- `CascadeType.ALL`: Root 저장 시 하위도 저장
- `orphanRemoval = true`: Root에서 제거 시 하위도 삭제

### Aggregate 간 (사용 금지)

```java
// Bad - 다른 Aggregate에 Cascade
@ManyToOne(cascade = CascadeType.ALL)
private Inventory inventory;  // X
```

## 7. 불변식 (Invariant) 보장

각 Aggregate Root는 내부 불변식을 보장합니다:

```java
// Order Aggregate 불변식
public class Order {

    // 불변식 1: totalAmount = sum(items.subtotal)
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        calculateFinalAmount();
    }

    // 불변식 2: finalAmount = totalAmount - discountAmount (>=0)
    private void calculateFinalAmount() {
        BigDecimal discount = this.discountAmount != null
            ? this.discountAmount : BigDecimal.ZERO;
        this.finalAmount = this.totalAmount.subtract(discount);
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }
}
```

```java
// Cart Aggregate 불변식
public class Cart {

    // 불변식: ACTIVE 상태에서만 수정 가능
    private void validateActive() {
        if (this.status != CartStatus.ACTIVE) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ALREADY_CHECKED_OUT);
        }
    }
}
```

```java
// Inventory Aggregate 불변식
public class Inventory {

    // 불변식: availableQuantity + reservedQuantity = totalQuantity
    public void reserve(int quantity) {
        // 이 메서드 실행 후에도 불변식 유지
        this.availableQuantity -= quantity;  // -qty
        this.reservedQuantity += quantity;   // +qty
        // totalQuantity는 그대로 -> 불변식 유지
    }
}
```

## 8. Repository 규칙

### Aggregate Root만 Repository 사용

```java
// Good
OrderRepository -> Order (Aggregate Root)
CartRepository -> Cart (Aggregate Root)
InventoryRepository -> Inventory (Aggregate Root)

// Bad
OrderItemRepository -> OrderItem (내부 Entity)
CartItemRepository -> CartItem (내부 Entity)
```

### 예외: 감사 로그 등

```java
// StockMovementRepository - 감사 로그용 별도 Entity
stockMovementRepository.save(movement);
```

## 9. 소스 위치

| Aggregate | Root Entity | 내부 Entity | 소스 위치 |
|-----------|-------------|-------------|-----------|
| Order | Order | OrderItem | `order/domain/` |
| Cart | Cart | CartItem | `cart/domain/` |
| Inventory | Inventory | - | `inventory/domain/` |
| Payment | Payment | - | `payment/domain/` |
| Coupon | Coupon | UserCoupon | `coupon/domain/` |
| TimeDeal | TimeDeal | TimeDealProduct, TimeDealPurchase | `timedeal/domain/` |
