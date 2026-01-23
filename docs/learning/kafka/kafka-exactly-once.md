# Kafka Exactly-Once Semantics (EOS)

## 학습 목표
- 메시지 전달 보장 수준 3가지의 차이점 이해
- Idempotent Producer의 동작 원리 파악
- Kafka Transactions를 활용한 정확히 한 번 처리 구현
- Spring Kafka에서 트랜잭션 설정 방법 습득

---

## 1. 메시지 전달 보장 수준 (Delivery Semantics)

분산 시스템에서 메시지 전달은 **네트워크 장애**, **브로커 장애**, **Consumer 장애** 등으로 인해 복잡해집니다. Kafka는 세 가지 전달 보장 수준을 제공합니다.

### 1.1 At-Most-Once (최대 한 번)

메시지가 **손실될 수 있지만 중복은 없음**을 보장합니다.

```
┌──────────────────────────────────────────────────────────────┐
│  Producer                      Broker                        │
│     │                            │                           │
│     ├──── Message ──────────────►│                           │
│     │         (acks=0)           │                           │
│     │                            │  (저장 실패 가능)          │
│     │      No confirmation       │                           │
│     │                            │                           │
│  결과: 메시지 손실 가능, 중복 없음                            │
└──────────────────────────────────────────────────────────────┘
```

**특징:**
- Producer는 확인 응답을 기다리지 않음 (`acks=0`)
- 가장 빠른 처리 속도
- 메시지 손실 허용 가능한 경우에만 사용

**사용 사례:**
- 로그 수집 (일부 손실 허용)
- 실시간 메트릭 (최신 값만 중요)

### 1.2 At-Least-Once (최소 한 번)

메시지가 **최소 한 번 전달되지만 중복 가능**을 보장합니다.

```
┌──────────────────────────────────────────────────────────────┐
│  Producer                      Broker                        │
│     │                            │                           │
│     ├──── Message ──────────────►│                           │
│     │         (acks=all)         │  저장 완료                 │
│     │                            │                           │
│     │◄──── ACK ─────────────────┤  (네트워크 장애로 ACK 손실) │
│     │      (ACK lost!)           │                           │
│     │                            │                           │
│     ├──── Message (재전송) ─────►│  중복 저장!                │
│     │                            │                           │
│  결과: 메시지 중복 가능, 손실 없음                            │
└──────────────────────────────────────────────────────────────┘
```

**특징:**
- Producer는 확인 응답을 기다림 (`acks=1` 또는 `acks=all`)
- 응답 실패 시 재전송 → 중복 발생 가능
- Kafka의 기본 동작 방식

**사용 사례:**
- 대부분의 비즈니스 로직 (중복 처리 로직 포함)
- Consumer 측에서 멱등성 보장 필요

### 1.3 Exactly-Once (정확히 한 번)

메시지가 **정확히 한 번만 전달됨**을 보장합니다.

```
┌──────────────────────────────────────────────────────────────┐
│  Producer                      Broker                        │
│  (PID=1, Seq=0)                  │                           │
│     │                            │                           │
│     ├──── Message(PID=1,Seq=0)──►│  저장 (Seq=0 기록)         │
│     │                            │                           │
│     │◄──── ACK ─────────────────┤  (ACK 손실)                │
│     │                            │                           │
│     ├──── Message(PID=1,Seq=0)──►│  중복 감지! 무시           │
│     │      (같은 PID, Seq)        │                           │
│     │                            │                           │
│  결과: 메시지 정확히 한 번만 저장                              │
└──────────────────────────────────────────────────────────────┘
```

**특징:**
- Idempotent Producer + Transactions 조합
- 약간의 성능 오버헤드
- 복잡한 설정 필요

**사용 사례:**
- 금융 거래 처리
- 재고 관리
- 결제 시스템

### 전달 보장 수준 비교

