# Kafka Troubleshooting Guide

Kafka 운영 중 발생하는 일반적인 문제들의 진단 및 해결 방법을 다룹니다.

## 목차

1. [자주 발생하는 문제와 해결책](#1-자주-발생하는-문제와-해결책)
2. [문제 진단 도구 및 명령어](#2-문제-진단-도구-및-명령어)
3. [로그 분석 가이드](#3-로그-분석-가이드)
4. [성능 튜닝 체크리스트](#4-성능-튜닝-체크리스트)
5. [디버깅 팁](#5-디버깅-팁)

---

## 1. 자주 발생하는 문제와 해결책

### 1.1 Consumer Lag 급증

**증상**
- Consumer가 Producer의 메시지 생산 속도를 따라가지 못함
- 모니터링 대시보드에서 Lag 수치가 지속적으로 증가
- 메시지 처리 지연 발생

**원인 분석**

```bash
# Consumer Lag 확인
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group my-consumer-group

# 출력 예시
GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
my-group        my-topic        0          1000            5000            4000
my-group        my-topic        1          2000            6000            4000
```

**주요 원인**

| 원인 | 설명 | 해결 방법 |
|------|------|----------|
| 처리 속도 부족 | Consumer 처리 로직이 느림 | 병렬 처리, 배치 처리 적용 |
| Consumer 수 부족 | Partition 수 대비 Consumer가 적음 | Consumer 인스턴스 증가 |
| 외부 시스템 지연 | DB, API 호출 등 외부 의존성 지연 | Connection Pool, 캐시 적용 |
| GC 문제 | 긴 GC Pause로 인한 처리 지연 | JVM 튜닝, 힙 사이즈 조정 |

**해결책**

```java
// 1. 배치 처리 적용
@KafkaListener(topics = "orders", containerFactory = "batchFactory")
public void consumeBatch(List<ConsumerRecord<String, Order>> records) {
    // 배치 단위로 DB 저장
    List<Order> orders = records.stream()
        .map(ConsumerRecord::value)
        .collect(Collectors.toList());
    orderRepository.saveAll(orders);
}

// 2. 배치 Factory 설정
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> batchFactory(
        ConsumerFactory<String, Order> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setBatchListener(true);
    factory.setConcurrency(3); // 병렬 처리
    return factory;
}
```

```yaml
# Consumer 설정 최적화
spring:
  kafka:
    consumer:
      max-poll-records: 500          # 한 번에 가져올 레코드 수
      fetch-min-size: 1048576        # 1MB - 최소 fetch 크기
      fetch-max-wait: 500ms          # 최대 대기 시간
```

---

### 1.2 Rebalancing 빈번 발생

**증상**
- Consumer가 자주 Partition 재할당을 받음
- 처리 중단 및 중복 처리 발생
- 로그에 "Rebalance" 관련 메시지 빈번

**원인과 해결책**

#### 원인 1: session.timeout 초과

```
[Consumer] Member [consumer-1] sending LeaveGroup request due to consumer poll timeout has expired
```

```yaml
# 해결: timeout 값 조정
spring:
  kafka:
    consumer:
      properties:
        session.timeout.ms: 45000      # 기본 10초 → 45초
        heartbeat.interval.ms: 15000   # session.timeout의 1/3
        max.poll.interval.ms: 300000   # 5분 (처리 시간 고려)
```

#### 원인 2: 처리 시간이 max.poll.interval.ms 초과

```java
// 문제: 한 번의 poll에서 너무 오래 처리
@KafkaListener(topics = "heavy-tasks")
public void consume(List<Task> tasks) {
    for (Task task : tasks) {
        heavyProcess(task); // 각 태스크가 10초 걸림
    }
}

// 해결: max-poll-records 줄이기 + 비동기 처리
@KafkaListener(topics = "heavy-tasks")
public void consume(Task task) {
    CompletableFuture.runAsync(() -> heavyProcess(task), executor);
    // 빠르게 poll 완료, 실제 처리는 별도 스레드
}
```

#### 원인 3: Consumer 인스턴스 빈번한 재시작

```java
// Static Membership 적용 (Kafka 2.3+)
spring:
  kafka:
    consumer:
      properties:
        group.instance.id: ${HOSTNAME}  # 고정 인스턴스 ID
```

**Rebalancing 모니터링**

```java
@Component
public class RebalanceListener implements ConsumerRebalanceListener {

    private static final Logger log = LoggerFactory.getLogger(RebalanceListener.class);

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.warn("Partitions revoked: {}", partitions);
        // 처리 중인 offset commit
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("Partitions assigned: {}", partitions);
    }
}
```

---

### 1.3 메시지 순서 문제

**증상**
- 같은 키를 가진 메시지들이 순서대로 처리되지 않음
- 이벤트 순서 의존성이 있는 로직에서 오류 발생

**원인과 해결책**

#### 원인 1: Partition 분산으로 인한 순서 미보장

```java
// 문제: 키 없이 전송하면 Round-Robin 분산
kafkaTemplate.send("orders", orderEvent);

// 해결: 동일 키는 동일 Partition으로
kafkaTemplate.send("orders", order.getUserId(), orderEvent);
```

#### 원인 2: 재시도로 인한 순서 뒤바뀜

```yaml
# 해결: 순서 보장 설정
spring:
  kafka:
    producer:
      properties:
        max.in.flight.requests.per.connection: 1  # 순서 보장
        enable.idempotence: true                   # 멱등성 활성화
        retries: 3
```

#### 원인 3: 병렬 Consumer 처리

```java
// 문제: 여러 스레드가 동시에 처리
factory.setConcurrency(5); // Partition당 다른 스레드

// 해결: 순서가 중요한 경우 단일 Partition 사용 또는
// 같은 키 메시지는 같은 스레드에서 처리되도록 설계
```

---

### 1.4 Timeout/Connection 에러

**증상별 분류**

#### 1.4.1 Producer Timeout

```
org.apache.kafka.common.errors.TimeoutException:
  Topic orders not present in metadata after 60000 ms
```

**해결책**

```yaml
spring:
  kafka:
    producer:
      properties:
        request.timeout.ms: 30000
        delivery.timeout.ms: 120000
        metadata.max.age.ms: 300000
```

```java
// 자동 토픽 생성 활성화 (개발 환경)
@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
            .partitions(3)
            .replicas(2)
            .build();
    }
}
```

#### 1.4.2 Consumer Connection 실패

```
org.apache.kafka.common.errors.DisconnectException:
  Connection to node -1 was disconnected
```

**해결책**

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: broker1:9092,broker2:9092,broker3:9092  # 여러 브로커 지정
      properties:
        connections.max.idle.ms: 540000
        reconnect.backoff.ms: 50
        reconnect.backoff.max.ms: 1000
```

#### 1.4.3 Network 관련 에러

```
java.net.UnknownHostException: kafka-broker
```

**체크리스트**
- [ ] DNS 해석 가능 여부 확인
- [ ] Firewall 규칙 확인 (포트 9092, 9093)
- [ ] advertised.listeners 설정 확인

```bash
# 연결 테스트
nc -zv kafka-broker 9092
telnet kafka-broker 9092

# DNS 확인
nslookup kafka-broker
```

---

## 2. 문제 진단 도구 및 명령어

### 2.1 Kafka CLI 도구

#### Topic 관련

```bash
# 토픽 목록 조회
kafka-topics.sh --bootstrap-server localhost:9092 --list

# 토픽 상세 정보
kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic my-topic

# ISR (In-Sync Replicas) 상태 확인
kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --under-replicated-partitions
```

#### Consumer Group 관련

```bash
# Consumer Group 목록
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# 특정 그룹 상세 정보 (Lag 포함)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group my-group

# 모든 그룹의 상태 조회
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --all-groups

# Consumer Group 상태 (Stable, Rebalancing 등)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group my-group --state
```

#### 메시지 확인

```bash
# 최신 메시지 확인
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic my-topic --from-beginning --max-messages 10

# 특정 Partition의 특정 Offset부터 읽기
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic my-topic --partition 0 --offset 1000

# 키와 값 함께 출력
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic my-topic \
  --property print.key=true \
  --property key.separator=":"
```

#### Offset 관리

```bash
# Offset 리셋 (--dry-run으로 먼저 확인)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group my-group --topic my-topic \
  --reset-offsets --to-earliest --dry-run

# 실제 리셋 실행
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group my-group --topic my-topic \
  --reset-offsets --to-earliest --execute

# 특정 시간으로 리셋
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group my-group --topic my-topic \
  --reset-offsets --to-datetime 2024-01-15T10:00:00.000 --execute
```

### 2.2 Kafka 내부 도구

#### kafka-log-dirs (디스크 사용량)

```bash
kafka-log-dirs.sh --bootstrap-server localhost:9092 \
  --describe --topic-list my-topic

# 브로커별 디스크 사용량
kafka-log-dirs.sh --bootstrap-server localhost:9092 \
  --broker-list 0,1,2 --describe
```

#### kafka-dump-log (세그먼트 분석)

```bash
# 로그 세그먼트 내용 확인
kafka-dump-log.sh --files /kafka-logs/my-topic-0/00000000000000000000.log \
  --print-data-log
```

### 2.3 JMX 메트릭 조회

```bash
# JMX 활성화 (Kafka 시작 시)
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false"
```

**주요 모니터링 메트릭**

| 메트릭 | MBean | 설명 |
|--------|-------|------|
| Messages In | `kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec` | 초당 수신 메시지 |
| Bytes Out | `kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec` | 초당 송신 바이트 |
| Under Replicated | `kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions` | 복제 지연 Partition |
| ISR Shrink | `kafka.server:type=ReplicaManager,name=IsrShrinksPerSec` | ISR 축소 빈도 |

### 2.4 Kubernetes 환경 진단

```bash
# Kafka Pod 상태 확인
kubectl get pods -l app=kafka -n kafka

# Kafka 로그 확인
kubectl logs -f kafka-0 -n kafka

# Pod 내부에서 직접 명령 실행
kubectl exec -it kafka-0 -n kafka -- \
  kafka-topics.sh --bootstrap-server localhost:9092 --list

# PVC 상태 확인 (디스크 문제)
kubectl get pvc -n kafka
kubectl describe pvc kafka-data-kafka-0 -n kafka
```

---

## 3. 로그 분석 가이드

### 3.1 로그 위치 및 종류

```
/var/log/kafka/
├── server.log           # 브로커 메인 로그
├── controller.log       # Controller 관련 로그
├── state-change.log     # 파티션 상태 변경 로그
├── kafka-request.log    # 클라이언트 요청 로그
└── log-cleaner.log      # 로그 정리 관련
```

### 3.2 주요 에러 패턴 및 의미

#### Pattern 1: Leader Election 문제

```
[2024-01-15 10:30:00] WARN [Controller] Partition [my-topic,0]
  is under min ISR, current ISR is [1], required ISR is [2]
```

**의미**: Replica가 Leader를 따라가지 못함
**조치**: 해당 브로커 상태 확인, 네트워크/디스크 점검

#### Pattern 2: Consumer Coordinator 문제

```
[2024-01-15 10:30:00] WARN [GroupCoordinator]
  Group my-group is rebalancing, member consumer-1 has left
```

**의미**: Consumer가 그룹을 떠남 (Timeout 또는 명시적 종료)
**조치**: Consumer 애플리케이션 로그 확인, Timeout 설정 검토

#### Pattern 3: 디스크 공간 부족

```
[2024-01-15 10:30:00] ERROR [Log partition=my-topic-0]
  Failed to append messages to log, reason:
  No space left on device
```

**의미**: 브로커 디스크 Full
**조치**: 디스크 확장, Retention 정책 조정, 불필요 토픽 삭제

#### Pattern 4: 인증/인가 실패

```
[2024-01-15 10:30:00] ERROR [SocketServer]
  Failed authentication with /10.0.0.5
  (Authentication failed during authentication
  due to: Invalid username or password)
```

**의미**: SASL 인증 실패
**조치**: 클라이언트 credentials 확인

### 3.3 로그 분석 스크립트

```bash
#!/bin/bash
# kafka-log-analyzer.sh

LOG_FILE=${1:-"/var/log/kafka/server.log"}
PERIOD=${2:-"1h"}

echo "=== Kafka Log Analysis (Last $PERIOD) ==="

# 에러 카운트
echo -e "\n[ERROR Count by Type]"
grep -E "ERROR|WARN" "$LOG_FILE" | \
  awk -F']' '{print $2}' | \
  sort | uniq -c | sort -rn | head -10

# Rebalance 이벤트
echo -e "\n[Rebalance Events]"
grep -c "rebalancing" "$LOG_FILE"

# Controller 변경
echo -e "\n[Controller Changes]"
grep "controller" "$LOG_FILE" | tail -5

# Under Replicated Partitions
echo -e "\n[Under Replicated Warnings]"
grep "UnderReplicatedPartitions" "$LOG_FILE" | tail -5
```

### 3.4 Spring Boot 애플리케이션 로그 설정

```yaml
# application.yml
logging:
  level:
    org.apache.kafka: INFO
    org.springframework.kafka: DEBUG  # 상세 디버깅 시

    # 특정 컴포넌트만 상세 로깅
    org.apache.kafka.clients.consumer: DEBUG
    org.apache.kafka.clients.producer: DEBUG
    org.apache.kafka.clients.admin: WARN
```

```xml
<!-- logback-spring.xml -->
<appender name="KAFKA_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/kafka-client.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/kafka-client.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<logger name="org.apache.kafka" level="INFO" additivity="false">
    <appender-ref ref="KAFKA_LOG"/>
</logger>
```

---

## 4. 성능 튜닝 체크리스트

### 4.1 Producer 튜닝

| 설정 | 기본값 | 권장값 | 설명 |
|------|--------|--------|------|
| `batch.size` | 16KB | 32KB-128KB | 배치 크기 증가로 처리량 향상 |
| `linger.ms` | 0 | 5-100 | 배치 형성 대기 시간 |
| `buffer.memory` | 32MB | 64MB+ | Producer 버퍼 크기 |
| `compression.type` | none | lz4 / snappy | 압축으로 네트워크 절약 |
| `acks` | 1 | all (안정성) / 1 (성능) | 복제 확인 수준 |

```yaml
# 고성능 Producer 설정
spring:
  kafka:
    producer:
      batch-size: 65536         # 64KB
      buffer-memory: 67108864   # 64MB
      compression-type: lz4
      properties:
        linger.ms: 20
        acks: 1
```

### 4.2 Consumer 튜닝

| 설정 | 기본값 | 권장값 | 설명 |
|------|--------|--------|------|
| `fetch.min.bytes` | 1 | 1KB-1MB | 최소 fetch 크기 |
| `fetch.max.wait.ms` | 500 | 100-500 | fetch 최대 대기 |
| `max.poll.records` | 500 | 100-1000 | poll당 레코드 수 |
| `max.partition.fetch.bytes` | 1MB | 1-10MB | Partition당 최대 fetch |

```yaml
# 고성능 Consumer 설정
spring:
  kafka:
    consumer:
      fetch-min-size: 1048576    # 1MB
      fetch-max-wait: 200ms
      max-poll-records: 1000
      properties:
        max.partition.fetch.bytes: 5242880  # 5MB
```

### 4.3 Broker 튜닝

```properties
# server.properties

# 네트워크 스레드
num.network.threads=8
num.io.threads=16

# 소켓 버퍼
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# 로그 설정
num.partitions=6
default.replication.factor=3
min.insync.replicas=2

# 로그 정리
log.retention.hours=168
log.segment.bytes=1073741824
log.cleanup.policy=delete
```

### 4.4 JVM 튜닝

```bash
# Kafka Broker JVM 설정
export KAFKA_HEAP_OPTS="-Xmx6g -Xms6g"
export KAFKA_JVM_PERFORMANCE_OPTS="-XX:+UseG1GC \
  -XX:MaxGCPauseMillis=20 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:+ExplicitGCInvokesConcurrent"
```

```yaml
# Spring Boot Consumer JVM
java -Xmx2g -Xms2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -jar consumer-app.jar
```

### 4.5 튜닝 체크리스트

#### Producer

- [ ] 배치 사이즈와 linger.ms 조합 최적화
- [ ] 압축 타입 설정 (네트워크 vs CPU 트레이드오프)
- [ ] acks 설정이 요구사항에 맞는지 확인
- [ ] idempotence 활성화 여부 검토
- [ ] 재시도 설정 (retries, retry.backoff.ms)

#### Consumer

- [ ] max.poll.records와 처리 시간 균형
- [ ] fetch 관련 설정 (min.bytes, max.wait)
- [ ] Consumer Group 내 인스턴스 수 vs Partition 수
- [ ] commit 전략 (auto vs manual)
- [ ] session.timeout과 heartbeat.interval 설정

#### Broker

- [ ] Partition 수가 적절한지 검토
- [ ] Replication Factor 설정
- [ ] min.insync.replicas 설정
- [ ] Log Retention 정책
- [ ] 디스크 I/O 성능 확인

---

## 5. 디버깅 팁

### 5.1 메시지 추적하기

```java
@Component
public class KafkaMessageTracer {

    // Producer Interceptor로 발송 추적
    public static class TracingProducerInterceptor
            implements ProducerInterceptor<String, Object> {

        @Override
        public ProducerRecord<String, Object> onSend(
                ProducerRecord<String, Object> record) {
            String traceId = UUID.randomUUID().toString();
            record.headers().add("X-Trace-Id",
                traceId.getBytes(StandardCharsets.UTF_8));
            log.debug("Sending message with traceId: {} to topic: {}",
                traceId, record.topic());
            return record;
        }

        @Override
        public void onAcknowledgement(
                RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                log.error("Failed to send: {}", exception.getMessage());
            } else {
                log.debug("Message sent to {}:{} at offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
            }
        }
    }
}
```

### 5.2 Consumer 상태 확인

```java
@RestController
@RequestMapping("/debug/kafka")
public class KafkaDebugController {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @GetMapping("/consumers")
    public Map<String, Object> getConsumerStatus() {
        Map<String, Object> status = new HashMap<>();

        registry.getListenerContainerIds().forEach(id -> {
            MessageListenerContainer container =
                registry.getListenerContainer(id);

            Map<String, Object> containerInfo = new HashMap<>();
            containerInfo.put("running", container.isRunning());
            containerInfo.put("paused", container.isContainerPaused());

            if (container instanceof ConcurrentMessageListenerContainer) {
                ConcurrentMessageListenerContainer<?, ?> concurrent =
                    (ConcurrentMessageListenerContainer<?, ?>) container;
                containerInfo.put("concurrency", concurrent.getConcurrency());
            }

            status.put(id, containerInfo);
        });

        return status;
    }

    @PostMapping("/consumers/{id}/pause")
    public void pauseConsumer(@PathVariable String id) {
        registry.getListenerContainer(id).pause();
    }

    @PostMapping("/consumers/{id}/resume")
    public void resumeConsumer(@PathVariable String id) {
        registry.getListenerContainer(id).resume();
    }
}
```

### 5.3 Dead Letter Queue (DLQ) 패턴

```java
@Configuration
public class DlqConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        // DLQ로 보내는 Recoverer
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(
                    record.topic() + ".DLQ", record.partition()));

        // 3번 재시도 후 DLQ로 이동
        DefaultErrorHandler handler = new DefaultErrorHandler(
            recoverer, new FixedBackOff(1000L, 3));

        // 특정 예외는 재시도하지 않음
        handler.addNotRetryableExceptions(
            DeserializationException.class,
            ValidationException.class);

        return handler;
    }
}

// DLQ Consumer
@KafkaListener(topics = "orders.DLQ", groupId = "dlq-processor")
public void processDlq(ConsumerRecord<String, String> record) {
    log.error("Processing DLQ message: topic={}, partition={}, offset={}, value={}",
        record.topic(), record.partition(), record.offset(), record.value());

    // 실패 원인 헤더에서 확인
    Headers headers = record.headers();
    String exception = new String(headers.lastHeader(
        "kafka_dlt-exception-message").value());
    log.error("Original exception: {}", exception);

    // 알림 발송 또는 수동 처리 로직
}
```

### 5.4 테스트 환경에서 디버깅

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1,
    topics = {"test-topic"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "auto.create.topics.enable=false"
    })
class KafkaDebugTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void debugMessageFlow() throws Exception {
        // 테스트 Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ConsumerFactory<String, String> cf =
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps =
            new ContainerProperties("test-topic");

        BlockingQueue<ConsumerRecord<String, String>> records =
            new LinkedBlockingQueue<>();

        containerProps.setMessageListener(
            (MessageListener<String, String>) records::add);

        KafkaMessageListenerContainer<String, String> container =
            new KafkaMessageListenerContainer<>(cf, containerProps);
        container.start();

        // 메시지 발송
        kafkaTemplate.send("test-topic", "key1", "value1").get();

        // 수신 확인
        ConsumerRecord<String, String> received =
            records.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("key1");
        assertThat(received.value()).isEqualTo("value1");

        container.stop();
    }
}
```

### 5.5 일반적인 디버깅 시나리오

#### 시나리오 1: 메시지가 Consumer에 도착하지 않음

```
1. Producer 측 확인
   - 전송 성공 여부 (send().get() 또는 Callback 확인)
   - 토픽 이름 오타 확인

2. Broker 측 확인
   - 토픽 존재 여부 (kafka-topics.sh --describe)
   - 메시지 도착 확인 (kafka-console-consumer.sh)

3. Consumer 측 확인
   - Consumer Group ID 확인
   - auto.offset.reset 설정 (earliest vs latest)
   - 토픽 구독 상태 (kafka-consumer-groups.sh --describe)
```

#### 시나리오 2: 메시지 중복 처리

```
1. 원인 파악
   - enable.auto.commit=true이고 처리 중 실패
   - Rebalancing 중 offset commit 전 처리 완료

2. 해결책
   - 멱등성 처리 로직 구현 (DB unique constraint)
   - Manual commit 사용
   - Exactly-once semantics (Kafka Transactions)
```

```java
// 멱등성 처리 예시
@KafkaListener(topics = "orders")
@Transactional
public void processOrder(Order order) {
    // DB에서 이미 처리된 메시지인지 확인
    if (processedMessageRepository.existsByMessageId(order.getOrderId())) {
        log.warn("Duplicate message detected: {}", order.getOrderId());
        return;
    }

    // 처리 로직
    orderService.process(order);

    // 처리 완료 기록
    processedMessageRepository.save(new ProcessedMessage(order.getOrderId()));
}
```

#### 시나리오 3: 성능 병목 찾기

```java
// 처리 시간 측정
@KafkaListener(topics = "heavy-topic")
public void processWithMetrics(ConsumerRecord<String, Data> record) {
    long startTime = System.currentTimeMillis();

    try {
        process(record.value());
    } finally {
        long duration = System.currentTimeMillis() - startTime;

        if (duration > 1000) {
            log.warn("Slow processing: {}ms for offset={}, partition={}",
                duration, record.offset(), record.partition());
        }

        // Micrometer metrics
        meterRegistry.timer("kafka.consumer.processing.time")
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

---

## 요약: 트러블슈팅 플로우차트

```
문제 발생
    │
    ├─→ Consumer Lag 증가?
    │       └─→ 처리 속도 확인 → 배치/병렬 처리 적용
    │
    ├─→ Rebalancing 빈번?
    │       └─→ Timeout 설정 확인 → session/poll timeout 조정
    │
    ├─→ 메시지 순서 문제?
    │       └─→ Partition Key 확인 → max.in.flight 조정
    │
    ├─→ Connection 에러?
    │       └─→ 네트워크 확인 → Bootstrap servers, DNS 점검
    │
    └─→ 원인 불명?
            └─→ 로그 분석 → JMX 메트릭 확인 → 재현 테스트
```

---

## 추가 리소스

- [Kafka Documentation - Operations](https://kafka.apache.org/documentation/#operations)
- [Confluent Troubleshooting Guide](https://docs.confluent.io/platform/current/kafka/monitoring.html)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
