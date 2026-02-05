# Inter-Service Communication (서비스 간 통신)

## 학습 목표

- 동기(Synchronous) vs 비동기(Asynchronous) 통신 이해
- REST, gRPC, 메시지 큐 비교
- Portal Universe의 통신 패턴 분석

---

## 1. 통신 패턴 개요

마이크로서비스 간 통신은 크게 두 가지 방식으로 나뉩니다:

```
┌─────────────────────────────────────────────────────────┐
│              Inter-Service Communication                 │
├───────────────────────┬─────────────────────────────────┤
│     Synchronous       │         Asynchronous            │
│   (Request-Response)  │       (Event-Driven)            │
├───────────────────────┼─────────────────────────────────┤
│ • REST API            │ • Message Queue (Kafka)         │
│ • gRPC                │ • Event Streaming               │
│ • GraphQL             │ • Pub/Sub                       │
└───────────────────────┴─────────────────────────────────┘
```

### 언제 어떤 방식을 사용하나?

| 상황 | 동기 | 비동기 |
|------|------|--------|
| 즉각적인 응답 필요 | ✅ | ❌ |
| 서비스 간 느슨한 결합 | ❌ | ✅ |
| 실패 시 재시도 | 어려움 | ✅ |
| 여러 서비스에 알림 | 비효율 | ✅ (Pub/Sub) |
| 트랜잭션 일관성 | ✅ | 어려움 |

---

## 2. 동기 통신 (Synchronous)

### 2.1 REST API

**특징:**
- HTTP 기반, 가장 널리 사용
- 자원(Resource) 중심 설계
- Stateless

**Portal Universe 예시:**

```java
// Shopping Service → Auth Service (사용자 조회)
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthClient {

    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable Long userId);

    @GetMapping("/api/v1/users/{userId}/exists")
    ApiResponse<Boolean> existsUser(@PathVariable Long userId);
}
```

**사용 예시:**

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final AuthClient authClient;

    public Order createOrder(Long userId, OrderRequest request) {
        // 동기 호출: 사용자 존재 확인
        Boolean exists = authClient.existsUser(userId).getData();
        if (!exists) {
            throw new UserNotFoundException(userId);
        }

        // 주문 생성 로직
        return orderRepository.save(request.toEntity(userId));
    }
}
```

### 2.2 REST 통신의 문제점

**1. Temporal Coupling (시간적 결합)**

```
Client ──→ Service A ──→ Service B ──→ Service C
                              │
                         장애 발생!
                              │
                    전체 요청 실패 ✗
```

**2. 연쇄 장애 (Cascading Failure)**

```java
// Service B가 응답하지 않으면 Service A도 timeout
@Retry(name = "authService", fallbackMethod = "fallback")
public UserResponse getUser(Long userId) {
    return authClient.getUser(userId);  // 5초 timeout
}
```

**해결책: Circuit Breaker**

```java
@CircuitBreaker(name = "authService", fallbackMethod = "fallbackUser")
public UserResponse getUser(Long userId) {
    return authClient.getUser(userId);
}

public UserResponse fallbackUser(Long userId, Throwable t) {
    return UserResponse.unknown(userId);  // 기본값 반환
}
```

### 2.3 gRPC (선택적 학습)

**특징:**
- Protocol Buffers (효율적 직렬화)
- HTTP/2 기반 (멀티플렉싱)
- 양방향 스트리밍 지원

**REST vs gRPC 비교:**

| 항목 | REST | gRPC |
|------|------|------|
| 프로토콜 | HTTP/1.1 | HTTP/2 |
| 데이터 형식 | JSON | Protocol Buffers |
| 성능 | 상대적 낮음 | 높음 |
| 브라우저 지원 | ✅ | 제한적 |
| 학습 곡선 | 낮음 | 높음 |

---

## 3. 비동기 통신 (Asynchronous)

### 3.1 메시지 기반 통신

```
Producer ──→ [ Message Broker ] ──→ Consumer
              (Kafka, RabbitMQ)
