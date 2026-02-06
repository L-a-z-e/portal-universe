# Redis Session Management

Spring Session Redis를 사용한 분산 세션 관리 방법을 학습합니다.

## 목차

1. [세션 관리 개요](#1-세션-관리-개요)
2. [Spring Session Redis 설정](#2-spring-session-redis-설정)
3. [세션 저장소 구조](#3-세션-저장소-구조)
4. [클러스터 환경 설정](#4-클러스터-환경-설정)
5. [JWT와 Redis 세션](#5-jwt와-redis-세션)
6. [세션 보안](#6-세션-보안)
7. [성능 최적화](#7-성능-최적화)
8. [모니터링](#8-모니터링)

---

## 1. 세션 관리 개요

### 전통적인 세션 vs Redis 세션

```
전통적인 세션 (서버 메모리):
+--------+     +--------+     +--------+
| Server | --- | Server | --- | Server |
| (Sess) |     | (Sess) |     | (Sess) |
+--------+     +--------+     +--------+
    |              |              |
    v              v              v
[User A]       [User B]       [User C]

문제점:
- Sticky Session 필요
- 서버 장애 시 세션 유실
- Scale-out 어려움


Redis 기반 분산 세션:
+--------+     +--------+     +--------+
| Server |     | Server |     | Server |
+--------+     +--------+     +--------+
    |              |              |
    +------+-------+-------+------+
           |               |
           v               v
      +---------+    +---------+
      |  Redis  |----| Replica |
      | Primary |    |         |
      +---------+    +---------+

장점:
- 로드 밸런싱 자유로움
- 서버 무상태 (Stateless)
- Scale-out 용이
- 세션 공유
```

### 아키텍처 비교

| 특성 | In-Memory Session | Redis Session |
|------|-------------------|---------------|
| 확장성 | Sticky Session 필요 | 자유로운 Scale-out |
| 가용성 | 서버 장애 시 유실 | 고가용성 |
| 성능 | 매우 빠름 (로컬) | 빠름 (네트워크 오버헤드) |
| 복잡도 | 단순 | 인프라 추가 필요 |
| 비용 | 낮음 | Redis 서버 비용 |

---

## 2. Spring Session Redis 설정

### 의존성 추가

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'
}
```

### 기본 설정

```yaml
# application.yml
spring:
  session:
    store-type: redis
    timeout: 30m  # 세션 타임아웃
    redis:
      flush-mode: on-save  # immediate or on-save
      namespace: spring:session  # Redis 키 접두사

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
```

### Java 설정

```java
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800,  // 30분
    redisNamespace = "portal:session",
    flushMode = FlushMode.ON_SAVE
)
public class SessionConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379)
        );
    }

    /**
     * 세션 Serializer 커스터마이징
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 세션 이벤트 리스너
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
```

### Security 통합

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)  // 동시 세션 수 제한
                .maxSessionsPreventsLogin(false)  // 새 로그인 시 기존 세션 만료
                .expiredUrl("/login?expired")
            )
            .rememberMe(remember -> remember
                .key("rememberMeKey")
                .tokenValiditySeconds(604800)  // 7일
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION")
            )
            .build();
    }
}
```

---

## 3. 세션 저장소 구조

### Redis 키 구조

```
Spring Session Redis 저장 구조:

1. 세션 데이터 (Hash)
   Key: spring:session:sessions:{sessionId}
   Fields:
     - creationTime: 생성 시간
     - lastAccessedTime: 마지막 접근 시간
     - maxInactiveInterval: 만료 간격
     - sessionAttr:{attrName}: 세션 속성

2. 세션 만료 관리 (Set)
   Key: spring:session:expirations:{expireTime}
   Members: 해당 시간에 만료될 세션 ID들

3. 세션 인덱스 (Set) - principal 기반 조회용
   Key: spring:session:index:org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME:{username}
   Members: 해당 사용자의 세션 ID들
```

### 세션 라이프사이클

```
세션 생성:
Client                    Spring                    Redis
   |                         |                        |
   |-- HTTP Request -------->|                        |
   |                         |-- HSET (session) ----->|
   |                         |-- SADD (expiration) -->|
   |<-- Set-Cookie: SESSION--|                        |


세션 조회:
Client                    Spring                    Redis
   |                         |                        |
   |-- HTTP Request -------->|                        |
   |   (Cookie: SESSION)     |                        |
   |                         |-- HGETALL ------------>|
   |                         |<-- Session Data -------|
   |                         |-- HSET (lastAccess) -->|
   |<-- Response ------------|                        |


세션 만료:
Redis                     Spring
   |                         |
   |-- Key Expired Event --->|
   |                         |-- SessionDestroyedEvent
   |                         |-- Cleanup Index
```

### 세션 데이터 조회

```java
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 세션 정보 확인 (디버깅용)
     */
    @GetMapping("/session/info")
    public Map<String, Object> getSessionInfo(HttpSession session) {
        Map<String, Object> info = new HashMap<>();

        info.put("sessionId", session.getId());
        info.put("creationTime", new Date(session.getCreationTime()));
        info.put("lastAccessedTime", new Date(session.getLastAccessedTime()));
        info.put("maxInactiveInterval", session.getMaxInactiveInterval());

        // 세션 속성들
        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            attributes.put(name, session.getAttribute(name));
        }
        info.put("attributes", attributes);

        return info;
    }

    /**
     * Redis에서 직접 세션 조회
     */
    @GetMapping("/session/redis/{sessionId}")
    public Map<Object, Object> getSessionFromRedis(@PathVariable String sessionId) {
        String key = "spring:session:sessions:" + sessionId;
        return redisTemplate.opsForHash().entries(key);
    }
}
```

---

## 4. 클러스터 환경 설정

### Redis Sentinel 구성

```yaml
# application-production.yml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - sentinel1.example.com:26379
          - sentinel2.example.com:26379
          - sentinel3.example.com:26379
        password: ${REDIS_PASSWORD}
```

```java
@Configuration
@Profile("production")
public class SentinelSessionConfig {

    @Bean
    public RedisConnectionFactory sentinelConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("sentinel1.example.com", 26379)
            .sentinel("sentinel2.example.com", 26379)
            .sentinel("sentinel3.example.com", 26379);

        sentinelConfig.setPassword(RedisPassword.of("password"));

        // Lettuce 클라이언트 설정
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)  // 읽기는 Replica 우선
            .commandTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }
}
```

### Redis Cluster 구성

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis1:6379
          - redis2:6379
          - redis3:6379
          - redis4:6379
          - redis5:6379
          - redis6:6379
        max-redirects: 3
```

