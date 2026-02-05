# JPA Entity Mapping

## 개요

JPA(Java Persistence API)는 Java 객체를 관계형 데이터베이스 테이블에 매핑하는 ORM(Object-Relational Mapping) 표준입니다. shopping-service에서는 JPA/Hibernate를 활용하여 엔티티를 관리합니다.

---

## 1. @Entity 기본 매핑

### 기본 엔티티 구조

```java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;
}
```

### 주요 어노테이션

| 어노테이션 | 설명 | 예시 |
|-----------|------|------|
| `@Entity` | JPA 엔티티 선언 | 클래스에 필수 |
| `@Table` | 테이블명 지정 | `@Table(name = "products")` |
| `@Id` | Primary Key 지정 | 필드에 필수 |
| `@GeneratedValue` | PK 생성 전략 | `IDENTITY`, `SEQUENCE`, `AUTO` |
| `@Column` | 컬럼 세부 설정 | `nullable`, `length`, `unique` |

### GenerationType 전략

```java
// MySQL - AUTO_INCREMENT 사용 (권장)
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// PostgreSQL/Oracle - Sequence 사용
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
```

---

## 2. 연관관계 매핑

### 2.1 @OneToMany / @ManyToOne

shopping-service의 Order-OrderItem 관계:

```java
// Order.java (부모)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // 연관관계 편의 메서드
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
        recalculateTotalAmount();
        return orderItem;
    }
}
```

```java
// OrderItem.java (자식)
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;
}
```

### 2.2 Cart-CartItem 관계

```java
// Cart.java
@Entity
@Table(name = "carts")
public class Cart {

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public CartItem addItem(Long productId, String productName,
                           BigDecimal price, int quantity) {
        // 중복 검증
        Optional<CartItem> existingItem = findItemByProductId(productId);
        if (existingItem.isPresent()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
        }

        CartItem cartItem = CartItem.builder()
                .cart(this)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();

        this.items.add(cartItem);
        return cartItem;
    }
}
```

```java
// CartItem.java
@Entity
@Table(name = "cart_items")
public class CartItem {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;
}
```

### 2.3 Delivery-DeliveryHistory 관계

```java
// Delivery.java
@Entity
@Table(name = "deliveries")
public class Delivery {

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")  // 정렬 지정
    private List<DeliveryHistory> histories = new ArrayList<>();

    private void addHistory(DeliveryStatus status, String location, String description) {
        DeliveryHistory history = DeliveryHistory.builder()
                .delivery(this)
                .status(status)
                .location(location)
                .description(description)
                .build();

        this.histories.add(history);
    }
}
```

---

## 3. FetchType 전략

### LAZY vs EAGER

| FetchType | 로딩 시점 | 사용 케이스 |
|-----------|----------|-------------|
| `LAZY` | 실제 접근 시 | 대부분의 연관관계 (권장) |
| `EAGER` | 즉시 로딩 | 항상 함께 사용되는 경우 |

### shopping-service 적용 사례

```java
// CartItem - LAZY 사용 (권장)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cart_id", nullable = false)
private Cart cart;

// OrderItem - LAZY 사용
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private Order order;

// DeliveryHistory - LAZY 사용
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "delivery_id", nullable = false)
private Delivery delivery;
```

> **주의**: `@ManyToOne`, `@OneToOne`의 기본값은 `EAGER`이므로 명시적으로 `LAZY`를 지정해야 합니다.

---

## 4. Cascade 옵션

### CascadeType 종류

| Type | 설명 | 사용 상황 |
|------|------|----------|
| `ALL` | 모든 작업 전파 | 부모-자식 완전 의존 |
| `PERSIST` | 저장만 전파 | 생성 시 함께 저장 |
| `MERGE` | 병합만 전파 | 수정 시 함께 수정 |
| `REMOVE` | 삭제만 전파 | 삭제 시 함께 삭제 |
| `REFRESH` | 새로고침 전파 | DB 동기화 |

