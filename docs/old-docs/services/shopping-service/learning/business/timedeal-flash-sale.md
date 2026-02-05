# TimeDeal / Flash Sale (타임딜)

## 개요

대용량 트래픽 환경에서의 타임딜(Flash Sale) 구현을 설명합니다.
Redis Lua Script를 활용하여 재고 동시성 제어와 1인당 구매 수량 제한을 처리합니다.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ TimeDealService │────▶│ TimeDealRedis   │────▶│     Redis       │
│                 │     │    Service      │     │  (Lua Script)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        ▼                                               ▼
┌─────────────────┐                          ┌──────────────────┐
│ TimeDeal        │                          │ timedeal:stock:  │
│ TimeDealProduct │                          │ timedeal:        │
│ TimeDealPurchase│                          │   purchased:     │
└─────────────────┘                          └──────────────────┘
```

## Redis Key 구조

| Key Pattern | Type | 용도 |
|-------------|------|------|
| `timedeal:stock:{dealId}:{productId}` | String | 남은 재고 수량 |
| `timedeal:purchased:{dealId}:{productId}:{userId}` | String | 사용자별 구매 수량 |

## Lua Script (timedeal_purchase.lua)

```lua
-- KEYS[1] = timedeal:stock:{dealId}:{productId} (타임딜 재고)
-- KEYS[2] = timedeal:purchased:{dealId}:{productId}:{userId} (사용자별 구매 수량)
-- ARGV[1] = 구매 요청 수량
-- ARGV[2] = 1인당 최대 구매 수량

-- Return values:
-- > 0: 구매 성공 (남은 재고 수량)
-- 0: 재고 소진
-- -1: 1인당 구매 제한 초과

local stockKey = KEYS[1]
local purchasedKey = KEYS[2]
local requestedQuantity = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 1. 사용자의 현재 구매 수량 확인
local currentPurchased = tonumber(redis.call('GET', purchasedKey) or 0)
if currentPurchased + requestedQuantity > maxPerUser then
    return -1
end

-- 2. 현재 재고 확인
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock < requestedQuantity then
    return 0
end

-- 3. 원자적으로 재고 감소
local newStock = redis.call('DECRBY', stockKey, requestedQuantity)
if newStock < 0 then
    -- 롤백: 재고가 음수가 되면 다시 증가
    redis.call('INCRBY', stockKey, requestedQuantity)
    return 0
end

-- 4. 사용자 구매 수량 증가
redis.call('INCRBY', purchasedKey, requestedQuantity)

-- 남은 재고 반환
return newStock
```

## Domain Model

### TimeDeal Entity

```java
@Entity
@Table(name = "time_deals")
public class TimeDeal {
    private Long id;
    private String name;
    private String description;
    private TimeDealStatus status;  // SCHEDULED, ACTIVE, ENDED, CANCELLED
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL)
    private List<TimeDealProduct> products = new ArrayList<>();

    public void addProduct(TimeDealProduct product) {
        this.products.add(product);
        product.setTimeDeal(this);
    }
}
```

### TimeDealProduct Entity

```java
@Entity
@Table(name = "time_deal_products")
public class TimeDealProduct {
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TimeDeal timeDeal;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private BigDecimal dealPrice;     // 할인가
    private Integer dealQuantity;     // 판매 수량
    private Integer soldQuantity;     // 판매된 수량
    private Integer maxPerUser;       // 1인당 최대 구매 수량

    public void incrementSoldQuantity(int quantity) {
        this.soldQuantity += quantity;
    }
}
```

### TimeDealPurchase Entity

```java
@Entity
@Table(name = "time_deal_purchases")
public class TimeDealPurchase {
    private Long id;
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    private TimeDealProduct timeDealProduct;

    private Integer quantity;
    private BigDecimal purchasePrice;
    private LocalDateTime purchasedAt;
}
```

## 타임딜 구매 플로우

### 1. 타임딜 생성 (관리자)

```java
@Transactional
public TimeDealResponse createTimeDeal(TimeDealCreateRequest request) {
    validateTimeDealPeriod(request.startsAt(), request.endsAt());

    TimeDeal timeDeal = TimeDeal.builder()
            .name(request.name())
            .description(request.description())
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .build();

    for (TimeDealProductRequest productRequest : request.products()) {
        Product product = productRepository.findById(productRequest.productId())
                .orElseThrow(() -> new CustomBusinessException(PRODUCT_NOT_FOUND));

        TimeDealProduct timeDealProduct = TimeDealProduct.builder()
                .product(product)
                .dealPrice(productRequest.dealPrice())
                .dealQuantity(productRequest.dealQuantity())
                .maxPerUser(productRequest.maxPerUser())
                .build();

        timeDeal.addProduct(timeDealProduct);
    }

    return TimeDealResponse.from(timeDealRepository.save(timeDeal));
}
```

### 2. 타임딜 구매

```java
@Transactional
public TimeDealPurchaseResponse purchaseTimeDeal(String userId, TimeDealPurchaseRequest request) {
    TimeDealProduct timeDealProduct = timeDealProductRepository
            .findByIdWithProductAndDeal(request.timeDealProductId())
            .orElseThrow(() -> new CustomBusinessException(TIMEDEAL_PRODUCT_NOT_FOUND));

    TimeDeal timeDeal = timeDealProduct.getTimeDeal();

    // 1. 타임딜 상태 검증
    validateTimeDealForPurchase(timeDeal);

    // 2. Lua Script를 통한 원자적 구매 처리
    Long result = timeDealRedisService.purchaseProduct(
            timeDeal.getId(),
            timeDealProduct.getProduct().getId(),
            userId,
            request.quantity(),
            timeDealProduct.getMaxPerUser()
    );

    if (result == -1) {
        throw new CustomBusinessException(TIMEDEAL_PURCHASE_LIMIT_EXCEEDED);
    }
    if (result == 0) {
        throw new CustomBusinessException(TIMEDEAL_SOLD_OUT);
    }

    // 3. DB에 구매 기록 저장
    TimeDealPurchase purchase = TimeDealPurchase.builder()
            .userId(userId)
            .timeDealProduct(timeDealProduct)
            .quantity(request.quantity())
            .purchasePrice(timeDealProduct.getDealPrice())
            .build();

    TimeDealPurchase savedPurchase = timeDealPurchaseRepository.save(purchase);

    // 4. 판매 수량 업데이트
    timeDealProduct.incrementSoldQuantity(request.quantity());
    timeDealProductRepository.save(timeDealProduct);

    return TimeDealPurchaseResponse.from(savedPurchase);
}
```

### 3. 타임딜 검증

```java
private void validateTimeDealForPurchase(TimeDeal timeDeal) {
    if (timeDeal.getStatus() != TimeDealStatus.ACTIVE) {
        throw new CustomBusinessException(TIMEDEAL_NOT_ACTIVE);
    }

    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(timeDeal.getStartsAt()) || now.isAfter(timeDeal.getEndsAt())) {
        throw new CustomBusinessException(TIMEDEAL_EXPIRED);
    }
}
```

## TimeDealRedisService

```java
@Service
@RequiredArgsConstructor
public class TimeDealRedisService {

