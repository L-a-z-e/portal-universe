# Kafka Producers 심층 분석

## 학습 목표
- Producer의 내부 동작 원리 완전 이해
- acks, retries, idempotence 설정의 의미와 선택 기준 파악
- 메시지 순서 보장 메커니즘 이해
- Portal Universe의 KafkaTemplate 설정 분석 및 최적화

---

## 1. Producer 아키텍처 개요

### 1.1 Producer 내부 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Kafka Producer                              │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────────┐ │
│  │   App    │──►│Serializer│──►│Partitioner│──►│ Record Accumulator│ │
│  │  Thread  │   │          │   │          │   │    (Batching)     │ │
│  └──────────┘   └──────────┘   └──────────┘   └────────┬─────────┘ │
│                                                         │           │
│                                              ┌──────────▼─────────┐ │
│                                              │   Sender Thread    │ │
│                                              │ (Network I/O)      │ │
│                                              └──────────┬─────────┘ │
└──────────────────────────────────────────────────────────┼──────────┘
                                                           │
                                                           ▼
                                              ┌──────────────────────┐
                                              │    Kafka Cluster     │
                                              └──────────────────────┘
```

Producer는 **두 개의 스레드**로 동작합니다:
1. **Main Thread**: Serialization, Partitioning, Batching
2. **Sender Thread**: 실제 네트워크 I/O 및 전송

---

## 2. Producer 내부 동작 상세

### 2.1 Serialization (직렬화)

메시지의 Key와 Value를 **바이트 배열로 변환**하는 과정입니다.

```java
// Portal Universe 설정
configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
```

**직렬화 흐름:**
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ OrderCreatedEvent│───►│  JsonSerializer │───►│   byte[]        │
│ {                │    │                 │    │ [123,34,111,...]│
│   orderNumber,   │    │                 │    │                 │
│   items,         │    │                 │    │                 │
│   totalPrice     │    │                 │    │                 │
│ }                │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**주요 Serializer 종류:**

| Serializer | 용도 | 특징 |
|------------|------|------|
| `StringSerializer` | 문자열 Key/Value | 가장 기본적인 형태 |
| `JsonSerializer` | JSON 객체 | 유연하지만 스키마 검증 없음 |
| `AvroSerializer` | Avro 포맷 | Schema Registry 연동, 스키마 진화 지원 |
| `ProtobufSerializer` | Protobuf 포맷 | 고성능, 타입 안전 |

**커스텀 Serializer 예시:**
```java
public class OrderEventSerializer implements Serializer<OrderEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, OrderEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize", e);
        }
    }
}
```

### 2.2 Partitioning (파티셔닝)

메시지가 어떤 Partition으로 전송될지 결정합니다.

**파티셔닝 전략:**

```
┌─────────────────────────────────────────────────────────────────┐
│                      Partitioner Decision                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Key = null?                                                    │
│     │                                                           │
│     ├── Yes ──► Sticky Partitioner (배치 완료까지 같은 파티션)  │
│     │                                                           │
│     └── No ───► hash(key) % numPartitions                       │
│                                                                 │
│  예: key="ORDER-001", numPartitions=3                           │
│      hash("ORDER-001") % 3 = 1  → Partition 1로 전송            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Portal Universe의 Key 기반 파티셔닝:**
```java
// ShoppingEventPublisher.java
public void publishOrderCreated(OrderCreatedEvent event) {
    publishEvent(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);
    //                                            ↑
    //                          orderNumber를 Key로 사용
    //                          같은 주문의 모든 이벤트가 동일 파티션으로
}

private void publishEvent(String topic, String key, Object event) {
    kafkaTemplate.send(topic, key, event);  // Key 지정
}
```

**Key 선택의 중요성:**

| 시나리오 | 권장 Key | 이유 |
|---------|----------|------|
| 주문 이벤트 | orderNumber | 같은 주문의 이벤트 순서 보장 |
| 사용자 이벤트 | userId | 사용자별 이벤트 순서 보장 |
| 결제 이벤트 | paymentNumber | 결제 상태 변경 순서 보장 |
| 로그 데이터 | null (균등 분배) | 순서 불필요, 처리량 우선 |

