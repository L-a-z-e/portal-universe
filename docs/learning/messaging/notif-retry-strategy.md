# Retry Strategy

## 개요

재시도 전략은 일시적인 오류로 실패한 메시지를 자동으로 재처리하는 메커니즘입니다. Spring Kafka는 `BackOff` 인터페이스를 통해 다양한 재시도 전략을 지원합니다.

## 재시도 전략 유형

### 1. Fixed BackOff (고정 간격)

일정한 간격으로 재시도합니다.

```java
// 1초 간격으로 3회 재시도
FixedBackOff backOff = new FixedBackOff(1000L, 3L);
```

```
실패 → 1초 대기 → 재시도1 → 1초 대기 → 재시도2 → 1초 대기 → 재시도3 → DLQ
```

**장점:** 간단하고 예측 가능
**단점:** 일시적 과부하 상황에서 비효율적

### 2. Exponential BackOff (지수 증가)

재시도 간격이 지수적으로 증가합니다.

```java
ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
backOff.setMaxInterval(30000L);    // 최대 30초
backOff.setMaxElapsedTime(60000L); // 총 1분까지
```

```
실패 → 1초 → 재시도1 → 2초 → 재시도2 → 4초 → 재시도3 → 8초 → 재시도4 → DLQ
```

**장점:** 시스템 복구 시간 확보, 과부하 방지
**단점:** 최악의 경우 긴 지연 발생

### 3. Exponential Random BackOff (지터 추가)

지수 증가에 랜덤 요소를 추가합니다.

```java
ExponentialBackOffWithJitter backOff = new ExponentialBackOffWithJitter(
    1000L,   // 초기 간격
    2.0,     // 승수
    0.5      // 지터 비율 (50%)
);
```

```
실패 → 1±0.5초 → 재시도1 → 2±1초 → 재시도2 → 4±2초 → 재시도3 → DLQ
```

**장점:** Thundering Herd 문제 방지
**단점:** 재시도 타이밍 예측 어려움

## Portal Universe 구현

### 현재 설정

```java
// KafkaConsumerConfig.java
private static final long RETRY_INTERVAL_MS = 1000L;  // 1초
private static final long MAX_RETRY_ATTEMPTS = 3L;    // 3회

@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    // Fixed BackOff 사용
    FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 재시도 리스너: 각 재시도 시 로깅
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
        log.warn("Retry attempt {} for topic={}, key={}, error={}",
                deliveryAttempt, record.topic(), record.key(), ex.getMessage());
    });

    return errorHandler;
}
```

### 재시도 타임라인

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Retry Timeline (Fixed BackOff)                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Time:  0s        1s        2s        3s        4s                   │
│        │         │         │         │         │                    │
│        ▼         ▼         ▼         ▼         ▼                    │
│     [Fail]    [Retry1]  [Retry2]  [Retry3]  [DLQ]                   │
│        │         │         │         │         │                    │
│     처리 실패   1차 재시도  2차 재시도  3차 재시도  DLQ로 이동        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 재시도 불가 예외 설정

특정 예외는 재시도해도 성공할 수 없으므로 즉시 DLQ로 이동합니다.

```java
errorHandler.addNotRetryableExceptions(
    // 입력 검증 오류
    IllegalArgumentException.class,
    NullPointerException.class,

    // 역직렬화 오류
    DeserializationException.class,

    // 비즈니스 로직 오류
    InvalidOrderException.class,
    UserNotFoundException.class
);
```

### 예외 분류 가이드

```
┌─────────────────────────────────────────────────────────────────┐
│                    Exception Classification                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Retryable (재시도 가능)           │  Not Retryable (재시도 불가)  │
│  ─────────────────────────         │  ───────────────────────────  │
│  • ConnectException                │  • IllegalArgumentException   │
│  • SocketTimeoutException          │  • NullPointerException       │
│  • TransientDataAccessException    │  • DeserializationException   │
│  • ResourceAccessException         │  • ValidationException        │
│  • SQLTransientConnectionException │  • BusinessRuleException      │
│                                    │                              │
│  일시적 오류 → 재시도로 복구 가능    │  영구적 오류 → 재시도 무의미   │
│                                    │                              │
└─────────────────────────────────────────────────────────────────┘
```

