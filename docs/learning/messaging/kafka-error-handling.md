# Kafka Error Handling

Kafka 메시지 처리 시 발생하는 에러를 효과적으로 처리하는 방법을 다룹니다.

---

## 목차

1. [Producer 에러 처리](#1-producer-에러-처리)
2. [Consumer 에러 처리](#2-consumer-에러-처리)
3. [Dead Letter Queue (DLQ) 패턴](#3-dead-letter-queue-dlq-패턴)
4. [Spring Kafka ErrorHandler](#4-spring-kafka-errorhandler)
5. [SeekToCurrentErrorHandler와 DeadLetterPublishingRecoverer](#5-seektocurrenterrorhandler와-deadletterpublishingrecoverer)
6. [Portal Universe 에러 처리 분석](#6-portal-universe-에러-처리-분석)

---

## 1. Producer 에러 처리

### 1.1 에러 발생 시나리오

Producer에서 발생할 수 있는 주요 에러:

| 에러 유형 | 원인 | 재시도 가능 여부 |
|----------|------|-----------------|
| `NetworkException` | 네트워크 단절 | O |
| `NotLeaderForPartitionException` | 리더 변경 중 | O |
| `TimeoutException` | 응답 지연 | O |
| `SerializationException` | 직렬화 실패 | X |
| `RecordTooLargeException` | 메시지 크기 초과 | X |

### 1.2 Retry 설정

```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // ========================================
    // 재시도 관련 설정
    // ========================================

    // 재시도 횟수 (기본값: 2147483647 in Kafka 2.1+)
    props.put(ProducerConfig.RETRIES_CONFIG, 3);

    // 재시도 간격 (기본값: 100ms)
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

    // 전송 타임아웃 (기본값: 120000ms)
    // retries * retry.backoff.ms보다 커야 함
    props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

    // ========================================
    // 메시지 신뢰성 설정
    // ========================================

    // 모든 ISR(In-Sync Replica)의 확인 필요
    props.put(ProducerConfig.ACKS_CONFIG, "all");

    // 멱등성 활성화: 네트워크 오류로 인한 중복 방지
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    // 동시에 보낼 수 있는 요청 수 (멱등성 활성화 시 최대 5)
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

    return new DefaultKafkaProducerFactory<>(props);
}
```

### 1.3 Callback을 통한 에러 처리

#### 동기 방식 (Blocking)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 동기 방식: 결과를 기다림
     * 장점: 에러를 즉시 처리 가능
     * 단점: 블로킹으로 인한 성능 저하
     */
    public void publishSync(String topic, String key, Object event) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, event)
                    .get(10, TimeUnit.SECONDS);  // 타임아웃 설정

            log.info("Message sent successfully: topic={}, partition={}, offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (ExecutionException e) {
            // Kafka 전송 실패
            log.error("Failed to send message: {}", e.getCause().getMessage());
            throw new MessagePublishException("Kafka 메시지 전송 실패", e.getCause());

        } catch (TimeoutException e) {
            // 타임아웃
            log.error("Message send timeout: {}", e.getMessage());
            throw new MessagePublishException("Kafka 메시지 전송 타임아웃", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException("메시지 전송 중 인터럽트", e);
        }
    }
}
```

#### 비동기 방식 (Non-blocking)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 비동기 방식: 콜백으로 결과 처리
     * 장점: 논블로킹으로 높은 처리량
     * 단점: 에러 처리가 비동기적으로 발생
     */
    public void publishAsync(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // 성공
                log.info("Event published: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            } else {
                // 실패 - 별도 처리 필요
                handlePublishFailure(topic, key, event, ex);
            }
        });
    }

    private void handlePublishFailure(String topic, String key, Object event, Throwable ex) {
        log.error("Failed to publish event: topic={}, key={}, error={}",
                topic, key, ex.getMessage());

        // 실패 처리 옵션:
        // 1. DB에 저장 후 나중에 재시도 (Outbox Pattern)
        // 2. 로컬 파일에 저장
        // 3. 알림 발송
        // 4. 메트릭 기록
    }
}
```

### 1.4 ProducerListener를 통한 전역 에러 처리

```java
@Slf4j
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);

        // 전역 ProducerListener 설정
        template.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, Object> record,
                                  RecordMetadata metadata) {
                log.debug("Message sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }

            @Override
            public void onError(ProducerRecord<String, Object> record,
                               RecordMetadata metadata,
                               Exception exception) {
                log.error("Message send failed: topic={}, key={}, error={}",
                        record.topic(), record.key(), exception.getMessage());

                // 메트릭 기록
                meterRegistry.counter("kafka.producer.errors",
                        "topic", record.topic()).increment();
            }
        });

        return template;
    }
}
```

---

## 2. Consumer 에러 처리

### 2.1 Consumer 에러 유형

| 에러 유형 | 설명 | 처리 방법 |
|----------|------|----------|
| Deserialization Error | 메시지 역직렬화 실패 | ErrorHandlingDeserializer |
| Business Logic Error | 비즈니스 로직 예외 | ErrorHandler + Retry |
| Transient Error | 일시적 오류 (DB 연결 등) | 재시도 |
| Permanent Error | 영구적 오류 (잘못된 데이터) | DLQ로 이동 |

### 2.2 ErrorHandlingDeserializer

역직렬화 에러 발생 시 Consumer가 중단되지 않도록 처리:

```java
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

    // ErrorHandlingDeserializer로 감싸기
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            ErrorHandlingDeserializer.class);

    // 실제 역직렬화기 지정
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
            StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
            JsonDeserializer.class);

    // 신뢰할 수 있는 패키지 (역직렬화 허용)
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");

    return new DefaultKafkaConsumerFactory<>(props);
}
```

### 2.3 try-catch vs ErrorHandler

#### Anti-Pattern: try-catch로 예외 삼키기

```java
// ❌ 잘못된 방식: 예외를 삼키면 문제를 숨김
@KafkaListener(topics = "orders")
public void handleOrder(OrderEvent event) {
    try {
        orderService.process(event);
    } catch (Exception e) {
        log.error("Order processing failed", e);
        // 예외가 삼켜져서 에러 핸들러가 동작하지 않음
        // 메시지는 처리된 것으로 간주되어 offset 커밋됨
    }
}
```

#### Best Practice: ErrorHandler에 위임

```java
// ✅ 올바른 방식: 예외를 던져서 ErrorHandler가 처리하도록 함
@KafkaListener(topics = "orders")
public void handleOrder(OrderEvent event) {
    log.info("Processing order: {}", event.getOrderId());

    // 비즈니스 로직 수행
    // 예외 발생 시 ErrorHandler가 재시도/DLQ 처리
    orderService.process(event);

    log.info("Order processed successfully: {}", event.getOrderId());
}
```

### 2.4 Offset 관리

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        CommonErrorHandler errorHandler) {

    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);

    // 수동 커밋 모드
    factory.getContainerProperties().setAckMode(AckMode.RECORD);

    return factory;
}
```

#### AckMode 종류

| AckMode | 설명 | 사용 시나리오 |
|---------|------|--------------|
| `RECORD` | 각 레코드 처리 후 커밋 | 메시지 손실 방지 필요 시 |
| `BATCH` | 배치 처리 후 커밋 (기본값) | 높은 처리량 필요 시 |
| `MANUAL` | 수동으로 직접 커밋 | 세밀한 제어 필요 시 |
| `MANUAL_IMMEDIATE` | 즉시 수동 커밋 | 즉각적인 커밋 필요 시 |

---

## 3. Dead Letter Queue (DLQ) 패턴

### 3.1 DLQ 개념

```
┌─────────────┐     처리 성공     ┌─────────────────┐
│   Producer  │ ───────────────→ │    Consumer     │
└─────────────┘                  └─────────────────┘
       │                                  │
       │                                  │ 처리 실패
       ▼                                  │ (재시도 초과)
┌─────────────┐                          │
│ Main Topic  │                          ▼
│  (orders)   │                  ┌─────────────────┐
└─────────────┘                  │   DLQ Topic     │
                                 │ (orders.DLT)    │
                                 └─────────────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │  Manual Review  │
                                 │  or Reprocess   │
                                 └─────────────────┘
```

### 3.2 DLQ 장점

1. **메시지 보존**: 실패한 메시지를 잃지 않음
2. **Consumer 중단 방지**: 문제 메시지로 인한 전체 처리 중단 방지
3. **디버깅 용이**: 실패 원인 분석 가능
4. **재처리 가능**: 문제 해결 후 재처리 가능

### 3.3 DLQ 메시지에 포함되는 정보

```java
// Spring Kafka가 DLQ 메시지에 자동으로 추가하는 헤더
public class DlqHeaders {
    // 원본 토픽
    public static final String ORIGINAL_TOPIC = "kafka_dlt-original-topic";

    // 원본 파티션
    public static final String ORIGINAL_PARTITION = "kafka_dlt-original-partition";

    // 원본 offset
    public static final String ORIGINAL_OFFSET = "kafka_dlt-original-offset";

    // 원본 timestamp
    public static final String ORIGINAL_TIMESTAMP = "kafka_dlt-original-timestamp";

    // 예외 메시지
    public static final String EXCEPTION_MESSAGE = "kafka_dlt-exception-message";

    // 예외 스택트레이스
    public static final String EXCEPTION_STACKTRACE = "kafka_dlt-exception-stacktrace";
}
```

### 3.4 DLQ Consumer 구현

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final DlqRepository dlqRepository;
    private final AlertService alertService;

    @KafkaListener(topics = "orders.DLT", groupId = "dlq-handler-group")
    public void handleDlq(ConsumerRecord<String, Object> record) {
        log.warn("Processing DLQ message: topic={}, key={}",
                record.topic(), record.key());

        // 헤더에서 원본 정보 추출
        String originalTopic = getHeader(record, "kafka_dlt-original-topic");
        String exceptionMessage = getHeader(record, "kafka_dlt-exception-message");

        // DB에 저장 (나중에 분석/재처리용)
        DlqMessage dlqMessage = DlqMessage.builder()
                .originalTopic(originalTopic)
                .messageKey(record.key())
                .payload(record.value().toString())
                .errorMessage(exceptionMessage)
                .createdAt(LocalDateTime.now())
                .status(DlqStatus.PENDING)
                .build();

        dlqRepository.save(dlqMessage);

        // 알림 발송 (운영팀에게)
        alertService.sendAlert(
                "DLQ Message Received",
                String.format("Topic: %s, Error: %s", originalTopic, exceptionMessage)
        );
    }

    private String getHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
```

### 3.5 DLQ 재처리 서비스

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqReprocessService {

    private final DlqRepository dlqRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 특정 DLQ 메시지를 원본 토픽으로 재발행
     */
    @Transactional
    public void reprocess(Long dlqMessageId) {
        DlqMessage message = dlqRepository.findById(dlqMessageId)
                .orElseThrow(() -> new NotFoundException("DLQ message not found"));

        try {
            // 원본 토픽으로 재발행
            kafkaTemplate.send(
                    message.getOriginalTopic(),
                    message.getMessageKey(),
                    objectMapper.readValue(message.getPayload(), Object.class)
            ).get(10, TimeUnit.SECONDS);

            message.setStatus(DlqStatus.REPROCESSED);
            message.setReprocessedAt(LocalDateTime.now());

            log.info("DLQ message reprocessed: id={}", dlqMessageId);

        } catch (Exception e) {
            message.setStatus(DlqStatus.FAILED);
            message.setRetryCount(message.getRetryCount() + 1);
            log.error("Failed to reprocess DLQ message: id={}", dlqMessageId, e);
            throw new ReprocessException("재처리 실패", e);
        }
    }

    /**
     * 특정 토픽의 모든 PENDING 상태 DLQ 메시지 일괄 재처리
     */
    @Transactional
    public int reprocessAll(String originalTopic) {
        List<DlqMessage> messages = dlqRepository.findByOriginalTopicAndStatus(
                originalTopic, DlqStatus.PENDING);

        int successCount = 0;
        for (DlqMessage message : messages) {
            try {
                reprocess(message.getId());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to reprocess: id={}", message.getId());
            }
        }

        return successCount;
    }
}
```

---

## 4. Spring Kafka ErrorHandler

### 4.1 ErrorHandler 발전 역사

| 버전 | 클래스명 | 상태 |
|------|---------|------|
| Spring Kafka 2.2 이전 | `SeekToCurrentErrorHandler` | Deprecated |
| Spring Kafka 2.8+ | `DefaultErrorHandler` | 현재 권장 |

### 4.2 DefaultErrorHandler 상세 설정

```java
@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

    // 1. DeadLetterPublishingRecoverer 설정
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                // DLQ 토픽 결정 로직
                String dlqTopic = record.topic() + ".DLT";
                log.error("Sending to DLQ: topic={}, key={}, error={}",
                        dlqTopic, record.key(), ex.getMessage());
                return new TopicPartition(dlqTopic, record.partition());
            }
    );

    // 2. BackOff 전략 설정
    // 옵션 A: 고정 간격
    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);  // 1초 간격, 3회 재시도

    // 옵션 B: 지수 백오프 (Exponential Backoff)
    ExponentialBackOff exponentialBackOff = new ExponentialBackOff(1000L, 2.0);
    exponentialBackOff.setMaxElapsedTime(30000L);  // 최대 30초

    // 3. DefaultErrorHandler 생성
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);

    // 4. 재시도하지 않을 예외 설정
    // 이 예외들은 재시도해도 실패하므로 바로 DLQ로 이동
    errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,      // 잘못된 인자
            NullPointerException.class,          // Null 참조
            JsonParseException.class,            // JSON 파싱 실패
            DeserializationException.class       // 역직렬화 실패
    );

    // 5. 재시도할 예외 설정 (선택적)
    errorHandler.addRetryableExceptions(
            TransientDataAccessException.class,  // 일시적 DB 오류
            SocketTimeoutException.class         // 네트워크 타임아웃
    );

    // 6. RetryListener 설정 (재시도 시 로깅/메트릭)
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
        log.warn("Retry attempt {}/{} for topic={}, key={}, error={}",
                deliveryAttempt, 3, record.topic(), record.key(), ex.getMessage());

        // 메트릭 기록
        meterRegistry.counter("kafka.consumer.retry",
                "topic", record.topic(),
                "attempt", String.valueOf(deliveryAttempt)
        ).increment();
    });

    return errorHandler;
}
```

### 4.3 BackOff 전략 비교

#### FixedBackOff (고정 간격)

```java
// 1초 간격으로 최대 3번 재시도
// 총 시도: 1번 (최초) + 3번 (재시도) = 4번
FixedBackOff backOff = new FixedBackOff(1000L, 3L);
```

```
시도 1 ──(실패)──▶ 1초 대기 ──▶ 시도 2 ──(실패)──▶ 1초 대기 ──▶ 시도 3 ──(실패)──▶ 1초 대기 ──▶ 시도 4 ──(실패)──▶ DLQ
```

#### ExponentialBackOff (지수 백오프)

```java
// 초기 1초, 2배씩 증가, 최대 30초
ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
backOff.setMaxElapsedTime(30000L);  // 총 경과 시간 제한
backOff.setMaxInterval(10000L);     // 최대 대기 간격 제한
```

```
시도 1 ──(실패)──▶ 1초 대기 ──▶ 시도 2 ──(실패)──▶ 2초 대기 ──▶ 시도 3 ──(실패)──▶ 4초 대기 ──▶ 시도 4 ──(실패)──▶ DLQ
```

### 4.4 커스텀 Recoverer 구현

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRecoverer implements ConsumerRecordRecoverer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FailedMessageRepository failedMessageRepository;
    private final MeterRegistry meterRegistry;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        log.error("Message recovery triggered: topic={}, key={}, error={}",
                record.topic(), record.key(), exception.getMessage());

        // 1. 메트릭 기록
        meterRegistry.counter("kafka.consumer.recovery",
                "topic", record.topic()
        ).increment();

        // 2. DB에 저장 (상세 정보 포함)
        FailedMessage failedMessage = FailedMessage.builder()
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .key(record.key() != null ? record.key().toString() : null)
                .value(record.value() != null ? record.value().toString() : null)
                .errorMessage(exception.getMessage())
                .stackTrace(ExceptionUtils.getStackTrace(exception))
                .createdAt(LocalDateTime.now())
                .build();

        failedMessageRepository.save(failedMessage);

        // 3. DLQ로 전송
        String dlqTopic = record.topic() + ".DLT";
        kafkaTemplate.send(dlqTopic, (String) record.key(), record.value());

        // 4. 심각한 오류는 알림 발송
        if (isCriticalError(exception)) {
            alertService.sendCriticalAlert(record, exception);
        }
    }

    private boolean isCriticalError(Exception ex) {
        return ex instanceof DatabaseConnectionException
                || ex instanceof SystemException;
    }
}
```

