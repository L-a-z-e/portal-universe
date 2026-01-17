# PRD-002: 동시성 / 대용량 트래픽 처리

## 1. 개요

### 1.1 목적
대규모 동시 접속 상황에서 안정적인 서비스를 제공하기 위한 동시성 제어 및 트래픽 관리 시스템 구현

### 1.2 배경
- Phase 1에서 구현한 이커머스 기능은 일반적인 트래픽에서는 문제없이 동작
- 그러나 선착순 이벤트, 타임딜 등 순간적인 트래픽 폭증 상황에서는 한계 존재
- Redis 기반 분산 락과 대기열 시스템으로 이를 해결

### 1.3 범위
| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 선착순 쿠폰 | N명 한정 쿠폰 발급 | P0 |
| 타임딜 | 시간/수량 한정 특가 판매 | P0 |
| 대기열 | 트래픽 제어, 순차 입장 | P1 |

---

## 2. 기술 스택

### 2.1 신규 인프라
| 기술 | 버전 | 용도 |
|------|------|------|
| Redis | 7.x | 분산 락, 카운터, 대기열 |
| Redisson | 3.x | Redis 클라이언트 (분산 락) |

### 2.2 기존 인프라 활용
- Kafka: 이벤트 발행 (쿠폰 발급 완료, 타임딜 종료 등)
- MySQL: 영속 데이터 저장
- Spring Boot 3.5.5

---

## 3. 선착순 쿠폰 발급

### 3.1 요구사항

#### 기능 요구사항
- 관리자가 N개 한정 쿠폰을 생성할 수 있다
- 사용자는 선착순으로 쿠폰을 발급받을 수 있다
- 동일 사용자의 중복 발급을 방지한다
- 정확히 N개만 발급되어야 한다 (초과 발급 방지)

#### 비기능 요구사항
- 10,000 TPS 이상 처리 가능
- 99.9% 정확도 (Race Condition 방지)
- 발급 응답 시간 < 100ms

### 3.2 도메인 설계

```
coupon/
├── domain/
│   ├── Coupon.java              # 쿠폰 정의
│   ├── CouponIssue.java         # 발급 기록
│   ├── CouponStatus.java        # ACTIVE, EXHAUSTED, EXPIRED
│   └── CouponType.java          # FIXED_AMOUNT, PERCENTAGE, FREE_SHIPPING
├── repository/
│   ├── CouponRepository.java
│   └── CouponIssueRepository.java
├── service/
│   ├── CouponService.java
│   ├── CouponIssueService.java
│   └── CouponRedisService.java
├── controller/
│   └── CouponController.java
└── dto/
    ├── CouponCreateRequest.java
    ├── CouponResponse.java
    └── CouponIssueResponse.java
```

### 3.3 엔티티 설계

#### Coupon (쿠폰 정의)
```java
@Entity
public class Coupon {
    @Id @GeneratedValue
    private Long id;

    private String code;                    // 쿠폰 코드 (SUMMER2026)
    private String name;                    // 쿠폰명

    @Enumerated(EnumType.STRING)
    private CouponType type;                // FIXED_AMOUNT, PERCENTAGE

    private BigDecimal discountValue;       // 할인 금액/비율
    private BigDecimal minOrderAmount;      // 최소 주문 금액
    private BigDecimal maxDiscountAmount;   // 최대 할인 금액 (비율일 경우)

    private Integer totalQuantity;          // 총 발급 가능 수량
    private Integer issuedQuantity;         // 발급된 수량 (DB 기준)

    @Enumerated(EnumType.STRING)
    private CouponStatus status;            // ACTIVE, EXHAUSTED, EXPIRED

    private LocalDateTime startAt;          // 발급 시작 시간
    private LocalDateTime endAt;            // 발급 종료 시간
    private LocalDateTime expireAt;         // 사용 만료 시간

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### CouponIssue (발급 기록)
```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"coupon_id", "user_id"})
})
public class CouponIssue {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    private Coupon coupon;

