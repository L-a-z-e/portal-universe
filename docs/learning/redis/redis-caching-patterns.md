# Redis Caching Patterns

애플리케이션 성능을 최적화하기 위한 다양한 캐싱 전략과 패턴을 학습합니다.

## 목차

1. [캐싱 기본 개념](#1-캐싱-기본-개념)
2. [Cache-Aside Pattern](#2-cache-aside-pattern)
3. [Write-Through Pattern](#3-write-through-pattern)
4. [Write-Behind Pattern](#4-write-behind-pattern)
5. [TTL 전략](#5-ttl-전략)
6. [캐시 무효화 전략](#6-캐시-무효화-전략)
7. [캐시 워밍](#7-캐시-워밍)
8. [고급 패턴](#8-고급-패턴)

---

## 1. 캐싱 기본 개념

### 캐싱이 필요한 이유

```
+------------------+        +------------------+        +------------------+
|                  |        |                  |        |                  |
|   Application    | -----> |     Redis        | -----> |    Database      |
|                  | <----- |     (Cache)      | <----- |                  |
|                  |        |                  |        |                  |
+------------------+        +------------------+        +------------------+
     Response: 1ms              Response: 1-5ms            Response: 50-200ms

캐시 적중 시: Application <-> Redis (1-5ms)
캐시 미스 시: Application <-> Redis <-> Database (50-200ms)
```

### 캐시 Hit/Miss

```
Cache Hit (캐시 적중):
+------+    +-------+
| App  | -> | Redis | -> Data Found! -> Return
+------+    +-------+

Cache Miss (캐시 미스):
+------+    +-------+         +------+
| App  | -> | Redis | -> X -> |  DB  | -> Return + Cache
+------+    +-------+         +------+
```

### 캐싱 결정 기준

| 캐싱에 적합 | 캐싱에 부적합 |
|------------|--------------|
| 읽기 빈도 높음 | 쓰기 빈도 높음 |
| 데이터 변경 적음 | 실시간 정확성 필요 |
| 계산 비용 높음 | 개인화 데이터 |
| 응답 시간 중요 | 민감한 정보 |

---

## 2. Cache-Aside Pattern

### 개념

가장 일반적인 캐싱 패턴으로, 애플리케이션이 캐시와 데이터베이스를 직접 관리합니다.

```
[읽기 흐름 - Cache Hit]
+------+        +-------+
| App  | -----> | Redis |
|      | <----- |       |
+------+        +-------+
   1. GET
   2. Return Data

[읽기 흐름 - Cache Miss]
+------+        +-------+        +------+
| App  | -----> | Redis |        |      |
|      |   X    |       |        |  DB  |
|      | -----------------------> |      |
|      | <----------------------- |      |
|      | -----> | Redis |        |      |
+------+        +-------+        +------+
   1. GET (Miss)
   2. Query DB
   3. SET Cache
   4. Return Data

[쓰기 흐름]
+------+        +------+        +-------+
| App  | -----> |  DB  |        |       |
|      | <----- |      |        | Redis |
|      | ----------------DELETE->|       |
+------+        +------+        +-------+
   1. Update DB
   2. Invalidate Cache
```

### 구현 예제

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * Cache-Aside 읽기 패턴
     */
    public ProductResponse getProduct(Long productId) {
        String cacheKey = PRODUCT_CACHE_PREFIX + productId;

        // 1. 캐시에서 조회
        ProductResponse cached = (ProductResponse) redisTemplate
            .opsForValue().get(cacheKey);

        if (cached != null) {
            log.debug("Cache HIT: {}", cacheKey);
            return cached;
        }

        log.debug("Cache MISS: {}", cacheKey);

        // 2. 캐시 미스 시 DB에서 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductResponse response = ProductResponse.from(product);

        // 3. 캐시에 저장
        redisTemplate.opsForValue()
            .set(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        return response;
    }

    /**
     * Cache-Aside 쓰기 패턴 (Invalidation)
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        // 1. DB 업데이트
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        product.update(request);
        productRepository.save(product);

        // 2. 캐시 무효화
        String cacheKey = PRODUCT_CACHE_PREFIX + productId;
        redisTemplate.delete(cacheKey);

        log.info("Cache invalidated: {}", cacheKey);

        return ProductResponse.from(product);
    }

    /**
     * 벌크 조회 최적화
     */
    public List<ProductResponse> getProducts(List<Long> productIds) {
        // 1. 캐시 키 생성
        List<String> cacheKeys = productIds.stream()
            .map(id -> PRODUCT_CACHE_PREFIX + id)
            .collect(Collectors.toList());

        // 2. 멀티 GET
        List<Object> cachedResults = redisTemplate.opsForValue()
            .multiGet(cacheKeys);

        Map<Long, ProductResponse> resultMap = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // 3. 캐시 히트/미스 분류
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Object cached = cachedResults.get(i);

            if (cached != null) {
                resultMap.put(productId, (ProductResponse) cached);
            } else {
                missedIds.add(productId);
            }
        }

        // 4. 캐시 미스된 항목 DB 조회
        if (!missedIds.isEmpty()) {
            List<Product> products = productRepository.findAllById(missedIds);

            Map<String, ProductResponse> toCache = new HashMap<>();
            for (Product product : products) {
                ProductResponse response = ProductResponse.from(product);
                resultMap.put(product.getId(), response);
                toCache.put(PRODUCT_CACHE_PREFIX + product.getId(), response);
            }

            // 5. 멀티 SET
            if (!toCache.isEmpty()) {
                redisTemplate.opsForValue().multiSet(toCache);
                // TTL 설정 (파이프라인 사용)
                toCache.keySet().forEach(key ->
                    redisTemplate.expire(key, CACHE_TTL_MINUTES, TimeUnit.MINUTES));
            }
        }

        // 6. 순서 유지하며 반환
        return productIds.stream()
            .map(resultMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 구현 간단 | 첫 요청은 항상 느림 |
| 캐시 장애 시 DB로 대체 가능 | 캐시와 DB 불일치 가능 |
| 필요한 데이터만 캐싱 | 코드 복잡도 증가 |
| 캐시 크기 효율적 관리 | N+1 쿼리 주의 필요 |

---

## 3. Write-Through Pattern

### 개념

모든 쓰기가 캐시를 통과하여 동시에 DB에 저장됩니다.

```
[쓰기 흐름]
+------+        +-------+        +------+
| App  | -----> | Redis | -----> |  DB  |
|      | <----- |       | <----- |      |
+------+        +-------+        +------+
   1. Write to Cache
   2. Cache writes to DB (sync)
   3. Return Success

[읽기 흐름]
+------+        +-------+
| App  | -----> | Redis |  <- Always Cache Hit
|      | <----- |       |     (after first write)
+------+        +-------+
```

### 구현 예제

```java
@Service
@RequiredArgsConstructor
@Transactional
public class WriteThroughCacheService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_PREFIX = "product:";

    /**
     * Write-Through 패턴
     * 캐시와 DB에 동시 쓰기 (동기식)
     */
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 1. DB 저장
        Product product = request.toEntity();
        product = productRepository.save(product);

        // 2. 캐시 저장 (동기)
        ProductResponse response = ProductResponse.from(product);
        String cacheKey = CACHE_PREFIX + product.getId();

        redisTemplate.opsForValue().set(cacheKey, response);

        log.info("Write-Through: DB and Cache updated for {}", cacheKey);

        return response;
    }

    /**
     * Write-Through 업데이트
     */
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        // 1. DB 업데이트
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        product.update(request);
        product = productRepository.save(product);

        // 2. 캐시 업데이트 (동기)
        ProductResponse response = ProductResponse.from(product);
        String cacheKey = CACHE_PREFIX + productId;

        redisTemplate.opsForValue().set(cacheKey, response);

        return response;
    }

    /**
     * 읽기는 항상 캐시 우선
     */
    public ProductResponse getProduct(Long productId) {
        String cacheKey = CACHE_PREFIX + productId;

        // 캐시에서 조회
        ProductResponse cached = (ProductResponse) redisTemplate
            .opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // 캐시 미스 (최초 로드 또는 만료)
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductResponse response = ProductResponse.from(product);
        redisTemplate.opsForValue().set(cacheKey, response);

        return response;
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 캐시-DB 일관성 보장 | 쓰기 지연 시간 증가 |
| 읽기 항상 빠름 | 불필요한 데이터도 캐싱 |
| 데이터 유실 없음 | 캐시 장애 시 쓰기 실패 |

---

## 4. Write-Behind Pattern

### 개념

비동기로 DB에 쓰기를 수행하여 애플리케이션 응답 속도를 개선합니다.

```
[쓰기 흐름]
+------+        +-------+          +------+
| App  | -----> | Redis |          |  DB  |
|      | <----- |       |          |      |
+------+        +-------+          +------+
   1. Write to Cache              (비동기)
   2. Return Success                 |
                   +-------+         |
                   | Queue | ------->|
                   +-------+    3. Batch Write

[타임라인]
t=0    t=1    t=2    t=3    t=100ms
 |      |      |      |         |
App -> Cache  App gets Response
              |                  |
              +--- Background ---|
                   DB Write
```

### 구현 예제

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WriteBehindCacheService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_PREFIX = "product:";
    private static final String WRITE_QUEUE = "write:queue:products";

    /**
     * Write-Behind 패턴 (비동기 DB 쓰기)
     */
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 1. 임시 ID 생성 (실제로는 snowflake 등 사용)
        String tempId = UUID.randomUUID().toString();

        // 2. 캐시에 즉시 저장
        ProductCacheEntry entry = ProductCacheEntry.builder()
            .id(tempId)
            .name(request.getName())
            .price(request.getPrice())
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

        String cacheKey = CACHE_PREFIX + tempId;
        redisTemplate.opsForValue().set(cacheKey, entry);

        // 3. 쓰기 큐에 추가 (비동기 처리용)
        WriteOperation operation = WriteOperation.builder()
            .operationType("CREATE")
            .cacheKey(cacheKey)
            .data(entry)
            .timestamp(System.currentTimeMillis())
            .build();

        redisTemplate.opsForList().rightPush(WRITE_QUEUE, operation);

        log.info("Write-Behind: Queued write operation for {}", cacheKey);

        return ProductResponse.from(entry);
    }

    /**
     * 백그라운드 Writer (스케줄러 또는 별도 스레드)
     */
    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void processWriteQueue() {
        List<WriteOperation> batch = new ArrayList<>();

        // 배치로 처리할 항목 수집
        for (int i = 0; i < 100; i++) {
            WriteOperation op = (WriteOperation) redisTemplate
                .opsForList().leftPop(WRITE_QUEUE);

            if (op == null) break;
            batch.add(op);
        }

        if (batch.isEmpty()) return;

        // 배치 DB 저장
        try {
            List<Product> products = batch.stream()
                .filter(op -> "CREATE".equals(op.getOperationType()))
                .map(op -> ((ProductCacheEntry) op.getData()).toEntity())
                .collect(Collectors.toList());

            List<Product> saved = productRepository.saveAll(products);

            // 캐시 업데이트 (실제 ID로)
            for (int i = 0; i < batch.size(); i++) {
                WriteOperation op = batch.get(i);
                Product product = saved.get(i);

                // 임시 캐시 삭제하고 실제 ID로 재저장
                redisTemplate.delete(op.getCacheKey());

                String newKey = CACHE_PREFIX + product.getId();
                redisTemplate.opsForValue().set(newKey, ProductResponse.from(product));
            }

            log.info("Write-Behind: Processed {} write operations", batch.size());

        } catch (Exception e) {
            log.error("Write-Behind: Failed to process batch", e);
            // 실패 시 다시 큐에 추가 (재시도 로직)
            batch.forEach(op ->
                redisTemplate.opsForList().rightPush(WRITE_QUEUE, op));
        }
    }
}

/**
 * 고급 Write-Behind with Coalescing (병합)
 */
@Service
public class CoalescingWriteBehindService {

    private final Map<String, Object> pendingWrites = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        // 5초마다 플러시
        executor.scheduleAtFixedRate(this::flush, 5, 5, TimeUnit.SECONDS);
    }

    public void write(String key, Object value) {
        // 같은 키에 대한 쓰기는 마지막 값으로 병합
        pendingWrites.put(key, value);
    }

    private void flush() {
        if (pendingWrites.isEmpty()) return;

        Map<String, Object> toFlush = new HashMap<>(pendingWrites);
        pendingWrites.clear();

        // 배치로 DB 쓰기
        // ...
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 쓰기 응답 매우 빠름 | 데이터 유실 가능성 |
| DB 부하 분산 | 구현 복잡도 높음 |
| 배치 최적화 가능 | 일관성 보장 어려움 |
| 쓰기 병합 가능 | 장애 복구 복잡 |

---

## 5. TTL 전략

### TTL 설정 원칙

```
                    TTL 선택 가이드
                         |
          +--------------+---------------+
          |              |               |
    [변경 빈도]     [일관성 요구]    [메모리 제약]
          |              |               |
     +----+----+    +----+----+     +----+----+
     |         |    |         |     |         |
  [높음]     [낮음] [높음]   [낮음] [많음]   [적음]
     |         |    |         |     |         |
 짧은TTL  긴TTL  짧은TTL 긴TTL  짧은TTL  긴TTL
 (분)    (시간)  (분)   (일)   (분)   (일)
```

### 상황별 TTL 가이드

| 데이터 유형 | 권장 TTL | 이유 |
|------------|---------|------|
| 세션 데이터 | 30분 - 24시간 | 보안 + 사용자 경험 |
| 상품 목록 | 5-30분 | 가격/재고 변경 가능 |
| 상품 상세 | 1-6시간 | 상대적으로 정적 |
| 사용자 프로필 | 1-24시간 | 자주 변경되지 않음 |
| 검색 결과 | 1-5분 | 동적 데이터 |
| 설정값 | 1-24시간 | 거의 변경 안 됨 |
| Rate Limit | 1분 - 1시간 | 제한 윈도우에 따라 |

### TTL 구현 패턴

```java
@Service
public class TtlStrategyService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1. 고정 TTL
    public void setWithFixedTtl(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
    }

    // 2. 동적 TTL (데이터 특성에 따라)
    public void setWithDynamicTtl(String key, Object value, DataType type) {
        Duration ttl = switch (type) {
            case HOT -> Duration.ofMinutes(5);    // 자주 변경
            case WARM -> Duration.ofHours(1);     // 보통
            case COLD -> Duration.ofHours(24);    // 거의 변경 안 됨
        };
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    // 3. 슬라이딩 TTL (접근 시마다 갱신)
    public Object getWithSlidingTtl(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 접근 시 TTL 갱신
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        }
        return value;
    }

    // 4. 지터(Jitter) 적용 (캐시 스탬피드 방지)
    public void setWithJitter(String key, Object value, long baseTtlMinutes) {
        // 기본 TTL에 0-20% 랜덤 추가
        long jitter = (long) (baseTtlMinutes * 0.2 * Math.random());
        long ttl = baseTtlMinutes + jitter;

        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
    }

    // 5. 계층적 TTL
    public void setHierarchicalTtl(String category, String itemId, Object value) {
        // 카테고리 캐시: 긴 TTL
        redisTemplate.opsForValue()
            .set("category:" + category, getCategoryData(), 1, TimeUnit.HOURS);

        // 아이템 캐시: 짧은 TTL
        redisTemplate.opsForValue()
            .set("item:" + itemId, value, 10, TimeUnit.MINUTES);
    }

    // 6. 조건부 TTL
    public void setConditionalTtl(String key, ProductResponse product) {
        Duration ttl;

        if (product.getStock() < 10) {
            // 재고 적으면 짧은 TTL (빈번한 업데이트 예상)
            ttl = Duration.ofMinutes(1);
        } else if (product.isOnSale()) {
            // 세일 상품은 중간 TTL
            ttl = Duration.ofMinutes(5);
        } else {
            // 일반 상품은 긴 TTL
            ttl = Duration.ofHours(1);
        }

        redisTemplate.opsForValue().set(key, product, ttl);
    }
}
```

### TTL 모니터링

```java
// TTL 확인
Long ttl = redisTemplate.getExpire("key");
Long ttlInSeconds = redisTemplate.getExpire("key", TimeUnit.SECONDS);

// TTL이 설정되지 않은 키 찾기 (관리용)
// SCAN으로 키 순회 후 TTL 확인
```

---

## 6. 캐시 무효화 전략

### 무효화 패턴

```
1. Time-Based (TTL)
   +------+              +------+
   | Data | ---(TTL)---> | 삭제  |
   +------+              +------+

2. Event-Based (명시적 무효화)
   +--------+    +-------+    +-------+
   | Update | -> | Event | -> | DELETE|
   +--------+    +-------+    +-------+

3. Version-Based (키에 버전 포함)
   "product:123:v1" -> "product:123:v2"
   (이전 버전은 자연 만료)
```

### 구현 예제

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // 1. 단일 키 무효화
    public void invalidate(String key) {
        redisTemplate.delete(key);
        log.info("Cache invalidated: {}", key);
    }

    // 2. 패턴 기반 무효화
    public void invalidateByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cache invalidated {} keys matching: {}", keys.size(), pattern);
        }
    }

    // 3. 연관 데이터 무효화
    @Transactional
    public void updateProductWithCacheInvalidation(Long productId, ProductUpdateRequest request) {
        // DB 업데이트
        productRepository.save(/* ... */);

        // 연관 캐시 모두 무효화
        invalidate("product:" + productId);
        invalidateByPattern("category:*:products");  // 카테고리별 상품 목록
        invalidateByPattern("search:*");  // 검색 결과
    }

    // 4. 이벤트 기반 무효화
    @EventListener
    public void handleProductUpdate(ProductUpdatedEvent event) {
        invalidate("product:" + event.getProductId());
    }

    // 5. 태그 기반 무효화 (고급)
    public void setWithTags(String key, Object value, String... tags) {
        // 데이터 저장
        redisTemplate.opsForValue().set(key, value);

        // 태그별로 키 등록
        for (String tag : tags) {
            redisTemplate.opsForSet().add("tag:" + tag, key);
        }
    }

    public void invalidateByTag(String tag) {
        Set<Object> keys = redisTemplate.opsForSet().members("tag:" + tag);
        if (keys != null && !keys.isEmpty()) {
            // 데이터 삭제
            redisTemplate.delete(keys.stream()
                .map(Object::toString)
                .collect(Collectors.toList()));

            // 태그 셋 삭제
            redisTemplate.delete("tag:" + tag);

            log.info("Invalidated {} keys with tag: {}", keys.size(), tag);
        }
    }
}

// 사용 예시
cacheService.setWithTags("product:123",
    productData,
    "category:electronics", "brand:apple", "sale:summer");

// "electronics" 카테고리 모든 상품 캐시 무효화
cacheService.invalidateByTag("category:electronics");
```

### Cache Stampede 방지

```java
@Service
public class StampedePreventionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * Lock을 사용한 Stampede 방지
     */
    public Object getWithLock(String key, Supplier<Object> loader) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        String lockKey = "lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 5초 대기)
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // Double-check
                    cached = redisTemplate.opsForValue().get(key);
                    if (cached != null) {
                        return cached;
                    }

                    // DB에서 로드
                    Object value = loader.get();
                    redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
                    return value;

                } finally {
                    lock.unlock();
                }
            } else {
                // 락 획득 실패 시 짧은 대기 후 재시도
                Thread.sleep(100);
                return redisTemplate.opsForValue().get(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("Lock acquisition interrupted", e);
        }
    }

    /**
     * Probabilistic Early Expiration (XFetch 알고리즘)
     */
    public Object getWithEarlyExpiration(String key, Supplier<Object> loader) {
        CacheEntry entry = (CacheEntry) redisTemplate.opsForValue().get(key);

        if (entry == null) {
            return loadAndCache(key, loader);
        }

        // 남은 TTL
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return loadAndCache(key, loader);
        }

        // 초기 TTL의 10% 미만이면 확률적으로 갱신
        if (ttl < entry.getInitialTtl() * 0.1) {
            double probability = 1 - (ttl.doubleValue() / (entry.getInitialTtl() * 0.1));
            if (Math.random() < probability) {
                // 백그라운드에서 갱신
                CompletableFuture.runAsync(() -> loadAndCache(key, loader));
            }
        }

        return entry.getValue();
    }

    private Object loadAndCache(String key, Supplier<Object> loader) {
        Object value = loader.get();
        long ttl = 1800; // 30분

        CacheEntry entry = new CacheEntry(value, ttl);
        redisTemplate.opsForValue().set(key, entry, ttl, TimeUnit.SECONDS);

        return value;
    }
}
```

---

## 7. 캐시 워밍

### 워밍 전략

```
Cold Start 문제:
+------------------+
| Application      |  서버 재시작
| Start            |
+------------------+
         |
         v
+------------------+
| Empty Cache      |  캐시 비어있음
+------------------+
         |
         v
+------------------+
| Slow Response    |  모든 요청이 DB 직접 조회
| High DB Load     |
+------------------+

해결: Cache Warming
+------------------+
| Application      |
| Start            |
+------------------+
         |
    +----+----+
    |         |
    v         v
+-------+  +--------+
| Warm  |  | Accept |
| Cache |  | Traffic|
+-------+  +--------+
    |         |
    +----+----+
         |
         v
+------------------+
| Fast Response    |
| Normal DB Load   |
+------------------+
```

### 구현 예제

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 애플리케이션 시작 시 캐시 워밍
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("Starting cache warm-up...");

        warmPopularProducts();
        warmCategories();
        warmConfigurations();

        log.info("Cache warm-up completed");
    }

    private void warmPopularProducts() {
        // 인기 상품 TOP 100 캐싱
        List<Product> popularProducts = productRepository
            .findTop100ByOrderByViewCountDesc();

        Map<String, ProductResponse> cacheMap = popularProducts.stream()
            .collect(Collectors.toMap(
                p -> "product:" + p.getId(),
                ProductResponse::from
            ));

        redisTemplate.opsForValue().multiSet(cacheMap);

        // TTL 설정
        cacheMap.keySet().forEach(key ->
            redisTemplate.expire(key, 1, TimeUnit.HOURS));

        log.info("Warmed {} popular products", cacheMap.size());
    }

    private void warmCategories() {
        // 카테고리 목록 캐싱
        List<Category> categories = categoryRepository.findAll();
        redisTemplate.opsForValue().set("categories:all", categories, 24, TimeUnit.HOURS);
    }

    private void warmConfigurations() {
        // 설정값 캐싱
        Map<String, String> configs = configRepository.findAllAsMap();
        redisTemplate.opsForHash().putAll("config:app", configs);
        redisTemplate.expire("config:app", 24, TimeUnit.HOURS);
    }

    /**
     * 스케줄 기반 캐시 워밍 (피크 시간 전)
     */
    @Scheduled(cron = "0 0 8 * * *") // 매일 오전 8시
    public void scheduledWarmUp() {
        log.info("Scheduled cache warm-up starting...");

        // 오늘의 딜 상품
        warmTodayDeals();

        // 추천 상품
        warmRecommendations();
    }

    /**
     * 점진적 워밍 (서버 부하 분산)
     */
    public void gradualWarmUp() {
        List<Long> productIds = productRepository.findAllIds();

        // 100개씩 배치로 워밍
        Lists.partition(productIds, 100).forEach(batch -> {
            warmBatch(batch);
            try {
                Thread.sleep(1000); // 1초 간격
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void warmBatch(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        RedisCallback<Object> callback = connection -> {
            products.forEach(product -> {
                byte[] key = ("product:" + product.getId()).getBytes();
                byte[] value = serialize(ProductResponse.from(product));
                connection.setEx(key, 3600, value);
            });
            return null;
        };

        redisTemplate.executePipelined(callback);
    }
}
```

---

## 8. 고급 패턴

### Read-Through 캐시

```java
/**
 * Spring Cache 추상화를 활용한 Read-Through
 */
@Service
public class ReadThroughService {

    @Cacheable(value = "products", key = "#productId")
    public ProductResponse getProduct(Long productId) {
        // 캐시 미스 시에만 실행
        return productRepository.findById(productId)
            .map(ProductResponse::from)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @CacheEvict(value = "products", key = "#productId")
    public void evictProduct(Long productId) {
        // 캐시만 삭제
    }

    @CachePut(value = "products", key = "#result.id")
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        // 항상 실행되고 결과를 캐시에 저장
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        product.update(request);
        return ProductResponse.from(productRepository.save(product));
    }
}
```

### 다중 레벨 캐시

```java
/**
 * L1 (Local) + L2 (Redis) 캐시
 */
@Service
@RequiredArgsConstructor
public class MultiLevelCacheService {

    private final Cache<String, Object> localCache;  // Caffeine
    private final RedisTemplate<String, Object> redisTemplate;

    public Object get(String key) {
        // L1: 로컬 캐시 확인
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }

        // L2: Redis 확인
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // L1에 저장
            localCache.put(key, value);
            return value;
        }

        return null;
    }

    public void put(String key, Object value) {
        // L2에 저장
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

        // L1에 저장
        localCache.put(key, value);
    }

    public void evict(String key) {
        // 둘 다 삭제
        redisTemplate.delete(key);
        localCache.invalidate(key);
    }
}
```

### 캐시 패턴 선택 가이드

```
+------------------+------------------+------------------+------------------+
| 패턴             | 읽기 성능        | 쓰기 성능        | 일관성           |
+------------------+------------------+------------------+------------------+
| Cache-Aside      | 좋음             | 좋음             | 약함             |
| Write-Through    | 매우 좋음        | 보통             | 강함             |
| Write-Behind     | 매우 좋음        | 매우 좋음        | 약함             |
| Read-Through     | 좋음             | 좋음             | 중간             |
+------------------+------------------+------------------+------------------+

선택 기준:
- 읽기 많음 + 일관성 중요 X -> Cache-Aside
- 읽기 많음 + 일관성 중요 O -> Write-Through
- 쓰기 많음 + 성능 중요 -> Write-Behind
- Spring 환경 + 간단한 구현 -> Read-Through (@Cacheable)
```

---

## 관련 문서

- [Redis Data Structures](./redis-data-structures.md)
- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Troubleshooting](./redis-troubleshooting.md)
