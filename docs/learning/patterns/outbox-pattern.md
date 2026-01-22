# Outbox 패턴

## 학습 목표
- Outbox 패턴의 원리와 필요성 이해
- 트랜잭션 메시지 발행의 문제점과 해결 방안
- 이벤트 발행 보장 메커니즘 구현
- Portal Universe에서의 적용 방안

---

## 1. Outbox 패턴 개요

### 1.1 문제: 이중 쓰기 (Dual Write Problem)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    THE DUAL WRITE PROBLEM                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   시나리오: 주문 생성 후 이벤트 발행                                          │
│                                                                             │
│   @Transactional                                                            │
│   void createOrder(request) {                                               │
│       Order order = ...;                                                    │
│       orderRepository.save(order);     // 1. DB 저장 (성공)                 │
│       kafkaTemplate.send("order-events", event);  // 2. Kafka 발행          │
│   }                                                                         │
│                                                                             │
│   문제 상황들:                                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   Case 1: DB 성공, Kafka 실패                                        │  │
│   │   ┌────────┐    ┌────────┐    ┌────────┐                            │  │
│   │   │ DB     │ ✓  │ Kafka  │ ✗  │ Result │                            │  │
│   │   │ COMMIT │────│ SEND   │────│ 불일치!│                            │  │
│   │   └────────┘    └────────┘    └────────┘                            │  │
│   │   → 주문은 저장됐지만 다른 서비스는 모름                              │  │
│   │                                                                      │  │
│   │   Case 2: Kafka 성공, DB 실패 (롤백)                                 │  │
│   │   ┌────────┐    ┌────────┐    ┌────────┐                            │  │
│   │   │ Kafka  │ ✓  │ DB     │ ✗  │ Result │                            │  │
│   │   │ SEND   │────│ ROLLBACK────│ 불일치!│                            │  │
│   │   └────────┘    └────────┘    └────────┘                            │  │
│   │   → 이벤트는 발행됐지만 실제 주문은 없음                              │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   핵심 문제: DB와 메시지 브로커는 별개의 트랜잭션                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Outbox 패턴 개념

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       OUTBOX PATTERN CONCEPT                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   핵심 아이디어:                                                             │
│   "이벤트를 같은 DB의 Outbox 테이블에 먼저 저장한 후,                        │
│    별도 프로세스가 Outbox를 읽어 메시지 브로커로 발행"                        │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   @Transactional  ← 단일 트랜잭션                                    │  │
│   │   void createOrder(request) {                                        │  │
│   │       orderRepository.save(order);                                   │  │
│   │       outboxRepository.save(outboxEvent);  // 같은 DB!               │  │
│   │   }                                                                  │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   전체 흐름:                                                                 │
│                                                                             │
│   ┌────────────┐    ┌─────────────────────────────────┐                    │
│   │ Application│    │           Database              │                    │
│   │            │    │  ┌─────────┐  ┌──────────────┐  │                    │
│   │  Order     │────│──│ orders  │  │   outbox     │  │                    │
│   │  Service   │    │  │  table  │  │   table      │  │                    │
│   │            │    │  └─────────┘  └──────┬───────┘  │                    │
│   └────────────┘    └──────────────────────┼──────────┘                    │
│                                            │                                │
│                                  ┌─────────▼─────────┐                      │
│                                  │  Outbox Relay     │                      │
│                                  │  (Polling/CDC)    │                      │
│                                  └─────────┬─────────┘                      │
│                                            │                                │
│                                  ┌─────────▼─────────┐                      │
│                                  │     Kafka         │                      │
│                                  │  Message Broker   │                      │
│                                  └───────────────────┘                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Outbox 패턴의 장점

| 장점 | 설명 |
|------|------|
| **원자성 보장** | 비즈니스 데이터와 이벤트가 동일 트랜잭션 |
| **최소 1회 전달** | Outbox 재처리로 이벤트 손실 방지 |
| **순서 보장** | Outbox 레코드 순서대로 발행 가능 |
| **재시도 용이** | 발행 실패 시 재시도 가능 |
| **감사 로그** | Outbox 테이블이 이벤트 히스토리 역할 |

