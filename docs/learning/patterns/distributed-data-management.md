# Distributed Data Management

## 학습 목표

- Database per Service 패턴의 원칙과 트레이드오프 이해
- CAP Theorem과 Eventual Consistency 개념 습득
- 분산 트랜잭션 패턴 (2PC, Saga Pattern) 학습
- Portal Universe의 Polyglot Persistence 전략 분석

---

## 1. Database per Service 원칙

마이크로서비스 아키텍처에서 **각 서비스는 자신만의 데이터베이스를 소유**합니다. 이는 서비스 간 느슨한 결합을 보장하는 핵심 원칙입니다.

### 기본 원칙

```
┌─────────────────────────────────────────────────────────────────┐
│                     Shared Database (Anti-pattern)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ Service A│  │ Service B│  │ Service C│                      │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘                      │
│        │             │             │                            │
│        └─────────────┼─────────────┘                            │
│                      ▼                                          │
│              ┌──────────────┐                                   │
│              │   Database   │  ← 모든 서비스가 공유             │
│              └──────────────┘                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                Database per Service (Recommended)               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ Service A│  │ Service B│  │ Service C│                      │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘                      │
│        │             │             │                            │
│        ▼             ▼             ▼                            │
│   ┌────────┐   ┌────────┐   ┌────────┐                         │
│   │  DB A  │   │  DB B  │   │  DB C  │  ← 서비스별 독립 DB      │
│   └────────┘   └────────┘   └────────┘                         │
└─────────────────────────────────────────────────────────────────┘
```

### Database per Service를 사용하는 이유

| 이유 | 설명 |
|------|------|
| **느슨한 결합** | 서비스 간 데이터베이스 스키마 의존성 제거 |
| **독립적 배포** | 다른 서비스에 영향 없이 스키마 변경 가능 |
| **기술 다양성** | 서비스 요구사항에 맞는 DB 기술 선택 가능 |
| **장애 격리** | 한 DB 장애가 다른 서비스에 영향 최소화 |
| **확장성** | 서비스별 독립적인 스케일링 가능 |

### 장점과 단점

**장점:**
- 서비스 자율성 보장
- Polyglot Persistence 가능 (MySQL, MongoDB, Redis 등 혼용)
- 마이크로서비스 원칙에 부합
- 스키마 진화의 독립성

**단점:**
- 분산 트랜잭션 복잡성 증가
- 데이터 일관성 유지 어려움
- 서비스 간 데이터 조인 불가
- 데이터 중복 가능성

---

## 2. 데이터 일관성 문제

### CAP Theorem

분산 시스템에서는 **Consistency(일관성)**, **Availability(가용성)**, **Partition Tolerance(분할 내성)** 세 가지를 동시에 완벽하게 만족할 수 없습니다.

```
                    CAP Theorem

              Consistency (C)
                   /\
                  /  \
                 /    \
                /  CA  \      ← 단일 노드 시스템
               /________\        (네트워크 분할 없음)
              /    |     \
             / CP  |  AP  \
            /______|_______\
    Partition           Availability
    Tolerance (P)           (A)
```

| 조합 | 설명 | 예시 |
|------|------|------|
| **CP** | 일관성 + 분할 내성 (가용성 포기) | MongoDB (Strong Consistency mode), HBase |
| **AP** | 가용성 + 분할 내성 (일관성 포기) | Cassandra, DynamoDB, CouchDB |
| **CA** | 일관성 + 가용성 (분할 내성 포기) | 전통적인 RDBMS (단일 노드) |

### Strong Consistency vs Eventual Consistency

