# Event Sourcing 패턴

## 학습 목표
- Event Sourcing의 핵심 개념과 원리 이해
- 이벤트 저장 및 상태 재구성 메커니즘 학습
- Event Store 설계와 구현 방법 습득
- Portal Universe에서의 적용 방안 분석

---

## 1. Event Sourcing 개요

### 1.1 전통적인 상태 저장 vs Event Sourcing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    STATE-BASED vs EVENT SOURCING                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   전통적인 상태 저장 (CRUD):                                                │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Order Table                                                         │  │
│   │  ┌───────┬────────────┬─────────┬───────────┐                       │  │
│   │  │ ID    │ Status     │ Total   │ UpdatedAt │                       │  │
│   │  ├───────┼────────────┼─────────┼───────────┤                       │  │
│   │  │ 1001  │ SHIPPED    │ 50000   │ 2024-01-22│  ← 현재 상태만 저장   │  │
│   │  └───────┴────────────┴─────────┴───────────┘                       │  │
│   │                                                                      │  │
│   │  문제: 어떤 과정으로 이 상태가 되었는지 알 수 없음                    │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Event Sourcing:                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Event Store                                                         │  │
│   │  ┌───────┬────────────────────┬──────────────────┬─────────────┐    │  │
│   │  │ Seq   │ Event Type         │ Payload          │ Timestamp   │    │  │
│   │  ├───────┼────────────────────┼──────────────────┼─────────────┤    │  │
│   │  │ 1     │ OrderCreated       │ {items, total}   │ 2024-01-20  │    │  │
│   │  │ 2     │ PaymentCompleted   │ {paymentId}      │ 2024-01-20  │    │  │
│   │  │ 3     │ OrderConfirmed     │ {}               │ 2024-01-21  │    │  │
│   │  │ 4     │ OrderShipped       │ {trackingNo}     │ 2024-01-22  │    │  │
│   │  └───────┴────────────────────┴──────────────────┴─────────────┘    │  │
│   │                                                                      │  │
│   │  장점: 모든 변경 이력이 보존됨, 어느 시점으로든 재구성 가능          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 개념

| 개념 | 설명 |
|------|------|
| **Event** | 과거에 발생한 불변의 사실 (fact) |
| **Event Store** | 이벤트를 순서대로 저장하는 저장소 |
| **Aggregate** | 이벤트를 발생시키는 도메인 객체 |
| **Projection** | 이벤트를 읽어 특정 뷰를 생성 |
| **Replay** | 이벤트를 재생하여 상태 재구성 |

### 1.3 Event Sourcing의 장단점

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    EVENT SOURCING TRADE-OFFS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   장점:                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ ✓ 완전한 감사 로그 (Audit Trail)                                    │  │
│   │ ✓ 시간 여행 (Time Travel) - 특정 시점의 상태 조회                   │  │
│   │ ✓ 이벤트 재생으로 버그 재현 가능                                    │  │
│   │ ✓ 새로운 Projection 추가 용이                                       │  │
│   │ ✓ 도메인 이벤트 기반 설계와 자연스럽게 연결                         │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   단점:                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ ✗ 학습 곡선 높음                                                    │  │
│   │ ✗ 이벤트 스키마 변경 어려움 (Versioning 필요)                       │  │
│   │ ✗ 조회 성능 - Projection 없이는 느림                                │  │
│   │ ✗ 이벤트 저장소 용량 증가                                           │  │
│   │ ✗ Eventual Consistency 다루기                                       │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Event Store 설계

### 2.1 Event Store 스키마

```sql
-- Event Store Table
CREATE TABLE event_store (
    -- 식별자
    event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Aggregate 정보
    aggregate_type  VARCHAR(100) NOT NULL,  -- 예: 'Order', 'Product'
    aggregate_id    UUID NOT NULL,          -- 예: orderId

    -- 이벤트 정보
    event_type      VARCHAR(100) NOT NULL,  -- 예: 'OrderCreated'
    event_version   INT NOT NULL,           -- 이벤트 스키마 버전
    payload         JSONB NOT NULL,         -- 이벤트 데이터
    metadata        JSONB,                  -- 추가 메타데이터

    -- 순서 보장
    sequence_number BIGSERIAL,              -- 전역 순서
    aggregate_version INT NOT NULL,         -- Aggregate 내 순서

    -- 시간 정보
    created_at      TIMESTAMP DEFAULT NOW(),

    -- 인덱스를 위한 제약
    UNIQUE(aggregate_id, aggregate_version)
);

-- 조회 최적화 인덱스
CREATE INDEX idx_event_aggregate ON event_store(aggregate_type, aggregate_id);
CREATE INDEX idx_event_type ON event_store(event_type);
CREATE INDEX idx_event_created ON event_store(created_at);
```