---

## 2. Outbox 테이블 설계

### 2.1 기본 스키마

```sql
-- Outbox Table
CREATE TABLE outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Aggregate 정보
    aggregate_type  VARCHAR(100) NOT NULL,     -- 예: 'Order'
    aggregate_id    VARCHAR(100) NOT NULL,     -- 예: 주문 ID

    -- 이벤트 정보
    event_type      VARCHAR(100) NOT NULL,     -- 예: 'OrderCreated'
    payload         JSONB NOT NULL,            -- 이벤트 데이터

    -- 라우팅 정보
    topic           VARCHAR(100) NOT NULL,     -- Kafka topic
    partition_key   VARCHAR(100),              -- 파티션 키

    -- 상태 관리
    status          VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, SENT, FAILED
    retry_count     INT DEFAULT 0,
    last_error      TEXT,

    -- 시간 정보
    created_at      TIMESTAMP DEFAULT NOW(),
    processed_at    TIMESTAMP,

    -- 인덱스
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- 조회 최적화 인덱스
CREATE INDEX idx_outbox_pending ON outbox(status, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate ON outbox(aggregate_type, aggregate_id);
```

### 2.2 Outbox Entity

```java
/**
 * Outbox Entity
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String topic;

    private String partitionKey;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    private int retryCount = 0;

    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    // Factory Method
    public static OutboxEvent create(String aggregateType, String aggregateId,
                                     String eventType, Object payload,
                                     String topic) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = serializePayload(payload);
        event.topic = topic;
        event.partitionKey = aggregateId;  // 기본 파티션 키
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = error;
        this.retryCount++;
    }

    public void retry() {
        this.status = OutboxStatus.PENDING;
    }

    public boolean isRetryable(int maxRetries) {
        return retryCount < maxRetries;
    }

    private static String serializePayload(Object payload) {
        try {
            return new ObjectMapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}

public enum OutboxStatus {
    PENDING,    // 발행 대기
    SENT,       // 발행 완료
    FAILED      // 발행 실패
}
```

---

## 3. Outbox 구현 방식

### 3.1 Polling 기반

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    POLLING-BASED OUTBOX                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   while (true) {                                                     │  │
│   │       events = outboxRepository.findPending(limit);                  │  │
│   │       for (event : events) {                                         │  │
│   │           try {                                                      │  │
│   │               kafkaTemplate.send(event.topic, event.payload);        │  │
│   │               event.markAsSent();                                    │  │
│   │           } catch (Exception e) {                                    │  │
│   │               event.markAsFailed(e);                                 │  │
│   │           }                                                          │  │
│   │       }                                                              │  │
│   │       Thread.sleep(pollingInterval);                                 │  │
│   │   }                                                                  │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   장점:                                                                     │
│   • 구현이 간단                                                             │
│   • 추가 인프라 불필요                                                       │
│                                                                             │
│   단점:                                                                     │
│   • Polling 간격만큼 지연                                                   │
│   • DB 부하 (주기적 쿼리)                                                    │
│   • 다중 인스턴스 시 동시성 처리 필요                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 CDC (Change Data Capture) 기반

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CDC-BASED OUTBOX (Debezium)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐   WAL/Binlog    ┌─────────────┐    ┌─────────────┐       │
│   │  PostgreSQL │────────────────►│  Debezium   │───►│   Kafka     │       │
│   │   outbox    │                 │  Connector  │    │   Topic     │       │
│   └─────────────┘                 └─────────────┘    └─────────────┘       │
│                                                                             │
│   장점:                                                                     │
│   • 실시간에 가까운 이벤트 전파                                              │
│   • DB 부하 최소화 (Log 기반)                                               │
│   • 순서 보장                                                               │
│                                                                             │
│   단점:                                                                     │
│   • 추가 인프라 필요 (Debezium, Kafka Connect)                              │
│   • 설정 복잡도                                                             │
│   • DB에 따른 설정 차이                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Java 구현