```
┌─────────────────────────────────────────────────────────────────┐
│                    Strong Consistency                           │
│  ┌──────────┐     Write     ┌──────────┐                       │
│  │  Client  │──────────────▶│  Node A  │                       │
│  └──────────┘               └────┬─────┘                       │
│                                  │ Sync                         │
│                                  ▼                              │
│                             ┌──────────┐                       │
│                             │  Node B  │                       │
│                             └──────────┘                       │
│  Read from any node → Same data (blocked until sync)           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Eventual Consistency                          │
│  ┌──────────┐     Write     ┌──────────┐                       │
│  │  Client  │──────────────▶│  Node A  │                       │
│  └──────────┘   (returns    └────┬─────┘                       │
│                 immediately)     │ Async                        │
│                                  ▼ (later)                      │
│                             ┌──────────┐                       │
│                             │  Node B  │                       │
│                             └──────────┘                       │
│  Read from Node B → May see stale data (temporarily)           │
└─────────────────────────────────────────────────────────────────┘
```

### Eventual Consistency 패턴

마이크로서비스에서는 **Eventual Consistency**가 일반적입니다:

```java
// Portal Universe 예시: 주문 생성 후 재고 차감
// 1. 주문 서비스에서 주문 생성 (즉시 완료)
// 2. 이벤트 발행 (Kafka)
// 3. 재고 서비스가 이벤트 수신 후 재고 차감 (비동기)

// shopping-service/ShoppingEventPublisher.java
public void publishOrderCreated(OrderCreatedEvent event) {
    // 주문 생성 이벤트를 비동기로 발행
    // 다른 서비스들이 eventually 이 이벤트를 처리
    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);

    future.whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Event published: topic={}, offset={}",
                    topic, result.getRecordMetadata().offset());
        }
    });
}
```

---

## 3. 분산 트랜잭션 패턴

### 2PC (Two-Phase Commit)의 문제점

전통적인 분산 트랜잭션 방식인 2PC는 마이크로서비스에서 **권장되지 않습니다**.

```
┌─────────────────────────────────────────────────────────────────┐
│                 Two-Phase Commit (2PC)                          │
│                                                                 │
│  Phase 1: Prepare (Voting)                                      │
│  ┌─────────────┐                                               │
│  │ Coordinator │───▶ Prepare? ───▶ Participants                │
│  └─────────────┘◀─── Yes/No ◀────                              │
│                                                                 │
│  Phase 2: Commit/Rollback                                       │
│  ┌─────────────┐                                               │
│  │ Coordinator │───▶ Commit/Rollback ───▶ Participants         │
│  └─────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

**2PC의 문제점:**

| 문제 | 설명 |
|------|------|
| **동기식 블로킹** | 모든 참여자가 응답할 때까지 대기 |
| **Single Point of Failure** | Coordinator 장애 시 전체 트랜잭션 중단 |
| **성능 저하** | 네트워크 지연이 누적됨 |
| **서비스 결합도** | 서비스 간 강한 결합 발생 |
| **확장성 제한** | 참여자 수 증가에 따른 성능 급감 |

### Saga Pattern

Saga Pattern은 **로컬 트랜잭션의 시퀀스**로 분산 트랜잭션을 구현합니다. 실패 시 **보상 트랜잭션(Compensating Transaction)**으로 롤백합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                       Saga Pattern                              │
│                                                                 │
│  정상 흐름 (Happy Path):                                        │
│  T1 ──▶ T2 ──▶ T3 ──▶ T4 ──▶ SUCCESS                          │
│                                                                 │
│  실패 시 보상 (Compensation):                                    │
│  T1 ──▶ T2 ──▶ T3 ──✗ (fail)                                   │
│                     │                                           │
│                     ▼                                           │
│              C3 ◀── C2 ◀── C1 (compensating transactions)      │
│                                                                 │
│  T = Local Transaction                                          │
│  C = Compensating Transaction                                   │
└─────────────────────────────────────────────────────────────────┘
```

#### Saga 구현 방식: Choreography vs Orchestration