### 2.2 이벤트 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EVENT STRUCTURE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Event Envelope:                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  {                                                                   │  │
│   │    "eventId": "uuid-1234-5678",                                     │  │
│   │    "eventType": "OrderCreated",                                     │  │
│   │    "eventVersion": 1,                                               │  │
│   │    "aggregateId": "order-uuid-9999",                                │  │
│   │    "aggregateType": "Order",                                        │  │
│   │    "aggregateVersion": 1,                                           │  │
│   │    "timestamp": "2024-01-22T10:30:00Z",                             │  │
│   │    "payload": {                        ← 실제 이벤트 데이터          │  │
│   │      "customerId": "cust-123",                                      │  │
│   │      "items": [...],                                                │  │
│   │      "totalAmount": 50000                                           │  │
│   │    },                                                               │  │
│   │    "metadata": {                       ← 부가 정보                   │  │
│   │      "correlationId": "req-abc",                                    │  │
│   │      "causationId": "evt-prev",                                     │  │
│   │      "userId": "user-456"                                           │  │
│   │    }                                                                │  │
│   │  }                                                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Java 구현

### 3.1 Event 기본 클래스

```java
/**
 * 모든 도메인 이벤트의 기본 클래스
 */
public abstract class DomainEvent {

    private final String eventId;
    private final String aggregateId;
    private final int aggregateVersion;
    private final Instant occurredAt;
    private final Map<String, String> metadata;

    protected DomainEvent(String aggregateId, int aggregateVersion) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    public abstract String getEventType();

    // Getters...
}

/**
 * 주문 생성 이벤트
 */
public class OrderCreatedEvent extends DomainEvent {

    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, int version,
                            String customerId,
                            List<OrderItem> items,
                            BigDecimal totalAmount) {
        super(orderId, version);
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.totalAmount = totalAmount;
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }

    // Getters...
}

/**
 * 주문 배송 이벤트
 */
public class OrderShippedEvent extends DomainEvent {

    private final String trackingNumber;
    private final String carrier;

    public OrderShippedEvent(String orderId, int version,
                            String trackingNumber, String carrier) {
        super(orderId, version);
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
    }

    @Override
    public String getEventType() {
        return "OrderShipped";
    }
}
```

### 3.2 Event-Sourced Aggregate

```java
/**
 * Event Sourcing을 지원하는 Aggregate 기본 클래스
 */
public abstract class EventSourcedAggregate {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private int version = 0;

    /**
     * 이벤트를 적용하고 uncommitted 목록에 추가
     */
    protected void applyChange(DomainEvent event) {
        // 1. 상태 변경 적용
        apply(event);
        // 2. 아직 저장되지 않은 이벤트 목록에 추가
        uncommittedEvents.add(event);
        // 3. 버전 증가
        version++;
    }

    /**
     * 이벤트를 상태에 적용 (서브클래스에서 구현)
     */
    protected abstract void apply(DomainEvent event);

    /**
     * 이벤트 히스토리로부터 상태 재구성
     */
    public void loadFromHistory(List<DomainEvent> history) {
        for (DomainEvent event : history) {
            apply(event);
            version++;
        }
    }

    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void markChangesAsCommitted() {
        uncommittedEvents.clear();
    }

    public int getVersion() {
        return version;
    }
}

/**
 * Order Aggregate
 */
public class Order extends EventSourcedAggregate {

    private String orderId;
    private String customerId;
    private OrderStatus status;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalAmount;
    private String trackingNumber;

    // 생성자 - 새 주문 생성
    public static Order create(String orderId, String customerId,
                               List<OrderItem> items) {
        Order order = new Order();
        BigDecimal total = calculateTotal(items);

        order.applyChange(new OrderCreatedEvent(
            orderId, 1, customerId, items, total
        ));

        return order;
    }

    // 비공개 기본 생성자 (재구성용)
    private Order() {}

    // 배송 처리
    public void ship(String trackingNumber, String carrier) {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Only confirmed orders can be shipped"
            );
        }

        applyChange(new OrderShippedEvent(
            orderId, getVersion() + 1, trackingNumber, carrier
        ));
    }

    /**
     * 이벤트 적용 - 상태 변경 로직
     */
    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case OrderCreatedEvent e -> {
                this.orderId = e.getAggregateId();
                this.customerId = e.getCustomerId();
                this.items = new ArrayList<>(e.getItems());
                this.totalAmount = e.getTotalAmount();
                this.status = OrderStatus.CREATED;
            }
            case OrderShippedEvent e -> {
                this.trackingNumber = e.getTrackingNumber();
                this.status = OrderStatus.SHIPPED;
            }
            case PaymentCompletedEvent e -> {
                this.status = OrderStatus.PAID;
            }
            case OrderConfirmedEvent e -> {
                this.status = OrderStatus.CONFIRMED;
            }
            default -> throw new IllegalArgumentException(
                "Unknown event type: " + event.getClass()
            );
        }
    }

    // Getters...
}
```