**커스텀 Partitioner:**
```java
public class RegionBasedPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (key instanceof String orderKey) {
            // 지역 코드 기반 파티셔닝 (예: KR-ORDER-001)
            if (orderKey.startsWith("KR-")) return 0;
            if (orderKey.startsWith("US-")) return 1;
            if (orderKey.startsWith("EU-")) return 2;
        }

        // 기본: 해시 기반
        return Math.abs(key.hashCode()) % numPartitions;
    }
}
```

### 2.3 Batching (배칭)

**성능 최적화**를 위해 여러 메시지를 모아서 전송합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Record Accumulator                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Topic: shopping.order.created                                  │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐   │
│  │   Partition 0   │ │   Partition 1   │ │   Partition 2   │   │
│  │ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │   │
│  │ │ Batch (16KB)│ │ │ │ Batch (16KB)│ │ │ │ Batch (16KB)│ │   │
│  │ │ [msg1,msg2, │ │ │ │ [msg3,msg4] │ │ │ │ [msg5]      │ │   │
│  │ │  msg6,msg7] │ │ │ │             │ │ │ │             │ │   │
│  │ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │   │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**배칭 관련 설정:**

```java
// Batch 크기 (바이트 단위)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16KB (기본값)

// 배치 대기 시간 (밀리초)
props.put(ProducerConfig.LINGER_MS_CONFIG, 5);  // 5ms 대기 후 전송

// 전체 버퍼 메모리
props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);  // 32MB
```

**배칭 동작 원리:**
```
시간 ─────────────────────────────────────────────────►

     │ msg1 │ msg2 │     │ msg3 │        │ 배치 전송!
     └──────┴──────┴─────┴──────┴────────┴──────►
     t=0    t=1    t=2   t=3    t=4      t=5 (linger.ms)

     ▲                                   ▲
     │                                   │
  배치 시작                          linger.ms 도달 or
                                    batch.size 도달
```

**배칭 튜닝 가이드:**

| 설정 | 낮은 값 | 높은 값 |
|------|--------|--------|
| `batch.size` | 낮은 지연, 낮은 처리량 | 높은 처리량, 메모리 사용 증가 |
| `linger.ms` | 즉시 전송, 네트워크 오버헤드 | 배치 효율 증가, 지연 증가 |

---

## 3. acks 설정 심층 분석

`acks`는 메시지 **전송 확인 수준**을 결정합니다.

### 3.1 acks=0 (Fire and Forget)

```
Producer                    Broker
    │                          │
    │ ──── Send Message ────►  │
    │                          │ 저장 시도
    │      (응답 대기 안 함)    │
    │                          │
    ▼                          ▼
  다음 작업                  저장 완료/실패
```

**특징:**
- 브로커 응답을 기다리지 않음
- **가장 빠름** (최저 지연)
- 메시지 손실 가능성 있음
- 재시도 불가능

**적합한 사용 사례:**
- 모니터링 메트릭
- 로그 수집
- 손실 허용 가능한 데이터

### 3.2 acks=1 (Leader Only)

```
Producer                    Leader Broker              Follower
    │                            │                        │
    │ ──── Send Message ────►    │                        │
    │                            │ 로컬 저장               │
    │ ◄──── ACK ─────────────    │                        │
    │                            │ ────── 복제 ──────►    │
    ▼                            ▼                        ▼
```

**특징:**
- Leader의 로컬 저장만 확인
- 중간 수준의 지연
- Leader 장애 시 **데이터 손실 가능**

**위험 시나리오:**
```
1. Producer가 Leader에 전송
2. Leader가 ACK 반환
3. Leader가 Follower에 복제 전 장애 발생
4. 새 Leader 선출 (복제 안 된 메시지 손실)
```