### 4.1 Outbox Service (Polling 방식)

```java
/**
 * Outbox Service - 이벤트 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Outbox에 이벤트 저장 (트랜잭션 참여)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId,
                         String eventType, Object payload, String topic) {
        OutboxEvent event = OutboxEvent.create(
            aggregateType, aggregateId, eventType, payload, topic
        );

        outboxRepository.save(event);
        log.debug("Saved outbox event: type={}, id={}", eventType, event.getId());
    }

    /**
     * 도메인 이벤트를 Outbox에 저장
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(DomainEvent domainEvent, String topic) {
        saveEvent(
            domainEvent.getAggregateType(),
            domainEvent.getAggregateId(),
            domainEvent.getEventType(),
            domainEvent,
            topic
        );
    }
}

/**
 * Outbox Relay - Polling 방식 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    /**
     * 주기적으로 Pending 이벤트 발행
     */
    @Scheduled(fixedDelayString = "${outbox.polling-interval:1000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findByStatusOrderByCreatedAt(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            // Kafka로 발행
            kafkaTemplate.send(
                event.getTopic(),
                event.getPartitionKey(),
                event.getPayload()
            ).get(5, TimeUnit.SECONDS);  // 동기 대기

            // 성공 처리
            event.markAsSent();
            outboxRepository.save(event);

            log.debug("Successfully sent outbox event: id={}, type={}",
                     event.getId(), event.getEventType());

        } catch (Exception e) {
            log.error("Failed to send outbox event: id={}, error={}",
                     event.getId(), e.getMessage());

            event.markAsFailed(e.getMessage());
            outboxRepository.save(event);

            // 재시도 가능 여부 확인
            if (!event.isRetryable(MAX_RETRIES)) {
                // 알림 발송 (수동 처리 필요)
                alertDeadLetter(event);
            }
        }
    }

    /**
     * 실패 이벤트 재시도
     */
    @Scheduled(fixedDelayString = "${outbox.retry-interval:60000}")
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxRepository
            .findByStatusAndRetryCountLessThan(OutboxStatus.FAILED, MAX_RETRIES);

        for (OutboxEvent event : failedEvents) {
            event.retry();
            outboxRepository.save(event);
        }

        if (!failedEvents.isEmpty()) {
            log.info("Scheduled {} failed events for retry", failedEvents.size());
        }
    }

    private void alertDeadLetter(OutboxEvent event) {
        log.error("Dead letter event: id={}, type={}, aggregateId={}",
                 event.getId(), event.getEventType(), event.getAggregateId());
        // 알림 서비스 호출 또는 Dead Letter 테이블로 이동
    }
}
```

### 4.2 비즈니스 로직에서 사용

```java
/**
 * Order Service - Outbox 패턴 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    /**
     * 주문 생성 - 단일 트랜잭션으로 주문과 이벤트 저장
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // 1. 주문 생성 및 저장
        Order order = Order.create(request);
        orderRepository.save(order);

        // 2. Outbox에 이벤트 저장 (같은 트랜잭션!)
        outboxService.saveEvent(
            "Order",
            order.getId(),
            "OrderCreated",
            OrderCreatedEvent.from(order),
            "order-events"
        );

        log.info("Order created: id={}", order.getId());
        return CreateOrderResponse.from(order);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));

        order.cancel(reason);
        orderRepository.save(order);

        // Outbox에 취소 이벤트 저장
        outboxService.saveEvent(
            "Order",
            orderId,
            "OrderCancelled",
            new OrderCancelledEvent(orderId, reason, LocalDateTime.now()),
            "order-events"
        );
    }

    /**
     * 여러 이벤트 발행이 필요한 경우
     */
    @Transactional
    public void completeOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));

        order.complete();
        orderRepository.save(order);

        // 여러 이벤트를 Outbox에 저장
        outboxService.saveEvent(
            "Order", orderId, "OrderCompleted",
            new OrderCompletedEvent(orderId),
            "order-events"
        );

        outboxService.saveEvent(
            "Order", orderId, "RewardPointsEarned",
            new RewardPointsEarnedEvent(order.getCustomerId(), calculatePoints(order)),
            "reward-events"
        );

        outboxService.saveEvent(
            "Order", orderId, "NotificationRequired",
            new OrderCompletionNotification(order),
            "notification-events"
        );
    }
}
```

