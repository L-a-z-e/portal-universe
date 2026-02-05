# Apache Kafka 소개

## 학습 목표
- Kafka의 핵심 아키텍처와 구성 요소 이해
- 메시지 기반 시스템에서 Kafka의 역할 파악
- 기존 메시지 큐와의 차이점 인식

---

## 1. Kafka란?

Apache Kafka는 **분산 이벤트 스트리밍 플랫폼**입니다. LinkedIn에서 개발하여 2011년 오픈소스로 공개했으며, 현재 대규모 데이터 파이프라인의 표준으로 자리잡았습니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **고처리량** | 초당 수백만 메시지 처리 가능 |
| **내구성** | 디스크에 메시지를 저장하여 데이터 손실 방지 |
| **확장성** | 브로커 추가로 수평 확장 |
| **내결함성** | 복제를 통한 장애 대응 |
| **실시간 처리** | 낮은 지연시간(ms 단위) |

### 전통적 메시지 큐 vs Kafka

```
[전통적 Message Queue]
Producer → Queue → Consumer (메시지 소비 후 삭제)

[Apache Kafka]
Producer → Topic(Log) → Consumer Group A
                     → Consumer Group B (여러 그룹이 동일 메시지 소비)
```

---

## 2. 핵심 개념

### 2.1 Topic (토픽)

메시지를 분류하는 **논리적 채널**입니다.

```
┌─────────────────────────────────────────┐
│  Topic: shopping.order.created          │
├─────────────────────────────────────────┤
│  Partition 0: [msg0][msg3][msg6]...     │
│  Partition 1: [msg1][msg4][msg7]...     │
│  Partition 2: [msg2][msg5][msg8]...     │
└─────────────────────────────────────────┘
```

**Portal Universe 토픽 예시:**
- `shopping.order.created` - 주문 생성 이벤트
- `shopping.payment.completed` - 결제 완료 이벤트
- `shopping.delivery.shipped` - 배송 시작 이벤트
- `user-signup` - 회원가입 이벤트

### 2.2 Partition (파티션)

토픽을 **물리적으로 분할**하여 병렬 처리를 가능하게 합니다.

```java
// Portal Universe - 모든 토픽이 3개 파티션으로 설정됨
@Bean
public NewTopic orderCreatedTopic() {
    return TopicBuilder.name("shopping.order.created")
            .partitions(3)       // 3개 파티션
            .replicas(1)         // 1개 복제본
            .build();
}
```

**파티션의 장점:**
- 병렬 처리로 처리량 증가
- Consumer Group 내 Consumer 수만큼 병렬 소비
- 파티션 내 메시지 순서 보장

### 2.3 Producer (프로듀서)

메시지를 토픽에 **발행**하는 역할입니다.

```
Producer
    │
    ├─► Key = "ORDER-001" → Partition 0
    ├─► Key = "ORDER-002" → Partition 1  (Key 해시로 파티션 결정)
    └─► Key = "ORDER-001" → Partition 0  (같은 Key는 같은 파티션)
```

### 2.4 Consumer & Consumer Group

메시지를 토픽에서 **구독**하는 역할입니다.

```
                    Topic (3 partitions)
                   ┌─────────────────────┐
                   │ P0 │ P1 │ P2        │
                   └─────────────────────┘
                         ↓
       ┌─────────────────────────────────────┐
       │     Consumer Group: notification    │
       │  ┌───────┐ ┌───────┐ ┌───────┐     │
       │  │ C1    │ │ C2    │ │ C3    │     │
       │  │ (P0)  │ │ (P1)  │ │ (P2)  │     │
       │  └───────┘ └───────┘ └───────┘     │
       └─────────────────────────────────────┘
```

**Consumer Group 규칙:**
- 각 파티션은 그룹 내 **하나의 Consumer만** 할당
- Consumer 수 > 파티션 수 → 일부 Consumer 유휴 상태
- Consumer 수 < 파티션 수 → 일부 Consumer가 여러 파티션 담당

### 2.5 Offset (오프셋)

파티션 내 메시지의 **순차적 위치**입니다.

