# 📊 Rate Limiting 학습

> API 요청 횟수를 제한하여 시스템을 보호하는 기법

**난이도**: ⭐⭐⭐ (중급)
**학습 시간**: 45분
**실습 시간**: 30분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Rate Limiting이 필요한 이유 설명하기
- [ ] Token Bucket 알고리즘 동작 원리 이해하기
- [ ] Redis 기반 분산 Rate Limiting 구현하기
- [ ] 다양한 Rate Limit 정책 설계하기

---

## 1️⃣ 왜 Rate Limiting이 필요한가?

### 문제 상황

```
🔴 Brute Force 로그인 시도
- 공격자가 1초에 100번 로그인 시도
- 비밀번호 조합을 무차별 대입
- 서버 리소스 고갈

🔴 API 남용
- 봇이 무제한 크롤링
- 의도치 않은 무한 루프 요청
- 정상 사용자 서비스 방해

🔴 DDoS 공격
- 대량 요청으로 서버 마비
- 가용성 저하
```

### 해결책

```
✅ Rate Limiting 적용
- 요청 횟수 제한
- 초과 시 429 Too Many Requests 반환
- 일정 시간 후 재시도 허용
```

---

## 2️⃣ Token Bucket 알고리즘

### 개념

버킷에 토큰을 담고, 요청마다 토큰을 소비하는 방식

```
┌─────────────────┐
│   Token Bucket  │
│  🪙 🪙 🪙 🪙 🪙 │  Capacity: 5개
│                 │  Refill Rate: 1/초
└─────────────────┘

t=0초:  🪙🪙🪙🪙🪙  (5개) → 요청 ✓ → 🪙🪙🪙🪙 (4개)
t=1초:  🪙🪙🪙🪙🪙  (5개) → 충전됨
t=1초:  🪙🪙🪙🪙🪙  (5개) → 요청 5번 연속 → (0개)
t=1초:  (0개) → 요청 ✗ 429 Error
t=2초:  🪙 (1개) → 충전됨 → 요청 ✓
```

### 장점

- **Burst 허용**: 갑작스런 트래픽 수용
- **유연함**: 용량과 충전 속도 독립 설정
- **직관적**: 이해하기 쉬움

### 우리 프로젝트 코드

```java
// services/api-gateway/src/main/java/com/portal/universe/apigateway/config/RateLimiterConfig.java

@Bean
public RedisRateLimiter strictRedisRateLimiter() {
    return new RedisRateLimiter(
        1,   // replenishRate: 초당 1개 토큰 충전
        5,   // burstCapacity: 최대 5개 토큰 저장
        1    // requestedTokens: 요청당 1개 토큰 소비
    );
}
```

**계산 예시**:
- 5개 저장 가능 → 5번 연속 요청 가능
- 초당 1개 충전 → 분당 60번 요청 가능
- 실제 제한: 5 req/min (버스트) + 60 req/min (지속) = 약 60 req/min

---

## 3️⃣ 프로필별 Rate Limit 설계

### 로그인 API (Strict)

```java
@Bean("strictRedisRateLimiter")
public RedisRateLimiter strictRedisRateLimiter() {
    return new RedisRateLimiter(1, 5, 1);  // 5 req/min
}
```

**적용 이유**:
- Brute Force 공격 방어
- 비밀번호 추측 시도 차단
- 계정 탈취 위험 감소

### 회원가입 API (Very Strict)

```java
@Bean("signupRedisRateLimiter")
public RedisRateLimiter signupRedisRateLimiter() {
    return new RedisRateLimiter(0.05, 3, 1);  // 3 req/min
}
```

**적용 이유**:
- 스팸 계정 생성 방지
- 이메일/SMS 남용 차단
- 리소스 절약

### 인증된 사용자 (Authenticated)

```java
@Bean("authenticatedRedisRateLimiter")
public RedisRateLimiter authenticatedRedisRateLimiter() {
    return new RedisRateLimiter(100, 200, 1);  // 100 req/min
}
```

**적용 이유**:
- 정상 사용자 경험 보장
- 의도치 않은 무한 루프 차단
- 서버 부하 분산

### 비인증 사용자 (Unauthenticated)

```java
@Bean("unauthenticatedRedisRateLimiter")
public RedisRateLimiter unauthenticatedRedisRateLimiter() {
    return new RedisRateLimiter(30, 60, 1);  // 30 req/min
}
```

**적용 이유**:
- 크롤러/봇 차단
- 공개 API 보호
- 익명 남용 방지

---

## 4️⃣ KeyResolver: 누구를 제한할 것인가?

### IP 기반 (비인증)

```java
@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> {
        String ip = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Forwarded-For");

        if (ip == null) {
            ip = exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
        }

        return Mono.just(ip);
    };
}
```

**Redis Key**: `request_rate_limiter:{ip}:{userId}`
- 예: `request_rate_limiter:192.168.1.100:{userId}`

### User 기반 (인증)

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String userId = exchange.getRequest()
            .getHeaders()
            .getFirst("X-User-Id");

        return Mono.justOrEmpty(userId);
    };
}
```

**Redis Key**: `request_rate_limiter:{ip}:{userId}`
- 예: `request_rate_limiter:192.168.1.100:user-123`

### Composite (IP + User)

```java
@Bean
public KeyResolver compositeKeyResolver() {
    return exchange -> {
        String ip = getClientIp(exchange);
        String userId = exchange.getAttribute("X-User-Id");

        if (userId != null) {
            return Mono.just(ip + ":" + userId);
        }
        return Mono.just(ip);
    };
}
```

---

## 5️⃣ Spring Cloud Gateway 라우트 설정

```yaml
# application.yml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            # 로그인 API - Strict Rate Limit
            - id: auth-service-login
              uri: ${services.auth.url}
              predicates:
                - Path=/api/auth/login
                - Method=POST
              filters:
                - name: RequestRateLimiter
                  args:
                    rate-limiter: "#{@strictRedisRateLimiter}"
                    key-resolver: "#{@compositeKeyResolver}"
