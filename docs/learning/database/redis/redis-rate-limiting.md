# Redis Rate Limiting

Redis를 사용한 API Rate Limiting 구현 방법과 다양한 알고리즘을 학습합니다.

## 목차

1. [Rate Limiting 개요](#1-rate-limiting-개요)
2. [Fixed Window Counter](#2-fixed-window-counter)
3. [Sliding Window Log](#3-sliding-window-log)
4. [Sliding Window Counter](#4-sliding-window-counter)
5. [Token Bucket](#5-token-bucket)
6. [Leaky Bucket](#6-leaky-bucket)
7. [Lua Script 구현](#7-lua-script-구현)
8. [Spring 통합](#8-spring-통합)

---

## 1. Rate Limiting 개요

### Rate Limiting이 필요한 이유

```
보호 대상:
+--------------------------------------------------+
|                                                  |
|   [API 남용 방지]     [시스템 보호]    [공정성]    |
|                                                  |
|   - 스팸 방지         - 서버 과부하    - 리소스    |
|   - DoS 공격 방지     - DB 보호         공평 분배  |
|   - 크롤링 제한       - 비용 관리      - QoS 보장  |
|                                                  |
+--------------------------------------------------+

일반적인 Rate Limit 설정:
+-------------------+-------------------+
| API Type          | Rate Limit        |
+-------------------+-------------------+
| 인증 (Login)      | 5 req/min         |
| API 일반          | 100 req/min       |
| 검색              | 30 req/min        |
| 파일 업로드       | 10 req/hour       |
| 관리자 API        | 1000 req/min      |
+-------------------+-------------------+
```

### 알고리즘 비교

```
+-------------------+----------+----------+----------+----------+
| 알고리즘          | 정확도   | 메모리   | 구현     | 버스트   |
+-------------------+----------+----------+----------+----------+
| Fixed Window      | 낮음     | O(1)     | 쉬움     | 허용     |
| Sliding Log       | 높음     | O(N)     | 중간     | 불허     |
| Sliding Counter   | 중간     | O(1)     | 중간     | 제한적   |
| Token Bucket      | 높음     | O(1)     | 중간     | 제어가능 |
| Leaky Bucket      | 높음     | O(N)     | 어려움   | 불허     |
+-------------------+----------+----------+----------+----------+

버스트(Burst): 짧은 시간에 많은 요청 허용 여부
```

---

## 2. Fixed Window Counter

### 개념

```
시간을 고정된 윈도우로 나누고, 각 윈도우에서 요청 수를 카운트합니다.

Timeline:
|<-- Window 1 -->|<-- Window 2 -->|<-- Window 3 -->|
0:00            1:00            2:00            3:00

Window 1: count=50 (limit=100) -> OK
Window 2: count=100 (limit=100) -> LIMIT REACHED
Window 3: count=30 (limit=100) -> OK

문제점 - Boundary 문제:
|<-- Window 1 -->|<-- Window 2 -->|
        50 req  |  50 req
              ^^
              실제로는 1분 내에 100 req 발생
```

### 구현

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FixedWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Fixed Window Rate Limiting
     *
     * @param key 사용자/IP 식별자
     * @param limit 윈도우당 최대 요청 수
     * @param windowSeconds 윈도우 크기 (초)
     * @return true: 허용, false: 제한
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        // 현재 윈도우 계산
        long currentWindow = System.currentTimeMillis() / 1000 / windowSeconds;
        String redisKey = "rate:" + key + ":" + currentWindow;

        // INCR + EXPIRE 원자적 실행
        Long count = redisTemplate.execute(new SessionCallback<Long>() {
            @Override
            public Long execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().increment(redisKey);
                operations.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
                List<Object> results = operations.exec();
                return (Long) results.get(0);
            }
        });

        boolean allowed = count != null && count <= limit;

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, count: {}, limit: {}",
                key, count, limit);
        }

        return allowed;
    }

    /**
     * 남은 요청 수 조회
     */
    public RateLimitInfo getRateLimitInfo(String key, int limit, int windowSeconds) {
        long currentWindow = System.currentTimeMillis() / 1000 / windowSeconds;
        String redisKey = "rate:" + key + ":" + currentWindow;

        String countStr = redisTemplate.opsForValue().get(redisKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

        long windowStart = currentWindow * windowSeconds * 1000;
        long windowEnd = windowStart + (windowSeconds * 1000);
        long resetTime = windowEnd - System.currentTimeMillis();

        return RateLimitInfo.builder()
            .limit(limit)
            .remaining(Math.max(0, limit - currentCount))
            .resetInMs(resetTime)
            .build();
    }
}

@Data
@Builder
public class RateLimitInfo {
    private int limit;
    private int remaining;
    private long resetInMs;
}
```

---

## 3. Sliding Window Log

### 개념

```
각 요청의 타임스탬프를 저장하고, 현재 시간 기준 윈도우 내 요청 수를 계산합니다.

Sorted Set 구조:
Key: rate:user123
+------------+-----------------+
| Timestamp  | Member          |
+------------+-----------------+
| 1609459200 | req_1           |
| 1609459201 | req_2           |
| 1609459205 | req_3           |
| ...        | ...             |
+------------+-----------------+

윈도우: [현재시간 - 60초, 현재시간]
요청 수: ZCOUNT 결과
```

### 구현

```java
@Service
@RequiredArgsConstructor
public class SlidingWindowLogRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Sliding Window Log Rate Limiting
     *
     * 정확하지만 메모리 사용량이 요청 수에 비례
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        String redisKey = "rate:log:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // Lua Script로 원자적 실행
        String script = """
            -- 오래된 요청 제거
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])

            -- 현재 요청 수 확인
            local count = redis.call('ZCARD', KEYS[1])

            if count < tonumber(ARGV[2]) then
                -- 새 요청 추가
                redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4])
                redis.call('EXPIRE', KEYS[1], ARGV[5])
                return 1
            else
                return 0
            end
            """;

        String requestId = UUID.randomUUID().toString();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(
            redisScript,
            List.of(redisKey),
            String.valueOf(windowStart),
            String.valueOf(limit),
            String.valueOf(now),
            requestId,
            String.valueOf(windowSeconds)
        );

        return result != null && result == 1;
    }

    /**
     * 현재 상태 조회
     */
    public RateLimitInfo getRateLimitInfo(String key, int limit, int windowSeconds) {
        String redisKey = "rate:log:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // 오래된 항목 제거 후 카운트
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(redisKey);

        int currentCount = count != null ? count.intValue() : 0;

        // 가장 오래된 요청의 만료 시간
        Set<String> oldest = redisTemplate.opsForZSet().range(redisKey, 0, 0);
        long resetTime = windowSeconds * 1000L;

        if (oldest != null && !oldest.isEmpty()) {
            Double score = redisTemplate.opsForZSet().score(redisKey, oldest.iterator().next());
            if (score != null) {
                resetTime = (long) (score + windowSeconds * 1000L - now);
            }
        }

        return RateLimitInfo.builder()
            .limit(limit)
            .remaining(Math.max(0, limit - currentCount))
            .resetInMs(Math.max(0, resetTime))
            .build();
    }
}
```

---

## 4. Sliding Window Counter

### 개념

```
Fixed Window의 메모리 효율성 + Sliding Window의 정확성을 결합합니다.

현재 윈도우와 이전 윈도우의 가중 평균을 사용합니다.

Timeline:
|<-- Previous Window -->|<-- Current Window -->|
        count_prev             count_curr
                         ^
                    현재 시간 (70% 진행)

가중 요청 수 = count_curr + count_prev * (1 - 현재 윈도우 진행률)
            = 30 + 50 * 0.3
            = 45
```

### 구현

```java
@Service
@RequiredArgsConstructor
public class SlidingWindowCounterRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Sliding Window Counter Rate Limiting
     *
     * 메모리 효율적이면서 Fixed Window보다 정확
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long currentWindow = now / 1000 / windowSeconds;
        long previousWindow = currentWindow - 1;

        String currentKey = "rate:swc:" + key + ":" + currentWindow;
        String previousKey = "rate:swc:" + key + ":" + previousWindow;

        // 현재 윈도우 진행률
        double windowProgress = (now % (windowSeconds * 1000L)) / (double) (windowSeconds * 1000L);

        // 이전 윈도우와 현재 윈도우 카운트 조회
        List<String> keys = Arrays.asList(previousKey, currentKey);
        List<String> counts = redisTemplate.opsForValue().multiGet(keys);

        int previousCount = counts.get(0) != null ? Integer.parseInt(counts.get(0)) : 0;
        int currentCount = counts.get(1) != null ? Integer.parseInt(counts.get(1)) : 0;

        // 가중 평균 계산
        double weightedCount = currentCount + previousCount * (1 - windowProgress);

        if (weightedCount >= limit) {
            return false;
        }

        // 현재 윈도우 카운트 증가
        redisTemplate.opsForValue().increment(currentKey);
        redisTemplate.expire(currentKey, windowSeconds * 2, TimeUnit.SECONDS);

        return true;
    }

    /**
     * Lua Script 버전 (원자성 보장)
     */
    public boolean isAllowedAtomic(String key, int limit, int windowSeconds) {
        String script = """
            local current_key = KEYS[1]
            local previous_key = KEYS[2]
            local limit = tonumber(ARGV[1])
            local window_progress = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])

            local previous_count = tonumber(redis.call('GET', previous_key) or 0)
            local current_count = tonumber(redis.call('GET', current_key) or 0)

            local weighted_count = current_count + previous_count * (1 - window_progress)

            if weighted_count >= limit then
                return 0
            end

            redis.call('INCR', current_key)
            redis.call('EXPIRE', current_key, ttl)

            return 1
            """;

        long now = System.currentTimeMillis();
        long currentWindow = now / 1000 / windowSeconds;
        long previousWindow = currentWindow - 1;

        String currentKey = "rate:swc:" + key + ":" + currentWindow;
        String previousKey = "rate:swc:" + key + ":" + previousWindow;

        double windowProgress = (now % (windowSeconds * 1000L)) / (double) (windowSeconds * 1000L);

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(
            redisScript,
            Arrays.asList(currentKey, previousKey),
            String.valueOf(limit),
            String.valueOf(windowProgress),
            String.valueOf(windowSeconds * 2)
        );

        return result != null && result == 1;
    }
}
```

---

## 5. Token Bucket

### 개념

```
버킷에 토큰이 일정 속도로 채워지고, 요청마다 토큰을 소비합니다.
버스트 트래픽을 허용하면서도 평균 속도를 제한합니다.

