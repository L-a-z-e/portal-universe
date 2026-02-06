# Consumer Error Handling

## 개요

Kafka Consumer에서 에러 처리는 메시지 유실을 방지하고 시스템 안정성을 보장하는 핵심 요소입니다. Spring Kafka는 `DefaultErrorHandler`를 통해 재시도와 DLQ(Dead Letter Queue) 처리를 제공합니다.

## 에러 발생 지점

```
┌──────────────────────────────────────────────────────────────────┐
│                     Kafka Consumer Pipeline                       │
│                                                                   │
│  ① Poll            ② Deserialize      ③ Process        ④ Commit │
│  ┌─────────┐       ┌─────────┐        ┌─────────┐      ┌───────┐ │
│  │Fetch Msg│──────▶│ JSON→   │───────▶│Handler  │─────▶│Offset │ │
│  │from Kafka│      │ Object  │        │Logic    │      │Commit │ │
│  └─────────┘       └─────────┘        └─────────┘      └───────┘ │
│       │                  │                 │                │     │
│       ▼                  ▼                 ▼                ▼     │
│   Network Error    Deserialization    Business Logic    Commit    │
│   Timeout          Error              Exception         Failure   │
└──────────────────────────────────────────────────────────────────┘
```

### 에러 유형

| 단계 | 에러 유형 | 예시 | 재시도 가능 |
|------|----------|------|------------|
| Poll | 네트워크 오류 | Connection timeout | O |
| Deserialize | 역직렬화 오류 | Invalid JSON format | X |
| Process | 비즈니스 예외 | DB connection error | O |
| Process | 검증 오류 | NullPointerException | X |
| Commit | 커밋 실패 | Coordinator unavailable | O |

## Portal Universe 에러 핸들러 구현

### CommonErrorHandler 설정

```java
@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    // 1. DLQ Recoverer 설정
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

    // 2. 재시도 전략 설정
    FixedBackOff backOff = new FixedBackOff(
            RETRY_INTERVAL_MS,   // 1000ms
            MAX_RETRY_ATTEMPTS   // 3회
    );

    // 3. ErrorHandler 생성
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 4. 재시도하지 않을 예외 설정
    errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            NullPointerException.class
    );

    // 5. 재시도 리스너 설정
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
        log.warn("Retry attempt {} for topic={}, key={}, error={}",
                deliveryAttempt, record.topic(), record.key(), ex.getMessage());
    });

    return errorHandler;
}
```

## ErrorHandlingDeserializer

역직렬화 단계에서 발생하는 에러를 처리합니다.

```java
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();

    // ErrorHandlingDeserializer로 래핑
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
              ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
              ErrorHandlingDeserializer.class);

    // 실제 Deserializer 지정
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
              StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
              JsonDeserializer.class);

    // 신뢰할 수 있는 패키지 지정
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");

    return new DefaultKafkaConsumerFactory<>(props);
}
```

### 역직렬화 에러 처리 흐름

```
잘못된 JSON 메시지 수신
        │
        ▼
┌───────────────────────┐
│ ErrorHandlingDeserializer │
│                       │
│ try {                 │
│   deserialize()       │
│ } catch (Exception) { │
│   return null +       │
│   DeserializationException │
│ }                     │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ DefaultErrorHandler   │
│                       │
│ 재시도 없이 바로 DLQ  │
│ (역직렬화 에러는      │
│  재시도해도 무의미)   │
└───────────────────────┘
```

## 예외 분류 전략

### 재시도 가능한 예외 (Retryable)

```java
// 일시적 오류 - 재시도로 복구 가능
public class RetryableExceptions {
    // 네트워크 관련
    - java.net.ConnectException
    - java.net.SocketTimeoutException

    // 데이터베이스 관련
    - org.springframework.dao.TransientDataAccessException
    - java.sql.SQLTransientConnectionException

    // 외부 서비스 관련
    - org.springframework.web.client.ResourceAccessException
}
```

### 재시도 불가능한 예외 (Not Retryable)

```java
// 영구적 오류 - 재시도해도 실패
errorHandler.addNotRetryableExceptions(
    // 잘못된 입력
    IllegalArgumentException.class,
    NullPointerException.class,

    // 비즈니스 로직 오류
    CustomBusinessException.class,

    // 역직렬화 오류
    DeserializationException.class
);
```

## 에러 처리 흐름