```
Partition 0:
┌───┬───┬───┬───┬───┬───┬───┐
│ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │  ← Offset
└───┴───┴───┴───┴───┴───┴───┘
              ↑
        Consumer 현재 위치 (offset 3)
```

**Offset 관리:**
- Consumer가 어디까지 읽었는지 추적
- `__consumer_offsets` 내부 토픽에 저장
- 장애 복구 시 마지막 위치부터 재개

### 2.6 Broker (브로커)

Kafka 서버 인스턴스입니다. 클러스터는 여러 브로커로 구성됩니다.

```
┌─────────────────────────────────────────────────┐
│                 Kafka Cluster                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Broker 1 │  │ Broker 2 │  │ Broker 3 │      │
│  │ (Leader) │  │(Follower)│  │(Follower)│      │
│  │    P0    │  │    P0    │  │    P0    │      │
│  └──────────┘  └──────────┘  └──────────┘      │
└─────────────────────────────────────────────────┘
```

---

## 3. 메시지 전달 보장

### 3.1 전달 보장 수준

| 수준 | 설명 | 적용 상황 |
|------|------|----------|
| **At-most-once** | 최대 1회 (메시지 손실 가능) | 로그 등 손실 허용 |
| **At-least-once** | 최소 1회 (중복 가능) | 일반적 비즈니스 |
| **Exactly-once** | 정확히 1회 | 금융, 결제 |

### 3.2 acks 설정

```java
// Portal Universe 설정 (KafkaConfig.java)
props.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 복제본 확인
```

| acks | 의미 | 지연 | 내구성 |
|------|------|------|--------|
| `0` | 확인 안 함 | 최소 | 낮음 |
| `1` | Leader만 확인 | 중간 | 중간 |
| `all` | 모든 ISR 확인 | 높음 | **높음** ✓ |

---

## 4. Kafka vs 다른 메시지 시스템

| 특성 | Kafka | RabbitMQ | AWS SQS |
|------|-------|----------|---------|
| **모델** | Pub/Sub + Log | Queue/Pub/Sub | Queue |
| **메시지 보존** | 설정 기간 유지 | 소비 후 삭제 | 소비 후 삭제 |
| **재처리** | 가능 (offset 조정) | 불가 | 제한적 |
| **처리량** | 매우 높음 | 높음 | 중간 |
| **순서 보장** | 파티션 내 보장 | Queue 내 보장 | FIFO 옵션 |
| **복잡도** | 높음 | 중간 | 낮음 |

---

## 5. Portal Universe에서의 역할

### 이벤트 기반 아키텍처 (EDA)

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│ Auth Service│────►│   Kafka     │────►│Notification Svc  │
│ (Producer)  │     │  Cluster    │     │  (Consumer)      │
└─────────────┘     └─────────────┘     └──────────────────┘
       │                   ▲
       │  user-signup      │
       └───────────────────┘

┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│Shopping Svc │────►│   Kafka     │────►│Notification Svc  │
│ (Producer)  │     │  Cluster    │     │  (Consumer)      │
└─────────────┘     └─────────────┘     └──────────────────┘
       │                   ▲
       │  order.created    │
       │  payment.completed│
       │  delivery.shipped │
       └───────────────────┘
```

### 장점

1. **서비스 간 느슨한 결합**: 직접 호출 없이 이벤트로 통신
2. **확장성**: Consumer 추가로 처리량 증가
3. **내구성**: 메시지가 디스크에 저장되어 손실 방지
4. **재처리**: 문제 발생 시 offset 조정으로 재처리 가능

---

## 6. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Topic** | 메시지 분류 단위 |
| **Partition** | 병렬 처리를 위한 물리적 분할 |
| **Offset** | 파티션 내 메시지 위치 |
| **Consumer Group** | 협력하여 토픽을 소비하는 Consumer 집합 |
| **acks=all** | 모든 복제본 확인으로 내구성 보장 |

---

## 다음 학습

- [Kafka 핵심 개념 심화](./kafka-core-concepts.md)
- [Kafka Spring 통합](./kafka-spring-integration.md)
- [Kafka Portal Universe 적용](./kafka-portal-universe.md)

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Confluent Kafka 튜토리얼](https://developer.confluent.io/)