| 수준 | 손실 | 중복 | 성능 | 복잡도 |
|------|------|------|------|--------|
| At-Most-Once | O | X | 최고 | 낮음 |
| At-Least-Once | X | O | 높음 | 중간 |
| Exactly-Once | X | X | 중간 | 높음 |

---

## 2. Idempotent Producer (멱등성 프로듀서)

### 2.1 개념

Idempotent Producer는 **동일한 메시지를 여러 번 전송해도 한 번만 저장**되도록 보장합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Idempotent Producer 동작 원리                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Producer                              Broker                    │
│  ┌────────────────┐                    ┌────────────────┐        │
│  │ PID: 1000      │                    │ Partition 0    │        │
│  │ Epoch: 0       │                    │                │        │
│  │                │                    │ PID=1000:      │        │
│  │ Seq Counter:   │                    │ ├─ Seq 0 ✓     │        │
│  │   0 → 1 → 2    │                    │ ├─ Seq 1 ✓     │        │
│  └────────────────┘                    │ └─ Seq 2 ✓     │        │
│                                        └────────────────┘        │
│                                                                  │
│  핵심 컴포넌트:                                                   │
│  - PID (Producer ID): 프로듀서 고유 식별자                        │
│  - Epoch: 프로듀서 재시작 감지용                                  │
│  - Sequence Number: 메시지별 순차 번호                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 동작 메커니즘

**1) Producer 초기화 시:**
- Broker에서 고유한 **PID (Producer ID)** 발급
- 초기 **Sequence Number = 0**

**2) 메시지 전송 시:**
- 각 메시지에 `(PID, Partition, Sequence Number)` 포함
- Broker는 Partition별로 마지막 Sequence Number 추적

**3) 중복 감지:**
- 동일한 `(PID, Sequence)` 조합이 오면 → 중복으로 판단, 무시
- Sequence가 비연속적이면 → `OutOfOrderSequenceException` 발생

### 2.3 설정 방법

```java
// Producer 설정
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

// 멱등성 활성화
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

// 멱등성 활성화 시 자동 설정되는 값들
// props.put(ProducerConfig.ACKS_CONFIG, "all");           // 자동 설정
// props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 자동 설정
// props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // 기본값

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

**Spring Kafka 설정:**

```yaml
# application.yml
spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      properties:
        enable.idempotence: true
