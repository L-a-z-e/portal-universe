# Saga 패턴 심화

## 학습 목표
- Saga 패턴의 원리와 필요성 이해
- Orchestration vs Choreography 비교
- Portal Universe의 Saga 구현 분석

---

## 1. Saga 패턴 개요

### 1.1 분산 트랜잭션의 문제

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    DISTRIBUTED TRANSACTION PROBLEM                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   전통적인 단일 DB 트랜잭션:                                                 │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │  BEGIN TRANSACTION                                                   │  │
│   │    INSERT INTO orders ...                                            │  │
│   │    UPDATE inventory ...                                              │  │
│   │    INSERT INTO payments ...                                          │  │
│   │  COMMIT (or ROLLBACK)                                                │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│   마이크로서비스 환경:                                                       │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│   │ Order DB    │    │ Inventory DB│    │ Payment DB  │                     │
│   │ (Shopping)  │    │ (Shopping)  │    │ (Shopping)  │                     │
│   └─────────────┘    └─────────────┘    └─────────────┘                     │
│         │                  │                  │                              │
│         └──────────────────┴──────────────────┘                              │
│                     How to ensure consistency?                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Saga 패턴의 정의

**Saga**는 각 서비스의 로컬 트랜잭션 시퀀스입니다:
- 각 단계는 자신의 DB 트랜잭션을 완료
- 실패 시 이전 단계들을 **보상(Compensation)** 트랜잭션으로 롤백

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SAGA PATTERN                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   정상 흐름:                                                                 │
│   T1 ──────► T2 ──────► T3 ──────► T4 ──────► 완료                          │
│   (재고예약)   (결제처리)   (재고차감)   (주문확정)                           │
│                                                                              │
│   실패 시 보상:                                                              │
│   T1 ──────► T2 ──────► T3 ──X                                              │
│   │          │          │                                                    │
│   │          │          └── C3 (재고 차감 취소)                              │
│   │          └───────────── C2 (결제 취소)                                   │
│   └──────────────────────── C1 (재고 예약 해제)                              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Orchestration vs Choreography

### 2.1 비교

| 특성 | Orchestration | Choreography |
|------|---------------|--------------|
| **제어 방식** | 중앙 집중 (Orchestrator) | 분산 (이벤트 기반) |
| **결합도** | 높음 (Orchestrator가 모든 서비스 알아야) | 낮음 (이벤트만 구독) |
| **흐름 파악** | 쉬움 (한 곳에서 관리) | 어려움 (이벤트 추적 필요) |
| **장애 처리** | 명시적 보상 로직 | 보상 이벤트 발행 |
| **테스트** | 단위 테스트 용이 | 통합 테스트 필요 |

### 2.2 Orchestration (Portal Universe 사용)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ORCHESTRATION PATTERN                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                     ┌─────────────────────┐                                  │
│                     │   Orchestrator      │                                  │
│                     │ (OrderSagaOrchestrator)                                │
│                     └─────────────────────┘                                  │
│                              │                                               │
│         ┌────────────────────┼────────────────────┐                          │
│         │                    │                    │                          │
│         ▼                    ▼                    ▼                          │
│   ┌───────────┐       ┌───────────┐       ┌───────────┐                      │
│   │ Inventory │       │  Payment  │       │   Order   │                      │
│   │  Service  │       │  Service  │       │  Service  │                      │
│   └───────────┘       └───────────┘       └───────────┘                      │
│                                                                              │
│   장점:                                                                      │
│   • 비즈니스 흐름이 한 곳에 집중                                              │
│   • 보상 로직이 명확                                                         │
│   • 디버깅/모니터링 용이                                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Choreography

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CHOREOGRAPHY PATTERN                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌───────────┐     OrderCreated      ┌───────────┐                          │
│   │   Order   │ ─────────────────────►│ Inventory │                          │
│   │  Service  │                       │  Service  │                          │
│   └───────────┘                       └───────────┘                          │
│         ▲                                   │                                │
│         │                                   │ InventoryReserved              │
│         │                                   ▼                                │
│         │                             ┌───────────┐                          │
│         │                             │  Payment  │                          │
│         │                             │  Service  │                          │
│         │                             └───────────┘                          │
│         │                                   │                                │
│         └───────── PaymentCompleted ────────┘                                │
│                                                                              │
│   단점:                                                                      │
│   • 전체 흐름 파악 어려움                                                    │
│   • 이벤트 순서 보장 필요                                                    │
│   • 순환 참조 가능성                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Portal Universe Saga 구현

