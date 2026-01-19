---
id: ADR-001
title: Shopping Service에서 Saga Orchestration 패턴 적용
type: adr
status: accepted
created: 2026-01-19
updated: 2026-01-19
author: Laze
decision_date: 2026-01-19
reviewers: []
tags: [saga, distributed-transaction, orchestration, order-processing]
related:
  - architecture/order-flow.md
---

# ADR-001: Shopping Service에서 Saga Orchestration 패턴 적용

## Context (배경)

Shopping Service의 주문 처리는 여러 단계의 작업을 순차적으로 수행해야 합니다:

1. **재고 예약** (RESERVE_INVENTORY)
2. **결제 처리** (PROCESS_PAYMENT)
3. **재고 차감** (DEDUCT_INVENTORY)
4. **배송 생성** (CREATE_DELIVERY)
5. **주문 확정** (CONFIRM_ORDER)

각 단계는 독립적인 도메인 로직을 가지며, 중간 단계에서 실패 시 이미 완료된 작업들을 롤백해야 하는 요구사항이 있습니다. 전통적인 ACID 트랜잭션으로는 이러한 분산 작업들의 일관성을 보장하기 어렵습니다.

### 기술적 제약 조건
- 각 단계는 다른 도메인 엔티티를 다룸 (재고, 결제, 배송)
- 외부 결제 시스템과의 통신 필요
- 실패 시 부분 완료 상태를 복구해야 함
- 장시간 실행되는 트랜잭션 (Long-running transaction)

### 비즈니스 제약 조건
- 주문 처리 중 실패 시 고객에게 명확한 실패 원인 제공
- 재고와 결제 간 데이터 일관성 보장
- 시스템 장애 시에도 복구 가능한 구조 필요

## Decision Drivers (결정 요인)

1. **데이터 일관성 보장**: 모든 단계가 성공하거나, 실패 시 원자적으로 롤백되어야 함
2. **실패 복구 메커니즘**: 중간 단계 실패 시 보상 트랜잭션(Compensation) 자동 실행
3. **추적 가능성**: 주문 처리 과정의 각 단계를 추적하고 디버깅 가능
4. **유지보수성**: 새로운 단계 추가 및 기존 단계 수정이 용이
5. **성능**: 동기식 처리로 즉각적인 결과 반환 (비동기 메시징 오버헤드 최소화)
6. **복잡도**: 구현 및 운영의 복잡도가 팀의 역량에 적합

## Considered Options (검토한 대안)

### Option 1: Two-Phase Commit (2PC)

**설명**: 분산 트랜잭션 코디네이터를 사용하여 모든 참여자가 커밋 준비 후 일괄 커밋

**장점**:
- 강력한 일관성 보장 (ACID)
- 표준 프로토콜 존재

**단점**:
- 모든 참여자가 락을 유지해야 하므로 성능 저하
- 코디네이터 SPOF (Single Point of Failure)
- 외부 결제 시스템 등에서 2PC 미지원
- 마이크로서비스 환경에서 권장되지 않음

**결론**: ❌ 부적합

---

### Option 2: Saga Choreography 패턴

**설명**: 각 서비스가 자신의 로컬 트랜잭션을 완료한 후 이벤트를 발행하고, 다른 서비스가 이벤트를 구독하여 다음 단계 실행

**장점**:
- 서비스 간 느슨한 결합 (Loose coupling)
- 중앙 조정자 없이 분산 처리 가능
- 각 서비스의 독립성 보장

**단점**:
- 전체 흐름 파악이 어려움 (이벤트 체인 추적 복잡)
- 순환 의존성 발생 가능
- 디버깅 및 모니터링 복잡
- 현재 Shopping Service 내부 로직이므로 과도한 분산 불필요

**결론**: ❌ 향후 서비스 간 통합 시 고려 가능하나, 현재는 과도한 복잡도

---

### Option 3: Saga Orchestration 패턴 ✅

**설명**: 중앙 오케스트레이터(`OrderSagaOrchestrator`)가 각 단계를 순차적으로 실행하고, 실패 시 보상 트랜잭션을 역순으로 실행

**구현 방식**:
```java
// OrderSagaOrchestrator
public void executeOrderSaga(Order order) {
    SagaState sagaState = new SagaState(order.getId());

    try {
        // 1. 재고 예약
        sagaState.addCompletedStep("RESERVE_INVENTORY");
        reserveInventory(order);

        // 2. 결제 처리
        sagaState.addCompletedStep("PROCESS_PAYMENT");
        processPayment(order);

        // 3. 재고 차감
        sagaState.addCompletedStep("DEDUCT_INVENTORY");
        deductInventory(order);

        // 4. 배송 생성
        sagaState.addCompletedStep("CREATE_DELIVERY");
        createDelivery(order);

        // 5. 주문 확정
        confirmOrder(order);

    } catch (Exception e) {
        compensate(sagaState); // 역순 롤백
        throw e;
    }
}
```