```

### 2.4 제한사항

| 제한 | 설명 |
|------|------|
| **단일 파티션** | 동일 파티션 내에서만 멱등성 보장 |
| **단일 세션** | Producer 재시작 시 새 PID 발급 (이전 세션과 연속성 없음) |
| **단일 토픽** | 여러 토픽에 걸친 원자성 없음 |

**→ 이러한 제한을 극복하기 위해 Kafka Transactions 필요**

---

## 3. Kafka Transactions

### 3.1 개념

Kafka Transactions는 **여러 파티션/토픽에 걸친 원자적 쓰기**를 보장합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kafka Transaction 범위                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐        │
│  │  Topic A    │     │  Topic B    │     │  Topic C    │        │
│  │  Partition 0│     │  Partition 0│     │  Partition 0│        │
│  │  ┌───────┐  │     │  ┌───────┐  │     │  ┌───────┐  │        │
│  │  │ msg1  │  │     │  │ msg2  │  │     │  │ msg3  │  │        │
│  │  └───────┘  │     │  └───────┘  │     │  └───────┘  │        │
│  └─────────────┘     └─────────────┘     └─────────────┘        │
│         │                  │                   │                 │
│         └──────────────────┼───────────────────┘                 │
│                            │                                     │
│                   ┌────────▼────────┐                            │
│                   │   Transaction   │                            │
│                   │   (atomic)      │                            │
│                   │                 │                            │
│                   │ All succeed OR  │                            │
│                   │ All fail        │                            │
│                   └─────────────────┘                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 트랜잭션 상태 관리

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transaction State Machine                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────┐   initTransactions()   ┌──────────────────┐        │
│  │  Empty  │ ───────────────────────►  Initializing    │        │
│  └─────────┘                         └────────┬─────────┘        │
│                                               │                  │
│                                               ▼                  │
│                                      ┌──────────────────┐        │
│                                      │      Ready       │◄──┐    │
│                                      └────────┬─────────┘   │    │
│                              beginTransaction()│            │    │
│                                               ▼            │    │
│                                      ┌──────────────────┐   │    │
│                                      │   In Progress    │   │    │
│                                      └────────┬─────────┘   │    │
│                                               │             │    │
│                    ┌──────────────────────────┼─────────────┤    │
│                    │                          │             │    │
│           abortTransaction()         commitTransaction()    │    │
│                    │                          │             │    │
│                    ▼                          ▼             │    │
│           ┌──────────────┐          ┌──────────────┐        │    │
│           │   Aborting   │          │  Committing  │────────┘    │
│           └──────────────┘          └──────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Transaction Coordinator

```
┌─────────────────────────────────────────────────────────────────┐
│               Transaction Coordinator Architecture               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Producer                   Kafka Cluster                        │
│  ┌────────────────┐         ┌────────────────────────────────┐  │
│  │ transactional  │         │                                │  │
│  │ .id="order-tx" │         │   Transaction Coordinator      │  │
│  └───────┬────────┘         │   (Broker에서 실행)             │  │
│          │                  │   ┌──────────────────────────┐ │  │
│          │ 1. Find          │   │ __transaction_state      │ │  │
│          │    Coordinator   │   │ (internal topic)         │ │  │
│          │──────────────────►   │                          │ │  │
│          │                  │   │ TxnID: "order-tx"        │ │  │
│          │ 2. InitProducer  │   │ PID: 1000                │ │  │
│          │──────────────────►   │ Epoch: 0                 │ │  │
│          │                  │   │ State: Ready             │ │  │
│          │ 3. Begin/Commit  │   │ Partitions: [TopicA-0]   │ │  │
│          │──────────────────►   └──────────────────────────┘ │  │
│          │                  │                                │  │
│          │                  └────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.4 기본 사용법

```java
// Producer 설정
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

// 트랜잭션 설정 (필수)
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-transaction-1");

// 멱등성은 트랜잭션 설정 시 자동 활성화
// props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 자동 설정

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// 트랜잭션 초기화 (한 번만 호출)
producer.initTransactions();

try {
    // 트랜잭션 시작
    producer.beginTransaction();

    // 여러 토픽/파티션에 메시지 전송
    producer.send(new ProducerRecord<>("orders", "order-1", "{...}"));
    producer.send(new ProducerRecord<>("inventory", "product-1", "{...}"));
    producer.send(new ProducerRecord<>("notifications", "user-1", "{...}"));

    // 트랜잭션 커밋 - 모든 메시지가 원자적으로 커밋
    producer.commitTransaction();

} catch (ProducerFencedException | OutOfOrderSequenceException e) {
    // 복구 불가능한 에러 - 프로듀서 재생성 필요
    producer.close();
    throw e;
} catch (KafkaException e) {
    // 복구 가능한 에러 - 트랜잭션 롤백
    producer.abortTransaction();
}
```

### 3.5 Transactional ID의 중요성

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transactional ID와 Fencing                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  시나리오: 같은 transactional.id를 가진 Producer 두 개           │
│                                                                  │
│  Producer A (PID=1000, Epoch=0)                                  │
│  transactional.id = "order-tx"                                   │
│       │                                                          │
│       │ 1. 트랜잭션 진행 중                                       │
│       │                                                          │
│       │                     Producer B (시작)                    │
│       │                     transactional.id = "order-tx"        │
│       │                          │                               │
│       │                          │ 2. InitTransactions 호출      │
│       │                          │                               │
│       │                          ▼                               │
│       │                     Broker assigns:                      │
│       │                     PID=1000, Epoch=1 (증가!)             │
│       │                          │                               │
│       ▼                          │                               │
│  Producer A 작업 시               │                               │
│  ProducerFencedException!        │                               │
│  (Epoch 불일치로 거부됨)          │                               │
│                                  │                               │
│                                  ▼                               │
│                          Producer B 정상 동작                    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Fencing의 목적:**
- Zombie Producer 방지 (네트워크 파티션 후 복구된 이전 프로듀서)
- 동일 `transactional.id`에 대해 단일 활성 프로듀서 보장

