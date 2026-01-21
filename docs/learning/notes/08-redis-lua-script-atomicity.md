---
id: learning-08
title: Redis Lua Script 원자성
type: learning
status: current
created: 2025-01-21
updated: 2025-01-21
author: Laze
tags:
  - redis
  - lua
  - concurrency
  - atomicity
related:
  - SCENARIO-001-coupon-issue
  - ADR-003-coupon-concurrency
---

# Redis Lua Script 원자성

## 개요

Redis Lua 스크립트는 **여러 Redis 명령을 하나의 원자적(atomic) 단위로 실행**할 수 있게 해주는 강력한 기능입니다. 이 문서에서는 선착순 쿠폰 발급 시나리오를 통해 Lua 스크립트의 원자성이 왜 중요하고 어떻게 동작하는지 설명합니다.

---

## 문제 상황: Race Condition

### 일반적인 Redis 명령어 조합의 문제

```
시나리오: 쿠폰 재고 1개, 사용자 A와 B가 동시 요청
```

```
시간  | 사용자 A              | 사용자 B              | 재고
-----|----------------------|----------------------|-----
T1   | GET stock → 1        |                      | 1
T2   |                      | GET stock → 1        | 1
T3   | DECR stock → 0       |                      | 0
T4   |                      | DECR stock → -1      | -1 ❌
T5   | SADD issued A        |                      |
T6   |                      | SADD issued B        | 과잉 발급!
```

**문제**: 재고가 1개인데 2명이 발급받음 → **과잉 발급 발생**

### 왜 이런 일이 발생하는가?

Redis 명령어는 개별적으로는 원자적이지만, **여러 명령어의 조합은 원자적이지 않습니다**.

```
GET  → 원자적 ✓
DECR → 원자적 ✓
GET + DECR → 원자적 ✗ (중간에 다른 명령 끼어들 수 있음)
```

---

## 해결책: Lua 스크립트

### Redis의 단일 스레드 특성

Redis는 **단일 스레드**로 명령을 처리합니다:
- 한 번에 하나의 명령만 실행
- Lua 스크립트 = 하나의 명령으로 취급
- 스크립트 실행 중 다른 클라이언트 명령 차단

### Lua 스크립트로 해결

```lua
-- coupon_issue.lua
local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]

-- 1. 중복 확인 (원자적으로 시작)
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

-- 2. 재고 확인
local stock = tonumber(redis.call('GET', stockKey) or 0)
if stock <= 0 then
    return 0
end

-- 3. 재고 감소
local newStock = redis.call('DECR', stockKey)

-- 4. 경합 상황 처리 (음수 방지)
if newStock < 0 then
    redis.call('INCR', stockKey)  -- 롤백
    return 0
end

-- 5. 발급 기록 (원자적으로 완료)
redis.call('SADD', issuedKey, userId)
return 1
```

### 실행 흐름 (Lua 스크립트)

```
시나리오: 쿠폰 재고 1개, 사용자 A와 B가 동시 요청
```

```
시간  | Redis 처리            | 재고 | 결과
-----|----------------------|-----|------
T1   | [A 스크립트 전체 실행]  |     |
     | └─ SISMEMBER → false  | 1   |
     | └─ GET → 1            | 1   |
     | └─ DECR → 0           | 0   |
     | └─ SADD A             | 0   | A 성공 ✓
T2   | [B 스크립트 전체 실행]  |     |
     | └─ SISMEMBER → false  | 0   |
     | └─ GET → 0            | 0   |
     | └─ return 0           | 0   | B 실패 (재고 소진) ✓
```

**결과**: 정확히 1명만 발급받음

---

## 핵심 개념

### 1. EVAL 명령어

```bash
EVAL script numkeys key [key ...] arg [arg ...]
```

- `script`: Lua 스크립트 본문
- `numkeys`: 키 개수
- `key`: Redis 키 (KEYS 배열로 접근)
- `arg`: 추가 인자 (ARGV 배열로 접근)

### 2. KEYS vs ARGV

