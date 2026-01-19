# 가격 스냅샷 패턴 분석

## 개요

E-commerce 시스템에서 주문 시점의 상품 정보를 보존하는 **스냅샷(Snapshot) 패턴**을 분석합니다. 이 패턴은 가격 변동, 상품 삭제 등의 변경에도 주문 내역의 정확성을 보장합니다.

---

## 1. 스냅샷 패턴이란?

### 문제 상황

```
Day 1: 상품 A 가격 = 10,000원
       User가 상품 A 3개 구매 → 총 30,000원 결제

Day 2: 상품 A 가격 = 15,000원으로 인상

Day 3: User가 주문 내역 확인
       → "3 × 15,000 = 45,000원" 표시? ❌ (틀림!)
       → "3 × 10,000 = 30,000원" 표시? ✅ (정확!)
```

### 해결책: 스냅샷 저장

주문/장바구니 항목에 **주문 시점의 상품 정보를 복사**하여 저장합니다:

```java
public class OrderItem {
    private Long productId;       // 참조 ID (원본 연결)
    private String productName;   // 스냅샷 (복사본)
    private BigDecimal price;     // 스냅샷 (복사본)
}
```

---

## 2. CartItem의 스냅샷

**파일**: `cart/domain/CartItem.java`

```java
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Column(name = "product_id", nullable = false)
    private Long productId;           // 상품 참조 ID

    /**
     * 상품명 (스냅샷)
     * 장바구니에 담은 시점의 상품명을 저장
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 단가 (스냅샷)
     * 장바구니에 담은 시점의 가격을 저장
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
```

### 장바구니 추가 시 스냅샷 생성

```java
// Cart.java
public CartItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
    validateActive();

    // 이미 같은 상품이 있는지 확인
    Optional<CartItem> existingItem = findItemByProductId(productId);
    if (existingItem.isPresent()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
    }

    // 스냅샷으로 저장 (productName, price 복사)
    CartItem cartItem = CartItem.builder()
            .cart(this)
            .productId(productId)
            .productName(productName)   // 스냅샷!
            .price(price)               // 스냅샷!
            .quantity(quantity)
            .build();

    this.items.add(cartItem);
    return cartItem;
}
```

### 가격 업데이트 메서드 (선택적)

```java
// CartItem.java
/**
 * 가격 정보를 업데이트합니다 (관리자 또는 시스템에 의해).
 * 장바구니 페이지에서 현재 가격과 동기화할 때 사용
 */
public void updateProductInfo(BigDecimal newPrice, String newProductName) {
    if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
        this.price = newPrice;
    }
    if (newProductName != null && !newProductName.isBlank()) {
        this.productName = newProductName;
    }
}
```

---

## 3. OrderItem의 스냅샷

**파일**: `order/domain/OrderItem.java`

```java
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 상품명 (스냅샷)
     * 주문 시점의 상품명 - 변경 불가
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 단가 (스냅샷)
     * 주문 시점의 가격 - 변경 불가
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 소계 (단가 × 수량)
     * 주문 시점에 계산되어 저장
     */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Builder
    public OrderItem(Order order, Long productId, String productName,
                     BigDecimal price, Integer quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));  // 계산 후 저장
    }
}
```

### 주문 생성 시 스냅샷 복사

```java
// Order.java
public OrderItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
    OrderItem orderItem = OrderItem.builder()
            .order(this)
            .productId(productId)
            .productName(productName)   // CartItem에서 복사
            .price(price)               // CartItem에서 복사
            .quantity(quantity)
            .build();

    this.items.add(orderItem);
    recalculateTotalAmount();
    return orderItem;
}
```

---

## 4. StockMovement의 이력 스냅샷

**파일**: `inventory/domain/StockMovement.java`

재고 변동 이력도 스냅샷 패턴을 적용합니다:

```java
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    /**
     * 상품 ID (빠른 조회를 위해 비정규화)
     * Inventory가 삭제되어도 이력은 유지됨
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    private MovementType movementType;  // RESERVE, DEDUCT, RELEASE, INBOUND

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 이동 전/후 재고 상태 (스냅샷)
     * 나중에 재고가 변경되어도 당시 상태를 확인 가능
     */
    @Column(name = "previous_available", nullable = false)
    private Integer previousAvailable;

    @Column(name = "after_available", nullable = false)
    private Integer afterAvailable;

    @Column(name = "previous_reserved", nullable = false)
    private Integer previousReserved;

    @Column(name = "after_reserved", nullable = false)
    private Integer afterReserved;

    /**
     * 참조 정보 (주문번호, 결제번호 등)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;  // ORDER, PAYMENT, RETURN, ADMIN

    @Column(name = "reference_id", length = 100)
    private String referenceId;    // ORD-20250117-A1B2C3D4

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "performed_by", length = 100)
    private String performedBy;
}
```

### 이력 기록 메서드

```java
// InventoryServiceImpl.java
private void recordMovement(Inventory inventory, MovementType movementType, int quantity,
                            int previousAvailable, int afterAvailable,
                            int previousReserved, int afterReserved,
                            String referenceType, String referenceId,
                            String reason, String performedBy) {

    StockMovement movement = StockMovement.builder()
            .inventoryId(inventory.getId())
            .productId(inventory.getProductId())    // 비정규화 스냅샷
            .movementType(movementType)
            .quantity(quantity)
            .previousAvailable(previousAvailable)   // 이전 상태 스냅샷
            .afterAvailable(afterAvailable)         // 이후 상태 스냅샷
            .previousReserved(previousReserved)
            .afterReserved(afterReserved)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .reason(reason)
            .performedBy(performedBy)
            .build();

    stockMovementRepository.save(movement);
}
```