### 3.3 acks=all (-1) (Full ISR Sync)

```
Producer                    Leader Broker              Followers (ISR)
    │                            │                        │
    │ ──── Send Message ────►    │                        │
    │                            │ 로컬 저장               │
    │                            │ ────── 복제 ──────►    │
    │                            │ ◄─── 복제 완료 ────    │
    │ ◄──── ACK ─────────────    │                        │
    │                            │                        │
    ▼                            ▼                        ▼
```

**특징:**
- **모든 ISR**(In-Sync Replicas)에 복제 확인
- 가장 높은 내구성
- 가장 높은 지연

**Portal Universe 설정:**
```java
// KafkaConfig.java - 데이터 손실 방지를 위해 all 사용
configProps.put(ProducerConfig.ACKS_CONFIG, "all");
```

**min.insync.replicas와 연동:**
```yaml
# Topic 설정
min.insync.replicas: 2  # 최소 2개 복제본 동기화 필요

# acks=all + min.insync.replicas=2 조합:
# - Leader + 최소 1개 Follower 동기화 확인
# - ISR이 2 미만이면 Producer 예외 발생
```

### 3.4 acks 설정 비교

| acks | 내구성 | 지연 | 처리량 | 사용 사례 |
|------|--------|------|--------|----------|
| `0` | 낮음 | 최저 | 최고 | 로그, 메트릭 |
| `1` | 중간 | 중간 | 높음 | 일반 이벤트 |
| `all` | **최고** | 높음 | 낮음 | **주문, 결제** (Portal Universe) |

---

## 4. Retries와 Idempotence

### 4.1 Retries (재시도)

네트워크 오류나 일시적 장애 시 **자동으로 재전송**합니다.

```
Producer                         Broker
    │                               │
    │ ──── Send Message ────►       │
    │ ◄──── Network Error ────      │
    │                               │
    │ ──── Retry 1 ────────►        │
    │ ◄──── Timeout ───────         │
    │                               │
    │ ──── Retry 2 ────────►        │
    │ ◄──── ACK ───────────         │ 성공!
    │                               │
    ▼                               ▼
```

**재시도 관련 설정:**
```java
// 최대 재시도 횟수
props.put(ProducerConfig.RETRIES_CONFIG, 3);  // Portal Universe 설정

// 재시도 간격 (백오프)
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);  // 100ms

// 전송 타임아웃
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);  // 2분
```

**재시도 가능한 오류:**

| 오류 유형 | 재시도 가능 | 설명 |
|----------|-------------|------|
| `NetworkException` | Yes | 네트워크 일시 장애 |
| `NotLeaderForPartitionException` | Yes | Leader 변경 중 |
| `TimeoutException` | Yes | 응답 타임아웃 |
| `SerializationException` | **No** | 직렬화 실패 |
| `RecordTooLargeException` | **No** | 메시지 크기 초과 |

### 4.2 재시도의 문제: 중복 메시지

```
┌───────────────────────────────────────────────────────────────┐
│                    중복 발생 시나리오                          │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Producer                    Broker                           │
│      │                          │                             │
│      │ ──── msg (seq=1) ────►   │                             │
│      │                          │ 저장 완료 ✓                 │
│      │ ◄── ACK 손실 (timeout)   │                             │
│      │                          │                             │
│      │ ──── msg (seq=1) ────►   │  재시도                     │
│      │                          │ 중복 저장! ✗                │
│      │ ◄──── ACK ───────────    │                             │
│                                                               │
│  결과: 동일 메시지가 2번 저장됨                                │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### 4.3 Idempotent Producer (멱등성 프로듀서)

**중복 메시지를 방지**하는 메커니즘입니다.

```java
// Portal Universe 설정
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

