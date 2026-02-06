# Redis in Portal Universe

Portal Universe 프로젝트에서 Redis가 어떻게 사용되는지 분석합니다.

## 목차

1. [Redis 사용 개요](#1-redis-사용-개요)
2. [Auth Service](#2-auth-service)
3. [Shopping Service](#3-shopping-service)
4. [Notification Service](#4-notification-service)
5. [공통 설정](#5-공통-설정)
6. [아키텍처 다이어그램](#6-아키텍처-다이어그램)
7. [학습 포인트](#7-학습-포인트)

---

## 1. Redis 사용 개요

### 서비스별 Redis 활용

```
Portal Universe Redis 아키텍처:

+------------------+        +------------------+        +------------------+
|   Auth Service   |        | Shopping Service |        |Notification Svc  |
+------------------+        +------------------+        +------------------+
| - Token Blacklist|        | - Coupon Stock   |        | - Pub/Sub        |
| - Refresh Token  |        | - TimeDeal Stock |        | - WebSocket 연동 |
| - Session (opt)  |        | - Queue (ZSet)   |        |                  |
+------------------+        | - Distributed Lock|       +------------------+
         |                  | - Lua Scripts    |                |
         |                  +------------------+                |
         |                           |                         |
         +---------------------------+-------------------------+
                                     |
                              +------v------+
                              |    Redis    |
                              |   Server    |
                              +-------------+
```

### 주요 사용 패턴

| 서비스 | 패턴 | 자료구조 |
|--------|------|----------|
| Auth | Token Blacklist | String (TTL) |
| Auth | Refresh Token | String |
| Shopping | 쿠폰 재고 | String + Set |
| Shopping | 대기열 | Sorted Set |
| Shopping | 분산 락 | Redisson Lock |
| Shopping | 재고 관리 | Lua Script |
| Notification | 실시간 알림 | Pub/Sub |

---

## 2. Auth Service

### 파일 구조

```
services/auth-service/
├── src/main/java/.../
│   ├── config/
│   │   └── RedisConfig.java
│   └── service/
│       ├── TokenBlacklistService.java
│       └── RefreshTokenService.java
└── src/main/resources/
    └── application.yml
```

### Token Blacklist Service

로그아웃된 Access Token을 관리합니다.

```java
// TokenBlacklistService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token을 블랙리스트에 추가
     * TTL = 토큰의 남은 만료 시간
     */
    public void addToBlacklist(String token, long remainingExpiration) {
        if (remainingExpiration <= 0) {
            log.warn("Cannot blacklist expired token");
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(
            key,
            "blacklisted",
            remainingExpiration,
            TimeUnit.MILLISECONDS
        );
        log.info("Token added to blacklist with TTL: {}ms", remainingExpiration);
    }

    /**
     * Token이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

### 설계 포인트

```
Token Blacklist 흐름:

로그아웃 요청 시:
+--------+     +-------------+     +-------+
| Client | --> | Auth Service| --> | Redis |
+--------+     +-------------+     +-------+
                    |
              1. JWT 검증
              2. 남은 만료 시간 계산
              3. Redis에 blacklist:{token} 저장 (TTL 설정)


API 요청 시:
+--------+     +-------------+     +-------+
| Client | --> | JWT Filter  | --> | Redis |
+--------+     +-------------+     +-------+
                    |
              1. JWT 서명 검증
              2. Redis blacklist 확인
              3. 블랙리스트에 있으면 401 반환

장점:
- Stateless JWT의 장점 유지
- 로그아웃 기능 구현 가능
- TTL로 자동 정리 (메모리 효율)
```

### Refresh Token Service

```java
// RefreshTokenService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_PREFIX = "refresh:";
    private static final long REFRESH_TTL_DAYS = 7;

    public void saveRefreshToken(String userId, String refreshToken) {
        String key = REFRESH_PREFIX + userId;
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            REFRESH_TTL_DAYS,
            TimeUnit.DAYS
        );
    }

    public boolean validateRefreshToken(String userId, String refreshToken) {
        String key = REFRESH_PREFIX + userId;
        String stored = (String) redisTemplate.opsForValue().get(key);
        return refreshToken.equals(stored);
    }

    public void deleteRefreshToken(String userId) {
        String key = REFRESH_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
```

---

## 3. Shopping Service

### 파일 구조

```
services/shopping-service/
├── src/main/java/.../
│   ├── common/
│   │   ├── config/
│   │   │   └── RedisConfig.java
│   │   ├── aop/
│   │   │   └── DistributedLockAspect.java
│   │   └── annotation/
│   │       └── DistributedLock.java
│   ├── coupon/
│   │   └── redis/
│   │       └── CouponRedisService.java
│   ├── timedeal/
│   │   └── redis/
│   │       └── TimeDealRedisService.java
│   └── queue/
│       └── service/
│           └── QueueServiceImpl.java
└── src/main/resources/
    ├── scripts/
    │   ├── coupon_issue.lua
    │   └── timedeal_purchase.lua
    └── redisson.yml
```

### Redis 설정 (Redisson 기반)

```java
// RedisConfig.java
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setConnectionMinimumIdleSize(5)
            .setConnectionPoolSize(50)
            .setIdleConnectionTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);
        config.setThreads(16);
        config.setNettyThreads(32);
        return Redisson.create(config);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        return template;
    }

    @Bean
    public DefaultRedisScript<Long> couponIssueScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("scripts/coupon_issue.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
```

### 쿠폰 발급 (Lua Script)

```lua
-- scripts/coupon_issue.lua
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

-- 이미 발급받았는지 확인
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

-- 현재 재고 확인
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock <= 0 then
    return 0
end

-- 원자적으로 재고 감소
local newStock = redis.call('DECR', stockKey)
if newStock < 0 then
    -- 롤백: 재고가 음수가 되면 다시 증가
    redis.call('INCR', stockKey)
    return 0
end

-- 발급 사용자 기록
redis.call('SADD', issuedKey, userId)

return 1
```

### Coupon Redis Service

```java
// CouponRedisService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_ISSUED_KEY = "coupon:issued:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> couponIssueScript;

    /**
     * 쿠폰 재고 초기화
     */
    public void initializeCouponStock(Long couponId, int quantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        redisTemplate.opsForValue().set(stockKey, quantity);
        log.info("Initialized coupon stock: couponId={}, quantity={}", couponId, quantity);
    }

    /**
     * Lua Script를 사용하여 원자적으로 쿠폰 발급
     * @return 1: 성공, 0: 재고 소진, -1: 이미 발급됨
     */
    public Long issueCoupon(Long couponId, String userId, int maxQuantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;

        Long result = redisTemplate.execute(
            couponIssueScript,
            Arrays.asList(stockKey, issuedKey),
            String.valueOf(userId),
            String.valueOf(maxQuantity)
        );

        log.debug("Coupon issue result: couponId={}, userId={}, result={}",
            couponId, userId, result);
        return result;
    }

    /**
     * 이미 발급 여부 확인
     */
    public boolean isAlreadyIssued(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(issuedKey, userId));
    }

    /**
     * 현재 재고 조회
     */
    public int getStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        Object stock = redisTemplate.opsForValue().get(stockKey);
        return stock != null ? Integer.parseInt(stock.toString()) : 0;
    }
}
```

### 대기열 시스템 (Sorted Set)

```java
// QueueServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueServiceImpl implements QueueService {

    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ENTERED_KEY_PREFIX = "queue:entered:";

    /**
     * 대기열 진입
     * Sorted Set: score = timestamp
     */
    @Override
    @Transactional
    public QueueStatusResponse enterQueue(String eventType, Long eventId, String userId) {
        // ...
        String queueKey = getQueueKey(eventType, eventId);
        double score = System.currentTimeMillis();

        // Sorted Set에 추가 (score = 진입 시간)
        redisTemplate.opsForZSet().add(queueKey, entry.getEntryToken(), score);

        log.info("User {} entered queue for {} {}", userId, eventType, eventId);
        return getQueueStatusInternal(queue, entry);
    }

    /**
     * 대기 순번 조회
     */
    private QueueStatusResponse getQueueStatusInternal(WaitingQueue queue, QueueEntry entry) {
        String queueKey = getQueueKey(queue.getEventType(), queue.getEventId());

        // ZRANK로 순번 조회
        Long position = redisTemplate.opsForZSet().rank(queueKey, entry.getEntryToken());
        Long totalWaiting = redisTemplate.opsForZSet().zCard(queueKey);

        // 예상 대기 시간 계산
        long estimatedWaitSeconds =
            ((position + 1) / queue.getEntryBatchSize()) * queue.getEntryIntervalSeconds();

        return QueueStatusResponse.waiting(
            entry.getEntryToken(),
            position + 1,
            estimatedWaitSeconds,
            totalWaiting
        );
    }

    /**
     * 대기열 처리 (상위 N명 입장)
     */
    @Override
    @Transactional
    public void processEntries(String eventType, Long eventId) {
        String queueKey = getQueueKey(eventType, eventId);
        String enteredKey = getEnteredKey(eventType, eventId);

        // 현재 입장 인원 확인
        Long enteredCount = redisTemplate.opsForSet().size(enteredKey);
        int availableSlots = queue.getMaxCapacity() - enteredCount.intValue();

        if (availableSlots <= 0) return;

        int toProcess = Math.min(availableSlots, queue.getEntryBatchSize());

        // Sorted Set에서 상위 N명 추출 (ZPOPMIN)
        Set<ZSetOperations.TypedTuple<String>> topEntries =
            redisTemplate.opsForZSet().popMin(queueKey, toProcess);

        for (ZSetOperations.TypedTuple<String> tuple : topEntries) {
            String entryToken = tuple.getValue();

            // 입장 처리
            entry.enter();

            // 입장 목록에 추가 (Set)
            redisTemplate.opsForSet().add(enteredKey, entryToken);

            log.info("User {} entered from queue for {} {}",
                entry.getUserId(), eventType, eventId);
        }
    }
}
```

### 분산 락 (Redisson AOP)

```java
// DistributedLock.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();
    long waitTime() default 5;
    long leaseTime() default 30;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}

// DistributedLockAspect.java
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "lock:";
    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {

        String key = LOCK_PREFIX + parseKey(distributedLock.key(), /* ... */);
        RLock lock = redissonClient.getLock(key);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("Failed to acquire lock: key={}", key);
                throw new CustomBusinessException(ShoppingErrorCode.CONCURRENT_STOCK_MODIFICATION);
            }

            log.debug("Lock acquired: key={}", key);
            return joinPoint.proceed();

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: key={}", key);
            }
        }
    }

    // SpEL로 동적 키 파싱
    private String parseKey(String keyExpression, Method method, Object[] args) {
        // ...
    }
}

