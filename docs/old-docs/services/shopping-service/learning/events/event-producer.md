# Event Producer - Kafka 이벤트 발행

## 개요

이 문서에서는 KafkaTemplate을 사용하여 이벤트를 발행하는 패턴과 Shopping Service의 실제 구현을 설명합니다.

## KafkaTemplate 설정

### Producer Configuration

```java
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka 브로커 주소
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key/Value Serializer
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 메시지 전송 신뢰성 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");       // 모든 ISR 복제 완료 대기
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);        // 재시도 횟수
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 중복 전송 방지

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 설정 옵션 상세

| 설정 | 값 | 설명 |
|------|-----|------|
| `acks` | `all` | 모든 In-Sync Replica가 메시지를 받을 때까지 대기 |
| `retries` | `3` | 전송 실패 시 재시도 횟수 |
| `enable.idempotence` | `true` | 동일 메시지 중복 전송 방지 |

## Topic 정의

토픽명은 상수로 정의하여 일관성을 유지합니다:

```java
public class KafkaConfig {
    // Topic Names
    public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
    public static final String TOPIC_INVENTORY_RESERVED = "shopping.inventory.reserved";
    public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
}
```

### Topic 자동 생성

```java
@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(TOPIC_ORDER_CREATED)
            .partitions(3)      // 파티션 수 (병렬 처리 단위)
            .replicas(1)        // 복제본 수 (운영 환경에서는 3 권장)
            .build();
}
```

## ShoppingEventPublisher 구현

Shopping Service의 이벤트 발행을 담당하는 컴포넌트입니다:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 생성 이벤트를 발행합니다.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);
    }

    /**
     * 결제 완료 이벤트를 발행합니다.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    /**
     * 결제 실패 이벤트를 발행합니다.
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent(KafkaConfig.TOPIC_PAYMENT_FAILED, event.paymentNumber(), event);
    }

    /**
     * 재고 예약 이벤트를 발행합니다.
     */
    public void publishInventoryReserved(InventoryReservedEvent event) {
        publishEvent(KafkaConfig.TOPIC_INVENTORY_RESERVED, event.orderNumber(), event);
    }

    /**
     * 배송 발송 이벤트를 발행합니다.
     */
    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        publishEvent(KafkaConfig.TOPIC_DELIVERY_SHIPPED, event.trackingNumber(), event);
    }

    /**
     * 이벤트를 Kafka로 발행합니다.
     *
     * @param topic 토픽명
     * @param key 메시지 키 (파티션 결정에 사용)
     * @param event 이벤트 객체
     */
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
}
```

## 이벤트 발행 패턴

### 1. 비동기 발행 (Fire-and-Forget)

대부분의 경우 사용하는 패턴입니다:

```java
public void createOrder(CreateOrderRequest request) {
    Order order = orderRepository.save(createOrderEntity(request));

    // 비동기로 이벤트 발행 (결과를 기다리지 않음)
    eventPublisher.publishOrderCreated(OrderCreatedEvent.from(order));

    return OrderResponse.from(order);
}
```

### 2. 동기 발행 (Blocking)

이벤트 발행 성공을 보장해야 할 때 사용합니다:

```java
public void createOrderWithGuarantee(CreateOrderRequest request) {
    Order order = orderRepository.save(createOrderEntity(request));

    try {
        // 발행 완료까지 대기
        kafkaTemplate.send(TOPIC_ORDER_CREATED, order.getOrderNumber(), event)
            .get(5, TimeUnit.SECONDS);  // 5초 타임아웃
    } catch (Exception e) {
        log.error("Failed to publish event", e);
        throw new EventPublishException("이벤트 발행 실패", e);
    }

    return OrderResponse.from(order);
}
```

### 3. Transactional Outbox Pattern

