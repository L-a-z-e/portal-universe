# Coupon Issuance (쿠폰 발급)

## 개요

선착순 쿠폰 발급 시스템의 동시성 제어 구현을 설명합니다.
Redis Lua Script를 활용하여 원자적 발급을 보장합니다.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  CouponService  │────▶│ CouponRedis     │────▶│     Redis       │
│                 │     │    Service      │     │  (Lua Script)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        ▼                                               ▼
┌─────────────────┐                          ┌─────────────────┐
│ CouponRepository│                          │  coupon:stock:  │
│ UserCoupon      │                          │  coupon:issued: │
│   Repository    │                          │     (Keys)      │
└─────────────────┘                          └─────────────────┘
```

## Redis Key 구조

| Key Pattern | Type | 용도 |
|-------------|------|------|
| `coupon:stock:{couponId}` | String | 남은 재고 수량 |
| `coupon:issued:{couponId}` | Set | 발급받은 사용자 ID 목록 |

## Lua Script (coupon_issue.lua)

```lua
-- KEYS[1] = coupon:stock:{couponId} (쿠폰 재고)
-- KEYS[2] = coupon:issued:{couponId} (발급된 사용자 Set)
-- ARGV[1] = userId
-- ARGV[2] = 최대 발급 수량

-- Return values:
-- 1: 발급 성공
-- 0: 재고 소진
-- -1: 이미 발급됨

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]
local maxQuantity = tonumber(ARGV[2])

-- 1. 이미 발급받았는지 확인
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

-- 2. 현재 재고 확인
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock <= 0 then
    return 0
end

-- 3. 원자적으로 재고 감소
local newStock = redis.call('DECR', stockKey)
if newStock < 0 then
    -- 롤백: 재고가 음수가 되면 다시 증가
    redis.call('INCR', stockKey)
    return 0
end

-- 4. 발급 사용자 기록
redis.call('SADD', issuedKey, userId)

