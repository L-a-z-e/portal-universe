---
id: arch-queue-system
title: Queue System Architecture
type: architecture
status: current
created: 2026-02-06
updated: 2026-03-01
author: Laze
tags: [architecture, shopping-service, queue, redis, sse, sorted-set]
related:
  - arch-system-overview
  - arch-data-flow
  - arch-timedeal-system
---

# Queue System Architecture

## 개요

| 항목 | 내용 |
|------|------|
| **범위** | 이벤트 대기열 관리, 순번 추적, 실시간 상태 알림 |
| **주요 기술** | Redis Sorted Set, SSE (SseEmitter), MySQL |
| **배포 환경** | Shopping Service 내 Queue 도메인 |
| **관련 서비스** | TimeDeal, Coupon (Queue-Coupon 연동), seller-service (Kafka 동기화), auth-service (userId) |

이벤트(타임딜, 플래시세일, 쿠폰 등)에 대한 대기열을 Redis Sorted Set으로 관리하며, SSE(Server-Sent Events)를 통해 클라이언트에 실시간 순번 업데이트를 전달합니다. 대기열 설정은 seller-service에서 Kafka 이벤트로 동기화됩니다.

---

## 아키텍처 다이어그램

```mermaid
graph TB
    subgraph "Client"
        C[Client]
    end

    subgraph "Queue Domain"
        QC[QueueController]
        AQC[AdminQueueController]
        QSC[QueueStreamController<br/>SSE]
        QS[QueueServiceImpl]
    end

    subgraph "Redis"
        ZS[queue:waiting:{type}:{id}<br/>Sorted Set]
        RS[queue:entered:{type}:{id}<br/>Set]
    end

    subgraph "MySQL"
        WQT[(waiting_queues)]
        QET[(queue_entries)]
    end

    C --> QC --> QS
    C -.->|SSE| QSC
    AQC --> QS
    QS --> ZS & RS
    QS --> WQT & QET
    QSC --> QS
```

---

## 핵심 컴포넌트

### WaitingQueue 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| eventType | String | 이벤트 유형 (TIMEDEAL, FLASH_SALE 등) |
| eventId | Long | 이벤트 ID |
| maxCapacity | Integer | 동시 입장 가능 인원 |
| entryBatchSize | Integer | 한 번에 입장시킬 인원 수 |
| entryIntervalSeconds | Integer | 입장 처리 간격 (초) |
| isActive | Boolean | 활성화 여부 (default false) |
| createdAt | DateTime | 생성 일시 |
| activatedAt | DateTime | 활성화 일시 |
| deactivatedAt | DateTime | 비활성화 일시 |

### QueueEntry 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| queue | WaitingQueue (FK) | 대기열 참조 |
| userId | String | 사용자 ID |
| entryToken | String (UK) | UUID 토큰 (SSE 구독 시 사용) |
| status | QueueStatus | WAITING, ENTERED, EXPIRED, LEFT |
| joinedAt | DateTime | 입장 일시 |
| enteredAt | DateTime | 입장 완료 일시 |
| expiredAt | DateTime | 만료 일시 |
| leftAt | DateTime | 이탈 일시 |

**Index**: (queue_id, user_id), (entry_token)

### Redis 데이터 구조

| Key | Type | 설명 |
|-----|------|------|
| `queue:waiting:{eventType}:{eventId}` | Sorted Set | 대기 사용자 (score = timestamp) |
| `queue:entered:{eventType}:{eventId}` | Set | 입장 완료 사용자 |

**Redis 명령어 활용**:

| 명령어 | 용도 |
|--------|------|
| `ZADD` | 대기열에 사용자 추가 (score = currentTimeMillis) |
| `ZRANK` | 사용자 순번 조회 (0-based) |
| `ZCARD` | 전체 대기 인원 수 |
| `ZPOPMIN` | 최전방 N명 추출 (입장 처리) |
| `ZREM` | 대기열에서 사용자 제거 (이탈) |
| `SCARD` | 현재 입장 인원 수 |
| `SADD` | 입장 완료 기록 |

---

## 데이터 플로우

### 대기열 입장