Token Bucket 상태:
+------------------+
|  Bucket          |
|  Capacity: 10    |
|  +--+--+--+--+   |
|  |T |T |T |T |   | <- 현재 토큰: 4
|  +--+--+--+--+   |
|                  |
|  Refill: 1/sec   | <- 초당 1토큰 추가
+------------------+

요청 처리:
- 토큰 있음 -> 토큰 소비 + 요청 허용
- 토큰 없음 -> 요청 거부 (또는 대기)

버스트 허용:
- 버킷이 가득 차면 (10 토큰) 한 번에 10개 요청 가능
- 평균적으로는 1 req/sec
```

### 구현

```java
@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Token Bucket Rate Limiting
     *
     * @param key 사용자/IP 식별자
     * @param capacity 버킷 최대 용량
     * @param refillRate 초당 토큰 추가 수
     * @param tokensRequired 요청당 필요한 토큰 수
     */
    public boolean isAllowed(String key, int capacity, double refillRate, int tokensRequired) {
        String script = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local tokens_required = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])

            -- 현재 상태 조회
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1]) or capacity
            local last_refill = tonumber(bucket[2]) or now

            -- 토큰 리필 계산
            local elapsed = (now - last_refill) / 1000
            local refill = elapsed * refill_rate
            tokens = math.min(capacity, tokens + refill)

            -- 토큰 소비
            if tokens >= tokens_required then
                tokens = tokens - tokens_required
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                redis.call('EXPIRE', key, 86400)  -- 24시간 TTL
                return 1
            else
                -- 토큰 부족 - 상태만 업데이트
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                redis.call('EXPIRE', key, 86400)
                return 0
            end
            """;

        String redisKey = "rate:bucket:" + key;
        long now = System.currentTimeMillis();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(
            redisScript,
            List.of(redisKey),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(tokensRequired),
            String.valueOf(now)
        );

        return result != null && result == 1;
    }

    /**
     * 현재 토큰 수 조회
     */
    public TokenBucketInfo getBucketInfo(String key, int capacity, double refillRate) {
        String redisKey = "rate:bucket:" + key;
        Map<Object, Object> bucket = redisTemplate.opsForHash().entries(redisKey);

        if (bucket.isEmpty()) {
            return TokenBucketInfo.builder()
                .tokens(capacity)
                .capacity(capacity)
                .refillRate(refillRate)
                .build();
        }

        double tokens = Double.parseDouble((String) bucket.get("tokens"));
        long lastRefill = Long.parseLong((String) bucket.get("last_refill"));

        // 현재 토큰 계산
        double elapsed = (System.currentTimeMillis() - lastRefill) / 1000.0;
        double currentTokens = Math.min(capacity, tokens + elapsed * refillRate);

        return TokenBucketInfo.builder()
            .tokens(currentTokens)
            .capacity(capacity)
            .refillRate(refillRate)
            .build();
    }
}

@Data
@Builder
public class TokenBucketInfo {
    private double tokens;
    private int capacity;
    private double refillRate;
}
```

---

## 6. Leaky Bucket

### 개념

```
요청이 버킷에 들어가고, 일정 속도로 처리됩니다.
버킷이 가득 차면 새 요청은 거부됩니다.

Leaky Bucket:
                  +-- 요청 입구 --+
                        |
                  +-----v-----+
                  |     10    | <- Bucket (queue)
                  |     9     |
                  |     8     |
                  |     7     |
                  |     ...   |
                  +-----+-----+
                        |
                        v
                   (일정 속도로 처리)
                   1 req/sec

Token Bucket vs Leaky Bucket:
- Token Bucket: 버스트 허용 (평균 제한)
- Leaky Bucket: 버스트 불가 (일정 속도 보장)
```

### 구현

```java
@Service
@RequiredArgsConstructor
public class LeakyBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Leaky Bucket Rate Limiting
     *
     * @param key 사용자/IP 식별자
     * @param capacity 버킷 최대 용량 (대기열 크기)
     * @param leakRate 초당 처리 속도
     */
    public boolean isAllowed(String key, int capacity, double leakRate) {
        String script = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local leak_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            -- 현재 상태 조회
            local bucket = redis.call('HMGET', key, 'water', 'last_leak')
            local water = tonumber(bucket[1]) or 0
            local last_leak = tonumber(bucket[2]) or now

            -- 누수(처리) 계산
            local elapsed = (now - last_leak) / 1000
            local leaked = elapsed * leak_rate
            water = math.max(0, water - leaked)

            -- 새 요청 추가 시도
            if water < capacity then
                water = water + 1
                redis.call('HMSET', key, 'water', water, 'last_leak', now)
                redis.call('EXPIRE', key, 86400)
                return 1
            else
                -- 버킷 가득 참
                redis.call('HMSET', key, 'water', water, 'last_leak', now)
                redis.call('EXPIRE', key, 86400)
                return 0
            end
            """;

        String redisKey = "rate:leaky:" + key;
        long now = System.currentTimeMillis();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(
            redisScript,
            List.of(redisKey),
            String.valueOf(capacity),
            String.valueOf(leakRate),
            String.valueOf(now)
        );

        return result != null && result == 1;
    }

    /**
     * 실제 처리 대기열 구현 (Leaky Bucket Queue)
     */
    public boolean enqueue(String key, String requestId, int capacity, double leakRate) {
        String queueKey = "rate:leaky:queue:" + key;

        // 큐 크기 확인
        Long size = redisTemplate.opsForList().size(queueKey);
        if (size != null && size >= capacity) {
            return false;  // 큐 가득 참
        }

        // 요청을 큐에 추가
        redisTemplate.opsForList().rightPush(queueKey, requestId);
        redisTemplate.expire(queueKey, 1, TimeUnit.HOURS);

        return true;
    }

    /**
     * 일정 속도로 큐에서 요청 처리
     */
    @Scheduled(fixedRate = 100)  // 100ms마다 실행
    public void processQueue() {
        // 모든 활성 큐에서 처리
        Set<String> queueKeys = redisTemplate.keys("rate:leaky:queue:*");
        if (queueKeys == null) return;

        for (String queueKey : queueKeys) {
            String requestId = redisTemplate.opsForList().leftPop(queueKey);
            if (requestId != null) {
                // 요청 처리 로직
                processRequest(requestId);
            }
        }
    }

    private void processRequest(String requestId) {
        // 실제 요청 처리 또는 이벤트 발행
    }
}
```

---

## 7. Lua Script 구현

### 통합 Rate Limiter Script

```lua
-- rate_limiter.lua
-- 범용 Rate Limiter (여러 알고리즘 지원)

