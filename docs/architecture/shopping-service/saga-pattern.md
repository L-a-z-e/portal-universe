---
id: arch-saga-pattern
title: Order Saga Pattern Architecture
type: architecture
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [architecture, shopping-service, saga, distributed-transaction, compensation]
related:
  - arch-system-overview
  - arch-data-flow
---

# Order Saga Pattern Architecture

## 개요

| 항목 | 내용 |
|------|------|
| **범위** | 주문 생성부터 확정까지의 5단계 분산 트랜잭션 관리 |
| **주요 기술** | Orchestrator Saga, Compensation Transaction |
| **배포 환경** | Shopping Service 내 Order/Saga 도메인 |
| **관련 서비스** | Inventory, Payment, Delivery (내부 도메인) |

Shopping Service의 주문 처리는 `OrderSagaOrchestrator`가 5단계 Forward/Compensation을 조율합니다. 각 단계의 성공/실패에 따라 자동 보상이 실행되며, 최대 3회 보상 재시도 후 수동 개입으로 전환됩니다.

---

## 아키텍처 다이어그램

```mermaid
graph TB
    subgraph "Order Domain"
        OS[OrderService]
        SAGA[OrderSagaOrchestrator]
    end

    subgraph "Saga Steps"
        S1[Step 1<br/>RESERVE_INVENTORY]
        S2[Step 2<br/>PROCESS_PAYMENT]
        S3[Step 3<br/>DEDUCT_INVENTORY]
        S4[Step 4<br/>CREATE_DELIVERY]
        S5[Step 5<br/>CONFIRM_ORDER]
    end

    subgraph "Services"
        IS[InventoryService]
        PS[PaymentService]
        DS[DeliveryService]
    end

    subgraph "MySQL"
        SST[(saga_states)]
    end

    OS -->|startSaga| SAGA
    PS -->|completeSagaAfterPayment| SAGA

    SAGA --> S1 --> IS
    SAGA --> S2
    SAGA --> S3 --> IS
    SAGA --> S4 --> DS
    SAGA --> S5

    SAGA --> SST
```

---

## Saga State Machine

```mermaid
stateDiagram-v2
    [*] --> STARTED: startSaga()

    STARTED --> RESERVE_INVENTORY: Step 1
    RESERVE_INVENTORY --> PROCESS_PAYMENT: reserve success
    RESERVE_INVENTORY --> COMPENSATING: reserve failed

    PROCESS_PAYMENT --> DEDUCT_INVENTORY: payment success
    PROCESS_PAYMENT --> COMPENSATING: payment failed

    DEDUCT_INVENTORY --> CREATE_DELIVERY: deduct success
    DEDUCT_INVENTORY --> COMPENSATING: deduct failed

    CREATE_DELIVERY --> CONFIRM_ORDER: delivery created
    CREATE_DELIVERY --> COMPENSATING: delivery failed

    CONFIRM_ORDER --> COMPLETED: confirm success
    CONFIRM_ORDER --> COMPENSATING: confirm failed

    COMPENSATING --> FAILED: compensation success
    COMPENSATING --> COMPENSATION_FAILED: 3회 실패

    COMPLETED --> [*]
    FAILED --> [*]
    COMPENSATION_FAILED --> [*]: 수동 개입 필요
```

---

## 핵심 컴포넌트

### SagaState 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| sagaId | String (UK) | `SAGA-{UUID 8자리}` |
| orderId | Long | 주문 ID |
| orderNumber | String | 주문 번호 |
| currentStep | SagaStep | 현재 단계 |
| status | SagaStatus | 현재 상태 |
| completedSteps | String | 완료된 단계 (CSV) |
| lastErrorMessage | String | 마지막 에러 메시지 |
| compensationAttempts | Integer | 보상 시도 횟수 (default 0) |
| startedAt | DateTime | 시작 일시 |
| completedAt | DateTime | 완료 일시 |

### SagaStep Enum