```
메시지 처리 실패
        │
        ▼
┌───────────────────────┐
│ 예외 타입 확인        │
└───────────┬───────────┘
            │
    ┌───────┴───────┐
    │               │
    ▼               ▼
Retryable?      Not Retryable
    │               │
    ▼               │
┌─────────────┐     │
│ 재시도 (1)  │     │
└──────┬──────┘     │
       │            │
   실패?            │
       │            │
       ▼            │
┌─────────────┐     │
│ 재시도 (2)  │     │
└──────┬──────┘     │
       │            │
   실패?            │
       │            │
       ▼            │
┌─────────────┐     │
│ 재시도 (3)  │     │
└──────┬──────┘     │
       │            │
   실패?            │
       │            │
       ▼            ▼
┌─────────────────────┐
│ DLQ로 이동          │
│ (topic.DLT)         │
└─────────────────────┘
```

## Consumer 측 에러 처리 원칙

### 예외를 삼키지 않기

```java
// 잘못된 패턴: 예외를 catch하고 삼김
@KafkaListener(topics = "some-topic")
public void handleMessage(Event event) {
    try {
        processEvent(event);
    } catch (Exception e) {
        log.error("Processing failed", e);
        // 예외가 사라짐 → ErrorHandler가 동작하지 않음
        // → 재시도 없음, DLQ 없음
    }
}

// 올바른 패턴: 예외를 전파
@KafkaListener(topics = "some-topic")
public void handleMessage(Event event) {
    processEvent(event);
    // 예외 발생 시 자연스럽게 전파
    // → ErrorHandler가 재시도 및 DLQ 처리
}
```

### 부분 성공 처리

```java
@KafkaListener(topics = "bulk-topic")
public void handleBatch(List<Event> events) {
    List<Event> failed = new ArrayList<>();

    for (Event event : events) {
        try {
            process(event);
        } catch (Exception e) {
            failed.add(event);
        }
    }

    if (!failed.isEmpty()) {
        // 실패한 이벤트만 재처리 요청
        throw new BatchPartialFailureException(failed);
    }
}
```

## 로깅 전략

### 에러 발생 시 로깅

```java
errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
    log.warn("Kafka message retry - " +
             "topic: {}, " +
             "partition: {}, " +
             "offset: {}, " +
             "attempt: {}, " +
             "error: {}",
             record.topic(),
             record.partition(),
             record.offset(),
             deliveryAttempt,
             ex.getMessage());
});
```

### DLQ 이동 시 로깅

```java
DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
    kafkaTemplate,
    (record, ex) -> {
        log.error("Message sent to DLQ - " +
                  "originalTopic: {}, " +
                  "dlqTopic: {}, " +
                  "key: {}, " +
                  "error: {}, " +
                  "stackTrace: {}",
                  record.topic(),
                  record.topic() + ".DLT",
                  record.key(),
                  ex.getMessage(),
                  ExceptionUtils.getStackTrace(ex));

        return new TopicPartition(record.topic() + ".DLT", record.partition());
    }
);
```

## 모니터링 및 알림

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
            meterRegistry.counter("kafka.consumer.dlq.messages",
                    "topic", record.topic(),
                    "exception", ex.getClass().getSimpleName()
            ).increment();

            return new TopicPartition(record.topic() + ".DLT", record.partition());
        }
    );

    // ...
}
```

### 알림 설정 (AlertManager 예시)

```yaml
groups:
  - name: kafka-consumer-alerts
    rules:
      - alert: KafkaConsumerDLQMessages
        expr: increase(kafka_consumer_dlq_messages_total[5m]) > 10
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "DLQ messages increasing"
          description: "More than 10 messages sent to DLQ in last 5 minutes"
```

## Container Factory 설정

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        CommonErrorHandler errorHandler) {

    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);

    // 수동 커밋: 처리 완료 후 offset 커밋
    factory.getContainerProperties().setAckMode(AckMode.RECORD);

    return factory;
}
```

## Best Practices

1. **예외 분류 명확화** - Retryable vs Not Retryable 구분
2. **적절한 재시도 횟수** - 일반적으로 3-5회
3. **DLQ 필수 구성** - 실패 메시지 보존
4. **상세한 로깅** - 디버깅을 위한 컨텍스트 정보
5. **모니터링 설정** - DLQ 메시지 증가 알림
6. **수동 커밋 사용** - 처리 완료 보장

## 관련 문서

- [retry-strategy.md](./retry-strategy.md) - 재시도 전략
- [dlq-processing.md](./dlq-processing.md) - DLQ 처리
- [consumer-architecture.md](./consumer-architecture.md) - Consumer 아키텍처
