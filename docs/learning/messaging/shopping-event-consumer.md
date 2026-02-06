# Event Consumer - Kafka 이벤트 소비

## 개요

이 문서에서는 @KafkaListener를 사용하여 이벤트를 소비하는 패턴과 Portal Universe의 실제 구현을 설명합니다.

## Consumer Configuration

### KafkaConsumerConfig

```java
@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Kafka 브로커 연결 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // ErrorHandlingDeserializer로 감싸서 역직렬화 에러 처리
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // 실제 역직렬화기 지정
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JSON 역직렬화 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Offset 관리 설정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        // 수동 커밋 모드: 각 레코드 처리 후 커밋
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.RECORD
        );

        return factory;
    }
}
```

### 설정 옵션 상세

| 설정 | 값 | 설명 |
|------|-----|------|
| `auto.offset.reset` | `earliest` | 새 Consumer Group일 때 처음부터 읽기 |
| `enable.auto.commit` | `false` | 수동 offset 커밋 사용 |
| `trusted.packages` | `com.portal.universe.*` | JSON 역직렬화 허용 패키지 |

### AckMode 옵션

| Mode | 설명 | 사용 케이스 |
|------|------|-----------|
| `RECORD` | 각 레코드 처리 후 커밋 | 개별 메시지 신뢰성 필요 |
| `BATCH` | 배치 처리 후 커밋 | 높은 처리량 필요 |
| `MANUAL` | 명시적 ack 호출 필요 | 세밀한 제어 필요 |

## @KafkaListener 기본 사용법

### 단순 리스너

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: {}", event.orderNumber());
        notificationService.sendOrderConfirmation(event);
    }
}
```

### 메타데이터 접근

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(
        OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp
) {
    log.info("Received message: topic={}, partition={}, offset={}, key={}",
            topic, partition, offset, key);
    // 처리 로직
}
```

### ConsumerRecord 직접 사용

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
    log.info("Received: key={}, value={}, partition={}, offset={}",
            record.key(), record.value(), record.partition(), record.offset());

    OrderCreatedEvent event = record.value();
    // 처리 로직
}
```

## 실제 구현 예시

### NotificationConsumer

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.delivery.shipped", groupId = "notification-group")
    public void handleDeliveryShipped(NotificationEvent event) {
        log.info("Received delivery shipped event: userId={}", event.getUserId());
        createAndPush(event);
    }

    @KafkaListener(topics = "shopping.payment.completed", groupId = "notification-group")
    public void handlePaymentCompleted(NotificationEvent event) {
        log.info("Received payment completed event: userId={}", event.getUserId());
        createAndPush(event);
    }

    /**
     * 알림을 생성하고 푸시합니다.
     *
     * 중요: try-catch로 예외를 삼키지 않습니다.
     * 예외가 발생하면 ErrorHandler가:
     * 1. 설정된 횟수만큼 재시도
     * 2. 재시도 실패 시 DLQ로 이동
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

        log.info("Notification created: userId={}, type={}",
                event.getUserId(), event.getType());
    }
}
```

## 에러 처리 패턴

### 1. 예외 전파 (권장)

예외를 잡지 않고 ErrorHandler에 위임합니다:

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 예외가 발생하면 ErrorHandler가 처리
    // - 재시도 후 DLQ로 이동
    notificationService.sendOrderConfirmation(event);
}
```

### 2. 선택적 예외 처리

특정 예외만 처리하고 나머지는 전파:

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        notificationService.sendOrderConfirmation(event);
    } catch (DuplicateNotificationException e) {
        // 중복은 무시 (멱등성)
        log.info("Duplicate notification ignored: {}", event.orderNumber());
    }
    // 다른 예외는 ErrorHandler로 전파
}
```

### 3. 수동 Acknowledgement

세밀한 제어가 필요할 때:

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(
        OrderCreatedEvent event,
        Acknowledgment ack
) {
    try {
        notificationService.sendOrderConfirmation(event);
        ack.acknowledge();  // 성공 시 수동 커밋
    } catch (Exception e) {
        // 커밋하지 않으면 재처리됨
        log.error("Failed to process event", e);
        throw e;
    }
}
```

## Concurrency 설정

### 병렬 처리

```java
@KafkaListener(
    topics = "shopping.order.created",
    groupId = "notification-group",
    concurrency = "3"  // 3개의 Consumer 스레드
)
public void handleOrderCreated(OrderCreatedEvent event) {
    // 병렬 처리
}
```

### Container Factory에서 설정

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(...) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConcurrency(3);  // 기본 동시성 설정
    return factory;
}
```

## 배치 처리

대량의 메시지를 효율적으로 처리:

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> batchFactory(...) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setBatchListener(true);  // 배치 모드 활성화
    return factory;
}

@KafkaListener(
    topics = "shopping.order.created",
    groupId = "batch-group",
    containerFactory = "batchFactory"
)
public void handleOrderCreatedBatch(List<OrderCreatedEvent> events) {
    log.info("Received {} events", events.size());
    events.forEach(event -> {
        // 배치 처리
    });
}
```

## 필터링

특정 조건의 메시지만 처리:

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> filteringFactory(...) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    // VIP 사용자 이벤트만 처리
    factory.setRecordFilterStrategy(record -> {
        OrderCreatedEvent event = (OrderCreatedEvent) record.value();
        return !vipUserService.isVip(event.userId());  // true면 필터링(제외)
    });

    return factory;
}
```

## 멱등성 보장

동일한 메시지가 중복 처리되어도 안전하게:

```java
@Service
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        String eventId = String.format("%d-%d", partition, offset);

        // 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event ignored: {}", eventId);
            return;
        }

        // 처리 로직
        notificationService.sendOrderConfirmation(event);

        // 처리 완료 기록
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
    }
}
```

## 모니터링

### Consumer Lag 모니터링

```java
@Component
@RequiredArgsConstructor
public class ConsumerLagMonitor {

    private final KafkaAdmin kafkaAdmin;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)
    public void monitorLag() {
        // Consumer Lag 측정 및 메트릭 기록
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // Lag 계산 로직
        }
    }
}
```

### 처리 시간 메트릭

```java
@KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
public void handleOrderCreated(OrderCreatedEvent event) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
        notificationService.sendOrderConfirmation(event);
    } finally {
        sample.stop(Timer.builder("kafka.consumer.process")
                .tag("topic", "shopping.order.created")
                .register(meterRegistry));
    }
}
```

## Best Practices

1. **예외는 ErrorHandler에 위임**: try-catch로 삼키지 않기
2. **멱등성 보장**: 중복 처리에 안전한 로직 구현
3. **적절한 동시성**: 파티션 수와 Consumer 수 조정
4. **수동 커밋 사용**: 처리 완료 후 커밋으로 데이터 손실 방지
5. **로깅과 모니터링**: Consumer Lag 및 처리 시간 추적

## 관련 문서

- [Shopping Events](./shopping-events.md) - 이벤트 정의
- [Event Producer](./event-producer.md) - 이벤트 발행
- [DLQ Handling](./dlq-handling.md) - 실패 이벤트 처리