// 사용 예시
@Service
public class InventoryServiceImpl {

    @DistributedLock(key = "'inventory:' + #productId")
    public void decreaseStock(Long productId, int quantity) {
        // 재고 감소 로직 (동시성 보장)
    }
}
```

---

## 4. Notification Service

### 파일 구조

```
services/notification-service/
├── src/main/java/.../
│   ├── config/
│   │   ├── RedisConfig.java
│   │   └── NotificationRedisSubscriber.java
│   └── service/
│       └── NotificationPushService.java
```

### Redis Pub/Sub 연동

```java
// NotificationRedisSubscriber.java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    /**
     * Redis Pub/Sub 메시지 수신 -> WebSocket 전달
     */
    public void onMessage(String message, String pattern) {
        try {
            // 채널에서 userId 추출 (notification:{userId})
            String channel = pattern;
            if (channel.startsWith("notification:")) {
                String userId = channel.substring("notification:".length());

                NotificationResponse notification =
                    redisObjectMapper.readValue(message, NotificationResponse.class);

                // WebSocket으로 사용자에게 푸시
                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    notification
                );

                log.debug("Pushed notification to user {} via WebSocket", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification message", e);
        }
    }
}
```

### 실시간 알림 아키텍처

```
다중 서버 환경에서 실시간 알림:

+----------+     +----------+     +----------+
| Client A |     | Client B |     | Client C |
|  (WS)    |     |  (WS)    |     |  (WS)    |
+----------+     +----------+     +----------+
     |                |                |
     v                v                v
+----------+     +----------+     +----------+
| Server 1 |     | Server 2 |     | Server 3 |
| (Notif)  |     | (Notif)  |     | (Notif)  |
+----------+     +----------+     +----------+
     |                |                |
     +--------+-------+--------+-------+
              |                |
              v                v
         +--------+       +--------+
         | Redis  |<----->| Redis  |
         |  Pub   |       |  Sub   |
         +--------+       +--------+

흐름:
1. Shopping Service에서 주문 완료
2. Notification Service로 알림 요청
3. Redis PUBLISH notification:{userId}
4. 모든 Notification Server가 메시지 수신
5. 해당 사용자가 연결된 서버가 WebSocket 전송
```

---

## 5. 공통 설정

### application.yml

```yaml
# Auth Service
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms

# Shopping Service (Docker 환경)
spring:
  profiles: docker
  data:
    redis:
      host: redis
      port: 6379

# Kubernetes 환경
spring:
  profiles: k8s
  data:
    redis:
      host: redis-master.redis.svc.cluster.local
      port: 6379
```

### Redis 키 네이밍 컨벤션

```
Portal Universe Redis Key Convention:

Auth Service:
  - blacklist:{accessToken}     # Token Blacklist
  - refresh:{userId}            # Refresh Token

Shopping Service:
  - coupon:stock:{couponId}     # 쿠폰 재고 (String)
  - coupon:issued:{couponId}    # 발급된 사용자 (Set)
  - timedeal:stock:{dealId}     # 타임딜 재고
  - timedeal:purchased:{dealId} # 구매한 사용자
  - queue:waiting:{type}:{id}   # 대기열 (Sorted Set)
  - queue:entered:{type}:{id}   # 입장 목록 (Set)
  - lock:{resource}:{id}        # 분산 락

Notification Service:
  - notification:{userId}       # Pub/Sub 채널
```

---

## 6. 아키텍처 다이어그램

### 전체 Redis 사용 흐름

```
+-----------------------------------------------------------------------+
|                         Portal Universe                                |
+-----------------------------------------------------------------------+
|                                                                       |
|   +-------------------+     +-------------------+                     |
|   |   Frontend        |     |   API Gateway     |                     |
|   | (Vue/React)       |<--->|                   |                     |
|   +-------------------+     +-------------------+                     |
|           |                         |                                 |
|           | WebSocket               | REST                            |
|           v                         v                                 |
|   +-------------------+     +-------------------+                     |
|   | Notification      |     |   Auth Service    |                     |
|   | Service           |     +-------------------+                     |
|   +-------------------+             |                                 |
|           |                         |                                 |
|           | Pub/Sub                 | Token Blacklist                 |
|           |                         | Refresh Token                   |
|           v                         v                                 |
|   +-------------------------------------------------------+          |
|   |                      Redis Server                      |          |
|   |                                                        |          |
|   |  +-------------+  +-------------+  +-------------+    |          |
|   |  | String      |  | Set         |  | Sorted Set  |    |          |
|   |  | (Tokens)    |  | (Issued)    |  | (Queue)     |    |          |
|   |  +-------------+  +-------------+  +-------------+    |          |
|   |                                                        |          |
|   |  +-------------+  +-------------+  +-------------+    |          |
|   |  | Pub/Sub     |  | Locks       |  | Lua Scripts |    |          |
|   |  | (Notif)     |  | (Redisson)  |  | (Atomic)    |    |          |
|   |  +-------------+  +-------------+  +-------------+    |          |
|   +-------------------------------------------------------+          |
|           ^                                                           |
|           |                                                           |
|   +-------------------+     +-------------------+                     |
|   | Shopping Service  |     | Blog Service      |                     |
|   +-------------------+     +-------------------+                     |
|   | - Coupon          |     | (Redis 사용 예정) |                     |
|   | - TimeDeal        |     |                   |                     |
|   | - Queue           |     |                   |                     |
|   | - Inventory Lock  |     |                   |                     |
|   +-------------------+     +-------------------+                     |
|                                                                       |
+-----------------------------------------------------------------------+
```

### 쿠폰 발급 시퀀스

```
Client          Shopping Service      Redis              MySQL
   |                   |                |                   |
   |  POST /coupons    |                |                   |
   |  /issue           |                |                   |
   |------------------>|                |                   |
   |                   |                |                   |
   |                   | SISMEMBER      |                   |
   |                   | (중복 체크)    |                   |
   |                   |--------------->|                   |
   |                   |<---------------|                   |
   |                   |                |                   |
   |                   | Lua Script     |                   |
   |                   | (원자적 발급)  |                   |
   |                   |--------------->|                   |
   |                   |  - SISMEMBER   |                   |
   |                   |  - GET stock   |                   |
   |                   |  - DECR stock  |                   |
   |                   |  - SADD issued |                   |
   |                   |<---------------|                   |
   |                   |                |                   |
   |                   | 발급 결과      |                   |
   |                   |  1: 성공       |                   |
   |                   |  0: 재고 없음  |                   |
   |                   | -1: 중복 발급  |                   |
   |                   |                |                   |
   |                   |                | INSERT            |
   |                   |                | (비동기)         |
   |                   |---------------------------------->|
   |<------------------|                |                   |
   |  Response         |                |                   |