---

## 5. SeekToCurrentErrorHandler와 DeadLetterPublishingRecoverer

### 5.1 SeekToCurrentErrorHandler (Deprecated)

> **Note**: Spring Kafka 2.8부터 `DefaultErrorHandler`로 대체되었습니다.

```java
// ❌ Deprecated 방식
@Bean
public SeekToCurrentErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);

    return new SeekToCurrentErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
}

// ✅ 현재 권장 방식
@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);

    return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
}
```

### 5.2 DeadLetterPublishingRecoverer 상세 설정

```java
@Bean
public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
        KafkaTemplate<String, Object> kafkaTemplate) {

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            // 토픽/파티션 결정 함수
            (record, ex) -> {
                // 예외 유형에 따라 다른 DLQ로 라우팅
                if (ex.getCause() instanceof DeserializationException) {
                    return new TopicPartition("deserialization-errors", 0);
                } else if (ex.getCause() instanceof ValidationException) {
                    return new TopicPartition("validation-errors", 0);
                } else {
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
            }
    );

    // 원본 메시지의 timestamp 유지
    recoverer.setRetainExceptionHeader(true);

    // 실패 헤더에 상세 정보 추가
    recoverer.setHeadersFunction((record, ex) -> {
        Headers headers = new RecordHeaders();
        headers.add("X-Original-Topic", record.topic().getBytes());
        headers.add("X-Error-Timestamp",
                LocalDateTime.now().toString().getBytes());
        headers.add("X-Service-Name", "notification-service".getBytes());
        return headers;
    });

    return recoverer;
}
```

