# Kafka Consumer 심화

## 학습 목표
- Consumer의 내부 동작 메커니즘 이해 (Poll Loop, Heartbeat)
- Offset 관리 전략 (Auto/Manual Commit) 숙지
- Consumer Group Rebalancing 과정 파악
- Partition Assignment Strategy 선택 기준 학습
- Spring Kafka의 @KafkaListener 활용법 습득

---

## 1. Consumer 내부 동작

### 1.1 Consumer의 기본 동작 원리

Kafka Consumer는 **Pull 모델**을 사용합니다. Consumer가 능동적으로 Broker에게 메시지를 요청합니다.

```
┌─────────────┐                    ┌─────────────┐
│   Consumer  │ ──── poll() ────► │   Broker    │
│             │ ◄── messages ──── │             │
│             │                    │             │
│             │ ── heartbeat() ─► │             │
│             │ ◄─── ack ──────── │             │
└─────────────┘                    └─────────────┘
```

**Pull 모델의 장점:**
- Consumer가 처리 속도에 맞춰 메시지 가져옴
- 백프레셔(Backpressure) 자연스럽게 처리
- 배치 처리 최적화 가능

### 1.2 Poll Loop

Consumer의 핵심은 **Poll Loop**입니다. `poll()` 메서드가 여러 역할을 동시에 수행합니다.

```java
// 기본 Consumer Poll Loop 예시
while (running) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

    for (ConsumerRecord<String, String> record : records) {
        // 메시지 처리
        processRecord(record);
    }

    // 수동 커밋 (enable.auto.commit=false 일 때)
    consumer.commitSync();
}
```

**poll() 메서드의 역할:**

| 역할 | 설명 |
|------|------|
| **메시지 가져오기** | Broker에서 레코드 배치 조회 |
| **Heartbeat 전송** | Consumer 생존 신호 (3.x 이전 버전) |
| **Coordinator 통신** | Consumer Group 상태 관리 |
| **Rebalance 처리** | 파티션 재할당 콜백 실행 |
| **Offset 커밋** | Auto commit 활성화 시 주기적 커밋 |

### 1.3 Heartbeat와 Session Timeout

**Kafka 3.x 이후**부터 Heartbeat는 별도 스레드에서 동작합니다.

```
┌─────────────────────────────────────────────────────────┐
│                    Consumer Process                      │
│  ┌──────────────────┐    ┌──────────────────────────┐   │
│  │  Main Thread     │    │  Heartbeat Thread        │   │
│  │  - poll()        │    │  - heartbeat 전송        │   │
│  │  - 메시지 처리    │    │  - session.timeout 관리  │   │
│  │  - commit()      │    │  - 3초마다 heartbeat     │   │
│  └──────────────────┘    └──────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**주요 설정:**

```properties
# Heartbeat 간격 (기본값: 3초)
heartbeat.interval.ms=3000

# 세션 타임아웃 (기본값: 45초)
# 이 시간 내 heartbeat 없으면 Consumer dead로 판단
session.timeout.ms=45000

# poll() 간격 최대 시간 (기본값: 5분)
# 이 시간 내 poll() 없으면 Consumer 제외
max.poll.interval.ms=300000
```

**타임아웃 관계:**

```
heartbeat.interval.ms < session.timeout.ms < max.poll.interval.ms
      (3s)                   (45s)                  (5m)
```

### 1.4 Fetch 설정

Consumer가 Broker에서 데이터를 가져오는 방식을 제어합니다.

```properties
# 한 번에 가져올 최소 바이트 (기본값: 1)
fetch.min.bytes=1

# 한 번에 가져올 최대 바이트 (기본값: 50MB)
fetch.max.bytes=52428800

# 파티션당 최대 바이트 (기본값: 1MB)
max.partition.fetch.bytes=1048576

# fetch.min.bytes 충족까지 대기 시간 (기본값: 500ms)
fetch.max.wait.ms=500

