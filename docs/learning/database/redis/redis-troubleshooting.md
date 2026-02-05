# Redis Troubleshooting

Redis 운영 중 발생할 수 있는 문제들의 진단과 해결 방법을 학습합니다.

## 목차

1. [연결 문제](#1-연결-문제)
2. [Connection Pool 문제](#2-connection-pool-문제)
3. [메모리 문제](#3-메모리-문제)
4. [성능 문제](#4-성능-문제)
5. [분산 락 문제](#5-분산-락-문제)
6. [데이터 일관성 문제](#6-데이터-일관성-문제)
7. [모니터링 및 진단](#7-모니터링-및-진단)
8. [운영 체크리스트](#8-운영-체크리스트)

---

## 1. 연결 문제

### 증상 및 에러 메시지

```
1. 연결 거부
   org.springframework.data.redis.RedisConnectionFailureException:
   Unable to connect to Redis

2. 타임아웃
   io.lettuce.core.RedisCommandTimeoutException:
   Command timed out after 60 second(s)

3. 연결 끊김
   io.lettuce.core.RedisException:
   Connection reset by peer
```

### 진단 순서

```
+-------------------+
| 연결 문제 발생    |
+-------------------+
         |
         v
+-------------------+
| 1. 네트워크 확인  |
|    - ping test    |
|    - port check   |
+-------------------+
         |
         v
+-------------------+
| 2. Redis 서버     |
|    상태 확인      |
|    - redis-cli    |
|    - INFO         |
+-------------------+
         |
         v
+-------------------+
| 3. 인증 확인      |
|    - password     |
|    - ACL          |
+-------------------+
         |
         v
+-------------------+
| 4. 클라이언트     |
|    설정 확인      |
+-------------------+
```

### 해결 방법

```bash
# 1. 네트워크 연결 확인
$ telnet redis-host 6379
$ nc -zv redis-host 6379

# 2. Redis 서버 상태 확인
$ redis-cli -h redis-host -p 6379 ping
PONG

$ redis-cli INFO server
# Server
redis_version:7.0.0
uptime_in_seconds:86400

# 3. 클라이언트 목록 확인
$ redis-cli CLIENT LIST

# 4. 연결 수 확인
$ redis-cli INFO clients
connected_clients:150
blocked_clients:0
```

### Spring Boot 설정 개선

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000ms
      connect-timeout: 3000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5
          max-wait: 3000ms
        shutdown-timeout: 100ms
```

```java
@Configuration
public class RedisConnectionConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration serverConfig =
            new RedisStandaloneConfiguration("localhost", 6379);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofMillis(100))
            .clientOptions(ClientOptions.builder()
                .autoReconnect(true)  // 자동 재연결
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .keepAlive(true)
                    .build())
                .build())
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
}
```

---

## 2. Connection Pool 문제

### 증상

```
1. Pool Exhausted
   org.springframework.data.redis.RedisConnectionFailureException:
   Could not get a resource from the pool

2. Pool Deadlock
   애플리케이션이 응답하지 않음

3. Connection Leak
   연결이 반환되지 않아 점진적으로 풀 소진
```

### 진단

```java
@Component
@Slf4j
public class ConnectionPoolMonitor {

    @Autowired
    private LettuceConnectionFactory connectionFactory;

    @Scheduled(fixedRate = 30000)
    public void monitorPool() {
        // Lettuce는 내부적으로 Netty 사용
        // 직접적인 Pool 메트릭 조회는 어려움

        // Redis INFO로 클라이언트 정보 확인
        try (RedisConnection conn = connectionFactory.getConnection()) {
            Properties info = conn.serverCommands().info("clients");
            log.info("Connected clients: {}",
                info.getProperty("connected_clients"));
        }
    }
}
```

```bash
# Redis 서버에서 클라이언트 연결 확인
$ redis-cli INFO clients
connected_clients:50
client_recent_max_input_buffer:0
client_recent_max_output_buffer:0
blocked_clients:0

# 클라이언트 상세 목록
$ redis-cli CLIENT LIST
id=5 addr=10.0.0.1:55000 fd=8 name= age=300 idle=10 ...
id=6 addr=10.0.0.2:55001 fd=9 name= age=250 idle=0 ...
```

### 해결 방법

```java
// 1. Pool 설정 최적화
@Bean
public LettuceConnectionFactory optimizedConnectionFactory() {
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(50);           // 최대 연결 수
    poolConfig.setMaxIdle(20);            // 최대 유휴 연결
    poolConfig.setMinIdle(5);             // 최소 유휴 연결
    poolConfig.setMaxWait(Duration.ofSeconds(3));  // 최대 대기 시간

    // 유효성 검사
    poolConfig.setTestOnBorrow(true);     // 빌릴 때 검사
    poolConfig.setTestOnReturn(false);    // 반환 시 검사 (비활성화 권장)
    poolConfig.setTestWhileIdle(true);    // 유휴 시 검사

    // 유휴 연결 정리
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
    poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));

    LettucePoolingClientConfiguration clientConfig =
        LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(5))
            .build();

    return new LettuceConnectionFactory(
        new RedisStandaloneConfiguration("localhost", 6379),
        clientConfig
    );
}

// 2. Connection Leak 방지 - try-with-resources 사용
public void safeOperation() {
    try (RedisConnection conn = connectionFactory.getConnection()) {
        conn.stringCommands().get("key".getBytes());
    }  // 자동으로 연결 반환
}

// 3. RedisTemplate 사용 시 자동 관리됨
public void templateOperation() {
    // RedisTemplate이 연결 관리
    redisTemplate.opsForValue().get("key");
}
```

### Pool 사이징 가이드

```
권장 Pool 크기 계산:

Pool Size = (Core Count * 2) + Effective Spindle Count

예시 (4 Core, SSD):
  Pool Size = (4 * 2) + 1 = 9 (최소)
  실제 권장: 20-50 (동시 요청 고려)

+------------------+------------------+------------------+
| 트래픽 수준      | maxTotal         | maxIdle          |
+------------------+------------------+------------------+
| 낮음 (< 100 TPS) | 10-20            | 5-10             |
| 중간 (100-1K)    | 20-50            | 10-20            |
| 높음 (> 1K TPS)  | 50-100           | 20-50            |
+------------------+------------------+------------------+
```

---

## 3. 메모리 문제

### 증상

```
1. OOM (Out of Memory)
   OOM command not allowed when used memory > 'maxmemory'

2. 메모리 급증
   used_memory가 지속적으로 증가

3. Swap 사용
   성능 저하
```

### 진단

```bash
# 메모리 정보 확인
$ redis-cli INFO memory
used_memory:1073741824
used_memory_human:1.00G
used_memory_rss:1200000000
used_memory_peak:1500000000
used_memory_peak_human:1.40G
maxmemory:2147483648
maxmemory_human:2.00G
maxmemory_policy:noeviction
mem_fragmentation_ratio:1.12

# 큰 키 찾기
$ redis-cli --bigkeys
[00.00%] Biggest string found so far 'large_key' with 10485760 bytes
[00.00%] Biggest list found so far 'large_list' with 100000 items

# 메모리 사용량 분석
$ redis-cli MEMORY DOCTOR
$ redis-cli MEMORY STATS
```

### 해결 방법

```bash
# 1. maxmemory 설정
$ redis-cli CONFIG SET maxmemory 2gb

# 2. Eviction Policy 설정
$ redis-cli CONFIG SET maxmemory-policy allkeys-lru

# 3. 큰 키 삭제 (점진적)
$ redis-cli UNLINK large_key  # 비동기 삭제
```

### Eviction Policy 가이드

```
+--------------------+--------------------------------------------+
| Policy             | 설명                                       |
+--------------------+--------------------------------------------+
| noeviction         | 메모리 초과 시 에러 반환                   |
| allkeys-lru        | 모든 키 중 LRU 삭제                        |
| allkeys-lfu        | 모든 키 중 LFU 삭제                        |
| allkeys-random     | 모든 키 중 랜덤 삭제                       |
| volatile-lru       | TTL 있는 키 중 LRU 삭제                    |
| volatile-lfu       | TTL 있는 키 중 LFU 삭제                    |
| volatile-random    | TTL 있는 키 중 랜덤 삭제                   |
| volatile-ttl       | TTL이 가장 짧은 키 삭제                    |
+--------------------+--------------------------------------------+

권장:
- 캐시: allkeys-lru 또는 allkeys-lfu
- 세션: volatile-lru
- 중요 데이터: noeviction (모니터링 필수)
```

### 메모리 최적화

```java
// 1. 키 이름 축약
// Bad: "application:service:user:profile:12345"
// Good: "u:p:12345"

// 2. TTL 설정
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

// 3. 작은 Hash 사용 (ziplist 최적화)
// redis.conf: hash-max-ziplist-entries 512
//             hash-max-ziplist-value 64

// 4. 불필요한 데이터 정리
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
public void cleanupOldData() {
    // 오래된 키 삭제
    long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
    redisTemplate.opsForZSet().removeRangeByScore("events", 0, cutoff);
}
```

---

## 4. 성능 문제

### 증상

```
1. 높은 지연 시간
   응답 시간 > 10ms

2. 명령어 큐 대기
   blocked_clients 증가

3. 느린 쿼리
   Slowlog 증가
```

### 진단

```bash
# 1. 지연 시간 측정
$ redis-cli --latency
min: 0, max: 5, avg: 0.30 (1000 samples)

# 2. Slowlog 확인
$ redis-cli SLOWLOG GET 10
1) 1) (integer) 14
   2) (integer) 1609459200
   3) (integer) 15000  # 15ms
   4) 1) "KEYS"
      2) "*"

