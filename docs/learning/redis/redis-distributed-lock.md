# Redis 분산 락 (Distributed Lock)

## 학습 목표
- 분산 환경에서 동시성 제어의 필요성 이해
- Redisson을 활용한 분산 락 구현 방법 습득
- Portal Universe의 실제 구현 패턴 학습

---

## 1. 분산 락이란?

분산 시스템에서 **여러 인스턴스가 동일 리소스에 동시 접근**하는 것을 제어하는 메커니즘입니다.

### 문제 상황: 재고 차감

```
[문제] 재고 100개, 동시에 3명이 구매 시도

Instance 1: 재고 조회 → 100개
Instance 2: 재고 조회 → 100개
Instance 3: 재고 조회 → 100개

Instance 1: 99개로 업데이트
Instance 2: 99개로 업데이트 (동시에!)
Instance 3: 99개로 업데이트 (동시에!)

결과: 3개 판매했는데 재고는 99개 (2개 손실!)
```

### 분산 락 적용 후

```
[해결] 분산 락으로 순차 처리

Instance 1: 락 획득 → 재고 조회(100) → 99로 업데이트 → 락 해제
                   ↓ 락 대기
Instance 2:        락 획득 → 재고 조회(99) → 98로 업데이트 → 락 해제
                                            ↓ 락 대기
Instance 3:                                 락 획득 → 재고 조회(98) → 97로 업데이트

결과: 3개 판매, 재고 97개 (정확!)
```

---

## 2. Redisson 설정

### 2.1 의존성

```groovy
dependencies {
    implementation 'org.redisson:redisson-spring-boot-starter:3.23.0'
}
```

### 2.2 RedisConfig.java (Shopping Service)

```java
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Single Server 모드
        config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setConnectionPoolSize(50)           // 연결 풀 크기
            .setRetryAttempts(3)                 // 재시도 횟수
            .setRetryInterval(1500)              // 재시도 간격 (ms)
            .setTimeout(3000)                    // 타임아웃 (ms)
            .setThreads(16)                      // 스레드 수
            .setNettyThreads(32);                // Netty 스레드 수

        return Redisson.create(config);
    }
}
```

---

## 3. 어노테이션 기반 분산 락

### 3.1 커스텀 어노테이션 정의

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키 (SpEL 표현식 지원)
     * 예: "'coupon:' + #couponId"
     */
    String key();

    /**
     * 락 획득 대기 시간 (기본 5초)
     */
    long waitTime() default 5L;

    /**
     * 락 유지 시간 (기본 10초)
     */
    long leaseTime() default 10L;

    /**
     * 시간 단위 (기본 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
```

### 3.2 AOP 구현

```java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint,
                         DistributedLock distributedLock) throws Throwable {

        // 1. SpEL로 동적 키 생성
        String lockKey = resolveLockKey(distributedLock.key(), joinPoint);

        // 2. Redisson 락 객체 획득
        RLock lock = redissonClient.getLock("lock:" + lockKey);

        boolean acquired = false;
        try {
            // 3. 락 획득 시도
            acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new CustomBusinessException(
                    ShoppingErrorCode.CONCURRENT_STOCK_MODIFICATION
                );
            }

            log.debug("Lock acquired: {}", lockKey);

            // 4. 비즈니스 로직 실행
            return joinPoint.proceed();

        } finally {
            // 5. 락 해제 (현재 스레드가 보유 중일 때만)
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    /**
     * SpEL 표현식을 파싱하여 실제 키 값 생성
     */
    private String resolveLockKey(String keyExpression,
                                   ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 파라미터 이름과 값 매핑
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // SpEL 파싱
        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
```

---

## 4. 사용 예시

### 4.1 쿠폰 발급

```java
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    @Override
    @DistributedLock(key = "'coupon:' + #couponId", waitTime = 3, leaseTime = 5)
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {
        // 락이 획득된 상태에서 실행됨

        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CustomBusinessException(COUPON_NOT_FOUND));

        // 2. 이미 발급 확인
        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new CustomBusinessException(COUPON_ALREADY_ISSUED);
        }

        // 3. 재고 확인 및 차감
        if (coupon.getRemaining() <= 0) {
            throw new CustomBusinessException(COUPON_OUT_OF_STOCK);
        }
        coupon.decreaseRemaining();

        // 4. 사용자 쿠폰 발급
        UserCoupon userCoupon = UserCoupon.issue(coupon, userId);
        userCouponRepository.save(userCoupon);

        return CouponIssueResponse.from(userCoupon);
    }
}
```

### 4.2 재고 차감

```java
@Service
public class InventoryServiceImpl implements InventoryService {

