# Order Saga (주문 Saga 패턴)

## 개요

분산 트랜잭션 환경에서 주문 처리를 위한 Saga Orchestration 패턴 구현을 설명합니다.
단계별 실행과 실패 시 보상(Compensation) 처리를 통해 데이터 일관성을 보장합니다.

## Saga Pattern 개요

### Choreography vs Orchestration

| 구분 | Choreography | Orchestration |
|------|--------------|---------------|
| 조정자 | 없음 (이벤트 기반) | 중앙 Orchestrator |
| 복잡도 | 단순한 플로우에 적합 | 복잡한 플로우에 적합 |
| 가시성 | 낮음 | 높음 (상태 추적 용이) |
| 결합도 | 느슨함 | 상대적으로 높음 |

**Shopping Service는 Orchestration 방식을 채택합니다.**

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     OrderSagaOrchestrator                           │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                        Saga 실행 흐름                         │  │
│  │  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐      │  │
│  │  │ RESERVE │──▶│ PROCESS │──▶│ DEDUCT  │──▶│ CONFIRM │      │  │
│  │  │INVENTORY│   │ PAYMENT │   │INVENTORY│   │  ORDER  │      │  │
│  │  └─────────┘   └─────────┘   └─────────┘   └─────────┘      │  │
│  │       │             │             │                          │  │
│  │       ▼             ▼             ▼                          │  │
│  │  ┌─────────┐   ┌─────────┐   ┌─────────┐                    │  │
│  │  │ RELEASE │◀──│ REFUND  │◀──│ (수동)  │    Compensation    │  │
│  │  │  STOCK  │   │ PAYMENT │   │         │                    │  │
│  │  └─────────┘   └─────────┘   └─────────┘                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Saga Steps

```java
public enum SagaStep {
    RESERVE_INVENTORY(1, "재고 예약", true),    // 보상 가능
    PROCESS_PAYMENT(2, "결제 처리", true),      // 보상 가능
    DEDUCT_INVENTORY(3, "재고 차감", true),     // 보상 가능 (수동 개입 필요)
    CREATE_DELIVERY(4, "배송 생성", true),      // 보상 가능
    CONFIRM_ORDER(5, "주문 확정", false);       // 최종 단계

    private final int order;
    private final String description;
    private final boolean compensatable;

    public SagaStep next() {
        SagaStep[] steps = values();
        for (int i = 0; i < steps.length - 1; i++) {
            if (steps[i] == this) return steps[i + 1];
        }
        return null;
    }

    public SagaStep previous() {
        SagaStep[] steps = values();
        for (int i = 1; i < steps.length; i++) {
            if (steps[i] == this) return steps[i - 1];
        }
        return null;
    }
}
```

## Saga Status

```java
public enum SagaStatus {
    STARTED("실행 중"),          // Saga 진행 중
    COMPLETED("완료"),           // 모든 단계 성공
    COMPENSATING("보상 처리 중"), // 롤백 진행 중
    FAILED("실패"),              // 실패 (보상 완료)
    COMPENSATION_FAILED("보상 실패");  // 롤백도 실패 (수동 개입 필요)
}
```

## SagaState Entity

```java
@Entity
@Table(name = "saga_states")
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;           // SAGA-XXXXXXXX

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Column(name = "completed_steps")
    private String completedSteps;   // "RESERVE_INVENTORY,PROCESS_PAYMENT,..."

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "compensation_attempts")
    private Integer compensationAttempts;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public void proceedToNextStep() {
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

    public boolean isStepCompleted(SagaStep step) {
        return this.completedSteps.contains(step.name());
    }
}
```

## OrderSagaOrchestrator

### Saga 시작 (주문 생성 시)

```java
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * Saga를 시작합니다 (주문 생성 시 호출).
     * 재고 예약 단계까지만 실행합니다.
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
            throw new CustomBusinessException(SAGA_EXECUTION_FAILED);
        }
    }
}
```

### 결제 완료 후 나머지 단계 실행

```java
/**
 * 결제 완료 후 나머지 Saga 단계를 실행합니다.
 */
@Transactional
public void completeSagaAfterPayment(String orderNumber) {
    SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(SAGA_NOT_FOUND));

    Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

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

        log.info("Saga {} completed successfully for order: {}",
                sagaState.getSagaId(), orderNumber);

    } catch (Exception e) {
        log.error("Saga {} - Failed after payment: {}", sagaState.getSagaId(), e.getMessage());
        compensate(sagaState, e.getMessage());
        throw new CustomBusinessException(SAGA_EXECUTION_FAILED);
    }
}
```

### 보상 (Compensation) 처리

```java
/**
 * Saga 보상(롤백)을 수행합니다.
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void compensate(SagaState sagaState, String errorMessage) {
    log.info("Starting compensation for saga {}: {}", sagaState.getSagaId(), errorMessage);

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
            // 재고 차감 보상: 이미 차감된 재고는 자동 복원 불가
            log.warn("Saga {} - Deducted inventory requires manual intervention",
                    sagaState.getSagaId());
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
        log.error("Saga {} - Compensation failed: {}", sagaState.getSagaId(), e.getMessage());
        sagaState.incrementCompensationAttempts();

        if (sagaState.getCompensationAttempts() >= MAX_COMPENSATION_ATTEMPTS) {
            sagaState.markAsCompensationFailed(e.getMessage());
            log.error("Saga {} - Max compensation attempts reached, requires manual intervention",
                    sagaState.getSagaId());
        }

        sagaStateRepository.save(sagaState);
    }
}
```