```
┌─────────────────────────────────────────────────────────────────┐
│              Choreography-based Saga                            │
│                                                                 │
│  각 서비스가 이벤트를 발행하고 다른 서비스가 구독               │
│                                                                 │
│  Order      Inventory     Payment     Delivery                  │
│  Service    Service       Service     Service                   │
│    │           │             │           │                      │
│    │──Event──▶│             │           │                      │
│    │           │──Event────▶│           │                      │
│    │           │             │──Event──▶│                      │
│                                                                 │
│  장점: 서비스 간 느슨한 결합                                    │
│  단점: 전체 흐름 파악 어려움, 복잡한 보상 로직                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│               Orchestration-based Saga                          │
│                                                                 │
│  중앙 Orchestrator가 전체 흐름을 제어                          │
│                                                                 │
│                ┌─────────────────┐                             │
│                │   Orchestrator  │                             │
│                └────────┬────────┘                             │
│           ┌─────────────┼─────────────┐                        │
│           ▼             ▼             ▼                        │
│      ┌────────┐   ┌────────┐   ┌────────┐                     │
│      │Inventory│   │ Payment│   │Delivery│                     │
│      └────────┘   └────────┘   └────────┘                     │
│                                                                 │
│  장점: 전체 흐름 명확, 보상 로직 집중 관리                      │
│  단점: Orchestrator가 SPOF가 될 수 있음                         │
└─────────────────────────────────────────────────────────────────┘
```

### Portal Universe의 Saga 구현

Portal Universe의 Shopping Service는 **Orchestration-based Saga**를 사용합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│          Portal Universe: Order Saga Flow                       │
│                                                                 │
│  Step 1: RESERVE_INVENTORY (재고 예약)                          │
│      │                                                          │
│      ▼                                                          │
│  Step 2: PROCESS_PAYMENT (결제 처리)                            │
│      │                                                          │
│      ▼                                                          │
│  Step 3: DEDUCT_INVENTORY (재고 차감)                           │
│      │                                                          │
│      ▼                                                          │
│  Step 4: CREATE_DELIVERY (배송 생성)                            │
│      │                                                          │
│      ▼                                                          │
│  Step 5: CONFIRM_ORDER (주문 확정)                              │
│                                                                 │
│  실패 시: 역순으로 보상 트랜잭션 실행                           │
└─────────────────────────────────────────────────────────────────┘
```

#### SagaStep 열거형

```java
// services/shopping-service/.../order/saga/SagaStep.java
public enum SagaStep {
    RESERVE_INVENTORY(1, "재고 예약", true),      // 보상 가능
    PROCESS_PAYMENT(2, "결제 처리", true),        // 보상 가능
    DEDUCT_INVENTORY(3, "재고 차감", true),       // 보상 가능
    CREATE_DELIVERY(4, "배송 생성", true),        // 보상 가능
    CONFIRM_ORDER(5, "주문 확정", false);         // 보상 불필요

    private final int order;
    private final String description;
    private final boolean compensatable;

    // 다음 단계 반환
    public SagaStep next() {
        SagaStep[] steps = values();
        for (int i = 0; i < steps.length - 1; i++) {
            if (steps[i] == this) return steps[i + 1];
        }
        return null;
    }

