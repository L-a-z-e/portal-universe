---
id: ADR-002
title: Redis Sorted Set 기반 대기열 시스템 설계
type: adr
status: accepted
created: 2026-01-19
updated: 2026-01-19
author: Laze
decision_date: 2026-01-19
reviewers: []
tags: [queue, redis, sse, timedeal, concurrency]
related:
  - api-queue
  - api-admin-queue
  - api-timedeal
---

# ADR-002: Redis Sorted Set 기반 대기열 시스템 설계

## Context (배경)

Shopping Service의 타임딜(TimeDeal) 및 플래시 세일(Flash Sale) 이벤트는 단시간에 대량의 트래픽이 집중됩니다:

### 문제 상황
1. **동시 접속 폭증**: 이벤트 시작 시 수천 명이 동시 접속
2. **재고 부족**: 재고보다 훨씬 많은 주문 요청 발생
3. **시스템 과부하**: DB 커넥션 풀 고갈, 응답 지연
4. **사용자 경험 저하**: 무한 로딩, 에러 페이지 노출

### 기술적 제약 조건
- 단일 서비스 인스턴스로는 감당 불가
- DB 직접 접근 시 부하 집중
- 공정한 순서 보장 필요 (선착순)
- 실시간 대기 상태 업데이트 필요

### 비즈니스 제약 조건
- 사용자에게 예상 대기 시간 제공
- 대기 순번 실시간 표시
- 입장 완료 시 자동 페이지 전환
- 관리자가 대기열 제어 가능

## Decision Drivers (결정 요인)

1. **공정성**: 진입 순서대로 입장 보장 (FIFO)
2. **확장성**: 수만 명 동시 대기 처리 가능
3. **성능**: 빠른 순번 조회 및 입장 처리
4. **실시간성**: 대기 상태 실시간 업데이트
5. **관리 용이성**: 관리자가 대기열 제어 가능
6. **장애 격리**: 대기열 장애가 전체 시스템에 영향 최소화

## Considered Options (검토한 대안)

### Option 1: DB 기반 대기열 (RDBMS)

**설명**: PostgreSQL의 순번 테이블로 대기열 구현

```sql
CREATE TABLE queue_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    joined_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_event_position (event_id, position)
);
```

**장점**:
- ✅ 영속성 보장 (데이터 유실 없음)
- ✅ 트랜잭션 지원
- ✅ 기존 인프라 활용

**단점**:
- ❌ 대량 트래픽 시 DB 부하 집중
- ❌ 순번 조회 시 `COUNT(*)` 연산 느림
- ❌ 락(Lock) 경합 발생 가능
- ❌ 스케일 아웃 어려움

**결론**: ❌ 부적합 (성능 및 확장성 부족)

---

### Option 2: Redis List + Polling

**설명**: Redis LIST 자료구조를 사용하여 FIFO 큐 구현

```redis
LPUSH queue:TIMEDEAL:123 "user1"
LPUSH queue:TIMEDEAL:123 "user2"
RPOP queue:TIMEDEAL:123  # user1 입장
```

**장점**:
- ✅ 빠른 성능 (O(1) 삽입/삭제)
- ✅ FIFO 보장
- ✅ Redis 기본 기능 활용

**단점**:
- ❌ 순번 조회 어려움 (`LPOS` 사용 시 O(N))
- ❌ 특정 사용자 삭제 어려움 (`LREM` O(N))
- ❌ timestamp 기반 정렬 불가능
- ❌ 범위 조회 비효율적

**결론**: ❌ 부적합 (순번 조회 및 관리 기능 부족)

---

### Option 3: Redis Sorted Set + SSE ✅

**설명**: Redis SORTED SET을 사용하여 timestamp 기반 대기열 구현 + SSE로 실시간 업데이트

```redis
ZADD queue:TIMEDEAL:123:waiting 1737292800 "token1"
ZADD queue:TIMEDEAL:123:waiting 1737292801 "token2"
ZRANK queue:TIMEDEAL:123:waiting "token1"  # 순번 조회
ZRANGE queue:TIMEDEAL:123:waiting 0 49     # 상위 50명 조회
ZREM queue:TIMEDEAL:123:waiting "token1"   # 제거
```

**장점**:
- ✅ **빠른 순번 조회**: `ZRANK` O(log N)
- ✅ **정렬 자동 유지**: timestamp 기반 순서 보장
- ✅ **범위 조회 효율적**: `ZRANGE` O(log N + M)
- ✅ **개별 삭제 빠름**: `ZREM` O(log N)
- ✅ **실시간 업데이트**: SSE를 통한 Push 기반 업데이트
- ✅ **확장성 우수**: Redis 클러스터링 지원