```lua
KEYS[1]  -- 첫 번째 키: coupon:stock:1
KEYS[2]  -- 두 번째 키: coupon:issued:1
ARGV[1]  -- 첫 번째 인자: userId
ARGV[2]  -- 두 번째 인자: maxQuantity
```

**왜 분리하는가?**
- `KEYS`: Redis 클러스터 모드에서 샤딩 결정에 사용
- `ARGV`: 단순 값, 샤딩과 무관

### 3. redis.call vs redis.pcall

```lua
-- redis.call: 에러 발생 시 스크립트 중단
local result = redis.call('GET', key)

-- redis.pcall: 에러 발생 시 에러 객체 반환 (스크립트 계속)
local result = redis.pcall('GET', key)
if result.err then
    -- 에러 처리
end
```

---

## 주의사항

### 1. 스크립트 실행 시간

```
⚠️ Lua 스크립트 실행 중에는 Redis가 다른 요청을 처리하지 못함
```

**권장사항**:
- 스크립트는 가능한 짧게 유지 (< 100ms)
- 복잡한 로직은 애플리케이션 레벨에서 처리
- 무한 루프 주의

### 2. 부작용 관리

```lua
-- ❌ 잘못된 예: 외부 상태 변경 후 실패 가능성
redis.call('DECR', stockKey)
-- ... 중간에 에러 발생하면 DECR은 롤백 안됨!
```

```lua
-- ✓ 올바른 예: 검증 후 변경
local stock = redis.call('GET', stockKey)
if tonumber(stock) <= 0 then
    return 0  -- 변경 없이 종료
end
redis.call('DECR', stockKey)
```

### 3. 클러스터 모드 주의

```lua
-- ❌ 다른 슬롯의 키 접근 시 에러
KEYS[1] = "coupon:stock:1"   -- 슬롯 A
KEYS[2] = "user:balance:101" -- 슬롯 B (CROSSSLOT 에러!)
```

```lua
-- ✓ 같은 해시태그 사용
KEYS[1] = "{coupon}:stock:1"   -- 같은 슬롯
KEYS[2] = "{coupon}:issued:1"  -- 같은 슬롯
```

---

## Spring Boot 연동

### 1. 스크립트 Bean 등록

```java
@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> couponIssueScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("scripts/coupon_issue.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
```

### 2. 스크립트 실행

```java
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> couponIssueScript;

    public Long issueCoupon(Long couponId, Long userId, int maxQuantity) {
        String stockKey = "coupon:stock:" + couponId;
        String issuedKey = "coupon:issued:" + couponId;

        return redisTemplate.execute(
            couponIssueScript,
            Arrays.asList(stockKey, issuedKey),  // KEYS
            String.valueOf(userId),               // ARGV[1]
            String.valueOf(maxQuantity)           // ARGV[2]
        );
    }
}
```

### 3. 스크립트 위치

```
src/main/resources/
└── scripts/
    └── coupon_issue.lua
```

---

## 트레이드오프 분석

### Lua 스크립트 vs 대안

| 방식 | 장점 | 단점 | 적합 상황 |
|------|------|------|----------|
| **Lua 스크립트** | 원자성 보장, 고성능 | 디버깅 어려움, 복잡한 로직 한계 | 간단한 원자적 연산 |
| **Redis Transaction (MULTI/EXEC)** | 명령어 묶음 실행 | 조건부 실행 불가 | 무조건적 묶음 실행 |
| **Redisson RLock** | 익숙한 Lock API | 네트워크 오버헤드 | 복잡한 비즈니스 로직 |
| **DB Pessimistic Lock** | 트랜잭션 일관성 | 낮은 처리량 | 일반 상품 재고 |

### 선택 기준

```
Q: 조건부 로직이 필요한가?
├─ Yes → Lua 스크립트 또는 Redisson
└─ No  → Redis Transaction

Q: 높은 처리량이 필요한가?
├─ Yes → Lua 스크립트
└─ No  → DB Lock 가능

Q: 복잡한 비즈니스 로직인가?
├─ Yes → 애플리케이션 레벨 처리
└─ No  → Lua 스크립트
```

---

## 성능 특성

### 벤치마크 (단일 Redis 인스턴스)