# 3. 명령어 통계
$ redis-cli INFO commandstats
cmdstat_get:calls=1000000,usec=500000,usec_per_call=0.50
cmdstat_set:calls=500000,usec=300000,usec_per_call=0.60

# 4. 클라이언트 대기 확인
$ redis-cli INFO clients
blocked_clients:5
```

### 해결 방법

```java
// 1. KEYS 명령어 대신 SCAN 사용
// Bad
Set<String> keys = redisTemplate.keys("user:*");

// Good
Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
    connection -> connection.scan(
        ScanOptions.scanOptions().match("user:*").count(100).build()
    )
);
while (cursor.hasNext()) {
    byte[] key = cursor.next();
    // 처리
}

// 2. Pipeline 사용
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.stringCommands().get(("key:" + i).getBytes());
    }
    return null;
});

// 3. Lua Script로 라운드트립 감소
String script = """
    local value1 = redis.call('GET', KEYS[1])
    local value2 = redis.call('GET', KEYS[2])
    return {value1, value2}
    """;

// 4. 적절한 데이터 구조 선택
// O(N) 연산 주의: LRANGE, SMEMBERS, HGETALL 등

// 5. Connection Pool 튜닝
// maxTotal 증가, 타임아웃 조정
```

### 성능 최적화 체크리스트

```
[ ] KEYS 명령어 사용하지 않음
[ ] 큰 컬렉션 조회 시 SCAN 사용
[ ] Pipeline 또는 Multi/Exec 활용
[ ] 적절한 데이터 구조 선택
[ ] TTL 설정으로 메모리 관리
[ ] Connection Pool 적절히 설정
[ ] Slowlog 모니터링
```

---

## 5. 분산 락 문제

### 증상

```
1. 락 획득 실패
   Failed to acquire lock

