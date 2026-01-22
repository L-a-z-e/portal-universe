# 재고 동시성 제어

## 학습 목표
- 재고 관리의 동시성 문제 이해
- 낙관적/비관적 잠금 전략 학습
- Redis 기반 원자적 재고 처리 패턴

---

## 1. 동시성 문제

### 1.1 Lost Update 문제

```
[재고 100개, 동시에 2명이 각 1개 구매]

Thread A                      Thread B
---------                     ---------
읽기: stock = 100
                              읽기: stock = 100
stock = 99
                              stock = 99
저장: 99
                              저장: 99

결과: 2개 팔았는데 재고 99개 (1개 손실!)
```

### 1.2 문제 발생 시나리오

| 상황 | 위험도 | 발생 확률 |
|------|--------|----------|
| 일반 주문 | 중간 | 낮음 |
| 인기 상품 | 높음 | 중간 |
| 선착순 쿠폰 | 매우 높음 | 높음 |
| 타임딜 (선착순) | 매우 높음 | 매우 높음 |

---

## 2. 동시성 제어 전략

### 2.1 전략 비교

| 전략 | 적용 상황 | 장점 | 단점 |
|------|----------|------|------|
| **Optimistic Lock** | 일반 주문 | 락 경합 적음 | 충돌 시 재시도 |
| **Pessimistic Lock** | 재고 부족 예상 | 확실한 보장 | 성능 저하 |
| **Redis 분산 락** | 다중 인스턴스 | 확장성 | 복잡도 |
| **Lua Script** | 선착순 특가 | 원자성 + 고성능 | Redis 필수 |

### 2.2 Portal Universe 적용

```
┌─────────────────────────────────────────────────────────────────┐
│                    동시성 제어 전략 선택                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  일반 주문                 타임딜/쿠폰                           │
│  ┌────────────────┐       ┌────────────────────────────┐        │
│  │ Optimistic Lock│       │  Redis + Lua Script       │        │
│  │   (JPA @Version)│       │  (원자적 재고 확인+차감)   │        │
│  └────────────────┘       └────────────────────────────┘        │
│         │                           │                            │
│         ▼                           ▼                            │
│  ┌────────────────┐       ┌────────────────────────────┐        │
│  │   Inventory    │       │  TimeDealRedisService     │        │
│  │   Entity       │       │  CouponRedisService       │        │
│  └────────────────┘       └────────────────────────────┘        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Optimistic Lock (낙관적 잠금)

### 3.1 구현

```java
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long productId;

    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;

    @Version  // 낙관적 잠금 버전 필드
    private Long version;

    /**
     * 재고 예약 (주문 생성 시)
     */
    public void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    /**
     * 재고 확정 (결제 완료 시)
     */
    public void deduct(int quantity) {
        if (reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_STATE);
        }
        reservedQuantity -= quantity;
        totalQuantity -= quantity;
    }

    /**
     * 예약 해제 (주문 취소 시)
     */
    public void release(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
}
```

### 3.2 동작 원리

```sql
-- 조회 시 version 포함
SELECT * FROM inventory WHERE product_id = 1;
-- 결과: id=1, available=100, version=5

-- 업데이트 시 version 체크 + 증가
UPDATE inventory
SET available_quantity = 99,
    reserved_quantity = 1,
    version = 6
WHERE id = 1 AND version = 5;  -- version 일치해야 성공