**동작 원리:**
```
┌───────────────────────────────────────────────────────────────┐
│                    Idempotent Producer                        │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Producer                         Broker                      │
│  PID: 12345                       │                           │
│  Sequence: 1                      │                           │
│      │                            │                           │
│      │ ── msg (PID=12345,seq=1) ► │                           │
│      │                            │ 저장 (PID=12345,seq=1) ✓  │
│      │ ◄── ACK 손실 ──            │                           │
│      │                            │                           │
│      │ ── msg (PID=12345,seq=1) ► │ 재시도                    │
│      │                            │ 중복 감지! → 무시         │
│      │ ◄──── ACK ─────────────    │ (저장 안 함, ACK만 반환)  │
│                                                               │
│  결과: 메시지는 정확히 1번만 저장됨                            │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**구성 요소:**

| 요소 | 설명 |
|------|------|
| **PID** (Producer ID) | Producer 고유 식별자, 브로커가 할당 |
| **Sequence Number** | 파티션별 메시지 순서 번호 |
| **Broker 중복 감지** | (PID, Partition, Sequence) 조합으로 식별 |

**Idempotence 활성화 시 자동 설정:**
```java
// enable.idempotence=true 설정 시 자동 적용:
acks = "all"
retries = Integer.MAX_VALUE
max.in.flight.requests.per.connection ≤ 5
```

---

## 5. Ordering Guarantee (순서 보장)

### 5.1 파티션 내 순서 보장

Kafka는 **파티션 내에서만** 순서를 보장합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     순서 보장 범위                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Topic: shopping.order.created                                  │
│                                                                 │
│  Partition 0: [ORDER-001 생성] → [ORDER-001 확정] → [ORDER-001 배송]
│               ↑ 순서 보장 ✓                                     │
│                                                                 │
│  Partition 1: [ORDER-002 생성] → [ORDER-002 취소]               │
│               ↑ 순서 보장 ✓                                     │
│                                                                 │
│  ⚠️ Partition 0과 Partition 1 간에는 순서 보장 없음!            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 max.in.flight.requests.per.connection

**동시에 전송 가능한 요청 수**를 제한합니다.

**값이 1보다 클 때 순서 문제:**
```
┌───────────────────────────────────────────────────────────────┐
│  max.in.flight.requests = 5 (순서 역전 가능)                   │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Producer                              Partition 0            │
│      │                                      │                 │
│      │ ── Batch 1 (msg1, msg2) ──────►      │                 │
│      │ ── Batch 2 (msg3, msg4) ──────►      │ 동시 전송       │
│      │                                      │                 │
│      │ ◄── Batch 1 실패 ─────────────       │                 │
│      │ ◄── Batch 2 성공 ─────────────       │                 │
│      │                                      │                 │
│      │ ── Batch 1 재시도 ────────────►      │                 │
│      │ ◄── Batch 1 성공 ─────────────       │                 │
│                                                               │
│  결과: [msg3, msg4, msg1, msg2] 순서 역전!                     │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### 5.3 순서 보장을 위한 설정 조합

**방법 1: max.in.flight = 1 (엄격한 순서, 낮은 처리량)**
```java
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
```

**방법 2: Idempotence + max.in.flight ≤ 5 (권장)**
```java
// Portal Universe 설정 - Idempotence 활성화로 순서 보장
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
// max.in.flight는 기본값 5 이하에서 순서 보장됨
```

**Idempotent Producer의 순서 보장 원리:**
```
┌───────────────────────────────────────────────────────────────┐
│  Idempotent Producer (max.in.flight ≤ 5)                      │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Producer                              Broker                 │
│  seq=1                                     │                  │
│      │                                     │                  │
│      │ ── msg1 (seq=1) ──────────────►     │                  │
│      │ ── msg2 (seq=2) ──────────────►     │                  │
│      │                                     │                  │
│      │ ◄── msg1 실패 ──────────────        │                  │
│      │ ◄── msg2 성공 ──────────────        │                  │
│      │                                     │ seq=2 도착했지만 │
│      │                                     │ seq=1 대기중     │
│      │ ── msg1 (seq=1) 재시도 ──────►      │                  │
│      │                                     │ seq=1 저장       │
│      │                                     │ seq=2 저장       │
│                                                               │
│  결과: [msg1, msg2] 올바른 순서로 저장                         │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## 6. Portal Universe KafkaTemplate 설정 분석

### 6.1 현재 설정 분석

```java
// services/shopping-service/src/main/java/.../config/KafkaConfig.java