# poll() 한 번에 반환할 최대 레코드 수 (기본값: 500)
max.poll.records=500
```

**처리량 vs 지연시간 트레이드오프:**

| 목표 | 설정 방향 |
|------|----------|
| **높은 처리량** | fetch.min.bytes ↑, fetch.max.wait.ms ↑, max.poll.records ↑ |
| **낮은 지연시간** | fetch.min.bytes ↓, fetch.max.wait.ms ↓ |

---

## 2. Offset 관리

### 2.1 Offset이란?

Offset은 파티션 내 메시지의 **고유한 순차 번호**입니다.

```
Partition 0:
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│  0  │  1  │  2  │  3  │  4  │  5  │  6  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                    ↑           ↑
             Committed      Current
              Offset        Position
               (3)            (5)
```

**Offset 종류:**

| 종류 | 설명 |
|------|------|
| **Current Position** | Consumer가 현재 읽고 있는 위치 |
| **Committed Offset** | 처리 완료를 Kafka에 기록한 위치 |
| **Log End Offset (LEO)** | 파티션의 마지막 메시지 위치 |
| **High Watermark** | 모든 복제본에 복제된 마지막 위치 |

### 2.2 Auto Commit

**자동 커밋**은 가장 간단한 방식입니다.

```properties
# 자동 커밋 활성화 (기본값: true)
enable.auto.commit=true

# 자동 커밋 간격 (기본값: 5초)
auto.commit.interval.ms=5000
```

**동작 방식:**

```
Time: ─────────────────────────────────────────────►
       │ poll() │ process │ poll() │ process │ poll()
       ▼        ▼         ▼        ▼         ▼
       │ records│ 처리중  │ records│ 처리중  │
       │        │         │        │         │
       └────────┴─────────┴────────┴─────────┘
                          ↑
                    5초 경과 시
                   Auto Commit
```

**Auto Commit의 문제점:**

```java
// 위험한 시나리오
ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));
// ↑ poll() 시점에 이전 배치가 auto commit됨

for (ConsumerRecord<K, V> record : records) {
    processRecord(record);  // 처리 중 장애 발생!
}
// 이미 커밋된 메시지는 재처리되지 않음 → 메시지 손실
```

**결론:** 메시지 손실이 허용되지 않는 경우 **수동 커밋** 사용

### 2.3 Manual Commit

#### 2.3.1 Synchronous Commit

```java
// 동기 커밋 - 커밋 완료까지 블로킹
try {
    while (running) {
        ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<K, V> record : records) {
            processRecord(record);
        }

        // 처리 완료 후 커밋
        consumer.commitSync();
    }
} catch (CommitFailedException e) {
    log.error("Commit failed", e);
}
```

**특정 오프셋만 커밋:**

```java
Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

for (ConsumerRecord<K, V> record : records) {
    processRecord(record);

    // 레코드별 오프셋 저장 (다음 읽을 위치 = current + 1)
    offsets.put(
        new TopicPartition(record.topic(), record.partition()),
        new OffsetAndMetadata(record.offset() + 1)
    );
}

consumer.commitSync(offsets);
```

#### 2.3.2 Asynchronous Commit

```java
// 비동기 커밋 - 논블로킹, 콜백으로 결과 처리
consumer.commitAsync((offsets, exception) -> {
    if (exception != null) {
        log.error("Async commit failed for offsets: {}", offsets, exception);
    } else {
        log.debug("Async commit succeeded for offsets: {}", offsets);
    }
});
```

**Sync vs Async Commit:**

| 방식 | 장점 | 단점 | 사용 시점 |
|------|------|------|----------|
| **commitSync()** | 신뢰성 보장 | 지연시간 증가 | 중요한 데이터, 종료 시 |
| **commitAsync()** | 높은 처리량 | 실패 시 재시도 어려움 | 일반적인 처리 |

#### 2.3.3 하이브리드 패턴 (권장)

```java
try {
    while (running) {
        ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<K, V> record : records) {
            processRecord(record);
        }

        // 일반적으로 비동기 커밋 (처리량 우선)
        consumer.commitAsync();
    }
} finally {
    try {
        // 종료 시에는 동기 커밋 (신뢰성 우선)
        consumer.commitSync();
    } finally {
        consumer.close();
    }
}
```

### 2.4 auto.offset.reset 설정

Consumer Group이 처음 시작하거나 커밋된 오프셋이 없을 때 동작을 결정합니다.

```properties
# earliest: 파티션 처음부터 읽기
auto.offset.reset=earliest

