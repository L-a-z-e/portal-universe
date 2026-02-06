# Order 도메인 심화

## 학습 목표
- Order Aggregate의 구조와 책임 이해
- 상태 머신 기반 주문 흐름 학습
- 스냅샷 패턴과 가격 불변성 원칙 이해

---

## 1. Order Aggregate 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                      ORDER AGGREGATE                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │                    Order (Root)                           │  │
│   ├──────────────────────────────────────────────────────────┤  │
│   │  id                Long (PK)                              │  │
│   │  orderNumber       String (unique) ORD-YYYYMMDD-XXXXXXXX  │  │
│   │  userId            String                                 │  │
│   │  status            OrderStatus                            │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  totalAmount       BigDecimal (할인 전)                    │  │
│   │  discountAmount    BigDecimal (쿠폰 할인)                  │  │
│   │  finalAmount       BigDecimal (실결제)                     │  │
│   │  appliedUserCouponId  Long                                │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  shippingAddress   Address (Embedded)                     │  │
│   │  cancelReason      String                                 │  │
│   │  cancelledAt       LocalDateTime                          │  │
│   │  createdAt         LocalDateTime                          │  │
│   │  updatedAt         LocalDateTime                          │  │
│   └──────────────────────────────────────────────────────────┘  │
│                            │                                     │
│                            │ 1:N (Cascade ALL)                   │
│                            ▼                                     │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │                  OrderItem (하위 Entity)                   │  │
│   ├──────────────────────────────────────────────────────────┤  │
│   │  id              Long (PK)                                │  │
│   │  productId       Long                                     │  │
│   │  productName     String (스냅샷)                           │  │
│   │  price           BigDecimal (스냅샷)                       │  │
│   │  quantity        Integer                                  │  │
│   │  subtotal        BigDecimal (price × quantity)            │  │
│   └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │                 Address (Value Object)                    │  │
│   ├──────────────────────────────────────────────────────────┤  │
│   │  receiverName    String                                   │  │
│   │  zipCode         String                                   │  │
│   │  address         String                                   │  │
│   │  detailAddress   String                                   │  │
│   │  phoneNumber     String                                   │  │
│   └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 상태 머신 (State Machine)

### 2.1 OrderStatus Enum

```java
public enum OrderStatus {
    PENDING("대기 중"),      // 주문 생성, 결제 대기
    CONFIRMED("확정됨"),     // 재고 예약 완료
    PAID("결제 완료"),       // 결제 성공
    SHIPPING("배송 중"),     // 출고 완료
    DELIVERED("배송 완료"),   // 수령 완료
    CANCELLED("취소됨"),     // 주문 취소
    REFUNDED("환불됨");      // 결제 환불 완료

    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED || this == PAID;
    }

    public boolean isRefundable() {
        return this == PAID || this == SHIPPING;
    }
}
```

### 2.2 상태 전이 다이어그램

```
                    ┌───────────────────────────────────────┐
                    │                                       │
                    ▼                                       │
┌─────────┐   confirm()   ┌──────────┐   markAsPaid()   ┌──────┐
│ PENDING │──────────────►│ CONFIRMED│─────────────────►│ PAID │
└─────────┘               └──────────┘                  └──────┘
     │                         │                            │
     │ cancel()                │ cancel()                   │ ship()
     ▼                         ▼                            ▼
┌───────────┐            ┌───────────┐               ┌──────────┐
│ CANCELLED │            │ CANCELLED │               │ SHIPPING │
└───────────┘            └───────────┘               └──────────┘
                                                          │
                              ┌────────────────────────────┤
                              │                            │ deliver()
                         cancel()                          ▼
                              │                      ┌───────────┐
                              ▼                      │ DELIVERED │
                         ┌──────────┐                └───────────┘
                         │ REFUNDED │
                         └──────────┘
```

### 2.3 상태 전이 메서드

```java
@Entity
public class Order {

    /**
     * PENDING → CONFIRMED
     * 재고 예약 완료 후 호출
     */
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * CONFIRMED → PAID
     * 결제 완료 후 호출
     */
    public void markAsPaid() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.PAID;
    }

    /**
     * PAID → SHIPPING
     * 출고 완료 후 호출
     */
    public void ship() {
        if (this.status != OrderStatus.PAID) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.SHIPPING;
    }

    /**
     * SHIPPING → DELIVERED
     * 배송 완료 후 호출
     */
    public void deliver() {
        if (this.status != OrderStatus.SHIPPING) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * → CANCELLED
     * 취소 가능 상태에서만 허용
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * PAID/SHIPPING → REFUNDED
     * 환불 처리
     */
    public void refund() {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.REFUNDED;
    }
}
```

---

## 3. 주문번호 생성 전략

### 3.1 형식

```
ORD-YYYYMMDD-XXXXXXXX
└─┘ └──────┘ └──────┘
 │     │        └── UUID 앞 8자리 (대문자)
 │     └── 날짜 (BASIC_ISO_DATE)
 └── 접두어
```

### 3.2 구현

```java
private static String generateOrderNumber() {
    String datePrefix = LocalDate.now()
        .format(DateTimeFormatter.BASIC_ISO_DATE);  // 20250122

    String uuidSuffix = UUID.randomUUID()
        .toString()
        .replace("-", "")
        .substring(0, 8)
        .toUpperCase();  // A1B2C3D4

    return "ORD-" + datePrefix + "-" + uuidSuffix;
    // 결과: ORD-20250122-A1B2C3D4
}
```

### 3.3 특성

| 특성 | 설명 |
|------|------|
| **유일성** | UUID 기반으로 충돌 확률 극히 낮음 |
| **가독성** | 날짜 포함으로 생성 시점 파악 가능 |
| **정렬성** | 날짜 기준 자연 정렬 가능 |
| **길이** | 22자 고정 |

