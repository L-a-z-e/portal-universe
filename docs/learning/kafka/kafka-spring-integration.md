# Kafka Spring 통합

## 학습 목표
- Spring Kafka의 핵심 컴포넌트 이해
- KafkaTemplate과 @KafkaListener 활용법 습득
- Portal Universe의 실제 구현 패턴 학습

---

## 1. 의존성 설정

### build.gradle

```groovy
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
}
```

Spring Boot 3.x에서는 별도 버전 명시 없이 BOM으로 관리됩니다.

---

## 2. Producer 설정

### 2.1 KafkaConfig.java (Shopping Service)

```java
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 브로커 연결
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 직렬화 설정
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  JsonSerializer.class);

        // 신뢰성 설정
        props.put(ProducerConfig.ACKS_CONFIG, "all");        // 모든 복제본 확인
        props.put(ProducerConfig.RETRIES_CONFIG, 3);         // 재시도 횟수
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 멱등성

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 2.2 설정 옵션 상세

| 설정 | 값 | 설명 |
|------|-----|------|
| `ACKS_CONFIG` | `"all"` | 모든 ISR 복제본이 확인해야 성공 |
| `RETRIES_CONFIG` | `3` | 실패 시 최대 3회 재시도 |
| `ENABLE_IDEMPOTENCE_CONFIG` | `true` | 중복 메시지 방지 |

### 2.3 Topic 자동 생성

```java
@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name("shopping.order.created")
            .partitions(3)    // 병렬 처리를 위한 3개 파티션
            .replicas(1)      // 개발 환경 - 1개 복제본
            .build();
}

@Bean
public NewTopic paymentCompletedTopic() {
    return TopicBuilder.name("shopping.payment.completed")
            .partitions(3)
            .replicas(1)
            .build();
}
```

---

## 3. 이벤트 발행 (Producer)

### 3.1 이벤트 클래스 정의

```java
// 주문 생성 이벤트
public record OrderCreatedEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt
) {}

// 결제 완료 이벤트
public record PaymentCompletedEvent(
    String paymentNumber,
    String orderNumber,
    String userId,
    BigDecimal amount,
    String paymentMethod,
    String pgTransactionId,
    LocalDateTime paidAt
) {}
```

### 3.2 ShoppingEventPublisher 구현

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 토픽명 상수 정의
    private static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    private static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";

    /**
     * 주문 생성 이벤트 발행
     * Key: orderNumber (같은 주문은 같은 파티션으로)
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        publish(TOPIC_ORDER_CREATED, event.orderNumber(), event);
    }

    /**
     * 결제 완료 이벤트 발행
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publish(TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    /**
     * 공통 발행 메서드 - 비동기 콜백 처리
     */
    private void publish(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, key={}, offset={}",
                         topic, key,
                         result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish: topic={}, key={}, error={}",
                          topic, key, ex.getMessage());
                // 실패 처리 로직 (재시도, 알림 등)
            }
        });
    }
}
```

### 3.3 서비스에서 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문 생성
        Order order = Order.create(request);
        Order savedOrder = orderRepository.save(order);

        // 2. 이벤트 발행 (트랜잭션 외부에서 발행 권장)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderNumber(savedOrder.getOrderNumber())
            .userId(savedOrder.getUserId())
            .totalAmount(savedOrder.getTotalAmount())
            .itemCount(savedOrder.getItems().size())
            .items(toOrderItemInfoList(savedOrder.getItems()))
            .createdAt(savedOrder.getCreatedAt())
            .build();

        eventPublisher.publishOrderCreated(event);

        return OrderResponse.from(savedOrder);
    }
}
```

---

## 4. Consumer 설정

### 4.1 KafkaConsumerConfig.java (Notification Service)

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 브로커 연결
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");

        // 역직렬화 (에러 핸들링 포함)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                  JsonDeserializer.class.getName());

        // JSON 역직렬화 신뢰 패키지
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Offset 관리
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
           kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 3개 스레드로 병렬 처리

        // 수동 Offset 커밋
        factory.getContainerProperties()
               .setAckMode(ContainerProperties.AckMode.RECORD);

        // 에러 핸들러 + 재시도 설정
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }
}
```

### 4.2 재시도 및 DLQ 설정

```java
@Bean
public DefaultErrorHandler errorHandler() {
    // 재시도 전략: 1초 간격, 최대 3회
    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);

    // Dead Letter Topic으로 실패 메시지 전송
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(kafkaTemplate(),
            (record, ex) -> new TopicPartition(
                record.topic() + ".DLT",  // 원본 토픽명 + .DLT
                record.partition()
            ));

    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(recoverer, fixedBackOff);

    // 재시도하지 않을 예외 지정
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class,
        NullPointerException.class
    );

    return errorHandler;
}
```

**재시도 흐름:**
```
메시지 처리 시도
    ↓ (실패)
재시도 1 (1초 대기)
    ↓ (실패)
재시도 2 (1초 대기)
    ↓ (실패)
재시도 3 (1초 대기)
    ↓ (실패)
DLQ로 전송 (shopping.order.created.DLT)
```

---

## 5. 이벤트 소비 (@KafkaListener)