### 3.1 Saga 단계 정의

```java
public enum SagaStep {
    RESERVE_INVENTORY,   // 1. 재고 예약
    PROCESS_PAYMENT,     // 2. 결제 처리
    DEDUCT_INVENTORY,    // 3. 재고 차감
    CREATE_DELIVERY,     // 4. 배송 생성
    CONFIRM_ORDER;       // 5. 주문 확정

    public SagaStep next() {
        SagaStep[] steps = values();
        int nextIndex = this.ordinal() + 1;
        return nextIndex < steps.length ? steps[nextIndex] : null;
    }

    public SagaStep previous() {
        int prevIndex = this.ordinal() - 1;
        return prevIndex >= 0 ? values()[prevIndex] : null;
    }
}
```

### 3.2 Saga 상태 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SAGA STATE DIAGRAM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                            ┌─────────┐                                       │
│                            │ STARTED │                                       │
│                            └────┬────┘                                       │
│                                 │                                            │
│                                 │ 실행 중                                    │
│                                 ▼                                            │
│   ┌──────────────┐       ┌────────────┐       ┌───────────┐                  │
│   │ COMPENSATING │◄──────│  RUNNING   │──────►│ COMPLETED │                  │
│   │              │ 실패  └────────────┘ 성공  │           │                  │
│   └──────┬───────┘                            └───────────┘                  │
│          │                                                                   │
│          │ 보상 결과                                                         │
│          ▼                                                                   │
│   ┌────────────────────┐                                                     │
│   │      FAILED        │  보상 성공 (정상 롤백)                              │
│   └────────────────────┘                                                     │
│          │                                                                   │
│          │ 보상 실패 (MAX_ATTEMPTS 초과)                                     │
│          ▼                                                                   │
│   ┌────────────────────┐                                                     │
│   │ COMPENSATION_FAILED│  수동 개입 필요                                     │
│   └────────────────────┘                                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Orchestrator 구현

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * Saga 시작: 주문 생성 시 호출
     */
    @Transactional
    public SagaState startSaga(Order order) {
        log.info("Starting saga for order: {}", order.getOrderNumber());

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
            log.error("Saga {} - Failed at step {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());
            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * 결제 완료 후 Saga 계속 실행
     */
    @Transactional
    public void completeSagaAfterPayment(String orderNumber) {
        SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(
                    ShoppingErrorCode.SAGA_NOT_FOUND));

        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(
                    ShoppingErrorCode.ORDER_NOT_FOUND));

        try {
            // Step 3: Deduct Inventory
            executeDeductInventory(order, sagaState);
            sagaState.proceedToNextStep();

            // Step 4: Create Delivery (별도 서비스)
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
}
```

### 3.4 보상(Compensation) 로직

```java
/**
 * Saga 보상(롤백)
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void compensate(SagaState sagaState, String errorMessage) {
    log.info("Starting compensation for saga {}: {}",
            sagaState.getSagaId(), errorMessage);

    sagaState.startCompensation(errorMessage);
    sagaStateRepository.save(sagaState);

    Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
            .orElse(null);

    if (order == null) {
        sagaState.markAsFailed("Order not found during compensation");
        sagaStateRepository.save(sagaState);
        return;
    }

    try {
        // 완료된 단계들을 역순으로 보상
        if (sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY)) {
            // 재고 차감 보상: 수동 개입 필요
            log.warn("Deducted inventory requires manual intervention");
        }

        if (sagaState.isStepCompleted(SagaStep.RESERVE_INVENTORY)) {
            compensateReserveInventory(order, sagaState);
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

        sagaStateRepository.save(sagaState);
    }
}
```

---

## 4. Saga State Entity

### 4.1 영속성 설계

```java
@Entity
@Table(name = "saga_states", indexes = {
        @Index(name = "idx_saga_order_id", columnList = "order_id"),
        @Index(name = "idx_saga_status", columnList = "status")
})
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;  // "SAGA-XXXXXXXX"

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private SagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;

    /**
     * 완료된 단계들 (쉼표로 구분)
     */
    @Column(name = "completed_steps")
    private String completedSteps;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "compensation_attempts")
    private Integer compensationAttempts;

    @CreatedDate
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
```

### 4.2 상태 전이 메서드

```java
/**
 * 다음 단계로 진행
 */