    private static final String TIMEDEAL_STOCK_KEY = "timedeal:stock:";
    private static final String TIMEDEAL_PURCHASED_KEY = "timedeal:purchased:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> timeDealPurchaseScript;

    public void initializeStock(Long timeDealId, Long productId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        redisTemplate.opsForValue().set(stockKey, quantity);
    }

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

    public void rollbackStock(Long timeDealId, Long productId,
                              String userId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);

        redisTemplate.opsForValue().increment(stockKey, quantity);
        redisTemplate.opsForValue().decrement(purchasedKey, quantity);
    }

    private String buildStockKey(Long timeDealId, Long productId) {
        return TIMEDEAL_STOCK_KEY + timeDealId + ":" + productId;
    }

    private String buildPurchasedKey(Long timeDealId, Long productId, String userId) {
        return TIMEDEAL_PURCHASED_KEY + timeDealId + ":" + productId + ":" + userId;
    }
}
```

## 타임딜 Scheduler

```java
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealRedisService timeDealRedisService;

    // 1분마다 타임딜 상태 업데이트
    @Scheduled(fixedRate = 60000)
    public void updateTimeDealStatus() {
        LocalDateTime now = LocalDateTime.now();

        // SCHEDULED -> ACTIVE 전환
        timeDealRepository.findByStatusAndStartsAtBefore(
            TimeDealStatus.SCHEDULED, now
        ).forEach(timeDeal -> {
            timeDeal.activate();

            // Redis에 재고 초기화
            timeDeal.getProducts().forEach(product -> {
                timeDealRedisService.initializeStock(
                    timeDeal.getId(),
                    product.getProduct().getId(),
                    product.getDealQuantity()
                );
            });
        });

        // ACTIVE -> ENDED 전환
        timeDealRepository.findByStatusAndEndsAtBefore(
            TimeDealStatus.ACTIVE, now
        ).forEach(TimeDeal::end);
    }
}
```

## 대용량 트래픽 처리 전략

### 1. Redis Lua Script 활용
- 원자적 연산으로 Race Condition 방지
- DB 접근 최소화로 응답 시간 단축

### 2. 대기열 시스템 연동
- 트래픽 급증 시 `QueueService`와 연동
- 순차적 입장으로 시스템 부하 분산

### 3. Rate Limiting
```java
// API Gateway 레벨에서 Rate Limiting 적용
// 사용자당 초당 요청 수 제한
```

### 4. 캐싱 전략
```java
// 타임딜 정보 캐싱 (조회 빈도 높음)
@Cacheable(value = "timedeal", key = "#timeDealId")
public TimeDealResponse getTimeDeal(Long timeDealId) {
    // ...
}
```

## Error Codes

| Code | 설명 |
|------|------|
| `TIMEDEAL_NOT_FOUND` | 타임딜을 찾을 수 없음 |
| `TIMEDEAL_PRODUCT_NOT_FOUND` | 타임딜 상품을 찾을 수 없음 |
| `TIMEDEAL_NOT_ACTIVE` | 활성화되지 않은 타임딜 |
| `TIMEDEAL_EXPIRED` | 만료된 타임딜 |
| `TIMEDEAL_SOLD_OUT` | 품절 |
| `TIMEDEAL_PURCHASE_LIMIT_EXCEEDED` | 1인당 구매 제한 초과 |
| `TIMEDEAL_INVALID_PERIOD` | 잘못된 기간 설정 |

## 관련 파일

- `/timedeal/service/TimeDealServiceImpl.java` - 타임딜 서비스
- `/timedeal/redis/TimeDealRedisService.java` - Redis 연동
- `/resources/scripts/timedeal_purchase.lua` - Lua Script
- `/timedeal/scheduler/TimeDealScheduler.java` - 스케줄러
- `/timedeal/domain/*.java` - 도메인 엔티티들