### 5.1 NotificationConsumer 구현

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    /**
     * 주문 생성 이벤트 처리
     */
    @KafkaListener(
        topics = "shopping.order.created",
        groupId = "notification-group"
    )
    public void handleOrderCreated(NotificationEvent event) {
        log.info("Received order created event: userId={}, orderId={}",
                 event.getUserId(), event.getReferenceId());

        notificationService.createNotification(
            event.getUserId(),
            event.getType(),
            event.getTitle(),
            event.getMessage(),
            event.getLink()
        );
    }

    /**
     * 결제 완료 이벤트 처리
     */
    @KafkaListener(
        topics = "shopping.payment.completed",
        groupId = "notification-group"
    )
    public void handlePaymentCompleted(NotificationEvent event) {
        log.info("Received payment completed event: userId={}",
                 event.getUserId());

        notificationService.createAndPushNotification(
            event.getUserId(),
            event.getType(),
            event.getTitle(),
            event.getMessage(),
            event.getLink()
        );
    }

    /**
     * 회원가입 이벤트 처리
     */
    @KafkaListener(
        topics = "user-signup",
        groupId = "notification-group"
    )
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: email={}", event.email());

        // 환영 알림 생성
        notificationService.createWelcomeNotification(
            event.userId(),
            event.name()
        );
    }
}
```

### 5.2 @KafkaListener 옵션

```java
@KafkaListener(
    topics = "shopping.order.created",
    groupId = "notification-group",
    concurrency = "3",                    // 병렬 처리 스레드 수
    containerFactory = "kafkaListenerContainerFactory"
)
public void handle(
    @Payload NotificationEvent event,    // 메시지 본문
    @Header(KafkaHeaders.OFFSET) long offset,      // 오프셋
    @Header(KafkaHeaders.PARTITION) int partition, // 파티션
    Acknowledgment ack                   // 수동 커밋용
) {
    try {
        processEvent(event);
        ack.acknowledge();  // 성공 시 커밋
    } catch (Exception e) {
        // 예외 발생 시 재시도 (ack 호출 안 함)
        throw e;
    }
}
```

---

## 6. 이벤트 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                        Shopping Service                          │
├─────────────────────────────────────────────────────────────────┤
│  OrderService                                                    │
│       │                                                          │
│       ▼                                                          │
│  ShoppingEventPublisher                                          │
│       │                                                          │
│       ▼                                                          │
│  KafkaTemplate.send("shopping.order.created", key, event)       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Topic: shopping.order.created                            │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐                     │  │
│  │  │ Part 0  │ │ Part 1  │ │ Part 2  │                     │  │
│  │  └─────────┘ └─────────┘ └─────────┘                     │  │
│  └───────────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Notification Service                          │
├─────────────────────────────────────────────────────────────────┤
│  @KafkaListener(topics = "shopping.order.created")              │
│       │                                                          │
│       ▼                                                          │
│  NotificationConsumer.handleOrderCreated()                       │
│       │                                                          │
│       ▼                                                          │
│  NotificationService.createNotification()                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. 환경별 설정

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      enable-auto-commit: false
```

### Docker 환경

```yaml
# docker-compose.yml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

---

## 8. 테스트 전략

### 8.1 EmbeddedKafka 사용

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldPublishAndConsumeMessage() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(...);

        // When
        kafkaTemplate.send("test-topic", "key", event);

        // Then
        // Consumer에서 메시지 수신 확인
    }
}
```

---

## 9. 모범 사례

### 9.1 Key 설계

```java
// 같은 주문은 항상 같은 파티션으로 (순서 보장)
kafkaTemplate.send("order-topic", order.getOrderNumber(), event);

// 같은 사용자의 이벤트는 같은 파티션으로
kafkaTemplate.send("user-topic", user.getUserId(), event);
```

### 9.2 에러 처리

```java
// 비즈니스 예외는 DLQ로
// 일시적 장애는 재시도
errorHandler.addNotRetryableExceptions(
    BusinessException.class,      // 재시도 불필요
    ValidationException.class     // 재시도 불필요
);
// 나머지는 자동 재시도 → DLQ
```

### 9.3 멱등성 보장

```java
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 이미 처리된 이벤트인지 확인
    if (notificationRepository.existsByReferenceId(event.orderNumber())) {
        log.info("Duplicate event ignored: {}", event.orderNumber());
        return;
    }

    // 처리 로직
    createNotification(event);
}
```

---

## 10. 핵심 정리

| 컴포넌트 | 역할 |
|----------|------|
| `KafkaTemplate` | 메시지 발행 |
| `@KafkaListener` | 메시지 소비 |
| `ProducerFactory` | Producer 설정 |
| `ConsumerFactory` | Consumer 설정 |
| `DefaultErrorHandler` | 에러 처리 + 재시도 |
| `DeadLetterPublishingRecoverer` | DLQ 전송 |

---

## 다음 학습

- [Kafka 에러 처리 심화](./kafka-error-handling.md)
- [Kafka Exactly-Once](./kafka-exactly-once.md)
- [Portal Universe Kafka 적용](./kafka-portal-universe.md)