**장점**:
- ✅ 전체 워크플로우를 한 곳에서 명확히 관리
- ✅ 실패 시 보상 로직을 중앙에서 제어 (최대 3회 재시도)
- ✅ `SagaState` 엔티티로 각 단계 상태 영속화 (추적 가능)
- ✅ 새로운 단계 추가 시 오케스트레이터만 수정하면 됨
- ✅ 동기식 처리로 즉각적인 응답 가능
- ✅ 디버깅 및 모니터링 용이

**단점**:
- ⚠️ 오케스트레이터가 SPOF 가능성 (단, 동일 서비스 내이므로 영향 제한적)
- ⚠️ 오케스트레이터에 비즈니스 로직 집중 (적절한 도메인 분리 필요)

**결론**: ✅ 현재 요구사항에 가장 적합

## Decision (최종 결정)

**Saga Orchestration 패턴을 채택합니다.**

### 선택 이유

1. **명확한 흐름 제어**: `OrderSagaOrchestrator` 클래스에서 전체 주문 프로세스를 순차적으로 관리
2. **효과적인 보상 처리**: 실패 시 완료된 단계들을 역순으로 롤백 (`releaseStockBatch`, 주문 취소 등)
3. **상태 추적**: `SagaState` 엔티티와 `SagaStateRepository`를 통해 각 단계의 성공/실패 이력 저장
4. **팀의 이해도**: Choreography보다 직관적이며, 기존 Spring `@Transactional` 패턴과 유사
5. **현재 아키텍처 적합성**: 서비스 내부 로직이므로 과도한 분산 메커니즘 불필요

### 구현 세부사항

- **클래스**: `OrderSagaOrchestrator`
- **실행 흐름**:
  1. RESERVE_INVENTORY
  2. PROCESS_PAYMENT (별도 호출)
  3. DEDUCT_INVENTORY
  4. CREATE_DELIVERY
  5. CONFIRM_ORDER
- **보상 처리**:
  - 역순으로 완료된 단계 롤백
  - 최대 3회 보상 시도 (`MAX_COMPENSATION_ATTEMPTS`)
- **사용 기술**:
  - Spring `@Transactional`
  - `SagaState` 엔티티 (상태 영속화)
  - `SagaStateRepository` (JPA)

## Consequences (영향)

### 긍정적 영향 ✅

1. **데이터 일관성 보장**: 부분 실패 시 자동 롤백으로 데이터 정합성 유지
2. **운영 가시성**: SagaState 테이블 조회로 주문 처리 과정 추적 가능
3. **확장성**: 새로운 단계(예: 쿠폰 차감, 포인트 적립) 추가 시 오케스트레이터만 수정
4. **테스트 용이성**: 각 단계를 독립적으로 모킹(Mock) 가능
5. **장애 격리**: 특정 단계 실패가 다른 주문 처리에 영향 최소화

### 부정적 영향 (트레이드오프) ⚠️

1. **오케스트레이터 의존성**: 모든 워크플로우가 `OrderSagaOrchestrator`에 집중
   - **완화 방안**: 도메인 로직은 각 Service에 위임, 오케스트레이터는 조정만 담당

2. **동기 처리 한계**: 장시간 작업 시 응답 지연 가능
   - **완화 방안**: 외부 API 호출(결제) 타임아웃 설정, 필요 시 비동기 전환 고려

3. **보상 트랜잭션 복잡도**: 롤백 로직 구현 및 유지보수 필요
   - **완화 방안**: 각 단계별 명확한 보상 로직 문서화 및 테스트 코드 작성

4. **SagaState 테이블 관리**: 완료된 Saga 상태 데이터 증가
   - **완화 방안**: 주기적으로 오래된 데이터 아카이빙 또는 삭제 (Runbook 작성 필요)

### 향후 고려사항

- **서비스 간 분산 확장 시**: Saga Choreography 또는 메시지 큐 기반 Orchestration 전환 검토
- **비동기 처리 필요 시**: Kafka 기반 이벤트 발행으로 개선 가능
- **모니터링 강화**: SagaState 변경 이력을 메트릭으로 수집 (실패율, 보상 성공률 등)

---

**최종 승인일**: 2026-01-19
**검토자**: -
**관련 문서**:
- 구현 코드: `com.universe.shopping.saga.OrderSagaOrchestrator`
- Architecture: `docs/architecture/order-flow.md`