2. 데드락
   락이 해제되지 않음

3. 락 유실
   네트워크 파티션 시 락 상태 불일치
```

### 일반적인 문제와 해결

```
문제 1: 락 만료 전 작업 미완료
+---------+      +-------+      +---------+
| Client1 | ---> | Redis | ---> | Client1 |
+---------+      +-------+      +---------+
     |               |               |
  Lock(30s)          |               |
     |               |               |
  Processing...      |               |
     |               |               |
     |          Expire(30s)          |
     |               |               |
     |          Lock released!       |
     |               |               |
     |               |<--- Lock(30s) Client2
     |               |               |
  Still processing   |  Processing   |
     |               |               |
     v               v               v
  Race Condition!!!

해결: Watchdog (자동 연장) 사용
```

```java
// Redisson Watchdog 활용
@Bean
public RedissonClient redissonClientWithWatchdog() {
    Config config = new Config();
    config.useSingleServer()
        .setAddress("redis://localhost:6379");

    // Watchdog 기본 활성화 (lockWatchdogTimeout = 30초)
    // 락을 보유하는 동안 자동으로 TTL 연장

    return Redisson.create(config);
}

// 사용
public void operationWithWatchdog() {
    RLock lock = redissonClient.getLock("myLock");

    // leaseTime을 지정하지 않으면 Watchdog 활성화
    lock.lock();  // Watchdog 활성
    // lock.lock(30, TimeUnit.SECONDS);  // Watchdog 비활성

    try {
        // 작업 수행 (Watchdog가 TTL 자동 연장)
    } finally {
        lock.unlock();
    }
}
```

### 락 해제 실패 처리

```java
@Service
@Slf4j
public class SafeLockService {

