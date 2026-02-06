# DLQ (Dead Letter Queue) Processing

## 개요

Dead Letter Queue(DLQ)는 처리에 실패한 메시지를 저장하는 별도의 토픽입니다. 재시도를 모두 소진한 메시지가 DLQ로 이동하며, 나중에 실패 원인을 분석하고 재처리할 수 있습니다.

## DLQ 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│                        DLQ Flow                                      │
│                                                                      │
│  ┌──────────────┐     실패      ┌─────────────┐     3회 실패        │
│  │ Main Topic   │───────────────▶│   Retry    │───────────────────▶ │
│  │ order.created│               │  Process   │                      │
│  └──────────────┘               └─────────────┘                      │
│                                                        │             │
│                                                        ▼             │
│                                              ┌─────────────────┐     │
│                                              │  DLQ Topic      │     │
│                                              │ order.created.DLT│    │
│                                              └─────────────────┘     │
│                                                        │             │
│                                                        ▼             │
│                                              ┌─────────────────┐     │
│                                              │  수동 분석/재처리 │     │
│                                              └─────────────────┘     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Portal Universe DLQ 구현

### DeadLetterPublishingRecoverer 설정

```java
@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    // DLQ Recoverer 설정
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                // DLQ 토픽명: 원본토픽.DLT
                String dlqTopic = record.topic() + ".DLT";

                log.error("Message sent to DLQ: topic={}, key={}, error={}",
                        dlqTopic, record.key(), ex.getMessage());

                // 같은 파티션으로 전송 (순서 보장)
                return new TopicPartition(dlqTopic, record.partition());
            }
    );

    return new DefaultErrorHandler(recoverer, backOff);
}
```

### DLQ 토픽 네이밍

```
원본 토픽                      → DLQ 토픽
─────────────────────────────────────────────────
shopping.order.created         → shopping.order.created.DLT
shopping.delivery.shipped      → shopping.delivery.shipped.DLT
shopping.payment.completed     → shopping.payment.completed.DLT
shopping.coupon.issued         → shopping.coupon.issued.DLT
shopping.timedeal.started      → shopping.timedeal.started.DLT
user-signup                    → user-signup.DLT
```

## DLQ 메시지 헤더

DLQ로 이동한 메시지에는 실패 정보가 헤더에 추가됩니다.

```java
Headers:
┌────────────────────────────────────────────────────────────────┐
│ Header Name                    │ Description                   │
├────────────────────────────────┼───────────────────────────────┤
│ kafka_dlt-original-topic       │ 원본 토픽명                    │
│ kafka_dlt-original-partition   │ 원본 파티션 번호               │
│ kafka_dlt-original-offset      │ 원본 오프셋                    │
│ kafka_dlt-original-timestamp   │ 원본 타임스탬프                │
│ kafka_dlt-exception-fqcn       │ 예외 클래스 전체 이름          │
│ kafka_dlt-exception-message    │ 예외 메시지                    │
│ kafka_dlt-exception-stacktrace │ 스택 트레이스                  │
└────────────────────────────────┴───────────────────────────────┘
```

## DLQ 메시지 조회

### Kafka CLI 사용

```bash
# DLQ 토픽의 메시지 조회
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shopping.order.created.DLT \
  --from-beginning \
  --property print.headers=true \
  --property print.key=true
```

### Java Consumer로 조회

```java
@Component
public class DLQMonitor {

    @KafkaListener(
        topics = "#{@dlqTopics}",  // 모든 DLQ 토픽
        groupId = "dlq-monitor-group"
    )
    public void monitorDLQ(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String errorMessage) {

        log.error("DLQ message detected - " +
                  "originalTopic: {}, " +
                  "key: {}, " +
                  "error: {}",
                  originalTopic,
                  record.key(),
                  errorMessage);

        // 알림 발송, 대시보드 업데이트 등
    }
}
```

## DLQ 재처리 전략

### 1. 수동 재처리

```java
@Service
@RequiredArgsConstructor
public class DLQReprocessingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * DLQ 메시지를 원본 토픽으로 다시 발행
     */
    public void reprocess(ConsumerRecord<String, Object> dlqRecord) {
        String originalTopic = new String(
            dlqRecord.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value()
        );

        // 원본 토픽으로 재발행
        kafkaTemplate.send(originalTopic, dlqRecord.key(), dlqRecord.value());

        log.info("Reprocessed DLQ message to topic: {}", originalTopic);
    }
}
```

### 2. 자동 재처리 스케줄러

```java
@Component
@RequiredArgsConstructor
public class DLQAutoReprocessor {

    private final KafkaConsumer<String, Object> dlqConsumer;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
    public void reprocessDLQMessages() {
        dlqConsumer.subscribe(List.of(
            "shopping.order.created.DLT",
            "shopping.payment.completed.DLT"
        ));

        ConsumerRecords<String, Object> records = dlqConsumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, Object> record : records) {
            try {
                String originalTopic = extractOriginalTopic(record);
                kafkaTemplate.send(originalTopic, record.key(), record.value());
                log.info("Auto-reprocessed: {}", record.key());
            } catch (Exception e) {
                log.error("Auto-reprocessing failed: {}", record.key(), e);
            }
        }
    }
}
```