### 3.3 Event Store Repository

```java
/**
 * Event Store Repository Interface
 */
public interface EventStore {

    /**
     * 이벤트 저장
     */
    void saveEvents(String aggregateId, String aggregateType,
                   List<DomainEvent> events, int expectedVersion);

    /**
     * Aggregate의 모든 이벤트 조회
     */
    List<DomainEvent> getEvents(String aggregateId);

    /**
     * 특정 버전 이후의 이벤트 조회
     */
    List<DomainEvent> getEventsAfterVersion(String aggregateId,
                                            int afterVersion);

    /**
     * 특정 시점까지의 이벤트 조회
     */
    List<DomainEvent> getEventsUntil(String aggregateId,
                                     Instant until);
}

/**
 * JPA 기반 Event Store 구현
 */
@Repository
@RequiredArgsConstructor
public class JpaEventStore implements EventStore {

    private final EventStoreJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveEvents(String aggregateId, String aggregateType,
                          List<DomainEvent> events, int expectedVersion) {

        // Optimistic Locking - 버전 확인
        int currentVersion = repository.findMaxVersion(aggregateId)
                                        .orElse(0);

        if (currentVersion != expectedVersion) {
            throw new OptimisticLockingException(
                "Expected version " + expectedVersion +
                " but found " + currentVersion
            );
        }

        // 이벤트 저장
        int version = expectedVersion;
        for (DomainEvent event : events) {
            EventEntity entity = EventEntity.builder()
                .eventId(event.getEventId())
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(event.getEventType())
                .aggregateVersion(++version)
                .payload(serialize(event))
                .metadata(serializeMetadata(event.getMetadata()))
                .createdAt(event.getOccurredAt())
                .build();

            repository.save(entity);
        }
    }

    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return repository.findByAggregateIdOrderByAggregateVersion(aggregateId)
                        .stream()
                        .map(this::deserialize)
                        .toList();
    }

    @Override
    public List<DomainEvent> getEventsUntil(String aggregateId,
                                            Instant until) {
        return repository.findByAggregateIdAndCreatedAtBeforeOrderByAggregateVersion(
                aggregateId, until)
            .stream()
            .map(this::deserialize)
            .toList();
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize", e);
        }
    }

    private DomainEvent deserialize(EventEntity entity) {
        try {
            Class<? extends DomainEvent> eventClass =
                EventTypeRegistry.getClass(entity.getEventType());
            return objectMapper.readValue(entity.getPayload(), eventClass);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize", e);
        }
    }
}
```

### 3.4 Aggregate Repository

```java
/**
 * Event-Sourced Aggregate Repository
 */
@Repository
@RequiredArgsConstructor
public class EventSourcedOrderRepository {

    private final EventStore eventStore;

    /**
     * 주문 저장 - 새 이벤트만 저장
     */
    public void save(Order order) {
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();

        if (!uncommittedEvents.isEmpty()) {
            eventStore.saveEvents(
                order.getOrderId(),
                "Order",
                uncommittedEvents,
                order.getVersion() - uncommittedEvents.size()
            );
            order.markChangesAsCommitted();
        }
    }

    /**
     * 주문 조회 - 이벤트로부터 재구성
     */
    public Optional<Order> findById(String orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId);

        if (events.isEmpty()) {
            return Optional.empty();
        }

        Order order = new Order();
        order.loadFromHistory(events);
        return Optional.of(order);
    }

    /**
     * 특정 시점의 주문 상태 조회 (Time Travel)
     */
    public Optional<Order> findByIdAsOf(String orderId, Instant asOf) {
        List<DomainEvent> events = eventStore.getEventsUntil(orderId, asOf);

        if (events.isEmpty()) {
            return Optional.empty();
        }

        Order order = new Order();
        order.loadFromHistory(events);
        return Optional.of(order);
    }
}
```

