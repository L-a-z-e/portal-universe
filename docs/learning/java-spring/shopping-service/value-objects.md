# Value Objects

## 1. 개요

Value Object는 도메인에서 개념적 일체성을 표현하는 불변 객체입니다. 식별자가 없으며, 속성 값의 조합으로 동등성이 결정됩니다.

## 2. Address (주소)

### 구현

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(name = "receiver_name", length = 100)
    private String receiverName;      // 수령인 이름

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;     // 수령인 연락처

    @Column(name = "zip_code", length = 10)
    private String zipCode;           // 우편번호

    @Column(name = "address1", length = 255)
    private String address1;          // 기본 주소

    @Column(name = "address2", length = 255)
    private String address2;          // 상세 주소

    @Builder
    public Address(String receiverName, String receiverPhone,
                   String zipCode, String address1, String address2) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.address1 = address1;
        this.address2 = address2;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (zipCode != null) {
            sb.append("[").append(zipCode).append("] ");
        }
        if (address1 != null) {
            sb.append(address1);
        }
        if (address2 != null) {
            sb.append(" ").append(address2);
        }
        return sb.toString().trim();
    }
}
```

### 사용 위치

- `Order.shippingAddress`: 주문의 배송 주소
- `Delivery.address`: 배송 정보의 주소

### @Embeddable 사용

JPA `@Embeddable`로 구현하여 Order 테이블에 컬럼으로 매핑:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    -- ... 기타 컬럼 ...
    receiver_name VARCHAR(100),
    receiver_phone VARCHAR(20),
    zip_code VARCHAR(10),
    address1 VARCHAR(255),
    address2 VARCHAR(255)
);
```

### 특징

- **불변성**: 생성 후 변경 불가 (setter 없음)
- **재사용성**: 여러 Entity에서 동일하게 사용
- **자기 완결성**: `getFullAddress()` 같은 도메인 로직 포함

## 3. ProductInfo (상품 정보 스냅샷)

### 개념

CartItem, OrderItem에서 상품 정보를 스냅샷으로 저장합니다. 이는 완전한 Value Object는 아니지만 유사한 패턴입니다.

### 현재 구현 (필드로 분산)

```java
// CartItem
@Column(name = "product_id", nullable = false)
private Long productId;

@Column(name = "product_name", nullable = false, length = 255)
private String productName;   // 스냅샷

@Column(name = "price", nullable = false, precision = 12, scale = 2)
private BigDecimal price;     // 스냅샷
```

### 권장 리팩토링 (Value Object 분리)

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductInfo {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder
    public ProductInfo(Long productId, String productName, BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }

    // 정적 팩토리 메서드
    public static ProductInfo from(Product product) {
        return ProductInfo.builder()
            .productId(product.getId())
            .productName(product.getName())
            .price(BigDecimal.valueOf(product.getPrice()))
            .build();
    }
}
```

### 스냅샷 패턴의 목적

1. **가격 일관성**: 장바구니 담기/주문 시점의 가격 보존
2. **히스토리**: 주문 당시 상품 정보 추적
3. **독립성**: 상품 정보 변경이 기존 주문에 영향 없음

## 4. Money (금액) - 권장 구현

### 현재 상태

현재는 `BigDecimal`을 직접 사용하고 있습니다.

### 권장 구현

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    // 연산 메서드
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return new Money(result);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }

    public Money percentage(BigDecimal percent) {
        return new Money(this.amount.multiply(percent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    // 비교 메서드
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
```

### Money 사용 예시

```java
// 리팩토링 후
Money price = Money.of(10000);
Money total = price.multiply(3);           // 30,000원
Money discount = total.percentage(10);     // 3,000원 (10% 할인)
Money finalAmount = total.subtract(discount); // 27,000원

// 검증
if (orderAmount.isLessThan(coupon.getMinimumAmount())) {
    throw new Exception("최소 주문 금액 미달");
}
```

## 5. Value Object 설계 원칙

### 1. 불변성 (Immutability)

```java
// Good - 새 객체 반환
public Money add(Money other) {
    return new Money(this.amount.add(other.amount));
}

// Bad - 내부 상태 변경
public void add(Money other) {
    this.amount = this.amount.add(other.amount);  // X
}
```

### 2. 값 동등성 (Value Equality)

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return amount.compareTo(money.amount) == 0;  // 값으로 비교
}
```

### 3. Side-Effect Free

```java
// 연산이 외부 상태를 변경하지 않음
public Money multiply(int qty) {
    return new Money(this.amount.multiply(BigDecimal.valueOf(qty)));
}
```

### 4. 자기 유효성 검증 (Self-Validation)

```java
private Money(BigDecimal amount) {
    if (amount == null) {
        throw new IllegalArgumentException("Amount cannot be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Amount cannot be negative");
    }
    this.amount = amount;
}
```

## 6. Entity vs Value Object

| 특성 | Entity | Value Object |
|------|--------|--------------|
| 식별자 | ID로 식별 | 값으로 식별 |
| 동등성 | ID 동등성 | 값 동등성 |
| 생명주기 | 독립적 | 소유 Entity에 종속 |
| 변경 | 상태 변경 가능 | 불변 (교체로 변경) |
| 예시 | Order, Product | Address, Money |

## 7. 소스 위치

- Address: `common/domain/Address.java`
- 스냅샷 패턴: `cart/domain/CartItem.java`, `order/domain/OrderItem.java`