    private Long userId;
    private boolean used;
    private Long usedOrderId;               // 사용된 주문 ID

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
```

### 3.4 Redis 키 설계

```
# 쿠폰별 발급 카운터 (정확한 수량 제어)
coupon:count:{couponId}                    → Integer (발급된 수량)

# 쿠폰별 발급 사용자 Set (중복 발급 방지)
coupon:issued:{couponId}                   → Set<userId>

# 쿠폰 메타 정보 캐시
coupon:meta:{couponId}                     → Hash {totalQty, startAt, endAt, status}
```

### 3.5 Lua Script (원자적 발급)

```lua
-- coupon_issue.lua
-- KEYS[1]: coupon:count:{couponId}
-- KEYS[2]: coupon:issued:{couponId}
-- ARGV[1]: userId
-- ARGV[2]: totalQuantity

-- 1. 이미 발급받은 사용자인지 확인
local alreadyIssued = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if alreadyIssued == 1 then
    return -1  -- 이미 발급됨
end

-- 2. 현재 발급 수량 확인
local currentCount = tonumber(redis.call('GET', KEYS[1]) or '0')
local totalQuantity = tonumber(ARGV[2])

if currentCount >= totalQuantity then
    return -2  -- 수량 소진
end

-- 3. 발급 처리 (원자적)
redis.call('INCR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

return currentCount + 1  -- 발급 성공, 발급 순번 반환
```

### 3.6 서비스 로직

```java
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final CouponIssueRepository issueRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final RedisScript<Long> ISSUE_SCRIPT = ...;

    @Transactional
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {
        // 1. Redis에서 원자적 발급 시도
        Long result = redisTemplate.execute(
            ISSUE_SCRIPT,
            List.of(countKey(couponId), issuedKey(couponId)),
            userId.toString(),
            getTotalQuantity(couponId).toString()
        );

        if (result == -1) {
            throw new CustomBusinessException(CouponErrorCode.ALREADY_ISSUED);
        }
        if (result == -2) {
            throw new CustomBusinessException(CouponErrorCode.COUPON_EXHAUSTED);
        }

        // 2. DB에 발급 기록 저장 (비동기 or 동기)
        CouponIssue issue = CouponIssue.create(couponId, userId);
        issueRepository.save(issue);

        // 3. 이벤트 발행
        kafkaTemplate.send("coupon.issued", new CouponIssuedEvent(couponId, userId, result));

        // 4. 수량 소진 시 상태 업데이트
        if (result.equals(getTotalQuantity(couponId).longValue())) {
            updateCouponStatus(couponId, CouponStatus.EXHAUSTED);
        }

        return CouponIssueResponse.success(result.intValue());
    }
}
```

### 3.7 API 설계

```
# 쿠폰 관리 (Admin)
POST   /api/shopping/admin/coupons              # 쿠폰 생성
GET    /api/shopping/admin/coupons              # 쿠폰 목록
GET    /api/shopping/admin/coupons/{id}         # 쿠폰 상세
PUT    /api/shopping/admin/coupons/{id}         # 쿠폰 수정
DELETE /api/shopping/admin/coupons/{id}         # 쿠폰 삭제

# 쿠폰 발급 (User)
POST   /api/shopping/coupons/{couponId}/issue   # 쿠폰 발급
GET    /api/shopping/coupons/my                 # 내 쿠폰 목록
GET    /api/shopping/coupons/available          # 발급 가능 쿠폰 목록
```

### 3.8 에러 코드

```java
public enum CouponErrorCode implements ErrorCode {
    COUPON_NOT_FOUND(NOT_FOUND, "S601", "쿠폰을 찾을 수 없습니다"),
    COUPON_EXHAUSTED(CONFLICT, "S602", "쿠폰이 모두 소진되었습니다"),
    ALREADY_ISSUED(CONFLICT, "S603", "이미 발급받은 쿠폰입니다"),
    COUPON_NOT_STARTED(BAD_REQUEST, "S604", "쿠폰 발급 기간이 아닙니다"),
    COUPON_EXPIRED(BAD_REQUEST, "S605", "쿠폰 발급 기간이 종료되었습니다"),
    COUPON_NOT_USABLE(BAD_REQUEST, "S606", "사용할 수 없는 쿠폰입니다"),
    MIN_ORDER_AMOUNT_NOT_MET(BAD_REQUEST, "S607", "최소 주문 금액을 충족하지 않습니다");
}
```

---

## 4. 타임딜 / 플래시세일

### 4.1 요구사항

#### 기능 요구사항
- 특정 시간에만 할인가로 구매 가능
- 수량 한정 (예: 100개 한정 50% 할인)
- 1인당 구매 수량 제한
- 타임딜 시작/종료 자동 처리

#### 비기능 요구사항
- 정확한 시간에 시작/종료
- 수량 초과 판매 방지
- 동시 접속 10,000명 이상 처리

### 4.2 도메인 설계

```
timedeal/
├── domain/
│   ├── TimeDeal.java            # 타임딜 정의
│   ├── TimeDealItem.java        # 타임딜 상품
│   ├── TimeDealStatus.java      # SCHEDULED, ACTIVE, ENDED, SOLD_OUT
│   └── TimeDealPurchase.java    # 구매 기록
├── repository/
│   ├── TimeDealRepository.java
│   └── TimeDealPurchaseRepository.java
├── service/
│   ├── TimeDealService.java
│   └── TimeDealScheduler.java   # 시작/종료 스케줄러
├── controller/
│   └── TimeDealController.java
└── dto/
    └── ...
```

### 4.3 엔티티 설계

#### TimeDeal
```java
@Entity
public class TimeDeal {
    @Id @GeneratedValue
    private Long id;

