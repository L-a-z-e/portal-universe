# 도메인 모델 분석

## 개요

shopping-service의 핵심 도메인 모델을 분석합니다. E-commerce 시스템의 핵심 개념인 주문(Order), 장바구니(Cart), 재고(Inventory)를 중심으로 JPA 엔티티 설계 패턴을 학습합니다.

---

## 1. Entity 관계도

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Product     │     │    Inventory    │     │  StockMovement  │
│  (상품 정보)     │◄────│   (재고 관리)    │────►│   (이력 추적)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │
         │ productId (참조)
         ▼
┌─────────────────┐     ┌─────────────────┐
│      Cart       │────►│    CartItem     │
│   (장바구니)     │     │  (장바구니 항목) │
└─────────────────┘     └─────────────────┘
                              │ 스냅샷
                              ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Order       │────►│   OrderItem     │     │    SagaState    │
│     (주문)       │     │   (주문 항목)    │     │  (Saga 상태)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │
         │ @Embedded
         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Address      │     │    Delivery     │────►│ DeliveryHistory │
│  (배송 주소)     │     │     (배송)       │     │   (배송 이력)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 2. 핵심 엔티티 분석

### 2.1 Order (주문)

**파일**: `order/domain/Order.java`

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;  // ORD-YYYYMMDD-XXXXXXXX 형식

    @Embedded
    private Address shippingAddress;  // 값 객체 패턴

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
```

**학습 포인트:**

1. **인덱스 설계**: 조회 빈도가 높은 컬럼(orderNumber, userId, status, createdAt)에 인덱스 생성
2. **@Embedded Address**: 값 객체(Value Object) 패턴으로 주소 정보 재사용
3. **UUID 기반 주문번호**: 읽기 쉬운 형태 `ORD-20250117-A1B2C3D4`
4. **cascade = ALL**: Order 저장/삭제 시 OrderItem도 함께 처리
5. **orphanRemoval = true**: 부모에서 제거된 자식은 자동 삭제

### 2.2 OrderItem (주문 항목)

**파일**: `order/domain/OrderItem.java`

```java
@Entity
@Table(name = "order_items")
public class OrderItem {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // 스냅샷 필드 - 주문 시점의 가격 보존
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;  // price × quantity
}
```

**학습 포인트:**

1. **FetchType.LAZY**: N+1 문제 방지를 위한 지연 로딩
2. **스냅샷 패턴**: productName, price를 복사 저장 (시점 가격 보존)
3. **subtotal 계산 필드**: 빌더에서 `price.multiply(BigDecimal.valueOf(quantity))` 계산

### 2.3 Cart (장바구니)

**파일**: `cart/domain/Cart.java`

```java
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_cart_user_status", columnList = "user_id, status")
})
public class Cart {
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    private CartStatus status;  // ACTIVE, CHECKED_OUT

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // 비즈니스 메서드
    public CartItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
        validateActive();
        // 중복 체크 후 추가
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

**학습 포인트:**

1. **복합 인덱스**: `(user_id, status)` - 사용자의 활성 장바구니 조회 최적화
2. **상태 기반 검증**: `validateActive()` - 체크아웃된 장바구니는 수정 불가
3. **도메인 불변식**: 비어있는 장바구니 체크아웃 불가

### 2.4 Inventory (재고)

**파일**: `inventory/domain/Inventory.java`

```java
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "product_id", unique = true)
})
public class Inventory {
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;  // 가용 재고

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;   // 예약 재고

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;      // 전체 재고 (가용 + 예약)

    @Version
    private Long version;  // 낙관적 락

    // 재고 상태 전이 메서드
    public void reserve(int quantity) {
        // 가용 → 예약
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void deduct(int quantity) {
        // 예약 → 차감 (totalQuantity도 감소)
        reservedQuantity -= quantity;
        totalQuantity -= quantity;
    }

    public void release(int quantity) {
        // 예약 → 가용 (취소 시)
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
}
```

**학습 포인트:**

1. **3단계 재고 모델**: available → reserved → deducted
2. **@Version**: 낙관적 락 (Pessimistic Lock의 백업용)
3. **불변식 유지**: `totalQuantity = availableQuantity + reservedQuantity`

---

## 3. @Embedded 값 객체 패턴

### Address 클래스

**파일**: `common/domain/Address.java`

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public String getFullAddress() {
        // [12345] 서울시 강남구 역삼동 123-45
    }
}
```

**장점:**

1. **재사용**: Order, Delivery 등 여러 엔티티에서 동일한 주소 구조 사용
2. **응집도**: 주소 관련 로직을 한 곳에 모음
3. **테이블 구조**: 컬럼이 부모 테이블에 직접 포함됨 (별도 테이블 없음)

---

## 4. UUID 기반 주문번호 생성

**파일**: `order/domain/Order.java:210-214`

```java
private static String generateOrderNumber() {
    String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String uuidSuffix = UUID.randomUUID().toString()
        .replace("-", "")
        .substring(0, 8)
        .toUpperCase();
    return "ORD-" + datePrefix + "-" + uuidSuffix;
}
// 결과: ORD-20250117-A1B2C3D4
```

**설계 의도:**

1. **가독성**: 순수 UUID보다 읽기 쉬움
2. **정렬 가능**: 날짜 기반 prefix로 시간순 정렬 지원
3. **고유성**: UUID 8자리로 일 단위 충돌 확률 매우 낮음 (16^8 = 42억)
4. **고객 지원**: 전화로 주문번호 전달 시 용이

---

## 5. @OneToMany Cascade 전략

### cascade = CascadeType.ALL

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

| CascadeType | 동작 | 사용 케이스 |
|-------------|------|------------|
| PERSIST | 부모 저장 시 자식도 저장 | 새 주문 + 주문항목 동시 생성 |
| MERGE | 부모 병합 시 자식도 병합 | 주문 수정 시 항목도 업데이트 |
| REMOVE | 부모 삭제 시 자식도 삭제 | 주문 삭제 시 항목도 삭제 |
| REFRESH | 부모 새로고침 시 자식도 새로고침 | DB에서 최신 데이터 로드 |
| DETACH | 부모 분리 시 자식도 분리 | 영속성 컨텍스트에서 제거 |
| **ALL** | 위 모든 동작 적용 | 부모-자식 생명주기 완전 동기화 |

### orphanRemoval = true

```java
cart.getItems().remove(cartItem);  // 컬렉션에서 제거 시
// → cartItem DELETE 쿼리 자동 실행
```

**주의사항:**
- `orphanRemoval`은 부모-자식 관계가 강한 경우에만 사용
- 자식이 다른 부모를 가질 수 있는 경우 사용 금지

---

## 6. 상태 전이 (State Transition)

### OrderStatus 상태 다이어그램

```
┌─────────┐   confirm   ┌───────────┐   pay   ┌────────┐
│ PENDING │────────────►│ CONFIRMED │────────►│  PAID  │
└─────────┘             └───────────┘         └────────┘
     │                        │                    │
     │ cancel                 │ cancel             │ cancel/ship
     ▼                        ▼                    ▼