---

## 4. Consume-Transform-Produce 패턴

### 4.1 패턴 개요

가장 일반적인 Exactly-Once 사용 사례는 **메시지를 소비하고, 변환하고, 다시 발행**하는 패턴입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│              Consume-Transform-Produce Pattern                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Input Topic              Processing              Output Topic   │
│  ┌──────────┐            ┌──────────┐            ┌──────────┐   │
│  │ orders   │            │          │            │ enriched │   │
│  │          │─────────►  │ Consumer │─────────►  │  orders  │   │
│  │          │   consume  │    +     │  produce   │          │   │
│  └──────────┘            │ Producer │            └──────────┘   │
│                          └────┬─────┘                           │
│                               │                                  │
│                               │ offset commit                    │
│                               ▼                                  │
│                          ┌──────────┐                            │
│                          │ __consumer │                          │
│                          │ _offsets  │                            │
│                          └──────────┘                            │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Transaction Scope:                                        │ │
│  │  1. Produce to output topic                                │ │
│  │  2. Commit consumer offsets                                │ │
│  │  → 둘 다 성공하거나 둘 다 실패                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 구현 예시

```java
public class ExactlyOnceProcessor {

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;

    public ExactlyOnceProcessor() {
        // Consumer 설정
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processor-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 중요: 자동 커밋 비활성화 (트랜잭션에서 관리)
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // read_committed: 커밋된 트랜잭션 메시지만 읽기
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        consumer = new KafkaConsumer<>(consumerProps);

        // Producer 설정
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-processor-tx");

        producer = new KafkaProducer<>(producerProps);
        producer.initTransactions();
    }

    public void process() {
        consumer.subscribe(Collections.singletonList("orders"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            if (!records.isEmpty()) {
                try {
                    // 트랜잭션 시작
                    producer.beginTransaction();

                    for (ConsumerRecord<String, String> record : records) {
                        // 1. 메시지 변환
                        String enrichedOrder = transformOrder(record.value());

                        // 2. 출력 토픽에 발행
                        producer.send(new ProducerRecord<>(
                            "enriched-orders",
                            record.key(),
                            enrichedOrder
                        ));
                    }

                    // 3. Consumer Offset을 트랜잭션에 포함
                    Map<TopicPartition, OffsetAndMetadata> offsets =
                        getOffsetsToCommit(records);
                    producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());

                    // 4. 트랜잭션 커밋
                    producer.commitTransaction();

                } catch (ProducerFencedException | OutOfOrderSequenceException e) {
                    throw e; // 복구 불가 - 재시작 필요
                } catch (KafkaException e) {
                    producer.abortTransaction();
                    // 메시지는 재처리됨 (offset이 커밋되지 않았으므로)
                }
            }
        }
    }

    private Map<TopicPartition, OffsetAndMetadata> getOffsetsToCommit(
            ConsumerRecords<String, String> records) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

        for (TopicPartition partition : records.partitions()) {
            List<ConsumerRecord<String, String>> partitionRecords =
                records.records(partition);
            long lastOffset = partitionRecords
                .get(partitionRecords.size() - 1)
                .offset();
            // 다음에 읽을 offset을 저장 (현재 offset + 1)
            offsets.put(partition, new OffsetAndMetadata(lastOffset + 1));
        }

        return offsets;
    }

    private String transformOrder(String order) {
        // 비즈니스 로직 (가격 계산, 재고 확인 등)
        return order;
    }
}
```

### 4.3 Consumer Isolation Level