```

---

## 7. 학습 포인트

### 핵심 패턴 정리

| 패턴 | 구현 위치 | 학습 포인트 |
|------|-----------|-------------|
| Token Blacklist | Auth Service | TTL을 활용한 자동 만료 |
| Lua Script | Shopping (Coupon) | 원자적 연산, Race Condition 방지 |
| Sorted Set Queue | Shopping (Queue) | 시간 기반 정렬, 순번 관리 |
| Distributed Lock | Shopping (Inventory) | Redisson, AOP 조합 |
| Pub/Sub | Notification | 실시간 메시징, WebSocket 연동 |

### 코드 분석 체크리스트

```
[ ] RedisConfig.java
    - Redisson vs Lettuce 설정 차이
    - Connection Pool 설정
    - Serializer 설정

[ ] Lua Script 파일들
    - 원자적 연산 보장 방법
    - 에러 핸들링 (롤백)

[ ] 분산 락 구현
    - @DistributedLock 어노테이션
    - SpEL 기반 동적 키
    - AOP Aspect 패턴

[ ] Pub/Sub 구현
    - RedisMessageListenerContainer
    - WebSocket 통합
    - 다중 서버 지원
```

### 확장 가능한 패턴

```
현재 구현되지 않았지만 적용 가능한 패턴:

1. 캐싱 레이어
   - 상품 정보 캐싱
   - 검색 결과 캐싱
   - API 응답 캐싱

2. Rate Limiting
   - API 호출 제한
   - 로그인 시도 제한

3. Session Management
   - Spring Session Redis
   - 분산 세션

4. 실시간 기능 확장
   - 실시간 재고 현황
   - 실시간 주문 대시보드
```

### 관련 문서 학습 순서

```
추천 학습 순서:

1. redis-introduction.md        # Redis 기본 개념
2. redis-data-structures.md     # 자료구조 이해
3. redis-spring-integration.md  # Spring 통합 방법
4. redis-distributed-lock.md    # 분산 락 상세
5. redis-pub-sub.md             # Pub/Sub 패턴
6. redis-caching-patterns.md    # 캐싱 전략
7. redis-portal-universe.md     # 프로젝트 적용 사례 (현재 문서)
8. redis-troubleshooting.md     # 문제 해결
```

---

## 관련 문서

- [Redis Introduction](./redis-introduction.md)
- [Redis Data Structures](./redis-data-structures.md)
- [Redis Distributed Lock](./redis-distributed-lock.md)
- [Redis Pub/Sub](./redis-pub-sub.md)
- [Redis Troubleshooting](./redis-troubleshooting.md)