### 각 단계 실행 메서드

```java
/**
 * Step 1: 재고 예약 실행
 */
private void executeReserveInventory(Order order, SagaState sagaState) {
    log.debug("Saga {} - Executing step: RESERVE_INVENTORY", sagaState.getSagaId());

    Map<Long, Integer> quantities = order.getItems().stream()
            .collect(Collectors.toMap(
                    OrderItem::getProductId,
                    OrderItem::getQuantity,
                    Integer::sum
            ));

    inventoryService.reserveStockBatch(
            quantities,
            "ORDER",
            order.getOrderNumber(),
            order.getUserId()
    );
}

/**
 * Step 3: 재고 차감 실행
 */
private void executeDeductInventory(Order order, SagaState sagaState) {
    log.debug("Saga {} - Executing step: DEDUCT_INVENTORY", sagaState.getSagaId());

    Map<Long, Integer> quantities = order.getItems().stream()
            .collect(Collectors.toMap(
                    OrderItem::getProductId,
                    OrderItem::getQuantity,
                    Integer::sum
            ));

    inventoryService.deductStockBatch(
            quantities,
            "ORDER",
            order.getOrderNumber(),
            order.getUserId()
    );
}

/**
 * Step 1 보상: 재고 예약 해제
 */
private void compensateReserveInventory(Order order, SagaState sagaState) {
    log.debug("Saga {} - Compensating step: RESERVE_INVENTORY", sagaState.getSagaId());

    Map<Long, Integer> quantities = order.getItems().stream()
            .collect(Collectors.toMap(
                    OrderItem::getProductId,
                    OrderItem::getQuantity,
                    Integer::sum
            ));

    inventoryService.releaseStockBatch(
            quantities,
            "ORDER_CANCEL",
            order.getOrderNumber(),
            "SYSTEM"
    );
}
```

## Saga 실행 흐름 다이어그램

### 정상 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        정상 주문 처리                            │
├─────────────────────────────────────────────────────────────────┤
│  1. createOrder() 호출                                          │
│      │                                                          │
│      ▼                                                          │
│  2. startSaga() - 재고 예약                                     │
│      │  SagaState: STARTED, Step: RESERVE_INVENTORY → PROCESS   │
│      │                                                          │
│      ▼                                                          │
│  3. processPayment() 호출 (결제 화면으로 이동)                   │
│      │                                                          │
│      ▼                                                          │
│  4. PG사 결제 완료                                              │
│      │                                                          │
│      ▼                                                          │
│  5. completeSagaAfterPayment()                                  │
│      │  - 재고 차감                                             │
│      │  - 주문 상태 PAID로 변경                                 │
│      │  SagaState: COMPLETED                                    │
│      ▼                                                          │
│  6. 배송 생성 (별도 프로세스)                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 실패 & 보상 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        결제 후 실패                              │
├─────────────────────────────────────────────────────────────────┤
│  1. completeSagaAfterPayment() 호출                             │
│      │                                                          │
│      ▼                                                          │
│  2. 재고 차감 시도 → 실패!                                      │
│      │                                                          │
│      ▼                                                          │
│  3. compensate() 호출                                           │
│      │  SagaState: COMPENSATING                                 │
│      │                                                          │
│      ▼                                                          │
│  4. 역순 보상 실행                                              │
│      │  - 재고 예약 해제                                        │
│      │  - 결제 환불 (PaymentService 호출)                       │
│      │  - 주문 취소                                             │
│      │                                                          │
│      ▼                                                          │
│  5. SagaState: FAILED                                           │
│      │  (또는 보상도 실패 시 COMPENSATION_FAILED)                │
│      ▼                                                          │
│  6. 사용자에게 주문 실패 알림                                    │
└─────────────────────────────────────────────────────────────────┘
```

## 모니터링 및 복구

### COMPENSATION_FAILED 상태 처리

보상도 실패한 Saga는 수동 개입이 필요합니다.

```java
// 관리자 API: 실패한 Saga 조회
@GetMapping("/admin/sagas/failed")
public List<SagaStateResponse> getFailedSagas() {
    return sagaStateRepository.findByStatusIn(
        List.of(SagaStatus.COMPENSATION_FAILED)
    ).stream()
     .map(SagaStateResponse::from)
     .toList();
}

// 관리자 API: 수동 보상 재시도
@PostMapping("/admin/sagas/{sagaId}/retry-compensation")
public void retryCompensation(@PathVariable String sagaId) {
    SagaState saga = sagaStateRepository.findBySagaId(sagaId)
            .orElseThrow();
    orderSagaOrchestrator.compensate(saga, "Manual retry");
}
```

## 관련 파일

- `/order/saga/OrderSagaOrchestrator.java` - Saga Orchestrator
- `/order/saga/SagaState.java` - Saga 상태 엔티티
- `/order/saga/SagaStep.java` - Saga 단계 enum
- `/order/saga/SagaStatus.java` - Saga 상태 enum
- `/order/service/OrderServiceImpl.java` - 주문 서비스
- `/inventory/service/InventoryServiceImpl.java` - 재고 서비스
