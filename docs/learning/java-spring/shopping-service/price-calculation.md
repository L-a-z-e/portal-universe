# Price Calculation (가격 계산)

## 개요

주문 금액 계산, 쿠폰 할인 적용, 최종 결제 금액 산정 로직을 설명합니다.

## 가격 계산 구조

```
┌─────────────────────────────────────────────────────────┐
│                      Order                               │
│  ┌─────────────────────────────────────────────────────┐│
│  │ totalAmount = Σ(OrderItem.subtotal)                 ││
│  │ discountAmount = Coupon.calculateDiscount()         ││
│  │ finalAmount = totalAmount - discountAmount          ││
│  └─────────────────────────────────────────────────────┘│
│                           │                              │
│           ┌───────────────┴───────────────┐             │
│           ▼                               ▼             │
│  ┌─────────────────┐           ┌─────────────────┐     │
│  │   OrderItem 1   │           │   OrderItem 2   │     │
│  │ price × quantity│           │ price × quantity│     │
│  │   = subtotal    │           │   = subtotal    │     │
│  └─────────────────┘           └─────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

## OrderItem - 상품 금액 계산

### subtotal 계산

```java
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;      // 단가 (스냅샷)

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;   // 단가 × 수량

    @Builder
    public OrderItem(Order order, Long productId, String productName,
                     BigDecimal price, Integer quantity) {
        this.price = price;
        this.quantity = quantity;
        // subtotal 자동 계산
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

## Order - 총 금액 계산

### totalAmount 재계산

```java
@Entity
@Table(name = "orders")
public class Order {

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;      // 총 주문 금액 (할인 전)

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;  // 할인 금액

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;      // 최종 결제 금액

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * 총 주문 금액을 재계산합니다.
     */
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        calculateFinalAmount();
    }

    /**
     * 최종 결제 금액을 계산합니다.
     */
    private void calculateFinalAmount() {
        BigDecimal discount = this.discountAmount != null
            ? this.discountAmount
            : BigDecimal.ZERO;

        this.finalAmount = this.totalAmount.subtract(discount);

        // 최종 금액이 음수가 되지 않도록
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }
}
```

### 주문에 상품 추가

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
    recalculateTotalAmount();  // 자동 재계산
    return orderItem;
}
```

## Coupon - 할인 금액 계산

### 할인 타입

```java
public enum DiscountType {
    FIXED,      // 정액 할인 (예: 5,000원)
    PERCENTAGE  // 정률 할인 (예: 10%)
}
```

### 할인 계산 로직

```java
@Entity
@Table(name = "coupons")
public class Coupon {

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;        // 할인 값 (금액 또는 %)

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;   // 최소 주문 금액

    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount; // 최대 할인 금액

