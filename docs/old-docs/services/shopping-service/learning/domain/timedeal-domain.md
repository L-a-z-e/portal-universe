# TimeDeal Domain

## 1. 개요

TimeDeal 도메인은 시간 제한 특가 판매(타임딜, 플래시 세일)를 관리합니다. Redis Lua Script를 활용하여 선착순 구매를 원자적으로 처리합니다.

## 2. Entity 구조

### TimeDeal Entity

```java
@Entity
@Table(name = "time_deals")
public class TimeDeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                    // 타임딜 이름

    private String description;             // 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeDealStatus status = TimeDealStatus.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime startsAt;         // 시작 시간

    @Column(nullable = false)
    private LocalDateTime endsAt;           // 종료 시간

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeDealProduct> products = new ArrayList<>();  // 타임딜 상품들

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### TimeDealProduct Entity

```java
@Entity
@Table(name = "time_deal_products")
public class TimeDealProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_deal_id", nullable = false)
    private TimeDeal timeDeal;              // 타임딜 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;                // 상품 참조

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dealPrice;           // 타임딜 가격

    @Column(nullable = false)
    private Integer dealQuantity;           // 판매 수량

    @Column(nullable = false)
    private Integer soldQuantity = 0;       // 판매된 수량

    @Column(nullable = false)
    private Integer maxPerUser;             // 1인당 최대 구매 수량
}
```

### TimeDealPurchase Entity

```java
@Entity
@Table(name = "time_deal_purchases", indexes = {
    @Index(name = "idx_tdp_user_product", columnList = "user_id, time_deal_product_id")
})
public class TimeDealPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;                  // 구매자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_deal_product_id", nullable = false)
    private TimeDealProduct timeDealProduct; // 타임딜 상품 참조

    @Column(nullable = false)
    private Integer quantity;               // 구매 수량

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;       // 구매 가격

    @Column(name = "order_id")
    private Long orderId;                   // 연결된 주문 ID

    private LocalDateTime purchasedAt;      // 구매 시간
}
```

## 3. 타임딜 상태 (TimeDealStatus)

```java
public enum TimeDealStatus {
    SCHEDULED,  // 예정됨
    ACTIVE,     // 진행중
    ENDED,      // 종료됨
    CANCELLED   // 취소됨
}
```

### 상태 전이 다이어그램

```
                  시작 시간 도달
┌───────────┐  ─────────────────>  ┌──────────┐
│ SCHEDULED │                      │  ACTIVE  │
└─────┬─────┘                      └────┬─────┘
      │                                 │
      │ cancel()                        │ 종료 시간 도달
      v                                 │
┌───────────┐                           v
│ CANCELLED │                     ┌──────────┐
└───────────┘                     │  ENDED   │
                                  └──────────┘
```

### 자동 상태 전환 (Scheduler)

```java
@Scheduled(fixedRate = 60000)  // 1분마다 실행
public void updateTimeDealStatus() {
    // 시작해야 할 타임딜 활성화
    timeDealRepository.findScheduledDeals().forEach(deal -> {
        if (deal.shouldStart()) {
            deal.activate();
            initializeRedisStock(deal);
        }
    });

    // 종료해야 할 타임딜 종료
    timeDealRepository.findActiveDeals().forEach(deal -> {
        if (deal.shouldEnd()) {
            deal.end();
            cleanupRedisCache(deal);
        }
    });
}
```

## 4. Redis 기반 선착순 구매

### TimeDealRedisService

```java
@Service
public class TimeDealRedisService {
    private static final String TIMEDEAL_STOCK_KEY = "timedeal:stock:";
    private static final String TIMEDEAL_PURCHASED_KEY = "timedeal:purchased:";

    // 재고 초기화 (타임딜 시작 시)
    public void initializeStock(Long timeDealId, Long productId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        redisTemplate.opsForValue().set(stockKey, quantity);
    }

    // Lua Script를 통한 원자적 구매
    // 반환값: > 0 = 성공 (남은 재고), 0 = 재고 소진, -1 = 구매 제한 초과
    public Long purchaseProduct(Long timeDealId, Long productId, String userId,
                                int requestedQuantity, int maxPerUser) {
        String stockKey = buildStockKey(timeDealId, productId);
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);

        return redisTemplate.execute(
            timeDealPurchaseScript,
            Arrays.asList(stockKey, purchasedKey),
            String.valueOf(requestedQuantity),
            String.valueOf(maxPerUser)
        );
    }

    // 재고 롤백 (구매 취소 시)
    public void rollbackStock(Long timeDealId, Long productId, String userId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);

        redisTemplate.opsForValue().increment(stockKey, quantity);
        redisTemplate.opsForValue().decrement(purchasedKey, quantity);
    }
}
```

### Lua Script (원자적 구매)

```lua
-- KEYS[1]: timedeal:stock:{timeDealId}:{productId}
-- KEYS[2]: timedeal:purchased:{timeDealId}:{productId}:{userId}
-- ARGV[1]: requestedQuantity
-- ARGV[2]: maxPerUser

local stock = tonumber(redis.call('GET', KEYS[1]) or 0)
local purchased = tonumber(redis.call('GET', KEYS[2]) or 0)
local requested = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 1인당 구매 제한 확인
if purchased + requested > maxPerUser then
    return -1  -- 구매 제한 초과
