# Kafka Partitioning Strategy

## Overview

Kafka의 파티셔닝(Partitioning)은 토픽 데이터를 여러 파티션에 분산 저장하여 병렬 처리를 가능하게 하는 핵심 메커니즘입니다. 올바른 파티셔닝 전략은 처리량(Throughput), 순서 보장(Ordering), 부하 분산(Load Balancing)에 직접적인 영향을 미칩니다.

---

## 1. 파티션 개념과 병렬 처리

### 1.1 파티션이란?

파티션은 토픽 내의 물리적인 데이터 저장 단위입니다.

```
Topic: shopping.order.created
├── Partition 0: [msg1, msg4, msg7, ...]
├── Partition 1: [msg2, msg5, msg8, ...]
└── Partition 2: [msg3, msg6, msg9, ...]
```

**핵심 특성:**
- 각 파티션은 **독립적인 로그** (append-only)
- 파티션 내에서만 **순서 보장**
- 파티션은 Consumer Group 내 **하나의 Consumer만** 읽을 수 있음

### 1.2 병렬 처리 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                     Producer                                     │
│         (shopping-service)                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
┌────────────┐ ┌────────────┐ ┌────────────┐
│ Partition 0│ │ Partition 1│ │ Partition 2│
│  Order A   │ │  Order B   │ │  Order C   │
│  Order A'  │ │  Order B'  │ │  Order C'  │
└─────┬──────┘ └─────┬──────┘ └─────┬──────┘
      │              │              │
      ▼              ▼              ▼
┌────────────┐ ┌────────────┐ ┌────────────┐
│ Consumer 1 │ │ Consumer 2 │ │ Consumer 3 │
│  (Pod 1)   │ │  (Pod 2)   │ │  (Pod 3)   │
└────────────┘ └────────────┘ └────────────┘
         └───────────┴───────────┘
              Consumer Group
            (notification-group)
```

### 1.3 병렬 처리의 이점

| 측면 | 단일 파티션 | 다중 파티션 |
|------|------------|------------|
| **처리량** | 제한적 | 파티션 수에 비례 |
| **Consumer 확장성** | 불가 | 파티션 수까지 확장 가능 |
| **장애 격리** | 전체 영향 | 파티션 단위 격리 |
| **순서 보장** | 전체 보장 | 파티션 내에서만 보장 |

---

## 2. 키 기반 파티셔닝

### 2.1 키의 역할

메시지 키(Key)는 파티션 할당의 결정자입니다.

```java
// Portal Universe의 키 기반 발행 예시
kafkaTemplate.send(topic, key, event);
//                        ↑
//                   이 키가 파티션 결정
```

**파티션 할당 공식:**
```
partition = hash(key) % numPartitions
```

### 2.2 키 선정 원칙

#### 좋은 키 선정 기준

1. **유니크성**: 데이터가 고르게 분산되도록 충분히 고유해야 함
2. **연관성**: 관련 메시지가 같은 파티션으로 가도록 논리적 연관성
3. **카디널리티**: 키의 고유값 수가 파티션 수보다 충분히 커야 함

#### Portal Universe의 키 전략 분석

```java
// ShoppingEventPublisher.java

// 주문 관련 이벤트 - orderNumber 사용
public void publishOrderCreated(OrderCreatedEvent event) {
    publishEvent(TOPIC_ORDER_CREATED, event.orderNumber(), event);
}

public void publishOrderConfirmed(OrderConfirmedEvent event) {
    publishEvent(TOPIC_ORDER_CONFIRMED, event.orderNumber(), event);
}

public void publishOrderCancelled(OrderCancelledEvent event) {
    publishEvent(TOPIC_ORDER_CANCELLED, event.orderNumber(), event);
}

// 결제 관련 이벤트 - paymentNumber 사용
public void publishPaymentCompleted(PaymentCompletedEvent event) {
    publishEvent(TOPIC_PAYMENT_COMPLETED, event.paymentNumber(), event);
}