# latest: 최신 메시지부터 읽기 (기본값)
auto.offset.reset=latest

# none: 커밋된 오프셋 없으면 예외 발생
auto.offset.reset=none
```

**선택 기준:**

| 설정 | 사용 사례 |
|------|----------|
| **earliest** | 모든 메시지 처리 필요, 신규 Consumer Group |
| **latest** | 실시간 데이터만 필요, 과거 데이터 불필요 |
| **none** | 명시적 offset 관리 필요 |

---

## 3. Consumer Group Rebalancing

### 3.1 Rebalancing이란?

Consumer Group 내 파티션 할당을 **재조정**하는 과정입니다.

```
[Rebalance 전]                    [Rebalance 후]
Consumer Group                    Consumer Group
┌───────────────────────┐         ┌───────────────────────┐
│ C1: P0, P1            │         │ C1: P0                │
│ C2: P2                │   ───►  │ C2: P1                │
│ (C3 추가됨)            │         │ C3: P2                │
└───────────────────────┘         └───────────────────────┘
```

### 3.2 Rebalancing 트리거

| 트리거 | 설명 |
|--------|------|
| **Consumer 추가** | 새 Consumer가 그룹에 참여 |
| **Consumer 제거** | Consumer 종료 또는 장애 |
| **Consumer 장애** | session.timeout 초과, max.poll.interval 초과 |
| **토픽 변경** | 구독 토픽의 파티션 수 변경 |
| **정규식 매칭** | 구독 패턴에 새 토픽 매칭 |

### 3.3 Rebalancing 프로토콜

#### 3.3.1 Eager Rebalancing (기본, 전통적)

```
Phase 1: Revoke (모든 파티션 해제)
┌─────────────────────────────────────────────┐
│ C1: P0, P1 → (해제)                         │
│ C2: P2     → (해제)                         │
│                                             │
│ ⚠️ 이 순간 모든 Consumer가 멈춤 (Stop-the-World) │
└─────────────────────────────────────────────┘

Phase 2: Assign (파티션 재할당)
┌─────────────────────────────────────────────┐
│ C1: ← P0 (재할당)                           │
│ C2: ← P1 (재할당)                           │
│ C3: ← P2 (재할당)                           │
└─────────────────────────────────────────────┘
```

**문제점:** Rebalancing 동안 전체 Consumer Group이 메시지 처리 중단

#### 3.3.2 Cooperative Rebalancing (Incremental)

Kafka 2.4+부터 지원. 파티션을 **점진적으로** 재할당합니다.

```
Phase 1: 이동할 파티션만 해제
┌─────────────────────────────────────────────┐
│ C1: P0, P1 → P0 유지, P1 해제               │
│ C2: P2     → P2 유지                        │
│ C3:        → (대기)                         │
│                                             │
│ ✓ C1(P0), C2(P2)는 계속 처리               │
└─────────────────────────────────────────────┘