```

**장점:**
- 서비스 간 느슨한 결합
- 장애 격리
- 확장성 (Consumer 추가 용이)
- 재시도 및 재처리 가능

### 3.2 Portal Universe의 Kafka 통신

**이벤트 발행 (Producer):**

```java
// Shopping Service: 주문 생성 후 이벤트 발행
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .createdAt(LocalDateTime.now())
            .build();

        kafkaTemplate.send(orderCreatedTopic,
                          order.getId().toString(),
                          event);
    }
}
```

**이벤트 소비 (Consumer):**

```java
// Notification Service: 주문 이벤트 수신
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topic.order-created}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 알림 발송
        notificationService.sendOrderConfirmation(
            event.getUserId(),
            event.getOrderId()
        );
    }
}
```

### 3.3 Portal Universe 이벤트 토픽 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Kafka Cluster                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Topics:                                                 │
│  ┌──────────────────┬──────────────────────────────┐   │
│  │ user.registered  │ Auth → Notification          │   │
│  ├──────────────────┼──────────────────────────────┤   │
│  │ order.created    │ Shopping → Notification      │   │
│  ├──────────────────┼──────────────────────────────┤   │
│  │ payment.completed│ Shopping → Notification      │   │
│  ├──────────────────┼──────────────────────────────┤   │
│  │ post.published   │ Blog → Notification          │   │
│  └──────────────────┴──────────────────────────────┘   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 통신 패턴 비교

### 4.1 Request-Response vs Event-Driven

**Request-Response (동기):**

```
Shopping     Auth        Notification
Service     Service       Service
   │           │             │
   │──GET User─▶│             │
   │◀──Response─│             │
   │           │             │
   │ 주문생성   │             │
   │──────────────알림요청────▶│
   │◀──────────────응답───────│
```

**Event-Driven (비동기):**

```
Shopping         Kafka          Notification
Service                          Service
   │              │                 │
   │ 주문생성     │                 │
   │──order.created──▶│             │
   │              │──order.created──▶│
   │              │                 │ 알림발송
   │              │                 │
   │ 즉시 응답    │                 │
```

### 4.2 어떤 방식을 선택할까?

| 사용 사례 | 권장 방식 | 이유 |
|----------|----------|------|
| 사용자 인증 | REST | 즉시 결과 필요 |
| 주문 상태 조회 | REST | 즉시 응답 필요 |
| 결제 완료 알림 | Kafka | 느슨한 결합 |
| 재고 동기화 | Kafka | 여러 서비스 구독 |
| 로그 수집 | Kafka | 대용량 처리 |

---

## 5. Portal Universe 통신 아키텍처

### 5.1 전체 통신 흐름

```
                          ┌────────────────┐
                          │  API Gateway   │
                          └───────┬────────┘
                                  │ REST
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────┐          ┌─────────────┐          ┌─────────────┐
│Auth Service │◀──REST──▶│Blog Service │          │Shopping Svc │
│             │          │             │          │             │
└──────┬──────┘          └──────┬──────┘          └──────┬──────┘
       │                        │                        │
       │                        │                        │
       │     ┌──────────────────┴────────────────────────┤
       │     │                                           │
       │     │         ┌─────────────────┐               │
       │     │         │                 │               │
       │     ▼         ▼                 ▼               ▼
       │  ┌─────────────────────────────────────────────────┐
       │  │              Apache Kafka                       │
       │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐           │
       └──▶  │ user.*  │ │ post.*  │ │ order.* │           │
          │  └─────────┘ └─────────┘ └─────────┘           │
          └────────────────────┬────────────────────────────┘
                               │
                               ▼
                    ┌───────────────────┐
                    │Notification Service│
                    │   (Consumer)       │
                    └───────────────────┘