### 5.3 조건부 DLQ 라우팅

```java
@Bean
public DeadLetterPublishingRecoverer conditionalRecoverer(
        KafkaTemplate<String, Object> kafkaTemplate) {

    return new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                String originalTopic = record.topic();
                Throwable cause = ex.getCause();

                // 1. 역직렬화 오류 전용 DLQ
                if (cause instanceof DeserializationException) {
                    log.warn("Deserialization error for topic: {}", originalTopic);
                    return new TopicPartition("dlq.deserialization", 0);
                }

                // 2. 검증 오류 전용 DLQ
                if (cause instanceof ConstraintViolationException) {
                    log.warn("Validation error for topic: {}", originalTopic);
                    return new TopicPartition("dlq.validation", 0);
                }

                // 3. 비즈니스 로직 오류 전용 DLQ
                if (cause instanceof BusinessException) {
                    log.warn("Business error for topic: {}", originalTopic);
                    return new TopicPartition("dlq.business", 0);
                }

                // 4. 기본 DLQ
                log.warn("Unknown error for topic: {}", originalTopic);
                return new TopicPartition(originalTopic + ".DLT", record.partition());
            }
    );
}
```

---

## 6. Portal Universe 에러 처리 분석

Portal Universe 프로젝트의 Kafka 에러 처리 구현을 분석합니다.