| 연산 | 처리량 | 지연 시간 |
|------|--------|----------|
| 단순 GET | ~100,000 ops/sec | < 1ms |
| Lua 스크립트 (5 명령) | ~50,000 ops/sec | 1-2ms |
| Lua 스크립트 (10 명령) | ~30,000 ops/sec | 2-3ms |

### 최적화 팁

1. **EVALSHA 사용**: 스크립트 본문 대신 SHA1 해시로 호출
   ```bash
   SCRIPT LOAD "return redis.call('GET', KEYS[1])"
   # "a42059..." 반환
   EVALSHA a42059... 1 mykey
   ```

2. **스크립트 캐싱**: Spring의 `DefaultRedisScript`는 자동 캐싱

3. **배치 처리**: 가능하면 여러 키를 한 스크립트에서 처리

---

## 디버깅

### 1. redis-cli에서 테스트

```bash
redis-cli EVAL "$(cat coupon_issue.lua)" 2 coupon:stock:1 coupon:issued:1 101 100
```

### 2. 로깅 추가

```lua
-- 개발 환경에서만 사용
redis.log(redis.LOG_DEBUG, "stock: " .. tostring(stock))
```

### 3. 일반적인 오류

| 오류 | 원인 | 해결 |
|------|------|------|
| `ERR CROSSSLOT` | 다른 슬롯 키 접근 | 해시태그 사용 |
| `NOSCRIPT` | 스크립트 없음 | SCRIPT LOAD 다시 실행 |
| `ERR wrong number of arguments` | 인자 개수 불일치 | KEYS/ARGV 확인 |

---

## 실제 적용: 쿠폰 발급 스크립트

### 전체 코드

```lua
-- coupon_issue.lua
-- KEYS[1] = coupon:stock:{couponId}
-- KEYS[2] = coupon:issued:{couponId}
-- ARGV[1] = userId
-- ARGV[2] = maxQuantity (참고용)

-- Return: 1 (성공), 0 (재고 소진), -1 (중복 발급)

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]

-- Step 1: 중복 발급 확인
-- SISMEMBER: O(1), Set에 멤버 존재 여부 확인
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1  -- 이미 발급받은 사용자
end

-- Step 2: 현재 재고 확인
-- GET: O(1), String 값 조회
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock <= 0 then
    return 0  -- 재고 없음
end

-- Step 3: 재고 감소 (원자적)
-- DECR: O(1), 값 1 감소
local newStock = redis.call('DECR', stockKey)

-- Step 4: 경합 상황 처리
-- DECR 후 음수가 되면 다른 요청이 먼저 처리된 것
if newStock < 0 then
    redis.call('INCR', stockKey)  -- 롤백
    return 0  -- 재고 소진
end

-- Step 5: 발급 기록
-- SADD: O(1), Set에 멤버 추가
redis.call('SADD', issuedKey, userId)

return 1  -- 성공
```

### 동작 보장

| 동시 요청 | 결과 |
|----------|------|
| 100명 vs 50쿠폰 | 정확히 50명 성공 |
| 같은 사용자 10회 | 1회만 성공 |
| 1,000명 vs 1,000쿠폰 | 정확히 1,000명 성공 |

---

## 정리

### 핵심 포인트

1. **원자성**: Lua 스크립트는 Redis에서 단일 명령으로 실행됨
2. **격리성**: 스크립트 실행 중 다른 클라이언트 명령 차단
3. **성능**: 네트워크 왕복 최소화로 높은 처리량

### 사용 시 체크리스트

- [ ] 스크립트 실행 시간 100ms 이내
- [ ] KEYS에 접근할 모든 키 포함
- [ ] 클러스터 모드 시 해시태그 사용
- [ ] 에러 처리 로직 포함
- [ ] 반환값 명확히 정의

---

## 참고 자료

- [Redis Lua Scripting Documentation](https://redis.io/docs/manual/programmability/eval-intro/)
- [SCENARIO-001 선착순 쿠폰 발급](../../scenarios/SCENARIO-001-coupon-issue.md)
- [03-concurrency-control.md](./03-concurrency-control.md) - 동시성 제어 전반