```mermaid
sequenceDiagram
    participant C as Client
    participant QS as QueueService
    participant Redis as Redis
    participant DB as MySQL

    C->>QS: enterQueue(eventType, eventId, userId)
    QS->>DB: SELECT waiting_queue WHERE eventType AND eventId
    QS->>QS: validate (isActive, 중복 입장 확인)

    QS->>DB: INSERT queue_entry (WAITING, token=UUID)
    QS->>Redis: ZADD queue:waiting:{type}:{id} {timestamp} {userId}
    QS->>Redis: ZRANK queue:waiting:{type}:{id} {userId}
    Redis-->>QS: position (0-based)

    QS->>QS: estimatedWait = (position / batchSize) * intervalSeconds
    QS-->>C: {position, estimatedWaitSeconds, entryToken, totalWaiting}
```

### SSE 실시간 상태 전달

```mermaid
sequenceDiagram
    participant C as Client
    participant SSE as QueueStreamController
    participant QS as QueueService
    participant Redis as Redis

    C->>SSE: GET /queue/{type}/{id}/subscribe/{token}
    SSE->>SSE: Create SseEmitter (timeout=5min)
    SSE->>SSE: Register cleanup (onCompletion, onError, onTimeout)

    loop 3초마다 (ScheduledExecutorService)
        SSE->>QS: getQueueStatus(entryToken)
        QS->>Redis: ZRANK queue:waiting:{type}:{id} {userId}

        alt 대기 중 (rank != null)
            Redis-->>QS: position
            QS-->>SSE: {status: WAITING, position, estimatedWait}
            SSE-->>C: event: queue-status

        else 입장 완료 (rank = null, entered check)
            QS->>Redis: SISMEMBER queue:entered:{type}:{id} {userId}
            Redis-->>QS: true
            QS-->>SSE: {status: ENTERED}
            SSE-->>C: event: queue-entered
            SSE->>SSE: emitter.complete()
        end
    end
```

**SSE 연결 관리**:
- `ConcurrentHashMap<String, SseEmitter>` - 토큰별 emitter
- `ConcurrentHashMap<String, ScheduledFuture<?>>` - 토큰별 스케줄 태스크
- `@PreDestroy` - 서버 종료 시 모든 연결 정리
- Timeout: 5분 (클라이언트 재연결 필요)

### 입장 처리 (Batch Admission — Lua Script)

TOCTOU(Time-of-Check-to-Time-of-Use) Race Condition을 방지하기 위해 `queue_process.lua` Lua Script로 CHECK+POP+ADD를 원자적으로 실행합니다. `@DistributedLock`으로 멀티 Pod 동시 실행을 이중 방어합니다.

```mermaid
sequenceDiagram
    participant SCHED as QueueScheduler<br/>@DistributedLock
    participant QS as QueueService
    participant Redis as Redis (Lua)
    participant DB as PostgreSQL

    SCHED->>QS: processEntries(eventType, eventId)
    QS->>Redis: EVAL queue_process.lua

    Note over Redis: Lua 원자적 실행
    Note over Redis: 1. SCARD entered → 현재 입장 인원
    Note over Redis: 2. availableSlots = maxCapacity - entered
    Note over Redis: 3. toAdmit = min(available, batchSize)
    Note over Redis: 4. ZPOPMIN waiting toAdmit
    Note over Redis: 5. SADD entered (각 userId)

    Redis-->>QS: admitted tokens[]

    loop 각 admitted user
        QS->>DB: UPDATE queue_entry SET status=ENTERED, enteredAt=now
    end
```

### Lua Script 상세 (`queue_process.lua`)

```
KEYS[1] = queue:entered:{eventType}:{eventId}     (Set)
KEYS[2] = queue:waiting:{eventType}:{eventId}      (Sorted Set)
ARGV[1] = maxCapacity
ARGV[2] = entryBatchSize

1. SCARD KEYS[1]                    → 현재 입장 인원
2. availableSlots = max - entered   → 0이면 빈 목록 반환
3. toAdmit = min(available, batch)
4. ZPOPMIN KEYS[2] toAdmit          → 최전방 N명 추출
5. SADD KEYS[1] (각 userId)         → 입장 완료 기록
6. return admitted tokens
```

---

## 기술적 결정

### Redis Sorted Set을 선택한 이유

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|:---------:|
| **Redis Sorted Set** | O(log N) 삽입/조회, 순번 즉시 계산 | 메모리 사용 | **선택** |
| Redis List | O(1) push/pop | 순번 조회 O(N) | - |
| MySQL + polling | 영속성 | 성능 병목, 폴링 오버헤드 | - |
| RabbitMQ | 메시지 큐 전문 | 순번 조회 어려움 | - |

