# ADR-026: Shopping Service Saga 보상 액션 실패 처리 정책

**Status**: Proposed
**Date**: 2026-02-07
**Author**: Laze

## Context

Shopping Service의 주문-결제 Saga 패턴에서 보상 액션(compensation action)이 실패할 때의 처리 정책이 부재합니다.

1. **주문 취소 시 재고 해제 실패 (C4)**: `OrderServiceImpl.cancelOrder()`에서 `inventoryService.releaseStockBatch()`가 실패하면 catch 블록에서 로그만 남기고 주문 취소를 계속 진행합니다. 결과적으로 주문은 취소되었으나 재고는 예약 상태로 남아 판매 가능 수량이 줄어듭니다.

2. **결제 완료 후 주문 완료 실패 시 환불 (C5)**: `PaymentServiceImpl.processPayment()`에서 결제 성공 후 `orderService.completeOrderAfterPayment()`가 실패하면 `refundPaymentInternal()`을 호출합니다. 그러나 환불 자체가 실패할 경우의 처리가 없으며, `CustomBusinessException`만 throw하여 결제는 완료되었으나 주문도 미완/환불도 안 된 불일치 상태가 됩니다.

## Decision

**DLQ(Dead Letter Queue) + 스케줄러 기반 비동기 재시도 패턴을 채택합니다.**

보상 액션이 실패하면 즉시 3회 재시도하고, 전부 실패 시 `compensation_failures` 테이블에 기록하여 스케줄러가 주기적으로 재처리합니다. 재처리 한도(5회) 초과 시 CRITICAL 알림을 발행합니다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 즉시 재시도 (max N) | 구현 단순 | 인프라 장애 시 N회 모두 실패, 사용자 응답 지연 |
| ② 스케줄러 기반 비동기 재시도 (채택) | 사용자 응답 차단 안 함, 장애 복구 후 자동 처리 | 테이블/스케줄러 추가 필요 |
| ③ Kafka DLQ + Consumer | 기존 Kafka 인프라 활용 | 보상 로직이 이벤트 컨슈머에 분산, 디버깅 어려움 |
| ④ Saga 상태 머신 확장 | 완전한 상태 추적 | 현재 간소화된 Saga를 대폭 재설계 필요, 과도 |

## Rationale

- **사용자 경험 보호**: 주문 취소/결제 응답이 보상 액션 재시도에 의해 지연되면 안 됩니다. 비동기 재시도로 응답 시간을 보장합니다.
- **점진적 도입**: 기존 Saga 구조를 유지하면서 `compensation_failures` 테이블과 스케줄러만 추가하면 됩니다. 상태 머신 전면 재설계보다 위험이 낮습니다.
- **가시성 확보**: 실패한 보상 액션이 DB에 기록되므로 운영 대시보드에서 현황 파악이 가능하며, 수동 개입도 용이합니다.
- **기존 패턴 활용**: Spring `@Scheduled` + JPA로 구현 가능하며, 별도 인프라 추가가 불필요합니다.

## Trade-offs

✅ **장점**:
- 보상 실패가 묵인되지 않고 추적·재처리 가능
- 사용자 응답 시간에 영향 없음
- 운영팀이 실패 현황을 모니터링 가능

⚠️ **단점 및 완화**:
- `compensation_failures` 테이블 추가 필요 → (완화: 단순 구조, Flyway 마이그레이션 1건)
- 보상 완료까지 일시적 데이터 불일치 → (완화: 재시도 주기를 1분으로 설정하여 최소화, CRITICAL 알림으로 장기 미처리 방지)
- 스케줄러 중복 실행 방지 필요 → (완화: `@SchedulerLock` 또는 DB row-level lock 사용)

## Implementation

### 1. compensation_failures 테이블
```sql
CREATE TABLE compensation_failures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    compensation_type VARCHAR(50) NOT NULL,  -- STOCK_RELEASE, PAYMENT_REFUND
    reference_id VARCHAR(100) NOT NULL,       -- orderNumber, paymentNumber
    payload JSON NOT NULL,                    -- 보상에 필요한 데이터
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 5,
    status VARCHAR(20) DEFAULT 'PENDING',     -- PENDING, PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at DATETIME NOT NULL,
    last_attempted_at DATETIME,
    completed_at DATETIME
);
```

### 2. 수정 대상
- `OrderServiceImpl.cancelOrder()`: catch 블록에서 `compensation_failures` INSERT
- `PaymentServiceImpl.processPayment()`: 환불 실패 시 `compensation_failures` INSERT + 결제 상태를 `REFUND_PENDING`으로 설정
- 신규 `CompensationRetryScheduler`: `@Scheduled(fixedDelay=60000)` + `@SchedulerLock`

## References

- [ADR-016: Shopping Saga Pattern과 분산 트랜잭션](./ADR-016-shopping-feature-implementation.md)
- `OrderServiceImpl.java:176-193` (재고 해제 실패 무시)
- `PaymentServiceImpl.java:94-101` (환불 실패 미처리)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