```java
@Configuration
@Profile("cluster")
public class ClusterSessionConfig {

    @Bean
    public RedisConnectionFactory clusterConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
            Arrays.asList(
                "redis1:6379", "redis2:6379", "redis3:6379",
                "redis4:6379", "redis5:6379", "redis6:6379"
            )
        );
        clusterConfig.setMaxRedirects(3);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

### 다중 인스턴스 환경

```
                    Load Balancer
                          |
         +----------------+----------------+
         |                |                |
    +---------+      +---------+      +---------+
    | App #1  |      | App #2  |      | App #3  |
    +---------+      +---------+      +---------+
         |                |                |
         +----------------+----------------+
                          |
                    Redis Cluster
         +----------------+----------------+
         |                |                |
    +---------+      +---------+      +---------+
    | Redis 1 |      | Redis 2 |      | Redis 3 |
    | Primary |      | Primary |      | Primary |
    +---------+      +---------+      +---------+
         |                |                |
    +---------+      +---------+      +---------+
    | Redis 4 |      | Redis 5 |      | Redis 6 |
    | Replica |      | Replica |      | Replica |
    +---------+      +---------+      +---------+
```

---

## 5. JWT와 Redis 세션

### Stateless JWT + Redis 블랙리스트

Portal Universe에서 사용하는 패턴입니다.

```
JWT 인증 흐름:
                                            +-------+
Client                  Auth Service        | Redis |
   |                          |             +-------+
   |-- Login Request -------->|                 |
   |                          |                 |
   |<-- JWT (Access+Refresh) -|                 |
   |                          |                 |
   |                          |-- Store Refresh Token
   |                          |---------------->|
   |                          |                 |
   |-- API Request + JWT ---->|                 |
   |                          |-- Check Blacklist
   |                          |---------------->|
   |                          |<----------------|
   |<-- Response -------------|                 |


로그아웃 흐름:
Client                  Auth Service        Redis
   |                          |               |
   |-- Logout + JWT --------->|               |
   |                          |-- Add to Blacklist
   |                          |-------------->|
   |                          |-- Delete Refresh
   |                          |-------------->|
   |<-- Success --------------|               |