### 4.3 분산 락을 사용한 동시성 처리

```java
/**
 * Outbox Relay with Distributed Lock
 * 다중 인스턴스 환경에서 중복 처리 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedOutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "outbox-relay-lock";

    @Scheduled(fixedDelay = 1000)
    public void processOutbox() {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            // 락 획득 시도 (최대 100ms 대기, 30초 유지)
            if (lock.tryLock(100, 30000, TimeUnit.MILLISECONDS)) {
                try {
                    doProcessOutbox();
                } finally {
                    lock.unlock();
                }
            } else {
                log.trace("Could not acquire outbox relay lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Outbox relay interrupted");
        }
    }

    @Transactional
    protected void doProcessOutbox() {
        List<OutboxEvent> events = outboxRepository
            .findAndLockPendingEvents(100);  // SELECT ... FOR UPDATE

        for (OutboxEvent event : events) {
            // 발행 처리
        }
    }
}
```

---

## 5. Debezium CDC 설정

### 5.1 Debezium Outbox Connector

```json
// debezium-outbox-connector.json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",

    // Database 연결
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "portal",
    "database.password": "${DB_PASSWORD}",
    "database.dbname": "shopping_db",
    "database.server.name": "shopping",

    // Outbox 테이블만 캡처
    "table.include.list": "public.outbox",

    // Outbox Event Router 사용
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",

    // Event Router 설정
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.table.expand.json.payload": "true",

    // Topic 라우팅
    "transforms.outbox.route.by.field": "topic",
    "transforms.outbox.route.topic.replacement": "${routedByValue}",

    // 처리 완료된 레코드 삭제
    "transforms.outbox.table.fields.additional.placement": "event_type:header:eventType"
  }
}
```

### 5.2 Docker Compose 설정

```yaml
# docker-compose.yml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  kafka-connect:
    image: debezium/connect:2.4
    depends_on:
      - kafka
      - postgres
    ports:
      - "8083:8083"
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: connect-cluster
      CONFIG_STORAGE_TOPIC: connect-configs
      OFFSET_STORAGE_TOPIC: connect-offsets
      STATUS_STORAGE_TOPIC: connect-status
      KEY_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      VALUE_CONVERTER: org.apache.kafka.connect.json.JsonConverter

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: shopping_db
      POSTGRES_USER: portal
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    command:
      - "postgres"
      - "-c"
      - "wal_level=logical"  # CDC를 위해 필수
```

---

## 6. Portal Universe 적용