| Step | 이름 | 보상 가능 | 설명 |
|------|------|:---------:|------|
| 1 | RESERVE_INVENTORY | Yes | 재고 예약 (available -> reserved) |
| 2 | PROCESS_PAYMENT | Yes | 결제 처리 (외부 PG 호출) |
| 3 | DEDUCT_INVENTORY | Yes | 재고 차감 (reserved 감소, total 감소) |
| 4 | CREATE_DELIVERY | Yes | 배송 생성 |
| 5 | CONFIRM_ORDER | No | 주문 확정 (status = PAID) |

### SagaStatus Enum

| 상태 | 설명 |
|------|------|
| STARTED | 실행 중 |
| COMPLETED | 정상 완료 |
| COMPENSATING | 보상 처리 중 |
| FAILED | 실패 (보상 완료) |
| COMPENSATION_FAILED | 보상 실패 (수동 개입 필요) |

---

## 데이터 플로우

### Forward Flow (정상 진행)

```mermaid
sequenceDiagram
    participant OS as OrderService
    participant SAGA as OrderSagaOrchestrator
    participant IS as InventoryService
    participant PS as PaymentService
    participant DS as DeliveryService
    participant DB as MySQL

    Note over OS,DB: Phase 1: 주문 생성 시 (startSaga)
    OS->>SAGA: startSaga(order)
    SAGA->>DB: INSERT saga_states (STARTED)

    Note over SAGA,IS: Step 1: RESERVE_INVENTORY
    SAGA->>IS: reserveStockBatch(quantities, "ORDER", orderNumber, userId)
    IS->>DB: SELECT inventory FOR UPDATE
    IS->>DB: available -= qty, reserved += qty
    IS->>DB: INSERT stock_movement (RESERVATION)
    IS-->>SAGA: Success
    SAGA->>DB: completedSteps += "RESERVE_INVENTORY"
    SAGA->>DB: currentStep = PROCESS_PAYMENT

    Note over SAGA: Step 2: PROCESS_PAYMENT (대기)
    Note over SAGA: 클라이언트가 결제 API 호출할 때까지 대기

    Note over PS,DB: Phase 2: 결제 완료 후 (completeSagaAfterPayment)
    PS->>SAGA: completeSagaAfterPayment(orderNumber)
    SAGA->>DB: GET sagaState

    Note over SAGA,IS: Step 3: DEDUCT_INVENTORY
    SAGA->>IS: deductStockBatch(quantities, "ORDER", orderNumber, userId)
    IS->>DB: reserved -= qty, total -= qty
    IS->>DB: INSERT stock_movement (SALE)
    IS-->>SAGA: Success
    SAGA->>DB: completedSteps += "DEDUCT_INVENTORY"

    Note over SAGA,DS: Step 4: CREATE_DELIVERY
    SAGA->>DS: createDelivery(order)
    DS->>DB: INSERT delivery (PREPARING)
    DS-->>SAGA: Success
    SAGA->>DB: completedSteps += "CREATE_DELIVERY"

    Note over SAGA: Step 5: CONFIRM_ORDER
    SAGA->>DB: order.status = PAID
    SAGA->>DB: sagaState.status = COMPLETED
    SAGA->>DB: sagaState.completedAt = now
```

### Compensation Flow (보상 처리)

```mermaid
sequenceDiagram
    participant SAGA as OrderSagaOrchestrator
    participant IS as InventoryService
    participant DS as DeliveryService
    participant DB as MySQL

    Note over SAGA: 실패 발생 → compensate()
    Note over SAGA: @Transactional(REQUIRES_NEW)

    SAGA->>SAGA: 완료된 단계를 역순으로 보상

    alt CREATE_DELIVERY 완료됨
        SAGA->>DS: cancelDelivery(orderId)
    end

    alt DEDUCT_INVENTORY 완료됨
        SAGA->>SAGA: 로그 기록 (수동 개입 필요)
        Note over SAGA: 이미 차감된 재고는 자동 복원 불가
    end

    alt RESERVE_INVENTORY 완료됨
        SAGA->>IS: releaseStockBatch(quantities)
        IS->>DB: reserved -= qty, available += qty
        IS->>DB: INSERT stock_movement (RELEASE)
    end

    SAGA->>DB: order.status = CANCELLED
    SAGA->>DB: order.cancelReason = errorMessage
    SAGA->>DB: sagaState.status = FAILED
```

