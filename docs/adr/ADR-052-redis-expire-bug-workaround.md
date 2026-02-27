# ADR-052: Redis EXPIRE 버그 우회 전략 (SETEX + Lua Script)

**Status**: Accepted
**Date**: 2026-02-27
**Author**: Laze

## Context

### 기존 상태

Shopping Service의 Redis 키(쿠폰 재고, 타임딜 재고, 구매 기록)에 TTL이 설정되지 않아 Redis 메모리가 무한 증가하는 문제가 있었다. `initializeStock()`, `initializeCouponStock()` 메서드는 값만 설정하고 만료 시간을 설정하지 않았다.

### TTL 설정 시도 시 발견된 버그

`StringRedisTemplate.expire(key, timeout, unit)` 호출 시 **StackOverflowError** 발생:

```
java.lang.StackOverflowError
  at DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:220)
  at DefaultedRedisConnection.expire(DefaultedRedisConnection.java:213)
  at DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:220)
  ... (무한 재귀)
```

### 근본 원인

두 가지 요인이 결합하여 발생:

1. **`RedissonConnectionFactory`**: `RedissonConnection`이 `expire()`/`pExpire()`를 직접 override하지 않음. `DefaultedRedisConnection`의 default 메서드가 `expire()` ↔ `pExpire()` 상호 호출하여 무한 재귀
2. **`micrometer-tracing-bridge-otel`**: 트레이싱 계측이 Connection을 래핑하면서 메서드 dispatch를 방해

### 영향받는 Redis 키 타입

| 키 패턴 | Redis 타입 | 용도 |
|---------|-----------|------|
| `coupon:stock:{id}` | String | 쿠폰 잔여 수량 |
| `coupon:issued:{id}` | Set | 발급된 사용자 ID 집합 |
| `timedeal:stock:{dealId}:{productId}` | String | 타임딜 상품 잔여 수량 |
| `timedeal:purchased:{dealId}:{productId}:{userId}` | String | 사용자별 구매 수량 |

### 검토한 대안

| 대안 | 적용 대상 | 장점 | 단점 | 선택 여부 |
|------|----------|------|------|:---------:|
| **SETEX (`set(key, value, timeout, unit)`)** | String 값 초기화 시 | `expire()` 미경유, Redis `SET EX` 단일 명령어 | 값 설정 시에만 사용 가능 | **String 키에 선택** |
| **Lua Script (`redis.call('EXPIRE')`)** | 이미 존재하는 키 | Redis 서버에서 직접 실행, Java Connection 계층 우회 | Lua Script 관리 필요 | **Set 키에 선택** |
| Lettuce로 교체 | 전체 | 근본 해결 | Redisson 분산 락/동기화 기능 상실, 대규모 변경 | 미선택 |
| `RedissonConnection` 패치 | 전체 | 근본 해결 | 외부 라이브러리 포크/유지보수 부담 | 미선택 |

## Decision

### 이중 전략: String 키는 SETEX, Set/기존 키는 Lua Script

#### 1. String 키 — SETEX

값 초기화 시 `opsForValue().set(key, value, timeout, TimeUnit)` (= Redis `SET key value EX seconds`)를 사용한다.

```java
// CouponRedisService
public void initializeCouponStock(Long couponId, int quantity, long ttlSeconds) {
    stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity),
            ttlSeconds, TimeUnit.SECONDS);
}

// TimeDealRedisService
public void initializeStock(Long timeDealId, Long productId, int quantity, long ttlSeconds) {
    stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity),
            ttlSeconds, TimeUnit.SECONDS);
}
```

#### 2. Set 키 / 기존 키 — Lua Script

`SADD` 등으로 이미 생성된 키에 TTL을 부여할 때는 Lua Script로 `EXPIRE` 명령어를 직접 실행한다.

```java
private static final String EXPIRE_LUA = "return redis.call('EXPIRE', KEYS[1], ARGV[1])";
private static final DefaultRedisScript<Long> EXPIRE_SCRIPT;
static { EXPIRE_SCRIPT = new DefaultRedisScript<>(EXPIRE_LUA, Long.class); }

private void expireViaLua(String key, long ttlSeconds) {
    stringRedisTemplate.execute(EXPIRE_SCRIPT, Collections.singletonList(key),
            String.valueOf(ttlSeconds));
}
```

#### 3. TTL 정책

| 키 | TTL 계산 |
|-----|---------|
| 쿠폰/타임딜 재고 (stock) | `expiresAt - now + 1일` |
| 쿠폰 발급 목록 (issued) | `expiresAt - now + 1일` |
| 타임딜 구매 기록 (purchased) | `endsAt - now + 1일` |

+1일 버퍼: 만료 직전 구매/발급 처리 중 키가 사라지는 것을 방지.

### 적용 범위

| 서비스 | 파일 | SETEX | Lua Script |
|--------|------|:-----:|:----------:|
| shopping-service | `CouponRedisService` | `initializeCouponStock(id, qty, ttl)` | `setIssuedKeyExpiration(id, ttl)` |
| shopping-service | `CouponRedisBootstrap` | Stock 키 초기화 | Issued Set TTL |
| shopping-service | `CouponServiceImpl` | 쿠폰 생성 시 Stock | - |
| shopping-service | `TimeDealRedisService` | `initializeStock(dealId, prodId, qty, ttl)` | `setPurchasedKeyExpiration(dealId, prodId, userId, ttl)` |
| shopping-service | `TimeDealRedisInitializer` | Stock 키 복원 | - |

## Consequences

### 긍정적

- Redis 메모리 무한 증가 방지 (만료 후 자동 삭제)
- `expire()`/`pExpire()` StackOverflow 완전 우회
- Redisson 분산 락 기능 유지 (Lettuce 교체 불필요)
- SETEX는 원자적 (값 설정 + TTL을 단일 명령어로)

### 부정적

- `expire()` 버그가 코드베이스 전체에 잠재적 영향 (향후 `expire()` 직접 호출 시 동일 에러 가능)
- Lua Script 상수가 두 서비스 파일에 중복 정의

### 리스크

| 리스크 | 완화 |
|--------|------|
| 향후 개발자가 `expire()` 직접 호출 | 코드 주석에 Lua Script 사용 이유 명시 |
| Redisson 업데이트로 버그 수정 시 | Lua Script는 그대로 동작하므로 호환성 문제 없음, 필요 시 제거 가능 |

## Related

- [Coupon System Architecture](../architecture/shopping-service/coupon-system.md)
- [TimeDeal System Architecture](../architecture/shopping-service/timedeal-system.md)
- `services/shopping-service/src/main/java/.../coupon/redis/CouponRedisService.java`
- `services/shopping-service/src/main/java/.../timedeal/redis/TimeDealRedisService.java`
