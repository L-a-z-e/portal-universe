# Snapshot Pattern (스냅샷 패턴)

## 개요

주문 시점의 상품 정보(가격, 이름 등)를 OrderItem에 스냅샷으로 저장하는 패턴을 설명합니다.
원본 상품 데이터가 변경되어도 주문 이력의 무결성을 보장합니다.

## 왜 스냅샷이 필요한가?

### 문제 상황

```
시나리오: 상품 가격 변경 후 주문 조회

1. 사용자 A가 상품 X를 10,000원에 주문
2. 관리자가 상품 X의 가격을 12,000원으로 인상
3. 사용자 A가 주문 내역 조회
   - 스냅샷 없이 Product 참조: 12,000원으로 표시 (잘못됨)
   - 스냅샷 사용: 10,000원으로 표시 (정확함)
```

### 스냅샷의 장점

1. **데이터 무결성**: 주문 시점의 정확한 정보 유지
2. **감사(Audit) 추적**: 과거 거래 내역 정확히 보존
3. **분쟁 해결**: 주문 당시 조건을 증명 가능
4. **성능 최적화**: Product 테이블 JOIN 없이 주문 정보 조회

## 구현 구조

### OrderItem Entity

```java
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * 상품 ID (Product 테이블 참조용)
     * - 상품 상세 페이지 링크 등에 사용
     * - 재주문 기능에 활용
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 상품명 (스냅샷)
     * - 주문 시점의 상품명 저장
     * - Product.name이 변경되어도 영향 없음
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 단가 (스냅샷)
     * - 주문 시점의 가격 저장
     * - Product.price가 변경되어도 영향 없음
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * 수량
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 소계 (단가 × 수량)
     * - 계산된 값을 저장하여 조회 성능 향상
     */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Builder
    public OrderItem(Order order, Long productId, String productName,
                     BigDecimal price, Integer quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;  // 스냅샷 저장
        this.price = price;              // 스냅샷 저장
        this.quantity = quantity;
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

## 스냅샷 생성 시점

### 장바구니 → 주문 변환

```java
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 체크아웃된 장바구니 조회
    Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
            .orElseThrow(() -> new CustomBusinessException(CART_NOT_FOUND));

    // 주문 생성
    Order order = Order.builder()
            .userId(userId)
            .shippingAddress(request.shippingAddress().toEntity())
            .build();

    // 장바구니 항목을 주문 항목으로 변환 (스냅샷 생성)
    for (CartItem cartItem : cart.getItems()) {
        order.addItem(
                cartItem.getProductId(),
                cartItem.getProductName(),  // 스냅샷: 장바구니에 담긴 시점의 이름
                cartItem.getPrice(),        // 스냅샷: 장바구니에 담긴 시점의 가격
                cartItem.getQuantity()
        );
    }

    return OrderResponse.from(orderRepository.save(order));
}
```

### Order.addItem() 메서드

```java
public OrderItem addItem(Long productId, String productName,
                         BigDecimal price, int quantity) {
    OrderItem orderItem = OrderItem.builder()
            .order(this)
            .productId(productId)
            .productName(productName)   // 스냅샷 저장
            .price(price)               // 스냅샷 저장
            .quantity(quantity)
            .build();

    this.items.add(orderItem);
    recalculateTotalAmount();
    return orderItem;
}
```

## 데이터 모델 비교

### 스냅샷 방식 (현재 구현)

```
┌─────────────────────────────────────────────────────┐
│                    order_items                       │
├─────────────────────────────────────────────────────┤
│ id          │ BIGINT      │ PK                      │
│ order_id    │ BIGINT      │ FK → orders             │
│ product_id  │ BIGINT      │ 참조용 (FK 아님)        │
│ product_name│ VARCHAR(255)│ ★ 스냅샷                │
│ price       │ DECIMAL     │ ★ 스냅샷                │
│ quantity    │ INT         │                         │
│ subtotal    │ DECIMAL     │ ★ 계산된 스냅샷         │
└─────────────────────────────────────────────────────┘
```

### 참조 방식 (스냅샷 없는 경우)

```
┌─────────────────────────────────────────────────────┐
│                    order_items                       │
├─────────────────────────────────────────────────────┤
│ id          │ BIGINT      │ PK                      │
│ order_id    │ BIGINT      │ FK → orders             │
│ product_id  │ BIGINT      │ FK → products           │
│ quantity    │ INT         │                         │
│ -- price, name은 Product 조인으로 조회 --          │
└─────────────────────────────────────────────────────┘

