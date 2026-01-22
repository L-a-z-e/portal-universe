# Coupon Domain

## 1. 개요

Coupon 도메인은 할인 쿠폰의 생성, 발급, 사용을 관리합니다. Redis를 활용한 선착순 발급 처리와 다양한 할인 정책을 지원합니다.

## 2. Entity 구조

### Coupon Entity

```java
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;                    // 쿠폰 코드 (WELCOME2024 등)

    @Column(nullable = false, length = 100)
    private String name;                    // 쿠폰명

    private String description;             // 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;      // 할인 유형

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;       // 할인 값 (금액 또는 퍼센트)

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;  // 최소 주문 금액

    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount; // 최대 할인 금액

    @Column(nullable = false)
    private Integer totalQuantity;          // 총 발급 가능 수량

    @Column(nullable = false)
    private Integer issuedQuantity = 0;     // 발급된 수량

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime startsAt;         // 발급 시작일

    @Column(nullable = false)
    private LocalDateTime expiresAt;        // 발급 종료일
}
```

### UserCoupon Entity (발급된 쿠폰)

```java
@Entity
@Table(name = "user_coupons",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_id"}))
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;                  // 사용자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;                  // 쿠폰 참조

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;

    @Column(name = "order_id")
    private Long usedOrderId;               // 사용된 주문 ID

    private LocalDateTime issuedAt;         // 발급 시간
    private LocalDateTime usedAt;           // 사용 시간
    private LocalDateTime expiresAt;        // 만료 시간
}
```

## 3. 상태 Enum

### DiscountType (할인 유형)

```java
public enum DiscountType {
    FIXED,      // 고정 금액 할인 (예: 1000원)
    PERCENTAGE  // 퍼센트 할인 (예: 10%)
}
```

### CouponStatus (쿠폰 상태)

```java
public enum CouponStatus {
    ACTIVE,     // 발급 가능
    EXHAUSTED,  // 소진됨
    EXPIRED,    // 만료됨
    INACTIVE    // 비활성화
}
```

### UserCouponStatus (발급된 쿠폰 상태)

```java
public enum UserCouponStatus {
    AVAILABLE,  // 사용 가능
    USED,       // 사용됨
    EXPIRED     // 만료됨
}
```

## 4. 할인 계산 로직

```java
public BigDecimal calculateDiscount(BigDecimal orderAmount) {
    // 1. 최소 주문 금액 검증
    if (minimumOrderAmount != null &&
        orderAmount.compareTo(minimumOrderAmount) < 0) {
        return BigDecimal.ZERO;
    }

    // 2. 할인 금액 계산
    BigDecimal discount;
    if (discountType == DiscountType.FIXED) {
        discount = discountValue;
    } else { // PERCENTAGE
        discount = orderAmount.multiply(discountValue)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // 3. 최대 할인 금액 제한
    if (maximumDiscountAmount != null &&
        discount.compareTo(maximumDiscountAmount) > 0) {
        discount = maximumDiscountAmount;
    }

    // 4. 할인 금액이 주문 금액을 초과하지 않도록
    if (discount.compareTo(orderAmount) > 0) {
        discount = orderAmount;
    }

    return discount;
}
```

### 할인 예시

| 쿠폰 설정 | 주문 금액 | 할인 금액 |
|-----------|-----------|-----------|
| FIXED 3,000원 | 10,000원 | 3,000원 |
| PERCENTAGE 10%, max 5,000원 | 30,000원 | 3,000원 |
| PERCENTAGE 10%, max 5,000원 | 100,000원 | 5,000원 (최대 제한) |
| FIXED 3,000원, min 5,000원 | 3,000원 | 0원 (최소 미달) |

## 5. Redis 기반 선착순 발급

### CouponRedisService

```java
@Service
public class CouponRedisService {
    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_ISSUED_KEY = "coupon:issued:";

    // 재고 초기화
    public void initializeCouponStock(Long couponId, int quantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        redisTemplate.opsForValue().set(stockKey, quantity);
    }

    // Lua Script를 통한 원자적 발급
    // 반환값: 1=성공, 0=재고 소진, -1=이미 발급됨
    public Long issueCoupon(Long couponId, String userId, int maxQuantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;

        return redisTemplate.execute(
            couponIssueScript,
            Arrays.asList(stockKey, issuedKey),
            String.valueOf(userId),
            String.valueOf(maxQuantity)
        );
    }
}
```