@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // 브로커 연결
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // 직렬화 설정
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

    // ✅ 내구성 설정 (금융급)
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 ISR 확인
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);    // 3회 재시도
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 중복 방지

    return new DefaultKafkaProducerFactory<>(configProps);
}
```

**설정 평가:**

| 설정 | 값 | 평가 |
|------|------|------|
| `acks` | `all` | **적합** - 주문/결제 데이터 손실 방지 |
| `retries` | `3` | ⚠️ idempotence 활성화 시 자동으로 MAX_VALUE |
| `enable.idempotence` | `true` | **적합** - 중복 방지 및 순서 보장 |
| `batch.size` | (기본값) | 검토 필요 - 처리량에 따라 튜닝 |
| `linger.ms` | (기본값) | 검토 필요 - 배치 효율성 |

### 6.2 ShoppingEventPublisher 패턴 분석

```java
// services/shopping-service/src/main/java/.../event/ShoppingEventPublisher.java

private void publishEvent(String topic, String key, Object event) {
    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(topic, key, event);

    future.whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Event published successfully: topic={}, key={}, offset={}",
                    topic, key, result.getRecordMetadata().offset());
        } else {
            log.error("Failed to publish event: topic={}, key={}, error={}",
                    topic, key, ex.getMessage());
        }
    });
}
```

**패턴 평가:**

| 항목 | 현재 상태 | 권장 사항 |
|------|----------|----------|
| 비동기 전송 | `CompletableFuture` 사용 | ✅ 적절 |
| 로깅 | 성공/실패 로깅 | ✅ 적절 |
| 에러 처리 | 로깅만 수행 | ⚠️ 재시도 로직 고려 |
| Key 사용 | 주문번호 등 사용 | ✅ 순서 보장 |

### 6.3 개선 권장 설정

```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // 브로커 연결
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // 직렬화
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

    // === 내구성 (현재와 동일) ===
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    // retries는 idempotence가 자동 설정하므로 제거 가능

    // === 배칭 최적화 (추가 권장) ===
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16KB
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);        // 5ms 대기

    // === 압축 (네트워크 효율) ===
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

    // === 타임아웃 설정 ===
    configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);  // 30초
    configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);   // 10초

    return new DefaultKafkaProducerFactory<>(configProps);
}
```

---

## 7. 코드 예제

### 7.1 기본 비동기 전송

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send("shopping.order.created",
                              event.orderNumber(),  // Key
                              event);               // Value

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("Order event sent: partition={}, offset={}",
                        metadata.partition(), metadata.offset());
            } else {
                log.error("Failed to send order event: {}", ex.getMessage());
                // 추가 에러 처리 (알림, 재시도 큐 등)
            }
        });
    }
}
```

### 7.2 동기 전송 (결과 대기)

```java
public void publishOrderCreatedSync(OrderCreatedEvent event)
        throws ExecutionException, InterruptedException, TimeoutException {

    SendResult<String, Object> result = kafkaTemplate
        .send("shopping.order.created", event.orderNumber(), event)
        .get(10, TimeUnit.SECONDS);  // 최대 10초 대기

    log.info("Order event sent synchronously: partition={}, offset={}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
}
```

### 7.3 ProducerRecord로 세밀한 제어

```java
public void publishWithHeaders(OrderCreatedEvent event) {
    ProducerRecord<String, Object> record = new ProducerRecord<>(
        "shopping.order.created",       // topic
        null,                           // partition (null = 자동)
        System.currentTimeMillis(),     // timestamp
        event.orderNumber(),            // key
        event                           // value
    );

    // 헤더 추가
    record.headers()
        .add("event-type", "ORDER_CREATED".getBytes())
        .add("correlation-id", UUID.randomUUID().toString().getBytes())
        .add("source-service", "shopping-service".getBytes());

    kafkaTemplate.send(record);
}
```

