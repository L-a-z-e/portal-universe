# Kafka Consumer Architecture

## 개요

Kafka Consumer는 분산 메시징 시스템에서 메시지를 수신하고 처리하는 컴포넌트입니다. Notification Service에서는 Spring Kafka를 활용하여 다양한 도메인 이벤트를 수신하고 알림으로 변환합니다.

## 핵심 개념

### Consumer Group

Consumer Group은 Kafka의 핵심 개념으로, 같은 그룹에 속한 Consumer들이 토픽의 파티션을 나눠서 처리합니다.

```
Topic: shopping.order.created (3 partitions)
┌─────────────────────────────────────────────────┐
│ Consumer Group: notification-group              │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Consumer 1│  │Consumer 2│  │Consumer 3│      │
│  │Partition0│  │Partition1│  │Partition2│      │
│  └──────────┘  └──────────┘  └──────────┘      │
└─────────────────────────────────────────────────┘
```

**특징:**
- 같은 그룹 내에서 파티션은 하나의 Consumer에만 할당
- Consumer 수가 파티션 수보다 많으면 유휴 Consumer 발생
- 다른 그룹은 동일한 메시지를 독립적으로 수신 가능

### Offset 관리

Offset은 Consumer가 어디까지 읽었는지를 나타내는 위치 정보입니다.

```
Partition Timeline:
[0] [1] [2] [3] [4] [5] [6] [7] [8]
              ^           ^
        Committed      Current
         Offset        Position
```

**Offset 관리 전략:**

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| Auto Commit | 주기적 자동 커밋 | 간단한 구현 | 중복/유실 가능 |
| Manual Commit | 처리 후 명시적 커밋 | 정확한 제어 | 구현 복잡도 |
| Batch Commit | 배치 처리 후 커밋 | 성능 향상 | 재처리 범위 확대 |

## Portal Universe 구현

### KafkaConsumerConfig 분석

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    // 재시도 설정
    private static final long RETRY_INTERVAL_MS = 1000L;  // 1초 간격
    private static final long MAX_RETRY_ATTEMPTS = 3L;    // 3회 재시도

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 1. 연결 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // 2. 역직렬화 설정 (ErrorHandling 래퍼 사용)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  ErrorHandlingDeserializer.class);

        // 3. Offset 설정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 수동 커밋

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Notification Service                              │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │            ConcurrentKafkaListenerContainerFactory          │    │
│  │                                                             │    │
│  │   ┌─────────────────┐   ┌─────────────────┐                │    │
│  │   │ ListenerContainer│   │ ListenerContainer│  ...          │    │
│  │   │ (order.created) │   │ (delivery.shipped)│               │    │
│  │   └────────┬────────┘   └────────┬────────┘                │    │
│  │            │                     │                          │    │
│  │   ┌────────▼────────┐   ┌────────▼────────┐                │    │
│  │   │  ErrorHandler   │   │  ErrorHandler   │                │    │
│  │   │ (Retry + DLQ)  │   │ (Retry + DLQ)  │                │    │
│  │   └────────┬────────┘   └────────┬────────┘                │    │
│  │            │                     │                          │    │
│  └────────────┼─────────────────────┼──────────────────────────┘    │
│               │                     │                               │
│   ┌───────────▼─────────────────────▼────────────┐                  │
│   │              NotificationConsumer            │                  │
│   │                                              │                  │
│   │  handleOrderCreated()                        │                  │
│   │  handleDeliveryShipped()                     │                  │
│   │  handlePaymentCompleted()                    │                  │
│   │  handleCouponIssued()                        │                  │
│   │  handleTimeDealStarted()                     │                  │
│   └──────────────────────────────────────────────┘                  │
│                           │                                          │
│               ┌───────────▼───────────┐                              │
│               │   NotificationService │                              │
│               │   (Create & Persist)  │                              │
│               └───────────┬───────────┘                              │
│                           │                                          │
│               ┌───────────▼───────────┐                              │
│               │ NotificationPushService│                             │
│               │  (WebSocket + Redis)  │                              │
│               └───────────────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
```

## 설정 상세

### Consumer 설정 속성

```yaml
spring:
  kafka:
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest  # 새 그룹일 때 처음부터
      enable-auto-commit: false    # 수동 커밋 모드
```

| 속성 | 값 | 설명 |
|------|-------|------|
| `group-id` | notification-group | Consumer 그룹 식별자 |
| `auto-offset-reset` | earliest | 새 Consumer Group 시작 위치 |
| `enable-auto-commit` | false | 수동 Offset 커밋 활성화 |

### AckMode 설정

```java
factory.getContainerProperties().setAckMode(
    ContainerProperties.AckMode.RECORD  // 레코드별 커밋
);
```

| AckMode | 동작 | 사용 케이스 |
|---------|------|------------|
| `RECORD` | 레코드별 커밋 | 메시지 유실 최소화 |
| `BATCH` | 배치 단위 커밋 | 처리량 우선 |
| `MANUAL` | 명시적 커밋 호출 | 완전한 제어 필요시 |

## 메시지 흐름

```
1. Kafka Broker에서 메시지 수신
   │
2. ErrorHandlingDeserializer로 역직렬화
   │
   ├─ 성공 → 3. @KafkaListener 메서드 호출
   │         │
   │         ├─ 성공 → 4. Offset 커밋
   │         │
   │         └─ 실패 → 5. ErrorHandler 처리
   │                   │
   │                   ├─ 재시도 (최대 3회)
   │                   │
   │                   └─ 실패 → DLQ로 이동
   │
   └─ 실패 → ErrorHandler로 위임 → DLQ
```

## 성능 고려사항

### 1. 병렬 처리

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> factory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();

    // 병렬 Consumer 수 설정
    factory.setConcurrency(3);  // 3개의 스레드로 병렬 처리

    return factory;
}
```

### 2. Batch 처리

```java
// 배치 리스너 설정
factory.setBatchListener(true);

@KafkaListener(topics = "bulk-topic")
public void handleBatch(List<NotificationEvent> events) {
    // 배치 단위 처리
}
```

### 3. 메시지 필터링

```java
factory.setRecordFilterStrategy(record -> {
    // true 반환시 메시지 건너뜀
    return record.value() == null;
});
```

## 모니터링 포인트

```java
// Consumer Lag 모니터링
@Autowired
private KafkaListenerEndpointRegistry registry;

public Map<String, Long> getConsumerLag() {
    // Consumer 지연 정보 수집
}

// 메트릭 노출
// - kafka.consumer.records.consumed.total
// - kafka.consumer.records.lag
// - kafka.consumer.commit.latency.avg
```

## Best Practices

1. **항상 수동 커밋 사용** - 메시지 처리 완료 후 커밋
2. **ErrorHandlingDeserializer 적용** - 잘못된 메시지로 인한 Consumer 중단 방지
3. **적절한 재시도 전략** - 일시적 오류 복구
4. **DLQ 구성** - 처리 실패 메시지 보존
5. **Consumer Lag 모니터링** - 처리 지연 감지

## 관련 문서

- [event-handlers.md](./event-handlers.md) - 이벤트별 핸들러
- [consumer-error-handling.md](./consumer-error-handling.md) - 에러 처리
- [retry-strategy.md](./retry-strategy.md) - 재시도 전략
- [dlq-processing.md](./dlq-processing.md) - DLQ 처리
