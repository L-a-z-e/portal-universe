# Portal Universe Kafka 구현 분석

이 문서는 Portal Universe 프로젝트에서 Kafka가 어떻게 사용되는지 실제 코드를 기반으로 분석합니다.

## 목차

1. [전체 이벤트 흐름 다이어그램](#1-전체-이벤트-흐름-다이어그램)
2. [토픽 설계 분석](#2-토픽-설계-분석)
3. [Producer 구현 분석](#3-producer-구현-분석)
4. [Consumer 구현 분석](#4-consumer-구현-분석)
5. [이벤트 페이로드 구조](#5-이벤트-페이로드-구조)
6. [에러 처리 및 DLQ 설정](#6-에러-처리-및-dlq-설정)
7. [실제 코드 예제](#7-실제-코드-예제)

---

## 1. 전체 이벤트 흐름 다이어그램

Portal Universe에서 Kafka를 통한 서비스 간 이벤트 흐름입니다.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Portal Universe Event Flow                        │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐                                    ┌──────────────────┐
│                  │                                    │                  │
│   Auth Service   │───────────────────────────────────▶│   Notification   │
│                  │     user-signup                    │     Service      │
└──────────────────┘                                    │                  │
                                                        │  ┌────────────┐  │
                                                        │  │ Consumer   │  │
                                                        │  │ Group:     │  │
                                                        │  │notification│  │
                                                        │  │   -group   │  │
                                                        │  └────────────┘  │
                                                        └──────────────────┘
                                                                 ▲
                                                                 │
┌──────────────────┐                                             │
│                  │                                             │
│ Shopping Service │─────────────────────────────────────────────┤
│                  │                                             │
│  ┌────────────┐  │     shopping.order.created                  │
│  │ Shopping   │──┼─────────────────────────────────────────────┤
│  │ Event      │  │     shopping.order.confirmed                │
│  │ Publisher  │──┼─────────────────────────────────────────────┤
│  │            │  │     shopping.order.cancelled                │
│  │            │──┼─────────────────────────────────────────────┤
│  │            │  │     shopping.payment.completed              │
│  │            │──┼─────────────────────────────────────────────┤
│  │            │  │     shopping.payment.failed                 │
│  │            │──┼─────────────────────────────────────────────┤
│  │            │  │     shopping.inventory.reserved             │
│  │            │──┼─────────────────────────────────────────────┤
│  │            │  │     shopping.delivery.shipped               │
│  └────────────┘──┼─────────────────────────────────────────────┘
│                  │
└──────────────────┘


                    ┌─────────────────────────────────────┐
                    │         Apache Kafka (KRaft)        │
                    │                                     │
                    │  ┌───────────────────────────────┐  │
                    │  │           Topics              │  │
                    │  │  ┌─────────────────────────┐  │  │
                    │  │  │ user-signup             │  │  │
                    │  │  │ shopping.order.created  │  │  │
                    │  │  │ shopping.order.confirmed│  │  │
                    │  │  │ shopping.order.cancelled│  │  │
                    │  │  │ shopping.payment.*      │  │  │
                    │  │  │ shopping.inventory.*    │  │  │
                    │  │  │ shopping.delivery.*     │  │  │
                    │  │  └─────────────────────────┘  │  │
                    │  └───────────────────────────────┘  │
                    │                                     │
                    │  ┌───────────────────────────────┐  │
                    │  │    Dead Letter Topics (DLT)   │  │
                    │  │  *.DLT (실패 메시지 저장)      │  │
                    │  └───────────────────────────────┘  │
                    └─────────────────────────────────────┘
```

### 서비스별 역할

| 서비스 | 역할 | Kafka 동작 |
|--------|------|-----------|
| **auth-service** | 인증/회원 관리 | Producer - 회원가입 이벤트 발행 |
| **shopping-service** | 주문/결제/배송 | Producer - 쇼핑 관련 이벤트 발행 |
| **notification-service** | 알림 전송 | Consumer - 이벤트 수신 및 알림 처리 |

---

## 2. 토픽 설계 분석

### 2.1 Naming Convention

Portal Universe는 **도메인 기반 계층적 네이밍 컨벤션**을 사용합니다.

```
{service}.{domain}.{action}
```

| 구성요소 | 설명 | 예시 |
|----------|------|------|
| `service` | 이벤트 발행 서비스 | `shopping`, `auth` |
| `domain` | 비즈니스 도메인 | `order`, `payment`, `delivery` |
| `action` | 발생한 액션 | `created`, `completed`, `cancelled` |

### 2.2 토픽 목록

```java
// services/shopping-service/.../KafkaConfig.java
public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
public static final String TOPIC_INVENTORY_RESERVED = "shopping.inventory.reserved";
public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";
```

| 토픽명 | 발행 시점 | 구독 서비스 |
|--------|----------|------------|
| `user-signup` | 회원가입 완료 | notification-service |
| `shopping.order.created` | 주문 생성 | notification-service |
| `shopping.order.confirmed` | 주문 확정 | notification-service |
| `shopping.order.cancelled` | 주문 취소 | notification-service |
| `shopping.payment.completed` | 결제 완료 | notification-service |
| `shopping.payment.failed` | 결제 실패 | notification-service |
| `shopping.inventory.reserved` | 재고 예약 | (내부 처리) |
| `shopping.delivery.shipped` | 배송 출발 | notification-service |

### 2.3 Partition 설계

```java
@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(TOPIC_ORDER_CREATED)
            .partitions(3)        // 3개 partition
            .replicas(1)          // 1개 replica (개발환경)
            .build();
}
```

**Partition 전략:**
- **Partition 수**: 3개 (병렬 처리를 위한 기본 설정)
- **Replica 수**: 1개 (개발 환경 - 프로덕션에서는 3+ 권장)
- **Key 기반 파티셔닝**: orderNumber, paymentNumber 등으로 같은 주문의 이벤트가 같은 파티션에 저장되어 순서 보장

---

## 3. Producer 구현 분석

### 3.1 KafkaConfig (Producer 설정)

```java
// services/shopping-service/.../KafkaConfig.java

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // 브로커 연결 설정
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 직렬화 설정 (Key: String, Value: JSON)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 메시지 전송 신뢰성 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");           // 모든 replica 확인
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);            // 실패 시 3회 재시도
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 중복 방지

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**Producer 설정 핵심 포인트:**

| 설정 | 값 | 설명 |
|------|-----|------|
| `acks` | `all` | 모든 replica에 쓰여진 후 응답 (가장 안전) |
| `retries` | `3` | 일시적 오류 시 재시도 횟수 |
| `enable.idempotence` | `true` | 중복 메시지 전송 방지 |

### 3.2 ShoppingEventPublisher (이벤트 발행)

```java
// services/shopping-service/.../ShoppingEventPublisher.java

@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(KafkaConfig.TOPIC_ORDER_CREATED, event.orderNumber(), event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    /**
     * 비동기 이벤트 발행 (콜백 패턴)
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

**발행 패턴 특징:**
1. **Key 기반 파티셔닝**: `orderNumber`를 키로 사용하여 같은 주문의 이벤트가 순서대로 처리됨
2. **비동기 발행**: `CompletableFuture`를 사용한 논블로킹 방식
3. **콜백 로깅**: 성공/실패 여부를 비동기로 확인

### 3.3 Auth Service Producer (회원가입 이벤트)

```java
// services/auth-service/.../UserService.java

@Service
@RequiredArgsConstructor
public class UserService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Long registerUser(SignupCommand command) {
        // 1. 사용자 생성 로직
        User savedUser = userRepository.save(newUser);

        // 2. 이벤트 발행
        UserSignedUpEvent event = new UserSignedUpEvent(
                savedUser.getUuid(),
                savedUser.getEmail(),
                savedUser.getProfile().getNickname()
        );
        kafkaTemplate.send("user-signup", event);

        return savedUser.getId();
    }
}
```

**주의사항:**
- 현재 트랜잭션 커밋 전에 이벤트를 발행함
- 프로덕션에서는 `@TransactionalEventListener`를 사용하여 커밋 후 발행 권장

---

## 4. Consumer 구현 분석

### 4.1 KafkaConsumerConfig (Consumer 설정)

```java
// services/notification-service/.../KafkaConsumerConfig.java

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final long RETRY_INTERVAL_MS = 1000L;
    private static final long MAX_RETRY_ATTEMPTS = 3L;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 브로커 연결
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // ErrorHandlingDeserializer로 역직렬화 에러 안전 처리
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JSON 역직렬화 - 신뢰 패키지 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Offset 관리
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 수동 커밋

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory(/* ... */) {

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        // 수동 커밋: 각 레코드 처리 완료 후 커밋
        factory.getContainerProperties().setAckMode(AckMode.RECORD);

        return factory;
    }
}
```

**Consumer 설정 핵심 포인트:**

| 설정 | 값 | 설명 |
|------|-----|------|
| `group-id` | `notification-group` | Consumer Group 식별자 |
| `auto.offset.reset` | `earliest` | 새 그룹은 처음부터 읽음 |
| `enable.auto.commit` | `false` | 수동 커밋 (처리 완료 후) |
| `ack-mode` | `RECORD` | 각 메시지 처리 후 커밋 |

### 4.2 NotificationConsumer (@KafkaListener)

```java
// services/notification-service/.../NotificationConsumer.java

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "user-signup", groupId = "notification-group")
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: {}", event);
        log.info("Sending welcome email to: {} ({})", event.name(), event.email());
    }

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
     * 알림 생성 및 푸시
     *
     * 중요: 예외를 삼키지 않음
     * - 예외 발생 시 ErrorHandler가 재시도 후 DLQ로 이동
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

        log.info("Notification created and pushed: userId={}, type={}, notificationId={}",
                event.getUserId(), event.getType(), notification.getId());
    }
}
```

**Consumer 패턴 특징:**
1. **토픽별 리스너**: 각 토픽에 대해 별도 메서드로 처리
2. **예외 전파**: try-catch로 삼키지 않고 ErrorHandler에 위임
3. **로깅**: 처리 시작/완료 로그로 추적 가능

---

## 5. 이벤트 페이로드 구조

Portal Universe는 Java `record`를 사용하여 불변 이벤트 객체를 정의합니다.

### 5.1 회원 관련 이벤트

```java
// services/common-library/.../UserSignedUpEvent.java

