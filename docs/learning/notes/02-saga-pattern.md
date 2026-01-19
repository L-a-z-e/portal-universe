# Saga 패턴 분석

## 개요

분산 트랜잭션 환경에서 데이터 일관성을 유지하기 위한 Saga 패턴을 분석합니다. shopping-service에서는 주문 생성 과정을 Saga Orchestration 패턴으로 구현했습니다.

---

## 1. Saga 패턴이란?

### 기존 문제: 분산 트랜잭션

마이크로서비스 환경에서는 하나의 비즈니스 트랜잭션이 여러 서비스에 걸쳐 실행됩니다:

```
주문 생성 = 재고 예약 + 결제 처리 + 배송 생성
                ↓           ↓           ↓
           Inventory    Payment     Delivery
```

기존 ACID 트랜잭션으로는 여러 서비스의 데이터를 원자적으로 처리할 수 없습니다.

### Saga 해결책

Saga는 **일련의 로컬 트랜잭션**으로 구성되며, 각 트랜잭션이 실패하면 **보상 트랜잭션(Compensation)**을 역순으로 실행하여 롤백합니다.

```
성공 흐름: T1 → T2 → T3 → T4 → T5 (Commit)

실패 흐름: T1 → T2 → T3 (실패)
           ↓
보상 흐름: C2 → C1 (Rollback)
```

---

## 2. Orchestration vs Choreography

### Orchestration (중앙 조정자)

```
        ┌──────────────────────────┐
        │    OrderSagaOrchestrator │  ◄── 중앙 조정자
        └──────────────────────────┘
              │    │    │    │
              ▼    ▼    ▼    ▼
           ┌────┐┌────┐┌────┐┌────┐
           │Inv.││Pay.││Del.││Ord.│
           └────┘└────┘└────┘└────┘
```

**장점:**
- 플로우 로직이 한 곳에 집중
- 디버깅 및 모니터링 용이
- 복잡한 비즈니스 로직 처리 가능

**단점:**
- 중앙 조정자가 단일 장애점(SPOF)이 될 수 있음
- 조정자에 비즈니스 로직 집중

### Choreography (이벤트 기반)

```
    ┌────┐  event  ┌────┐  event  ┌────┐  event  ┌────┐
    │Inv.│────────►│Pay.│────────►│Del.│────────►│Ord.│
    └────┘         └────┘         └────┘         └────┘
      ▲              │              │              │
      └──────────────┴──────────────┴──────────────┘
                compensation events
```

**장점:**
- 느슨한 결합
- 서비스 독립성 유지

**단점:**
- 플로우 추적 어려움
- 순환 의존성 발생 가능

### 프로젝트 선택: Orchestration

shopping-service는 **Orchestration 패턴**을 선택했습니다:

1. 주문 플로우가 명확한 순서를 가짐
2. 실패 시 보상 로직이 복잡함
3. 상태 추적 및 모니터링 필요

---

## 3. Saga 실행 흐름 (5단계)

**파일**: `order/saga/OrderSagaOrchestrator.java`

### 정상 흐름

```
Step 1: RESERVE_INVENTORY  ──►  재고 예약 (가용→예약)
            │
            ▼
Step 2: PROCESS_PAYMENT    ──►  결제 처리 (별도 호출)
            │
            ▼
Step 3: DEDUCT_INVENTORY   ──►  재고 차감 (예약→차감)
            │
            ▼
Step 4: CREATE_DELIVERY    ──►  배송 생성
            │
            ▼
Step 5: CONFIRM_ORDER      ──►  주문 확정
```

### 코드 분석: startSaga()

```java
@Transactional
public SagaState startSaga(Order order) {
    log.info("Starting saga for order: {}", order.getOrderNumber());

    // 1. Saga 상태 생성
    SagaState sagaState = SagaState.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .build();
    sagaState = sagaStateRepository.save(sagaState);

    try {
        // Step 1: Reserve Inventory
        executeReserveInventory(order, sagaState);
        sagaState.proceedToNextStep();
        sagaStateRepository.save(sagaState);

        return sagaState;
    } catch (Exception e) {
        compensate(sagaState, e.getMessage());
        throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
    }
}
```

### 코드 분석: completeSagaAfterPayment()