DB 트랜잭션과 이벤트 발행의 원자성을 보장합니다:

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(createOrderEntity(request));

        // 이벤트를 Outbox 테이블에 저장 (같은 트랜잭션)
        OutboxEvent outbox = OutboxEvent.builder()
            .aggregateId(order.getOrderNumber())
            .aggregateType("Order")
            .eventType("OrderCreated")
            .payload(objectMapper.writeValueAsString(OrderCreatedEvent.from(order)))
            .build();
        outboxRepository.save(outbox);

        return OrderResponse.from(order);
    }
}

// 별도 스케줄러가 Outbox 테이블을 폴링하여 Kafka로 발행
@Scheduled(fixedDelay = 1000)
public void publishOutboxEvents() {
    List<OutboxEvent> events = outboxRepository.findUnpublished();
    for (OutboxEvent event : events) {
        kafkaTemplate.send(event.getTopicName(), event.getAggregateId(), event.getPayload());
        event.markAsPublished();
        outboxRepository.save(event);
    }
}
```

## Message Key 전략

메시지 키는 파티션 할당에 사용됩니다. 동일한 키를 가진 메시지는 동일한 파티션으로 전송되어 순서가 보장됩니다.

```java
// 주문 관련 이벤트 - orderNumber를 키로 사용
publishEvent(TOPIC_ORDER_CREATED, event.orderNumber(), event);

// 결제 관련 이벤트 - paymentNumber를 키로 사용
publishEvent(TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);

// 배송 관련 이벤트 - trackingNumber를 키로 사용
publishEvent(TOPIC_DELIVERY_SHIPPED, event.trackingNumber(), event);
```

### 키 선택 기준

| 이벤트 유형 | 키 | 이유 |
|------------|-----|------|
| 주문 관련 | orderNumber | 동일 주문의 이벤트 순서 보장 |
| 결제 관련 | paymentNumber | 동일 결제의 이벤트 순서 보장 |
| 사용자 관련 | userId | 동일 사용자의 이벤트 순서 보장 |

## 에러 처리

### 발행 실패 처리

```java
private void publishEvent(String topic, String key, Object event) {
    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(topic, key, event);

    future.whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Event published: topic={}, offset={}",
                topic, result.getRecordMetadata().offset());
        } else {
            // 실패 로깅 및 모니터링 알림
            log.error("Event publish failed: topic={}, key={}", topic, key, ex);

            // 선택적: 재시도 큐에 저장
            retryQueue.add(new RetryableEvent(topic, key, event));
        }
    });
}
```

### 콜백 기반 처리

```java
public void publishWithCallback(String topic, String key, Object event) {
    ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);

    kafkaTemplate.send(record).addCallback(
        result -> log.info("Success: offset={}", result.getRecordMetadata().offset()),
        ex -> log.error("Failed: {}", ex.getMessage())
    );
}
```

## 모니터링

### 발행 메트릭

```java
@Component
@RequiredArgsConstructor
public class MetricAwareEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public void publishEvent(String topic, String key, Object event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                sample.stop(Timer.builder("kafka.producer.send")
                    .tag("topic", topic)
                    .tag("status", ex == null ? "success" : "failure")
                    .register(meterRegistry));

                if (ex != null) {
                    meterRegistry.counter("kafka.producer.errors", "topic", topic).increment();
                }
            });
    }
}
```

## Best Practices

1. **메시지 키 일관성**: 관련 이벤트들은 동일한 키를 사용하여 순서 보장
2. **Idempotent Producer 활성화**: 중복 메시지 방지
3. **적절한 재시도 설정**: 일시적 실패에 대응
4. **비동기 처리**: 성능을 위해 기본적으로 비동기 발행 사용
5. **로깅과 모니터링**: 발행 성공/실패 추적

## 관련 문서

- [Shopping Events](./shopping-events.md) - 이벤트 정의
- [Event Consumer](./event-consumer.md) - 이벤트 소비
- [DLQ Handling](./dlq-handling.md) - 실패 이벤트 처리