return 1
```

**핵심 포인트:**
- `SISMEMBER`로 중복 발급 방지
- `DECR` 후 음수 체크로 overselling 방지
- 모든 작업이 단일 Lua Script 내에서 원자적으로 실행

## 쿠폰 발급 플로우

### 1. 쿠폰 생성 (관리자)

```java
@Transactional
public CouponResponse createCoupon(CouponCreateRequest request) {
    if (couponRepository.existsByCode(request.code())) {
        throw new CustomBusinessException(COUPON_CODE_ALREADY_EXISTS);
    }

    Coupon coupon = Coupon.builder()
            .code(request.code())
            .name(request.name())
            .discountType(request.discountType())      // FIXED or PERCENTAGE
            .discountValue(request.discountValue())
            .minimumOrderAmount(request.minimumOrderAmount())
            .maximumDiscountAmount(request.maximumDiscountAmount())
            .totalQuantity(request.totalQuantity())
            .startsAt(request.startsAt())
            .expiresAt(request.expiresAt())
            .build();

    Coupon savedCoupon = couponRepository.save(coupon);

    // Redis에 쿠폰 재고 초기화
    couponRedisService.initializeCouponStock(
        savedCoupon.getId(),
        savedCoupon.getTotalQuantity()
    );

    return CouponResponse.from(savedCoupon);
}
```

### 2. 쿠폰 발급 (사용자)

```java
@Transactional
public UserCouponResponse issueCoupon(Long couponId, String userId) {
    Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CustomBusinessException(COUPON_NOT_FOUND));

    // 1. 기본 검증 (활성 상태, 기간, 재고)
    validateCouponForIssue(coupon);

    // 2. Lua Script를 통한 원자적 발급
    Long result = couponRedisService.issueCoupon(
        couponId, userId, coupon.getTotalQuantity()
    );

    if (result == -1) {
        throw new CustomBusinessException(COUPON_ALREADY_ISSUED);
    }
    if (result == 0) {
        throw new CustomBusinessException(COUPON_EXHAUSTED);
    }

    // 3. DB에 발급 기록 저장
    UserCoupon userCoupon = UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .expiresAt(coupon.getExpiresAt())
            .build();

    UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

    // 4. 쿠폰 발급 수량 증가
    coupon.incrementIssuedQuantity();
    couponRepository.save(coupon);

    return UserCouponResponse.from(savedUserCoupon);
}
```

### 3. 발급 검증 로직

```java
private void validateCouponForIssue(Coupon coupon) {
    LocalDateTime now = LocalDateTime.now();

    if (coupon.getStatus() != CouponStatus.ACTIVE) {
        throw new CustomBusinessException(COUPON_INACTIVE);
    }
    if (now.isBefore(coupon.getStartsAt())) {
        throw new CustomBusinessException(COUPON_NOT_STARTED);
    }
    if (now.isAfter(coupon.getExpiresAt())) {
        throw new CustomBusinessException(COUPON_EXPIRED);
    }
    if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
        throw new CustomBusinessException(COUPON_EXHAUSTED);
    }
}
```

## CouponRedisService

```java
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_ISSUED_KEY = "coupon:issued:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> couponIssueScript;

    public void initializeCouponStock(Long couponId, int quantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        redisTemplate.opsForValue().set(stockKey, quantity);
    }

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

    public boolean isAlreadyIssued(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(issuedKey, userId)
        );
    }

    public int getStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        Object stock = redisTemplate.opsForValue().get(stockKey);
        return stock == null ? 0 : Integer.parseInt(stock.toString());
    }
}
```

## Coupon Domain Model

### Coupon Entity

```java
@Entity
@Table(name = "coupons")
public class Coupon {
    private String code;              // 쿠폰 코드 (unique)
    private String name;
    private DiscountType discountType; // FIXED, PERCENTAGE
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;  // 최소 주문 금액
    private BigDecimal maximumDiscountAmount; // 최대 할인 금액
    private Integer totalQuantity;    // 총 발급 수량
    private Integer issuedQuantity;   // 발급된 수량
    private CouponStatus status;      // ACTIVE, EXHAUSTED, EXPIRED, INACTIVE
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
}
```

### 할인 계산 로직

```java
public BigDecimal calculateDiscount(BigDecimal orderAmount) {
    // 최소 주문 금액 검증
    if (minimumOrderAmount != null && orderAmount.compareTo(minimumOrderAmount) < 0) {
        return BigDecimal.ZERO;
    }

    BigDecimal discount;
    if (discountType == DiscountType.FIXED) {
        discount = discountValue;
    } else { // PERCENTAGE
        discount = orderAmount.multiply(discountValue)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // 최대 할인 금액 제한
    if (maximumDiscountAmount != null && discount.compareTo(maximumDiscountAmount) > 0) {
        discount = maximumDiscountAmount;
    }

    // 할인 금액이 주문 금액을 초과하지 않도록
    if (discount.compareTo(orderAmount) > 0) {
        discount = orderAmount;
    }

    return discount;
}
```

## 동시성 테스트 시나리오

```java
@Test
void 선착순_쿠폰_100개_동시_발급_테스트() throws InterruptedException {
    // Given: 100개 한정 쿠폰
    Coupon coupon = createCoupon(100);

    // When: 500명이 동시에 요청
    int threadCount = 500;
    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
        String userId = "user-" + i;
        executor.submit(() -> {
            try {
                couponService.issueCoupon(coupon.getId(), userId);
                successCount.incrementAndGet();
            } catch (CustomBusinessException e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // Then: 정확히 100개만 발급
    assertThat(successCount.get()).isEqualTo(100);
    assertThat(failCount.get()).isEqualTo(400);
}
```

## Error Codes

| Code | 설명 |
|------|------|
| `COUPON_NOT_FOUND` | 쿠폰을 찾을 수 없음 |
| `COUPON_CODE_ALREADY_EXISTS` | 중복된 쿠폰 코드 |
| `COUPON_INACTIVE` | 비활성 쿠폰 |
| `COUPON_NOT_STARTED` | 아직 시작하지 않은 쿠폰 |
| `COUPON_EXPIRED` | 만료된 쿠폰 |
| `COUPON_EXHAUSTED` | 재고 소진 |
| `COUPON_ALREADY_ISSUED` | 이미 발급받은 쿠폰 |
| `COUPON_MINIMUM_ORDER_NOT_MET` | 최소 주문 금액 미달 |

## 관련 파일

- `/coupon/service/CouponServiceImpl.java` - 쿠폰 서비스
- `/coupon/redis/CouponRedisService.java` - Redis 연동
- `/resources/scripts/coupon_issue.lua` - Lua Script
- `/coupon/domain/Coupon.java` - 쿠폰 엔티티
- `/coupon/domain/UserCoupon.java` - 사용자 쿠폰 엔티티