local key = KEYS[1]
local algorithm = ARGV[1]
local limit = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

-- Fixed Window Counter
local function fixed_window()
    local window_key = key .. ':' .. math.floor(now / 1000 / window)
    local count = redis.call('INCR', window_key)

    if count == 1 then
        redis.call('EXPIRE', window_key, window)
    end

    if count <= limit then
        return {1, limit - count, window * 1000}
    else
        local ttl = redis.call('TTL', window_key)
        return {0, 0, ttl * 1000}
    end
end

-- Token Bucket
local function token_bucket()
    local capacity = limit
    local refill_rate = tonumber(ARGV[5]) or (limit / window)

    local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
    local tokens = tonumber(bucket[1]) or capacity
    local last_refill = tonumber(bucket[2]) or now

    -- Refill
    local elapsed = (now - last_refill) / 1000
    tokens = math.min(capacity, tokens + elapsed * refill_rate)

    if tokens >= 1 then
        tokens = tokens - 1
        redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
        redis.call('EXPIRE', key, 86400)
        return {1, math.floor(tokens), 0}
    else
        redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
        redis.call('EXPIRE', key, 86400)
        local wait_time = (1 - tokens) / refill_rate * 1000
        return {0, 0, math.floor(wait_time)}
    end
end

-- Sliding Window Counter
local function sliding_window_counter()
    local current_window = math.floor(now / 1000 / window)
    local previous_window = current_window - 1
    local current_key = key .. ':' .. current_window
    local previous_key = key .. ':' .. previous_window

    local window_progress = (now % (window * 1000)) / (window * 1000)

    local previous_count = tonumber(redis.call('GET', previous_key) or 0)
    local current_count = tonumber(redis.call('GET', current_key) or 0)

    local weighted_count = current_count + previous_count * (1 - window_progress)

    if weighted_count < limit then
        redis.call('INCR', current_key)
        redis.call('EXPIRE', current_key, window * 2)
        return {1, math.floor(limit - weighted_count - 1), window * 1000}
    else
        local ttl = redis.call('TTL', current_key)
        return {0, 0, ttl * 1000}
    end