end

-- 재고 확인
if stock < requested then
    return 0  -- 재고 소진
end

-- 재고 감소 및 구매 수량 증가
redis.call('DECRBY', KEYS[1], requested)
redis.call('INCRBY', KEYS[2], requested)

return stock - requested  -- 남은 재고 반환
```

## 5. 구매 처리 흐름

```
1. 타임딜 구매 요청
   │
   v
2. 타임딜 유효성 검증
   ├─ 상태 = ACTIVE?
   └─ 현재 시간이 startsAt ~ endsAt 범위 내?
   │
   v
3. Redis Lua Script 실행 (원자적)
   ├─ 구매 제한 초과? → -1 반환 → TIMEDEAL_PURCHASE_LIMIT_EXCEEDED
   ├─ 재고 없음? → 0 반환 → TIMEDEAL_SOLD_OUT
   └─ 구매 성공 → 남은 재고 반환
   │
   v
4. DB에 TimeDealPurchase 저장
   │
   v
5. TimeDealProduct.soldQuantity 증가
   │
   v
6. (Optional) 주문 생성 연동
```

## 6. 할인율 계산

```java
public BigDecimal getDiscountRate() {
    BigDecimal originalPrice = BigDecimal.valueOf(product.getPrice());
    if (originalPrice.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ZERO;
    }
    return originalPrice.subtract(dealPrice)
        .divide(originalPrice, 2, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
}
```

### 예시

| 원가 | 타임딜가 | 할인율 |
|------|----------|--------|
| 50,000원 | 35,000원 | 30% |
| 100,000원 | 70,000원 | 30% |
| 10,000원 | 5,000원 | 50% |

## 7. 비즈니스 메서드

### TimeDeal 메서드

```java
// 타임딜 활성화
public void activate() {
    this.status = TimeDealStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
}

// 타임딜 종료
public void end() {
    this.status = TimeDealStatus.ENDED;
    this.updatedAt = LocalDateTime.now();
}

// 타임딜 취소
public void cancel() {
    this.status = TimeDealStatus.CANCELLED;
    this.updatedAt = LocalDateTime.now();
}

// 상품 추가
public void addProduct(TimeDealProduct product) {
    this.products.add(product);
    product.setTimeDeal(this);
}

// 활성 상태 확인
public boolean isActive() {
    LocalDateTime now = LocalDateTime.now();
    return this.status == TimeDealStatus.ACTIVE
        && now.isAfter(this.startsAt)
        && now.isBefore(this.endsAt);
}

// 시작해야 하는지 확인
public boolean shouldStart() {
    return this.status == TimeDealStatus.SCHEDULED
        && LocalDateTime.now().isAfter(this.startsAt);
}

// 종료해야 하는지 확인
public boolean shouldEnd() {
    return this.status == TimeDealStatus.ACTIVE
        && LocalDateTime.now().isAfter(this.endsAt);
}
```

### TimeDealProduct 메서드

```java
// 판매 수량 증가
public void incrementSoldQuantity(int quantity) {
    this.soldQuantity += quantity;
}

// 남은 수량
public int getRemainingQuantity() {
    return this.dealQuantity - this.soldQuantity;
}

// 구매 가능 여부
public boolean isAvailable() {
    return this.soldQuantity < this.dealQuantity;
}
```

## 8. Redis Key 구조

| Key 패턴 | 설명 | 예시 |
|----------|------|------|
| `timedeal:stock:{dealId}:{productId}` | 타임딜 재고 | `timedeal:stock:1:100` |
| `timedeal:purchased:{dealId}:{productId}:{userId}` | 사용자별 구매 수량 | `timedeal:purchased:1:100:user123` |

## 9. Error Codes

| 코드 | 설명 |
|------|------|
| `S701` | TIMEDEAL_NOT_FOUND - 타임딜을 찾을 수 없음 |
| `S702` | TIMEDEAL_NOT_ACTIVE - 진행 중인 타임딜이 아님 |
| `S703` | TIMEDEAL_EXPIRED - 타임딜 종료 |
| `S704` | TIMEDEAL_SOLD_OUT - 타임딜 상품 품절 |
| `S705` | TIMEDEAL_PURCHASE_LIMIT_EXCEEDED - 구매 제한 초과 |
| `S706` | TIMEDEAL_PRODUCT_NOT_FOUND - 타임딜 상품을 찾을 수 없음 |
| `S707` | TIMEDEAL_ALREADY_EXISTS - 이미 존재하는 타임딜 |
| `S708` | TIMEDEAL_INVALID_PERIOD - 유효하지 않은 기간 설정 |

## 10. 소스 위치

- Entity: `timedeal/domain/TimeDeal.java`, `TimeDealProduct.java`, `TimeDealPurchase.java`
- Enum: `timedeal/domain/TimeDealStatus.java`
- Repository: `timedeal/repository/TimeDealRepository.java`, `TimeDealProductRepository.java`, `TimeDealPurchaseRepository.java`
- Service: `timedeal/service/TimeDealService.java`, `TimeDealServiceImpl.java`
- Redis: `timedeal/redis/TimeDealRedisService.java`
- Scheduler: `timedeal/scheduler/TimeDealScheduler.java`
- Controller: `timedeal/controller/TimeDealController.java`, `AdminTimeDealController.java`