### 6.1 현재 구현 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Portal Universe                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐         ┌─────────────────┐                    │
│  │  auth-service   │         │ shopping-service│                    │
│  │                 │         │                 │                    │
│  │ KafkaTemplate   │         │ KafkaTemplate   │                    │
│  │ ├─ acks=all     │         │ ├─ acks=all     │                    │
│  │ ├─ retries=3    │         │ ├─ retries=3    │                    │
│  │ └─ (동기 전송)   │         │ └─ idempotence  │                    │
│  │                 │         │   (비동기+콜백)   │                    │
│  └────────┬────────┘         └────────┬────────┘                    │
│           │                           │                              │
│           │    user-signup            │  shopping.*                  │
│           ▼                           ▼                              │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                        Kafka Cluster                           │ │
│  │  Topics: user-signup, shopping.order.*, shopping.payment.*     │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                      │
│                               ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                   notification-service                       │    │
│  │                                                              │    │
│  │  ┌─────────────────────────────────────────────────────┐    │    │
│  │  │              KafkaConsumerConfig                     │    │    │
│  │  │                                                      │    │    │
│  │  │  ┌────────────────────────────────────────────┐     │    │    │
│  │  │  │ ErrorHandlingDeserializer                  │     │    │    │
│  │  │  │ - 역직렬화 오류 시 Consumer 중단 방지        │     │    │    │
│  │  │  └────────────────────────────────────────────┘     │    │    │
│  │  │                                                      │    │    │
│  │  │  ┌────────────────────────────────────────────┐     │    │    │
│  │  │  │ DefaultErrorHandler                        │     │    │    │
│  │  │  │ - FixedBackOff: 1초 간격, 3회 재시도        │     │    │    │
│  │  │  │ - NotRetryable: IllegalArgumentException,  │     │    │    │
│  │  │  │                 NullPointerException        │     │    │    │
│  │  │  │ - RetryListener: 재시도 시 로깅            │     │    │    │
│  │  │  └────────────────────────────────────────────┘     │    │    │
│  │  │                                                      │    │    │
│  │  │  ┌────────────────────────────────────────────┐     │    │    │
│  │  │  │ DeadLetterPublishingRecoverer              │     │    │    │
│  │  │  │ - DLQ Topic: {original-topic}.DLT          │     │    │    │
│  │  │  │ - 실패 메시지 보존                          │     │    │    │
│  │  │  └────────────────────────────────────────────┘     │    │    │
│  │  │                                                      │    │    │
│  │  │  AckMode: RECORD (각 메시지 처리 후 커밋)            │    │    │
│  │  └──────────────────────────────────────────────────────┘    │    │
│  │                                                              │    │
│  │  ┌─────────────────────────────────────────────────────┐    │    │
│  │  │              NotificationConsumer                    │    │    │
│  │  │                                                      │    │    │
│  │  │  @KafkaListener(topics = "shopping.order.created")  │    │    │
│  │  │  @KafkaListener(topics = "shopping.delivery.shipped")│    │    │
│  │  │  @KafkaListener(topics = "shopping.payment.completed")│   │    │
│  │  │                                                      │    │    │
│  │  │  - 예외를 삼키지 않고 ErrorHandler에 위임            │    │    │
│  │  │  - 알림 생성 및 푸시                                 │    │    │
│  │  └──────────────────────────────────────────────────────┘    │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 6.2 서비스별 구현 분석