// 배송 관련 이벤트 - trackingNumber 사용
public void publishDeliveryShipped(DeliveryShippedEvent event) {
    publishEvent(TOPIC_DELIVERY_SHIPPED, event.trackingNumber(), event);
}
```

**전략 분석:**

| 이벤트 유형 | 키 | 이유 |
|------------|-----|------|
| Order Events | `orderNumber` | 동일 주문의 모든 이벤트가 순서대로 처리됨 |
| Payment Events | `paymentNumber` | 결제 상태 변경이 순차 처리됨 |
| Delivery Events | `trackingNumber` | 배송 추적 이벤트 순서 보장 |

### 2.3 키가 없는 경우

키 없이 발행하면 Round-Robin으로 분산됩니다.

```java
// auth-service의 user-signup 이벤트 (키 없음)
kafkaTemplate.send("user-signup", event);  // 키 미지정
```

**Round-Robin 특성:**
- 파티션 간 균등 분산
- 순서 보장 없음
- 독립적인 이벤트에 적합

---

## 3. 순서 보장 전략

### 3.1 순서 보장 수준

```
┌─────────────────────────────────────────────────────────────────┐
│                    순서 보장 수준 (Level)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Level 1: 전체 순서 보장      [단일 파티션]                       │
│  ────────────────────────                                        │
│  msg1 → msg2 → msg3 → msg4 (전체 순서 보장)                      │
│  처리량 제한, 확장성 낮음                                         │
│                                                                  │
│  Level 2: 키별 순서 보장      [다중 파티션 + 키]                   │
│  ────────────────────────                                        │
│  Partition 0: order-A1 → order-A2 → order-A3 (A 주문 순서 보장)   │
│  Partition 1: order-B1 → order-B2 → order-B3 (B 주문 순서 보장)   │
│  높은 처리량, 실용적 순서 보장                                    │
│                                                                  │
│  Level 3: 순서 보장 없음      [키 없음]                           │
│  ────────────────────────                                        │
│  Round-Robin 분산                                                │
│  최고 처리량, 순서 무관한 작업용                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 비즈니스별 순서 보장 패턴

#### E-Commerce 주문 처리 (Portal Universe Shopping Service)

```
주문 생성 → 결제 완료 → 재고 예약 → 배송 시작

[같은 주문의 이벤트는 반드시 순서대로 처리되어야 함]
```

**해결책: 주문번호를 키로 사용**

```java
// 모든 주문 관련 이벤트가 같은 파티션으로
publishEvent(TOPIC_ORDER_CREATED, event.orderNumber(), event);
publishEvent(TOPIC_INVENTORY_RESERVED, event.orderNumber(), event);
```

#### 사용자 알림 (순서 덜 중요)

```java
// 독립적인 알림 - 키 없이 발행하여 최대 처리량 확보
kafkaTemplate.send("notification.send", notificationEvent);
```

### 3.3 순서 보장 위반 시나리오와 해결

#### 문제: Consumer 재할당 중 순서 역전

```
시간 T1: Consumer A가 Partition 0 처리 중 장애 발생
시간 T2: Consumer B가 Partition 0 재할당 받음
시간 T3: Consumer A가 처리 중이던 메시지와 Consumer B가 새로 읽은 메시지 충돌
```

**해결책:**

```java
// 1. Idempotent Consumer 구현
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 중복 처리 방지
    if (eventRepository.existsByEventId(event.eventId())) {
        log.info("Duplicate event ignored: {}", event.eventId());
        return;
    }

    processOrder(event);
    eventRepository.save(new ProcessedEvent(event.eventId()));
}

// 2. 버전 기반 처리
@KafkaListener(topics = "shopping.order.updated")
public void handleOrderUpdated(OrderUpdatedEvent event) {
    Order order = orderRepository.findById(event.orderId())
        .orElseThrow();

    // 이전 버전 이벤트 무시
    if (order.getVersion() >= event.version()) {
        log.info("Stale event ignored: order={}, eventVersion={}, currentVersion={}",
            event.orderId(), event.version(), order.getVersion());
        return;
    }

    processOrderUpdate(event);
}
```

---

## 4. 파티션 수 결정 가이드

### 4.1 파티션 수 결정 요소