## 고급 재시도 전략

### 토픽별 재시도 전략

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object>
        criticalListenerContainerFactory() {
    // 중요 토픽: 더 많은 재시도
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
    backOff.setMaxElapsedTime(300000L); // 5분까지 재시도

    factory.setCommonErrorHandler(
        new DefaultErrorHandler(recoverer, backOff)
    );

    return factory;
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object>
        normalListenerContainerFactory() {
    // 일반 토픽: 기본 재시도
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    FixedBackOff backOff = new FixedBackOff(1000L, 3L);

    factory.setCommonErrorHandler(
        new DefaultErrorHandler(recoverer, backOff)
    );

    return factory;
}
```

### 사용 예시

```java
// 중요 토픽 - 더 많은 재시도
@KafkaListener(
    topics = "shopping.payment.completed",
    containerFactory = "criticalListenerContainerFactory"
)
public void handlePayment(NotificationEvent event) { ... }

// 일반 토픽 - 기본 재시도
@KafkaListener(
    topics = "shopping.timedeal.started",
    containerFactory = "normalListenerContainerFactory"
)
public void handleTimeDeal(NotificationEvent event) { ... }
```

### Retry Topic 패턴

Spring Kafka 2.9+에서 제공하는 패턴으로, 재시도 메시지를 별도 토픽으로 분리합니다.

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    dltStrategy = DltStrategy.FAIL_ON_ERROR,
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
)
@KafkaListener(topics = "shopping.order.created")
public void handleOrder(NotificationEvent event) {
    // 처리 로직
}
```

**생성되는 토픽:**
```
shopping.order.created           # 원본 토픽
shopping.order.created-retry-0   # 1차 재시도
shopping.order.created-retry-1   # 2차 재시도
shopping.order.created-retry-2   # 3차 재시도
shopping.order.created-dlt       # DLQ
```

## 재시도 전략 선택 가이드

| 상황 | 권장 전략 | 설정 |
|------|----------|------|
| 일반적인 일시 오류 | Fixed BackOff | 1초, 3회 |
| 외부 서비스 호출 | Exponential BackOff | 1초 시작, 30초 최대 |
| 대량 트래픽 | Exponential + Jitter | 50% 지터 추가 |
| 중요 트랜잭션 | Retry Topic | 토픽 분리로 추적 용이 |

## 모니터링

### 재시도 메트릭

```java
errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
    // 메트릭 기록
    meterRegistry.counter("kafka.consumer.retry",
            "topic", record.topic(),
            "attempt", String.valueOf(deliveryAttempt),
            "exception", ex.getClass().getSimpleName()
    ).increment();

    // 로깅
    log.warn("Retry attempt {} for topic={}", deliveryAttempt, record.topic());
});
```

### 재시도 현황 대시보드

```
┌─────────────────────────────────────────────────────────────────┐
│                    Retry Metrics Dashboard                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Topic                    │ Retry1 │ Retry2 │ Retry3 │ DLQ      │
│  ─────────────────────────┼────────┼────────┼────────┼──────────│
│  shopping.order.created   │   15   │    5   │    2   │    1     │
│  shopping.payment.completed│    8   │    3   │    1   │    0     │
│  shopping.delivery.shipped│   12   │    4   │    1   │    0     │
│                                                                  │
│  Total Retry Rate: 2.5%                                         │
│  DLQ Rate: 0.1%                                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Best Practices

1. **적절한 재시도 횟수** - 너무 많으면 지연, 너무 적으면 유실
2. **Exponential BackOff 선호** - 시스템 복구 시간 확보
3. **Jitter 추가** - 다수 Consumer 동시 재시도 방지
4. **Not Retryable 예외 명시** - 불필요한 재시도 방지
5. **재시도 로깅** - 문제 추적을 위한 컨텍스트 기록
6. **토픽별 전략 차별화** - 중요도에 따른 전략 분리

## 관련 문서

- [consumer-error-handling.md](./consumer-error-handling.md) - 에러 처리
- [dlq-processing.md](./dlq-processing.md) - DLQ 처리
- [consumer-architecture.md](./consumer-architecture.md) - Consumer 아키텍처