end

-- 알고리즘 선택
if algorithm == 'fixed' then
    return fixed_window()
elseif algorithm == 'token' then
    return token_bucket()
elseif algorithm == 'sliding' then
    return sliding_window_counter()
else
    return {0, 0, 0}  -- Unknown algorithm
end
```

### Java에서 Lua Script 사용

```java
@Service
@RequiredArgsConstructor
public class UniversalRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("scripts/rate_limiter.lua"))
        );
        rateLimitScript.setResultType(List.class);
    }

    public enum Algorithm {
        FIXED("fixed"),
        TOKEN("token"),
        SLIDING("sliding");

        private final String value;

        Algorithm(String value) {
            this.value = value;
        }
    }

    /**
     * 범용 Rate Limiting
     */
    public RateLimitResult checkLimit(
            String key,
            Algorithm algorithm,
            int limit,
            int windowSeconds) {

        List<Long> result = redisTemplate.execute(
            rateLimitScript,
            List.of("rate:" + key),
            algorithm.value,
            String.valueOf(limit),
            String.valueOf(windowSeconds),
            String.valueOf(System.currentTimeMillis())
        );

        if (result == null || result.size() < 3) {
            return RateLimitResult.error();
        }

        return RateLimitResult.builder()
            .allowed(result.get(0) == 1)
            .remaining(result.get(1).intValue())
            .retryAfterMs(result.get(2))
            .build();
    }
}

