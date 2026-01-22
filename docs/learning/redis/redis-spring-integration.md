# Redis Spring Integration

Spring Boot에서 Redis를 효과적으로 사용하기 위한 설정과 패턴을 학습합니다.

## 목차

1. [의존성 설정](#1-의존성-설정)
2. [Connection 설정](#2-connection-설정)
3. [RedisTemplate](#3-redistemplate)
4. [StringRedisTemplate](#4-stringredistemplate)
5. [Repository 패턴](#5-repository-패턴)
6. [Spring Cache Abstraction](#6-spring-cache-abstraction)
7. [Redisson 통합](#7-redisson-통합)
8. [테스트 전략](#8-테스트-전략)

---

## 1. 의존성 설정

### Gradle 설정

```groovy
// build.gradle
dependencies {
    // Spring Data Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Redisson (분산 락, 고급 기능)
    implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'

    // Connection Pool
    implementation 'org.apache.commons:commons-pool2:2.12.0'

    // 직렬화 (선택)
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // 테스트
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
}
```

### Maven 설정

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.24.3</version>
    </dependency>

    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
</dependencies>
```

---

## 2. Connection 설정

### application.yml 설정

```yaml
spring:
  data:
    redis:
      # 단일 서버
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0

      # 타임아웃
      timeout: 3000ms
      connect-timeout: 2000ms

      # Lettuce 연결 풀
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5
          max-wait: 3000ms
          time-between-eviction-runs: 30s

      # Sentinel (고가용성)
      # sentinel:
      #   master: mymaster
      #   nodes:
      #     - sentinel1:26379
      #     - sentinel2:26379
      #     - sentinel3:26379

      # Cluster
      # cluster:
      #   nodes:
      #     - node1:6379
      #     - node2:6379
      #     - node3:6379
      #   max-redirects: 3
```

### Lettuce Connection 설정

```java
@Configuration
public class LettuceConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig =
            new RedisStandaloneConfiguration(host, port);

        if (!password.isEmpty()) {
            serverConfig.setPassword(RedisPassword.of(password));
        }

        // 클라이언트 옵션
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(3))
            .shutdownTimeout(Duration.ofMillis(100))
            .clientOptions(ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build())
            .clientResources(DefaultClientResources.builder()
                .ioThreadPoolSize(4)
                .computationThreadPoolSize(4)
                .build())
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * Connection Pool 설정 (별도 구성 시)
     */
    @Bean
    public LettuceConnectionFactory pooledConnectionFactory() {
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(3))
            .build();

        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(host, port),
            clientConfig
        );
    }
}
```

### Sentinel 설정

```java
@Configuration
@Profile("production")
public class RedisSentinelConfig {

    @Bean
    public RedisConnectionFactory sentinelConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("sentinel1", 26379)
            .sentinel("sentinel2", 26379)
            .sentinel("sentinel3", 26379);

        sentinelConfig.setPassword(RedisPassword.of("password"));

        return new LettuceConnectionFactory(sentinelConfig);
    }
}
```

### Cluster 설정

```java
@Configuration
@Profile("cluster")
public class RedisClusterConfig {

    @Bean
    public RedisConnectionFactory clusterConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
            Arrays.asList(
                "node1:6379",
                "node2:6379",
                "node3:6379"
            )
        );
        clusterConfig.setMaxRedirects(3);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)  // 읽기는 Replica 우선
            .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

---

## 3. RedisTemplate

### 기본 RedisTemplate 설정

```java
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Serializer (JSON)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 타입별 RedisTemplate (타입 안전성)
     */
    @Bean
    public RedisTemplate<String, ProductResponse> productRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, ProductResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<ProductResponse> valueSerializer =
            new Jackson2JsonRedisSerializer<>(ProductResponse.class);

        template.setValueSerializer(valueSerializer);

        return template;
    }
}
```

### RedisTemplate 사용 예제

```java
@Service
@RequiredArgsConstructor
public class RedisTemplateExamples {

    private final RedisTemplate<String, Object> redisTemplate;

    // ===== String Operations =====
    public void stringOperations() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // SET
        ops.set("key", "value");
        ops.set("key", "value", 1, TimeUnit.HOURS);  // with TTL
        ops.setIfAbsent("key", "value");  // SETNX
        ops.setIfPresent("key", "value"); // SETXX

        // GET
        Object value = ops.get("key");
        Object oldValue = ops.getAndSet("key", "newValue");

        // MGET / MSET
        Map<String, Object> map = Map.of("k1", "v1", "k2", "v2");
        ops.multiSet(map);
        List<Object> values = ops.multiGet(Arrays.asList("k1", "k2"));

        // INCR / DECR
        ops.increment("counter");
        ops.increment("counter", 10);
        ops.decrement("counter");
    }

    // ===== Hash Operations =====
    public void hashOperations() {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();

        // HSET / HGET
        ops.put("user:1", "name", "John");
        Object name = ops.get("user:1", "name");

        // HMSET / HMGET
        Map<Object, Object> userData = Map.of(
            "name", "John",
            "email", "john@example.com",
            "age", 30
        );
        ops.putAll("user:1", userData);

        List<Object> fields = ops.multiGet("user:1",
            Arrays.asList("name", "email"));

        // HGETALL
        Map<Object, Object> all = ops.entries("user:1");

        // HINCRBY
        ops.increment("user:1", "loginCount", 1);

        // HDEL
        ops.delete("user:1", "tempField");
    }

    // ===== List Operations =====
    public void listOperations() {
        ListOperations<String, Object> ops = redisTemplate.opsForList();

        // LPUSH / RPUSH
        ops.leftPush("queue", "item1");
        ops.rightPush("queue", "item2");
        ops.leftPushAll("queue", "a", "b", "c");

        // LPOP / RPOP
        Object left = ops.leftPop("queue");
        Object right = ops.rightPop("queue");

        // BLPOP / BRPOP (Blocking)
        Object blocked = ops.leftPop("queue", 30, TimeUnit.SECONDS);

        // LRANGE
        List<Object> range = ops.range("queue", 0, -1);  // 전체
        List<Object> first10 = ops.range("queue", 0, 9);

        // LLEN
        Long size = ops.size("queue");

        // LTRIM
        ops.trim("queue", 0, 99);  // 최근 100개만 유지
    }

    // ===== Set Operations =====
    public void setOperations() {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        // SADD
        ops.add("tags", "java", "spring", "redis");

        // SISMEMBER
        Boolean exists = ops.isMember("tags", "java");

        // SMEMBERS
        Set<Object> members = ops.members("tags");

        // SCARD
        Long count = ops.size("tags");

        // SREM
        ops.remove("tags", "redis");

        // 집합 연산
        Set<Object> intersection = ops.intersect("set1", "set2");
        Set<Object> union = ops.union("set1", "set2");
        Set<Object> difference = ops.difference("set1", "set2");

        // SRANDMEMBER
        Object random = ops.randomMember("tags");
        List<Object> randoms = ops.randomMembers("tags", 3);
    }

    // ===== Sorted Set Operations =====
    public void sortedSetOperations() {
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();

        // ZADD
        ops.add("leaderboard", "player1", 100);
        ops.add("leaderboard", "player2", 200);

        // ZSCORE
        Double score = ops.score("leaderboard", "player1");

        // ZRANK / ZREVRANK
        Long rank = ops.rank("leaderboard", "player1");
        Long revRank = ops.reverseRank("leaderboard", "player1");

        // ZINCRBY
        ops.incrementScore("leaderboard", "player1", 50);

        // ZRANGE / ZREVRANGE
        Set<Object> top10 = ops.reverseRange("leaderboard", 0, 9);

        // With Scores
        Set<ZSetOperations.TypedTuple<Object>> withScores =
            ops.reverseRangeWithScores("leaderboard", 0, 9);

        withScores.forEach(tuple -> {
            Object member = tuple.getValue();
            Double memberScore = tuple.getScore();
        });

        // ZRANGEBYSCORE
        Set<Object> byScore = ops.rangeByScore("leaderboard", 100, 500);

        // ZCARD
        Long size = ops.zCard("leaderboard");

        // ZPOPMIN / ZPOPMAX
        ZSetOperations.TypedTuple<Object> min = ops.popMin("leaderboard");
        Set<ZSetOperations.TypedTuple<Object>> minN = ops.popMin("leaderboard", 5);
    }

    // ===== HyperLogLog Operations =====
    public void hyperLogLogOperations() {
        HyperLogLogOperations<String, Object> ops = redisTemplate.opsForHyperLogLog();

        // PFADD
        ops.add("visitors:today", "user1", "user2", "user3");

        // PFCOUNT
        Long uniqueCount = ops.size("visitors:today");

        // PFMERGE
        ops.union("visitors:week",
            "visitors:mon", "visitors:tue", "visitors:wed");
    }

    // ===== Stream Operations =====
    public void streamOperations() {
        StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();

        // XADD
        Map<Object, Object> message = Map.of(
            "action", "order_created",
            "orderId", "123"
        );
        RecordId id = ops.add("events", message);

        // XREAD
        List<MapRecord<String, Object, Object>> records = ops.read(
            StreamOffset.fromStart("events")
        );

        // Consumer Group
        ops.createGroup("events", "my-group");

        List<MapRecord<String, Object, Object>> groupRecords = ops.read(
            Consumer.from("my-group", "consumer-1"),
            StreamOffset.create("events", ReadOffset.lastConsumed())
        );

        // XACK
        ops.acknowledge("events", "my-group", id);
    }

    // ===== 공통 Operations =====
    public void commonOperations() {
        // DELETE
        redisTemplate.delete("key");
        redisTemplate.delete(Arrays.asList("key1", "key2"));

        // EXISTS
        Boolean exists = redisTemplate.hasKey("key");

        // EXPIRE
        redisTemplate.expire("key", 1, TimeUnit.HOURS);
        redisTemplate.expireAt("key", Instant.now().plus(1, ChronoUnit.HOURS));

        // TTL
        Long ttl = redisTemplate.getExpire("key");
        Long ttlSeconds = redisTemplate.getExpire("key", TimeUnit.SECONDS);

        // KEYS (주의: 프로덕션에서는 SCAN 사용)
        Set<String> keys = redisTemplate.keys("user:*");

        // TYPE
        DataType type = redisTemplate.type("key");
    }
}
```

### Pipeline 사용

```java
@Service
public class RedisPipelineService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 파이프라인으로 대량 작업 처리
     */
    public void bulkInsert(Map<String, Object> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            data.forEach((key, value) -> {
                byte[] keyBytes = key.getBytes();
                byte[] valueBytes = serialize(value);
                connection.stringCommands().setEx(keyBytes, 3600, valueBytes);
            });
            return null;
        });
    }

    /**
     * 파이프라인으로 대량 조회
     */
    public List<Object> bulkGet(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key ->
                connection.stringCommands().get(key.getBytes()));
            return null;
        });
    }
}
```

---

## 4. StringRedisTemplate

### 문자열 전용 Template

```java
@Service
@RequiredArgsConstructor
public class StringRedisTemplateExamples {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 간단한 문자열 저장
     */
    public void simpleString() {
        stringRedisTemplate.opsForValue().set("status", "active");
        String status = stringRedisTemplate.opsForValue().get("status");
    }

    /**
     * JSON 직렬화/역직렬화
     */
    public void jsonHandling() throws JsonProcessingException {
        ProductResponse product = new ProductResponse(/* ... */);

        // 저장
        String json = objectMapper.writeValueAsString(product);
        stringRedisTemplate.opsForValue().set("product:123", json);

        // 조회
        String cached = stringRedisTemplate.opsForValue().get("product:123");
        ProductResponse restored = objectMapper.readValue(cached, ProductResponse.class);
    }

    /**
     * 카운터 (문자열로 저장되지만 INCR/DECR 가능)
     */
    public Long incrementCounter(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * Sorted Set with StringRedisTemplate
     */
    public void queueWithTimestamp(String queueKey, String entryToken) {
        double score = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(queueKey, entryToken, score);
    }
}
```

---

## 5. Repository 패턴

### Spring Data Redis Repository

```java
// Entity 정의
@RedisHash(value = "user", timeToLive = 3600)  // 1시간 TTL
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCache {

    @Id
    private String id;

    @Indexed  // 검색 가능
    private String email;

    private String name;
    private String role;

    @TimeToLive
    private Long expiration;  // 동적 TTL

    private LocalDateTime lastLogin;
}

// Repository
@Repository
public interface UserCacheRepository extends CrudRepository<UserCache, String> {

    Optional<UserCache> findByEmail(String email);

    List<UserCache> findByRole(String role);
}

// 사용
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserCacheRepository userCacheRepository;

    public void cacheUser(User user) {
        UserCache cache = UserCache.builder()
            .id(user.getId().toString())
            .email(user.getEmail())
            .name(user.getName())
            .role(user.getRole().name())
            .lastLogin(LocalDateTime.now())
            .build();

        userCacheRepository.save(cache);
    }

    public Optional<UserCache> findById(String userId) {
        return userCacheRepository.findById(userId);
    }

    public Optional<UserCache> findByEmail(String email) {
        return userCacheRepository.findByEmail(email);
    }

    public void deleteUser(String userId) {
        userCacheRepository.deleteById(userId);
    }

    public Iterable<UserCache> findAll() {
        return userCacheRepository.findAll();
    }
}
```

### 커스텀 Repository 구현

```java
/**
 * 복잡한 Redis 연산을 위한 커스텀 Repository
 */
public interface ProductCacheRepositoryCustom {
    void cacheWithDependencies(ProductResponse product, List<String> categories);
    List<ProductResponse> findByCategory(String category);
}

@Repository
@RequiredArgsConstructor
public class ProductCacheRepositoryImpl implements ProductCacheRepositoryCustom {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PRODUCT_KEY = "product:";
    private static final String CATEGORY_KEY = "category:";

    @Override
    public void cacheWithDependencies(ProductResponse product, List<String> categories) {
        String productKey = PRODUCT_KEY + product.getId();

        // 트랜잭션으로 원자성 보장
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                // 상품 캐싱
                operations.opsForValue().set(productKey, product);
                operations.expire(productKey, 1, TimeUnit.HOURS);

                // 카테고리별 인덱스
                for (String category : categories) {
                    String categoryKey = CATEGORY_KEY + category + ":products";
                    operations.opsForSet().add(categoryKey, product.getId());
                }

                return operations.exec();
            }
        });
    }

    @Override
    public List<ProductResponse> findByCategory(String category) {
        String categoryKey = CATEGORY_KEY + category + ":products";

        Set<Object> productIds = redisTemplate.opsForSet().members(categoryKey);
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = productIds.stream()
            .map(id -> PRODUCT_KEY + id)
            .collect(Collectors.toList());

        List<Object> products = redisTemplate.opsForValue().multiGet(keys);

        return products.stream()
            .filter(Objects::nonNull)
            .map(p -> (ProductResponse) p)
            .collect(Collectors.toList());
    }
}
```

---

## 6. Spring Cache Abstraction

### 캐시 설정

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // 캐시별 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("sessions", defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }
}
```

### 캐시 어노테이션 사용

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final ProductRepository productRepository;

    /**
     * @Cacheable: 캐시 조회, 없으면 메서드 실행 후 캐싱
     */
    @Cacheable(
        value = "products",
        key = "#productId",
        unless = "#result == null"
    )
    public ProductResponse getProduct(Long productId) {
        log.info("Cache miss - loading from DB: {}", productId);
        return productRepository.findById(productId)
            .map(ProductResponse::from)
            .orElse(null);
    }

    /**
     * 복잡한 키 표현식
     */
    @Cacheable(
        value = "products",
        key = "'category:' + #category + ':page:' + #page"
    )
    public Page<ProductResponse> getProductsByCategory(String category, int page) {
        return productRepository.findByCategory(category, PageRequest.of(page, 20))
            .map(ProductResponse::from);
    }

    /**
     * 조건부 캐싱
     */
    @Cacheable(
        value = "products",
        key = "#productId",
        condition = "#productId > 0",      // 캐시 조회 조건
        unless = "#result.stock < 10"       // 캐시 저장 조건
    )
    public ProductResponse getProductConditional(Long productId) {
        return productRepository.findById(productId)
            .map(ProductResponse::from)
            .orElse(null);
    }

    /**
     * @CachePut: 항상 메서드 실행 후 결과 캐싱
     */
    @CachePut(
        value = "products",
        key = "#result.id"
    )
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productRepository.save(request.toEntity());
        return ProductResponse.from(product);
    }

    /**
     * @CacheEvict: 캐시 삭제
     */
    @CacheEvict(
        value = "products",
        key = "#productId"
    )
    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    /**
     * 모든 캐시 삭제
     */
    @CacheEvict(
        value = "products",
        allEntries = true
    )
    public void clearAllProductCache() {
        log.info("All product cache cleared");
    }

    /**
     * @Caching: 여러 캐시 작업 조합
     */
    @Caching(
        put = @CachePut(value = "products", key = "#result.id"),
        evict = {
            @CacheEvict(value = "products", key = "'list'"),
            @CacheEvict(value = "products", key = "'category:' + #request.category")
        }
    )
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        product.update(request);
        return ProductResponse.from(productRepository.save(product));
    }
}
```

### 커스텀 KeyGenerator

```java
@Configuration
public class CacheKeyConfig {

    @Bean
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(":");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(":");
                sb.append(param != null ? param.toString() : "null");
            }
            return sb.toString();
        };
    }
}

// 사용
@Cacheable(value = "products", keyGenerator = "customKeyGenerator")
public ProductResponse getProduct(Long productId, String locale) {
    // ...
}
```

---

## 7. Redisson 통합

### Redisson 설정

```java
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Single Server
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectionMinimumIdleSize(5)
            .setConnectionPoolSize(50)
            .setIdleConnectionTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        // 스레드 풀 설정
        config.setThreads(16);
        config.setNettyThreads(32);

        return Redisson.create(config);
    }

    /**
     * Redisson 기반 ConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }
}
```

### Redisson 고급 기능

```java
@Service
@RequiredArgsConstructor
public class RedissonAdvancedService {

    private final RedissonClient redissonClient;

    /**
     * 분산 락 (Reentrant Lock)
     */
    public void withLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 5초 대기, 30초 후 자동 해제
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Cannot acquire lock: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        }
    }

    /**
     * ReadWrite Lock
     */
    public void readWriteLockExample() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");

        // 읽기 (동시에 여러 스레드 가능)
        rwLock.readLock().lock();
        try {
            // 읽기 작업
        } finally {
            rwLock.readLock().unlock();
        }

        // 쓰기 (독점)
        rwLock.writeLock().lock();
        try {
            // 쓰기 작업
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 분산 세마포어
     */
    public void semaphoreExample() {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");
        semaphore.trySetPermits(10);  // 동시 10개 허용

        try {
            semaphore.acquire();  // 허가 획득
            // 제한된 작업 수행
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();  // 허가 반환
        }
    }

    /**
     * 분산 CountDownLatch
     */
    public void countDownLatchExample() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("latch");
        latch.trySetCount(3);

        // Worker들
        new Thread(() -> {
            // 작업 수행
            latch.countDown();
        }).start();

        // 대기
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Rate Limiter
     */
    public boolean tryAcquire(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 1초에 10개 허용
        rateLimiter.trySetRate(
            RateType.OVERALL,
            10,
            1,
            RateIntervalUnit.SECONDS
        );

        return rateLimiter.tryAcquire();
    }

    /**
     * 분산 Map (RMap)
     */
    public void mapExample() {
        RMap<String, Object> map = redissonClient.getMap("myMap");

        map.put("key1", "value1");
        map.fastPut("key2", "value2");  // 이전 값 반환 안 함 (더 빠름)

        Object value = map.get("key1");

        // TTL 지원
        map.put("key3", "value3", 10, TimeUnit.MINUTES);
    }

    /**
     * 분산 Queue
     */
    public void queueExample() {
        RQueue<String> queue = redissonClient.getQueue("myQueue");

        queue.add("item1");
        queue.offer("item2");

        String item = queue.poll();
        String peeked = queue.peek();
    }

    /**
     * Pub/Sub
     */
    public void pubSubExample() {
        RTopic topic = redissonClient.getTopic("myTopic");

        // 구독
        topic.addListener(String.class, (channel, message) -> {
            System.out.println("Received: " + message);
        });

        // 발행
        topic.publish("Hello, Redisson!");
    }
}
```

---

## 8. 테스트 전략

### Embedded Redis (테스트용)

```java
@TestConfiguration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
```

### Testcontainers 사용

```java
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("test", "value");
        Object result = redisTemplate.opsForValue().get("test");

        assertThat(result).isEqualTo("value");
    }

    @Test
    void testCacheService() {
        // Given
        ProductResponse product = new ProductResponse(1L, "Test", 1000);

        // When
        redisTemplate.opsForValue().set("product:1", product);

        // Then
        ProductResponse cached = (ProductResponse) redisTemplate.opsForValue().get("product:1");
        assertThat(cached.getName()).isEqualTo("Test");
    }
}
```

### 단위 테스트 (Mock)

```java
@ExtendWith(MockitoExtension.class)
class ProductCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ProductCacheService productCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetProduct_CacheHit() {
        // Given
        Long productId = 1L;
        ProductResponse cached = new ProductResponse(productId, "Test", 1000);
        when(valueOperations.get("product:" + productId)).thenReturn(cached);

        // When
        ProductResponse result = productCacheService.getProduct(productId);

        // Then
        assertThat(result.getName()).isEqualTo("Test");
        verify(valueOperations).get("product:" + productId);
    }

    @Test
    void testGetProduct_CacheMiss() {
        // Given
        Long productId = 1L;
        when(valueOperations.get("product:" + productId)).thenReturn(null);

        // When & Then
        // DB에서 로드하고 캐시에 저장하는 로직 테스트
    }
}
```

---

## 관련 문서

- [Redis Data Structures](./redis-data-structures.md)
- [Redis Caching Patterns](./redis-caching-patterns.md)
- [Redis Distributed Lock](./redis-distributed-lock.md)
- [Redis Portal Universe](./redis-portal-universe.md)
