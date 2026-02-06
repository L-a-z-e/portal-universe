---
id: api-gateway-rate-limiting
title: API Gateway Redis Rate Limiting
type: guide
status: current
created: 2026-01-23
updated: 2026-01-30
author: Laze
tags: [api-gateway, rate-limiting, redis, security]
---

# API Gateway - Redis Rate Limiting

**난이도**: ⭐⭐⭐ | **예상 시간**: 30분 | **카테고리**: Development

## 개요

API Gateway에 Redis 기반 Rate Limiting을 구현하여 API 남용 및 DDoS 공격을 방어합니다.

## 아키텍처

```
Client → API Gateway (Rate Limiter) → Redis (Token Bucket) → Backend Service
```

- **알고리즘**: Token Bucket
- **저장소**: Redis (분산 환경 지원)
- **전략**: IP 기반, User ID 기반, 복합 키 기반

## Rate Limiting 정책

| 엔드포인트 | 제한 | KeyResolver | 목적 |
|-----------|------|-------------|------|
| `POST /api/auth/login` | 5회/분 | IP + 경로 | Brute Force 방어 |
| `POST /api/users/signup` | 3회/분 | IP + 경로 | 회원가입 남용 방지 |
| 인증 API (Profile 등) | 100회/분 | User ID | 인증된 사용자 |
| 공개 API (Blog, Shopping 조회) | 30회/분 | IP | 비인증 사용자 |
| 파일 업로드 | 100회/분 | User ID | 인증된 사용자 |

## 구성 요소

### 1. RateLimiterConfig
- **위치**: `config/RateLimiterConfig.java`
- **기능**:
  - Redis Rate Limiter Bean 정의
  - KeyResolver 구현 (IP, User ID, 복합)
  - 다양한 Rate Limiter 정책 제공

### 2. RateLimitHeaderFilter
- **위치**: `filter/RateLimitHeaderFilter.java`
- **기능**:
  - Rate Limit 정보를 응답 헤더에 추가
  - 429 응답 커스터마이징 (ApiResponse 형식)
  - Retry-After 헤더 자동 계산

### 3. Redis 설정
- **application.yml**: 기본 설정 (환경 변수 기반)
- **application-local.yml**: 로컬 Redis (localhost:6379)
- **application-docker.yml**: Docker Compose (redis:6379)
- **application-kubernetes.yml**: K8s (redis-service:6379)

## 응답 헤더

### 정상 응답
```
X-RateLimit-Remaining: 28
X-RateLimit-Replenish-Rate: 0.5
X-RateLimit-Burst-Capacity: 30
```

### 429 Too Many Requests
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Remaining: 0
X-RateLimit-Replenish-Rate: 0.083
X-RateLimit-Burst-Capacity: 5
Retry-After: 12
Content-Type: application/json

{
  "success": false,
  "data": null,
  "error": {
    "code": "TOO_MANY_REQUESTS",
    "message": "요청 한도를 초과했습니다. 12초 후에 다시 시도해주세요."
  }
}
```

## Token Bucket 파라미터

| 파라미터 | 설명 | 예시 |
|---------|------|------|
| `replenishRate` | 초당 토큰 충전 속도 | 1 = 1 req/sec<br>0.083 ≈ 5 req/min |
| `burstCapacity` | 최대 버스트 용량 | 20 = 순간 최대 20개 요청 |
| `requestedTokens` | 요청당 소비 토큰 | 1 (기본값) |

## 로컬 테스트

### 1. Redis 실행
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 2. API Gateway 실행
```bash
cd services/api-gateway
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Rate Limit 테스트
```bash
# 로그인 API - 5회/분 제한 테스트
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}' \
    -i
  echo "Request $i"
  sleep 1
done
```

**예상 결과**:
- 1~5번째 요청: 200/401 (정상 처리)
- 6번째부터: 429 Too Many Requests

### 4. Redis 키 확인
```bash
docker exec -it redis redis-cli

# Rate Limit 키 조회
KEYS request_rate_limiter*

# 특정 키의 남은 토큰 확인
GET request_rate_limiter.{192.168.1.100:/api/auth/login}.tokens
```

## 모니터링

### Prometheus Metrics
```
# Rate Limit 관련 메트릭
spring_cloud_gateway_requests_total{route="auth-service-login",status="429"}
```

### Logs
```
# 429 응답 로그
2025-01-23 15:30:45 WARN  [...] Rate limit exceeded for request: POST /api/auth/login | Retry after 12 seconds
```

## 운영 고려사항

### 1. Redis 가용성
- Redis 장애 시 Rate Limiting 비활성화 (Fail Open 전략)
- Redis Sentinel 또는 Redis Cluster 권장

### 2. 키 만료
- Redis는 자동으로 키 만료 처리 (TTL 기반)
- 메모리 부족 시 eviction policy: `allkeys-lru`

### 3. 정책 조정
```java
// 특정 엔드포인트의 Rate Limit 조정
@Bean
public RedisRateLimiter customRedisRateLimiter() {
    // replenishRate: 10 req/sec
    // burstCapacity: 50
    return new RedisRateLimiter(10, 50, 1);
}
```

```yaml
# application.yml에서 라우트에 적용
filters:
  - name: RequestRateLimiter
    args:
      rate-limiter: "#{@customRedisRateLimiter}"
      key-resolver: "#{@ipKeyResolver}"
```

### 4. 화이트리스트
특정 IP를 Rate Limit에서 제외하려면:

```java
@Bean
public KeyResolver whitelistKeyResolver() {
    return exchange -> {
        String clientIp = getClientIp(exchange);
        if (WHITELIST.contains(clientIp)) {
            return Mono.just("whitelist"); // 공통 키로 제한 없음
        }
        return Mono.just(clientIp);
    };
}
```

## 트러블슈팅

### 문제: Rate Limit이 적용되지 않음
**원인**: Redis 연결 실패
**해결**:
```bash
# Redis 상태 확인
docker logs redis

# API Gateway 로그 확인
tail -f logs/api-gateway.log | grep -i redis
```

### 문제: 429 응답이 예상보다 빨리 발생
**원인**: `burstCapacity`가 너무 작음
**해결**: `burstCapacity`를 `replenishRate`의 2~10배로 조정

### 문제: 분산 환경에서 Rate Limit이 일관되지 않음
**원인**: 각 Gateway 인스턴스가 독립적으로 계산
**해결**: Redis를 공유 저장소로 사용 (이미 구현됨)

## 참고 자료

- [Spring Cloud Gateway Rate Limiting](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-requestratelimiter-gatewayfilter-factory)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [Redis Rate Limiting Patterns](https://redis.io/docs/reference/patterns/rate-limiter/)
