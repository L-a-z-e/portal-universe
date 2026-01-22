# Redis 소개

## 학습 목표
- Redis의 특성과 사용 사례 이해
- 핵심 데이터 구조 파악
- Portal Universe에서의 Redis 활용 개요

---

## 1. Redis란?

Redis(Remote Dictionary Server)는 **인메모리 데이터 스토어**입니다. 모든 데이터를 메모리에 저장하여 매우 빠른 읽기/쓰기 성능을 제공합니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **In-Memory** | 모든 데이터가 RAM에 저장 |
| **초고속** | 평균 응답 시간 < 1ms |
| **다양한 자료구조** | String, Hash, List, Set, Sorted Set 등 |
| **영속성 옵션** | RDB 스냅샷, AOF 로그 |
| **Pub/Sub** | 실시간 메시지 브로드캐스트 |
| **Lua 스크립트** | 원자적 복합 연산 |

### Redis vs 전통 DB

```
[전통적 DB]
Application → 쿼리 → Disk I/O → 결과
           (수 ms ~ 수십 ms)

[Redis]
Application → 명령어 → Memory 접근 → 결과
           (< 1ms)
```

---

## 2. 데이터 구조

### 2.1 String

가장 기본적인 키-값 저장 구조입니다.

```
SET user:token:123 "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
GET user:token:123

SET coupon:stock:SUMMER2024 100
DECR coupon:stock:SUMMER2024  → 99
```

**Portal Universe 사용 예:**
- JWT Refresh Token 저장
- 쿠폰/타임딜 재고 카운터

### 2.2 Hash

필드-값 쌍의 집합으로, 객체 저장에 적합합니다.

```
HSET user:1000 name "홍길동" email "hong@example.com"
HGET user:1000 name  → "홍길동"
HGETALL user:1000    → {name: "홍길동", email: "hong@example.com"}
```

### 2.3 List

순서가 있는 문자열 목록입니다.

```
LPUSH search:recent:user123 "아이폰" "갤럭시" "맥북"
LRANGE search:recent:user123 0 -1  → ["맥북", "갤럭시", "아이폰"]
LTRIM search:recent:user123 0 19   # 최근 20개만 유지
```

**Portal Universe 사용 예:**
- 사용자별 최근 검색어 (최대 20개)

### 2.4 Set

중복 없는 문자열 집합입니다.

```
SADD coupon:issued:SUMMER2024 "user123" "user456"
SISMEMBER coupon:issued:SUMMER2024 "user123"  → 1 (존재)
SISMEMBER coupon:issued:SUMMER2024 "user789"  → 0 (미존재)
```

**Portal Universe 사용 예:**
- 쿠폰 발급 사용자 추적 (중복 발급 방지)

### 2.5 Sorted Set (ZSet)

스코어 기반 정렬 집합입니다.

```
ZADD search:popular 100 "아이폰" 85 "갤럭시" 70 "맥북"
ZINCRBY search:popular 1 "아이폰"  # 검색할 때마다 증가
ZREVRANGE search:popular 0 9       # 상위 10개 인기 검색어
```

**Portal Universe 사용 예:**
- 인기 검색어 순위
- 대기열 관리 (입장 시간 = 스코어)

### 데이터 구조 요약

| 구조 | 특성 | 사용 사례 |
|------|------|----------|
| **String** | 단순 키-값 | 토큰, 카운터, 캐시 |
| **Hash** | 객체 필드 | 사용자 정보, 세션 |
| **List** | 순서 있는 목록 | 최근 기록, 메시지 큐 |
| **Set** | 중복 없는 집합 | 태그, 발급 목록 |
| **Sorted Set** | 스코어 정렬 | 랭킹, 대기열 |

---

## 3. Portal Universe에서의 활용

### 3.1 서비스별 Redis 사용

```
┌─────────────────────────────────────────────────────────────┐
│                        Redis                                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────────┐    │
│  │Auth Service │  │Shopping Svc │  │Notification Svc   │    │
│  ├─────────────┤  ├─────────────┤  ├───────────────────┤    │
│  │• Token 저장 │  │• 분산 락    │  │• Pub/Sub 알림     │    │
│  │• Blacklist  │  │• 쿠폰 발급  │  │                   │    │
│  │             │  │• 타임딜 재고│  │                   │    │
│  │             │  │• 대기열     │  │                   │    │
│  │             │  │• 검색어     │  │                   │    │
│  └─────────────┘  └─────────────┘  └───────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 주요 키 패턴

| 서비스 | 키 패턴 | 데이터 구조 | 용도 |
|--------|---------|------------|------|
| Auth | `refresh_token:{userId}` | String | Refresh Token |
| Auth | `blacklist:{accessToken}` | String | 로그아웃 토큰 |
| Shopping | `coupon:stock:{couponId}` | String | 쿠폰 재고 |
| Shopping | `coupon:issued:{couponId}` | Set | 발급 사용자 |
| Shopping | `timedeal:stock:{dealId}:{productId}` | String | 타임딜 재고 |
| Shopping | `queue:waiting:{type}:{id}` | Sorted Set | 대기열 |
| Shopping | `search:popular` | Sorted Set | 인기 검색어 |
| Shopping | `search:recent:{userId}` | List | 최근 검색어 |

---

## 4. Spring 연동 기본

### 4.1 의존성

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.redisson:redisson-spring-boot-starter:3.23.0'  // 분산 락용
}
```