┌───────────┐           ┌───────────┐         ┌──────────┐
│ CANCELLED │           │ CANCELLED │         │ SHIPPING │
└───────────┘           └───────────┘         └──────────┘
                                                   │
                                                   │ deliver
                                                   ▼
                                              ┌───────────┐
                                              │ DELIVERED │
                                              └───────────┘
```

### 상태별 허용 동작

```java
public boolean isCancellable() {
    return this == PENDING || this == CONFIRMED || this == PAID;
}

public boolean isRefundable() {
    return this == PAID || this == SHIPPING;
}
```

---

## 7. 핵심 파일 요약

| 파일 | 역할 | 주요 패턴 |
|------|------|----------|
| `Order.java` | 주문 애그리거트 루트 | @Embedded, UUID 생성 |
| `OrderItem.java` | 주문 항목 | 스냅샷 패턴, Lazy Loading |
| `Cart.java` | 장바구니 | 상태 기반 검증, 도메인 불변식 |
| `CartItem.java` | 장바구니 항목 | 스냅샷 패턴 |
| `Inventory.java` | 재고 관리 | 3단계 재고 모델, @Version |
| `Address.java` | 배송 주소 값 객체 | @Embeddable |

---

## 8. 핵심 요약

1. **애그리거트 루트**: Order, Cart, Inventory가 각 도메인의 진입점
2. **값 객체**: Address를 @Embeddable로 재사용
3. **스냅샷 패턴**: CartItem, OrderItem에서 상품 정보 시점 보존
4. **상태 기반 검증**: 도메인 객체 내부에서 비즈니스 규칙 강제
5. **Cascade 전략**: 부모-자식 생명주기 동기화