```
┌──────────────────────────────────────────────────────────────────┐
│                     파티션 수 결정 요소                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 목표 처리량 (Throughput)                                      │
│     ────────────────────────                                      │
│     단일 파티션 처리량: ~10MB/s                                   │
│     필요 파티션 수 = 목표 처리량 / 단일 파티션 처리량              │
│                                                                   │
│  2. Consumer 수 (병렬성)                                          │
│     ────────────────────                                          │
│     파티션 수 ≥ 최대 Consumer 인스턴스 수                         │
│     (Consumer는 파티션보다 많을 수 없음)                          │
│                                                                   │
│  3. 키 카디널리티                                                 │
│     ─────────────────                                             │
│     고유 키 수 >> 파티션 수 (균등 분산 보장)                      │
│                                                                   │
│  4. 운영 고려사항                                                 │
│     ────────────────                                              │
│     파티션 증가는 쉬움, 감소는 불가능                             │
│     Broker 메모리/파일 핸들 제한 고려                             │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 파티션 수 계산 공식

```
P = max(T/t, C)

Where:
P = 필요 파티션 수
T = 목표 처리량 (messages/sec)
t = 단일 파티션 처리량 (messages/sec)
C = 예상 최대 Consumer 수
```

### 4.3 Portal Universe의 파티션 설정 분석

```java
// KafkaConfig.java

@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(TOPIC_ORDER_CREATED)
            .partitions(3)      // 파티션 수
            .replicas(1)        // 복제 팩터 (로컬 개발용)
            .build();
}
```

**현재 설정 분석:**

| 토픽 | 파티션 수 | 복제 팩터 | 분석 |
|------|----------|----------|------|
| `shopping.order.created` | 3 | 1 | 개발 환경 적합, 프로덕션에서는 증가 필요 |
| `shopping.payment.completed` | 3 | 1 | 동일 |
| `shopping.delivery.shipped` | 3 | 1 | 동일 |

### 4.4 환경별 권장 파티션 수

| 환경 | 파티션 수 | 복제 팩터 | 이유 |
|------|----------|----------|------|
| **Local/Dev** | 3 | 1 | 최소 병렬성 테스트 |
| **Staging** | 6 | 2 | 프로덕션 시뮬레이션 |
| **Production** | 12-24 | 3 | 고가용성, 확장성 |

### 4.5 파티션 수 변경 시 주의사항

```
⚠️ 경고: 파티션 수 변경 시 키 재분배 발생

Before (3 partitions):
  hash("order-123") % 3 = 1  → Partition 1

After (6 partitions):
  hash("order-123") % 6 = 4  → Partition 4

결과: 같은 주문의 새 이벤트가 다른 파티션으로 이동
      → 기존 이벤트와의 순서 보장 깨짐
```

**안전한 변경 전략:**

1. 기존 메시지 모두 소비 완료 확인
2. Producer/Consumer 일시 중지
3. 파티션 수 변경
4. 서비스 재개

---

## 5. Custom Partitioner 구현

### 5.1 기본 Partitioner 동작

```java
// Kafka 기본 파티셔너 (DefaultPartitioner)
public int partition(String topic, Object key, byte[] keyBytes,
                     Object value, byte[] valueBytes, Cluster cluster) {
    if (keyBytes == null) {
        // 키 없으면 Round-Robin (Sticky 방식)
        return stickyPartitionCache.partition(topic, cluster);
    }
    // 키 있으면 해시 기반
    return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
}
```

### 5.2 Custom Partitioner 구현 예시

#### Priority-based Partitioner

VIP 고객 주문을 특정 파티션(우선 처리 파티션)으로 라우팅하는 예시입니다.

```java
package com.portal.universe.shoppingservice.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * VIP 고객 주문을 우선 처리 파티션으로 라우팅하는 Custom Partitioner
 *
 * 파티션 0: VIP 전용 (우선 처리)
 * 파티션 1-N: 일반 주문
 */
public class PriorityPartitioner implements Partitioner {

    private static final int VIP_PARTITION = 0;

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (keyBytes == null) {
            // 키 없으면 일반 파티션에 Round-Robin
            return Utils.toPositive(Utils.murmur2(topic.getBytes()))
                   % (numPartitions - 1) + 1;
        }

        String keyString = new String(keyBytes);