public record UserSignedUpEvent(
    String userId,    // UUID
    String email,
    String name
) {}
```

### 5.2 주문 관련 이벤트

```java
// services/common-library/.../OrderCreatedEvent.java

public record OrderCreatedEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt
) {
    public record OrderItemInfo(
        Long productId,
        String productName,
        int quantity,
        BigDecimal price
    ) {}
}

// OrderConfirmedEvent.java
public record OrderConfirmedEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    String paymentNumber,
    LocalDateTime confirmedAt
) {}

// OrderCancelledEvent.java
public record OrderCancelledEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    String cancelReason,
    LocalDateTime cancelledAt
) {}
```

### 5.3 결제 관련 이벤트

```java
// services/common-library/.../PaymentCompletedEvent.java

public record PaymentCompletedEvent(
    String paymentNumber,
    String orderNumber,
    String userId,
    BigDecimal amount,
    String paymentMethod,
    String pgTransactionId,
    LocalDateTime paidAt
) {}

// PaymentFailedEvent.java
public record PaymentFailedEvent(
    String paymentNumber,
    String orderNumber,
    String userId,
    BigDecimal amount,
    String paymentMethod,
    String failureReason,
    LocalDateTime failedAt
) {}
```

### 5.4 재고/배송 관련 이벤트

```java
// InventoryReservedEvent.java
public record InventoryReservedEvent(
    String orderNumber,
    String userId,
    Map<Long, Integer> reservedQuantities,  // productId -> quantity
    LocalDateTime reservedAt
) {}