    private final RedissonClient redissonClient;

    public void safeOperation(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new LockAcquisitionException(lockKey);
            }

            task.run();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);

        } finally {
            // 안전한 락 해제
            if (acquired) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    } else {
                        log.warn("Lock not held by current thread: {}", lockKey);
                    }
                } catch (Exception e) {
                    log.error("Failed to release lock: {}", lockKey, e);
                }
            }
        }
    }
}
```

### 락 상태 진단

```java
@Component
@Slf4j
public class LockDiagnostics {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 락 상태 확인
     */
    public LockInfo getLockInfo(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);

        return LockInfo.builder()
            .name(lock.getName())
            .isLocked(lock.isLocked())
            .holdCount(lock.getHoldCount())
            .isHeldByCurrentThread(lock.isHeldByCurrentThread())
            .remainTimeToLive(lock.remainTimeToLive())
            .build();
    }

    /**
     * 강제 락 해제 (관리용)
     */
    public void forceUnlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);

        if (lock.isLocked()) {
            lock.forceUnlock();
            log.warn("Force unlocked: {}", lockKey);
        }
    }

    /**
     * 락 관련 키 스캔
     */
    public List<String> scanLockKeys() {
        List<String> lockKeys = new ArrayList<>();

        Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
            conn -> conn.scan(ScanOptions.scanOptions()
                .match("lock:*")
                .count(100)
                .build())
        );

        while (cursor.hasNext()) {
            lockKeys.add(new String(cursor.next()));
        }

        return lockKeys;
    }
}

@Data
@Builder
public class LockInfo {
    private String name;
    private boolean isLocked;
    private int holdCount;
    private boolean isHeldByCurrentThread;
    private long remainTimeToLive;
}
```

---

## 6. 데이터 일관성 문제

### 증상

```
1. 캐시와 DB 불일치
2. 중복 데이터 발생
3. 데이터 손실
```

### Cache-DB 일관성 문제

```
문제: 캐시 무효화 실패

Timeline:
Client A             Redis              DB             Client B
    |                  |                 |                 |
    |  Read(X)         |                 |                 |
    |-Cache Miss------>|                 |                 |
    |                  |  Read(X)        |                 |
    |------------------|---------------->|                 |
    |<-----------------|<----------------|                 |
    |  X = 100         |                 |                 |
    |  Set(X, 100)     |                 |                 |
    |----------------->|                 |                 |
    |                  |                 |                 |
    |                  |                 |  Update(X, 200) |
    |                  |                 |<----------------|
    |                  |  Delete(X)      |                 |
    |                  |<---FAIL---------|                 |
    |                  |                 |                 |
    |  Read(X)         |                 |                 |
    |----------------->|                 |                 |
    |<-----------------| X = 100 (Stale!)|                 |