### 6.1 Shopping Service Outbox 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              PORTAL UNIVERSE - OUTBOX ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   services/shopping-service/                                                │
│   ├── src/main/java/.../shopping/                                           │
│   │   ├── order/                                                            │
│   │   │   ├── service/                                                      │
│   │   │   │   └── OrderService.java  ← Outbox 사용                          │
│   │   │   └── event/                                                        │
│   │   │       ├── OrderCreatedEvent.java                                    │
│   │   │       └── OrderCancelledEvent.java                                  │
│   │   │                                                                     │
│   │   └── outbox/                    ← Outbox 모듈                          │
│   │       ├── entity/                                                       │
│   │       │   └── OutboxEvent.java                                          │
│   │       ├── repository/                                                   │
│   │       │   └── OutboxRepository.java                                     │
│   │       ├── service/                                                      │
│   │       │   └── OutboxService.java                                        │
│   │       └── relay/                                                        │
│   │           └── OutboxRelay.java                                          │
│   │                                                                         │
│   └── src/main/resources/                                                   │
│       └── application.yml                                                   │
│                                                                             │
│   이벤트 흐름:                                                               │
│                                                                             │
│   ┌─────────────┐   Transaction   ┌─────────────────────────────────────┐  │
│   │   Order     │────────────────►│  PostgreSQL                         │  │
│   │   Service   │                 │  ┌─────────┐    ┌─────────────────┐ │  │
│   │             │                 │  │ orders  │    │     outbox      │ │  │
│   └─────────────┘                 │  └─────────┘    └────────┬────────┘ │  │
│                                   └──────────────────────────┼──────────┘  │
│                                                              │              │
│                                              ┌───────────────┘              │
│                                              │                              │
│                                   ┌──────────▼───────────┐                  │
│                                   │   Outbox Relay       │                  │
│                                   │   (Polling/CDC)      │                  │
│                                   └──────────┬───────────┘                  │
│                                              │                              │
│                        ┌─────────────────────┼─────────────────────┐        │
│                        │                     │                     │        │
│                        ▼                     ▼                     ▼        │
│                 ┌────────────┐        ┌────────────┐        ┌────────────┐  │
│                 │order-events│        │inventory   │        │notification│  │
│                 │   (Kafka)  │        │  -events   │        │  -events   │  │
│                 └────────────┘        └────────────┘        └────────────┘  │
│                        │                     │                     │        │
│                        ▼                     ▼                     ▼        │
│                 ┌────────────┐        ┌────────────┐        ┌────────────┐  │
│                 │Notification│        │ Inventory  │        │   Other    │  │
│                 │  Service   │        │  Service   │        │  Services  │  │
│                 └────────────┘        └────────────┘        └────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 설정

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shopping_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# Outbox 설정
outbox:
  enabled: true
  polling-interval: 1000     # 1초마다 polling
  retry-interval: 60000      # 1분마다 실패 이벤트 재시도
  batch-size: 100
  max-retries: 3

# Kafka 설정
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.apache.kafka.common.serialization.StringSerializer
    acks: all                # 모든 replica 확인
    retries: 3
    properties:
      enable.idempotence: true  # 멱등성 보장

# 스케줄러 설정
spring:
  task:
    scheduling:
      pool:
        size: 2