Phase 2: 해제된 파티션만 재할당
┌─────────────────────────────────────────────┐
│ C1: P0 (유지)                               │
│ C2: P2 (유지)                               │
│ C3: ← P1 (새로 할당)                        │
└─────────────────────────────────────────────┘
```

**설정 방법:**

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### 3.4 Rebalance Listener

Rebalancing 시 커스텀 로직을 실행합니다.

```java
consumer.subscribe(Collections.singleton("my-topic"), new ConsumerRebalanceListener() {

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // 파티션 해제 전 호출
        // 현재까지 처리한 오프셋 커밋
        log.info("Partitions revoked: {}", partitions);
        consumer.commitSync(getCurrentOffsets());
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // 파티션 할당 후 호출
        // 필요시 특정 오프셋으로 seek
        log.info("Partitions assigned: {}", partitions);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        // Cooperative rebalancing에서 파티션 손실 시
        // (다른 Consumer에게 이미 할당된 경우)
        log.warn("Partitions lost: {}", partitions);
    }
});
```

### 3.5 Static Group Membership

Rebalancing 빈도를 줄이기 위해 Consumer에 **고정 ID**를 부여합니다.

```properties
# Consumer별 고유 ID 부여
group.instance.id=consumer-instance-1
```

**효과:**
- Consumer 재시작 시 동일 파티션 재할당
- 일시적 장애 시 Rebalancing 지연 (session.timeout까지 대기)
- 롤링 업데이트 시 불필요한 Rebalancing 방지

---

## 4. Partition Assignment Strategies

### 4.1 RangeAssignor (기본)

토픽별로 파티션을 **연속 범위**로 할당합니다.

```
Topic A: P0, P1, P2, P3    Topic B: P0, P1, P2
Consumers: C0, C1

결과:
C0 ← Topic A: P0, P1      Topic B: P0, P1
C1 ← Topic A: P2, P3      Topic B: P2

문제: C0이 더 많은 파티션을 받음 (불균형)
```

### 4.2 RoundRobinAssignor

모든 파티션을 **라운드 로빈**으로 분배합니다.

```
전체 파티션: A-P0, A-P1, A-P2, A-P3, B-P0, B-P1, B-P2
Consumers: C0, C1

결과:
C0 ← A-P0, A-P2, B-P0, B-P2
C1 ← A-P1, A-P3, B-P1

더 균등한 분배
```

### 4.3 StickyAssignor

균등 분배 + **기존 할당 유지**를 목표로 합니다.

```
[초기 할당]
C0 ← P0, P1
C1 ← P2, P3
C2 ← P4, P5

[C2 이탈 후 - StickyAssignor]
C0 ← P0, P1, P4    # 기존 P0, P1 유지
C1 ← P2, P3, P5    # 기존 P2, P3 유지

[C2 이탈 후 - RangeAssignor]
C0 ← P0, P1, P2    # 완전히 재할당
C1 ← P3, P4, P5
```

**장점:** Rebalancing 시 파티션 이동 최소화

### 4.4 CooperativeStickyAssignor

StickyAssignor + **Cooperative Rebalancing** 지원

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### 4.5 Strategy 비교

| Strategy | 균등성 | 안정성 | Cooperative | 권장 상황 |
|----------|--------|--------|-------------|----------|
| RangeAssignor | 낮음 | 낮음 | ❌ | 레거시 호환 |
| RoundRobinAssignor | 높음 | 낮음 | ❌ | 균등 분배 필요 |
| StickyAssignor | 높음 | 높음 | ❌ | 안정성 중시 |
| **CooperativeStickyAssignor** | 높음 | 높음 | ✅ | **권장** |

---

## 5. Spring Kafka @KafkaListener

### 5.1 기본 사용법

```java
@Component
public class MyConsumer {

    // 가장 기본적인 형태
    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message) {
        log.info("Received: {}", message);
    }
}
```

### 5.2 ConsumerRecord 접근

```java
@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listen(ConsumerRecord<String, String> record) {
    log.info("Topic: {}, Partition: {}, Offset: {}, Key: {}, Value: {}",
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            record.value());
}
```

### 5.3 JSON 메시지 자동 역직렬화

```java
// DTO 클래스
public record OrderEvent(String orderId, String userId, BigDecimal amount) {}

// Consumer
@KafkaListener(topics = "orders", groupId = "order-group")
public void listen(OrderEvent event) {
    log.info("Order received: {}", event.orderId());
}
```

### 5.4 배치 처리

```java
// 여러 메시지를 한 번에 처리
@KafkaListener(
    topics = "my-topic",
    groupId = "my-group",
    containerFactory = "batchKafkaListenerContainerFactory"
)
public void listen(List<String> messages) {
    log.info("Batch size: {}", messages.size());
    messages.forEach(this::process);
}
```

**배치 Factory 설정:**

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory
) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setBatchListener(true);  // 배치 모드 활성화
    return factory;
}
```