    @Override
    @DistributedLock(key = "'inventory:' + #productId")
    public void decreaseStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new CustomBusinessException(INVENTORY_NOT_FOUND));

        if (inventory.getStock() < quantity) {
            throw new CustomBusinessException(INSUFFICIENT_STOCK);
        }

        inventory.decrease(quantity);
        inventoryRepository.save(inventory);
    }
}
```

---

## 5. Lua 스크립트를 활용한 원자적 연산

AOP 락 외에도 **Lua 스크립트**로 원자적 연산을 구현할 수 있습니다.

### 5.1 쿠폰 발급 Lua 스크립트

```lua
-- coupon_issue.lua
-- KEYS[1] = coupon:stock:{couponId}
-- KEYS[2] = coupon:issued:{couponId}
-- ARGV[1] = userId
-- ARGV[2] = maxQuantity

-- 이미 발급 확인
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1  -- 이미 발급됨
end

-- 재고 확인 및 차감
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock <= 0 then
    return 0   -- 재고 없음
end

-- 재고 감소
local newStock = redis.call('DECR', KEYS[1])
if newStock < 0 then
    redis.call('INCR', KEYS[1])  -- 롤백
    return 0
end

-- 발급 기록
redis.call('SADD', KEYS[2], ARGV[1])
return 1  -- 성공
```

### 5.2 Spring에서 Lua 스크립트 실행

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<Long> couponIssueScript() {
        return RedisScript.of(
            new ClassPathResource("scripts/coupon_issue.lua"),
            Long.class
        );
    }
}

@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> couponIssueScript;

    public CouponIssueResult issueCoupon(Long couponId, Long userId) {
        List<String> keys = Arrays.asList(
            "coupon:stock:" + couponId,
            "coupon:issued:" + couponId
        );

        Long result = redisTemplate.execute(
            couponIssueScript,
            keys,
            userId.toString()
        );

        return switch (result.intValue()) {
            case 1 -> CouponIssueResult.SUCCESS;
            case 0 -> CouponIssueResult.OUT_OF_STOCK;
            case -1 -> CouponIssueResult.ALREADY_ISSUED;
            default -> CouponIssueResult.UNKNOWN_ERROR;
        };
    }
}
```

### 5.3 타임딜 구매 Lua 스크립트

```lua
-- timedeal_purchase.lua
-- KEYS[1] = timedeal:stock:{dealId}:{productId}
-- KEYS[2] = timedeal:purchased:{dealId}:{productId}:{userId}
-- ARGV[1] = requestedQuantity
-- ARGV[2] = maxPerUser

local requestedQty = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 1인당 구매 제한 확인
local currentPurchased = tonumber(redis.call('GET', KEYS[2]) or '0')
if currentPurchased + requestedQty > maxPerUser then
    return -1  -- 구매 제한 초과
end

-- 재고 확인 및 차감
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock < requestedQty then
    return 0   -- 재고 부족
end

-- 재고 감소
local newStock = redis.call('DECRBY', KEYS[1], requestedQty)
if newStock < 0 then
    redis.call('INCRBY', KEYS[1], requestedQty)  -- 롤백
    return 0
end

-- 구매 기록 업데이트
redis.call('INCRBY', KEYS[2], requestedQty)
return newStock  -- 성공, 남은 재고 반환
```

---

## 6. 분산 락 vs Lua 스크립트