---

## 5. 스냅샷 패턴의 장점

### 5.1 시점 가격 보존 (Point-in-time Pricing)

```
주문 시점 가격: 10,000원 → OrderItem.price = 10,000
현재 가격: 15,000원 → Product.price = 15,000

주문 내역 조회 시:
  - OrderItem.price 사용 (10,000원) ✅
  - Product.price 참조하지 않음
```

### 5.2 상품 삭제/변경에 대한 내결함성

```
시나리오: 상품 A 삭제

문제 (스냅샷 없이):
  - OrderItem에서 productId로 Product 조회 시 에러
  - 주문 내역 표시 불가

해결 (스냅샷 사용):
  - OrderItem.productName, price가 저장되어 있음
  - Product 삭제와 무관하게 주문 내역 표시 가능
```

### 5.3 감사 추적 (Audit Trail)

```
StockMovement 조회 결과:

ID | Product | Type    | Qty | Prev | After | Reference
---|---------|---------|-----|------|-------|------------------
1  | 101     | RESERVE | 5   | 100  | 95    | ORD-20250117-A1B2
2  | 101     | DEDUCT  | 5   | 95   | 90    | ORD-20250117-A1B2
3  | 101     | INBOUND | 50  | 90   | 140   | ADMIN-john
```

---

## 6. 스냅샷 vs 참조 비교

### 참조 방식 (비권장)

```java
public class OrderItem {
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;  // 직접 참조

    // 가격은 product.getPrice()로 조회
}
```

**문제점:**
- 상품 가격 변경 시 과거 주문 금액도 변경됨
- 상품 삭제 시 주문 내역 조회 불가
- N+1 쿼리 발생

### 스냅샷 방식 (권장)

```java
public class OrderItem {
    private Long productId;      // ID만 참조
    private String productName;  // 복사본
    private BigDecimal price;    // 복사본
}
```

**장점:**
- 시점 데이터 보존
- 상품 삭제에 독립적
- 쿼리 최적화 (JOIN 불필요)

---

## 7. 데이터 흐름

### 장바구니 → 주문 스냅샷 복사

```
1. 상품 조회
   Product: {id: 101, name: "노트북", price: 1,500,000}

2. 장바구니 추가 (스냅샷 1차)
   CartItem: {productId: 101, productName: "노트북", price: 1,500,000, qty: 1}

3. 상품 가격 변동
   Product: {id: 101, name: "노트북", price: 1,600,000}  ← 인상

4. 주문 생성 (스냅샷 2차 - CartItem에서 복사)
   OrderItem: {productId: 101, productName: "노트북", price: 1,500,000, qty: 1}
                                                         ↑ 장바구니 시점 가격 유지

5. 결과
   - 주문 금액: 1,500,000원 (장바구니 담은 시점)
   - 현재 상품 가격: 1,600,000원
```

---

## 8. 주의사항

### 8.1 스냅샷 동기화 정책

장바구니의 경우, 가격 동기화 정책을 결정해야 합니다:

**옵션 A: 담은 시점 가격 유지**
```java
// 장바구니 조회 시 스냅샷 가격 사용
return CartItem.price;
```

**옵션 B: 현재 가격으로 업데이트**
```java
// 장바구니 페이지 진입 시 최신 가격 동기화
Product product = productRepository.findById(cartItem.getProductId());
if (!product.getPrice().equals(cartItem.getPrice())) {
    cartItem.updateProductInfo(product.getPrice(), product.getName());
    // UI에 "가격이 변경되었습니다" 알림
}
```

### 8.2 정규화 vs 비정규화 트레이드오프

| 항목 | 정규화 (참조) | 비정규화 (스냅샷) |
|------|--------------|------------------|
| 데이터 일관성 | 자동 유지 | 수동 관리 필요 |
| 저장 공간 | 효율적 | 중복 저장 |
| 쿼리 성능 | JOIN 필요 | 단일 테이블 조회 |
| 시점 데이터 | 불가 | 가능 |
| 원본 삭제 | 영향 있음 | 영향 없음 |

**E-commerce 결론: 스냅샷 필수**

---

## 9. 핵심 파일 요약

| 파일 | 스냅샷 필드 | 용도 |
|------|------------|------|
| `CartItem.java` | productName, price | 장바구니 담은 시점 정보 |
| `OrderItem.java` | productName, price, subtotal | 주문 시점 정보 (불변) |
| `StockMovement.java` | productId, previous/after 수량 | 재고 변동 이력 |

---

## 10. 핵심 요약

1. **시점 가격 보존**: 주문/장바구니 시점의 가격을 복사 저장
2. **원본 독립성**: 상품 삭제/변경과 무관하게 주문 내역 유지
3. **감사 추적**: StockMovement로 재고 변동 전후 상태 기록
4. **비정규화 선택**: 저장 공간보다 데이터 무결성 우선
5. **스냅샷 시점 결정**: 장바구니 담을 때 vs 결제 완료 시 (비즈니스 정책)