#### auth-service (Producer)

**파일**: `services/auth-service/src/main/java/com/portal/universe/authservice/service/UserService.java`

```java
// 단순 비동기 전송 (Fire-and-forget)
kafkaTemplate.send("user-signup", event);
```

**특징**:
- 콜백 없이 단순 전송
- 전송 실패 시 별도 처리 없음
- 트랜잭션과 독립적으로 발행

**개선 포인트**:
```java
// 트랜잭션 커밋 후 이벤트 발행 권장
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleUserSignedUp(UserSignedUpDomainEvent domainEvent) {
    kafkaTemplate.send("user-signup", new UserSignedUpEvent(...));
}
```

#### shopping-service (Producer)

**파일**: `services/shopping-service/src/main/java/com/portal/universe/shoppingservice/event/ShoppingEventPublisher.java`

```java
// 비동기 전송 + 콜백 처리
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
```

**Producer 설정** (`KafkaConfig.java`):
```java
props.put(ProducerConfig.ACKS_CONFIG, "all");           // 모든 복제본 확인
props.put(ProducerConfig.RETRIES_CONFIG, 3);            // 3회 재시도
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 멱등성 활성화
```

**장점**:
- 전송 결과 로깅
- 멱등성으로 중복 방지
- 메시지 손실 최소화