| 특성 | 분산 락 (Redisson) | Lua 스크립트 |
|------|-------------------|-------------|
| **용도** | 복잡한 비즈니스 로직 | 단순 원자 연산 |
| **DB 접근** | 가능 | Redis 데이터만 |
| **코드 복잡도** | 낮음 (어노테이션) | 중간 (스크립트 작성) |
| **성능** | 락 오버헤드 존재 | 매우 빠름 |
| **재시도** | 자동 대기 | 직접 구현 필요 |

**Portal Universe 선택 기준:**
- **쿠폰 발급**: Lua 스크립트 (단순 재고 + 사용자 확인)
- **타임딜**: Lua 스크립트 (단순 재고 + 구매 제한)
- **주문 처리**: 분산 락 (DB 트랜잭션 포함)

---

## 7. 모범 사례

### 7.1 락 타임아웃 설정

```java
@DistributedLock(
    key = "'order:' + #orderId",
    waitTime = 3,    // 최대 3초 대기
    leaseTime = 10   // 최대 10초 보유
)
```

**waitTime 설정 원칙:**
- 너무 짧으면: 락 획득 실패 빈번
- 너무 길면: 응답 지연

**leaseTime 설정 원칙:**
- 비즈니스 로직 실행 시간의 2~3배
- 네트워크 지연, GC 고려

### 7.2 락 키 설계

```java
// Good: 구체적인 리소스 식별
"lock:coupon:123"
"lock:inventory:product:456"
"lock:order:ORD-2024-001"

// Bad: 너무 넓은 범위
"lock:coupon"           // 모든 쿠폰이 직렬화됨
"lock:inventory"        // 모든 재고가 직렬화됨
```

### 7.3 예외 처리

```java
@DistributedLock(key = "'product:' + #productId")
public void updateProduct(Long productId, ProductRequest request) {
    try {
        // 비즈니스 로직
    } catch (OptimisticLockingFailureException e) {
        // JPA Optimistic Lock 충돌 시 재시도
        throw new CustomBusinessException(CONCURRENT_MODIFICATION);
    }
}
```

---

## 8. 대기열 시스템과 연계

대량 트래픽 상황에서는 분산 락 대신 **대기열 시스템**을 사용합니다.

```java
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 대기열 입장 (Sorted Set - 시간순 정렬)
     */
    public QueueEntryResponse enterQueue(String eventType,
                                          Long eventId,
                                          Long userId) {
        String key = "queue:waiting:" + eventType + ":" + eventId;
        String token = generateToken(userId);

        // 입장 시간을 스코어로 사용
        redisTemplate.opsForZSet().add(key, token, System.currentTimeMillis());

        // 현재 순번 조회
        Long position = redisTemplate.opsForZSet().rank(key, token);
        Long totalWaiting = redisTemplate.opsForZSet().zCard(key);

        return new QueueEntryResponse(token, position + 1, totalWaiting);
    }

    /**
     * 배치 단위로 입장 처리
     */
    @Scheduled(fixedRate = 5000)  // 5초마다
    public void processEntries() {
        String waitingKey = "queue:waiting:timedeal:123";
        String enteredKey = "queue:entered:timedeal:123";
        int batchSize = 100;  // 한 번에 100명씩

        // 상위 N명 가져오기
        Set<Object> entries = redisTemplate.opsForZSet()
            .popMin(waitingKey, batchSize);

        if (entries != null) {
            for (Object entry : entries) {
                redisTemplate.opsForSet().add(enteredKey, entry);
            }
        }
    }
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **분산 락** | 다중 인스턴스 환경의 동시성 제어 |
| **Redisson** | Redis 기반 분산 락 라이브러리 |
| **tryLock** | 락 획득 시도 (대기 시간, 유지 시간 설정) |
| **Lua 스크립트** | Redis 원자적 복합 연산 |
| **SpEL** | 동적 락 키 생성 |

---

## 다음 학습

- [Redis 캐싱 패턴](./redis-caching-patterns.md)
- [Redis Rate Limiting](./redis-rate-limiting.md)
- [Portal Universe Redis 적용](./redis-portal-universe.md)

---

## 참고 자료

- [Redisson 공식 문서](https://github.com/redisson/redisson/wiki)
- [Redis Distributed Locks (Redlock)](https://redis.io/topics/distlock)
- [Spring AOP 레퍼런스](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