```java
@Transactional
public void completeSagaAfterPayment(String orderNumber) {
    SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.SAGA_NOT_FOUND));

    Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

    try {
        // Step 3: Deduct Inventory (결제 완료 후)
        executeDeductInventory(order, sagaState);
        sagaState.proceedToNextStep();

        // Step 4: Create Delivery (별도 서비스에서 처리)
        sagaState.proceedToNextStep();

        // Step 5: Confirm Order
        order.markAsPaid();
        orderRepository.save(order);

        // Saga 완료
        sagaState.complete();
        sagaStateRepository.save(sagaState);
    } catch (Exception e) {
        compensate(sagaState, e.getMessage());
        throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
    }
}
```

---

## 4. 보상 트랜잭션 (Compensation)

### 역순 실행 원칙

실패 지점부터 **완료된 단계들을 역순**으로 보상합니다:

```
실패 예: Step 3 (DEDUCT_INVENTORY) 실패

보상 순서:
  ← Step 1: RESERVE_INVENTORY 보상 (예약 해제)
  ← Order 취소
```

### 코드 분석: compensate()

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)  // 새 트랜잭션
public void compensate(SagaState sagaState, String errorMessage) {
    log.info("Starting compensation for saga {}", sagaState.getSagaId());

    sagaState.startCompensation(errorMessage);
    sagaStateRepository.save(sagaState);

    Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
            .orElse(null);

    if (order == null) {
        sagaState.markAsFailed("Order not found during compensation");
        return;
    }

    try {
        // 완료된 단계들을 역순으로 보상
        if (sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY)) {
            // 이미 차감된 재고는 자동 복원 불가 (반품 처리 필요)
            log.warn("Deducted inventory requires manual intervention");
        }

        if (sagaState.isStepCompleted(SagaStep.RESERVE_INVENTORY)) {
            compensateReserveInventory(order, sagaState);  // 예약 해제
        }

        // 주문 취소
        if (order.getStatus().isCancellable()) {
            order.cancel("Saga compensation: " + errorMessage);
            orderRepository.save(order);
        }

        sagaState.markAsFailed(errorMessage);
        sagaStateRepository.save(sagaState);

    } catch (Exception e) {
        sagaState.incrementCompensationAttempts();
        if (sagaState.getCompensationAttempts() >= MAX_COMPENSATION_ATTEMPTS) {
            sagaState.markAsCompensationFailed(e.getMessage());
            log.error("Max compensation attempts reached, requires manual intervention");
        }
    }
}
```

### 핵심 포인트

1. **REQUIRES_NEW**: 보상은 새 트랜잭션에서 실행 (원래 트랜잭션 롤백과 분리)
2. **idempotency**: 보상 재시도 시에도 안전해야 함
3. **수동 개입**: DEDUCT 이후 실패는 자동 복구 불가

---

## 5. SagaState를 통한 상태 추적

**파일**: `order/saga/SagaState.java`

### 엔티티 구조

```java
@Entity
@Table(name = "saga_states")
public class SagaState {
    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;  // SAGA-A1B2C3D4

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;  // 현재 단계

    @Enumerated(EnumType.STRING)
    private SagaStatus status;  // STARTED, COMPLETED, COMPENSATING, FAILED

    @Column(name = "completed_steps")
    private String completedSteps;  // "RESERVE_INVENTORY,PROCESS_PAYMENT"

    @Column(name = "compensation_attempts")
    private Integer compensationAttempts;  // 보상 시도 횟수
}
```

### SagaStep 열거형

```java
public enum SagaStep {
    RESERVE_INVENTORY(1, "재고 예약", true),     // 보상 가능
    PROCESS_PAYMENT(2, "결제 처리", true),       // 보상 가능
    DEDUCT_INVENTORY(3, "재고 차감", true),      // 보상 가능 (제한적)
    CREATE_DELIVERY(4, "배송 생성", true),       // 보상 가능
    CONFIRM_ORDER(5, "주문 확정", false);        // 보상 불가 (최종 단계)

