# Redis Session (세션 저장)

## 개요

Portal Universe auth-service는 JWT 기반 Stateless 인증을 사용하므로 전통적인 HTTP 세션은 사용하지 않습니다. 대신 Redis를 활용하여 Refresh Token 저장과 Token Blacklist 관리를 수행합니다. 이 문서에서는 Redis의 역할과 구성을 설명합니다.

## 현재 Redis 사용 방식

### Stateless vs Stateful 비교

```
┌─────────────────────────────────────────────────────────────┐
│               Traditional Session (Stateful)                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Client ──Cookie: JSESSIONID=abc123──▶ Server               │
│                                            │                 │
│                                            ▼                 │
│                                    ┌──────────────┐         │
│                                    │   Redis      │         │
│                                    │ sessions/abc123        │
│                                    │ { user data } │         │
│                                    └──────────────┘         │
│                                                              │
│  장점: 서버에서 세션 무효화 가능                              │
│  단점: 매 요청마다 Redis 조회 필요                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                JWT + Redis (현재 방식)                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Client ──Authorization: Bearer eyJ...──▶ Server            │
│                                            │                 │
│                                    JWT 자체 검증             │
│                                    (서명, 만료 확인)         │
│                                            │                 │
│                                            ▼                 │
│                                    ┌──────────────┐         │
│                                    │   Redis      │         │
│                                    │ blacklist:*  │ (로그아웃)│
│                                    │ refresh_token:* │       │
│                                    └──────────────┘         │
│                                                              │
│  장점: 서명 검증만으로 인증 (빠름)                            │
│  단점: 즉시 무효화 어려움 → Blacklist로 해결                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## RedisConfig

```java
package com.portal.universe.authservice.config;

/**
 * Redis 연결 및 Template 설정을 구성하는 클래스입니다.
 * Refresh Token 저장 및 Token Blacklist 관리에 사용됩니다.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate을 설정합니다.
     * Key와 Value 모두 String 형태로 직렬화하여 저장합니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key와 Value 직렬화 방식을 String으로 설정
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
```

## Redis 데이터 구조

### 1. Refresh Token 저장

```
┌─────────────────────────────────────────────────────────────┐
│                  Refresh Token Storage                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key Pattern: refresh_token:{user_uuid}                      │
│  Value: JWT Refresh Token                                    │
│  TTL: 7일 (604800000ms)                                      │
│                                                              │
│  예시:                                                       │
│  KEY:   refresh_token:550e8400-e29b-41d4-a716-446655440000  │
│  VALUE: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC...      │
│  TTL:   604800 seconds                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### RefreshTokenService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장합니다.
     */
    public void saveRefreshToken(String userId, String token) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(
            key,
            token,
            jwtConfig.getRefreshTokenExpiration(),
            TimeUnit.MILLISECONDS
        );
        log.info("Refresh token saved for user: {}", userId);
    }

    /**
     * Redis에서 Refresh Token을 조회합니다.
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * Redis에서 Refresh Token을 삭제합니다. (로그아웃 시)
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Refresh token deleted for user: {}", userId);
    }

    /**
     * 저장된 토큰과 요청 토큰을 비교하여 검증합니다.
     */
    public boolean validateRefreshToken(String userId, String token) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            log.warn("Refresh token not found for user: {}", userId);
            return false;
        }

        boolean isValid = storedToken.equals(token);
        if (!isValid) {
            log.warn("Refresh token mismatch for user: {}", userId);
        }
        return isValid;
    }
}
```

### 2. Token Blacklist

```
┌─────────────────────────────────────────────────────────────┐
│                    Token Blacklist                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key Pattern: blacklist:{jwt_token}                          │
│  Value: "blacklisted"                                        │
│  TTL: 토큰 남은 만료 시간                                     │
│                                                              │
│  예시:                                                       │
│  KEY:   blacklist:eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBl...  │
│  VALUE: "blacklisted"                                        │
│  TTL:   845 seconds (토큰 남은 유효시간)                      │
│                                                              │
│  * 토큰 만료 시 자동 삭제 (메모리 효율)                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### TokenBlacklistService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token을 블랙리스트에 추가합니다.
     * TTL은 토큰의 남은 만료 시간으로 설정됩니다.
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
     * Access Token이 블랙리스트에 있는지 확인합니다.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
```

## application.yml 설정

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}  # 선택적
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

## 환경별 설정

### Local (.env.local)

```bash
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Docker Compose (.env.docker)

```bash
REDIS_HOST=redis
REDIS_PORT=6379
```

### Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-service-config
data:
  REDIS_HOST: "redis-service.redis.svc.cluster.local"
  REDIS_PORT: "6379"
```

## Redis CLI 확인

```bash
# Redis 접속
redis-cli

# Refresh Token 확인
KEYS refresh_token:*
GET refresh_token:550e8400-e29b-41d4-a716-446655440000
TTL refresh_token:550e8400-e29b-41d4-a716-446655440000

# Blacklist 확인
KEYS blacklist:*
EXISTS blacklist:eyJhbGciOiJIUzI1NiJ9...
TTL blacklist:eyJhbGciOiJIUzI1NiJ9...
```

## Spring Session (선택적 확장)

Stateless에서 Stateful로 전환이 필요한 경우:

### 의존성 추가

```gradle
implementation 'org.springframework.session:spring-session-data-redis'
```

### 설정

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
```

### application.yml

```yaml
spring:
  session:
    store-type: redis
    redis:
      flush-mode: on_save
      namespace: portal:session
```

## 장애 대응

### Redis 연결 실패 처리

```java
@Service
public class TokenBlacklistService {

    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException e) {
            // 정책 선택:
            // 1. 보안 우선: 모든 토큰 거부
            log.error("Redis connection failed - rejecting all tokens for security");
            return true;

            // 2. 가용성 우선: 블랙리스트 체크 건너뜀
            // log.warn("Redis connection failed - skipping blacklist check");
            // return false;
        }
    }
}
```

### Redis 헬스체크

```java
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            redisTemplate.getConnectionFactory()
                .getConnection().ping();
            return Health.up()
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 모니터링

### Redis 메트릭

```yaml
# Prometheus 메트릭 노출
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 주요 모니터링 항목

| 메트릭 | 설명 | 임계값 |
|--------|------|-------|
| `redis_connected_clients` | 연결된 클라이언트 수 | < 100 |
| `redis_used_memory` | 메모리 사용량 | < 80% |
| `redis_keyspace_hits_ratio` | 캐시 히트율 | > 95% |
| `redis_expired_keys` | 만료된 키 수 | 모니터링 |

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/RedisConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/RefreshTokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenBlacklistService.java`

## 참고 자료

- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/)
- [Spring Session Redis](https://docs.spring.io/spring-session/reference/guides/boot-redis.html)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)