    // 이전 단계 반환 (보상 시 사용)
    public SagaStep previous() {
        SagaStep[] steps = values();
        for (int i = 1; i < steps.length; i++) {
            if (steps[i] == this) return steps[i - 1];
        }
        return null;
    }
}
```

#### SagaState 엔티티

```java
// services/shopping-service/.../order/saga/SagaState.java
@Entity
@Table(name = "saga_states")
public class SagaState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", unique = true)
    private String sagaId;              // "SAGA-ABC12345"

    private Long orderId;
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;       // 현재 실행 단계

    @Enumerated(EnumType.STRING)
    private SagaStatus status;          // STARTED, COMPLETED, COMPENSATING, FAILED

    private String completedSteps;      // "RESERVE_INVENTORY,PROCESS_PAYMENT"
    private String lastErrorMessage;
    private Integer compensationAttempts;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // 다음 단계로 진행
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

    // 특정 단계 완료 여부 확인
    public boolean isStepCompleted(SagaStep step) {
        return this.completedSteps.contains(step.name());
    }
}
```

#### OrderSagaOrchestrator 핵심 로직

```java
// services/shopping-service/.../order/saga/OrderSagaOrchestrator.java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /**
     * Saga 시작 (주문 생성 시 호출)
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
            // Step 1: 재고 예약
            executeReserveInventory(order, sagaState);
            sagaState.proceedToNextStep();
            sagaStateRepository.save(sagaState);

            return sagaState;

        } catch (Exception e) {
            log.error("Saga {} failed at {}: {}",
                    sagaState.getSagaId(), sagaState.getCurrentStep(), e.getMessage());
            compensate(sagaState, e.getMessage());
            throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
        }
    }

    /**
     * 보상(롤백) 수행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(SagaState sagaState, String errorMessage) {
        log.info("Starting compensation for saga {}", sagaState.getSagaId());

        sagaState.startCompensation(errorMessage);
        sagaStateRepository.save(sagaState);

        Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
                .orElse(null);

        try {
            // 완료된 단계들을 역순으로 보상
            if (sagaState.isStepCompleted(SagaStep.DEDUCT_INVENTORY)) {
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
                log.error("Saga {} - Max compensation attempts reached",
                        sagaState.getSagaId());
            }
            sagaStateRepository.save(sagaState);
        }
    }

    private void executeReserveInventory(Order order, SagaState sagaState) {
        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.reserveStockBatch(
                quantities, "ORDER", order.getOrderNumber(), order.getUserId()
        );
    }

    private void compensateReserveInventory(Order order, SagaState sagaState) {
        Map<Long, Integer> quantities = order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        // 예약된 재고 해제 (보상 트랜잭션)
        inventoryService.releaseStockBatch(
                quantities, "ORDER_CANCEL", order.getOrderNumber(), "SYSTEM"
        );
    }
}
```

---

## 4. 데이터 동기화 전략

### 4.1 API 호출 (동기식)

서비스 간 즉시 데이터가 필요한 경우 **Feign Client**를 사용합니다.

```java
// services/shopping-service/.../feign/BlogServiceClient.java
@FeignClient(
    name = "blog-service",
    url = "${feign.blog-service.url}",
    path = "/api/blog"
)
public interface BlogServiceClient {

    /**
     * 상품 ID로 리뷰(블로그 게시물) 조회
     * Shopping Service → Blog Service 동기 호출
     */
    @GetMapping("/reviews")
    List<BlogResponse> getPostByProductId(@RequestParam("productId") String productId);
}
```

**사용 시나리오:**
- 상품 상세 페이지에서 리뷰 정보 즉시 필요
- 인증 정보 검증 (API Gateway → Auth Service)

**주의사항:**
- 호출되는 서비스 장애 시 전파 가능
- Circuit Breaker 패턴과 함께 사용 권장
- Timeout 설정 필수

### 4.2 이벤트 기반 (비동기식)

**Kafka**를 통한 이벤트 기반 통신이 Portal Universe의 주요 패턴입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│              Event-Driven Data Synchronization                  │
│                                                                 │
│  ┌────────────┐    Publish     ┌─────────┐    Subscribe         │
│  │  Shopping  │───────────────▶│  Kafka  │◀───────────────┐     │
│  │  Service   │                │ Cluster │               │     │
│  └────────────┘                └─────────┘               │     │
│                                     │                    │     │
│        Topics:                      ▼               ┌────┴────┐│
│        • shopping.order.created     │               │Notifi-  ││
│        • shopping.payment.completed │               │cation   ││
│        • shopping.delivery.shipped  │               │Service  ││
│        • user-signup                │               └─────────┘│
└─────────────────────────────────────────────────────────────────┘
```

#### 이벤트 발행 (Producer)

