# Redis Data Structures

Redis의 핵심 자료구조를 이해하고 각 상황에 맞는 최적의 자료구조를 선택하는 방법을 학습합니다.

## 목차

1. [String](#1-string)
2. [Hash](#2-hash)
3. [List](#3-list)
4. [Set](#4-set)
5. [Sorted Set (ZSet)](#5-sorted-set-zset)
6. [HyperLogLog](#6-hyperloglog)
7. [Streams](#7-streams)
8. [자료구조 선택 가이드](#8-자료구조-선택-가이드)

---

## 1. String

가장 기본적인 자료구조로, 최대 512MB까지 저장 가능합니다.

### 특징

```
+------------------+
|     String       |
+------------------+
| Key -> Value     |
| "user:1:name"    |
|       |          |
|       v          |
|    "John"        |
+------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `SET key value` | 값 설정 | O(1) |
| `GET key` | 값 조회 | O(1) |
| `INCR key` | 값 1 증가 | O(1) |
| `DECR key` | 값 1 감소 | O(1) |
| `SETNX key value` | 키 없을 때만 설정 | O(1) |
| `SETEX key seconds value` | TTL과 함께 설정 | O(1) |
| `MSET k1 v1 k2 v2` | 다중 설정 | O(N) |
| `MGET k1 k2` | 다중 조회 | O(N) |

### 사용 사례

```java
// 1. 간단한 캐싱
redisTemplate.opsForValue().set("user:1:profile", userProfile);
redisTemplate.opsForValue().set("user:1:profile", userProfile, 1, TimeUnit.HOURS);

// 2. 카운터 (조회수, 좋아요)
redisTemplate.opsForValue().increment("post:123:views");
redisTemplate.opsForValue().increment("post:123:likes");

// 3. 분산 락 (간단한 버전)
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent("lock:order:123", "locked", 10, TimeUnit.SECONDS);

// 4. Rate Limiting 기본
String key = "rate:" + userId + ":" + currentMinute;
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, 60, TimeUnit.SECONDS);
}
```

### 메모리 최적화

```
# 작은 정수는 공유 객체 사용
SET counter 1    # 메모리 효율적

# 큰 문자열 압축 고려
SET data [gzip compressed data]
```

---

## 2. Hash

필드-값 쌍의 컬렉션으로, 객체 저장에 적합합니다.

### 구조

```
+--------------------------------+
|           Hash                 |
+--------------------------------+
| Key: "user:1"                  |
|   +----------+----------+      |
|   | Field    | Value    |      |
|   +----------+----------+      |
|   | name     | John     |      |
|   | email    | j@ex.com |      |
|   | age      | 30       |      |
|   +----------+----------+      |
+--------------------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `HSET key field value` | 필드 설정 | O(1) |
| `HGET key field` | 필드 조회 | O(1) |
| `HMSET key f1 v1 f2 v2` | 다중 필드 설정 | O(N) |
| `HMGET key f1 f2` | 다중 필드 조회 | O(N) |
| `HGETALL key` | 전체 조회 | O(N) |
| `HINCRBY key field n` | 필드 값 증가 | O(1) |
| `HDEL key field` | 필드 삭제 | O(1) |
| `HEXISTS key field` | 필드 존재 확인 | O(1) |

### 사용 사례

```java
// 1. 사용자 정보 저장
Map<String, Object> userMap = new HashMap<>();
userMap.put("name", user.getName());
userMap.put("email", user.getEmail());
userMap.put("age", String.valueOf(user.getAge()));
redisTemplate.opsForHash().putAll("user:" + userId, userMap);

// 2. 부분 업데이트
redisTemplate.opsForHash().put("user:" + userId, "lastLogin", now);

// 3. 장바구니
redisTemplate.opsForHash().put("cart:" + userId, productId, quantity);
redisTemplate.opsForHash().increment("cart:" + userId, productId, 1);

// 4. 세션 데이터
Map<String, Object> sessionData = new HashMap<>();
sessionData.put("userId", "123");
sessionData.put("role", "ADMIN");
sessionData.put("loginTime", System.currentTimeMillis());
redisTemplate.opsForHash().putAll("session:" + sessionId, sessionData);
```

### String vs Hash 선택 기준

```
+------------------+------------------+------------------+
| 기준             | String           | Hash             |
+------------------+------------------+------------------+
| 전체 읽기/쓰기   | 더 빠름          | 약간 느림        |
| 부분 업데이트    | 전체 교체 필요   | 필드별 가능      |
| 메모리 (작은 값) | 오버헤드 있음    | 효율적           |
| 직렬화           | 필요             | 불필요           |
+------------------+------------------+------------------+
```

---

## 3. List

순서가 있는 문자열 컬렉션으로, 양쪽 끝에서 push/pop이 가능합니다.

### 구조

```
+-------------------------------------------+
|                  List                      |
+-------------------------------------------+
| Key: "queue:orders"                        |
|                                           |
| HEAD                              TAIL    |
|   |                                 |     |
|   v                                 v     |
| [order1] <-> [order2] <-> [order3]        |
|                                           |
| LPUSH ->                      <- RPUSH   |
| LPOP  <-                      -> RPOP    |
+-------------------------------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `LPUSH key value` | 왼쪽 삽입 | O(1) |
| `RPUSH key value` | 오른쪽 삽입 | O(1) |
| `LPOP key` | 왼쪽 제거 | O(1) |
| `RPOP key` | 오른쪽 제거 | O(1) |
| `LRANGE key start stop` | 범위 조회 | O(N) |
| `LLEN key` | 길이 조회 | O(1) |
| `LINDEX key index` | 인덱스로 조회 | O(N) |
| `BLPOP key timeout` | 블로킹 왼쪽 제거 | O(1) |
| `BRPOP key timeout` | 블로킹 오른쪽 제거 | O(1) |

### 사용 사례

```java
// 1. 메시지 큐 (Producer)
redisTemplate.opsForList().rightPush("queue:notifications", notification);

// 2. 메시지 큐 (Consumer)
Object message = redisTemplate.opsForList().leftPop("queue:notifications");

// 3. 최근 활동 로그 (고정 크기)
redisTemplate.opsForList().leftPush("user:" + userId + ":activities", activity);
redisTemplate.opsForList().trim("user:" + userId + ":activities", 0, 99); // 최근 100개만

// 4. 타임라인/피드
redisTemplate.opsForList().leftPush("timeline:" + userId, postId);
List<Object> recentPosts = redisTemplate.opsForList()
    .range("timeline:" + userId, 0, 19); // 최근 20개

// 5. 블로킹 큐 (작업 처리)
Object task = redisTemplate.opsForList()
    .leftPop("queue:tasks", 30, TimeUnit.SECONDS);
```

### Stack vs Queue 패턴

```
Stack (LIFO):
  LPUSH + LPOP  또는  RPUSH + RPOP

Queue (FIFO):
  LPUSH + RPOP  또는  RPUSH + LPOP

  Producer         Queue              Consumer
  +------+    +-------------+    +----------+
  | PUSH |--->| [1][2][3]   |--->| POP      |
  +------+    +-------------+    +----------+
```

---

## 4. Set

중복 없는 문자열 컬렉션입니다.

### 구조

```
+----------------------------------+
|              Set                 |
+----------------------------------+
| Key: "product:123:tags"          |
|                                  |
|   +-------+  +-------+           |
|   | sale  |  | new   |           |
|   +-------+  +-------+           |
|                                  |
|   +-------+  +-------+           |
|   | hot   |  | trend |           |
|   +-------+  +-------+           |
|                                  |
| 순서 없음, 중복 불가             |
+----------------------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `SADD key member` | 멤버 추가 | O(1) |
| `SREM key member` | 멤버 제거 | O(1) |
| `SISMEMBER key member` | 멤버 존재 확인 | O(1) |
| `SMEMBERS key` | 전체 멤버 조회 | O(N) |
| `SCARD key` | 멤버 수 조회 | O(1) |
| `SINTER key1 key2` | 교집합 | O(N*M) |
| `SUNION key1 key2` | 합집합 | O(N) |
| `SDIFF key1 key2` | 차집합 | O(N) |
| `SRANDMEMBER key count` | 랜덤 멤버 | O(N) |

### 사용 사례

```java
// 1. 태그 관리
redisTemplate.opsForSet().add("product:123:tags", "sale", "new", "hot");

// 2. 좋아요/팔로우 (중복 방지)
redisTemplate.opsForSet().add("post:123:likes", userId);
Long likeCount = redisTemplate.opsForSet().size("post:123:likes");
Boolean isLiked = redisTemplate.opsForSet().isMember("post:123:likes", userId);

// 3. 온라인 사용자 추적
redisTemplate.opsForSet().add("online:users", sessionId);
redisTemplate.opsForSet().remove("online:users", sessionId);

// 4. 친구 관계 분석
// 공통 친구 찾기
Set<Object> commonFriends = redisTemplate.opsForSet()
    .intersect("user:1:friends", "user:2:friends");

// 친구의 친구 (나만 팔로우하지 않은 사람)
Set<Object> suggestions = redisTemplate.opsForSet()
    .difference("user:2:friends", "user:1:friends");

// 5. 쿠폰 발급 중복 체크 (Portal Universe 패턴)
Boolean isIssued = redisTemplate.opsForSet()
    .isMember("coupon:" + couponId + ":issued", userId);
if (!isIssued) {
    redisTemplate.opsForSet().add("coupon:" + couponId + ":issued", userId);
}
```

### 집합 연산 시각화

```
Set A: user:1:friends     Set B: user:2:friends
     +-------+                 +-------+
     |  Bob  |                 |  Bob  |
     | Carol |                 | David |
     | Alice |                 | Alice |
     +-------+                 +-------+

SINTER (A, B) = {Bob, Alice}      # 공통 친구
SUNION (A, B) = {Bob, Carol, Alice, David}  # 모든 친구
SDIFF  (A, B) = {Carol}           # A에만 있는 친구
SDIFF  (B, A) = {David}           # B에만 있는 친구
```

---

## 5. Sorted Set (ZSet)

Score로 정렬된 중복 없는 문자열 컬렉션입니다.

### 구조

```
+----------------------------------------+
|           Sorted Set                    |
+----------------------------------------+
| Key: "leaderboard:game1"                |
|                                        |
| Score     Member                       |
| +------+  +--------+                   |
| | 1000 |--| Alice  |  <- Rank 1        |
| +------+  +--------+                   |
| | 850  |--| Bob    |  <- Rank 2        |
| +------+  +--------+                   |
| | 720  |--| Carol  |  <- Rank 3        |
| +------+  +--------+                   |
|                                        |
| Score 기준 자동 정렬                    |
+----------------------------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `ZADD key score member` | 멤버 추가/업데이트 | O(log N) |
| `ZREM key member` | 멤버 제거 | O(log N) |
| `ZSCORE key member` | Score 조회 | O(1) |
| `ZRANK key member` | 순위 조회 (오름차순) | O(log N) |
| `ZREVRANK key member` | 순위 조회 (내림차순) | O(log N) |
| `ZRANGE key start stop` | 범위 조회 (오름차순) | O(log N + M) |
| `ZREVRANGE key start stop` | 범위 조회 (내림차순) | O(log N + M) |
| `ZINCRBY key increment member` | Score 증가 | O(log N) |
| `ZCOUNT key min max` | Score 범위 내 개수 | O(log N) |
| `ZRANGEBYSCORE key min max` | Score 범위로 조회 | O(log N + M) |

### 사용 사례

```java
// 1. 리더보드
redisTemplate.opsForZSet().add("leaderboard:game1", "player123", 1000);
redisTemplate.opsForZSet().incrementScore("leaderboard:game1", "player123", 50);

// Top 10 조회
Set<ZSetOperations.TypedTuple<Object>> top10 = redisTemplate.opsForZSet()
    .reverseRangeWithScores("leaderboard:game1", 0, 9);

// 내 순위 조회
Long myRank = redisTemplate.opsForZSet()
    .reverseRank("leaderboard:game1", "player123");

// 2. 대기열 관리 (Portal Universe 패턴)
double score = System.currentTimeMillis();
redisTemplate.opsForZSet().add("queue:waiting:timedeal:1", entryToken, score);

// 대기 순번 조회
Long position = redisTemplate.opsForZSet()
    .rank("queue:waiting:timedeal:1", entryToken);

// 상위 N명 처리
Set<ZSetOperations.TypedTuple<String>> topN = redisTemplate.opsForZSet()
    .popMin("queue:waiting:timedeal:1", 10);

// 3. 시간 기반 데이터 정리
long expireTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24시간 전
redisTemplate.opsForZSet()
    .removeRangeByScore("events:recent", 0, expireTime);

// 4. 검색어 자동완성 (인기도 기반)
redisTemplate.opsForZSet().incrementScore("search:keywords", "아이폰", 1);

// 상위 10개 인기 검색어
Set<Object> popularKeywords = redisTemplate.opsForZSet()
    .reverseRange("search:keywords", 0, 9);

// 5. 예약 스케줄링
long executeTime = futureTime.toEpochMilli();
redisTemplate.opsForZSet().add("scheduler:tasks", taskId, executeTime);

// 실행할 작업 조회
Set<Object> dueTasks = redisTemplate.opsForZSet()
    .rangeByScore("scheduler:tasks", 0, System.currentTimeMillis());
```

---

## 6. HyperLogLog

대용량 고유 요소 카운팅을 위한 확률적 자료구조입니다.

### 특징

```
+------------------------------------------+
|            HyperLogLog                    |
+------------------------------------------+
| - 고유 요소 수 추정 (Cardinality)         |
| - 표준 오차: 0.81%                        |
| - 메모리: 요소 수와 관계없이 12KB          |
| - 100만 개 고유 값 → 12KB                 |
| - 10억 개 고유 값 → 12KB                  |
+------------------------------------------+

일반 Set vs HyperLogLog:
+------------------+------------------+------------------+
| 고유 요소 수     | Set 메모리       | HyperLogLog      |
+------------------+------------------+------------------+
| 1,000            | ~80KB            | 12KB             |
| 100,000          | ~8MB             | 12KB             |
| 10,000,000       | ~800MB           | 12KB             |
+------------------+------------------+------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `PFADD key element` | 요소 추가 | O(1) |
| `PFCOUNT key` | 고유 요소 수 추정 | O(1) |
| `PFMERGE dest src1 src2` | HyperLogLog 병합 | O(N) |

### 사용 사례

```java
// 1. 일일 방문자 수 (UV - Unique Visitors)
String todayKey = "uv:" + LocalDate.now();
redisTemplate.opsForHyperLogLog().add(todayKey, visitorId);

Long uniqueVisitors = redisTemplate.opsForHyperLogLog().size(todayKey);

// 2. 월간 방문자 수 (일별 병합)
String monthKey = "uv:month:" + YearMonth.now();
redisTemplate.opsForHyperLogLog().union(monthKey,
    "uv:2024-01-01", "uv:2024-01-02", /* ... */ "uv:2024-01-31");

// 3. 페이지별 고유 방문자
redisTemplate.opsForHyperLogLog().add("page:home:visitors", visitorId);
redisTemplate.opsForHyperLogLog().add("page:product:visitors", visitorId);

// 4. 검색어 고유 사용자 수
redisTemplate.opsForHyperLogLog().add("search:iphone:users", userId);
Long searchUsers = redisTemplate.opsForHyperLogLog().size("search:iphone:users");
```

### 정확도 vs 메모리 트레이드오프

```
정확한 카운팅이 필요한 경우: Set 사용
  - 좋아요 수, 투표 수
  - 중복 체크가 필요한 경우

대략적인 추정이 충분한 경우: HyperLogLog 사용
  - 일일/월간 방문자 수
  - 검색어 인기도
  - 대규모 이벤트 참여자 수
```

---

## 7. Streams

로그 형태의 append-only 자료구조로, 메시지 큐 및 이벤트 소싱에 적합합니다.

### 구조

```
+----------------------------------------------------------+
|                     Stream                                |
+----------------------------------------------------------+
| Key: "events:orders"                                      |
|                                                          |
| Entry ID              Field-Value Pairs                  |
| +------------------+  +----------------------------+     |
| | 1609459200000-0  |--| orderId: 123              |     |
| +------------------+  | action: created           |     |
|                       | userId: user1             |     |
|                       +----------------------------+     |
|                                                          |
| +------------------+  +----------------------------+     |
| | 1609459200001-0  |--| orderId: 123              |     |
| +------------------+  | action: paid              |     |
|                       | amount: 50000             |     |
|                       +----------------------------+     |
+----------------------------------------------------------+
```

### 주요 명령어

| 명령어 | 설명 | 시간복잡도 |
|--------|------|-----------|
| `XADD key * field value` | 메시지 추가 | O(1) |
| `XREAD STREAMS key id` | 메시지 읽기 | O(N) |
| `XRANGE key start end` | 범위 조회 | O(N) |
| `XLEN key` | 메시지 수 | O(1) |
| `XGROUP CREATE key group id` | 소비자 그룹 생성 | O(1) |
| `XREADGROUP GROUP group consumer STREAMS key >` | 그룹에서 읽기 | O(N) |
| `XACK key group id` | 메시지 확인 | O(1) |
| `XTRIM key MAXLEN count` | 오래된 메시지 삭제 | O(N) |

### 사용 사례

```java
// 1. 이벤트 로깅
Map<String, Object> eventData = new HashMap<>();
eventData.put("orderId", orderId);
eventData.put("action", "created");
eventData.put("timestamp", System.currentTimeMillis());

RecordId recordId = redisTemplate.opsForStream()
    .add("events:orders", eventData);

// 2. 이벤트 읽기 (특정 ID 이후)
List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
    .read(StreamOffset.fromStart("events:orders"));

// 3. Consumer Group 기반 처리
// 그룹 생성 (한 번만)
redisTemplate.opsForStream().createGroup("events:orders", "order-processors");

// 메시지 읽기 (그룹 멤버로서)
List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
    .read(Consumer.from("order-processors", "consumer-1"),
          StreamOffset.create("events:orders", ReadOffset.lastConsumed()));

// 처리 완료 후 ACK
for (MapRecord<String, Object, Object> message : messages) {
    processMessage(message);
    redisTemplate.opsForStream()
        .acknowledge("events:orders", "order-processors", message.getId());
}

// 4. 재고 변경 이벤트 스트림 (Portal Universe 패턴)
Map<String, Object> inventoryEvent = new HashMap<>();
inventoryEvent.put("productId", productId);
inventoryEvent.put("change", -1);
inventoryEvent.put("type", "DEDUCT");
inventoryEvent.put("orderId", orderId);

redisTemplate.opsForStream().add(
    StreamRecords.newRecord()
        .ofMap(inventoryEvent)
        .withStreamKey("inventory:changes")
);

// 5. 스트림 크기 제한
redisTemplate.opsForStream()
    .trim("events:orders", 10000); // 최근 10000개만 유지
```

### Streams vs List 비교

```
+------------------+------------------+------------------+
| 특성             | List             | Streams          |
+------------------+------------------+------------------+
| 메시지 ID        | 없음             | 자동 생성        |
| Consumer Groups  | 지원 안 함       | 지원             |
| 메시지 ACK       | 불가             | 가능             |
| 범위 쿼리        | 인덱스 기반      | ID/시간 기반     |
| 재처리           | 어려움           | 쉬움             |
| 사용 사례        | 간단한 큐        | 이벤트 소싱      |
+------------------+------------------+------------------+
```

---

## 8. 자료구조 선택 가이드

### 결정 트리

```
                        [데이터 특성은?]
                              |
            +-----------------+-----------------+
            |                 |                 |
      [단일 값]         [컬렉션]          [고유 개수만]
            |                 |                 |
         String               |            HyperLogLog
                              |
            +-----------------+-----------------+
            |                 |                 |
      [순서 필요?]      [Score 정렬?]     [중복 허용?]
            |                 |                 |
     +------+------+         ZSet         +-----+-----+
     |             |                      |           |
   [Yes]         [No]                   [Yes]       [No]
     |             |                      |           |
   List           |                    List         Set
                  |
          [필드별 접근?]
                  |
           +------+------+
           |             |
         [Yes]         [No]
           |             |
         Hash         String
                    (JSON 직렬화)
```

### 자료구조별 적합 사용처

| 자료구조 | 최적 사용 사례 |
|----------|---------------|
| String | 캐싱, 카운터, 분산 락, 세션 토큰 |
| Hash | 객체 저장, 장바구니, 사용자 프로필 |
| List | 큐, 스택, 최근 항목, 타임라인 |
| Set | 태그, 좋아요, 팔로우, 유니크 체크 |
| Sorted Set | 리더보드, 대기열, 스케줄링, 인기도 |
| HyperLogLog | 방문자 수, 검색어 인기도 |
| Streams | 이벤트 로그, 메시지 브로커, 활동 피드 |

### 메모리 효율성 팁

```java
// 1. 키 이름 축약
// Bad:  "application:user:profile:12345"
// Good: "u:p:12345"

// 2. Hash 사용 시 ziplist 최적화 (작은 Hash)
// redis.conf: hash-max-ziplist-entries 512
//             hash-max-ziplist-value 64

// 3. 숫자는 문자열보다 정수로
redisTemplate.opsForValue().set("count", 100);  // 효율적
redisTemplate.opsForValue().set("count", "100"); // 덜 효율적

// 4. TTL 설정으로 자동 정리
redisTemplate.expire("temp:data", 1, TimeUnit.HOURS);

// 5. 대용량 컬렉션 분산
// Bad:  "users:all" -> 1,000,000 members
// Good: "users:0" -> 10,000 members
//       "users:1" -> 10,000 members
//       ...
```

---

## 관련 문서

- [Redis Caching Patterns](./redis-caching-patterns.md)
- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Portal Universe](./redis-portal-universe.md)
