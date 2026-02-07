# ADR-025: Shopping Service 분산 데이터 정합성 전략 (Redis ↔ DB)

**Status**: Proposed
**Date**: 2026-02-07
**Author**: Laze

## Context

Shopping Service에서 Redis와 DB(MySQL)를 함께 사용하는 패턴에서 두 가지 정합성 문제가 발견되었습니다.

1. **Queue popMin 유실 (C3)**: `QueueServiceImpl.processQueue()`에서 `redisTemplate.opsForZSet().popMin()`으로 Redis Sorted Set에서 대기열 항목을 꺼낸 뒤, DB에 `entry.enter()` + `save()`를 수행합니다. 만약 DB 저장이 실패하면 Redis에서는 이미 제거된 상태이므로, 해당 사용자의 대기열 항목이 영구 유실됩니다.

2. **Inventory Pub/Sub 발행 실패 무시 (C7)**: `InventoryServiceImpl.publishInventoryUpdate()`에서 Redis Pub/Sub 메시지 직렬화가 실패(`JsonProcessingException`)하면 로그만 남기고 무시합니다. 프론트엔드의 실시간 재고 표시가 stale 상태로 남게 됩니다.

두 이슈 모두 Redis와 DB 간 원자적 연산이 보장되지 않아 발생합니다.

## Decision

**DB 선행(Write-Through) 패턴을 기본 전략으로 채택하고, 실패 시 보상 로직을 추가합니다.**

- Queue: DB 상태 변경을 먼저 수행하고, 성공 시 Redis에서 제거. DB 실패 시 Redis 복원 불필요.
- Inventory Pub/Sub: DB 트랜잭션 완료 후 발행하며, 실패 시 재시도(최대 3회) + 메트릭 기록.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① DB 선행 후 Redis 반영 (채택) | 데이터 유실 방지, 구현 단순 | Redis 조회 시 짧은 stale 구간 |
| ② Transactional Outbox | 완전한 정합성 보장 | Outbox 테이블 + 폴러 필요, 복잡도 높음 |
| ③ CDC (Debezium) | 인프라 레벨 정합성 | Debezium 운영 부담, 현 규모 대비 과도 |
| ④ 보상 재삽입 (현행 개선) | 기존 구조 유지 | 재삽입 실패 시 2차 유실 위험, 순서 보장 불가 |

## Rationale

- **데이터 안전성 우선**: 대기열에서 사용자가 빠지는 것은 결제와 직결되므로, DB를 source of truth로 삼아야 합니다.
- **구현 복잡도 적정**: Outbox/CDC는 현재 단일 서비스 내 Redis↔DB 동기화에는 과도합니다. DB 선행 패턴으로 충분히 문제를 해결할 수 있습니다.
- **장애 격리**: Redis 장애 시에도 DB 데이터는 보존되므로, Redis 복구 후 DB 기준으로 재구성 가능합니다.
- **Pub/Sub 특성 인정**: Redis Pub/Sub은 fire-and-forget 특성이므로, 실패 시 재시도는 하되 최종 정합성은 클라이언트의 폴링 fallback으로 보완합니다.

## Trade-offs

✅ **장점**:
- 대기열 항목 유실 문제 근본 해결
- DB가 단일 source of truth → 장애 복구 시 Redis 재구성 가능
- 기존 코드 구조 큰 변경 없이 적용 가능

⚠️ **단점 및 완화**:
- Redis 조회 시 짧은 시간 동안 stale 데이터 가능 → (완화: 입장 처리 주기가 3초이므로 실질적 영향 없음)
- DB 쓰기 지연이 Redis 연산보다 길어 처리량 감소 가능 → (완화: 배치 처리로 DB 호출 최소화)

## Implementation

### Queue (C3 해결)
- `QueueServiceImpl.processQueue()`: `popMin()` 대신 `rangeWithScores(0, N-1)`로 조회 → DB 저장 성공 후 `removeRange()` 또는 `remove()`로 Redis에서 제거
- DB 저장 실패 시 Redis 항목은 그대로 유지되므로 다음 주기에 재처리

### Inventory (C7 해결)
- `InventoryServiceImpl.publishInventoryUpdate()`: `@Retryable(maxAttempts=3)` 적용 또는 try 블록 내 수동 재시도
- 재시도 전부 실패 시 `inventory.pubsub.failure` 메트릭 증가 + WARN 로그
- 프론트엔드 SSE 스트림에 주기적 full-sync 이벤트 추가 (fallback)

## References

- [ADR-020: Redis Sorted Set 대기열 시스템](./ADR-020-shopping-queue-system.md)
- [ADR-016: Shopping Saga Pattern과 분산 트랜잭션](./ADR-016-shopping-feature-implementation.md)
- `QueueServiceImpl.java:194-215` (popMin 로직)
- `InventoryServiceImpl.java:330-347` (publishInventoryUpdate)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