// DeliveryShippedEvent.java
public record DeliveryShippedEvent(
    String trackingNumber,
    String orderNumber,
    String userId,
    String carrier,
    LocalDate estimatedDeliveryDate,
    LocalDateTime shippedAt
) {}
```

### 5.5 알림 이벤트 (Consumer 측)

```java
// services/notification-service/.../NotificationEvent.java

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String referenceId;
    private String referenceType;
}
```

### 5.6 이벤트 페이로드 설계 원칙

| 원칙 | 설명 |
|------|------|
| **불변성** | Java `record` 사용으로 불변 객체 보장 |
| **자기 완결적** | 이벤트만으로 처리에 필요한 모든 정보 포함 |
| **버전 독립적** | 필드 추가는 backward compatible |
| **타임스탬프** | 모든 이벤트에 발생 시각 포함 |

---

## 6. 에러 처리 및 DLQ 설정

### 6.1 Error Handler 구성

```java
// services/notification-service/.../KafkaConsumerConfig.java

@Bean
public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

    // 1. Dead Letter Publishing Recoverer
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                String dlqTopic = record.topic() + ".DLT";
                log.error("Message sent to DLQ: topic={}, key={}, error={}",
                        dlqTopic, record.key(), ex.getMessage());
                return new TopicPartition(dlqTopic, record.partition());
            }
    );

    // 2. 재시도 정책 (1초 간격, 최대 3회)
    FixedBackOff backOff = new FixedBackOff(1000L, 3L);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 3. 재시도하지 않을 예외 (바로 DLQ로)
    errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            NullPointerException.class
    );

    // 4. 재시도 시 로깅
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
        log.warn("Retry attempt {} for topic={}, key={}, error={}",
                deliveryAttempt, record.topic(), record.key(), ex.getMessage());
    });

    return errorHandler;
}
```

### 6.2 에러 처리 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Error Handling Flow                              │
└─────────────────────────────────────────────────────────────────────┘

   메시지 수신
       │
       ▼
┌─────────────────┐
│ @KafkaListener  │
│   처리 시도     │
└────────┬────────┘
         │
         ▼
    ┌─────────┐
    │ 성공?   │──Yes──▶ Offset Commit ✓
    └────┬────┘
         │No (예외 발생)
         ▼
    ┌─────────────────┐
    │ 재시도 가능한   │
    │ 예외인가?       │
    └────────┬────────┘
             │
     ┌───────┴───────┐
     │Yes            │No (IllegalArgumentException 등)
     ▼               ▼
┌─────────────┐  ┌─────────────┐
│ 재시도      │  │ 바로 DLQ로  │
│ (최대 3회)  │  │ 이동        │
└──────┬──────┘  └─────────────┘
       │
       ▼
   ┌────────┐
   │ 성공?  │──Yes──▶ Offset Commit ✓
   └───┬────┘
       │No (3회 실패)
       ▼
┌─────────────────────────────┐
│ Dead Letter Topic (.DLT)    │
│ 예: shopping.order.created.DLT │
└─────────────────────────────┘
```