---

## 4. 스냅샷 패턴

### 4.1 필요성

주문 시점의 상품 정보(이름, 가격)는 변경되어서는 안 됩니다.

```
┌─────────────────┐        ┌─────────────────┐
│     Product     │        │    OrderItem    │
├─────────────────┤        ├─────────────────┤
│ name: "iPhone"  │───X───►│ productName:    │
│ price: 1000000  │        │   "iPhone"      │
│                 │        │ price: 1000000  │
└─────────────────┘        │ (스냅샷 유지)    │
      │                    └─────────────────┘
      │ 가격 변경
      ▼
┌─────────────────┐
│ name: "iPhone"  │   OrderItem의 가격은
│ price: 1100000  │   여전히 1000000원
└─────────────────┘
```

### 4.2 OrderItem 구현

```java
@Entity
public class OrderItem {

    @Column(name = "product_name", nullable = false)
    private String productName;  // 스냅샷

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;    // 스냅샷

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Builder
    public OrderItem(Order order, Long productId, String productName,
                     BigDecimal price, Integer quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;  // 현재 상품명 스냅샷
        this.price = price;               // 현재 가격 스냅샷
        this.quantity = quantity;
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

### 4.3 주문 생성 시 스냅샷 저장

```java
// OrderService
public Order createOrder(OrderCreateRequest request) {
    Order order = Order.builder()
        .userId(request.getUserId())
        .shippingAddress(request.getAddress())
        .build();

    for (OrderItemRequest itemReq : request.getItems()) {
        Product product = productRepository.findById(itemReq.getProductId())
            .orElseThrow(() -> new ProductNotFoundException());

        // 현재 상품 정보를 스냅샷으로 저장
        order.addItem(
            product.getId(),
            product.getName(),       // 스냅샷
            product.getPrice(),      // 스냅샷
            itemReq.getQuantity()
        );
    }

    return orderRepository.save(order);
}
```

---

## 5. 금액 계산 로직

### 5.1 금액 구조

```
┌─────────────────────────────────────────────────────────────┐
│                      ORDER 금액 구조                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   totalAmount = Σ (OrderItem.subtotal)                       │
│   ├── OrderItem[0].subtotal = price × quantity               │
│   ├── OrderItem[1].subtotal = price × quantity               │
│   └── ...                                                    │
│                                                              │
│   discountAmount = 쿠폰 할인액                                │
│                                                              │
│   finalAmount = totalAmount - discountAmount                 │
│                (음수인 경우 0으로 처리)                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 금액 재계산 메서드

```java
public void recalculateTotalAmount() {
    this.totalAmount = items.stream()
        .map(OrderItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    calculateFinalAmount();
}

private void calculateFinalAmount() {
    BigDecimal discount = this.discountAmount != null
        ? this.discountAmount
        : BigDecimal.ZERO;

    this.finalAmount = this.totalAmount.subtract(discount);

    // 최종 금액은 음수가 될 수 없음
    if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
        this.finalAmount = BigDecimal.ZERO;
    }
}
```

### 5.3 쿠폰 적용

```java
public void applyCoupon(Long userCouponId, BigDecimal discountAmount) {
    this.appliedUserCouponId = userCouponId;
    this.discountAmount = discountAmount;
    calculateFinalAmount();  // 최종 금액 재계산
}
```

---

## 6. 주문 항목 관리

### 6.1 항목 추가

```java
public OrderItem addItem(Long productId, String productName,
                         BigDecimal price, int quantity) {
    OrderItem orderItem = OrderItem.builder()
        .order(this)
        .productId(productId)
        .productName(productName)
        .price(price)
        .quantity(quantity)
        .build();

    this.items.add(orderItem);
    recalculateTotalAmount();  // 총액 재계산

    return orderItem;
}
```

### 6.2 Cascade 설정

```java
@OneToMany(mappedBy = "order",
           cascade = CascadeType.ALL,
           orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

| 설정 | 의미 |
|------|------|
| `cascade = ALL` | Order 저장/삭제 시 OrderItem도 함께 처리 |
| `orphanRemoval = true` | Order에서 제거된 OrderItem 자동 삭제 |
| `mappedBy = "order"` | OrderItem이 관계의 주인 |

---

## 7. 인덱스 전략

```java
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number",
           columnList = "order_number", unique = true),

    @Index(name = "idx_order_user_id",
           columnList = "user_id"),

    @Index(name = "idx_order_status",
           columnList = "status"),

    @Index(name = "idx_order_created_at",
           columnList = "created_at")
})
```

| 인덱스 | 용도 |
|--------|------|
| `order_number` | 주문번호 조회 (unique) |
| `user_id` | 사용자별 주문 내역 |
| `status` | 상태별 주문 필터링 |
| `created_at` | 기간별 주문 조회/정렬 |

---

## 8. 통계 메서드

```java
/**
 * 총 상품 수량 계산
 */
public int getTotalQuantity() {
    return items.stream()
        .mapToInt(OrderItem::getQuantity)
        .sum();
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Aggregate Root** | Order - 일관성 경계의 진입점 |
| **하위 Entity** | OrderItem - Order와 생명주기 공유 |
| **Value Object** | Address - 불변 임베디드 객체 |
| **상태 머신** | OrderStatus + 전이 메서드 |
| **스냅샷 패턴** | productName, price 스냅샷 저장 |
| **유일 식별자** | orderNumber (ORD-YYYYMMDD-XXXXXXXX) |
| **금액 불변성** | 주문 생성 시점 가격 보존 |

---

## 다음 학습

- [Inventory 도메인 심화](./inventory-domain.md)
- [Payment 도메인 심화](./payment-domain.md)
- [주문 Saga 흐름](../business/order-saga.md)