```

### 6.3 Outbox Repository

```java
/**
 * Outbox Repository
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Pending 이벤트 조회 (순서 보장)
     */
    List<OutboxEvent> findByStatusOrderByCreatedAt(
        OutboxStatus status, Pageable pageable);

    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    @Query("SELECT e FROM OutboxEvent e " +
           "WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries")
    List<OutboxEvent> findRetryableFailedEvents(@Param("maxRetries") int maxRetries);

    /**
     * 비관적 락으로 Pending 이벤트 조회 (동시성 제어)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e " +
           "WHERE e.status = 'PENDING' " +
           "ORDER BY e.createdAt")
    List<OutboxEvent> findAndLockPendingEvents(Pageable pageable);

    /**
     * 오래된 처리 완료 이벤트 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e " +
           "WHERE e.status = 'SENT' AND e.processedAt < :before")
    int deleteOldProcessedEvents(@Param("before") LocalDateTime before);

    /**
     * Aggregate별 이벤트 조회 (디버깅용)
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAt(
        String aggregateType, String aggregateId);
}
```

---

## 7. 고급 기능

### 7.1 이벤트 순서 보장

```java
/**
 * Aggregate별 순서 보장을 위한 Outbox Relay
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderedOutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Aggregate별로 그룹화하여 순서대로 처리
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        // 1. Pending 이벤트를 Aggregate별로 그룹화
        List<OutboxEvent> events = outboxRepository
            .findByStatusOrderByCreatedAt(OutboxStatus.PENDING, PageRequest.of(0, 100));

        Map<String, List<OutboxEvent>> byAggregate = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.getAggregateType() + ":" + e.getAggregateId(),
                LinkedHashMap::new,  // 순서 유지
                Collectors.toList()
            ));

        // 2. 각 Aggregate의 이벤트를 순서대로 처리
        for (Map.Entry<String, List<OutboxEvent>> entry : byAggregate.entrySet()) {
            processAggregateEvents(entry.getValue());
        }
    }

    private void processAggregateEvents(List<OutboxEvent> events) {
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                    event.getTopic(),
                    event.getPartitionKey(),  // 같은 Aggregate는 같은 파티션
                    event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.markAsSent();

            } catch (Exception e) {
                event.markAsFailed(e.getMessage());
                // 이 Aggregate의 나머지 이벤트는 건너뜀 (순서 보장)
                log.warn("Stopping processing for aggregate due to failure: {}",
                        event.getAggregateId());
                break;
            }

            outboxRepository.save(event);
        }
    }
}
```

### 7.2 멱등성 처리

```java
/**
 * Consumer 측 멱등성 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderEvent(ConsumerRecord<String, String> record) {
        String eventId = record.headers()
            .lastHeader("event-id")
            .value()
            .toString();

        // 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping already processed event: {}", eventId);
            return;
        }

        // 이벤트 처리
        OrderCreatedEvent event = parseEvent(record.value());
        inventoryService.reserveStock(event);

        // 처리 완료 기록
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
    }
}
```

### 7.3 Outbox 정리 (Cleanup)

```java
/**
 * Outbox 정리 스케줄러
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupService {

    private final OutboxRepository outboxRepository;

    @Value("${outbox.cleanup.retention-days:7}")
    private int retentionDays;

    /**
     * 오래된 처리 완료 이벤트 삭제 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        int deleted = outboxRepository.deleteOldProcessedEvents(cutoff);

        log.info("Cleaned up {} old outbox events (older than {})",
                deleted, cutoff);
    }

    /**
     * 실패 이벤트 아카이브 (Dead Letter)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void archiveDeadLetters() {
        List<OutboxEvent> deadLetters = outboxRepository
            .findByStatusAndRetryCountGreaterThanEqual(
                OutboxStatus.FAILED, 3);

        for (OutboxEvent event : deadLetters) {
            // Dead Letter 테이블로 이동
            archiveToDeadLetter(event);
            outboxRepository.delete(event);
        }

        if (!deadLetters.isEmpty()) {
            log.warn("Archived {} dead letter events", deadLetters.size());
        }
    }
}
```

---

## 8. 모범 사례

### 8.1 체크리스트

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      OUTBOX PATTERN CHECKLIST                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   설계:                                                                      │
│   ☐ Outbox 테이블에 적절한 인덱스 설정                                       │
│   ☐ Aggregate별 파티션 키로 순서 보장                                        │
│   ☐ 이벤트 페이로드에 필요한 정보 모두 포함                                   │
│                                                                             │
│   구현:                                                                      │
│   ☐ 비즈니스 트랜잭션과 Outbox 저장은 같은 트랜잭션                          │
│   ☐ Consumer에서 멱등성 처리                                                 │
│   ☐ 적절한 재시도 정책                                                       │
│   ☐ Dead Letter 처리                                                        │
│                                                                             │
│   운영:                                                                      │
│   ☐ Outbox 테이블 정리 스케줄러                                              │
│   ☐ 모니터링 및 알림                                                         │
│   ☐ 백로그 모니터링                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 주의사항

| 주의사항 | 해결 방안 |
|---------|----------|
| Outbox 테이블 증가 | 정기적 Cleanup |
| 중복 발행 가능 | Consumer 멱등성 |
| 순서 보장 범위 | Aggregate 단위 |
| Polling 지연 | CDC 또는 짧은 간격 |

---

## 9. 연관 패턴

| 패턴 | 관계 |
|------|------|
| **Event Sourcing** | Outbox 대신 Event Store 사용 가능 |
| **Saga** | 분산 트랜잭션에서 이벤트 발행 보장 |
| **CQRS** | Projection 업데이트용 이벤트 발행 |
| **Idempotent Consumer** | 중복 메시지 처리 |

---

## 10. 참고 자료

- [Microservices.io - Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [Debezium Outbox Pattern](https://debezium.io/documentation/reference/transformations/outbox-event-router.html)
- [Chris Richardson - Eventuate Tram](https://eventuate.io/docs/manual/eventuate-tram/latest/)
- Portal Universe: `services/shopping-service/` 예제 참조