```java
// services/shopping-service/.../event/ShoppingEventPublisher.java
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        publishEvent(KafkaConfig.TOPIC_DELIVERY_SHIPPED, event.trackingNumber(), event);
    }

    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish: topic={}, error={}", topic, ex.getMessage());
            }
        });
    }
}
```

#### 이벤트 소비 (Consumer)

```java
// services/notification-service/.../consumer/NotificationConsumer.java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "user-signup", groupId = "notification-group")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup: {} ({})", event.name(), event.email());
        // 웰컴 이메일 발송 로직
    }

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.delivery.shipped", groupId = "notification-group")
    public void handleDeliveryShipped(NotificationEvent event) {
        log.info("Received delivery shipped: userId={}", event.getUserId());
        createAndPush(event);
    }

    // 예외 발생 시 Kafka ErrorHandler가 재시도 후 DLQ로 이동
    private void createAndPush(NotificationEvent event) {
        Notification notification = notificationService.create(
                event.getUserId(),
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getLink(),
                event.getReferenceId(),
                event.getReferenceType()
        );

        pushService.push(notification);
    }
}
```

### 4.3 CQRS (Command Query Responsibility Segregation)

**쓰기(Command)**와 **읽기(Query)** 모델을 분리하는 패턴입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                         CQRS Pattern                            │
│                                                                 │
│  ┌──────────────┐                    ┌──────────────┐           │
│  │   Command    │                    │    Query     │           │
│  │   Service    │                    │   Service    │           │
│  └──────┬───────┘                    └──────┬───────┘           │
│         │                                   │                   │
│         ▼                                   ▼                   │
│  ┌──────────────┐    Event    ┌──────────────────┐             │
│  │ Write Model  │────────────▶│   Read Model     │             │
│  │   (MySQL)    │             │ (Elasticsearch)  │             │
│  └──────────────┘             └──────────────────┘             │
│                                                                 │
│  Write: Normalized, ACID       Read: Denormalized, Fast Search │
└─────────────────────────────────────────────────────────────────┘
```

**Portal Universe에서의 CQRS 적용:**
- **Shopping Service**: MySQL (원본 데이터) + Elasticsearch (상품 검색용)
- 상품 정보 변경 → 이벤트 발행 → Elasticsearch 인덱스 갱신

---

## 5. Portal Universe의 데이터 관리 분석

### 서비스별 데이터베이스 현황

```
┌─────────────────────────────────────────────────────────────────┐
│            Portal Universe: Polyglot Persistence                │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Auth Service │  │ Blog Service │  │   Shopping Service   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                 │                      │              │
│         ▼                 ▼                      ▼              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │    MySQL     │  │   MongoDB    │  │ MySQL + Elasticsearch│  │
│  │  (auth_db)   │  │  (blog_db)   │  │    (shopping_db)     │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │            Notification Service                            ││
│  └────────────────────┬───────────────────────────────────────┘│
│                       │                                        │
│                       ▼                                        │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │     MySQL (notification_db) + Redis (Real-time Push)      │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 5.1 Auth Service - MySQL

**선택 이유:**
- 사용자 인증은 **강한 일관성(Strong Consistency)** 필요
- 트랜잭션 보장 (회원가입, 비밀번호 변경 등)
- 관계형 데이터 (User ↔ Profile ↔ SocialAccount)

```java
// services/auth-service/.../repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.email = :email")
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.profile p WHERE p.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
}
```

**설정:**
```yaml
# services/auth-service/src/main/resources/application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
```

### 5.2 Blog Service - MongoDB

**선택 이유:**
- 게시물 구조가 **유연** (태그, 이미지 목록, 메타데이터)
- 스키마 변경이 잦음 (새 필드 추가 용이)
- **전문 검색(Text Search)** 지원
- 수평 확장 용이 (Sharding)