### 6.3 DLQ Topic Naming

```
{원본토픽명}.DLT
```

| 원본 토픽 | DLQ 토픽 |
|----------|---------|
| `shopping.order.created` | `shopping.order.created.DLT` |
| `shopping.payment.completed` | `shopping.payment.completed.DLT` |
| `user-signup` | `user-signup.DLT` |

### 6.4 재시도 정책

| 설정 | 값 | 설명 |
|------|-----|------|
| 재시도 간격 | 1000ms | 고정 간격 (FixedBackOff) |
| 최대 재시도 | 3회 | 총 4번 시도 (1번 + 3회 재시도) |
| 재시도 제외 예외 | `IllegalArgumentException`, `NullPointerException` | 비즈니스 로직 오류는 재시도 무의미 |

---

## 7. 실제 코드 예제

### 7.1 주문 생성 시 이벤트 발행 예제

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        // 1. 주문 생성
        Order order = Order.create(request, userId);
        Order savedOrder = orderRepository.save(order);

        // 2. 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
            savedOrder.getOrderNumber(),
            userId.toString(),
            savedOrder.getTotalAmount(),
            savedOrder.getItems().size(),
            convertToItemInfos(savedOrder.getItems()),
            savedOrder.getCreatedAt()
        );

        eventPublisher.publishOrderCreated(event);

        return OrderResponse.from(savedOrder);
    }
}
```

### 7.2 결제 완료 시 알림 처리 예제

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationHandler {

    private final NotificationService notificationService;

    @KafkaListener(topics = "shopping.payment.completed", groupId = "notification-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신: orderNumber={}, amount={}",
                event.orderNumber(), event.amount());

        // 알림 생성
        notificationService.create(
            Long.parseLong(event.userId()),
            NotificationType.PAYMENT,
            "결제가 완료되었습니다",
            String.format("주문번호 %s, 결제금액 %s원",
                event.orderNumber(), event.amount()),
            "/orders/" + event.orderNumber(),
            event.paymentNumber(),
            "PAYMENT"
        );
    }
}
```