public void proceedToNextStep() {
    // 현재 단계를 완료 목록에 추가
    if (this.completedSteps.isEmpty()) {
        this.completedSteps = this.currentStep.name();
    } else {
        this.completedSteps += "," + this.currentStep.name();
    }

    SagaStep nextStep = this.currentStep.next();
    if (nextStep != null) {
        this.currentStep = nextStep;
    }
}

/**
 * 특정 단계 완료 여부 확인
 */
public boolean isStepCompleted(SagaStep step) {
    return this.completedSteps.contains(step.name());
}
```

---

## 5. 주문 생성 Saga 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ORDER CREATION SAGA FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Client                OrderService        Orchestrator        Services    │
│     │                       │                    │                  │        │
│     │ POST /orders          │                    │                  │        │
│     ├──────────────────────►│                    │                  │        │
│     │                       │                    │                  │        │
│     │                       │ Order 생성 (PENDING)                  │        │
│     │                       │                    │                  │        │
│     │                       │ startSaga(order)   │                  │        │
│     │                       ├───────────────────►│                  │        │
│     │                       │                    │                  │        │
│     │                       │                    │ reserveStock     │        │
│     │                       │                    ├─────────────────►│        │
│     │                       │                    │◄─────────────────┤        │
│     │                       │                    │                  │        │
│     │                       │◄───────────────────┤ Saga 시작됨      │        │
│     │◄──────────────────────┤                    │                  │        │
│     │ 201 Created           │                    │                  │        │
│     │ (결제 대기)            │                    │                  │        │
│     │                       │                    │                  │        │
│     │ POST /payments        │                    │                  │        │
│     ├──────────────────────►│ (PaymentService)   │                  │        │
│     │                       │                    │                  │        │
│     │                       │  completeSagaAfterPayment            │        │
│     │                       ├───────────────────►│                  │        │
│     │                       │                    │ deductStock      │        │
│     │                       │                    ├─────────────────►│        │
│     │                       │                    │                  │        │
│     │                       │                    │ markAsPaid       │        │
│     │                       │                    ├─────────────────►│        │
│     │                       │                    │                  │        │
│     │                       │◄───────────────────┤ Saga 완료        │        │
│     │◄──────────────────────┤                    │                  │        │
│     │ 200 OK (결제 완료)     │                    │                  │        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 실패 시나리오와 보상

### 6.1 재고 예약 실패

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                SCENARIO: Inventory Reservation Failed                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. 주문 생성                                                               │
│   2. Saga 시작                                                               │
│   3. 재고 예약 시도 → 실패 (재고 부족)                                       │
│   4. 보상 실행                                                               │
│      - 완료된 단계 없음 → 보상 불필요                                        │
│   5. 주문 취소                                                               │
│   6. Saga 상태: FAILED                                                       │
│                                                                              │
│   Result:                                                                    │
│   • Order.status = CANCELLED                                                 │
│   • Saga.status = FAILED                                                     │
│   • 재고 변화 없음                                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 결제 실패

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                SCENARIO: Payment Failed                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. 주문 생성 ✓                                                             │
│   2. Saga 시작 ✓                                                             │
│   3. 재고 예약 ✓ (completedSteps: "RESERVE_INVENTORY")                       │
│   4. 결제 처리 → 실패 (카드 한도 초과)                                       │
│   5. 보상 실행                                                               │
│      - RESERVE_INVENTORY 보상: 재고 예약 해제                                │
│   6. 주문 취소                                                               │
│   7. Saga 상태: FAILED                                                       │
│                                                                              │
│   Compensation:                                                              │
│   inventoryService.releaseStockBatch(                                        │
│       quantities,                                                            │
│       "ORDER_CANCEL",                                                        │
│       orderNumber,                                                           │
│       "SYSTEM"                                                               │
│   );                                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 보상 실패

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                SCENARIO: Compensation Failed                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. 주문 처리 중 실패                                                       │
│   2. 보상 시도 1 → 실패 (DB 연결 오류)                                       │
│   3. compensationAttempts = 1                                                │
│   4. 보상 시도 2 → 실패                                                      │
│   5. compensationAttempts = 2                                                │
│   6. 보상 시도 3 → 실패                                                      │
│   7. compensationAttempts = 3 (MAX_COMPENSATION_ATTEMPTS)                    │
│   8. Saga 상태: COMPENSATION_FAILED                                          │
│                                                                              │
│   Alert:                                                                     │
│   log.error("Max compensation attempts reached, requires manual intervention")│
│                                                                              │
│   운영팀 수동 처리 필요:                                                     │
│   • 재고 수동 조정                                                           │
│   • 주문 상태 수동 변경                                                      │
│   • 고객 안내                                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 모니터링과 디버깅

### 7.1 Saga 상태 조회

```sql
-- 실패한 Saga 목록
SELECT saga_id, order_number, current_step, status,
       last_error_message, compensation_attempts