### 3. 조건부 재처리

```java
@Service
public class ConditionalDLQReprocessor {

    public void reprocessWithCondition(ConsumerRecord<String, Object> dlqRecord) {
        String exceptionClass = extractHeader(dlqRecord, KafkaHeaders.DLT_EXCEPTION_FQCN);

        // 일시적 오류만 재처리
        if (isTransientException(exceptionClass)) {
            reprocess(dlqRecord);
        } else {
            // 영구 오류는 로깅만
            log.warn("Skipping non-transient error: {}", exceptionClass);
        }
    }

    private boolean isTransientException(String exceptionClass) {
        return exceptionClass.contains("ConnectException") ||
               exceptionClass.contains("TimeoutException") ||
               exceptionClass.contains("TransientDataAccessException");
    }
}
```

## DLQ 모니터링

### 메트릭 수집

```java
@Bean
public CommonErrorHandler errorHandler(
        KafkaTemplate<String, Object> kafkaTemplate,
        MeterRegistry meterRegistry) {

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> {
            // 메트릭 기록
            meterRegistry.counter("kafka.dlq.messages",
                    "topic", record.topic(),
                    "exception", ex.getClass().getSimpleName()
            ).increment();

            return new TopicPartition(record.topic() + ".DLT", record.partition());
        }
    );

    return new DefaultErrorHandler(recoverer, backOff);
}
```

### Prometheus 쿼리 예시

```promql
# DLQ 메시지 증가율
rate(kafka_dlq_messages_total[5m])

# 토픽별 DLQ 메시지 수
sum by (topic) (kafka_dlq_messages_total)

# 예외 유형별 분포
sum by (exception) (kafka_dlq_messages_total)
```

### AlertManager 알림 설정

```yaml
groups:
  - name: kafka-dlq-alerts
    rules:
      - alert: KafkaDLQMessagesHigh
        expr: increase(kafka_dlq_messages_total[5m]) > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "DLQ messages increasing"
          description: "Topic {{ $labels.topic }} has {{ $value }} DLQ messages in 5min"

      - alert: KafkaDLQBacklogHigh
        expr: kafka_dlq_message_count > 100
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "DLQ backlog is high"
          description: "DLQ topic {{ $labels.topic }} has {{ $value }} unprocessed messages"
```

## DLQ 관리 도구

### Admin API로 DLQ 조회

```java
@RestController
@RequestMapping("/admin/dlq")
@RequiredArgsConstructor
public class DLQAdminController {

    private final AdminClient adminClient;
    private final KafkaConsumer<String, Object> consumer;

    @GetMapping("/topics")
    public List<String> getDLQTopics() {
        return adminClient.listTopics().names().get()
                .stream()
                .filter(t -> t.endsWith(".DLT"))
                .collect(Collectors.toList());
    }

    @GetMapping("/topics/{topic}/count")
    public Map<String, Long> getMessageCount(@PathVariable String topic) {
        // 토픽별 메시지 수 조회
    }

    @PostMapping("/topics/{topic}/reprocess")
    public void reprocessTopic(@PathVariable String topic) {
        // 특정 토픽 재처리
    }
}
```

## DLQ 보관 정책

### Kafka 토픽 설정

```properties
# DLQ 토픽 retention 설정 (7일)
retention.ms=604800000

# 최대 크기 제한
retention.bytes=10737418240  # 10GB
```

### 아카이빙 전략

```java
@Scheduled(cron = "0 0 0 * * SUN")  // 매주 일요일
public void archiveDLQMessages() {
    // 오래된 DLQ 메시지를 S3/GCS로 아카이빙
    for (String dlqTopic : getDLQTopics()) {
        List<ConsumerRecord<String, Object>> oldMessages =
            fetchMessagesOlderThan(dlqTopic, Duration.ofDays(7));

        archiveToStorage(dlqTopic, oldMessages);

        log.info("Archived {} messages from {}", oldMessages.size(), dlqTopic);
    }
}
```

## Best Practices

1. **DLQ 토픽 자동 생성** - 원본 토픽과 동일한 파티션 수
2. **헤더 정보 활용** - 원본 컨텍스트 보존
3. **정기적인 모니터링** - DLQ 증가 추이 확인
4. **재처리 전략 수립** - 자동/수동 재처리 프로세스
5. **보관 정책 설정** - 적절한 retention 기간
6. **알림 설정** - 비정상적인 DLQ 증가 감지

## 관련 문서

- [consumer-error-handling.md](./consumer-error-handling.md) - 에러 처리
- [retry-strategy.md](./retry-strategy.md) - 재시도 전략
- [consumer-architecture.md](./consumer-architecture.md) - Consumer 아키텍처
