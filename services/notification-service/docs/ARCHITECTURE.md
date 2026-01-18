# Notification Service 아키텍처

## 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                         Kafka                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │ user-signup  │ │order-created │ │delivery-status│        │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘        │
└─────────┼────────────────┼────────────────┼─────────────────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
                           ▼
          ┌────────────────────────────────┐
          │      Notification Service       │
          │  ┌──────────────────────────┐  │
          │  │   NotificationConsumer    │  │
          │  │   @KafkaListener          │  │
          │  └─────────────┬────────────┘  │
          │                │               │
          │    ┌───────────┴───────────┐  │
          │    ▼           ▼           ▼  │
          │ ┌──────┐  ┌──────┐  ┌──────┐ │
          │ │Email │  │ SMS  │  │ Push │ │
          │ │Sender│  │Sender│  │Sender│ │
          │ └──────┘  └──────┘  └──────┘ │
          └────────────────────────────────┘
```

## Kafka Consumer 설정

```java
@Service
public class NotificationConsumer {

    @KafkaListener(
        topics = "user-signup",
        groupId = "notification-group"
    )
    public void handleUserSignup(UserSignedUpEvent event) {
        log.info("Received user signup event: {}", event);

        // 환영 이메일 발송
        emailSender.sendWelcomeEmail(event.email(), event.name());
    }

    @KafkaListener(
        topics = "order-created",
        groupId = "notification-group"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 주문 확인 이메일/SMS 발송
        emailSender.sendOrderConfirmation(event);
        smsSender.sendOrderConfirmation(event.userPhone(), event.orderNumber());
    }
}
```

## 이벤트 DTO

### UserSignedUpEvent

```java
public record UserSignedUpEvent(
    String userId,
    String email,
    String name,
    LocalDateTime signedUpAt
) {}
```

### OrderCreatedEvent

```java
public record OrderCreatedEvent(
    String orderId,
    String orderNumber,
    String userId,
    String userEmail,
    String userPhone,
    BigDecimal totalAmount,
    List<OrderItemEvent> items,
    LocalDateTime createdAt
) {}
```

## 이메일 발송

### 템플릿 기반 발송

```java
@Service
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendWelcomeEmail(String to, String name) {
        Context context = new Context();
        context.setVariable("name", name);

        String html = templateEngine.process("welcome", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Welcome to Portal Universe!");
        helper.setText(html, true);

        mailSender.send(message);
    }
}
```

### 이메일 템플릿

```
templates/
├── welcome.html           # 환영 이메일
├── order-confirmation.html # 주문 확인
├── order-cancelled.html    # 주문 취소
├── delivery-started.html   # 배송 시작
└── delivery-completed.html # 배송 완료
```

## 재시도 전략

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
           kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        // 재시도 설정
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L)  // 1초 간격, 3회 재시도
        ));

        return factory;
    }
}
```

## Dead Letter Queue

```
┌──────────────┐     실패     ┌──────────────────┐
│ user-signup  │ ───────────▶ │ user-signup.DLT  │
└──────────────┘     (3회)    └──────────────────┘
```

```java
@KafkaListener(topics = "user-signup.DLT", groupId = "dlq-group")
public void handleDeadLetter(ConsumerRecord<String, Object> record) {
    log.error("Dead letter received: {}", record.value());
    // 수동 처리 또는 알림
}
```

## 알림 이력 관리

```java
@Entity
public class NotificationHistory {
    @Id @GeneratedValue
    private Long id;
    private String userId;
    private NotificationType type;    // EMAIL, SMS, PUSH
    private String recipient;         // 이메일/전화번호
    private String subject;
    private String content;
    private NotificationStatus status; // SENT, FAILED, PENDING
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
```

## 모니터링

### Kafka Consumer Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: notification-service
```

### 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `kafka_consumer_records_consumed_total` | 수신 메시지 수 |
| `kafka_consumer_records_lag` | Consumer Lag |
| `notification_sent_total` | 발송 알림 수 |
| `notification_failed_total` | 실패 알림 수 |