#### notification-service (Consumer)

**파일**: `services/notification-service/src/main/java/com/portal/universe/notificationservice/config/KafkaConsumerConfig.java`

**에러 처리 전략**:

1. **ErrorHandlingDeserializer**
   - 잘못된 형식의 메시지로 인한 Consumer 중단 방지
   - 역직렬화 실패 시 에러 핸들러로 위임

2. **DefaultErrorHandler + DeadLetterPublishingRecoverer**
   - 1초 간격으로 최대 3회 재시도
   - 재시도 실패 시 DLQ로 이동
   - DLQ 토픽: `{원본토픽}.DLT`

3. **NotRetryableExceptions**
   - `IllegalArgumentException`: 재시도해도 실패
   - `NullPointerException`: 재시도해도 실패

4. **RetryListener**
   - 재시도 시마다 로깅

5. **AckMode.RECORD**
   - 각 메시지 처리 완료 후 offset 커밋
   - 메시지 손실 방지

### 6.3 Consumer 구현 패턴

**파일**: `services/notification-service/src/main/java/com/portal/universe/notificationservice/consumer/NotificationConsumer.java`

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(NotificationEvent event) {
    log.info("Received order created event: userId={}", event.getUserId());
    createAndPush(event);  // 예외를 삼키지 않음
}

private void createAndPush(NotificationEvent event) {
    // 예외 발생 시 ErrorHandler가 처리
    Notification notification = notificationService.create(...);
    pushService.push(notification);
}
```

**핵심 원칙**: 예외를 try-catch로 삼키지 않고 ErrorHandler에 위임

### 6.4 개선 권장 사항

#### 1. auth-service Producer 개선

```java
// 현재
kafkaTemplate.send("user-signup", event);