@Data
@Builder
public class RateLimitResult {
    private boolean allowed;
    private int remaining;
    private long retryAfterMs;

    public static RateLimitResult error() {
        return RateLimitResult.builder()
            .allowed(false)
            .remaining(0)
            .retryAfterMs(1000)
            .build();
    }
}
```

---

## 8. Spring 통합

### Rate Limit Interceptor

```java
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final UniversalRateLimiter rateLimiter;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null) {
            return true;  // Rate limit 설정 없음
        }

        String key = resolveKey(request, rateLimit);
        RateLimitResult result = rateLimiter.checkLimit(
            key,
            rateLimit.algorithm(),
            rateLimit.limit(),
            rateLimit.window()
        );

        // 응답 헤더 설정
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset",
            String.valueOf(System.currentTimeMillis() + result.getRetryAfterMs()));

        if (!result.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After",
                String.valueOf(result.getRetryAfterMs() / 1000));
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"retryAfter\":" +
                result.getRetryAfterMs() + "}"
            );
            return false;
        }

        return true;
    }

    private String resolveKey(HttpServletRequest request, RateLimit rateLimit) {
        return switch (rateLimit.keyType()) {
            case IP -> getClientIp(request);
            case USER -> getCurrentUserId(request);
            case API -> request.getRequestURI();
            case CUSTOM -> rateLimit.key();
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getCurrentUserId(HttpServletRequest request) {
        // Spring Security에서 사용자 ID 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return getClientIp(request);  // 미인증 시 IP 사용
    }
}
```

### Rate Limit Annotation

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 윈도우당 최대 요청 수
     */
    int limit() default 100;

    /**
     * 윈도우 크기 (초)
     */
    int window() default 60;

    /**
     * Rate Limit 알고리즘
     */
    UniversalRateLimiter.Algorithm algorithm()
        default UniversalRateLimiter.Algorithm.SLIDING;

    /**
     * Rate Limit 키 타입
     */
    KeyType keyType() default KeyType.IP;

    /**
     * 커스텀 키 (keyType = CUSTOM 시 사용)
     */
    String key() default "";

    enum KeyType {
        IP, USER, API, CUSTOM
    }
}
```

### 사용 예시

```java
@RestController
@RequestMapping("/api/v1")
@RateLimit(limit = 100, window = 60)  // 클래스 레벨 기본 설정
public class ApiController {

    /**
     * 기본 Rate Limit (클래스 설정 상속)
     */
    @GetMapping("/products")
    public List<Product> getProducts() {
        return productService.findAll();
    }

    /**
     * 엄격한 Rate Limit (로그인)
     */
    @PostMapping("/auth/login")
    @RateLimit(limit = 5, window = 60, keyType = RateLimit.KeyType.IP)
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 검색 API (사용자 기준)
     */
    @GetMapping("/search")
    @RateLimit(
        limit = 30,
        window = 60,
        algorithm = UniversalRateLimiter.Algorithm.TOKEN,
        keyType = RateLimit.KeyType.USER
    )
    public SearchResult search(@RequestParam String query) {
        return searchService.search(query);
    }

    /**
     * 파일 업로드 (매우 제한적)
     */
    @PostMapping("/files")
    @RateLimit(limit = 10, window = 3600)  // 시간당 10개
    public FileResponse uploadFile(@RequestParam MultipartFile file) {
        return fileService.upload(file);
    }
}
```

### Redisson Rate Limiter

```java
@Service
@RequiredArgsConstructor
public class RedissonRateLimitService {

    private final RedissonClient redissonClient;

    /**
     * Redisson의 RRateLimiter 사용
     */
    public boolean tryAcquire(String key, int permits) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rateLimit:" + key);

        // Rate 설정 (최초 1회)
        rateLimiter.trySetRate(
            RateType.OVERALL,  // 전체 인스턴스 합산
            100,               // 100 요청
            1,                 // 1분당
            RateIntervalUnit.MINUTES
        );

        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 비동기 Rate Limiting
     */
    public CompletableFuture<Boolean> tryAcquireAsync(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rateLimit:" + key);

        return rateLimiter.tryAcquireAsync()
            .toCompletableFuture();
    }

    /**
     * Rate Limiter 설정 업데이트
     */
    public void updateRate(String key, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rateLimit:" + key);
        rateLimiter.setRate(RateType.OVERALL, rate, interval, unit);
    }

    /**
     * 현재 상태 조회
     */
    public RateLimiterConfig getConfig(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rateLimit:" + key);
        return rateLimiter.getConfig();
    }
}
```

---

## 관련 문서

- [Redis Data Structures](./redis-data-structures.md)
- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Distributed Lock](./redis-distributed-lock.md)
- [Redis Portal Universe](./redis-portal-universe.md)