### Compensation 전략

```mermaid
flowchart TD
    A[Saga 실패 발생] --> B{어느 단계에서 실패?}

    B -->|RESERVE_INVENTORY| C[보상 불필요<br/>예약 안 됨]
    B -->|PROCESS_PAYMENT| D[재고 예약 해제]
    B -->|DEDUCT_INVENTORY| E[결제 취소 + 재고 예약 해제]
    B -->|CREATE_DELIVERY| F[배송 취소 + 수동 재고 복원]
    B -->|CONFIRM_ORDER| G[전체 롤백]

    C --> H[주문 취소]
    D --> H
    E --> H
    F --> I[관리자 알림]
    G --> H

    H --> J[SagaState = FAILED]
    I --> K[SagaState = COMPENSATION_FAILED]
```

**보상 실패 처리**:
1. compensationAttempts 증가
2. 최대 3회 재시도 (`MAX_COMPENSATION_ATTEMPTS = 3`)
3. 3회 실패 시 `SagaState.status = COMPENSATION_FAILED`
4. 수동 개입 필요 (관리자 알림)

---

## 기술적 결정

### Orchestrator 패턴을 선택한 이유

| 패턴 | 장점 | 단점 | 선택 여부 |
|------|------|------|:---------:|
| **Orchestrator Saga** | 중앙 제어, 흐름 파악 용이, 보상 관리 단순 | 단일 장애점 | **선택** |
| Choreography Saga | 분산, 결합도 낮음 | 흐름 추적 어려움, 보상 복잡 | - |
| 2PC (Two-Phase Commit) | 강한 일관성 | 성능 저하, 가용성 문제 | - |

### PROCESS_PAYMENT 분리

Step 2 (PROCESS_PAYMENT)는 다른 단계와 달리 비동기적으로 처리됩니다:
- `startSaga()`에서 Step 1까지 완료 후 클라이언트에 응답
- 클라이언트가 별도 API (`POST /payments`)로 결제 요청
- 결제 완료/실패 시 `completeSagaAfterPayment()` 또는 `compensate()` 호출

이유: 결제는 사용자 상호작용이 필요한 단계 (결제 수단 선택, PG 리다이렉트 등)

### 보상 트랜잭션 격리

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void compensate(SagaState sagaState, String errorMessage) { ... }
```

- `REQUIRES_NEW`: 원래 트랜잭션과 별도 트랜잭션에서 실행
- 원래 트랜잭션이 롤백되더라도 보상 트랜잭션은 커밋됨
- SagaState 업데이트가 유실되지 않도록 보장

---

## 에러 코드

| 코드 | 이름 | 설명 |
|------|------|------|
| S901 | SAGA_EXECUTION_FAILED | Saga 실행 실패 |
| S902 | SAGA_COMPENSATION_FAILED | Saga 보상 실패 |
| S903 | SAGA_NOT_FOUND | Saga 없음 |
| S904 | SAGA_ALREADY_COMPLETED | 이미 완료됨 |
| S905 | SAGA_TIMEOUT | Saga 타임아웃 |

---

## 모니터링 포인트

| 지표 | 의미 | 임계값 |
|------|------|--------|
| `sagaState.status = COMPENSATION_FAILED` | 수동 개입 필요 | > 0 즉시 알림 |
| `compensationAttempts >= 2` | 보상 재시도 중 | 모니터링 |
| Saga 완료 시간 | 주문 처리 성능 | p95 < 2초 |
| `status = STARTED` 이면서 30분 이상 경과 | 멈춘 Saga | 즉시 확인 |

---

## 관련 문서

- [System Overview](./system-overview.md)
- [Data Flow](./data-flow.md) - 결제 처리 및 Saga 완료 흐름
- [Coupon System](./coupon-system.md) - 쿠폰 적용 시 Saga 확장 예정

---

**최종 업데이트**: 2026-02-06
