# Architecture Decision Records (ADR)

Shopping Service의 중요한 아키텍처 결정 사항을 기록한 문서 목록입니다.

## 📋 ADR 목록

| ID | 제목 | 상태 | 결정일 | 태그 |
|----|------|------|--------|------|
| [ADR-001](./ADR-001-saga-pattern.md) | Shopping Service에서 Saga Orchestration 패턴 적용 | ✅ Accepted | 2026-01-19 | saga, distributed-transaction, orchestration, order-processing |
| [ADR-002](./ADR-002-queue-system-design.md) | Redis Sorted Set 기반 대기열 시스템 설계 | ✅ Accepted | 2026-01-19 | queue, redis, sse, timedeal, concurrency |

## 📊 상태별 분류

### ✅ Accepted (승인됨)
- [ADR-001](./ADR-001-saga-pattern.md) - Saga Orchestration 패턴 적용
- [ADR-002](./ADR-002-queue-system-design.md) - Redis Sorted Set 기반 대기열 시스템

### 🚧 Proposed (제안됨)
- 없음

### ❌ Rejected (거부됨)
- 없음

### ⏸️ Deprecated (폐기됨)
- 없음

### 🔄 Superseded (대체됨)
- 없음

## 🏷️ 태그별 분류

### Distributed Transaction
- [ADR-001](./ADR-001-saga-pattern.md) - Saga Orchestration 패턴

### Order Processing
- [ADR-001](./ADR-001-saga-pattern.md) - Saga Orchestration 패턴

### Queue & Concurrency
- [ADR-002](./ADR-002-queue-system-design.md) - Redis Sorted Set 기반 대기열 시스템

### Real-time Communication
- [ADR-002](./ADR-002-queue-system-design.md) - SSE 기반 실시간 업데이트

## 📚 ADR 작성 가이드

새로운 ADR을 작성할 때는 다음을 참고하세요:
- [ADR 작성 가이드](../../../docs_template/guide/adr/how-to-write.md)
- 명명 규칙: `ADR-XXX-[decision-title].md`
- 마지막 ID: **ADR-002**
- 다음 ID: **ADR-003**

## 🔗 관련 문서

- [Architecture 문서](../architecture/)
- [API 문서](../api/)
- [Guides](../guides/)