### 5.5 수동 Offset 커밋

```java
@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listen(
        ConsumerRecord<String, String> record,
        Acknowledgment ack  // 수동 커밋용
) {
    try {
        process(record.value());
        ack.acknowledge();  // 처리 성공 시 커밋
    } catch (Exception e) {
        // 커밋하지 않음 → 재처리
        throw e;
    }
}
```

**설정:**

```java
factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
```

### 5.6 AckMode 종류

| AckMode | 설명 |
|---------|------|
| **RECORD** | 각 레코드 처리 후 자동 커밋 |
| **BATCH** | poll() 반환된 모든 레코드 처리 후 커밋 (기본) |
| **TIME** | 일정 시간마다 커밋 |
| **COUNT** | 일정 개수마다 커밋 |
| **MANUAL** | ack.acknowledge() 호출 시 배치 커밋 |
| **MANUAL_IMMEDIATE** | ack.acknowledge() 호출 시 즉시 커밋 |

### 5.7 동시성 설정

```java
@KafkaListener(
    topics = "my-topic",
    groupId = "my-group",
    concurrency = "3"  // 3개 Consumer 스레드 생성
)
public void listen(String message) {
    // 3개 파티션을 3개 스레드가 병렬 처리
}
```

### 5.8 에러 핸들링

```java
@KafkaListener(
    topics = "my-topic",
    groupId = "my-group",
    containerFactory = "kafkaListenerContainerFactory"
)
public void listen(String message) {
    if (invalid(message)) {
        throw new InvalidMessageException("Invalid: " + message);
    }
    process(message);
}

// 에러 핸들러는 ContainerFactory에서 설정
```

### 5.9 조건부 처리 (SpEL)

```java
@KafkaListener(
    topics = "#{@topicConfig.orderTopic}",  // Bean에서 토픽명 가져오기
    groupId = "#{@groupConfig.groupId}",
    autoStartup = "${kafka.consumer.enabled:true}"  // 프로퍼티로 활성화 제어
)
public void listen(String message) {
    // ...
}
```

### 5.10 특정 파티션 지정

```java
@KafkaListener(
    groupId = "my-group",
    topicPartitions = @TopicPartition(
        topic = "my-topic",
        partitions = {"0", "1"},  // 파티션 0, 1만 구독
        partitionOffsets = @PartitionOffset(partition = "2", initialOffset = "100")
    )
)
public void listen(String message) {
    // 파티션 0, 1은 처음부터, 파티션 2는 offset 100부터
}
```

---

## 6. Portal Universe Consumer 설정 분석

### 6.1 KafkaConsumerConfig.java 분석

Portal Universe의 notification-service에서 사용하는 Consumer 설정입니다.

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    // 재시도 설정
    private static final long RETRY_INTERVAL_MS = 1000L;  // 1초
    private static final long MAX_RETRY_ATTEMPTS = 3L;    // 3회
```

**핵심 설계 포인트:**

1. **ErrorHandlingDeserializer 사용**
   - 잘못된 형식의 메시지로 인한 Consumer 중단 방지

2. **수동 커밋 모드**
   - `enable.auto.commit=false`
   - 메시지 손실 방지

3. **Dead Letter Queue (DLQ)**
   - 실패한 메시지를 `원본토픽.DLT`로 이동
   - 나중에 분석 및 재처리 가능

### 6.2 Consumer Factory 설정

```java
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();

    // 브로커 연결
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

    // 에러 처리용 역직렬화기 래핑
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

    // JSON 역직렬화 설정
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

    // Offset 관리
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(props);
}
```

### 6.3 Error Handler 설정

```java
@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    // DLQ 발행 설정
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                String dlqTopic = record.topic() + ".DLT";
                log.error("Message sent to DLQ: topic={}, key={}", dlqTopic, record.key());
                return new TopicPartition(dlqTopic, record.partition());
            }
    );

    // 재시도 전략: 1초 간격, 최대 3회
    FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 재시도하지 않을 예외 (비즈니스 로직 오류)
    errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            NullPointerException.class
    );

    return errorHandler;
}
```

**Error Handling 흐름:**

```
메시지 수신 → 처리 시도
              │
              ├─ 성공 → 커밋
              │
              └─ 실패 → 재시도 (1초 후)
                        │
                        ├─ 성공 → 커밋
                        │
                        └─ 3회 실패 → DLQ로 이동
                                      (topic.DLT)