```

### 해결 방법

```java
// 1. Write-Through with Retry
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsistentCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;

    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateProductWithCache(Long productId, ProductUpdateRequest request) {
        // 1. DB 업데이트
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        product.update(request);
        productRepository.save(product);

        // 2. 캐시 무효화 (실패 시 재시도)
        String cacheKey = "product:" + productId;
        redisTemplate.delete(cacheKey);

        log.info("Product {} updated and cache invalidated", productId);
    }

    // 2. 이벤트 기반 캐시 무효화
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdate(ProductUpdatedEvent event) {
        String cacheKey = "product:" + event.getProductId();
        redisTemplate.delete(cacheKey);
    }
}

// 3. Cache-Aside with TTL
public ProductResponse getProductWithTtl(Long productId) {
    String cacheKey = "product:" + productId;
    ProductResponse cached = (ProductResponse) redisTemplate.opsForValue().get(cacheKey);

    if (cached != null) {
        return cached;
    }

    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));

    ProductResponse response = ProductResponse.from(product);

    // TTL 설정으로 최종 일관성 보장
    redisTemplate.opsForValue().set(cacheKey, response, 5, TimeUnit.MINUTES);

    return response;
}
```

### 중복 방지

```java
// 멱등성 보장
@Service
public class IdempotentService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean processOnce(String operationId, Runnable operation) {
        String key = "processed:" + operationId;

        // SetNX로 중복 체크
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", 24, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(isNew)) {
            try {
                operation.run();
                return true;
            } catch (Exception e) {
                // 실패 시 키 삭제 (재시도 허용)
                redisTemplate.delete(key);
                throw e;
            }
        }

        return false;  // 이미 처리됨
    }
}
```

---

## 7. 모니터링 및 진단

### 필수 메트릭

```java
@Component
@RequiredArgsConstructor
public class RedisMetricsCollector {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)
    public void collectMetrics() {
        // Redis INFO 명령어로 메트릭 수집
        Properties info = redisTemplate.execute(
            (RedisCallback<Properties>) conn -> conn.serverCommands().info()
        );

        if (info == null) return;

        // 메모리
        meterRegistry.gauge("redis.memory.used",
            parseBytes(info.getProperty("used_memory")));
        meterRegistry.gauge("redis.memory.rss",
            parseBytes(info.getProperty("used_memory_rss")));
        meterRegistry.gauge("redis.memory.fragmentation",
            Double.parseDouble(info.getProperty("mem_fragmentation_ratio", "1")));

        // 클라이언트
        meterRegistry.gauge("redis.clients.connected",
            Integer.parseInt(info.getProperty("connected_clients", "0")));
        meterRegistry.gauge("redis.clients.blocked",
            Integer.parseInt(info.getProperty("blocked_clients", "0")));

        // 키
        meterRegistry.gauge("redis.keys.total",
            parseTotalKeys(info));

        // 히트율
        long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
        long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
        double hitRate = hits + misses > 0 ?
            (double) hits / (hits + misses) : 0;
        meterRegistry.gauge("redis.cache.hit_rate", hitRate);

        // CPU
        meterRegistry.gauge("redis.cpu.sys",
            Double.parseDouble(info.getProperty("used_cpu_sys", "0")));
        meterRegistry.gauge("redis.cpu.user",
            Double.parseDouble(info.getProperty("used_cpu_user", "0")));
    }

    private long parseBytes(String value) {
        return value != null ? Long.parseLong(value) : 0;
    }

    private long parseTotalKeys(Properties info) {
        // db0:keys=1234,expires=100 형식 파싱
        String db0 = info.getProperty("db0");
        if (db0 != null && db0.contains("keys=")) {
            String keysPart = db0.split(",")[0];
            return Long.parseLong(keysPart.split("=")[1]);
        }
        return 0;
    }
}
```

### 알림 설정

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAlertService {

    private final StringRedisTemplate redisTemplate;
    private final AlertNotificationService alertService;

    // 임계값
    private static final double MEMORY_THRESHOLD = 0.85;  // 85%
    private static final double HIT_RATE_THRESHOLD = 0.90;  // 90%
    private static final int CLIENT_THRESHOLD = 200;

    @Scheduled(fixedRate = 60000)
    public void checkHealth() {
        Properties info = redisTemplate.execute(
            (RedisCallback<Properties>) conn -> conn.serverCommands().info()
        );

        if (info == null) {
            alertService.sendAlert(AlertLevel.CRITICAL,
                "Redis connection failed - cannot get INFO");
            return;
        }

        // 메모리 체크
        long usedMemory = Long.parseLong(info.getProperty("used_memory", "0"));
        long maxMemory = Long.parseLong(info.getProperty("maxmemory", "0"));
        if (maxMemory > 0 && (double) usedMemory / maxMemory > MEMORY_THRESHOLD) {
            alertService.sendAlert(AlertLevel.WARNING,
                String.format("Redis memory usage high: %.1f%%",
                    (double) usedMemory / maxMemory * 100));
        }

        // 히트율 체크
        long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
        long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
        if (hits + misses > 1000) {  // 충분한 샘플
            double hitRate = (double) hits / (hits + misses);
            if (hitRate < HIT_RATE_THRESHOLD) {
                alertService.sendAlert(AlertLevel.WARNING,
                    String.format("Redis hit rate low: %.1f%%", hitRate * 100));
            }
        }

        // 클라이언트 수 체크
        int clients = Integer.parseInt(info.getProperty("connected_clients", "0"));
        if (clients > CLIENT_THRESHOLD) {
            alertService.sendAlert(AlertLevel.WARNING,
                String.format("Redis connected clients high: %d", clients));
        }
    }
}
```