```

### 5.2 REST 통신 예시

**Gateway → Auth Service:**

```yaml
# API Gateway route configuration
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**
```

**Shopping → Auth (Feign Client):**

```java
@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @GetMapping("/internal/users/{userId}")
    UserInternalResponse getUserInternal(@PathVariable Long userId);
}
```

### 5.3 Kafka 통신 예시

**이벤트 정의:**

```java
// common-library의 공통 이벤트
@Data
@Builder
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime createdAt;
}
```

**Producer 설정:**

```yaml
# Shopping Service application.yml
spring:
  kafka:
    producer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Consumer 설정:**

```yaml
# Notification Service application.yml
spring:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

---

## 6. 통신 패턴 모범 사례

### 6.1 Idempotency (멱등성)

같은 요청을 여러 번 보내도 결과가 동일해야 합니다.

```java
@Service
public class OrderService {
    // 멱등성 키를 사용한 중복 방지
    public Order createOrder(String idempotencyKey, OrderRequest request) {
        // 이미 처리된 요청인지 확인
        if (idempotencyStore.exists(idempotencyKey)) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow();
        }

        Order order = processOrder(request);
        idempotencyStore.save(idempotencyKey, order.getId());
        return order;
    }
}
```

### 6.2 Timeout과 Retry

```java
// Resilience4j 설정
@Retry(name = "authService", fallbackMethod = "fallback")
@CircuitBreaker(name = "authService")
@Timeout(name = "authService")
public UserResponse getUser(Long userId) {
    return authClient.getUser(userId);
}
```

```yaml
# application.yml
resilience4j:
  retry:
    instances:
      authService:
        max-attempts: 3
        wait-duration: 500ms
  timeout:
    instances:
      authService:
        timeout-duration: 3s
```

### 6.3 Dead Letter Queue (DLQ)

처리 실패한 메시지를 별도 보관:

```java
@KafkaListener(topics = "order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        processOrder(event);
    } catch (Exception e) {
        // 재시도 후에도 실패하면 DLQ로 이동
        kafkaTemplate.send("order.created.dlq", event);
        throw e;
    }
}
```

---

## 7. 실습

### 실습 1: REST 통신 분석

```bash
# Feign Client 설정 확인
grep -r "@FeignClient" services/*/src/main/java --include="*.java"

# API 호출 패턴 확인
grep -r "RestTemplate\|WebClient" services/*/src/main/java --include="*.java"
```

### 실습 2: Kafka 토픽 확인

```bash
# Kafka 토픽 설정 확인
grep -r "kafka.topic" services/*/src/main/resources/*.yml

# Producer 확인
grep -r "KafkaTemplate" services/*/src/main/java --include="*.java"

# Consumer 확인
grep -r "@KafkaListener" services/*/src/main/java --include="*.java"
```

### 실습 3: Circuit Breaker 설정 확인

```bash
# Resilience4j 설정 확인
grep -r "resilience4j" services/*/src/main/resources/*.yml
```

---

## 8. 핵심 요약

### 통신 방식 선택 가이드

| 요구사항 | 선택 | Portal Universe 예시 |
|----------|------|---------------------|
| 즉시 응답 필요 | REST | 사용자 인증, 상품 조회 |
| 느슨한 결합 | Kafka | 주문 알림, 로그 수집 |
| 다수 구독자 | Kafka | 주문 이벤트 → 알림, 분석 |
| 데이터 일관성 | REST + Saga | 주문-재고-결제 |

### Portal Universe 핵심 패턴

1. **동기 (REST)**: Gateway ↔ Services, Services 간 조회
2. **비동기 (Kafka)**: 도메인 이벤트 발행/구독
3. **회복력**: Circuit Breaker, Retry, Timeout

---

## 관련 문서

- [마이크로서비스 개요](./microservices-overview.md)
- [API Gateway 패턴](./api-gateway-pattern.md)
- [Kafka 기초](../kafka/kafka-introduction.md)
- [Circuit Breaker](../patterns/circuit-breaker-resilience.md)