        // VIP 주문 식별 (예: VIP- 접두사)
        if (keyString.startsWith("VIP-")) {
            return VIP_PARTITION;
        }

        // 일반 주문은 해시 기반으로 파티션 1 이상에 분산
        int hash = Utils.toPositive(Utils.murmur2(keyBytes));
        return (hash % (numPartitions - 1)) + 1;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

#### Region-based Partitioner

지역별로 파티션을 분리하여 데이터 지역성을 확보하는 예시입니다.

```java
package com.portal.universe.shoppingservice.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 지역별 파티션 할당으로 데이터 지역성 확보
 *
 * Partition 0-2: SEOUL
 * Partition 3-5: BUSAN
 * Partition 6-8: DAEGU
 */
public class RegionPartitioner implements Partitioner {

    private static final Map<String, int[]> REGION_PARTITIONS = new HashMap<>();

    static {
        REGION_PARTITIONS.put("SEOUL", new int[]{0, 1, 2});
        REGION_PARTITIONS.put("BUSAN", new int[]{3, 4, 5});
        REGION_PARTITIONS.put("DAEGU", new int[]{6, 7, 8});
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        if (keyBytes == null) {
            return Utils.toPositive(Utils.murmur2(topic.getBytes()))
                   % cluster.partitionCountForTopic(topic);
        }

        String keyString = new String(keyBytes);
        // 키 형식: "REGION:orderNumber" (예: "SEOUL:ORD-12345")
        String[] parts = keyString.split(":");

        if (parts.length >= 2) {
            String region = parts[0];
            String orderKey = parts[1];

            int[] regionPartitions = REGION_PARTITIONS.get(region);
            if (regionPartitions != null) {
                // 지역 내에서 해시 기반 분산
                int hash = Utils.toPositive(Utils.murmur2(orderKey.getBytes()));
                return regionPartitions[hash % regionPartitions.length];
            }
        }

        // 기본 해시 기반 파티셔닝
        return Utils.toPositive(Utils.murmur2(keyBytes))
               % cluster.partitionCountForTopic(topic);
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

### 5.3 Custom Partitioner 적용

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Custom Partitioner 설정
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,
                        PriorityPartitioner.class.getName());

        return new DefaultKafkaProducerFactory<>(configProps);
    }
}
```

---

## 6. Portal Universe의 파티셔닝 전략 분석

### 6.1 현재 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                    Portal Universe Kafka Architecture            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐         ┌──────────────────────────────────┐  │
│  │ auth-service │─────────┤ user-signup (키 없음)             │  │
│  └──────────────┘         │ → Round-Robin 분산               │  │
│                           │ → 순서 보장 불필요               │  │
│                           └────────────────┬─────────────────┘  │
│                                            │                    │
│                                            ▼                    │
│                           ┌──────────────────────────────────┐  │
│                           │ notification-service              │  │
│                           │ (Consumer Group: notification-group)│
│                           └──────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────┐     ┌──────────────────────────────────┐  │
│  │ shopping-service │─────┤ shopping.order.created           │  │
│  │                  │     │  - 키: orderNumber               │  │
│  │  Event Publisher │     │  - 파티션: 3                     │  │
│  │                  │     │  - 순서 보장: 주문별             │  │
│  │                  │     ├──────────────────────────────────┤  │
│  │                  │─────┤ shopping.payment.completed       │  │
│  │                  │     │  - 키: paymentNumber             │  │
│  │                  │     │  - 결제별 순서 보장              │  │
│  │                  │     ├──────────────────────────────────┤  │
│  │                  │─────┤ shopping.delivery.shipped        │  │
│  │                  │     │  - 키: trackingNumber            │  │
│  │                  │     │  - 배송별 순서 보장              │  │
│  └──────────────────┘     └──────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 파티셔닝 전략 평가

#### 강점

1. **적절한 키 선택**
   - 주문 이벤트에 `orderNumber` 사용으로 동일 주문의 이벤트 순서 보장
   - 결제/배송도 각각의 식별자로 키 설정

2. **이벤트 분리**
   - 도메인별 토픽 분리 (`order`, `payment`, `delivery`)
   - 관심사 분리로 독립적 확장 가능

3. **신뢰성 설정**
   ```java
   configProps.put(ProducerConfig.ACKS_CONFIG, "all");
   configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
   configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
   ```

#### 개선 제안

1. **프로덕션 환경을 위한 파티션 수 증가**

```java
// 현재: 개발 환경용
.partitions(3)

// 권장: 프로덕션 환경
@Value("${kafka.partitions:3}")
private int partitions;

@Bean
@Profile("k8s")
public NewTopic orderCreatedTopicProd() {
    return TopicBuilder.name(TOPIC_ORDER_CREATED)
            .partitions(12)    // 프로덕션용 증가
            .replicas(3)       // 고가용성
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build();
}
```

2. **주문 관련 이벤트의 키 통일**

현재 결제와 배송이 별도 키를 사용하는데, 주문 전체 라이프사이클 추적이 필요하면:

```java
// 모든 주문 관련 이벤트를 orderNumber로 통일
public void publishPaymentCompleted(PaymentCompletedEvent event) {
    // paymentNumber 대신 orderNumber 사용
    publishEvent(TOPIC_PAYMENT_COMPLETED, event.orderNumber(), event);
}
```

3. **Consumer Group 분리 고려**

```java
// 현재: 단일 그룹
@KafkaListener(topics = "shopping.order.created",
               groupId = "notification-group")

// 권장: 목적별 그룹 분리
@KafkaListener(topics = "shopping.order.created",
               groupId = "notification-order-group")

@KafkaListener(topics = "shopping.order.created",
               groupId = "analytics-order-group")  // 분석용 별도 그룹
```

### 6.3 확장 시나리오별 권장 전략

#### 시나리오 1: 트래픽 10배 증가

```yaml
# 파티션 수 증가 + Consumer 확장
topics:
  shopping.order.created:
    partitions: 24        # 3 → 24
    replicas: 3

deployment:
  notification-service:
    replicas: 8           # 파티션 수 이하로 설정
```

#### 시나리오 2: 글로벌 확장 (다중 리전)

```java
// Region-aware 키 사용
public void publishOrderCreated(OrderCreatedEvent event) {
    String key = event.region() + ":" + event.orderNumber();
    publishEvent(TOPIC_ORDER_CREATED, key, event);
}

// Custom Partitioner로 지역별 파티션 분리
```

#### 시나리오 3: 실시간 분석 추가

```java
// 별도 Consumer Group으로 동일 이벤트 구독
@KafkaListener(
    topics = "shopping.order.created",
    groupId = "realtime-analytics-group",
    containerFactory = "batchListenerFactory"  // 배치 처리
)
public void analyzeOrders(List<OrderCreatedEvent> events) {
    analyticsService.processBatch(events);
}
```

---

## Quick Reference

### 파티셔닝 결정 체크리스트

| 질문 | Yes이면 | No이면 |
|------|---------|--------|
| 순서 보장이 필요한가? | 키 기반 파티셔닝 | Round-Robin 가능 |
| 특정 메시지 우선 처리가 필요한가? | Custom Partitioner | 기본 Partitioner |
| 지역별 데이터 분리가 필요한가? | Region-aware 키 | 단순 키 |
| 처리량이 주요 관심사인가? | 파티션 수 증가 | 최소 파티션 |

### 키 선정 가이드

| 도메인 | 권장 키 | 이유 |
|--------|---------|------|
| 주문 | `orderNumber` | 주문 라이프사이클 순서 보장 |
| 결제 | `orderNumber` or `paymentNumber` | 주문 연관 or 결제 독립 |
| 사용자 이벤트 | `userId` | 사용자별 이벤트 순서 |
| 알림 | 없음 (Round-Robin) | 독립적 처리, 최대 처리량 |

### Producer 신뢰성 설정

```java
// 필수 설정
configProps.put(ProducerConfig.ACKS_CONFIG, "all");
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
```

---

## 관련 문서

- [Kafka Fundamentals](./kafka-fundamentals.md) - Kafka 기본 개념
- [Kafka Consumer Groups](./kafka-consumer-groups.md) - Consumer Group 전략
- [Event-Driven Architecture](../patterns/event-driven-architecture.md) - 이벤트 기반 아키텍처
