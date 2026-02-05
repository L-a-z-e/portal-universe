# Consumer Scaling

## 개요

Kafka Consumer 스케일링은 메시지 처리량을 늘리기 위해 Consumer 인스턴스를 확장하는 것입니다. 효과적인 스케일링을 위해서는 파티션, Consumer Group, 그리고 애플리케이션 아키텍처를 이해해야 합니다.

## 스케일링 기본 원칙

### 파티션과 Consumer 관계

```
┌─────────────────────────────────────────────────────────────────────┐
│                Partition-Consumer Relationship                       │
│                                                                      │
│  Case 1: Consumers < Partitions (정상)                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Topic: 6 Partitions                                         │    │
│  │ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                              │    │
│  │ │P0│ │P1│ │P2│ │P3│ │P4│ │P5│                              │    │
│  │ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘                              │    │
│  │  │    │    │    │    │    │                                 │    │
│  │  ▼    ▼    ▼    ▼    ▼    ▼                                 │    │
│  │ ┌───────┐ ┌───────┐ ┌───────┐                              │    │
│  │ │ C1    │ │ C2    │ │ C3    │                              │    │
│  │ │P0, P1 │ │P2, P3 │ │P4, P5 │                              │    │
│  │ └───────┘ └───────┘ └───────┘                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Case 2: Consumers = Partitions (최적)                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                              │    │
│  │ │P0│ │P1│ │P2│ │P3│ │P4│ │P5│                              │    │
│  │ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘                              │    │
│  │  │    │    │    │    │    │                                 │    │
│  │  ▼    ▼    ▼    ▼    ▼    ▼                                 │    │
│  │ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                              │    │
│  │ │C1│ │C2│ │C3│ │C4│ │C5│ │C6│                              │    │
│  │ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Case 3: Consumers > Partitions (비효율)                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                              │    │
│  │ │P0│ │P1│ │P2│ │P3│ │P4│ │P5│                              │    │
│  │ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘ └┬─┘                              │    │
│  │  │    │    │    │    │    │                                 │    │
│  │  ▼    ▼    ▼    ▼    ▼    ▼                                 │    │
│  │ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                    │    │
│  │ │C1│ │C2│ │C3│ │C4│ │C5│ │C6│ │C7│ │C8│                    │    │
│  │ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘                    │    │
│  │                                    IDLE  IDLE               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

Key: Consumer 수는 파티션 수를 초과하면 유휴 Consumer 발생
```

### 스케일링 공식

```
최대 병렬 처리 수 = min(파티션 수, Consumer 수)

예시:
- 파티션 6개, Consumer 3개 → 병렬 처리 3
- 파티션 6개, Consumer 6개 → 병렬 처리 6
- 파티션 6개, Consumer 8개 → 병렬 처리 6 (2개 유휴)
```

## 수평적 스케일링 (Scale Out)

### Kubernetes에서의 스케일링

```yaml
# notification-service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
spec:
  replicas: 3  # 3개의 Consumer 인스턴스
  template:
    spec:
      containers:
        - name: notification-service
          env:
            - name: SPRING_KAFKA_CONSUMER_GROUP_ID
              value: "notification-group"
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-service
  minReplicas: 2
  maxReplicas: 6  # 파티션 수에 맞춤
  metrics:
    - type: External
      external:
        metric:
          name: kafka_consumer_lag
          selector:
            matchLabels:
              consumer_group: notification-group
        target:
          type: AverageValue
          averageValue: "1000"  # Lag이 1000 초과시 스케일업
```

## 수직적 스케일링 (Scale Up)

### Concurrency 설정

단일 인스턴스 내에서 여러 스레드로 병렬 처리합니다.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    // 3개의 스레드로 병렬 처리
    factory.setConcurrency(3);

    factory.setConsumerFactory(consumerFactory);
    return factory;
}
```

### Listener별 Concurrency

```java
@KafkaListener(
    topics = "shopping.order.created",
    groupId = "notification-group",
    concurrency = "3"  // 이 리스너만 3개 스레드
)
public void handleOrderCreated(NotificationEvent event) {
    // 처리 로직
}
```

## Consumer Rebalancing

### Rebalancing이란?

Consumer Group 내의 파티션 할당이 변경되는 과정입니다.

```
Rebalancing 발생 시점:
1. Consumer 추가/제거
2. Consumer 장애 감지
3. 토픽 파티션 수 변경
4. Consumer 그룹 메타데이터 변경
```

### Rebalancing 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Rebalancing Process                               │
│                                                                      │
│  초기 상태 (3 Consumer, 6 Partition)                                │
│  C1: P0, P1 | C2: P2, P3 | C3: P4, P5                               │
│                                                                      │
│  Consumer 추가 (C4 join)                                            │
│  ─────────────────────────────────────                              │
│  1. C4가 Group에 join 요청                                          │
│  2. Group Coordinator가 rebalance 시작                              │
│  3. 모든 Consumer가 파티션 release                                  │
│  4. 새로운 파티션 할당                                              │
│     C1: P0, P1 | C2: P2 | C3: P3, P4 | C4: P5                       │
│                                                                      │
│  Consumer 제거 (C3 crash)                                           │
│  ─────────────────────────────────                                  │
│  1. session.timeout.ms 이후 C3 감지                                 │
│  2. Group Coordinator가 rebalance 시작                              │
│  3. C3의 파티션 재할당                                              │
│     C1: P0, P1, P3 | C2: P2, P4 | C4: P5                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Rebalancing 최적화

```java
// session.timeout.ms: Consumer 장애 감지 시간
props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);  // 30초