**단점**:
- ⚠️ Redis 장애 시 대기열 데이터 유실 가능
  - **완화**: PostgreSQL에 `queue_entries` 테이블로 백업 저장
- ⚠️ SSE 연결 관리 필요
  - **완화**: 5분 타임아웃, 자동 재연결

**결론**: ✅ 채택 (성능, 확장성, 실시간성 모두 충족)

---

### Option 4: Kafka 기반 이벤트 큐

**설명**: Kafka Topic을 대기열로 사용

**장점**:
- ✅ 높은 처리량
- ✅ 영속성 보장
- ✅ 파티셔닝 지원

**단점**:
- ❌ 순번 조회 불가능 (Offset만 제공)
- ❌ 특정 사용자 삭제 불가
- ❌ 실시간 상태 조회 어려움
- ❌ 과도한 인프라 (대기열에는 오버스펙)

**결론**: ❌ 부적합 (대기열 특성에 맞지 않음)

## Decision (최종 결정)

**Redis Sorted Set + SSE 방식을 채택합니다.**

### 선택 이유

1. **최적의 성능**: Sorted Set의 O(log N) 연산으로 빠른 순번 조회
2. **자동 정렬**: timestamp score로 진입 순서 자동 유지
3. **효율적인 범위 조회**: `entryBatchSize`만큼 한 번에 조회 가능
4. **실시간 업데이트**: SSE를 통한 서버 Push 방식
5. **관리 용이성**: Redis CLI로 직접 확인 및 수정 가능
6. **검증된 기술**: 많은 대기열 시스템에서 사용 중

### 구현 세부사항

#### 1. Redis Sorted Set 구조

```
Key: queue:{eventType}:{eventId}:waiting
Score: joinedAt (timestamp)
Member: entryToken (UUID)
```

**예시**:
```redis
ZADD queue:TIMEDEAL:123:waiting 1737292800000 "f47ac10b-58cc-4372-a567-0e02b2c3d479"
ZADD queue:TIMEDEAL:123:waiting 1737292801000 "a1b2c3d4-1234-5678-9abc-def012345678"
```

#### 2. 주요 Redis 연산

| 연산 | 목적 | 시간 복잡도 |
|------|------|------------|
| `ZADD` | 대기열 진입 | O(log N) |
| `ZRANK` | 순번 조회 | O(log N) |
| `ZCARD` | 전체 대기 인원 | O(1) |
| `ZRANGE` | 입장 대상 조회 | O(log N + M) |
| `ZREM` | 대기열 이탈 | O(log N) |

#### 3. PostgreSQL 백업

Redis는 휘발성이므로 PostgreSQL에 엔트리 상태 저장:

```sql
CREATE TABLE queue_entries (
    id BIGSERIAL PRIMARY KEY,
    queue_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entry_token UUID UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,  -- WAITING, ENTERED, EXPIRED, LEFT
    joined_at TIMESTAMP NOT NULL,
    entered_at TIMESTAMP,
    INDEX idx_queue_user (queue_id, user_id),
    INDEX idx_token (entry_token)
);
```

#### 4. SSE 구현

```java
@GetMapping(value = "/{eventType}/{eventId}/subscribe/{entryToken}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable String entryToken) {
    SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

    // 3초마다 상태 전송
    scheduler.scheduleAtFixedRate(() -> {
        QueueStatusResponse status = queueService.getQueueStatusByToken(entryToken);
        emitter.send(SseEmitter.event()
            .name("queue-status")
            .data(status));

        if (status.status() == QueueStatus.ENTERED) {
            emitter.complete(); // 입장 완료 시 종료
        }
    }, 0, 3, TimeUnit.SECONDS);

    return emitter;
}
```

#### 5. 스케줄러 자동 처리

```java
@Scheduled(fixedDelay = 1000) // 1초마다 체크
public void processActiveQueues() {
    List<WaitingQueue> activeQueues = queueRepository.findByIsActiveTrue();

    for (WaitingQueue queue : activeQueues) {
        if (shouldProcessNow(queue)) {
            processEntries(queue.getEventType(), queue.getEventId());
        }
    }
}

private void processEntries(String eventType, Long eventId) {
    String key = String.format("queue:%s:%d:waiting", eventType, eventId);
    WaitingQueue queue = getQueue(eventType, eventId);

    // 상위 entryBatchSize명 조회
    Set<String> tokens = redisTemplate.opsForZSet()
        .range(key, 0, queue.getEntryBatchSize() - 1);

    for (String token : tokens) {
        // DB 상태 업데이트
        queueEntryRepository.findByEntryToken(token)
            .ifPresent(QueueEntry::enter);

        // Redis에서 제거
        redisTemplate.opsForZSet().remove(key, token);
    }
}
```