```

### 6.4 Container Factory 설정

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        CommonErrorHandler errorHandler
) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);

    // AckMode.RECORD: 각 레코드 처리 후 커밋
    factory.getContainerProperties().setAckMode(AckMode.RECORD);

    return factory;
}
```

### 6.5 NotificationConsumer.java 분석

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "user-signup", groupId = "notification-group")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: {}", event);
        // 환영 이메일 발송 로직
    }

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created event: userId={}", event.getUserId());
        createAndPush(event);
    }

    // 추가 리스너들...

    /**
     * 예외를 삼키지 않고 ErrorHandler에 위임
     * ErrorHandler가 재시도 및 DLQ 처리
     */
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

**설계 원칙:**
- 예외를 try-catch로 삼키지 않음
- ErrorHandler가 재시도/DLQ 처리를 담당
- 각 이벤트 타입별 전용 리스너

### 6.6 application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

    consumer:
      group-id: notification-group
      auto-offset-reset: earliest    # 새 그룹은 처음부터 읽기
      enable-auto-commit: false      # 수동 커밋

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                      # 모든 복제본 확인
      retries: 3                     # 재시도 3회
```

---

## 7. 코드 예제

### 7.1 기본 Consumer 구현

```java
public class BasicConsumerExample {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singleton("my-topic"));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Partition=%d, Offset=%d, Key=%s, Value=%s%n",
                            record.partition(), record.offset(), record.key(), record.value());
                }

                consumer.commitSync();
            }
        }
    }
}
```

### 7.2 Graceful Shutdown

```java
@Component
public class GracefulConsumer implements DisposableBean {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void start() {
        consumer = createConsumer();
        new Thread(this::pollLoop).start();
    }

    private void pollLoop() {
        try {
            consumer.subscribe(Collections.singleton("my-topic"));

            while (running.get()) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }

                consumer.commitAsync();
            }
        } finally {
            consumer.commitSync();  // 종료 전 최종 커밋
            consumer.close();
        }
    }

    @Override
    public void destroy() {
        running.set(false);
        consumer.wakeup();  // poll() 블로킹 해제
    }
}
```

### 7.3 Seek를 활용한 재처리

```java
@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listen(
        ConsumerRecord<String, String> record,
        Consumer<?, ?> consumer  // Consumer 직접 접근
) {
    try {
        processRecord(record);
    } catch (RetryableException e) {
        // 특정 오프셋부터 다시 읽기
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        consumer.seek(partition, record.offset());
        throw e;  // ErrorHandler에서 재시도
    }
}
```

### 7.4 특정 시간 기준 재처리

```java
public void seekToTimestamp(KafkaConsumer<String, String> consumer, long timestamp) {
    // 각 파티션의 특정 시간대 오프셋 찾기
    Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
    for (TopicPartition partition : consumer.assignment()) {
        timestampsToSearch.put(partition, timestamp);
    }

    // 타임스탬프에 해당하는 오프셋 조회
    Map<TopicPartition, OffsetAndTimestamp> offsets =
            consumer.offsetsForTimes(timestampsToSearch);

    // 각 파티션을 해당 오프셋으로 이동
    for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : offsets.entrySet()) {
        if (entry.getValue() != null) {
            consumer.seek(entry.getKey(), entry.getValue().offset());
        }
    }
}
```

### 7.5 멱등성 처리 (Idempotent Consumer)

```java
@Service
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final ProcessedEventRepository repository;

    @KafkaListener(topics = "orders", groupId = "order-processor")
    @Transactional
    public void process(OrderEvent event) {
        String eventId = event.getEventId();

        // 이미 처리한 이벤트인지 확인
        if (repository.existsByEventId(eventId)) {
            log.info("Event already processed: {}", eventId);
            return;
        }

        // 비즈니스 로직 실행
        processOrder(event);

        // 처리 기록 저장 (동일 트랜잭션)
        repository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
    }
}
```

### 7.6 Dead Letter Queue 처리 Consumer

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final AlertService alertService;
    private final DlqRepository dlqRepository;

    @KafkaListener(
        topics = "shopping.order.created.DLT",  // DLQ 토픽
        groupId = "dlq-processor"
    )
    public void processDlq(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.DLT_EXCEPTION_STACKTRACE) String stacktrace
    ) {
        log.error("DLQ message received: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        log.error("Exception: {}", exceptionMessage);

        // 1. DB에 저장 (나중에 수동 재처리용)
        DlqMessage dlqMessage = DlqMessage.builder()
                .originalTopic(extractOriginalTopic(record.topic()))
                .key(record.key())
                .value(record.value().toString())
                .errorMessage(exceptionMessage)
                .receivedAt(LocalDateTime.now())
                .build();
        dlqRepository.save(dlqMessage);

        // 2. 알림 발송
        alertService.sendDlqAlert(dlqMessage);
    }

    private String extractOriginalTopic(String dlqTopic) {
        return dlqTopic.replace(".DLT", "");
    }
}
```