---

## 4. 상태 재구성과 Snapshot

### 4.1 Snapshot 전략

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SNAPSHOT STRATEGY                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   문제: 이벤트가 많아지면 재구성 시간 증가                                   │
│                                                                             │
│   Events: [E1] → [E2] → [E3] → ... → [E1000] → 재구성 = 느림!              │
│                                                                             │
│   해결: Snapshot 사용                                                       │
│                                                                             │
│   [E1] → [E2] → ... → [E100] → [SNAPSHOT@100]                               │
│                                     ↓                                       │
│                         [E101] → [E102] → ... → [E150]                      │
│                                                                             │
│   재구성 = Snapshot 로드 + 이후 이벤트 적용                                  │
│                                                                             │
│   Snapshot 생성 전략:                                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ 1. N개 이벤트마다 (예: 100개)                                       │  │
│   │ 2. 시간 기반 (예: 매시간)                                           │  │
│   │ 3. 조회 시 동적 생성                                                │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Snapshot 구현

```java
/**
 * Snapshot Entity
 */
@Entity
@Table(name = "snapshots")
public class SnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private String aggregateType;
    private int version;

    @Column(columnDefinition = "jsonb")
    private String state;

    private Instant createdAt;
}

/**
 * Snapshot을 지원하는 Repository
 */
@Repository
@RequiredArgsConstructor
public class SnapshotEventSourcedOrderRepository {

    private final EventStore eventStore;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    private static final int SNAPSHOT_THRESHOLD = 100;

    /**
     * 주문 조회 - Snapshot + 이후 이벤트
     */
    public Optional<Order> findById(String orderId) {
        // 1. 최신 Snapshot 조회
        Optional<SnapshotEntity> snapshot =
            snapshotRepository.findLatestByAggregateId(orderId);

        Order order;
        int fromVersion;

        if (snapshot.isPresent()) {
            // 2a. Snapshot으로부터 복원
            order = deserializeOrder(snapshot.get().getState());
            fromVersion = snapshot.get().getVersion();
        } else {
            // 2b. 빈 상태에서 시작
            order = new Order();
            fromVersion = 0;
        }

        // 3. Snapshot 이후의 이벤트 적용
        List<DomainEvent> events =
            eventStore.getEventsAfterVersion(orderId, fromVersion);

        if (events.isEmpty() && snapshot.isEmpty()) {
            return Optional.empty();
        }

        order.loadFromHistory(events);

        // 4. 필요시 새 Snapshot 생성
        if (shouldCreateSnapshot(order, fromVersion)) {
            createSnapshot(order);
        }

        return Optional.of(order);
    }

    private boolean shouldCreateSnapshot(Order order, int lastSnapshotVersion) {
        return order.getVersion() - lastSnapshotVersion >= SNAPSHOT_THRESHOLD;
    }

    @Async
    protected void createSnapshot(Order order) {
        SnapshotEntity snapshot = SnapshotEntity.builder()
            .aggregateId(order.getOrderId())
            .aggregateType("Order")
            .version(order.getVersion())
            .state(serializeOrder(order))
            .createdAt(Instant.now())
            .build();

        snapshotRepository.save(snapshot);
    }
}
```

---

## 5. Event Versioning

### 5.1 이벤트 스키마 변경 전략

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EVENT VERSIONING STRATEGIES                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   문제: 이벤트 구조가 변경되면?                                              │
│                                                                             │
│   V1: OrderCreatedEvent { customerId, items, total }                        │
│   V2: OrderCreatedEvent { customerId, items, total, currency }  ← NEW!      │
│                                                                             │
│   전략 1: Upcasting (권장)                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  V1 이벤트를 읽을 때 V2로 변환                                       │  │
│   │  • 기존 이벤트 유지                                                  │  │
│   │  • Upcaster 체인으로 버전 변환                                       │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   전략 2: Event Migration                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  기존 이벤트를 새 형식으로 마이그레이션                               │  │
│   │  • 데이터 일관성 보장                                                │  │
│   │  • 대량 데이터 처리 필요                                             │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   전략 3: 새 이벤트 타입                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  OrderCreatedEvent → OrderCreatedEventV2                            │  │
│   │  • 명확한 버전 구분                                                  │  │
│   │  • 핸들러 복잡도 증가                                                │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Upcaster 구현