## Consequences (영향)

### 긍정적 영향 ✅

1. **시스템 안정성 향상**
   - DB 부하 분산: 대기열 조회는 Redis, 영속성은 PostgreSQL
   - 장애 격리: Redis 장애 시에도 서비스 가동 (DB 백업 사용)

2. **사용자 경험 개선**
   - 실시간 순번 업데이트 (SSE)
   - 정확한 예상 대기 시간 제공
   - 입장 완료 시 자동 페이지 전환

3. **확장성 확보**
   - Redis 클러스터링으로 수평 확장 가능
   - SSE 연결도 여러 인스턴스로 분산

4. **운영 편의성**
   - Redis CLI로 실시간 대기열 상태 확인
   - 관리자 API로 수동 제어 가능
   - Sorted Set의 직관적인 데이터 구조

### 부정적 영향 (트레이드오프) ⚠️

1. **Redis 의존성 증가**
   - **완화 방안**: PostgreSQL 백업으로 데이터 복구 가능
   - **모니터링**: Redis 상태 알림 설정

2. **SSE 연결 관리 복잡도**
   - **완화 방안**: 5분 타임아웃, 자동 재연결 로직
   - **부하 분산**: 여러 인스턴스로 SSE 연결 분산

3. **데이터 정합성 이슈**
   - Redis와 PostgreSQL 간 동기화 필요
   - **완화 방안**: 트랜잭션 처리, 주기적 정합성 체크

4. **메모리 사용량 증가**
   - Sorted Set에 모든 대기 엔트리 저장
   - **완화 방안**: 만료 엔트리 자동 삭제, TTL 설정

### 향후 고려사항

1. **Redis 고가용성**
   - Redis Sentinel 또는 Cluster 구성
   - 장애 복구 자동화

2. **SSE 대신 WebSocket 검토**
   - 양방향 통신 필요 시 전환
   - 현재는 단방향(서버→클라이언트)만 필요하므로 SSE 적합

3. **대기열 우선순위 기능**
   - VIP 회원 우선 입장
   - Score 조정으로 구현 가능

4. **통계 및 모니터링**
   - 대기 시간 평균/최대값 수집
   - 입장 성공률 추적
   - Grafana 대시보드 구성

## Implementation Notes (구현 참고사항)

### Redis Key 네이밍 규칙
```
queue:{eventType}:{eventId}:waiting    # 대기 중 엔트리
queue:{eventType}:{eventId}:entered    # 입장 완료 엔트리 (통계용, TTL 1시간)
```

### 예상 대기 시간 계산
```java
public Long calculateEstimatedWaitSeconds(Long position, WaitingQueue queue) {
    long batches = (long) Math.ceil((double) position / queue.getEntryBatchSize());
    return batches * queue.getEntryIntervalSeconds();
}
```

### SSE vs WebSocket 비교

| 항목 | SSE (채택) | WebSocket |
|------|-----------|-----------|
| **방향** | 단방향 (서버→클라이언트) | 양방향 |
| **프로토콜** | HTTP | TCP |
| **재연결** | 자동 | 수동 구현 |
| **브라우저 지원** | 모든 모던 브라우저 | IE 미지원 |
| **구현 복잡도** | 낮음 | 높음 |
| **사용 사례** | 대기열 상태 Push ✅ | 실시간 채팅 |

### Redis vs Kafka 비교

| 항목 | Redis Sorted Set (채택) | Kafka |
|------|------------------------|-------|
| **순번 조회** | O(log N) | 불가능 |
| **특정 항목 삭제** | O(log N) | 불가능 |
| **영속성** | RDB/AOF (선택) | 보장 |
| **처리량** | 10만 ops/s | 100만 msg/s |
| **인프라 복잡도** | 낮음 | 높음 |
| **대기열 적합성** | ✅ 높음 | ❌ 낮음 |

## Monitoring & Metrics (모니터링)

### 주요 메트릭

1. **대기열 크기**: `ZCARD queue:*:waiting`
2. **평균 대기 시간**: `avgWaitSeconds`
3. **입장 성공률**: `enteredCount / totalEntered`
4. **SSE 연결 수**: `activeSseConnections`
5. **Redis 응답 시간**: `redisLatency`

### 알림 기준

- 대기열 1만 명 초과
- 평균 대기 시간 10분 초과
- Redis 응답 시간 100ms 초과
- SSE 연결 실패율 5% 초과

---

**최종 승인일**: 2026-01-19
**검토자**: -
**관련 문서**:
- 구현 코드: `com.portal.universe.shoppingservice.queue.*`
- API 문서: `docs/api/queue-api.md`, `docs/api/admin-queue-api.md`