    private String title;                   // 타임딜 제목
    private String description;

    @ManyToOne(fetch = LAZY)
    private Product product;                // 대상 상품

    private BigDecimal originalPrice;       // 원가
    private BigDecimal dealPrice;           // 할인가
    private Integer discountPercent;        // 할인율

    private Integer totalQuantity;          // 타임딜 수량
    private Integer soldQuantity;           // 판매된 수량
    private Integer maxPerUser;             // 1인당 최대 구매 수량

    @Enumerated(EnumType.STRING)
    private TimeDealStatus status;

    private LocalDateTime startAt;          // 시작 시간
    private LocalDateTime endAt;            // 종료 시간

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4.4 Redis 키 설계

```
# 타임딜 재고 (실시간)
timedeal:stock:{dealId}                    → Integer (남은 수량)

# 타임딜별 사용자 구매 수량
timedeal:purchased:{dealId}:{userId}       → Integer (구매 수량)

# 활성 타임딜 목록 (빠른 조회)
timedeal:active                            → Set<dealId>

# 타임딜 메타 정보
timedeal:meta:{dealId}                     → Hash {startAt, endAt, maxPerUser, ...}
```

### 4.5 Lua Script (타임딜 구매)

```lua
-- timedeal_purchase.lua
-- KEYS[1]: timedeal:stock:{dealId}
-- KEYS[2]: timedeal:purchased:{dealId}:{userId}
-- ARGV[1]: quantity (구매 수량)
-- ARGV[2]: maxPerUser (1인당 최대)

local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
local purchased = tonumber(redis.call('GET', KEYS[2]) or '0')
local quantity = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 1인당 제한 확인
if purchased + quantity > maxPerUser then
    return -1  -- 1인당 제한 초과
end

-- 재고 확인
if stock < quantity then
    return -2  -- 재고 부족
end

-- 구매 처리 (원자적)
redis.call('DECRBY', KEYS[1], quantity)
redis.call('INCRBY', KEYS[2], quantity)

return stock - quantity  -- 남은 재고 반환
```

### 4.6 스케줄러 (시작/종료 자동화)

```java
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {

    private final TimeDealRepository timeDealRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 1분마다 실행
    @Scheduled(cron = "0 * * * * *")
    public void processTimeDeals() {
        LocalDateTime now = LocalDateTime.now();

        // 시작해야 할 타임딜 활성화
        List<TimeDeal> toStart = timeDealRepository
            .findByStatusAndStartAtBefore(SCHEDULED, now);

        for (TimeDeal deal : toStart) {
            activateTimeDeal(deal);
        }

        // 종료해야 할 타임딜 비활성화
        List<TimeDeal> toEnd = timeDealRepository
            .findByStatusAndEndAtBefore(ACTIVE, now);

        for (TimeDeal deal : toEnd) {
            deactivateTimeDeal(deal);
        }
    }

    private void activateTimeDeal(TimeDeal deal) {
        // Redis에 재고 설정
        redisTemplate.opsForValue().set(
            stockKey(deal.getId()),
            String.valueOf(deal.getTotalQuantity())
        );

        // 활성 목록에 추가
        redisTemplate.opsForSet().add("timedeal:active", deal.getId().toString());

        // DB 상태 업데이트
        deal.activate();
        timeDealRepository.save(deal);
    }
}
```

### 4.7 API 설계

```
# 타임딜 관리 (Admin)
POST   /api/shopping/admin/timedeals           # 타임딜 생성
GET    /api/shopping/admin/timedeals           # 타임딜 목록
PUT    /api/shopping/admin/timedeals/{id}      # 타임딜 수정

# 타임딜 조회/구매 (User)
GET    /api/shopping/timedeals                 # 활성 타임딜 목록
GET    /api/shopping/timedeals/{id}            # 타임딜 상세
POST   /api/shopping/timedeals/{id}/purchase   # 타임딜 구매
```

---

## 5. 대기열 시스템

### 5.1 요구사항

#### 기능 요구사항
- 대기열 진입 시 대기 순번 발급
- 순차적으로 입장 허용
- 대기 중 예상 대기 시간 표시
- 입장 후 일정 시간 내 미구매 시 자동 만료

#### 비기능 요구사항
- 100,000명 이상 대기 처리
- 공정한 선착순 보장
- 대기열 이탈/재진입 처리

### 5.2 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        대기열 흐름                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   [사용자] ──► [대기열 진입] ──► [대기 중] ──► [입장 허용]      │
│                    │              │              │          │
│                    ▼              ▼              ▼          │
│              Redis Sorted    폴링/SSE       입장 토큰        │
│              Set (순번)     (순번 확인)      발급            │
│                                                             │
│   [입장 토큰] ──► [구매 페이지] ──► [구매 완료 or 만료]        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 도메인 설계

```
queue/
├── domain/
│   ├── WaitingQueue.java        # 대기열 정의
│   ├── QueueEntry.java          # 대기열 엔트리
│   └── EntryToken.java          # 입장 토큰
├── service/
│   ├── QueueService.java
│   ├── QueueScheduler.java      # 입장 처리 스케줄러
│   └── QueueTokenService.java
├── controller/
│   └── QueueController.java
└── dto/
    ├── QueueEntryResponse.java  # 대기 순번, 예상 시간
    └── QueueStatusResponse.java
```

### 5.4 Redis 키 설계

```
# 대기열 (Sorted Set - score: 진입 시간)
queue:waiting:{eventId}                    → SortedSet<userId, timestamp>

# 입장 허용된 사용자 (Set)
queue:allowed:{eventId}                    → Set<userId>

# 입장 토큰 (String with TTL)
queue:token:{eventId}:{userId}             → token (TTL: 10분)

# 대기열 설정
queue:config:{eventId}                     → Hash {maxAllowed, allowRate, ...}

# 현재 활성 사용자 수
queue:active:{eventId}                     → Integer
```

### 5.5 대기열 진입/조회

```java
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;

    public QueueEntryResponse enterQueue(String eventId, Long userId) {
        String waitingKey = "queue:waiting:" + eventId;
        double score = System.currentTimeMillis();

        // 대기열에 추가
        redisTemplate.opsForZSet().add(waitingKey, userId.toString(), score);

        // 순번 조회
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, userId.toString());

        // 예상 대기 시간 계산
        int estimatedSeconds = calculateEstimatedWait(eventId, rank);

        return QueueEntryResponse.of(rank + 1, estimatedSeconds);
    }

    public QueueStatusResponse getQueueStatus(String eventId, Long userId) {
        String waitingKey = "queue:waiting:" + eventId;
        String allowedKey = "queue:allowed:" + eventId;

        // 이미 입장 허용되었는지 확인
        if (redisTemplate.opsForSet().isMember(allowedKey, userId.toString())) {
            String token = getOrCreateToken(eventId, userId);
            return QueueStatusResponse.allowed(token);
        }

        // 대기열에서 순번 확인
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, userId.toString());
        if (rank == null) {
            return QueueStatusResponse.notInQueue();
        }

        int estimatedSeconds = calculateEstimatedWait(eventId, rank);
        return QueueStatusResponse.waiting(rank + 1, estimatedSeconds);
    }
}
```

### 5.6 입장 처리 스케줄러

```java
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    // 5초마다 실행
    @Scheduled(fixedRate = 5000)
    public void processQueue() {
        List<String> activeEvents = getActiveEvents();

        for (String eventId : activeEvents) {
            processEventQueue(eventId);
        }
    }

    private void processEventQueue(String eventId) {
        String waitingKey = "queue:waiting:" + eventId;
        String allowedKey = "queue:allowed:" + eventId;
        String activeKey = "queue:active:" + eventId;

        // 현재 활성 사용자 수
        int currentActive = getActiveCount(eventId);
        int maxAllowed = getMaxAllowed(eventId);
        int allowRate = getAllowRate(eventId);  // 한 번에 허용할 인원

        // 여유 공간만큼 입장 허용
        int toAllow = Math.min(allowRate, maxAllowed - currentActive);
        if (toAllow <= 0) return;

        // 대기열에서 상위 N명 추출
        Set<String> topUsers = redisTemplate.opsForZSet()
            .range(waitingKey, 0, toAllow - 1);

        if (topUsers == null || topUsers.isEmpty()) return;

        // 입장 허용 처리
        for (String userId : topUsers) {
            // 대기열에서 제거
            redisTemplate.opsForZSet().remove(waitingKey, userId);
            // 허용 목록에 추가
            redisTemplate.opsForSet().add(allowedKey, userId);
            // 활성 카운트 증가
            redisTemplate.opsForValue().increment(activeKey);
            // 입장 토큰 생성 (10분 TTL)
            createEntryToken(eventId, userId);
        }
    }
}
```

### 5.7 입장 토큰 검증 (Guard)

```java
@Component
@RequiredArgsConstructor
public class QueueGuard {

    private final RedisTemplate<String, String> redisTemplate;

    public void validateEntry(String eventId, Long userId, String token) {
        String tokenKey = "queue:token:" + eventId + ":" + userId;
        String storedToken = redisTemplate.opsForValue().get(tokenKey);

        if (storedToken == null) {
            throw new CustomBusinessException(QueueErrorCode.TOKEN_EXPIRED);
        }

        if (!storedToken.equals(token)) {
            throw new CustomBusinessException(QueueErrorCode.INVALID_TOKEN);
        }
    }

    public void completeEntry(String eventId, Long userId) {
        String tokenKey = "queue:token:" + eventId + ":" + userId;
        String allowedKey = "queue:allowed:" + eventId;
        String activeKey = "queue:active:" + eventId;

        // 토큰 삭제
        redisTemplate.delete(tokenKey);
        // 허용 목록에서 제거
        redisTemplate.opsForSet().remove(allowedKey, userId.toString());
        // 활성 카운트 감소
        redisTemplate.opsForValue().decrement(activeKey);
    }
}
```

### 5.8 API 설계

```
# 대기열
POST   /api/shopping/queue/{eventId}/enter     # 대기열 진입
GET    /api/shopping/queue/{eventId}/status    # 대기 상태 조회 (폴링)
DELETE /api/shopping/queue/{eventId}/leave     # 대기열 이탈

# SSE (실시간 상태)
GET    /api/shopping/queue/{eventId}/subscribe # SSE 구독
```

### 5.9 프론트엔드 연동 (SSE)

```java
@RestController
@RequestMapping("/api/shopping/queue")
public class QueueController {

    @GetMapping(value = "/{eventId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<QueueStatusResponse>> subscribe(
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        return Flux.interval(Duration.ofSeconds(3))
            .map(seq -> {
                QueueStatusResponse status = queueService.getQueueStatus(eventId, userId);
                return ServerSentEvent.<QueueStatusResponse>builder()
                    .id(String.valueOf(seq))
                    .event("queue-status")
                    .data(status)
                    .build();
            })
            .takeUntil(sse -> sse.data().isAllowed());
    }
}
```

---

## 6. 인프라 설정

### 6.1 Redis 설정 (docker-compose.yml)

```yaml
redis:
  image: redis:7-alpine
  container_name: redis
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
  command: redis-server --appendonly yes
  networks:
    - portal-universe-net
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### 6.2 Spring Boot 설정

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 10
          min-idle: 5
```

### 6.3 Redisson 설정 (분산 락용)

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
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectionPoolSize(50)
            .setConnectionMinimumIdleSize(10);
        return Redisson.create(config);
    }
}
```

---

## 7. 에러 코드 (Phase 2)

```java
// 쿠폰 관련 (S6XX)
COUPON_NOT_FOUND(NOT_FOUND, "S601", "쿠폰을 찾을 수 없습니다"),
COUPON_EXHAUSTED(CONFLICT, "S602", "쿠폰이 모두 소진되었습니다"),
ALREADY_ISSUED(CONFLICT, "S603", "이미 발급받은 쿠폰입니다"),
COUPON_NOT_STARTED(BAD_REQUEST, "S604", "쿠폰 발급 기간이 아닙니다"),
COUPON_EXPIRED(BAD_REQUEST, "S605", "쿠폰 발급 기간이 종료되었습니다"),