### SSE vs WebSocket

| 항목 | SSE | WebSocket |
|------|-----|-----------|
| 방향 | 서버 -> 클라이언트 (단방향) | 양방향 |
| 프로토콜 | HTTP | WS |
| 복잡도 | 낮음 | 높음 |
| **선택 이유** | 대기열 상태는 서버 -> 클라이언트 단방향이면 충분 | - |

### 예상 대기 시간 계산

```
estimatedWaitSeconds = (position / entryBatchSize) * entryIntervalSeconds
```

예: position=15, batchSize=5, interval=30초 -> 예상 대기 90초

---

## Kafka 이벤트 동기화 (Seller → Buyer)

대기열 설정은 seller-service에서 관리되고 (shopping_seller_db), buyer 조회는 shopping-service에서 수행됩니다 (shopping_db). 두 DB 간 동기화를 위해 Kafka 이벤트를 사용합니다.

```mermaid
sequenceDiagram
    participant SS as Seller Service
    participant K as Kafka
    participant BS as Shopping Service
    participant SDB as shopping_seller_db
    participant BDB as shopping_db

    SS->>SDB: queue.activate()
    SS->>SS: ApplicationEventPublisher.publishEvent()
    SS->>K: QueueActivatedEvent (AFTER_COMMIT)
    K->>BS: @KafkaListener
    BS->>BDB: waiting_queues INSERT/UPDATE + activate()
```

| 이벤트 | Topic | 설명 |
|--------|-------|------|
| `QueueActivatedEvent` | `seller.queue.activated` | 대기열 활성화 (maxCapacity, batchSize, interval 포함) |
| `QueueDeactivatedEvent` | `seller.queue.deactivated` | 대기열 비활성화 |

### SSE 인증 모델

SSE 구독 엔드포인트는 Gateway `permitAll`로 설정되어 JWT 인증 없이 접근 가능합니다. entryToken(UUID v4) 자체가 비밀값으로서 인증 역할을 수행합니다.

- **이유**: 브라우저 EventSource API가 Authorization 헤더를 지원하지 않음
- **보안**: entryToken은 UUID v4로 추측 불가, `getQueueStatusByToken()`에서 존재 여부 검증

---

## 에러 코드

| 코드 | 이름 | 설명 |
|------|------|------|
| S801 | QUEUE_NOT_FOUND | 대기열 없음 |
| S802 | QUEUE_ALREADY_ENTERED | 이미 대기열에 있음 |
| S803 | QUEUE_ENTRY_NOT_FOUND | 대기열 항목 없음 |
| S804 | QUEUE_TOKEN_EXPIRED | 토큰 만료 |
| S805 | QUEUE_NOT_ALLOWED | 입장 불허 |
| S806 | QUEUE_ENTRY_REQUIRED | 대기열 입장 필요 |
| S807 | QUEUE_TOKEN_USER_MISMATCH | 토큰 소유자 불일치 |

---

## API 엔드포인트

### 사용자 API

| Method | Path | 설명 |
|--------|------|------|
| POST | `/queue/{eventType}/{eventId}/enter` | 대기열 입장 |
| GET | `/queue/{eventType}/{eventId}/status` | 대기열 상태 조회 |
| POST | `/queue/{eventType}/{eventId}/leave` | 대기열 이탈 |
| GET | `/queue/token/{entryToken}/status` | 토큰으로 상태 조회 |
| GET | `/queue/{eventType}/{eventId}/check` | 활성 여부 확인 |
| GET | `/queue/{eventType}/{eventId}/subscribe/{entryToken}` | SSE 구독 (permitAll) |

### 관리자 API

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/queue/activate` | 대기열 활성화 |
| POST | `/admin/queue/deactivate` | 대기열 비활성화 |
| POST | `/admin/queue/process` | 수동 입장 처리 |

---

## 관련 문서

- [System Overview](./system-overview.md)
- [Data Flow](./data-flow.md) - 전체 데이터 흐름
- [TimeDeal System](./timedeal-system.md) - 대기열 연동 대상

---

**최종 업데이트**: 2026-03-01