```
┌─────────────────────────────────────────────────────────────────┐
│                    Isolation Level 비교                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  read_uncommitted (기본값)                                       │
│  ─────────────────────────                                       │
│  ┌─────────────────────────────────────────┐                    │
│  │ Log: [A] [B] [C] [D-uncommitted] [E]    │                    │
│  │      ─────────────────────────────►     │  Consumer가        │
│  │      모든 메시지 읽기                    │  볼 수 있는 범위    │
│  └─────────────────────────────────────────┘                    │
│                                                                  │
│  read_committed                                                  │
│  ─────────────                                                   │
│  ┌─────────────────────────────────────────┐                    │
│  │ Log: [A] [B] [C] [D-uncommitted] [E]    │                    │
│  │      ───────────►     ▲                 │  Consumer가        │
│  │      커밋된 메시지만  │                 │  볼 수 있는 범위    │
│  │                       │                 │                    │
│  │              Last Stable Offset (LSO)   │                    │
│  └─────────────────────────────────────────┘                    │
│                                                                  │
│  LSO: 아직 커밋되지 않은 트랜잭션의 첫 번째 offset                │
│       read_committed Consumer는 LSO 이전까지만 읽음              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Spring Kafka의 트랜잭션 설정

### 5.1 기본 설정

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      # 트랜잭션 ID 프리픽스 설정
      transaction-id-prefix: order-tx-
      # 멱등성 자동 활성화됨

    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: order-processor
      auto-offset-reset: earliest
      enable-auto-commit: false  # 중요!
      isolation-level: read_committed  # 트랜잭션 읽기 레벨
      properties:
        spring.json.trusted.packages: "*"
```

### 5.2 Configuration 클래스

```java
@Configuration
@EnableKafka
public class KafkaTransactionConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Producer Factory with Transaction
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(config);

        // 트랜잭션 ID 프리픽스 설정
        factory.setTransactionIdPrefix("shopping-tx-");

        return factory;
    }

    // Transactional KafkaTemplate
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Factory
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    // Kafka Transaction Manager
    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager() {
        return new KafkaTransactionManager<>(producerFactory());
    }

    // Listener Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        // 수동 ACK 모드
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
```

### 5.3 @Transactional 사용

```java
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    // Kafka + DB 트랜잭션 (ChainedTransactionManager 또는 별도 관리 필요)
    @Transactional  // JPA 트랜잭션
    public void processOrder(OrderEvent event) {
        // 1. DB 저장
        Order order = orderRepository.save(Order.from(event));

        // 2. Kafka 발행 (KafkaTemplate의 트랜잭션 사용)
        kafkaTemplate.executeInTransaction(ops -> {
            ops.send("order-created", order.getId().toString(), order);
            ops.send("inventory-update", order.getProductId(),
                new InventoryEvent(order.getProductId(), -order.getQuantity()));
            return null;
        });
    }

    // 순수 Kafka 트랜잭션만 사용
    public void publishOrderEvents(Order order) {
        kafkaTemplate.executeInTransaction(ops -> {
            // 여러 토픽에 원자적 발행
            ops.send("orders", order.getId().toString(), order);
            ops.send("notifications", order.getUserId(),
                new NotificationEvent("Order created: " + order.getId()));
            ops.send("analytics", "order",
                new AnalyticsEvent("ORDER_CREATED", order));
            return true;
        });
    }
}
```