FROM saga_states
WHERE status IN ('FAILED', 'COMPENSATION_FAILED')
ORDER BY started_at DESC;

-- 진행 중인 Saga
SELECT saga_id, order_number, current_step, started_at
FROM saga_states
WHERE status IN ('STARTED', 'COMPENSATING')
  AND started_at < NOW() - INTERVAL 1 HOUR;  -- 1시간 이상 진행 중
```

### 7.2 로그 패턴

```
INFO  Starting saga for order: ORD-XXXXXXXX
INFO  Saga SAGA-XXXXXXXX - Inventory reserved successfully
INFO  Continuing saga SAGA-XXXXXXXX after payment
INFO  Saga SAGA-XXXXXXXX completed successfully

ERROR Saga SAGA-XXXXXXXX - Failed at step PROCESS_PAYMENT: Card declined
INFO  Starting compensation for saga SAGA-XXXXXXXX: Card declined
INFO  Saga SAGA-XXXXXXXX compensation completed
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Saga** | 분산 환경에서의 Long-running 트랜잭션 |
| **Orchestration** | 중앙 집중식 Saga 관리 (Portal Universe 사용) |
| **Choreography** | 이벤트 기반 분산 Saga |
| **Compensation** | 실패 시 역순으로 롤백하는 보상 트랜잭션 |
| **SagaState** | Saga 진행 상태 추적 엔티티 |
| **MAX_ATTEMPTS** | 보상 재시도 최대 횟수 |

---

## 9. 설계 결정 이유

### Orchestration 선택 이유

1. **명확한 비즈니스 흐름**: 주문 처리 로직이 한 곳에 집중
2. **보상 로직 관리**: 각 단계별 명시적 롤백 정의 가능
3. **모니터링 용이**: SagaState 테이블로 상태 추적
4. **디버깅 편의**: 실패 지점과 원인 파악 용이

### 단점 및 주의사항

1. **Orchestrator가 SPOF**: 고가용성 필요
2. **결합도**: 모든 서비스 의존성을 알아야 함
3. **확장성**: 새 단계 추가 시 Orchestrator 수정 필요

---

## 다음 학습

- [State Machine 패턴](./state-machine-pattern.md)
- [Circuit Breaker 패턴](./circuit-breaker-resilience.md)
- [Outbox 패턴](./outbox-pattern.md)
