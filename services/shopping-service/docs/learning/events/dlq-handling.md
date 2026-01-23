# Dead Letter Queue (DLQ) 처리

## 개요

Dead Letter Queue(DLQ)는 처리에 실패한 메시지를 저장하는 별도의 토픽입니다. Portal Universe는 Spring Kafka의 `DefaultErrorHandler`와 `DeadLetterPublishingRecoverer`를 사용하여 DLQ를 구현합니다.

## DLQ 아키텍처

```
┌─────────────────┐     실패     ┌─────────────────┐
│  Original Topic │ ──────────→ │   Error Handler  │
│ (shopping.order │              │   (재시도 3회)    │
│   .created)     │              └────────┬────────┘
└─────────────────┘                       │
                                   재시도 실패
                                          │
                                          ▼
                               ┌─────────────────┐
                               │   DLQ Topic     │
                               │ (shopping.order │
                               │  .created.DLT)  │
                               └────────┬────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │ DLQ Processor   │
                               │ (분석 및 재처리) │
                               └─────────────────┘
```

## Error Handler 설정

### KafkaConsumerConfig

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final long RETRY_INTERVAL_MS = 1000L;
    private static final long MAX_RETRY_ATTEMPTS = 3L;

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Dead Letter Publishing Recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                // DLQ 토픽명: 원본토픽.DLT
                String dlqTopic = record.topic() + ".DLT";
                log.error("Message sent to DLQ: topic={}, key={}, error={}",
                        dlqTopic, record.key(), ex.getMessage());
                return new TopicPartition(dlqTopic, record.partition());
            }
        );

        // 고정 간격 재시도: 1초 간격, 최대 3회
        FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도하지 않을 예외 타입 지정
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,    // 잘못된 인자
            NullPointerException.class,        // Null 참조
            JsonParseException.class           // JSON 파싱 오류
        );

        // 재시도 로깅
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Retry attempt {} for topic={}, key={}, error={}",
                    deliveryAttempt, record.topic(), record.key(), ex.getMessage());
        });

        return errorHandler;
    }
}
```

## DLQ 토픽 네이밍

| 원본 토픽 | DLQ 토픽 |
|----------|---------|
| `shopping.order.created` | `shopping.order.created.DLT` |
| `shopping.payment.completed` | `shopping.payment.completed.DLT` |
| `shopping.delivery.shipped` | `shopping.delivery.shipped.DLT` |

## 재시도 전략

### 1. Fixed Backoff

일정한 간격으로 재시도:

```java
// 1초 간격, 최대 3회 재시도
FixedBackOff backOff = new FixedBackOff(1000L, 3L);
```

### 2. Exponential Backoff

점진적으로 간격 증가:

```java
// 초기 1초, 2배씩 증가, 최대 10초, 최대 5회
ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
backOff.setMaxInterval(10000L);
backOff.setMaxElapsedTime(30000L);
```

### 재시도하지 않는 예외

비즈니스 로직 오류는 재시도해도 실패하므로 바로 DLQ로 이동:

```java
errorHandler.addNotRetryableExceptions(
    IllegalArgumentException.class,    // 잘못된 입력값
    NullPointerException.class,        // 필수 데이터 누락
    DuplicateKeyException.class,       // 중복 데이터
    ValidationException.class          // 검증 실패
);
```

## DLQ 메시지 구조

DLQ로 이동한 메시지에는 추가 헤더가 포함됩니다:

```java
// DLQ 메시지 헤더
kafka_dlt-original-topic: shopping.order.created
kafka_dlt-original-partition: 0
kafka_dlt-original-offset: 12345
kafka_dlt-exception-fqcn: java.lang.RuntimeException
kafka_dlt-exception-message: Connection refused
kafka_dlt-exception-stacktrace: java.lang.RuntimeException: ...
kafka_dlt-original-timestamp: 1705912200000
kafka_dlt-original-timestamp-type: CREATE_TIME
```

## DLQ Processor

### 기본 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DlqMessageRepository dlqMessageRepository;

    @KafkaListener(
        topics = "shopping.order.created.DLT",
        groupId = "dlq-processor-group"
    )
    public void processDlqMessage(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.DLT_ORIGINAL_OFFSET) long originalOffset
    ) {
        log.info("Processing DLQ message: originalTopic={}, offset={}, error={}",
                originalTopic, originalOffset, exceptionMessage);

        // 1. DLQ 메시지 저장 (분석용)
        DlqMessage dlqMessage = DlqMessage.builder()
            .originalTopic(originalTopic)
            .originalOffset(originalOffset)
            .messageKey(record.key())
            .payload(objectMapper.writeValueAsString(record.value()))
            .exceptionMessage(exceptionMessage)
            .status(DlqStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        dlqMessageRepository.save(dlqMessage);

        // 2. 알림 발송 (운영팀)
        alertService.sendDlqAlert(dlqMessage);
    }
}
```

### 재처리 기능