// heartbeat.interval.ms: Heartbeat 전송 간격
props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);  // 10초

// max.poll.interval.ms: poll 호출 최대 간격
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);  // 5분
```

### Cooperative Rebalancing

```java
// Eager (기본): 전체 파티션 재할당
// Cooperative: 변경되는 파티션만 재할당

props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
    CooperativeStickyAssignor.class.getName());
```

## 파티션 수 결정 가이드

### 권장 파티션 수 계산

```
예상 처리량 계산:
- 초당 메시지 수: 1000 msg/s
- 단일 Consumer 처리량: 200 msg/s
- 필요 Consumer 수: 1000 / 200 = 5
- 권장 파티션 수: 5 ~ 10 (여유 확보)
```

### 토픽별 파티션 설정

```bash
# 토픽 생성 시 파티션 수 지정
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic shopping.order.created \
  --partitions 6 \
  --replication-factor 3
```

## 모니터링 메트릭

### Consumer Lag 모니터링

```java
@Component
@RequiredArgsConstructor
public class ConsumerLagMonitor {

    private final KafkaListenerEndpointRegistry registry;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)  // 30초마다
    public void recordLag() {
        registry.getListenerContainerIds().forEach(id -> {
            MessageListenerContainer container = registry.getListenerContainer(id);

            Map<String, Map<Integer, Long>> lag = container.metrics();
            // lag 메트릭 기록
        });
    }
}
```

### 주요 모니터링 지표

| 메트릭 | 설명 | 임계값 예시 |
|--------|------|------------|
| Consumer Lag | 처리 대기 메시지 수 | > 10000 Warning |
| Records Consumed Rate | 초당 처리 메시지 수 | 예상 처리량의 80% 미만 시 확인 |
| Rebalance Rate | 분당 rebalance 횟수 | > 5/min Warning |
| Commit Latency | 커밋 지연 시간 | > 1초 Warning |

### Grafana 대시보드 쿼리

```promql
# Consumer Lag
sum by (consumer_group, topic) (
  kafka_consumer_lag{consumer_group="notification-group"}
)

# 처리 속도
rate(kafka_consumer_records_consumed_total[5m])

# Rebalance 횟수
increase(kafka_consumer_rebalances_total[1h])
```

## 스케일링 전략

### 점진적 스케일아웃

```yaml
# 1단계: 2개로 시작
replicas: 2

# 부하 증가 시 점진적 확장
# 2단계: 4개
# 3단계: 6개 (파티션 수와 동일)
```

### 토픽별 분리

```java
// 중요 토픽은 별도 Consumer Group으로 분리
@KafkaListener(
    topics = "shopping.payment.completed",
    groupId = "notification-payment-group"  // 전용 그룹
)
public void handlePayment(NotificationEvent event) { ... }

@KafkaListener(
    topics = "shopping.timedeal.started",
    groupId = "notification-timedeal-group"  // 전용 그룹
)
public void handleTimeDeal(NotificationEvent event) { ... }
```

### 자동 스케일링 전략

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Auto-scaling Decision Flow                        │
│                                                                      │
│  Consumer Lag 확인                                                   │
│        │                                                             │
│        ▼                                                             │
│  ┌─────────────┐                                                     │
│  │ Lag > 10000 │──Yes──▶ Scale Up (Replicas + 1)                    │
│  │    ?        │                                                     │
│  └─────────────┘                                                     │
│        │ No                                                          │
│        ▼                                                             │
│  ┌─────────────┐                                                     │
│  │ Lag < 100   │──Yes──▶ Scale Down (if Replicas > minReplicas)     │
│  │ for 10min?  │                                                     │
│  └─────────────┘                                                     │
│        │ No                                                          │
│        ▼                                                             │
│    유지 현상태                                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Best Practices

1. **파티션 수 미리 계획** - 나중에 늘리기는 쉬우나 줄이기 어려움
2. **Cooperative Rebalancing 사용** - Rebalancing 영향 최소화
3. **Consumer Lag 모니터링** - 스케일링 결정의 핵심 지표
4. **점진적 스케일링** - 급격한 변화 방지
5. **토픽별 전용 그룹 고려** - 중요 토픽 격리
6. **session.timeout 적절히 설정** - 장애 감지와 안정성 균형

## 관련 문서

- [consumer-architecture.md](./consumer-architecture.md) - Consumer 아키텍처
- [consumer-error-handling.md](./consumer-error-handling.md) - 에러 처리
- [retry-strategy.md](./retry-strategy.md) - 재시도 전략