```

### 구현 예제 (Portal Universe 패턴)

```java
/**
 * Access Token 블랙리스트 관리
 * (TokenBlacklistService from auth-service)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token을 블랙리스트에 추가
     * TTL은 토큰의 남은 만료 시간
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

/**
 * Refresh Token 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_PREFIX = "refresh:";
    private static final long REFRESH_TTL_DAYS = 7;

    /**
     * Refresh Token 저장
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        String key = REFRESH_PREFIX + userId;
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            REFRESH_TTL_DAYS,
            TimeUnit.DAYS
        );
    }

    /**
     * Refresh Token 검증
     */
    public boolean validateRefreshToken(String userId, String refreshToken) {
        String key = REFRESH_PREFIX + userId;
        String stored = (String) redisTemplate.opsForValue().get(key);
        return refreshToken.equals(stored);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 모든 디바이스에서 로그아웃
     */
    public void deleteAllUserTokens(String userId) {
        Set<String> keys = redisTemplate.keys(REFRESH_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### JWT + Session Hybrid

```java
/**
 * JWT를 사용하되 세션 상태도 Redis에 유지
 * (예: 실시간 사용자 상태, 권한 변경 즉시 반영)
 */
@Service
@RequiredArgsConstructor
public class HybridSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "session:";

    @Data
    @Builder
    public static class UserSession {
        private String userId;
        private String role;
        private Set<String> permissions;
        private LocalDateTime lastActivity;
        private Map<String, Object> metadata;
    }

    /**
     * 세션 생성/갱신
     */
    public void createOrUpdateSession(String userId, UserSession session) {
        String key = SESSION_PREFIX + userId;

        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("role", session.getRole());
        sessionMap.put("permissions", session.getPermissions());
        sessionMap.put("lastActivity", session.getLastActivity().toString());
        sessionMap.put("metadata", session.getMetadata());

        redisTemplate.opsForHash().putAll(key, sessionMap);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    /**
     * 세션 조회
     */
    public Optional<UserSession> getSession(String userId) {
        String key = SESSION_PREFIX + userId;
        Map<Object, Object> sessionMap = redisTemplate.opsForHash().entries(key);

        if (sessionMap.isEmpty()) {
            return Optional.empty();
        }

        // 세션 접근 시 TTL 갱신
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        return Optional.of(UserSession.builder()
            .userId(userId)
            .role((String) sessionMap.get("role"))
            .permissions((Set<String>) sessionMap.get("permissions"))
            .lastActivity(LocalDateTime.parse((String) sessionMap.get("lastActivity")))
            .metadata((Map<String, Object>) sessionMap.get("metadata"))
            .build());
    }

    /**
     * 권한 변경 즉시 반영
     */
    public void updatePermissions(String userId, Set<String> newPermissions) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.opsForHash().put(key, "permissions", newPermissions);
    }

    /**
     * 강제 로그아웃
     */
    public void forceLogout(String userId) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
```

---

## 6. 세션 보안

### 세션 고정 공격 방지

```java
@Configuration
@EnableWebSecurity
public class SessionSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session
                // 로그인 시 새 세션 ID 발급
                .sessionFixation().newSession()
            )
            .build();
    }
}
```

### 세션 속성 암호화

```java
@Component
public class EncryptedSessionAttributeSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    public EncryptedSessionAttributeSerializer(
            ObjectMapper objectMapper,
            @Value("${session.encryption.key}") String key) throws Exception {

        this.objectMapper = objectMapper;

        SecretKeySpec secretKey = new SecretKeySpec(
            key.getBytes(StandardCharsets.UTF_8), "AES");

        this.encryptCipher = Cipher.getInstance("AES");
        this.encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        this.decryptCipher = Cipher.getInstance("AES");
        this.decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        try {
            byte[] json = objectMapper.writeValueAsBytes(obj);
            return encryptCipher.doFinal(json);
        } catch (Exception e) {
            throw new SerializationException("Encryption failed", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        try {
            byte[] decrypted = decryptCipher.doFinal(bytes);
            return objectMapper.readValue(decrypted, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Decryption failed", e);
        }
    }
}
```

### IP 바인딩

```java
@Component
public class IpBoundSessionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            String boundIp = (String) session.getAttribute("BOUND_IP");
            String currentIp = getClientIp(request);

            if (boundIp == null) {
                // 첫 요청 시 IP 바인딩
                session.setAttribute("BOUND_IP", currentIp);
            } else if (!boundIp.equals(currentIp)) {
                // IP 변경 감지 - 세션 무효화
                session.invalidate();
                response.sendRedirect("/login?reason=ip_changed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 동시 세션 제어

```java
@Service
@RequiredArgsConstructor
public class ConcurrentSessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_SESSIONS = 3;

    /**
     * 사용자의 모든 세션 조회
     */
    public Map<String, ? extends Session> getUserSessions(String username) {
        return sessionRepository.findByIndexNameAndIndexValue(
            FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
            username
        );
    }

    /**
     * 동시 세션 제한 확인 및 처리
     */
    public void validateAndManageSessions(String username, String currentSessionId) {
        Map<String, ? extends Session> sessions = getUserSessions(username);

        if (sessions.size() > MAX_SESSIONS) {
            // 가장 오래된 세션들 제거
            sessions.entrySet().stream()
                .filter(e -> !e.getKey().equals(currentSessionId))
                .sorted(Comparator.comparing(e -> e.getValue().getLastAccessedTime()))
                .limit(sessions.size() - MAX_SESSIONS)
                .forEach(e -> {
                    sessionRepository.deleteById(e.getKey());
                });
        }
    }

    /**
     * 특정 세션 강제 종료
     */
    public void expireSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId);
        if (session != null) {
            sessionRepository.deleteById(sessionId);
        }
    }

    /**
     * 사용자의 모든 세션 종료
     */
    public void expireAllUserSessions(String username) {
        Map<String, ? extends Session> sessions = getUserSessions(username);
        sessions.keySet().forEach(sessionRepository::deleteById);
    }
}
```

---

## 7. 성능 최적화

### 세션 데이터 최소화

```java
/**
 * 세션에 저장할 최소한의 데이터만 정의
 */