```java
/**
 * Event Upcaster Interface
 */
public interface EventUpcaster {
    String eventType();
    int fromVersion();
    int toVersion();
    JsonNode upcast(JsonNode oldEvent);
}

/**
 * OrderCreated V1 → V2 Upcaster
 */
@Component
public class OrderCreatedV1ToV2Upcaster implements EventUpcaster {

    @Override
    public String eventType() {
        return "OrderCreated";
    }

    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public int toVersion() {
        return 2;
    }

    @Override
    public JsonNode upcast(JsonNode oldEvent) {
        ObjectNode newEvent = oldEvent.deepCopy();
        // 기본값 추가
        newEvent.put("currency", "KRW");
        return newEvent;
    }
}

/**
 * Upcasting Chain
 */
@Component
@RequiredArgsConstructor
public class EventUpcasterChain {

    private final List<EventUpcaster> upcasters;

    public JsonNode upcast(String eventType, int version, JsonNode event) {
        JsonNode current = event;
        int currentVersion = version;

        for (EventUpcaster upcaster : getSortedUpcasters(eventType)) {
            if (upcaster.fromVersion() == currentVersion) {
                current = upcaster.upcast(current);
                currentVersion = upcaster.toVersion();
            }
        }

        return current;
    }

    private List<EventUpcaster> getSortedUpcasters(String eventType) {
        return upcasters.stream()
            .filter(u -> u.eventType().equals(eventType))
            .sorted(Comparator.comparingInt(EventUpcaster::fromVersion))
            .toList();
    }
}
```

---

## 6. Portal Universe 적용

### 6.1 주문 서비스 Event Sourcing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                 PORTAL UNIVERSE - ORDER EVENT SOURCING                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Order Aggregate Events:                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ • OrderCreatedEvent      - 주문 생성                                │  │
│   │ • OrderItemAddedEvent    - 상품 추가                                │  │
│   │ • OrderItemRemovedEvent  - 상품 제거                                │  │
│   │ • PaymentCompletedEvent  - 결제 완료                                │  │
│   │ • OrderConfirmedEvent    - 주문 확정                                │  │
│   │ • OrderShippedEvent      - 배송 시작                                │  │
│   │ • OrderDeliveredEvent    - 배송 완료                                │  │
│   │ • OrderCancelledEvent    - 주문 취소                                │  │
│   │ • OrderRefundedEvent     - 환불 처리                                │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   이벤트 흐름:                                                               │
│                                                                             │
│   Customer                                                                  │
│      │                                                                      │
│      ▼                                                                      │
│   ┌──────────────┐    Events     ┌──────────────┐                          │
│   │ Order        │──────────────►│ Event Store  │                          │
│   │ Aggregate    │               │  (PostgreSQL)│                          │
│   └──────────────┘               └──────────────┘                          │
│                                         │                                   │
│                    ┌────────────────────┼────────────────────┐              │
│                    │                    │                    │              │
│                    ▼                    ▼                    ▼              │
│             ┌───────────┐        ┌───────────┐        ┌───────────┐        │
│             │ Order     │        │ Customer  │        │ Analytics │        │
│             │ Projection│        │ Dashboard │        │ Projection│        │
│             └───────────┘        └───────────┘        └───────────┘        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 구현 예시