### shopping-service 적용

```java
// Order와 OrderItem - CascadeType.ALL + orphanRemoval
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

### orphanRemoval 동작

```java
public void removeItem(Long itemId) {
    validateActive();
    CartItem item = findItemById(itemId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

    // 컬렉션에서 제거하면 자동으로 DELETE 쿼리 실행
    this.items.remove(item);
}

public void clear() {
    validateActive();
    // 컬렉션을 비우면 모든 자식 엔티티 DELETE
    this.items.clear();
}
```

---

## 5. @Embedded / @Embeddable

### Value Object 매핑

```java
// Address.java - 값 객체
@Embeddable
@Getter
@NoArgsConstructor
public class Address {

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address1", length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;
}
```

```java
// Order.java - 사용
@Entity
@Table(name = "orders")
public class Order {

    @Embedded
    private Address shippingAddress;
}

// Delivery.java - 동일한 Address 재사용
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Embedded
    private Address shippingAddress;
}
```

---

## 6. @Enumerated

### Enum 매핑 전략

```java
// OrderStatus Enum
public enum OrderStatus {
    PENDING,     // 주문 대기
    CONFIRMED,   // 주문 확정
    PAID,        // 결제 완료
    SHIPPING,    // 배송 중
    DELIVERED,   // 배송 완료
    CANCELLED,   // 취소
    REFUNDED;    // 환불
}

// Entity에서 사용
@Entity
public class Order {

    @Enumerated(EnumType.STRING)  // 문자열로 저장 (권장)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;
}
```

### EnumType 비교

| Type | 저장 형태 | 장점 | 단점 |
|------|----------|------|------|
| `STRING` | "PENDING" | 가독성, Enum 순서 변경 안전 | 저장 공간 |
| `ORDINAL` | 0, 1, 2... | 저장 공간 절약 | Enum 순서 변경 시 문제 |

> **권장**: 항상 `EnumType.STRING` 사용

---

## 7. 인덱스 정의

### @Table indexes 속성

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at")
})
public class Order { }
```

```java
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_item_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_item_product_id", columnList = "product_id")
})
public class CartItem { }
```

```java
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_inventory_id", columnList = "inventory_id"),
    @Index(name = "idx_stock_movement_product_id", columnList = "product_id"),
    @Index(name = "idx_stock_movement_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_movement_created_at", columnList = "created_at")
})
public class StockMovement { }
```

---

## 8. Entity 설계 베스트 프랙티스

### 8.1 protected/package-private 기본 생성자

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    // JPA 프록시 생성을 위해 필요
}
```

### 8.2 Builder 패턴 사용

```java
@Entity
public class Order {

    @Builder
    public Order(String userId, Address shippingAddress) {
        this.orderNumber = generateOrderNumber();
        this.userId = userId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
        this.shippingAddress = shippingAddress;
    }
}
```

### 8.3 연관관계 편의 메서드

```java
public OrderItem addItem(Long productId, String productName,
                         BigDecimal price, int quantity) {
    OrderItem orderItem = OrderItem.builder()
            .order(this)  // 양방향 관계 설정
            .productId(productId)
            .productName(productName)
            .price(price)
            .quantity(quantity)
            .build();

    this.items.add(orderItem);  // 컬렉션에 추가
    recalculateTotalAmount();   // 파생 값 재계산
    return orderItem;
}
```

### 8.4 상태 변경 메서드

```java
public void confirm() {
    if (this.status != OrderStatus.PENDING) {
        throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
    }
    this.status = OrderStatus.CONFIRMED;
}

public void cancel(String reason) {
    if (!this.status.isCancellable()) {
        throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();
}
```

---

## 관련 문서

- [JPA Query Optimization](./jpa-query-optimization.md) - N+1 문제 해결
- [JPA Locking](./jpa-locking.md) - 동시성 제어
- [Transaction Management](./transaction-management.md) - 트랜잭션 관리