```

**동작 흐름**:
1. `/api/auth/login` POST 요청
2. `compositeKeyResolver`로 Key 생성 (IP + User)
3. Redis에서 현재 토큰 수 조회
4. 토큰 있으면 ✓, 없으면 429 반환
5. 응답 헤더에 남은 토큰 수 포함

---

## 6️⃣ 응답 헤더로 Rate Limit 정보 제공

```http
HTTP/1.1 200 OK
X-RateLimit-Remaining: 4              # 남은 토큰 수
X-RateLimit-Replenish-Rate: 1         # 초당 충전 속도
X-RateLimit-Burst-Capacity: 5         # 최대 용량
```

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60                        # 60초 후 재시도
Content-Type: application/json

{
  "success": false,
  "error": {
    "code": "TOO_MANY_REQUESTS",
    "message": "요청 한도를 초과했습니다. 60초 후에 다시 시도해주세요."
  }
}
```

### 구현 코드

```java
// RateLimitHeaderFilter.java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return chain.filter(exchange)
        .doOnSuccess(aVoid -> {
            ServerHttpResponse response = exchange.getResponse();
            String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");

            if (remaining != null) {
                log.debug("Rate Limit - Remaining: {}", remaining);
            }
        });
}
```

---

## 7️⃣ Redis 데이터 구조

```redis
# Rate Limiter가 Redis에 저장하는 데이터

KEY: request_rate_limiter.{key}.tokens
VALUE: 남은 토큰 수
TTL: 토큰 만료 시간

KEY: request_rate_limiter.{key}.timestamp
VALUE: 마지막 충전 시간
TTL: 토큰 만료 시간

# 예시
redis> GET request_rate_limiter.192.168.1.100:user-123.tokens
"4"

redis> GET request_rate_limiter.192.168.1.100:user-123.timestamp
"1737640800"
```

---

## 8️⃣ WebFlux 비동기 처리 주의사항

### ❌ 잘못된 패턴

```java
// 응답이 이미 커밋된 후 실행됨 → 헤더 추가 불가
return chain.filter(exchange).then(
    Mono.fromRunnable(() -> addHeaders(exchange))
);
```

### ✅ 올바른 패턴

```java
// 응답 스트림에 영향 없이 로깅만 수행
return chain.filter(exchange)
    .doOnSuccess(aVoid -> {
        // 응답 완료 후 로깅
        logRateLimitInfo(exchange);
    });
```

---

## ✍️ 실습 과제

### 과제 1: Rate Limit 테스트 (기초)

로그인 API에 6번 연속 요청을 보내고 결과를 확인하세요.

```bash
# 5번은 성공
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}' \
    -w "\nStatus: %{http_code}\n"
done

# 6번째는 429 에러
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}' \
  -w "\nStatus: %{http_code}\n"
```

**확인사항**:
- [ ] 6번째 요청이 429 에러를 반환하는가?
- [ ] `Retry-After` 헤더가 포함되어 있는가?
- [ ] 60초 후 다시 요청 가능한가?

### 과제 2: KeyResolver 커스터마이징 (중급)

특정 API Key를 가진 프리미엄 사용자에게 더 높은 제한을 부여하세요.

```java
@Bean
public KeyResolver premiumKeyResolver() {
    return exchange -> {
        String apiKey = exchange.getRequest()
            .getHeaders()
            .getFirst("X-API-Key");

        // TODO: apiKey가 프리미엄이면 다른 prefix 사용
        // 힌트: "premium:" + ip vs "standard:" + ip

        return Mono.just(/* your code */);
    };
}
```

### 과제 3: 동적 Rate Limit (고급)

사용자 등급에 따라 동적으로 Rate Limit을 조정하세요.

```java
@Component
public class DynamicRateLimiter {

    public int getRateLimit(String userId) {
        // TODO: DB에서 사용자 등급 조회
        // BASIC: 30 req/min
        // PRO: 100 req/min
        // ENTERPRISE: 500 req/min
    }
}
```

---

## 🔍 더 알아보기

### 다른 알고리즘

1. **Fixed Window**
   - 고정된 시간 창 (예: 1분)마다 카운터 리셋
   - 구현 간단하지만, 경계에서 버스트 허용

2. **Sliding Window Log**
   - 요청 타임스탬프를 모두 기록
   - 정확하지만 메모리 사용량 높음

3. **Sliding Window Counter**
   - Fixed + Sliding의 하이브리드
   - 근사치로 계산하여 메모리 절약

### 분산 환경 고려사항

- **Redis 단일 장애점**: Redis Cluster 구성
- **네트워크 지연**: Lua 스크립트로 원자성 보장
- **일관성 vs 성능**: Eventual Consistency 허용

### 참고 자료

- [Token Bucket 알고리즘 상세](https://en.wikipedia.org/wiki/Token_bucket)
- [Spring Cloud Gateway Rate Limiter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories/requestratelimiter-factory.html)
- [Redis Rate Limiting Patterns](https://redis.io/docs/reference/patterns/rate-limiting/)

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Token Bucket 알고리즘을 그림으로 설명할 수 있다
- [ ] replenishRate와 burstCapacity의 차이를 이해한다
- [ ] KeyResolver의 역할을 설명할 수 있다
- [ ] 실제 프로젝트에서 Rate Limit 테스트를 완료했다
- [ ] 429 에러 처리 방법을 알고 있다

---

**이전**: [학습 가이드 홈](./README.md)
**다음**: [JWT Key Rotation 학습하기](./02-jwt-key-rotation.md) →