### 7.3 테스트 코드 예제

```java
// integration-tests/.../OrderEventTest.java

@Test
@DisplayName("주문 생성 시 Kafka 이벤트 발행 테스트")
void testOrderCreatedEventPublished() {
    try (KafkaConsumer<String, String> consumer = createKafkaConsumer("shopping.order.created")) {

        // When: 주문 생성
        Response orderResponse = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/shopping/orders");

        String orderNumber = orderResponse.jsonPath().getString("data.orderNumber");

        // Then: 이벤트 확인
        Optional<ConsumerRecord<String, String>> record =
            consumeMessage(consumer, "shopping.order.created", Duration.ofSeconds(15));

        assertThat(record).isPresent();

        JsonNode event = objectMapper.readTree(record.get().value());
        assertThat(event.get("orderNumber").asText()).isEqualTo(orderNumber);
    }
}
```

---

## 인프라 설정

### Docker Compose (개발 환경)

```yaml
# docker-compose.yml
kafka:
  image: apache/kafka:4.1.0
  container_name: kafka
  ports:
    - "9092:9092"
    - "29092:29092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller  # KRaft 모드
    KAFKA_LISTENERS: PLAINTEXT://:29092,PLAINTEXT_HOST://:9092,CONTROLLER://:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    # 단일 노드 설정
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
```

### Kubernetes (K8s 환경)

```yaml
# k8s/infrastructure/kafka.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: portal-universe
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: kafka
          image: apache/kafka:4.1.0
          ports:
            - containerPort: 9092   # 외부 통신
            - containerPort: 29092  # 내부 통신
            - containerPort: 9093   # 컨트롤러
          env:
            - name: KAFKA_PROCESS_ROLES
              value: "broker,controller"  # KRaft 모드
```

---

## 주요 참고 사항

### 1. KRaft 모드
- Portal Universe는 **Zookeeper 없는 KRaft 모드** 사용
- 단일 노드가 broker + controller 역할 동시 수행

### 2. 메시지 신뢰성
- Producer: `acks=all`, `enable.idempotence=true`
- Consumer: 수동 커밋 (`enable.auto.commit=false`)

### 3. 순서 보장
- 같은 Key(orderNumber)를 가진 메시지는 같은 partition에 저장
- 단일 partition 내에서 순서 보장

### 4. 에러 복구
- 3회 재시도 후 DLQ로 이동
- DLQ 메시지는 별도 모니터링 및 재처리 필요

---

## 관련 문서

- [Kafka 기본 개념](/docs/learning/kafka/kafka-introduction.md)
- [Spring Kafka 통합](/docs/learning/kafka/kafka-spring-integration.md)
- [주문 흐름 설계](/services/shopping-service/docs/learning/business/order-flow.md)