@Data
@Builder
public class MinimalSessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String role;  // 단일 역할만 저장

    // 상세 정보는 세션이 아닌 캐시에서 조회
    // private List<Permission> permissions;  // X
    // private UserProfile profile;  // X
}

@Service
public class SessionOptimizationService {

    private final UserDetailsCacheService userDetailsCache;

    public void setSessionUser(HttpSession session, User user) {
        // 세션에는 최소 정보만
        MinimalSessionUser sessionUser = MinimalSessionUser.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();

        session.setAttribute("user", sessionUser);

        // 상세 정보는 별도 캐시에 저장
        userDetailsCache.cacheUserDetails(user);
    }
}
```

### Lazy Loading 세션 속성

```java
@Component
public class LazySessionAttributeHolder {

    private final UserDetailsService userDetailsService;
    private final CacheManager cacheManager;

    /**
     * 세션에서 사용자 정보를 lazy하게 로드
     */
    public UserDetails getCurrentUser(HttpSession session) {
        MinimalSessionUser sessionUser =
            (MinimalSessionUser) session.getAttribute("user");

        if (sessionUser == null) {
            return null;
        }

        // 캐시에서 상세 정보 조회 (없으면 DB)
        Cache cache = cacheManager.getCache("userDetails");
        UserDetails cached = cache.get(sessionUser.getId(), UserDetails.class);

        if (cached != null) {
            return cached;
        }

        UserDetails details = userDetailsService.loadById(sessionUser.getId());
        cache.put(sessionUser.getId(), details);

        return details;
    }
}
```

### 세션 쓰기 최적화

```java
@Configuration
@EnableRedisHttpSession(flushMode = FlushMode.ON_SAVE)
public class OptimizedSessionConfig {

    /**
     * FlushMode.ON_SAVE: 요청 처리 완료 시 한 번에 저장
     * FlushMode.IMMEDIATE: 속성 변경 시 즉시 저장
     *
     * ON_SAVE가 네트워크 왕복을 줄여 성능에 유리
     */
}

/**
 * 세션 변경 배치 처리
 */
@Component
public class BatchSessionUpdater {

    private final HttpSession session;
    private final Map<String, Object> pendingUpdates = new HashMap<>();

    public void setAttribute(String name, Object value) {
        pendingUpdates.put(name, value);
    }

    @PreDestroy
    public void flush() {
        pendingUpdates.forEach(session::setAttribute);
        pendingUpdates.clear();
    }
}
```

### Redis Pipeline 활용

```java
@Configuration
public class PipelinedSessionConfig {

    @Bean
    public ConfigureRedisAction configureRedisAction() {
        // Keyspace Notifications 활성화 (세션 만료 이벤트)
        return (connection) -> {
            connection.setConfig("notify-keyspace-events", "Egx");
        };
    }