```java
// application.yml
spring:
  datasource:
    event-store:
      url: jdbc:postgresql://localhost:5432/portal_events
      driver-class-name: org.postgresql.Driver

// OrderEventEntity.java
@Entity
@Table(name = "order_events")
public class OrderEventEntity {
    @Id
    private String eventId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private int eventVersion;

    @Column(nullable = false)
    private int aggregateVersion;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(nullable = false)
    private Instant createdAt;
}

// OrderService with Event Sourcing
@Service
@RequiredArgsConstructor
public class OrderService {

    private final EventSourcedOrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Aggregate 생성 및 이벤트 발생
        Order order = Order.create(
            UUID.randomUUID().toString(),
            request.getCustomerId(),
            request.getItems()
        );

        // 2. Event Store에 저장
        orderRepository.save(order);

        // 3. 외부 이벤트 발행 (Kafka)
        order.getUncommittedEvents().forEach(event ->
            eventPublisher.publishEvent(event)
        );

        return OrderResponse.from(order);
    }

    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .map(OrderResponse::from)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));
    }

    /**
     * Time Travel - 특정 시점의 주문 상태 조회
     */
    public OrderResponse getOrderAsOf(String orderId, Instant asOf) {
        return orderRepository.findByIdAsOf(orderId, asOf)
            .map(OrderResponse::from)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));
    }

    /**
     * Audit - 주문 변경 이력 조회
     */
    public List<OrderEventResponse> getOrderHistory(String orderId) {
        return orderRepository.getEventHistory(orderId)
            .stream()
            .map(OrderEventResponse::from)
            .toList();
    }
}
```

### 6.3 Projection 구현 (CQRS와 연계)

```java
/**
 * Order Projection - 읽기 최적화된 뷰
 */
@Service
@RequiredArgsConstructor
public class OrderProjectionService {

    private final OrderReadRepository readRepository;

    /**
     * 이벤트 핸들러 - 읽기 모델 업데이트
     */
    @EventHandler
    public void on(OrderCreatedEvent event) {
        OrderReadModel model = OrderReadModel.builder()
            .orderId(event.getAggregateId())
            .customerId(event.getCustomerId())
            .status(OrderStatus.CREATED)
            .totalAmount(event.getTotalAmount())
            .itemCount(event.getItems().size())
            .createdAt(event.getOccurredAt())
            .lastUpdatedAt(event.getOccurredAt())
            .build();

        readRepository.save(model);
    }

    @EventHandler
    public void on(OrderShippedEvent event) {
        readRepository.findById(event.getAggregateId())
            .ifPresent(order -> {
                order.setStatus(OrderStatus.SHIPPED);
                order.setTrackingNumber(event.getTrackingNumber());
                order.setLastUpdatedAt(event.getOccurredAt());
                readRepository.save(order);
            });
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        readRepository.findById(event.getAggregateId())
            .ifPresent(order -> {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason(event.getReason());
                order.setLastUpdatedAt(event.getOccurredAt());
                readRepository.save(order);
            });
    }
}
```

---

## 7. 모범 사례

### 7.1 이벤트 설계 원칙

| 원칙 | 설명 |
|------|------|
| **과거 시제 사용** | OrderCreated (O), CreateOrder (X) |
| **불변성** | 저장된 이벤트는 절대 수정하지 않음 |
| **자기 완결성** | 이벤트만으로 상태 재구성 가능 |
| **비즈니스 의미** | 기술적 세부사항보다 비즈니스 의미 담기 |
| **작은 이벤트** | 하나의 이벤트 = 하나의 변경 사실 |

### 7.2 성능 최적화

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PERFORMANCE OPTIMIZATION                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. Snapshot 전략                                                          │
│      • 100개 이벤트마다 Snapshot 생성                                        │
│      • 비동기 Snapshot 생성                                                  │
│                                                                             │
│   2. 인덱싱                                                                  │
│      • aggregate_id + aggregate_version 복합 인덱스                         │
│      • event_type 인덱스 (Projection용)                                     │
│      • created_at 인덱스 (Time Travel용)                                    │
│                                                                             │
│   3. Projection 최적화                                                       │
│      • 읽기 전용 복제본 사용                                                 │
│      • 비동기 Projection 업데이트                                            │
│      • Projection별 최적화된 데이터 구조                                     │
│                                                                             │
│   4. Event Store 파티셔닝                                                    │
│      • aggregate_id 기반 파티셔닝                                            │
│      • 날짜 기반 파티셔닝 (아카이빙 용이)                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 연관 패턴

| 패턴 | 관계 |
|------|------|
| **CQRS** | Event Sourcing의 자연스러운 동반자 |
| **Saga** | 이벤트 기반 분산 트랜잭션 |
| **Outbox** | 이벤트 발행 보장 |
| **Domain Events** | DDD의 도메인 이벤트와 연결 |

---

## 9. 참고 자료

- [Martin Fowler - Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Event Store Documentation](https://www.eventstore.com/docs/)
- [Axon Framework](https://docs.axoniq.io/reference-guide/)
- Portal Universe: `services/shopping-service/` 예제 참조