문제점:
- Product 데이터 변경 시 과거 주문 금액도 변경됨
- Product 삭제 시 주문 데이터 무결성 깨짐
- 조회 시 항상 JOIN 필요 (성능 저하)
```

## 스냅샷 대상 필드

### 필수 스냅샷 필드

| 필드 | 이유 |
|------|------|
| `price` | 결제 금액 계산의 기준 |
| `productName` | 주문 내역 표시 |

### 선택적 스냅샷 필드 (확장 가능)

| 필드 | 사용 사례 |
|------|----------|
| `imageUrl` | 주문 내역에 이미지 표시 |
| `options` | 상품 옵션 정보 (색상, 사이즈 등) |
| `discountPrice` | 할인가 적용 정보 |
| `sellerId` | 판매자 정보 (마켓플레이스) |

### 확장 예시

```java
@Entity
public class OrderItem {
    // 기존 필드...

    @Column(name = "image_url")
    private String imageUrl;           // 상품 이미지 URL 스냅샷

    @Column(name = "options", columnDefinition = "JSON")
    private String options;            // 옵션 정보 (JSON)

    @Column(name = "original_price")
    private BigDecimal originalPrice;  // 정가 (할인 전 가격)

    @Column(name = "seller_name")
    private String sellerName;         // 판매자 이름
}
```

## 주문 조회 시 데이터 흐름

### Response DTO

```java
public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,     // 스냅샷된 이름
    BigDecimal price,       // 스냅샷된 가격
    Integer quantity,
    BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
            orderItem.getId(),
            orderItem.getProductId(),
            orderItem.getProductName(),   // OrderItem에서 직접 조회
            orderItem.getPrice(),         // Product JOIN 불필요
            orderItem.getQuantity(),
            orderItem.getSubtotal()
        );
    }
}
```

### 조회 쿼리 (Product JOIN 없음)

```sql
-- 스냅샷 방식: 단일 테이블 조회
SELECT oi.* FROM order_items oi
WHERE oi.order_id = :orderId;

-- 참조 방식: JOIN 필요
SELECT oi.*, p.name, p.price FROM order_items oi
JOIN products p ON oi.product_id = p.id
WHERE oi.order_id = :orderId;
```

## productId 유지 이유

스냅샷에도 `productId`를 유지하는 이유:

1. **재주문 기능**: 동일 상품 다시 주문
2. **상품 상세 링크**: 주문 내역에서 상품 페이지 이동
3. **리뷰 작성**: 구매 확인 후 리뷰 작성
4. **통계/분석**: 상품별 판매 통계

```java
// 재주문 예시
public void reorder(String orderNumber) {
    Order order = orderRepository.findByOrderNumber(orderNumber);

    for (OrderItem item : order.getItems()) {
        // productId로 현재 상품 정보 조회
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow();

        // 현재 가격으로 장바구니에 추가
        cartService.addItem(product.getId(), item.getQuantity());
    }
}
```

## 일관성 고려사항

### 장바구니 스냅샷

장바구니 항목도 추가 시점의 정보를 스냅샷합니다.

```java
@Entity
public class CartItem {
    private Long productId;
    private String productName;  // 장바구니 추가 시점 스냅샷
    private BigDecimal price;    // 장바구니 추가 시점 스냅샷
    private Integer quantity;
}
```

### 가격 변경 알림

장바구니에 담긴 상품 가격이 변경된 경우 사용자에게 알림:

```java
// 체크아웃 시 가격 변경 감지
public void validateCartPrices(Cart cart) {
    for (CartItem item : cart.getItems()) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow();

        if (!item.getPrice().equals(product.getPrice())) {
            // 가격 변경 알림
            throw new CustomBusinessException(PRICE_CHANGED,
                "상품 '" + item.getProductName() + "'의 가격이 변경되었습니다.");
        }
    }
}
```

## 관련 파일

- `/order/domain/OrderItem.java` - 주문 항목 (스냅샷 필드 포함)
- `/order/domain/Order.java` - 주문 (addItem 메서드)
- `/order/dto/OrderItemResponse.java` - 응답 DTO
- `/cart/domain/CartItem.java` - 장바구니 항목
- `/order/service/OrderServiceImpl.java` - 주문 생성 로직