    /**
     * 주문 금액에 대한 할인 금액을 계산합니다.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        // 1. 최소 주문 금액 검증
        if (minimumOrderAmount != null
            && orderAmount.compareTo(minimumOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        // 2. 할인 타입에 따른 계산
        if (discountType == DiscountType.FIXED) {
            // 정액 할인
            discount = discountValue;
        } else { // PERCENTAGE
            // 정률 할인
            discount = orderAmount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // 3. 최대 할인 금액 제한
        if (maximumDiscountAmount != null
            && discount.compareTo(maximumDiscountAmount) > 0) {
            discount = maximumDiscountAmount;
        }

        // 4. 할인 금액이 주문 금액을 초과하지 않도록
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }
}
```

## 주문 생성 시 쿠폰 적용

```java
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 1. 주문 생성 및 항목 추가
    Order order = Order.builder()
            .userId(userId)
            .shippingAddress(request.shippingAddress().toEntity())
            .build();

    for (CartItem cartItem : cart.getItems()) {
        order.addItem(
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getPrice(),
                cartItem.getQuantity()
        );
    }

    // 2. 쿠폰 적용 (선택 사항)
    if (request.userCouponId() != null) {
        // 쿠폰 검증
        couponService.validateCouponForOrder(
            request.userCouponId(),
            userId,
            order.getTotalAmount()
        );

        // 할인 금액 계산
        BigDecimal discountAmount = couponService.calculateDiscount(
            request.userCouponId(),
            order.getTotalAmount()
        );

        // 주문에 쿠폰 적용
        order.applyCoupon(request.userCouponId(), discountAmount);
    }

    order.confirm();
    return OrderResponse.from(orderRepository.save(order));
}
```

### Order.applyCoupon()

```java
public void applyCoupon(Long userCouponId, BigDecimal discountAmount) {
    this.appliedUserCouponId = userCouponId;
    this.discountAmount = discountAmount;
    calculateFinalAmount();  // finalAmount 재계산
}
```

## 쿠폰 검증 서비스

```java
public void validateCouponForOrder(Long userCouponId, String userId,
                                    BigDecimal orderAmount) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow(() -> new CustomBusinessException(USER_COUPON_NOT_FOUND));

    // 1. 소유자 확인
    if (!userCoupon.getUserId().equals(userId)) {
        throw new CustomBusinessException(USER_COUPON_NOT_FOUND);
    }

    // 2. 사용 가능 여부 확인
    if (!userCoupon.isUsable()) {
        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw new CustomBusinessException(USER_COUPON_ALREADY_USED);
        }
        throw new CustomBusinessException(USER_COUPON_EXPIRED);
    }

    // 3. 최소 주문 금액 확인
    Coupon coupon = userCoupon.getCoupon();
    if (coupon.getMinimumOrderAmount() != null
            && orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
        throw new CustomBusinessException(COUPON_MINIMUM_ORDER_NOT_MET);
    }
}
```

## 가격 계산 예시

### 예시 1: 정액 할인

```
주문 항목:
- 상품 A: 30,000원 × 2개 = 60,000원
- 상품 B: 15,000원 × 1개 = 15,000원

totalAmount = 75,000원

쿠폰: 10,000원 할인 (최소 주문 50,000원)
discountAmount = 10,000원

finalAmount = 75,000 - 10,000 = 65,000원
```

### 예시 2: 정률 할인 (최대 한도)

```
주문 항목:
- 상품 A: 100,000원 × 1개 = 100,000원

totalAmount = 100,000원

쿠폰: 20% 할인 (최대 15,000원)
계산된 할인: 100,000 × 20% = 20,000원
최대 한도 적용: 15,000원

discountAmount = 15,000원
finalAmount = 100,000 - 15,000 = 85,000원
```

### 예시 3: 최소 금액 미달

```
주문 항목:
- 상품 A: 10,000원 × 1개 = 10,000원

totalAmount = 10,000원

쿠폰: 5,000원 할인 (최소 주문 30,000원)
최소 금액 미달로 할인 불가

discountAmount = 0원
finalAmount = 10,000원
```

## 배송비 로직 (향후 구현)

현재는 배송비가 구현되어 있지 않지만, 확장 구조 예시입니다.

```java
public class Order {
    private BigDecimal shippingFee = BigDecimal.ZERO;

    public void calculateShippingFee() {
        // 무료 배송 기준 (예: 50,000원 이상)
        if (this.totalAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            this.shippingFee = BigDecimal.ZERO;
        } else {
            this.shippingFee = DEFAULT_SHIPPING_FEE;  // 예: 3,000원
        }
    }

    private void calculateFinalAmount() {
        BigDecimal discount = this.discountAmount != null
            ? this.discountAmount
            : BigDecimal.ZERO;

        this.finalAmount = this.totalAmount
            .subtract(discount)
            .add(this.shippingFee);  // 배송비 추가

        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }
}
```

## BigDecimal 사용 가이드

금액 계산 시 `BigDecimal`을 사용하는 이유와 주의사항입니다.

### 정밀도 유지

```java
// Good: BigDecimal 사용
BigDecimal price = new BigDecimal("19.99");
BigDecimal quantity = BigDecimal.valueOf(3);
BigDecimal total = price.multiply(quantity);  // 59.97

// Bad: double 사용 (정밀도 손실 가능)
double price = 19.99;
double total = price * 3;  // 59.97000000000001
```

### RoundingMode

```java
// 할인율 계산 시 소수점 처리
BigDecimal discount = orderAmount.multiply(discountRate)
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
```

| RoundingMode | 설명 |
|--------------|------|
| HALF_UP | 반올림 (일반적인 금액 계산) |
| DOWN | 버림 (할인 금액에 보수적) |
| UP | 올림 |

## 관련 파일

- `/order/domain/Order.java` - 주문 엔티티
- `/order/domain/OrderItem.java` - 주문 항목 엔티티
- `/coupon/domain/Coupon.java` - 쿠폰 엔티티
- `/coupon/service/CouponServiceImpl.java` - 쿠폰 서비스
- `/order/service/OrderServiceImpl.java` - 주문 서비스