```java
@Service
@RequiredArgsConstructor
public class DlqReprocessService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DlqMessageRepository dlqMessageRepository;

    /**
     * DLQ 메시지를 원본 토픽으로 재발행합니다.
     */
    @Transactional
    public void reprocess(Long dlqMessageId) {
        DlqMessage dlqMessage = dlqMessageRepository.findById(dlqMessageId)
            .orElseThrow(() -> new IllegalArgumentException("DLQ message not found"));

        if (dlqMessage.getStatus() != DlqStatus.PENDING) {
            throw new IllegalStateException("Message is not in PENDING status");
        }

        try {
            // 원본 토픽으로 재발행
            kafkaTemplate.send(
                dlqMessage.getOriginalTopic(),
                dlqMessage.getMessageKey(),
                objectMapper.readValue(dlqMessage.getPayload(), Object.class)
            ).get(10, TimeUnit.SECONDS);

            dlqMessage.markAsReprocessed();
            dlqMessageRepository.save(dlqMessage);

            log.info("DLQ message reprocessed: id={}", dlqMessageId);
        } catch (Exception e) {
            dlqMessage.incrementRetryCount();
            dlqMessage.setLastError(e.getMessage());
            dlqMessageRepository.save(dlqMessage);

            log.error("Failed to reprocess DLQ message: id={}", dlqMessageId, e);
            throw new RuntimeException("Reprocess failed", e);
        }
    }

    /**
     * 특정 조건의 DLQ 메시지를 일괄 재처리합니다.
     */
    public int reprocessBatch(String originalTopic, LocalDateTime before) {
        List<DlqMessage> messages = dlqMessageRepository
            .findByOriginalTopicAndStatusAndCreatedAtBefore(
                originalTopic, DlqStatus.PENDING, before);

        int successCount = 0;
        for (DlqMessage message : messages) {
            try {
                reprocess(message.getId());
                successCount++;
            } catch (Exception e) {
                log.error("Batch reprocess failed: id={}", message.getId(), e);
            }
        }

        return successCount;
    }
}
```

## DLQ 메시지 상태 관리

```java
public enum DlqStatus {
    PENDING,       // 대기 중
    REPROCESSED,   // 재처리 완료
    DISCARDED,     // 폐기됨
    MANUAL_FIX     // 수동 처리 필요
}

@Entity
@Table(name = "dlq_messages")
public class DlqMessage {
    @Id @GeneratedValue
    private Long id;

    private String originalTopic;
    private long originalOffset;
    private String messageKey;

    @Lob
    private String payload;

    private String exceptionMessage;

    @Enumerated(EnumType.STRING)
    private DlqStatus status;

    private int retryCount;
    private String lastError;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
```

## DLQ 관리 API

### REST Controller

```java
@RestController
@RequestMapping("/api/v1/admin/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqMessageRepository dlqMessageRepository;
    private final DlqReprocessService reprocessService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DlqMessageResponse>>> list(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) DlqStatus status,
            Pageable pageable
    ) {
        Page<DlqMessage> messages = dlqMessageRepository.findByTopicAndStatus(
            topic, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages.map(DlqMessageResponse::from)));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiResponse<Void>> reprocess(@PathVariable Long id) {
        reprocessService.reprocess(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<ApiResponse<Void>> discard(@PathVariable Long id) {
        DlqMessage message = dlqMessageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
        message.discard();
        dlqMessageRepository.save(message);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/batch-reprocess")
    public ResponseEntity<ApiResponse<BatchReprocessResult>> batchReprocess(
            @RequestBody BatchReprocessRequest request
    ) {
        int count = reprocessService.reprocessBatch(
            request.topic(), request.before());
        return ResponseEntity.ok(ApiResponse.success(new BatchReprocessResult(count)));
    }
}
```

## 모니터링 및 알림

### DLQ 메트릭

```java
@Component
@RequiredArgsConstructor
public class DlqMetrics {

    private final MeterRegistry meterRegistry;

    public void recordDlqMessage(String topic, String exceptionType) {
        meterRegistry.counter("dlq.messages.total",
            "topic", topic,
            "exception", exceptionType
        ).increment();
    }

    public void recordReprocess(String topic, boolean success) {
        meterRegistry.counter("dlq.reprocess.total",
            "topic", topic,
            "result", success ? "success" : "failure"
        ).increment();
    }
}
```

### Slack 알림

```java
@Component
@RequiredArgsConstructor
public class DlqAlertService {

    private final SlackClient slackClient;

    public void sendDlqAlert(DlqMessage message) {
        String alertMessage = String.format(
            ":warning: *DLQ 메시지 발생*\n" +
            "- 원본 토픽: `%s`\n" +
            "- 메시지 키: `%s`\n" +
            "- 에러: %s\n" +
            "- 발생 시각: %s",
            message.getOriginalTopic(),
            message.getMessageKey(),
            message.getExceptionMessage(),
            message.getCreatedAt()
        );

        slackClient.send("#alerts-dlq", alertMessage);
    }
}
```

## Best Practices

### 1. DLQ 토픽 자동 생성

```java
@Bean
public NewTopic orderCreatedDlqTopic() {
    return TopicBuilder.name("shopping.order.created.DLT")
        .partitions(1)
        .replicas(1)
        .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")  // 7일 보관
        .build();
}
```

### 2. DLQ 메시지 보관 정책

- **즉시 분석**: 발생 시 알림 및 분석
- **정기 리뷰**: 일일/주간 DLQ 현황 리뷰
- **자동 폐기**: 30일 이상 미처리 메시지 자동 폐기

### 3. 재처리 가이드라인

| 상황 | 조치 |
|------|------|
| 일시적 장애 (네트워크, DB 연결) | 자동 재처리 |
| 데이터 오류 | 수동 검토 후 수정/폐기 |
| 비즈니스 로직 버그 | 버그 수정 후 재처리 |
| 중복 메시지 | 폐기 |

### 4. 모니터링 대시보드

```
DLQ Dashboard
├── Total DLQ Messages (Today)
├── DLQ by Topic (Pie Chart)
├── DLQ Trend (Line Chart)
├── Pending Messages Count
├── Recent DLQ Messages (Table)
└── Reprocess Success Rate
```

## 관련 문서

- [Event Consumer](./event-consumer.md) - 이벤트 소비 패턴
- [Shopping Events](./shopping-events.md) - 이벤트 정의
- [Event Producer](./event-producer.md) - 이벤트 발행
