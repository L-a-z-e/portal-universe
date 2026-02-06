# ADR-020: Redis Sorted Set 기반 대기열 시스템

**Status**: Accepted
**Date**: 2026-01-19

## Context
Shopping Service의 타임딜 및 플래시 세일 이벤트는 단시간에 대량의 트래픽이 집중됩니다. 동시 접속 폭증, 재고 부족, DB 과부하로 인해 시스템 안정성과 사용자 경험이 저하됩니다. 공정한 선착순 보장과 실시간 대기 상태 업데이트가 필요합니다.

## Decision
**Redis Sorted Set + SSE** 방식을 채택하여 timestamp 기반 대기열을 구현하고, SSE로 실시간 상태를 Push 방식으로 업데이트합니다.

## Rationale
- 최적의 성능: Sorted Set의 O(log N) 연산으로 빠른 순번 조회
- 자동 정렬: timestamp score로 진입 순서 자동 유지
- 효율적인 범위 조회: `entryBatchSize`만큼 한 번에 조회
- 실시간 업데이트: SSE를 통한 서버 Push 방식
- 관리 용이성: Redis CLI로 직접 확인 및 수정 가능

## Trade-offs
✅ **장점**:
- 시스템 안정성 향상 (DB 부하 분산)
- 사용자 경험 개선 (실시간 순번, 예상 대기 시간)
- 확장성 확보 (Redis 클러스터링)
- 운영 편의성 (직관적인 데이터 구조)

⚠️ **단점 및 완화**:
- Redis 의존성 증가 → (완화: PostgreSQL 백업으로 데이터 복구 가능)
- SSE 연결 관리 복잡도 → (완화: 5분 타임아웃, 자동 재연결)
- 데이터 정합성 이슈 → (완화: 트랜잭션 처리, 주기적 정합성 체크)
- 메모리 사용량 증가 → (완화: 만료 엔트리 자동 삭제, TTL 설정)

## Implementation
### Redis Sorted Set 구조
```
Key: queue:{eventType}:{eventId}:waiting
Score: joinedAt (timestamp)
Member: entryToken (UUID)
```

### 주요 연산
| 연산 | 목적 | 시간 복잡도 |
|------|------|------------|
| `ZADD` | 대기열 진입 | O(log N) |
| `ZRANK` | 순번 조회 | O(log N) |
| `ZCARD` | 전체 대기 인원 | O(1) |
| `ZRANGE` | 입장 대상 조회 | O(log N + M) |
| `ZREM` | 대기열 이탈 | O(log N) |

### PostgreSQL 백업
- `queue_entries` 테이블: 엔트리 상태 저장 (WAITING, ENTERED, EXPIRED, LEFT)
- Redis 장애 시 복구용

### SSE 구현
- 3초마다 상태 전송 (`queue-status` 이벤트)
- 5분 타임아웃, 자동 재연결
- 입장 완료 시 연결 종료

### 대안 비교
| 대안 | 순번 조회 | 삭제 | 영속성 | 평가 |
|------|----------|------|--------|------|
| **Redis Sorted Set** | O(log N) | O(log N) | 선택적 | ✅ 채택 |
| DB (RDBMS) | O(N) | O(1) | 보장 | ❌ 성능 부족 |
| Redis List | O(N) | O(N) | 선택적 | ❌ 순번 조회 어려움 |
| Kafka | 불가능 | 불가능 | 보장 | ❌ 대기열 부적합 |

## References
- 구현 코드: `com.portal.universe.shoppingservice.queue.*`
- [Queue API 문서](../api/queue-api.md)
- [Admin Queue API 문서](../api/admin-queue-api.md)

---

📂 상세: [old-docs/services/shopping-service/adr/ADR-002-queue-system-design.md](../old-docs/services/shopping-service/adr/ADR-002-queue-system-design.md)