// 타임딜 관련 (S7XX)
TIMEDEAL_NOT_FOUND(NOT_FOUND, "S701", "타임딜을 찾을 수 없습니다"),
TIMEDEAL_NOT_ACTIVE(BAD_REQUEST, "S702", "진행 중인 타임딜이 아닙니다"),
TIMEDEAL_SOLD_OUT(CONFLICT, "S703", "타임딜 상품이 품절되었습니다"),
TIMEDEAL_LIMIT_EXCEEDED(BAD_REQUEST, "S704", "1인당 구매 수량을 초과했습니다"),

// 대기열 관련 (S8XX)
QUEUE_NOT_FOUND(NOT_FOUND, "S801", "대기열을 찾을 수 없습니다"),
NOT_IN_QUEUE(BAD_REQUEST, "S802", "대기열에 등록되지 않았습니다"),
TOKEN_EXPIRED(UNAUTHORIZED, "S803", "입장 토큰이 만료되었습니다"),
INVALID_TOKEN(UNAUTHORIZED, "S804", "유효하지 않은 입장 토큰입니다"),
QUEUE_CLOSED(BAD_REQUEST, "S805", "대기열이 마감되었습니다"),
```

---

## 8. 데이터베이스 마이그레이션

### 8.1 쿠폰 테이블

```sql
-- V9__Create_coupon_tables.sql

CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    max_discount_amount DECIMAL(10,2),
    total_quantity INT NOT NULL,
    issued_quantity INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    expire_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupons_status (status),
    INDEX idx_coupons_start_end (start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_order_id BIGINT,
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    used_at DATETIME,
    UNIQUE KEY uk_coupon_user (coupon_id, user_id),
    INDEX idx_issues_user (user_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.2 타임딜 테이블

```sql
-- V10__Create_timedeal_tables.sql

CREATE TABLE time_deals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    product_id BIGINT NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    deal_price DECIMAL(10,2) NOT NULL,
    discount_percent INT,
    total_quantity INT NOT NULL,
    sold_quantity INT DEFAULT 0,
    max_per_user INT DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_timedeals_status (status),
    INDEX idx_timedeals_start (start_at),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE time_deal_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_deal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    order_id BIGINT,
    purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchases_deal_user (time_deal_id, user_id),
    FOREIGN KEY (time_deal_id) REFERENCES time_deals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 9. 테스트 계획

### 9.1 단위 테스트
- Lua Script 로직 테스트 (Embedded Redis)
- 서비스 로직 테스트
- 동시성 시나리오 테스트

### 9.2 통합 테스트 (TestContainers)
```java
@Test
void 선착순_쿠폰_100개_동시_발급_테스트() {
    // given: 100개 한정 쿠폰
    // when: 200명이 동시에 발급 요청
    // then: 정확히 100명만 성공, 100명 실패
}

@Test
void 타임딜_재고_동시성_테스트() {
    // given: 50개 한정 타임딜
    // when: 100명이 동시에 1개씩 구매 시도
    // then: 정확히 50명만 성공
}
```

### 9.3 부하 테스트 (k6)
```javascript
// k6 script
export default function() {
    const res = http.post(
        'http://localhost:8080/api/shopping/coupons/1/issue',
        null,
        { headers: { 'Authorization': `Bearer ${token}` } }
    );
    check(res, { 'status is 200 or 409': (r) => r.status === 200 || r.status === 409 });
}

export const options = {
    vus: 1000,        // 동시 사용자
    duration: '30s',  // 테스트 시간
};
```

---

## 10. 구현 순서

| 순서 | 작업 | 예상 기간 |
|------|------|----------|
| 1 | Redis 인프라 설정 (docker-compose, config) | 0.5일 |
| 2 | 쿠폰 도메인 구현 | 2일 |
| 3 | 쿠폰 Lua Script + 서비스 | 1일 |
| 4 | 쿠폰 API + 테스트 | 1일 |
| 5 | 타임딜 도메인 구현 | 1.5일 |
| 6 | 타임딜 스케줄러 + 서비스 | 1일 |
| 7 | 타임딜 API + 테스트 | 1일 |
| 8 | 대기열 도메인 구현 | 1.5일 |
| 9 | 대기열 스케줄러 + SSE | 1.5일 |
| 10 | 대기열 API + 테스트 | 1일 |
| 11 | 통합 테스트 + 부하 테스트 | 2일 |
| **총계** | | **~14일** |

---

## 11. 참고 자료

- [Redis Lua Scripting](https://redis.io/docs/manual/programmability/eval-intro/)
- [Redisson Distributed Locks](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)
- [Spring WebFlux SSE](https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-methods/return-types.html)
- [k6 Load Testing](https://k6.io/docs/)
