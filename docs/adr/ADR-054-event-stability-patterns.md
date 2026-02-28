# ADR-054: Event Stability — 이벤트 유실 방지 및 발행 안정성 패턴

**Status**: Accepted
**Date**: 2026-02-28
**Author**: Laze
**Related**: ADR-032 (Kafka Configuration), ADR-047 (Avro Schema Registry), ADR-053 (Saga Compensation)

## Context

기존 이벤트 발행 구조에 여러 안정성 문제가 있었다:

1. **Ghost Event**: `@Transactional` 내부에서 Kafka 발행 시, DB 롤백 후에도 이벤트가 전파됨
2. **Dual Write Problem**: DB 저장과 Kafka 발행을 원자적으로 묶을 수 없는 근본적 한계
3. **Silent Drop (Prism)**: KafkaJS producer가 연결 실패 시 이벤트를 debug 로그만 남기고 폐기
4. **에러 삼킴 (Prism)**: producer.send() 실패를 try-catch로 감싸 호출자에게 전파하지 않음
5. **Consumer 병목**: 모든 서비스 concurrency=1 고정 → 파티션 3개 중 1개만 소비
6. **Replicas 하드코딩**: `replicas(1)`이 코드에 하드코딩 → 환경별 분기 불가

## Decision

서비스 특성에 맞는 3가지 이벤트 발행 패턴을 도입하고, 공통 인프라 설정을 프로퍼티 기반으로 전환한다.

### D1. Shopping Service — Outbox Pattern (Polling Publisher)

주문/결제 등 **비즈니스 크리티컬** 이벤트에 대해 Outbox 패턴을 적용한다.

- `outbox_events` 테이블에 이벤트를 같은 DB 트랜잭션으로 저장
- `OutboxPollingScheduler`가 3초 간격으로 PENDING 이벤트를 배치 조회 (`FOR UPDATE SKIP LOCKED`)
- Avro Wire JSON으로 직렬화 (`SpecificDatumWriter` + `JsonEncoder`) → `JsonDecoder`로 역직렬화
- 최대 5회 재시도 후 FAILED 처리
- 기존 `ApplicationEventPublisher` + `@TransactionalEventListener` 제거

**대안 검토**:
- CDC (Debezium): 가장 강력하나 인프라 복잡도 높음 → 현재 규모에서 과잉
- AFTER_COMMIT: Ghost Event 방지하나 Kafka 발행 실패 시 유실 → Outbox보다 약한 보장

### D2. Auth Service — ResilientKafkaPublisher (재시도 + 지수 백오프)

회원가입/역할 할당 등 **보상이 불필요한** 이벤트에 대해 재시도 패턴을 적용한다.

- `CompletableFuture.delayedExecutor()`로 비블로킹 지수 백오프 (1초 → 2초 → 4초)
- 3회 실패 시 에러 로그 + 포기 (알림용 이벤트이므로 유실 허용)
- `@TransactionalEventListener(AFTER_COMMIT)` 유지 → Ghost Event 방지

### D3. Blog Service — AFTER_COMMIT + MongoDB Replica Set

좋아요/댓글 등 **알림용** 이벤트에 대해 AFTER_COMMIT 패턴을 적용한다.

- MongoDB를 Replica Set 모드로 전환 (`--replSet rs0` + keyFile 인증)
- `MongoTransactionManager` 빈 등록 → `@Transactional` + `@TransactionalEventListener(AFTER_COMMIT)` 활성화
- `BlogKafkaEventListener`가 커밋 후 Kafka 발행

### D4. Prism Service — KafkaJS Lazy 재연결 + 에러 전파

- Silent Drop 제거: `isConnected=false`일 때 lazy 재연결 시도
- 재연결 실패 시 `throw Error` (기존: debug 로그만)
- `execution.service.ts`에서 Kafka 발행을 별도 try-catch로 격리 → 실행 결과 오염 방지

### D5. Consumer Concurrency — 프로퍼티 기반

- `AvroConsumerConfig`에 `spring.kafka.listener.concurrency` 프로퍼티 주입 (기본값: 3)
- 파티션 수(3)와 일치하는 consumer thread 보장

### D6. Topic Replicas — 프로파일별 분기

- `KafkaTopicConfig`에서 `app.kafka.topic.replicas` 프로퍼티 주입 (기본값: 1)
- `application-kubernetes.yml`에서 `replicas: 3` 설정
- local/docker: 1, kubernetes: 3

### D7. MongoDB Replica Set (Docker)

- `docker-compose.yml`: `--replSet rs0 --keyFile` 옵션 추가
- `infrastructure/mongodb/replica-keyfile`: 내부 인증용 키 파일
- Healthcheck에서 `rs.initiate()` 자동 실행 (최초 1회)

## Consequences

### 긍정적
- Shopping: 이벤트 유실 0건 보장 (at-least-once)
- Auth: 일시적 Kafka 장애에서 자동 복구
- Blog: MongoDB 트랜잭션 + AFTER_COMMIT으로 Ghost Event 방지
- Prism: 장애 가시성 확보 (Silent Drop → 명시적 에러)
- 전체: 환경별 Kafka 설정 유연성 확보

### 부정적
- Shopping Outbox: 폴링 간격(3초) 만큼의 이벤트 발행 지연
- Blog MongoDB: Replica Set 필수 → 로컬 개발 환경 복잡도 소폭 증가
- Consumer concurrency 3: 메모리 사용량 소폭 증가 (consumer thread × 3)

## 패턴 선택 기준

| 기준 | Outbox | Resilient | AFTER_COMMIT |
|------|--------|-----------|--------------|
| **보장 수준** | at-least-once (DB 기반) | best-effort (재시도) | at-most-once (커밋 후 1회) |
| **적합 대상** | 비즈니스 크리티컬 (주문, 결제) | 보상 불필요 (가입, 역할) | 알림용 (좋아요, 댓글) |
| **인프라 요구** | RDB outbox 테이블 | 없음 | MongoDB Replica Set |
| **지연** | 폴링 간격 (3초) | 즉시 (실패 시 백오프) | 즉시 (커밋 후) |