### Slowlog 모니터링

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SlowlogMonitor {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 300000)  // 5분마다
    public void checkSlowlog() {
        List<Object> slowlogs = redisTemplate.execute(
            (RedisCallback<List<Object>>) conn -> conn.execute("SLOWLOG", "GET", "10")
        );

        if (slowlogs == null || slowlogs.isEmpty()) {
            return;
        }

        for (Object entry : slowlogs) {
            if (entry instanceof List) {
                List<?> log = (List<?>) entry;
                // [id, timestamp, duration(us), command, ...]
                if (log.size() >= 4) {
                    long duration = (Long) log.get(2);
                    List<?> command = (List<?>) log.get(3);

                    if (duration > 10000) {  // 10ms 이상
                        log.warn("Slow Redis command: {}us - {}",
                            duration, command);
                    }
                }
            }
        }
    }
}
```

---

## 8. 운영 체크리스트

### 배포 전 체크리스트

```
[ ] Redis 버전 확인 (최소 6.x 권장)
[ ] maxmemory 설정
[ ] maxmemory-policy 설정
[ ] 인증 설정 (requirepass)
[ ] TLS/SSL 설정 (프로덕션)
[ ] 백업 설정 (RDB/AOF)
[ ] Sentinel 또는 Cluster 구성 (HA)
[ ] 모니터링 설정
[ ] 알림 설정
```

### 일일 운영 체크리스트

```
[ ] 메모리 사용량 확인
[ ] 연결 수 확인
[ ] Slowlog 확인
[ ] 복제 지연 확인 (Replica)
[ ] 백업 상태 확인
```

### 장애 대응 런북

```
1. Redis 연결 불가
   [ ] 네트워크 연결 확인 (ping, telnet)
   [ ] Redis 프로세스 상태 확인
   [ ] 로그 확인 (/var/log/redis/)
   [ ] 재시작 필요 시: sudo systemctl restart redis

2. 메모리 부족
   [ ] 메모리 사용량 확인 (INFO memory)
   [ ] 큰 키 확인 (--bigkeys)
   [ ] 불필요한 키 삭제
   [ ] maxmemory-policy 확인/변경
   [ ] 필요 시 maxmemory 증가

3. 성능 저하
   [ ] Slowlog 확인
   [ ] 클라이언트 수 확인
   [ ] CPU 사용량 확인
   [ ] 네트워크 지연 확인
   [ ] 문제 명령어 식별 및 최적화

4. 데이터 불일치
   [ ] 복제 상태 확인 (INFO replication)
   [ ] 복제 지연 확인
   [ ] 필요 시 강제 동기화
```

---

## 관련 문서

- [Redis Data Structures](./redis-data-structures.md)
- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Distributed Lock](./redis-distributed-lock.md)
- [Redis Portal Universe](./redis-portal-universe.md)