```java
// services/blog-service/.../post/domain/Post.java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;

    @TextIndexed(weight = 2.0f)  // 전문 검색 인덱스
    private String title;

    @TextIndexed
    private String content;

    @Indexed
    private String authorId;

    @Indexed
    private Set<String> tags = new HashSet<>();  // 유연한 태그 구조

    private List<String> images = new ArrayList<>();  // 동적 배열

    private Long viewCount = 0L;
    private Long likeCount = 0L;
}
```

```java
// services/blog-service/.../post/repository/PostRepository.java
public interface PostRepository extends MongoRepository<Post, String> {

    // MongoDB 전문 검색
    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

    // 관련 게시물 추천 (복합 조건)
    @Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
    List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

    // 피드 조회 (팔로잉 작성자 필터)
    Page<Post> findByAuthorIdInAndStatusOrderByPublishedAtDesc(
            List<String> authorIds, PostStatus status, Pageable pageable);
}
```

**설정:**
```yaml
# services/blog-service/src/main/resources/application-local.yml
spring:
  data:
    mongodb:
      uri: mongodb://laze:password@localhost:27017/blog_db?authSource=admin
```

### 5.3 Shopping Service - MySQL + Elasticsearch

**선택 이유:**
- 주문/결제는 **ACID 트랜잭션** 필수 (MySQL)
- 상품 검색은 **빠른 전문 검색** 필요 (Elasticsearch)
- 재고 관리는 **동시성 제어** 필요 (Pessimistic Lock)

```java
// services/shopping-service/.../inventory/repository/InventoryRepository.java
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 비관적 쓰기 락으로 동시성 제어
     * SELECT ... FOR UPDATE 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * 배치 조회 시 데드락 방지를 위해 ID 순서로 정렬
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);
}
```

**설정:**
```yaml
# services/shopping-service/src/main/resources/application.yml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 5.4 Notification Service - MySQL + Redis

**선택 이유:**
- 알림 이력은 **영속 저장** 필요 (MySQL)
- 실시간 푸시는 **빠른 접근** 필요 (Redis Pub/Sub)
- 읽지 않은 알림 카운트 캐싱 (Redis)

```java
// services/notification-service/.../repository/NotificationRepository.java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, NotificationStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt " +
           "WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId,
                      @Param("status") NotificationStatus status,
                      @Param("readAt") LocalDateTime readAt);
}
```

---

## 6. Polyglot Persistence 트레이드오프

### 언제 어떤 DB를 선택할까?

| 요구사항 | 추천 DB | 이유 |
|----------|---------|------|
| 트랜잭션/ACID 보장 | MySQL, PostgreSQL | 강한 일관성 |
| 유연한 스키마 | MongoDB | 스키마리스, JSON 구조 |
| 전문 검색 | Elasticsearch | 역인덱스, 분석기 |
| 캐싱/세션 | Redis | 인메모리, 빠른 접근 |
| 시계열 데이터 | InfluxDB, TimescaleDB | 시간 기반 집계 |
| 그래프 관계 | Neo4j | 관계 탐색 최적화 |
| 메시지 큐 | Kafka, RabbitMQ | 비동기 처리, 내구성 |

### 트레이드오프 분석

```
┌─────────────────────────────────────────────────────────────────┐
│                    Polyglot Persistence                         │
│                                                                 │
│  장점                          단점                             │
│  ────────────                  ────────────                     │
│  • 최적의 DB 선택              • 운영 복잡성 증가              │
│  • 각 워크로드 최적화          • 학습 곡선                     │
│  • 기술적 유연성               • 인프라 비용 증가              │
│  • 장애 격리                   • 데이터 일관성 관리            │
│                                • 백업/복구 복잡성              │
│                                                                 │
│  결정 기준:                                                     │
│  • 팀의 기술 역량                                               │
│  • 운영 리소스                                                  │
│  • 실제 필요성 (과도한 기술 도입 주의)                          │
└─────────────────────────────────────────────────────────────────┘
```

### Portal Universe의 선택 근거

| 서비스 | DB | 근거 |
|--------|-----|------|
| Auth | MySQL | 사용자 인증은 ACID 필수, 관계형 데이터 |
| Blog | MongoDB | 게시물 구조 유연, 전문 검색, 잦은 스키마 변경 |
| Shopping | MySQL + ES | 주문은 ACID, 상품 검색은 Elasticsearch |
| Notification | MySQL + Redis | 이력은 영속, 실시간 푸시는 Redis |

---

## 7. 실습 예제

### 실습 1: Saga 상태 확인

```sql
-- Saga 상태 조회
SELECT saga_id, order_number, current_step, status,
       completed_steps, compensation_attempts, started_at
