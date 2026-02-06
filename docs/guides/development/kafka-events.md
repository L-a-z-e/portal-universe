---
id: kafka-events
title: Kafka 이벤트 구현 가이드
type: guide
status: current
created: 2026-02-05
updated: 2026-02-06
author: Laze
tags: [kafka, event-driven, guide, backend]
---

# Kafka 이벤트 구현 가이드

**난이도**: ⭐⭐⭐ | **예상 시간**: 1시간 | **카테고리**: Development

## 개요

Portal Universe의 서비스 간 비동기 통신은 Kafka 이벤트 기반으로 구현됩니다. 이 가이드는 새로운 이벤트를 설계하고 Producer/Consumer를 구현하는 방법을 다룹니다.

---

## 사전 요구사항

### 필수 도구
- [ ] Kafka (docker-compose-local.yml로 실행)
- [ ] Java 17 + Spring Boot 3.5.5

### 필수 지식
- Spring Boot 서비스 기본 구조 이해
- Kafka 기본 개념 (Topic, Producer, Consumer, Consumer Group)

---

## 단계별 실행

### Step 1: 이벤트 설계

새로운 이벤트를 설계할 때는 다음 규칙을 따릅니다.

**토픽 네이밍 규칙**:
- 패턴: `{domain}-{action}`
- 예시: `order-created`, `user-signup`, `blog-post-liked`

**이벤트 키**:
- Aggregate identifier 사용 (예: orderNumber, userId)
- 같은 키를 가진 이벤트는 순서 보장

**이벤트 클래스**:
```java
package com.portal.universe.event.shopping;

public record OrderCreatedEvent(
    String orderNumber,
    String userId,
    String productId,
    int quantity,
    long timestamp
) {}
```

**스키마 호환성**:
- 호환 가능: 필드 추가 (선택 필드로)
- Breaking Change: 필드 제거, 타입 변경

---

### Step 2: Producer 구현

`ShoppingEventPublisher` 패턴을 따라 구현합니다.

```java
package com.portal.universe.shopping.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String topic = "order-created";
        String key = event.orderNumber();

        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, key={}, offset={}",
                    topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish: topic={}, key={}, error={}",
                    topic, key, ex.getMessage());
            }
        });
    }
}
```

**설명**:
- `KafkaTemplate<String, Object>`: Spring Kafka 제공, JsonSerializer 사용
- `send()`: 비동기로 메시지 전송
- `whenComplete()`: 성공/실패 로깅 (에러 발생 시 재시도는 Kafka Producer 설정에 따름)

**사용 예시**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ShoppingEventPublisher eventPublisher;

    public void createOrder(OrderRequest request) {
        // 주문 생성 로직
        Order order = orderRepository.save(...);

        // 이벤트 발행
        eventPublisher.publishOrderCreated(new OrderCreatedEvent(
            order.getOrderNumber(),
            order.getUserId(),
            order.getProductId(),
            order.getQuantity(),
            System.currentTimeMillis()
        ));
    }
}
```

---

### Step 3: Consumer 구현

`NotificationConsumer` 패턴을 따라 구현합니다.

```java
package com.portal.universe.notification.consumer;

import com.portal.universe.event.shopping.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
        topics = "order-created",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received event: orderNumber={}, userId={}",
            event.orderNumber(), event.userId());

        try {
            notificationService.sendOrderNotification(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
            throw e; // Spring Kafka retry handles this
        }
    }
}
```

**설명**:
- `@KafkaListener`: Spring이 Consumer 자동 생성
- `groupId`: Consumer Group ID (application.yml에서 주입)
- 예외 발생 시 `throw`: Spring Kafka의 재시도 정책 적용

**주의사항**:
- Consumer는 멱등성(idempotent)을 보장해야 합니다 (같은 이벤트 중복 수신 가능)
- 처리 실패 시 예외를 던져야 Spring Kafka가 재시도합니다

---

### Step 4: Kafka 설정

**application.yml** (공통 설정):
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: ${spring.application.name}-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.portal.universe.event.*
    listener:
      ack-mode: manual
```

**application-local.yml**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**application-docker.yml**:
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:29092
```

---

### 현재 토픽 맵

| Producer | Topics | Consumer |
|----------|--------|----------|
| auth-service | user-signup | notification-service |
| shopping-service | order-created, order-cancelled, payment-completed, payment-failed, delivery-shipped, coupon-issued, timedeal-started | notification-service |
| blog-service | blog-post-liked, blog-post-commented, blog-comment-replied, blog-user-followed | notification-service |
| prism-service | prism-task-completed, prism-task-failed | notification-service |

---

## 검증

### 최종 확인 체크리스트
- [ ] Kafka가 실행 중인지 확인
- [ ] Producer가 이벤트를 성공적으로 발행하는지 로그 확인
- [ ] Consumer가 이벤트를 수신하고 처리하는지 로그 확인
- [ ] Consumer offset이 증가하는지 확인

### Kafka 토픽 확인

```bash
# 토픽 목록 조회
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# 특정 토픽의 메시지 조회 (최근 10개)
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --from-beginning \
  --max-messages 10
```

**예상 출력**:
```
order-created
user-signup
blog-post-liked
...
```

### Consumer Offset 확인

```bash
# Consumer Group 상태 확인
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-service-group
```

**예상 출력**:
```
GROUP                    TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
notification-service-group order-created 0          145             145             0
```

- `LAG`: 0이면 정상 (모든 메시지 처리 완료)
- `LAG > 0`: Consumer가 처리 속도가 느림 (backlog 있음)

---

## 문제 해결

### 자주 발생하는 문제

#### 문제 1: "No broker available" 에러

**원인**: Kafka가 실행되지 않았거나 아직 준비되지 않음

**해결**:
```bash
# Kafka 컨테이너 상태 확인
docker ps | grep kafka

# Kafka 재시작
docker compose -f docker-compose-local.yml restart kafka

# Kafka 로그 확인 (Ready 메시지 대기)
docker logs kafka -f
```

#### 문제 2: Deserialization Error

**원인**: Consumer의 타입 매핑이 맞지 않음

**해결**:
1. `spring.json.trusted.packages` 설정 확인
2. 이벤트 클래스의 패키지가 trusted packages에 포함되어 있는지 확인
3. Producer와 Consumer의 이벤트 클래스 구조가 일치하는지 확인

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.trusted.packages: com.portal.universe.event.*
```

#### 문제 3: Consumer Lag 증가

**원인**: Consumer 처리 속도가 Producer 발행 속도보다 느림

**해결**:
1. Consumer 로그에서 처리 시간 확인
2. 불필요한 동기 호출 제거 (Feign Client 호출 최소화)
3. Consumer 인스턴스 스케일 아웃 (같은 group-id)
4. Partition 수 증가 (처리량 증가)

```bash
# Partition 수 확인
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic order-created
```

---

## 다음 단계

이 가이드를 완료했다면:
1. Outbox 패턴 학습 - DB 트랜잭션과 이벤트 발행의 원자성 보장
2. Dead Letter Topic (DLT) 구현 - 재시도 실패한 메시지 처리
3. Schema Registry 도입 - Avro/Protobuf로 스키마 관리

---

## 참고 자료

- [Spring Kafka 공식 문서](https://docs.spring.io/spring-kafka/reference/html/)
- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- `.claude/skills/kafka-events.md` - 상세 구현 패턴
- `docs/architecture/integration/event-driven.md` - 이벤트 기반 아키텍처 개요

---

작성자: Laze