    /**
     * 세션 조회 최적화
     */
    @Bean
    public RedisSessionRepository sessionRepository(
            RedisConnectionFactory connectionFactory) {

        // Pipeline을 사용한 세션 조회
        return new RedisIndexedSessionRepository(
            new RedisTemplate<>() {{
                setConnectionFactory(connectionFactory);
                setEnableDefaultSerializer(true);
                afterPropertiesSet();
            }}
        );
    }
}
```

---

## 8. 모니터링

### 세션 메트릭 수집

```java
@Component
@RequiredArgsConstructor
public class SessionMetricsCollector {

    private final FindByIndexNameSessionRepository<?> sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000)  // 1분마다
    public void collectMetrics() {
        // 총 세션 수
        Set<String> sessionKeys = redisTemplate.keys("spring:session:sessions:*");
        int totalSessions = sessionKeys != null ? sessionKeys.size() : 0;

        meterRegistry.gauge("session.total", totalSessions);

        // 활성 세션 수 (최근 5분 내 접근)
        long activeThreshold = System.currentTimeMillis() - (5 * 60 * 1000);
        // 실제 구현에서는 세션별 lastAccessedTime 확인 필요

        // 세션 생성/삭제 이벤트 카운터
        // SessionCreatedEvent, SessionDeletedEvent 리스너에서 증가
    }

    @EventListener
    public void onSessionCreated(SessionCreatedEvent event) {
        meterRegistry.counter("session.created").increment();
    }

    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        meterRegistry.counter("session.deleted").increment();
    }

    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        meterRegistry.counter("session.expired").increment();
    }
}
```

### 세션 대시보드 API

```java
@RestController
@RequestMapping("/admin/sessions")
@RequiredArgsConstructor
public class SessionAdminController {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 세션 통계 조회
     */
    @GetMapping("/stats")
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();

        Set<String> sessionKeys = redisTemplate.keys("spring:session:sessions:*");
        stats.put("totalSessions", sessionKeys != null ? sessionKeys.size() : 0);

        // Redis 메모리 사용량
        Properties info = redisTemplate.getRequiredConnectionFactory()
            .getConnection().info("memory");
        stats.put("usedMemory", info.getProperty("used_memory_human"));

        return stats;
    }

    /**
     * 사용자별 세션 조회
     */
    @GetMapping("/user/{username}")
    public List<Map<String, Object>> getUserSessions(@PathVariable String username) {
        Map<String, ? extends Session> sessions =
            sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                username
            );

        return sessions.entrySet().stream()
            .map(e -> {
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", e.getKey());
                sessionInfo.put("creationTime", e.getValue().getCreationTime());
                sessionInfo.put("lastAccessedTime", e.getValue().getLastAccessedTime());
                return sessionInfo;
            })
            .collect(Collectors.toList());
    }

    /**
     * 세션 강제 종료
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        sessionRepository.deleteById(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자의 모든 세션 종료
     */
    @DeleteMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> deleteUserSessions(
            @PathVariable String username) {

        Map<String, ? extends Session> sessions =
            sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                username
            );

        sessions.keySet().forEach(sessionRepository::deleteById);

        return ResponseEntity.ok(Map.of(
            "deletedCount", sessions.size(),
            "username", username
        ));
    }
}
```

### 알림 설정

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionAlertService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertNotificationService alertService;

    private static final int SESSION_WARNING_THRESHOLD = 10000;
    private static final int SESSION_CRITICAL_THRESHOLD = 50000;

    @Scheduled(fixedRate = 300000)  // 5분마다
    public void checkSessionHealth() {
        Set<String> sessionKeys = redisTemplate.keys("spring:session:sessions:*");
        int sessionCount = sessionKeys != null ? sessionKeys.size() : 0;

        if (sessionCount > SESSION_CRITICAL_THRESHOLD) {
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                "Session count critical: " + sessionCount
            );
        } else if (sessionCount > SESSION_WARNING_THRESHOLD) {
            alertService.sendAlert(
                AlertLevel.WARNING,
                "Session count high: " + sessionCount
            );
        }

        // Redis 메모리 체크
        Properties info = redisTemplate.getRequiredConnectionFactory()
            .getConnection().info("memory");

        long usedMemory = Long.parseLong(info.getProperty("used_memory"));
        long maxMemory = Long.parseLong(info.getProperty("maxmemory", "0"));

        if (maxMemory > 0 && usedMemory > maxMemory * 0.9) {
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                "Redis memory usage > 90%"
            );
        }
    }
}
```

---

## 관련 문서

- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Caching Patterns](./redis-caching-patterns.md)
- [Redis Distributed Lock](./redis-distributed-lock.md)
- [Redis Portal Universe](./redis-portal-universe.md)
