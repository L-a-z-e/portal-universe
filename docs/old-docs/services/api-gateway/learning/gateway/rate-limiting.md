# Rate Limiting

Redis 기반 Rate Limiting으로 API 호출량을 제어하는 방법을 학습합니다.

## 개요

Rate Limiting은 클라이언트가 일정 시간 내에 보낼 수 있는 요청 수를 제한하여 서비스를 보호합니다.

```
Client Request → Rate Limiter Check → Allow/Deny → Backend Service
                      ↓
                    Redis
              (Token Bucket 저장)
```

## Spring Cloud Gateway Rate Limiter

### 의존성 추가

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
}
```

### Redis 연결 설정

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # Cluster 환경
      # cluster:
      #   nodes:
      #     - redis-node1:6379
      #     - redis-node2:6379
```

## Token Bucket Algorithm

Spring Cloud Gateway는 Token Bucket 알고리즘을 사용합니다.

```
┌─────────────────────────────────────┐
│          Token Bucket               │
│  ┌───┬───┬───┬───┬───┐             │
│  │ T │ T │ T │ T │   │ ← burstCapacity (최대 토큰)
│  └───┴───┴───┴───┴───┘             │
│         ↑                           │
│   replenishRate (초당 토큰 충전)      │
│                                     │
│  요청 1개 = 토큰 1개 소비             │
│  토큰 부족 → 429 Too Many Requests   │
└─────────────────────────────────────┘
```

### 주요 파라미터

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| `replenishRate` | 초당 충전 토큰 수 | 10 (초당 10개) |
| `burstCapacity` | 최대 토큰 수 (순간 허용량) | 20 |
| `requestedTokens` | 요청당 소비 토큰 | 1 |

## 라우트별 Rate Limiting

### 기본 설정

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: shopping-service-route
          uri: ${services.shopping.url}
          predicates:
            - Path=/api/shopping/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10    # 초당 10개
                redis-rate-limiter.burstCapacity: 20    # 최대 20개
                key-resolver: "#{@userKeyResolver}"     # 사용자별 제한
```

### Key Resolver 구현

Rate Limit 대상을 결정하는 Key Resolver를 구현합니다.

```java
package com.portal.universe.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * 사용자 ID 기반 Rate Limiting
     * JWT에서 추출한 X-User-Id 헤더 사용
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            // 인증되지 않은 사용자는 IP 기반
            return Mono.just("ip:" + getClientIp(exchange));
        };
    }

    /**
     * IP 주소 기반 Rate Limiting
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * API 경로 기반 Rate Limiting
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            return Mono.just("path:" + path);
        };
    }

    /**
     * 복합 키 (사용자 + 경로)
     */
    @Bean
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");
            String path = exchange.getRequest().getPath().value();

            String key = (userId != null ? userId : getClientIp(exchange))
                    + ":" + path;
            return Mono.just(key);
        };
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }

        return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
    }
}
```

## 서비스별 차등 Rate Limiting

### 서비스 특성에 따른 설정

```yaml
routes:
  # Auth Service: 로그인 시도 제한 (보안)
  - id: auth-service-login
    uri: ${services.auth.url}
    predicates:
      - Path=/api/auth/login
      - Method=POST
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 5      # 초당 5회
          redis-rate-limiter.burstCapacity: 10     # 최대 10회
          key-resolver: "#{@ipKeyResolver}"        # IP 기반

  # Shopping Service: 주문 API 제한
  - id: shopping-service-orders
    uri: ${services.shopping.url}
    predicates:
      - Path=/api/shopping/orders/**
      - Method=POST
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 3      # 초당 3회
          redis-rate-limiter.burstCapacity: 5      # 최대 5회
          key-resolver: "#{@userKeyResolver}"      # 사용자 기반

  # Blog Service: 조회 API (완화된 제한)
  - id: blog-service-read
    uri: ${services.blog.url}
    predicates:
      - Path=/api/blog/**
      - Method=GET
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 50     # 초당 50회
          redis-rate-limiter.burstCapacity: 100    # 최대 100회
          key-resolver: "#{@ipKeyResolver}"
```

## 커스텀 Rate Limiter

### 고급 Rate Limiter 구현

```java
package com.portal.universe.apigateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class CustomRedisRateLimiter extends AbstractRateLimiter<CustomRedisRateLimiter.Config> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public CustomRedisRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class, "custom-rate-limiter", null);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        Config config = getConfig().get(routeId);

        String key = "rate_limit:" + routeId + ":" + id;
        long windowSizeMs = config.getWindowSize().toMillis();
        int limit = config.getLimit();

        return redisTemplate.opsForZSet()
                .removeRangeByScore(key, 0,
                        Instant.now().toEpochMilli() - windowSizeMs)
                .then(redisTemplate.opsForZSet().size(key))
                .flatMap(count -> {
                    if (count < limit) {
                        // 요청 허용, 현재 시간 추가
                        long now = Instant.now().toEpochMilli();
                        return redisTemplate.opsForZSet()
                                .add(key, String.valueOf(now), now)
                                .then(redisTemplate.expire(key, config.getWindowSize()))
                                .thenReturn(new Response(true,
                                        Map.of("X-RateLimit-Remaining",
                                               String.valueOf(limit - count - 1))));
                    } else {
                        // 요청 거부
                        return Mono.just(new Response(false,
                                Map.of("X-RateLimit-Retry-After",
                                       String.valueOf(windowSizeMs / 1000))));
                    }
                });
    }

    public static class Config {
        private int limit = 100;
        private Duration windowSize = Duration.ofMinutes(1);

        // getters and setters
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public Duration getWindowSize() { return windowSize; }
        public void setWindowSize(Duration windowSize) { this.windowSize = windowSize; }
    }
}
```

## 응답 헤더

Rate Limiting이 적용되면 응답에 관련 헤더가 추가됩니다.

```
X-RateLimit-Remaining: 15        # 남은 요청 수
X-RateLimit-Replenish-Rate: 10   # 초당 충전량
X-RateLimit-Burst-Capacity: 20   # 최대 허용량
X-RateLimit-Requested-Tokens: 1  # 소비된 토큰

# 제한 초과 시 (429 응답)
Retry-After: 1                   # 재시도까지 대기 시간(초)
```

## 에러 응답 커스터마이징

```java
@Component
public class RateLimitExceededHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            exchange.getResponse().getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            String body = """
                {
                    "error": "RATE_LIMIT_EXCEEDED",
                    "message": "Too many requests. Please try again later.",
                    "retryAfter": 60
                }
                """;

            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(body.getBytes());

            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return Mono.error(ex);
    }
}
```

## 모니터링

### Redis에서 Rate Limit 상태 확인

```bash
# 특정 사용자의 Rate Limit 상태
redis-cli ZRANGE "rate_limit:shopping-service:user:123" 0 -1 WITHSCORES

# 전체 Rate Limit 키 확인
redis-cli KEYS "rate_limit:*"
```

### Metrics 확인

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep rate

# 예상 metrics
gateway_requests_total{route="shopping-service",status="rate_limited"}
```

## 주의 사항

1. **Redis 가용성**: Redis 장애 시 Rate Limiting 우회 여부 결정 필요
2. **Cluster 환경**: Redis Cluster 사용 시 키 분산 고려
3. **메모리 관리**: TTL 설정으로 만료된 키 자동 삭제
4. **테스트 환경**: 개발 환경에서는 Rate Limit 완화 또는 비활성화 고려

## 참고 자료

- [Spring Cloud Gateway Rate Limiter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories/requestratelimiter-factory.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