### 5.4 Consume-Transform-Produce with Spring Kafka

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEnrichmentProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductService productService;

    @KafkaListener(
        topics = "orders",
        groupId = "order-enrichment-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional("kafkaTransactionManager")  // Kafka 트랜잭션 매니저 지정
    public void processOrder(
            @Payload OrderEvent order,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment ack) {

        try {
            // 1. 주문 정보 보강
            Product product = productService.getProduct(order.getProductId());
            EnrichedOrder enrichedOrder = EnrichedOrder.builder()
                .orderId(order.getOrderId())
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(order.getQuantity())
                .totalAmount(product.getPrice().multiply(
                    BigDecimal.valueOf(order.getQuantity())))
                .build();

            // 2. 보강된 주문 발행
            kafkaTemplate.send("enriched-orders", key, enrichedOrder);

            // 3. 재고 차감 이벤트 발행
            kafkaTemplate.send("inventory-deduct",
                order.getProductId().toString(),
                new InventoryDeductEvent(order.getProductId(), order.getQuantity()));

            // 4. 수동 ACK (@Transactional과 함께 사용 시 트랜잭션에 포함됨)
            ack.acknowledge();

            log.info("Order {} enriched and published", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order: {}", order.getOrderId(), e);
            // 트랜잭션 롤백됨 - 메시지 재처리
            throw e;
        }
    }
}
```

### 5.5 에러 처리와 재시도

```java
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTransactionManager<String, Object> transactionManager) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 트랜잭션 매니저 설정
        factory.getContainerProperties().setTransactionManager(transactionManager);

        // 에러 핸들러 설정
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate(),
                (record, ex) -> new TopicPartition(
                    record.topic() + ".DLT",
                    record.partition())),
            new FixedBackOff(1000L, 3L)  // 1초 간격, 3회 재시도
        ));

        return factory;
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate() {
        return null; // 실제로는 주입받음
    }
}
```

---

## 6. 트레이드오프와 주의사항

### 6.1 성능 영향

```
┌─────────────────────────────────────────────────────────────────┐
│                    EOS 성능 오버헤드                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  일반 Producer                                                   │
│  ─────────────                                                   │
│  [Send] → [ACK] → [Send] → [ACK] → ...                          │
│     └────────────────────────────────────────► Time             │
│                                                                  │
│  Transactional Producer                                          │
│  ─────────────────────                                           │
│  [InitTx] → [Begin] → [Send] → [Send] → [Commit] → ...          │
│     │          │                           │                     │
│     │          │    Coordinator 통신        │                     │
│     │          │◄──────────────────────────►│                     │
│     │                                                            │
│     └─────────────────────────────────────────────────► Time     │
│                                                                  │
│  추가 오버헤드:                                                   │
│  - Transaction Coordinator와의 추가 통신                         │
│  - 트랜잭션 로그 (__transaction_state) 쓰기                      │
│  - Commit markers 전파 대기                                      │
│                                                                  │
│  일반적으로 10-20% 처리량 감소 예상                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 주요 주의사항

| 항목 | 주의사항 |
|------|----------|
| **Transactional ID** | 인스턴스별 고유해야 함 (중복 시 Fencing 발생) |
| **Consumer Group** | read_committed 사용 시 처리 지연 발생 가능 |
| **트랜잭션 타임아웃** | 기본 60초, 긴 처리는 타임아웃 조정 필요 |
| **배치 크기** | 트랜잭션당 메시지 수 제한 고려 (메모리) |
| **외부 시스템** | DB 등 외부 시스템과의 원자성은 별도 처리 필요 |

### 6.3 Transactional ID 전략

```java
// 잘못된 예: 고정 ID (단일 인스턴스만 가능)
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-processor");

// 올바른 예: 인스턴스별 고유 ID
// 방법 1: 호스트명 + 파티션
String txId = String.format("order-processor-%s-%d",
    InetAddress.getLocalHost().getHostName(),
    assignedPartition);

// 방법 2: UUID (재시작 시 새 ID)
String txId = "order-processor-" + UUID.randomUUID();

// 방법 3: Kubernetes Pod 이름
String txId = "order-processor-" + System.getenv("HOSTNAME");

// Spring Kafka에서는 prefix 사용
factory.setTransactionIdPrefix("order-processor-");
// 결과: order-processor-0, order-processor-1, ...
```

### 6.4 EOS가 불필요한 경우

다음 경우에는 At-Least-Once + 멱등성 처리가 더 적합합니다:

1. **Consumer가 멱등성을 보장하는 경우**
   ```java
   // DB Upsert 사용
   @Transactional
   public void processOrder(OrderEvent event) {
       // INSERT ON DUPLICATE KEY UPDATE
       orderRepository.upsert(Order.from(event));
   }
   ```

2. **중복 허용 가능한 경우**
   - 로그 집계
   - 최신 상태만 중요한 경우 (마지막 값 덮어쓰기)

3. **성능이 최우선인 경우**
   - 실시간 메트릭 수집
   - 대용량 이벤트 스트리밍

### 6.5 모니터링 포인트

```java
// 트랜잭션 관련 메트릭
@Component
public class KafkaTransactionMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    public void recordTransactionMetrics(KafkaProducer<?, ?> producer) {
        // 트랜잭션 성공/실패 카운터
        Map<MetricName, ? extends Metric> metrics = producer.metrics();

        // 주요 모니터링 메트릭
        // - txn-init-time-ns-total: 트랜잭션 초기화 시간
        // - txn-begin-time-ns-total: 트랜잭션 시작 시간
        // - txn-send-offsets-time-ns-total: Offset 커밋 시간
        // - txn-commit-time-ns-total: 커밋 시간
        // - txn-abort-time-ns-total: 롤백 시간
    }
}
```

---

## 7. Portal Universe 적용 가이드

### 7.1 적용 대상 서비스

| 서비스 | EOS 필요성 | 이유 |
|--------|------------|------|
| shopping-service | **높음** | 주문/결제 데이터 정확성 필수 |
| notification-service | 낮음 | 알림 중복은 허용 가능 |
| blog-service | 낮음 | 게시물 이벤트 멱등 처리 가능 |

### 7.2 Shopping Service 예시

```java
// shopping-service/src/main/java/.../service/OrderKafkaService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 생성 시 여러 이벤트를 원자적으로 발행
     */
    public void publishOrderCreatedEvents(Order order) {
        kafkaTemplate.executeInTransaction(ops -> {
            // 1. 주문 생성 이벤트
            ops.send("shopping.order.created",
                order.getId().toString(),
                OrderCreatedEvent.from(order));

            // 2. 재고 차감 이벤트
            ops.send("shopping.inventory.deduct",
                order.getProductId().toString(),
                InventoryDeductEvent.builder()
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .orderId(order.getId())
                    .build());

            // 3. 알림 요청 이벤트
            ops.send("user.notification.request",
                order.getUserId().toString(),
                NotificationRequestEvent.builder()
                    .userId(order.getUserId())
                    .type("ORDER_CREATED")
                    .message("주문이 생성되었습니다: " + order.getId())
                    .build());

            log.info("Published order events atomically for order: {}",
                order.getId());

            return true;
        });
    }
}
```

---

## 요약

| 개념 | 설명 |
|------|------|
| **Idempotent Producer** | PID + Sequence Number로 단일 파티션 내 중복 방지 |
| **Kafka Transactions** | 여러 파티션/토픽에 걸친 원자적 쓰기 보장 |
| **Consume-Transform-Produce** | 메시지 소비 + 발행 + Offset 커밋을 하나의 트랜잭션으로 |
| **read_committed** | 커밋된 트랜잭션 메시지만 읽는 Consumer 설정 |
| **트레이드오프** | 10-20% 성능 오버헤드, 복잡도 증가 |

---

## 참고 자료

- [Kafka Documentation - Transactions](https://kafka.apache.org/documentation/#semantics)
- [KIP-98: Exactly Once Delivery and Transactional Messaging](https://cwiki.apache.org/confluence/display/KAFKA/KIP-98+-+Exactly+Once+Delivery+and+Transactional+Messaging)
- [Spring for Apache Kafka - Transactions](https://docs.spring.io/spring-kafka/docs/current/reference/html/#transactions)
- [Confluent - Exactly-Once Semantics](https://www.confluent.io/blog/exactly-once-semantics-are-possible-heres-how-apache-kafka-does-it/)