    public SagaStep next() { /* 다음 단계 반환 */ }
    public SagaStep previous() { /* 이전 단계 반환 */ }
    public boolean isCompensatable() { return compensatable; }
}
```

### SagaStatus 열거형

```java
public enum SagaStatus {
    STARTED("실행 중"),           // Saga 진행 중
    COMPLETED("완료"),            // 모든 단계 성공
    COMPENSATING("보상 처리 중"), // 롤백 진행 중
    FAILED("실패"),               // Saga 실패 (보상 완료)
    COMPENSATION_FAILED("보상 실패");  // 수동 개입 필요
}
```

---

## 6. 상태 전이 다이어그램

```
                                    ┌───────────────┐
                         성공       │   COMPLETED   │
            ┌──────────────────────►│     (완료)     │
            │                       └───────────────┘
            │
       ┌────┴────┐        실패      ┌───────────────┐
       │ STARTED │─────────────────►│ COMPENSATING  │
       │ (진행중) │                  │   (보상 중)    │
       └─────────┘                  └───────┬───────┘
                                            │
                          ┌─────────────────┼─────────────────┐
                          │ 보상 성공        │ 보상 실패        │
                          ▼                 │                 ▼
                   ┌───────────┐            │        ┌────────────────┐
                   │  FAILED   │            │        │ COMPENSATION_  │
                   │   (실패)   │◄───────────┘        │    FAILED      │
                   └───────────┘                     │  (보상 실패)    │
                                                     └────────────────┘
                                                            │
                                                     수동 개입 필요
```

---

## 7. 실패 시나리오 분석

### 시나리오 1: 재고 예약 실패

```
Step 1: RESERVE_INVENTORY → 실패 (재고 부족)

보상: 없음 (완료된 단계 없음)
결과: 주문 생성 실패, 사용자에게 재고 부족 안내
```

### 시나리오 2: 결제 실패

```
Step 1: RESERVE_INVENTORY → 성공
Step 2: PROCESS_PAYMENT → 실패 (카드 한도 초과)

보상:
  - Step 1 보상: 예약된 재고 해제 (release)
  - 주문 취소

결과: Order.status = CANCELLED, Inventory 복원
```

### 시나리오 3: 재고 차감 실패

```
Step 1: RESERVE_INVENTORY → 성공
Step 2: PROCESS_PAYMENT → 성공
Step 3: DEDUCT_INVENTORY → 실패 (DB 오류)

보상:
  - 결제 취소 (환불) - 외부 PG 연동 필요
  - Step 1 보상: 예약된 재고 해제
  - 주문 취소

결과: 복잡한 보상 필요, 수동 개입 가능
```

---

## 8. 핵심 설계 결정

### 8.1 결제 분리

```java
// startSaga()에서 재고 예약까지만 실행
// 결제는 사용자 상호작용 필요 (카드 정보 입력 등)
// completeSagaAfterPayment()에서 나머지 실행
```

**이유:**
- 결제는 비동기 외부 시스템 연동
- 사용자 인증/승인 필요
- 타임아웃 가능성

### 8.2 보상 재시도 횟수 제한

```java
private static final int MAX_COMPENSATION_ATTEMPTS = 3;

if (sagaState.getCompensationAttempts() >= MAX_COMPENSATION_ATTEMPTS) {
    sagaState.markAsCompensationFailed(e.getMessage());
    log.error("Requires manual intervention");
}
```

**이유:**
- 무한 재시도 방지
- Dead Letter Queue와 유사한 개념
- 운영팀 알림 및 수동 처리

### 8.3 DEDUCT 이후 실패 처리

```java
if (sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY)) {
    log.warn("Deducted inventory cannot be auto-restored, requires manual intervention");
}
```

**이유:**
- 재고 차감은 물리적 출고와 연결될 수 있음
- 자동 복구 시 재고 불일치 가능
- 반품/환불 프로세스로 처리 필요

---

## 9. 핵심 파일 요약

| 파일 | 역할 |
|------|------|
| `OrderSagaOrchestrator.java` | Saga 조정자, 단계 실행 및 보상 |
| `SagaState.java` | Saga 실행 상태 추적 엔티티 |
| `SagaStep.java` | 실행 단계 정의 |
| `SagaStatus.java` | Saga 상태 정의 |
| `SagaStateRepository.java` | 상태 영속화 |

---

## 10. 핵심 요약

1. **Orchestration 선택**: 명확한 플로우, 상태 추적 용이
2. **5단계 실행**: Reserve → Payment → Deduct → Delivery → Confirm
3. **역순 보상**: 실패 시 완료된 단계를 역순으로 롤백
4. **상태 영속화**: SagaState로 실행 상태 추적
5. **재시도 제한**: 3회 실패 시 수동 개입 필요
6. **결제 분리**: 비동기 외부 연동을 위해 단계 분리