FROM saga_states
WHERE status = 'COMPENSATING' OR status = 'COMPENSATION_FAILED'
ORDER BY started_at DESC;

-- 보상 실패한 Saga (수동 개입 필요)
SELECT * FROM saga_states
WHERE status = 'COMPENSATION_FAILED'
  AND compensation_attempts >= 3;
```

### 실습 2: 이벤트 기반 데이터 흐름 추적

```bash
# Kafka 토픽 목록 확인
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# 주문 생성 이벤트 모니터링
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shopping.order.created \
  --from-beginning

# 결제 완료 이벤트 모니터링
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shopping.payment.completed \
  --from-beginning
```

### 실습 3: 동시성 제어 테스트

```java
// 재고 동시 차감 테스트 (Race Condition 방지)
@Test
void concurrentStockDeduction() throws InterruptedException {
    Long productId = 1L;
    int threadCount = 10;
    int deductPerThread = 1;

    // 초기 재고: 10개
    inventoryService.initializeStock(productId, 10);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                inventoryService.deductStock(productId, deductPerThread);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // 비관적 락으로 인해 정확히 0개가 되어야 함
    Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
    assertThat(inventory.getAvailableQuantity()).isEqualTo(0);
}
```

### 실습 4: MongoDB 전문 검색

```javascript
// MongoDB Shell에서 실행
use blog_db

// 텍스트 인덱스 확인
db.posts.getIndexes()

// 전문 검색 쿼리
db.posts.find({
  $text: { $search: "마이크로서비스 아키텍처" },
  status: "PUBLISHED"
}).sort({ score: { $meta: "textScore" } })

// 관련 게시물 조회 (OR 조건)
db.posts.find({
  $or: [
    { category: "Tech" },
    { tags: { $in: ["microservices", "architecture"] } }
  ],
  status: "PUBLISHED",
  _id: { $ne: ObjectId("current_post_id") }
}).limit(5)
```

---

## 8. 핵심 요약

| 개념 | 핵심 내용 |
|------|----------|
| **Database per Service** | 서비스 자율성 보장, 느슨한 결합의 핵심 |
| **CAP Theorem** | 분산 시스템에서 C, A, P 중 2개만 선택 가능 |
| **Eventual Consistency** | 마이크로서비스의 기본 일관성 모델 |
| **Saga Pattern** | 로컬 트랜잭션 + 보상 트랜잭션으로 분산 트랜잭션 구현 |
| **이벤트 기반 동기화** | Kafka로 비동기 데이터 전파 |
| **Polyglot Persistence** | 워크로드에 맞는 DB 선택, 운영 복잡성 고려 |

---

## 다음 학습 주제

- [Kafka Deep Dive](../kafka/kafka-fundamentals.md) - 이벤트 스트리밍 심화
- [Redis Patterns](../redis/redis-patterns.md) - 캐싱 및 분산 락
- [Elasticsearch Basics](../elasticsearch/elasticsearch-basics.md) - 검색 엔진 활용

---

## 참고 자료

- [Microservices Patterns - Chris Richardson](https://microservices.io/patterns/data/database-per-service.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [CAP Theorem Explained](https://www.ibm.com/topics/cap-theorem)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