// 권장: 콜백 추가
kafkaTemplate.send("user-signup", event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Failed to publish user signup event", ex);
            // 실패 처리 로직 추가
        }
    });
```

#### 2. Outbox Pattern 도입

트랜잭션과 이벤트 발행의 일관성 보장:

```java
@Entity
public class OutboxEvent {
    @Id
    private Long id;
    private String topic;
    private String key;
    private String payload;
    private LocalDateTime createdAt;
    private OutboxStatus status;  // PENDING, PUBLISHED, FAILED
}

// 스케줄러로 PENDING 이벤트 발행
@Scheduled(fixedRate = 5000)
public void publishPendingEvents() {
    List<OutboxEvent> events = outboxRepository.findByStatus(PENDING);
    for (OutboxEvent event : events) {
        kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload());
        event.setStatus(PUBLISHED);
    }
}
```

#### 3. DLQ Consumer 추가

```java
@KafkaListener(topics = "shopping.order.created.DLT")
public void handleOrderCreatedDlq(ConsumerRecord<String, Object> record) {
    // DLQ 메시지 처리 로직
    // - DB 저장
    // - 알림 발송
    // - 수동 재처리 대기
}
```

#### 4. 모니터링 및 알림

```java
// 메트릭 기록
errorHandler.setRetryListeners((record, ex, attempt) -> {
    meterRegistry.counter("kafka.consumer.retry",
            "topic", record.topic()).increment();
});

// DLQ 메시지 수 알림
@Scheduled(cron = "0 0 * * * *")  // 매시간
public void checkDlqMessages() {
    long count = dlqMessageRepository.countByStatus(PENDING);
    if (count > 100) {
        alertService.sendAlert("DLQ messages exceeded threshold: " + count);
    }
}
```

### 6.5 에러 처리 흐름 요약

```
메시지 수신
    │
    ▼
ErrorHandlingDeserializer
    │
    ├─ 역직렬화 성공 ──────▶ @KafkaListener 메서드 실행
    │                              │
    │                              ├─ 성공 ──▶ offset 커밋 (AckMode.RECORD)
    │                              │
    │                              └─ 예외 발생
    │                                     │
    │                                     ▼
    │                              DefaultErrorHandler
    │                                     │
    │                                     ├─ NotRetryableException?
    │                                     │       │
    │                                     │       ├─ Yes ──▶ 즉시 DLQ로 이동
    │                                     │       │
    │                                     │       └─ No ──▶ 재시도 (FixedBackOff)
    │                                     │                     │
    │                                     │                     ├─ 성공 ──▶ offset 커밋
    │                                     │                     │
    │                                     │                     └─ 3회 실패 ──▶ DLQ로 이동
    │                                     │
    │                                     └─ DeadLetterPublishingRecoverer
    │                                             │
    │                                             ▼
    │                                       {topic}.DLT로 전송
    │
    └─ 역직렬화 실패 ──────▶ ErrorHandler로 위임 ──▶ DLQ로 이동
```

---

## 7. 모범 사례 체크리스트

### Producer

- [ ] `acks=all` 설정으로 메시지 손실 방지
- [ ] `enable.idempotence=true`로 중복 전송 방지
- [ ] 적절한 재시도 횟수 설정 (`retries`)
- [ ] 콜백을 통한 전송 실패 처리
- [ ] 중요한 이벤트는 Outbox Pattern 고려

### Consumer

- [ ] `ErrorHandlingDeserializer` 사용
- [ ] `DefaultErrorHandler` + BackOff 설정
- [ ] 재시도 불가능한 예외 분류 (`addNotRetryableExceptions`)
- [ ] DLQ 설정 (`DeadLetterPublishingRecoverer`)
- [ ] 수동 커밋 모드 (`AckMode.RECORD`)
- [ ] 예외를 삼키지 않고 ErrorHandler에 위임

### 운영

- [ ] DLQ 메시지 모니터링
- [ ] DLQ 재처리 프로세스 구축
- [ ] 재시도/DLQ 메트릭 수집
- [ ] 알림 설정 (DLQ 임계치 초과 시)

---

## 참고 자료

- [Spring for Apache Kafka Documentation](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Apache Kafka Documentation - Error Handling](https://kafka.apache.org/documentation/)
- [Spring Kafka Error Handling](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html)