---

## 8. Best Practices 정리

### 8.1 Offset 관리

| 권장 사항 | 설명 |
|----------|------|
| **수동 커밋 사용** | 메시지 손실 방지 |
| **처리 완료 후 커밋** | 처리 중 장애 시 재처리 보장 |
| **하이브리드 커밋** | 일반: async, 종료: sync |
| **멱등성 구현** | At-least-once 환경에서 중복 처리 방지 |

### 8.2 Consumer Group 설계

| 권장 사항 | 설명 |
|----------|------|
| **Consumer 수 = 파티션 수** | 최대 병렬 처리 |
| **CooperativeStickyAssignor** | Rebalancing 영향 최소화 |
| **Static Membership** | 롤링 배포 시 안정성 |

### 8.3 에러 처리

| 권장 사항 | 설명 |
|----------|------|
| **DLQ 구현** | 실패 메시지 보관 및 분석 |
| **재시도 제한** | 무한 재시도 방지 |
| **Not Retryable 예외 정의** | 비즈니스 오류는 즉시 DLQ |
| **예외 삼키지 않기** | ErrorHandler에 위임 |

### 8.4 성능 최적화

| 권장 사항 | 설명 |
|----------|------|
| **배치 처리** | 높은 처리량 필요 시 |
| **적절한 fetch.min.bytes** | 네트워크 효율성 |
| **concurrency 설정** | 파티션 수에 맞춰 조정 |
| **max.poll.records 조정** | 처리 시간에 맞춰 조정 |

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Poll Loop** | Consumer의 핵심 동작, 메시지 fetch + heartbeat + rebalance 처리 |
| **Manual Commit** | 메시지 손실 방지를 위한 필수 설정 |
| **Rebalancing** | Consumer Group 내 파티션 재할당, Cooperative 방식 권장 |
| **CooperativeStickyAssignor** | Stop-the-world 없는 점진적 rebalancing |
| **DLQ** | 실패 메시지 보관, 분석 및 재처리용 |
| **AckMode.RECORD** | 각 레코드 처리 후 커밋으로 손실 최소화 |

---

## 다음 학습

- [Kafka Producer 심화](./kafka-producers-deep-dive.md)
- [Kafka 트랜잭션](./kafka-transactions.md)
- [Kafka 모니터링](./kafka-monitoring.md)

---

## 참고 자료

- [Apache Kafka Consumer Documentation](https://kafka.apache.org/documentation/#consumerconfigs)
- [Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Confluent - Kafka Consumer](https://developer.confluent.io/courses/apache-kafka/consumers/)
- [KIP-429: Incremental Rebalance Protocol](https://cwiki.apache.org/confluence/display/KAFKA/KIP-429%3A+Kafka+Consumer+Incremental+Rebalance+Protocol)