### Lua Script (원자적 발급)

```lua
-- KEYS[1]: coupon:stock:{couponId}
-- KEYS[2]: coupon:issued:{couponId}
-- ARGV[1]: userId
-- ARGV[2]: maxQuantity

-- 이미 발급 여부 확인
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1  -- 이미 발급됨
end

-- 재고 확인 및 감소
local stock = tonumber(redis.call('GET', KEYS[1]) or 0)
if stock <= 0 then
    return 0  -- 재고 소진
end

-- 재고 감소 및 발급 기록
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

return 1  -- 발급 성공
```

## 6. 발급 흐름

```
1. 쿠폰 발급 요청
   │
   v
2. 쿠폰 유효성 검증
   ├─ 상태 = ACTIVE?
   ├─ 발급 기간 내?
   └─ 발급 가능 수량 남음?
   │
   v
3. Redis Lua Script 실행 (원자적)
   ├─ 이미 발급됨? → -1 반환
   ├─ 재고 없음? → 0 반환
   └─ 발급 성공 → 1 반환
   │
   v
4. DB에 UserCoupon 저장
   │
   v
5. Coupon.issuedQuantity 증가
```

## 7. 쿠폰 사용 흐름

```
1. 주문 시 쿠폰 적용 요청
   │
   v
2. UserCoupon 검증
   ├─ 소유자 확인
   ├─ 상태 = AVAILABLE?
   └─ 만료일 체크
   │
   v
3. 할인 금액 계산
   │
   v
4. 주문에 쿠폰 적용
   └─ Order.applyCoupon(userCouponId, discountAmount)
   │
   v
5. 결제 완료 시 UserCoupon 사용 처리
   └─ userCoupon.use(orderId)
```

## 8. 비즈니스 규칙

### 발급 규칙

- 사용자당 동일 쿠폰 1회만 발급 (`UniqueConstraint`)
- 발급 기간 내에만 발급 가능
- 총 발급 수량 제한
- 비활성 쿠폰은 발급 불가

### 사용 규칙

- AVAILABLE 상태의 쿠폰만 사용 가능
- 만료일 이전에만 사용 가능
- 최소 주문 금액 충족 필요
- 한 주문에 하나의 쿠폰만 적용

## 9. Error Codes

| 코드 | 설명 |
|------|------|
| `S601` | COUPON_NOT_FOUND - 쿠폰을 찾을 수 없음 |
| `S602` | COUPON_EXHAUSTED - 쿠폰 소진 |
| `S603` | COUPON_EXPIRED - 쿠폰 만료 |
| `S604` | COUPON_ALREADY_ISSUED - 이미 발급된 쿠폰 |
| `S605` | COUPON_NOT_STARTED - 발급 기간 전 |
| `S606` | COUPON_INACTIVE - 비활성 쿠폰 |
| `S607` | COUPON_CODE_ALREADY_EXISTS - 쿠폰 코드 중복 |
| `S608` | USER_COUPON_NOT_FOUND - 발급된 쿠폰 없음 |
| `S609` | USER_COUPON_ALREADY_USED - 이미 사용된 쿠폰 |
| `S610` | USER_COUPON_EXPIRED - 쿠폰 만료 |
| `S611` | COUPON_MINIMUM_ORDER_NOT_MET - 최소 주문 금액 미달 |

## 10. 소스 위치

- Entity: `coupon/domain/Coupon.java`, `UserCoupon.java`
- Enum: `coupon/domain/DiscountType.java`, `CouponStatus.java`, `UserCouponStatus.java`
- Repository: `coupon/repository/CouponRepository.java`, `UserCouponRepository.java`
- Service: `coupon/service/CouponService.java`, `CouponServiceImpl.java`
- Redis: `coupon/redis/CouponRedisService.java`
- Controller: `coupon/controller/CouponController.java`, `AdminCouponController.java`