### 7.4 트랜잭션 Producer

```java
// 설정
@Bean
public ProducerFactory<String, Object> producerFactory() {
    DefaultKafkaProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(producerConfigs());
    factory.setTransactionIdPrefix("shopping-tx-");  // 트랜잭션 활성화
    return factory;
}

// 사용
@Transactional
public void processOrder(Order order) {
    // DB 작업
    orderRepository.save(order);

    // Kafka 전송 (트랜잭션 내에서)
    kafkaTemplate.executeInTransaction(ops -> {
        ops.send("shopping.order.created", order.getOrderNumber(),
                new OrderCreatedEvent(order));
        ops.send("shopping.inventory.reserved", order.getOrderNumber(),
                new InventoryReservedEvent(order));
        return true;
    });
}
```

### 7.5 Callback 기반 에러 핸들링

```java
@Component
@RequiredArgsConstructor
public class ResilientEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DeadLetterQueueService dlqService;

    public void publishWithRetry(String topic, String key, Object event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(record, ex);
            }
        });
    }

    private void handleFailure(ProducerRecord<String, Object> record, Throwable ex) {
        log.error("Failed to send message to {}: {}", record.topic(), ex.getMessage());

        if (isRetryableException(ex)) {
            // 재시도 큐에 추가
            dlqService.addToRetryQueue(record);
        } else {
            // DLQ에 저장
            dlqService.sendToDeadLetterQueue(record, ex);
        }
    }

    private boolean isRetryableException(Throwable ex) {
        return ex instanceof TimeoutException ||
               ex instanceof NetworkException;
    }
}
```

---

## 8. 모니터링 메트릭

### 8.1 주요 Producer 메트릭

```java
// Micrometer 연동 시 노출되는 메트릭
kafka.producer.record-send-total          // 전송 시도 총 수
kafka.producer.record-send-rate           // 초당 전송 수
kafka.producer.record-error-total         // 에러 발생 수
kafka.producer.record-retry-total         // 재시도 수
kafka.producer.request-latency-avg        // 평균 요청 지연
kafka.producer.batch-size-avg             // 평균 배치 크기
kafka.producer.buffer-available-bytes     // 사용 가능한 버퍼
kafka.producer.waiting-threads            // 대기 중인 스레드
```

### 8.2 Spring Boot Actuator 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, metrics
  metrics:
    tags:
      application: shopping-service
    export:
      prometheus:
        enabled: true
```

---

## 9. 핵심 정리

| 개념 | 설명 | Portal Universe 적용 |
|------|------|---------------------|
| **Serialization** | 객체를 바이트로 변환 | JsonSerializer 사용 |
| **Partitioning** | 메시지 분배 전략 | orderNumber를 Key로 사용 |
| **Batching** | 여러 메시지 묶어서 전송 | 기본값 사용 (튜닝 가능) |
| **acks=all** | 모든 ISR 복제 확인 | ✅ 적용됨 |
| **Idempotence** | 중복 메시지 방지 | ✅ 적용됨 |
| **순서 보장** | 파티션 내 순서 유지 | Key 기반으로 보장 |

---

## 다음 학습

- [Kafka Consumers 심층 분석](./kafka-consumers-deep-dive.md)
- [Kafka Transactions](./kafka-transactions.md)
- [Kafka Error Handling Patterns](./kafka-error-handling.md)

---

## 참고 자료

- [Apache Kafka Producer Configs](https://kafka.apache.org/documentation/#producerconfigs)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [Confluent - Kafka Producer](https://docs.confluent.io/platform/current/clients/producer.html)
- [Idempotent Producer - KIP-98](https://cwiki.apache.org/confluence/display/KAFKA/KIP-98+-+Exactly+Once+Delivery+and+Transactional+Messaging)