### 4.2 RedisTemplate 설정

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
```

### 4.3 기본 사용법

```java
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    // 저장
    public void saveRefreshToken(String userId, String token, long ttlSeconds) {
        String key = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS);
    }

    // 조회
    public String getRefreshToken(String userId) {
        String key = "refresh_token:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    // 삭제
    public void deleteRefreshToken(String userId) {
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);
    }
}
```

---

## 5. TTL (Time To Live)

데이터 자동 만료 기능입니다.

```java
// 30분 후 자동 삭제
redisTemplate.opsForValue()
    .set("session:123", sessionData, 30, TimeUnit.MINUTES);

// 기존 키에 TTL 설정
redisTemplate.expire("temp:data", 1, TimeUnit.HOURS);

// TTL 확인
Long ttl = redisTemplate.getExpire("session:123");  // 남은 초
```

**활용 예:**
- Access Token 만료: 30분
- Refresh Token 만료: 7일
- 임시 인증 코드: 5분

---

## 6. Pub/Sub 메시징

### 실시간 브로드캐스트

```
┌─────────────┐         ┌─────────────┐
│  Publisher  │         │ Subscriber1 │
│ (Service A) │────────▶│ (Instance 1)│
└─────────────┘    │    └─────────────┘
                   │
      Channel:     │    ┌─────────────┐
  notification:123 │───▶│ Subscriber2 │
                        │ (Instance 2)│
                        └─────────────┘
```

```java
// 발행
redisTemplate.convertAndSend("notification:" + userId, message);

// 구독 (설정)
@Bean
public RedisMessageListenerContainer container(
        RedisConnectionFactory factory) {

    RedisMessageListenerContainer container =
        new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);
    container.addMessageListener(
        subscriber,
        new PatternTopic("notification:*")
    );
    return container;
}
```

**Portal Universe 사용:**
- 알림 실시간 푸시 (다중 인스턴스 환경)
- 재고 변동 실시간 동기화

---

## 7. Redis 사용 시 고려사항

### 7.1 메모리 관리

| 설정 | 설명 |
|------|------|
| `maxmemory` | 최대 메모리 제한 |
| `maxmemory-policy` | 메모리 초과 시 정책 |

**정책 옵션:**
- `noeviction`: 에러 반환 (기본)
- `allkeys-lru`: LRU 알고리즘으로 삭제
- `volatile-ttl`: TTL 짧은 키 먼저 삭제

### 7.2 영속성 옵션

| 방식 | 설명 | 성능 | 내구성 |
|------|------|------|--------|
| **없음** | 메모리만 | 최고 | 낮음 |
| **RDB** | 주기적 스냅샷 | 좋음 | 중간 |
| **AOF** | 모든 쓰기 로그 | 보통 | 높음 |
| **RDB+AOF** | 둘 다 사용 | 보통 | 최고 |

### 7.3 클러스터 vs 단일 노드

```
[Portal Universe - 개발 환경]
단일 Redis 노드 사용

[프로덕션 권장]
Redis Cluster 또는 Sentinel
- 고가용성
- 자동 장애 복구
- 수평 확장
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **In-Memory** | 메모리 기반 초고속 저장소 |
| **데이터 구조** | String, Hash, List, Set, Sorted Set |
| **TTL** | 데이터 자동 만료 |
| **Pub/Sub** | 실시간 메시지 브로드캐스트 |
| **Lua Script** | 원자적 복합 연산 |

---

## 다음 학습

- [Redis 데이터 구조 심화](./redis-data-structures.md)
- [Redis 분산 락](./redis-distributed-lock.md)
- [Portal Universe Redis 적용](./redis-portal-universe.md)

---

## 참고 자료

- [Redis 공식 문서](https://redis.io/documentation)
- [Redis 명령어 레퍼런스](https://redis.io/commands/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