-- 다른 트랜잭션이 먼저 수정한 경우
-- → 영향 받은 행 = 0 → OptimisticLockException
```

### 3.3 재시도 처리

```java
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_RETRY = 3;

    @Transactional
    public void reserveWithRetry(Long productId, int quantity) {
        int attempts = 0;

        while (attempts < MAX_RETRY) {
            try {
                Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new CustomBusinessException(INVENTORY_NOT_FOUND));

                inventory.reserve(quantity);
                inventoryRepository.save(inventory);
                return; // 성공

            } catch (OptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= MAX_RETRY) {
                    throw new CustomBusinessException(CONCURRENT_STOCK_MODIFICATION);
                }
                // 재시도 전 잠시 대기
                try {
                    Thread.sleep(50 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

---

## 4. Pessimistic Lock (비관적 잠금)

### 4.1 구현

```java
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
```

### 4.2 동작 원리

```sql
-- PESSIMISTIC_WRITE = SELECT ... FOR UPDATE
SELECT * FROM inventory
WHERE product_id = 1
FOR UPDATE;  -- 행 수준 잠금

-- 다른 트랜잭션은 대기 (타임아웃까지)
-- 트랜잭션 커밋/롤백 시 잠금 해제
```

### 4.3 사용 시점

```java
@Service
public class InventoryServiceImpl implements InventoryService {

    /**
     * 재고가 부족할 가능성이 높은 경우 비관적 잠금 사용
     */
    @Transactional
    public void reserveCritical(Long productId, int quantity) {
        // FOR UPDATE로 행 잠금
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow(() -> new CustomBusinessException(INVENTORY_NOT_FOUND));

        // 재고 확인 및 예약 (잠금 상태에서)
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);

        // 트랜잭션 종료 시 잠금 자동 해제
    }
}
```

---

## 5. Redis 분산 락 (Redisson)

다중 인스턴스 환경에서 동시성 제어가 필요한 경우 사용합니다.

### 5.1 AOP 기반 분산 락

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                    // SpEL 표현식
    long waitTime() default 5L;      // 대기 시간
    long leaseTime() default 10L;    // 유지 시간
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint,
                         DistributedLock distributedLock) throws Throwable {

        String lockKey = resolveLockKey(distributedLock.key(), joinPoint);
        RLock lock = redissonClient.getLock("lock:" + lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                throw new CustomBusinessException(CONCURRENT_STOCK_MODIFICATION);
            }

            return joinPoint.proceed();

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 5.2 사용 예시

```java
@Service
public class CouponServiceImpl implements CouponService {

    @Override
    @DistributedLock(key = "'coupon:' + #couponId", waitTime = 3, leaseTime = 5)
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {
        // 락이 획득된 상태에서 실행
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow();

        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new CustomBusinessException(COUPON_ALREADY_ISSUED);
        }

        if (coupon.getRemaining() <= 0) {
            throw new CustomBusinessException(COUPON_OUT_OF_STOCK);
        }

        coupon.decreaseRemaining();
        UserCoupon userCoupon = UserCoupon.issue(coupon, userId);
        userCouponRepository.save(userCoupon);

        return CouponIssueResponse.from(userCoupon);
    }
}
```

---

## 6. Redis Lua Script (원자적 연산)

선착순 특가에서 최고 성능이 필요한 경우 사용합니다.

### 6.1 타임딜 구매 스크립트

```lua
-- timedeal_purchase.lua
-- KEYS[1] = timedeal:stock:{dealId}:{productId}
-- KEYS[2] = timedeal:purchased:{dealId}:{productId}:{userId}
-- ARGV[1] = requestedQuantity
-- ARGV[2] = maxPerUser

local requestedQty = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 1. 1인당 구매 제한 확인
local currentPurchased = tonumber(redis.call('GET', KEYS[2]) or '0')
if currentPurchased + requestedQty > maxPerUser then
    return -1  -- 구매 제한 초과
end

-- 2. 재고 확인
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock < requestedQty then
    return 0   -- 재고 부족
end

-- 3. 원자적 재고 감소
local newStock = redis.call('DECRBY', KEYS[1], requestedQty)
if newStock < 0 then
    -- 음수 방지: 롤백
    redis.call('INCRBY', KEYS[1], requestedQty)
    return 0
end

-- 4. 구매 기록 업데이트
redis.call('INCRBY', KEYS[2], requestedQty)

return newStock  -- 성공, 남은 재고 반환
```

### 6.2 Java 연동

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<Long> timeDealPurchaseScript() {
        return RedisScript.of(
            new ClassPathResource("scripts/timedeal_purchase.lua"),
            Long.class
        );
    }
}

@Service
@RequiredArgsConstructor
public class TimeDealRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> timeDealPurchaseScript;

    /**
     * 타임딜 구매 시도 (원자적)
     *
     * @return > 0: 성공 (남은 재고), 0: 재고 부족, -1: 구매 제한 초과
     */
    public TimeDealPurchaseResult tryPurchase(
            Long dealId,
            Long productId,
            Long userId,
            int requestedQty,
            int maxPerUser) {

        List<String> keys = Arrays.asList(
            "timedeal:stock:" + dealId + ":" + productId,
            "timedeal:purchased:" + dealId + ":" + productId + ":" + userId
        );

        Long result = redisTemplate.execute(
            timeDealPurchaseScript,
            keys,
            String.valueOf(requestedQty),
            String.valueOf(maxPerUser)
        );

        return switch (result.intValue()) {
            case -1 -> TimeDealPurchaseResult.LIMIT_EXCEEDED;
            case 0 -> TimeDealPurchaseResult.OUT_OF_STOCK;
            default -> TimeDealPurchaseResult.success(result.intValue());
        };
    }
}
```

### 6.3 쿠폰 발급 스크립트

```lua
-- coupon_issue.lua
-- KEYS[1] = coupon:stock:{couponId}
-- KEYS[2] = coupon:issued:{couponId}
-- ARGV[1] = userId

-- 1. 중복 발급 확인 (Set에 존재하는지)
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1  -- 이미 발급됨
end

-- 2. 재고 확인 및 차감
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock <= 0 then
    return 0   -- 재고 없음
end

local newStock = redis.call('DECR', KEYS[1])
if newStock < 0 then
    redis.call('INCR', KEYS[1])  -- 롤백
    return 0
end

-- 3. 발급 기록 (Set에 추가)
redis.call('SADD', KEYS[2], ARGV[1])

return 1  -- 성공
```

---

## 7. 전략 선택 가이드

### 7.1 결정 트리

```
                      ┌─────────────────────────┐
                      │ 동시성 처리 필요?        │
                      └───────────┬─────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
                   Yes                          No
                    │                           │
                    ▼                           ▼
          ┌─────────────────┐          일반 트랜잭션
          │ 다중 인스턴스?   │
          └────────┬────────┘
                   │
         ┌─────────┴─────────┐
         │                   │
        Yes                  No
         │                   │
         ▼                   ▼
┌────────────────┐   ┌────────────────┐
│ 고성능 필요?    │   │ 충돌 빈번?     │
└───────┬────────┘   └───────┬────────┘
        │                    │
   ┌────┴────┐          ┌────┴────┐
   │         │          │         │
  Yes        No        Yes        No
   │         │          │         │
   ▼         ▼          ▼         ▼
Lua Script  분산 락    비관적     낙관적
(Redis)    (Redisson)   Lock      Lock
```

### 7.2 Portal Universe 적용 사례

| 기능 | 동시성 전략 | 이유 |
|------|------------|------|
| 일반 주문 | Optimistic Lock | 충돌 확률 낮음, 재시도로 해결 |
| 재고 부족 상품 | Pessimistic Lock | 확실한 재고 확보 필요 |
| 선착순 쿠폰 | Redis Lua Script | 초당 수천 건 처리 |
| 타임딜 구매 | Redis Lua Script | 1인당 제한 + 재고 원자 처리 |
| 대기열 입장 | Redis Sorted Set | 순서 보장 + 고성능 |

---

## 8. StockMovement 감사 추적

모든 재고 변동을 기록하여 문제 발생 시 추적 가능하게 합니다.

### 8.1 기록 항목

```java
@Entity
public class StockMovement {

    @Enumerated(EnumType.STRING)
    private MovementType movementType;
    // INBOUND, RESERVE, RELEASE, DEDUCT, RETURN, ADJUSTMENT

    private Integer quantity;
    private Integer previousAvailable;
    private Integer afterAvailable;
    private Integer previousReserved;
    private Integer afterReserved;

    private String referenceType;   // ORDER, PAYMENT, ADJUSTMENT
    private String referenceId;     // 주문번호, 결제번호 등
    private String reason;          // 변경 사유
    private String performedBy;     // 수행자 (시스템 or 관리자)

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 8.2 기록 예시

```java
@Service
public class InventoryServiceImpl implements InventoryService {

    @Transactional
    public void reserve(Long productId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow();

        // 이전 상태 저장
        int prevAvailable = inventory.getAvailableQuantity();
        int prevReserved = inventory.getReservedQuantity();

        // 재고 예약
        inventory.reserve(quantity);

        // 변동 기록
        StockMovement movement = StockMovement.builder()
            .inventory(inventory)
            .productId(productId)
            .movementType(MovementType.RESERVE)
            .quantity(quantity)
            .previousAvailable(prevAvailable)
            .afterAvailable(inventory.getAvailableQuantity())
            .previousReserved(prevReserved)
            .afterReserved(inventory.getReservedQuantity())
            .referenceType("ORDER")
            .referenceId(orderNumber)
            .reason("주문 생성에 따른 재고 예약")
            .performedBy("SYSTEM")
            .build();

        stockMovementRepository.save(movement);
    }
}
```

---

## 9. 핵심 정리

| 전략 | 장점 | 단점 | 적용 |
|------|------|------|------|
| **Optimistic Lock** | 락 경합 없음 | 충돌 시 재시도 | 일반 주문 |
| **Pessimistic Lock** | 확실한 보장 | DB 락 경합 | 재고 부족 예상 |
| **Redis 분산 락** | 다중 인스턴스 | 복잡도 증가 | 쿠폰 발급 |
| **Lua Script** | 원자성 + 고성능 | Redis 필수 | 타임딜 |

---

## 다음 학습

- [쿠폰 발급 시스템](./coupon-issuance.md)
- [타임딜 시스템](./timedeal-flash-sale.md)
- [대기열 시스템](./queue-system.md)
