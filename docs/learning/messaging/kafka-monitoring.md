# Kafka Monitoring

Kafka 클러스터와 애플리케이션의 안정적인 운영을 위한 모니터링 전략을 다룹니다.

## 목차

1. [핵심 모니터링 메트릭](#1-핵심-모니터링-메트릭)
2. [Consumer Lag 모니터링](#2-consumer-lag-모니터링)
3. [JMX 메트릭](#3-jmx-메트릭)
4. [Prometheus + Grafana 연동](#4-prometheus--grafana-연동)
5. [모니터링 도구](#5-모니터링-도구)
6. [알람 설정 가이드](#6-알람-설정-가이드)

---

## 1. 핵심 모니터링 메트릭

### 1.1 Broker 레벨 메트릭

| 메트릭 | 설명 | 임계값 (권장) |
|--------|------|--------------|
| `UnderReplicatedPartitions` | 복제가 완료되지 않은 파티션 수 | > 0 주의 |
| `OfflinePartitionsCount` | 리더가 없는 파티션 수 | > 0 긴급 |
| `ActiveControllerCount` | 활성 Controller 수 | != 1 긴급 |
| `RequestHandlerAvgIdlePercent` | Request Handler 유휴율 | < 0.3 경고 |
| `NetworkProcessorAvgIdlePercent` | Network Processor 유휴율 | < 0.3 경고 |

### 1.2 Topic/Partition 레벨 메트릭

| 메트릭 | 설명 | 임계값 (권장) |
|--------|------|--------------|
| `MessagesInPerSec` | 초당 메시지 유입량 | 용량 계획 기준 |
| `BytesInPerSec` | 초당 유입 바이트 | 네트워크 대역폭 80% |
| `BytesOutPerSec` | 초당 유출 바이트 | 네트워크 대역폭 80% |
| `LogFlushRateAndTimeMs` | Log Flush 빈도 및 소요시간 | P99 > 100ms 경고 |

### 1.3 Producer 메트릭

| 메트릭 | 설명 | 임계값 (권장) |
|--------|------|--------------|
| `record-send-rate` | 초당 전송 레코드 수 | 비정상적 감소 시 경고 |
| `record-error-rate` | 초당 에러 발생률 | > 0 주의 |
| `request-latency-avg` | 평균 요청 지연시간 | > 100ms 경고 |
| `batch-size-avg` | 평균 배치 크기 | 설정값 대비 확인 |
| `buffer-available-bytes` | 사용 가능한 버퍼 크기 | < 10% 경고 |

### 1.4 Consumer 메트릭

| 메트릭 | 설명 | 임계값 (권장) |
|--------|------|--------------|
| `records-consumed-rate` | 초당 소비 레코드 수 | 생산량 대비 확인 |
| `records-lag` | Consumer Lag (레코드 수) | 지속적 증가 시 경고 |
| `records-lag-max` | 최대 Consumer Lag | 임계값 초과 시 긴급 |
| `fetch-latency-avg` | 평균 Fetch 지연시간 | > 500ms 경고 |
| `commit-latency-avg` | 평균 Commit 지연시간 | > 100ms 경고 |

---

## 2. Consumer Lag 모니터링

Consumer Lag은 Kafka 모니터링에서 가장 중요한 메트릭 중 하나입니다.

### 2.1 Consumer Lag 이해

```
Producer → [Partition] → Consumer
              ↑
         Log End Offset (LEO): 10000
         Consumer Offset: 9500
         ─────────────────────────
         Consumer Lag: 500
```

**Lag 계산 공식:**
```
Consumer Lag = Log End Offset (LEO) - Consumer Committed Offset
```

### 2.2 Lag 유형

| 유형 | 설명 | 원인 |
|------|------|------|
| **일시적 Lag** | 순간적으로 발생 후 감소 | 트래픽 급증, GC Pause |
| **지속적 Lag** | 계속 증가하는 Lag | Consumer 처리 속도 부족 |
| **고정 Lag** | 일정 수준에서 유지 | Consumer 중지, 장애 |

### 2.3 Kafka CLI로 Lag 확인

```bash
# Consumer Group의 Lag 확인
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group my-consumer-group

# 출력 예시
# GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# my-group        my-topic        0          1000            1500            500
# my-group        my-topic        1          2000            2100            100
# my-group        my-topic        2          1500            1500            0
```

```bash
# 모든 Consumer Group 목록 조회
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# 특정 Group의 상세 상태
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group my-consumer-group --verbose
```

### 2.4 Spring Boot에서 Lag 모니터링

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumerLagMonitor {

    private final KafkaAdmin kafkaAdmin;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000) // 30초마다 실행
    public void monitorConsumerLag() {
        try (AdminClient adminClient = AdminClient.create(
                kafkaAdmin.getConfigurationProperties())) {

            // Consumer Group 목록 조회
            ListConsumerGroupsResult groups = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groupListings =
                groups.all().get(10, TimeUnit.SECONDS);

            for (ConsumerGroupListing group : groupListings) {
                String groupId = group.groupId();

                // Consumer Group Offset 조회
                ListConsumerGroupOffsetsResult offsetsResult =
                    adminClient.listConsumerGroupOffsets(groupId);
                Map<TopicPartition, OffsetAndMetadata> offsets =
                    offsetsResult.partitionsToOffsetAndMetadata()
                        .get(10, TimeUnit.SECONDS);

                // Log End Offset 조회
                Set<TopicPartition> partitions = offsets.keySet();
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    adminClient.listOffsets(
                        partitions.stream().collect(Collectors.toMap(
                            tp -> tp,
                            tp -> OffsetSpec.latest()
                        ))
                    ).all().get(10, TimeUnit.SECONDS);

                // Lag 계산 및 메트릭 등록
                for (TopicPartition tp : partitions) {
                    long currentOffset = offsets.get(tp).offset();
                    long endOffset = endOffsets.get(tp).offset();
                    long lag = endOffset - currentOffset;

                    // Micrometer 메트릭 등록
                    Gauge.builder("kafka.consumer.lag", () -> lag)
                        .tag("group", groupId)
                        .tag("topic", tp.topic())
                        .tag("partition", String.valueOf(tp.partition()))
                        .register(meterRegistry);

                    if (lag > 1000) {
                        log.warn("High consumer lag detected - Group: {}, Topic: {}, " +
                                "Partition: {}, Lag: {}",
                            groupId, tp.topic(), tp.partition(), lag);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to monitor consumer lag", e);
        }
    }
}
```

### 2.5 Consumer Lag 대응 전략

```java
@Configuration
public class ConsumerLagAlertConfig {

    @Bean
    public ConsumerLagAlertHandler lagAlertHandler() {
        return new ConsumerLagAlertHandler();
    }
}

@Component
@Slf4j
public class ConsumerLagAlertHandler {

    private final Map<String, Long> lagHistory = new ConcurrentHashMap<>();

    public void handleLag(String groupId, String topic, int partition, long lag) {
        String key = String.format("%s-%s-%d", groupId, topic, partition);

        // Lag 추이 분석
        Long previousLag = lagHistory.put(key, lag);

        if (previousLag != null) {
            long lagDelta = lag - previousLag;

            if (lagDelta > 0 && lag > 10000) {
                // Lag이 증가하고 있고, 임계값 초과
                triggerScaleUp(groupId, topic);
            }
        }

        // 심각한 Lag
        if (lag > 100000) {
            triggerAlert(AlertLevel.CRITICAL, groupId, topic, partition, lag);
        } else if (lag > 50000) {
            triggerAlert(AlertLevel.WARNING, groupId, topic, partition, lag);
        }
    }

    private void triggerScaleUp(String groupId, String topic) {
        log.warn("Consider scaling up consumers for group: {}, topic: {}",
            groupId, topic);
        // Kubernetes HPA 트리거 또는 알림 발송
    }

    private void triggerAlert(AlertLevel level, String groupId,
                             String topic, int partition, long lag) {
        log.error("[{}] Consumer lag alert - Group: {}, Topic: {}, " +
                 "Partition: {}, Lag: {}",
            level, groupId, topic, partition, lag);
        // 알림 시스템 연동 (Slack, PagerDuty 등)
    }

    enum AlertLevel {
        WARNING, CRITICAL
    }
}
```

---

## 3. JMX 메트릭

### 3.1 JMX 활성화

**Kafka Broker JMX 설정:**
```bash
# kafka-server-start.sh 실행 전 환경 변수 설정
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=localhost"
export JMX_PORT=9999
```

**Docker Compose 설정:**
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_JMX_PORT: 9999
      KAFKA_JMX_HOSTNAME: kafka
      KAFKA_JMX_OPTS: >-
        -Dcom.sun.management.jmxremote
        -Dcom.sun.management.jmxremote.authenticate=false
        -Dcom.sun.management.jmxremote.ssl=false
    ports:
      - "9999:9999"
```

### 3.2 주요 JMX MBean 경로

#### Broker 메트릭

```
# Controller 상태
kafka.controller:type=KafkaController,name=ActiveControllerCount
kafka.controller:type=KafkaController,name=OfflinePartitionsCount

# Replica 상태
kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions
kafka.server:type=ReplicaManager,name=PartitionCount
kafka.server:type=ReplicaManager,name=LeaderCount

# Request 처리
kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec
kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec
kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec

# Network
kafka.network:type=RequestMetrics,name=RequestsPerSec,request=Produce
kafka.network:type=RequestMetrics,name=RequestsPerSec,request=FetchConsumer
kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce
```

#### Topic별 메트릭

```
# 특정 Topic의 메트릭
kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec,topic=my-topic
kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=my-topic
kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=my-topic

# Log 관련
kafka.log:type=Log,name=Size,topic=my-topic,partition=0
kafka.log:type=Log,name=NumLogSegments,topic=my-topic,partition=0
```

### 3.3 JMX Exporter 설정

**jmx_exporter_config.yml:**
```yaml
lowercaseOutputName: true
lowercaseOutputLabelNames: true

rules:
  # Broker 메트릭
  - pattern: kafka.server<type=(.+), name=(.+), topic=(.+)><>(.+)
    name: kafka_server_$1_$2
    type: GAUGE
    labels:
      topic: "$3"
      attribute: "$4"

  - pattern: kafka.server<type=(.+), name=(.+)><>(.+)
    name: kafka_server_$1_$2
    type: GAUGE
    labels:
      attribute: "$3"

  # Controller 메트릭
  - pattern: kafka.controller<type=(.+), name=(.+)><>Value
    name: kafka_controller_$1_$2
    type: GAUGE

  # Network 메트릭
  - pattern: kafka.network<type=(.+), name=(.+), request=(.+)><>(.+)
    name: kafka_network_$1_$2
    type: GAUGE
    labels:
      request: "$3"
      attribute: "$4"

  # Consumer Group 메트릭
  - pattern: kafka.server<type=(.+), name=(.+), clientId=(.+), topic=(.+), partition=(.+)><>Value
    name: kafka_server_$1_$2
    type: GAUGE
    labels:
      clientId: "$3"
      topic: "$4"
      partition: "$5"

  # Log 메트릭
  - pattern: kafka.log<type=(.+), name=(.+), topic=(.+), partition=(.+)><>Value
    name: kafka_log_$1_$2
    type: GAUGE
    labels:
      topic: "$3"
      partition: "$4"
```

### 3.4 Spring Boot JMX 메트릭 노출

```java
@Configuration
public class KafkaJmxConfig {

    @Bean
    public MBeanServer mBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Bean
    public JmxMeterRegistry jmxMeterRegistry(MeterRegistry registry) {
        return new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);
    }
}
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
    jmx:
      exposure:
        include: "*"
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

---

## 4. Prometheus + Grafana 연동

### 4.1 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Kafka     │────→│ JMX         │────→│ Prometheus  │
│   Broker    │     │ Exporter    │     │             │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
┌─────────────┐     ┌─────────────┐            │
│   Spring    │────→│ Actuator    │────────────┤
│   App       │     │ /prometheus │            │
└─────────────┘     └─────────────┘            │
                                               ▼
                                        ┌─────────────┐
                                        │   Grafana   │
                                        │  Dashboard  │
                                        └─────────────┘
```

### 4.2 Docker Compose 구성

```yaml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_JMX_PORT: 9999
      # ... 기타 Kafka 설정
    volumes:
      - ./jmx_exporter_config.yml:/etc/kafka/jmx_exporter_config.yml

  jmx-exporter:
    image: bitnami/jmx-exporter:latest
    command:
      - "5556"
      - /etc/jmx_exporter/config.yml
    volumes:
      - ./jmx_exporter_config.yml:/etc/jmx_exporter/config.yml
    ports:
      - "5556:5556"
    depends_on:
      - kafka

  prometheus:
    image: prom/prometheus:v2.47.0
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:10.1.0
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    ports:
      - "3000:3000"
    depends_on:
      - prometheus

volumes:
  prometheus_data:
  grafana_data:
```

### 4.3 Prometheus 설정

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - /etc/prometheus/rules/*.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

scrape_configs:
  # Kafka JMX Exporter
  - job_name: 'kafka'
    static_configs:
      - targets: ['jmx-exporter:5556']
        labels:
          cluster: 'portal-universe'

  # Spring Boot Applications
  - job_name: 'spring-apps'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'auth-service:8080'
          - 'blog-service:8080'
          - 'shopping-service:8080'
          - 'notification-service:8080'

  # Kafka Exporter (Consumer Lag 전용)
  - job_name: 'kafka-exporter'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### 4.4 Grafana Dashboard 구성

**주요 패널 구성:**

```json
{
  "dashboard": {
    "title": "Kafka Monitoring Dashboard",
    "panels": [
      {
        "title": "Messages In/Out Per Second",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(kafka_server_brokertopicmetrics_messagesinpersec[5m])) by (topic)",
            "legendFormat": "In - {{topic}}"
          },
          {
            "expr": "sum(rate(kafka_server_brokertopicmetrics_bytesoutpersec[5m])) by (topic)",
            "legendFormat": "Out - {{topic}}"
          }
        ]
      },
      {
        "title": "Consumer Lag by Group",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(kafka_consumer_lag) by (group, topic)",
            "legendFormat": "{{group}} - {{topic}}"
          }
        ]
      },
      {
        "title": "Under Replicated Partitions",
        "type": "stat",
        "targets": [
          {
            "expr": "kafka_server_replicamanager_underreplicatedpartitions"
          }
        ],
        "thresholds": {
          "mode": "absolute",
          "steps": [
            { "color": "green", "value": 0 },
            { "color": "red", "value": 1 }
          ]
        }
      },
      {
        "title": "Request Latency P99",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, rate(kafka_network_requestmetrics_totaltimems_bucket[5m]))",
            "legendFormat": "{{request}} P99"
          }
        ]
      }
    ]
  }
}
```

### 4.5 PromQL 쿼리 예제

```promql
# Topic별 초당 메시지 수
sum(rate(kafka_server_brokertopicmetrics_messagesinpersec[5m])) by (topic)

# Consumer Group별 총 Lag
sum(kafka_consumergroup_lag) by (consumergroup)

# Broker별 네트워크 사용량
sum(rate(kafka_server_brokertopicmetrics_bytesinpersec[5m])) by (instance)

# Partition Leader 분포
count(kafka_server_replicamanager_leadercount) by (instance)

# 5분간 평균 Request Latency
avg_over_time(kafka_network_requestmetrics_totaltimems_mean[5m])

# Under-replicated Partition이 있는 Topic
kafka_server_replicamanager_underreplicatedpartitions > 0

# Consumer Lag 증가율 (1시간 기준)
increase(kafka_consumergroup_lag[1h])
```

---

## 5. 모니터링 도구

### 5.1 Burrow

LinkedIn에서 개발한 Kafka Consumer Lag 모니터링 도구입니다.

**특징:**
- Consumer Lag의 추이를 분석하여 상태 판단
- HTTP API로 상태 조회 가능
- 다양한 Notifier 지원 (HTTP, Email)

**설치 및 설정:**

```yaml
# burrow.toml
[general]
pidfile = "/var/run/burrow.pid"
stdout-logfile = "/var/log/burrow.log"

[logging]
filename = "/var/log/burrow.log"
level = "info"

[zookeeper]
servers = ["zookeeper:2181"]
timeout = 6
root-path = "/burrow"

[client-profile.kafka-profile]
client-id = "burrow-client"
kafka-version = "2.8.0"

[cluster.local]
class-name = "kafka"
servers = ["kafka:9092"]
client-profile = "kafka-profile"
topic-refresh = 60
offset-refresh = 30

[consumer.local]
class-name = "kafka"
cluster = "local"
servers = ["kafka:9092"]
client-profile = "kafka-profile"
offsets-topic = "__consumer_offsets"
start-latest = true

[httpserver.default]
address = ":8000"
```

**Docker Compose:**
```yaml
services:
  burrow:
    image: linkedin/burrow:latest
    volumes:
      - ./burrow.toml:/etc/burrow/burrow.toml
    ports:
      - "8000:8000"
    depends_on:
      - kafka
      - zookeeper
```

**API 사용 예:**
```bash
# 클러스터 목록
curl http://localhost:8000/v3/kafka

# Consumer Group 목록
curl http://localhost:8000/v3/kafka/local/consumer

# 특정 Consumer Group 상태
curl http://localhost:8000/v3/kafka/local/consumer/my-consumer-group/status

# Lag 상태
curl http://localhost:8000/v3/kafka/local/consumer/my-consumer-group/lag
```

**Burrow 상태 코드:**
| 상태 | 설명 |
|------|------|
| OK | 정상 |
| WARNING | Lag 증가 추이 감지 |
| ERROR | 심각한 Lag 또는 Consumer 중지 |
| STOP | Consumer가 중지됨 |
| STALL | Offset이 변하지 않음 |

### 5.2 Kafka UI (Provectus)

웹 기반 Kafka 클러스터 관리 및 모니터링 도구입니다.

**설치:**
```yaml
services:
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
      KAFKA_CLUSTERS_0_METRICS_PORT: 9999
      # 여러 클러스터 설정 가능
      KAFKA_CLUSTERS_1_NAME: production
      KAFKA_CLUSTERS_1_BOOTSTRAPSERVERS: kafka-prod:9092
    depends_on:
      - kafka
```

**주요 기능:**
- Broker, Topic, Consumer Group 상태 조회
- 메시지 조회 및 생산
- Topic 생성/삭제/설정 변경
- Consumer Group Offset 리셋
- ACL 관리
- Schema Registry 연동

### 5.3 AKHQ (KafkaHQ)

```yaml
services:
  akhq:
    image: tchiotludo/akhq:latest
    environment:
      AKHQ_CONFIGURATION: |
        akhq:
          connections:
            local:
              properties:
                bootstrap.servers: "kafka:9092"
              schema-registry:
                url: "http://schema-registry:8081"
    ports:
      - "8080:8080"
```

### 5.4 Conduktor

상용 Kafka 관리 도구로, 무료 버전도 제공합니다.

**주요 기능:**
- 직관적인 UI
- 다중 클러스터 관리
- 스키마 관리
- 보안 설정 관리
- 데이터 마스킹

### 5.5 도구 비교

| 기능 | Kafka UI | AKHQ | Burrow | Conduktor |
|------|----------|------|--------|-----------|
| 가격 | 무료 | 무료 | 무료 | 유료/무료 |
| 클러스터 관리 | O | O | X | O |
| Consumer Lag | O | O | O (특화) | O |
| 메시지 조회 | O | O | X | O |
| Schema Registry | O | O | X | O |
| Topic 관리 | O | O | X | O |
| 알람 기능 | X | X | O | O |

---

## 6. 알람 설정 가이드

### 6.1 Prometheus Alerting Rules

**/etc/prometheus/rules/kafka_alerts.yml:**
```yaml
groups:
  - name: kafka_alerts
    interval: 30s
    rules:
      # 1. Broker 관련 알람
      - alert: KafkaBrokerDown
        expr: up{job="kafka"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Kafka broker is down"
          description: "Kafka broker {{ $labels.instance }} has been down for more than 1 minute."

      - alert: KafkaNoActiveController
        expr: kafka_controller_kafkacontroller_activecontrollercount != 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "No active Kafka controller"
          description: "There is no active Kafka controller in the cluster."

      # 2. Partition 관련 알람
      - alert: KafkaUnderReplicatedPartitions
        expr: kafka_server_replicamanager_underreplicatedpartitions > 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka has under-replicated partitions"
          description: "{{ $value }} partitions are under-replicated."

      - alert: KafkaOfflinePartitions
        expr: kafka_controller_kafkacontroller_offlinepartitionscount > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Kafka has offline partitions"
          description: "{{ $value }} partitions are offline."

      # 3. Consumer Lag 알람
      - alert: KafkaConsumerLagHigh
        expr: kafka_consumergroup_lag > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High consumer lag detected"
          description: "Consumer group {{ $labels.consumergroup }} has lag of {{ $value }} on topic {{ $labels.topic }}."

      - alert: KafkaConsumerLagCritical
        expr: kafka_consumergroup_lag > 100000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Critical consumer lag detected"
          description: "Consumer group {{ $labels.consumergroup }} has critical lag of {{ $value }} on topic {{ $labels.topic }}."

      - alert: KafkaConsumerLagIncreasing
        expr: increase(kafka_consumergroup_lag[30m]) > 5000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Consumer lag is continuously increasing"
          description: "Consumer group {{ $labels.consumergroup }} lag is increasing on topic {{ $labels.topic }}."

      # 4. 성능 관련 알람
      - alert: KafkaRequestLatencyHigh
        expr: kafka_network_requestmetrics_totaltimems_mean{request="Produce"} > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka request latency"
          description: "Produce request latency is {{ $value }}ms."

      - alert: KafkaNetworkProcessorIdleLow
        expr: kafka_server_kafkarequesthandlerpool_requesthandleravgidlepercent < 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka network processor idle is low"
          description: "Network processor idle is {{ $value }}."

      # 5. 디스크 관련 알람
      - alert: KafkaLogSizeGrowing
        expr: increase(kafka_log_log_size[1h]) > 1073741824  # 1GB
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Kafka log size is growing rapidly"
          description: "Topic {{ $labels.topic }} partition {{ $labels.partition }} grew by {{ humanize $value }} in the last hour."
```

### 6.2 Alertmanager 설정

**alertmanager.yml:**
```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default-receiver'

  routes:
    # Critical 알람은 즉시 발송
    - match:
        severity: critical
      receiver: 'critical-receiver'
      group_wait: 10s
      repeat_interval: 1h

    # Warning 알람은 그룹화 후 발송
    - match:
        severity: warning
      receiver: 'warning-receiver'
      group_wait: 5m
      repeat_interval: 4h

receivers:
  - name: 'default-receiver'
    slack_configs:
      - channel: '#kafka-alerts'
        send_resolved: true
        title: '[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
        text: >-
          *Alert:* {{ .GroupLabels.alertname }}
          *Severity:* {{ .CommonLabels.severity }}
          *Description:* {{ range .Alerts }}{{ .Annotations.description }}{{ end }}

  - name: 'critical-receiver'
    slack_configs:
      - channel: '#kafka-alerts-critical'
        send_resolved: true
    pagerduty_configs:
      - service_key: '<pagerduty-service-key>'
        severity: critical

  - name: 'warning-receiver'
    slack_configs:
      - channel: '#kafka-alerts'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'cluster']
```

### 6.3 Spring Boot 기반 알람 시스템

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaAlertService {

    private final WebClient slackWebClient;
    private final MeterRegistry meterRegistry;

    @Value("${alert.slack.webhook-url}")
    private String slackWebhookUrl;

    @Value("${alert.thresholds.consumer-lag.warning:10000}")
    private long consumerLagWarning;

    @Value("${alert.thresholds.consumer-lag.critical:100000}")
    private long consumerLagCritical;

    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();
    private final Duration alertCooldown = Duration.ofMinutes(15);

    public void checkAndAlert(String groupId, String topic,
                             int partition, long lag) {
        String alertKey = String.format("%s-%s-%d", groupId, topic, partition);

        // Cooldown 체크
        Instant lastAlert = lastAlertTime.get(alertKey);
        if (lastAlert != null &&
            lastAlert.plus(alertCooldown).isAfter(Instant.now())) {
            return; // 쿨다운 기간 내
        }

        AlertLevel level = determineAlertLevel(lag);
        if (level != null) {
            sendAlert(level, groupId, topic, partition, lag);
            lastAlertTime.put(alertKey, Instant.now());

            // 메트릭 기록
            meterRegistry.counter("kafka.alerts",
                "level", level.name(),
                "group", groupId,
                "topic", topic
            ).increment();
        }
    }

    private AlertLevel determineAlertLevel(long lag) {
        if (lag >= consumerLagCritical) {
            return AlertLevel.CRITICAL;
        } else if (lag >= consumerLagWarning) {
            return AlertLevel.WARNING;
        }
        return null;
    }

    private void sendAlert(AlertLevel level, String groupId,
                          String topic, int partition, long lag) {
        SlackMessage message = SlackMessage.builder()
            .channel("#kafka-alerts")
            .username("Kafka Monitor")
            .iconEmoji(level == AlertLevel.CRITICAL ? ":rotating_light:" : ":warning:")
            .attachments(List.of(
                SlackAttachment.builder()
                    .color(level == AlertLevel.CRITICAL ? "danger" : "warning")
                    .title(String.format("[%s] Consumer Lag Alert", level))
                    .fields(List.of(
                        new SlackField("Consumer Group", groupId, true),
                        new SlackField("Topic", topic, true),
                        new SlackField("Partition", String.valueOf(partition), true),
                        new SlackField("Lag", String.format("%,d", lag), true)
                    ))
                    .footer("Kafka Monitoring System")
                    .timestamp(Instant.now().getEpochSecond())
                    .build()
            ))
            .build();

        slackWebClient.post()
            .uri(slackWebhookUrl)
            .bodyValue(message)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(e -> log.error("Failed to send Slack alert", e))
            .subscribe();

        log.warn("[{}] Kafka alert sent - Group: {}, Topic: {}, Partition: {}, Lag: {}",
            level, groupId, topic, partition, lag);
    }

    enum AlertLevel {
        WARNING, CRITICAL
    }
}

@Data
@Builder
class SlackMessage {
    private String channel;
    private String username;
    private String iconEmoji;
    private List<SlackAttachment> attachments;
}

@Data
@Builder
class SlackAttachment {
    private String color;
    private String title;
    private List<SlackField> fields;
    private String footer;
    private Long timestamp;
}

@Data
@AllArgsConstructor
class SlackField {
    private String title;
    private String value;
    @JsonProperty("short")
    private boolean shortField;
}
```

### 6.4 알람 임계값 가이드라인

| 메트릭 | Warning | Critical | 비고 |
|--------|---------|----------|------|
| Consumer Lag | > 10,000 | > 100,000 | 처리량에 따라 조정 |
| Under Replicated Partitions | > 0 (5분) | > 0 (1분) | 즉시 확인 필요 |
| Offline Partitions | - | > 0 | 즉시 대응 필요 |
| Request Latency (P99) | > 100ms | > 500ms | SLA에 따라 조정 |
| Broker Disk Usage | > 70% | > 85% | 용량 확장 계획 |
| Network Idle | < 30% | < 10% | 스케일 아웃 검토 |

### 6.5 Runbook 예시

**Consumer Lag 급증 대응:**
```markdown
## Alert: KafkaConsumerLagCritical

### 증상
- Consumer Group의 Lag이 임계값(100,000)을 초과

### 진단 단계
1. Consumer 상태 확인
   ```bash
   kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
     --describe --group <group-id>
   ```

2. Consumer Pod 상태 확인 (K8s)
   ```bash
   kubectl get pods -l app=<consumer-app>
   kubectl logs -f <pod-name>
   ```

3. 메시지 처리 시간 확인
   - Grafana에서 처리 latency 확인
   - 에러 로그 확인

### 대응 방안
1. **Consumer 장애 시**: Pod 재시작
2. **처리 속도 부족 시**: Consumer 스케일 아웃
3. **특정 메시지 문제 시**: Dead Letter Queue로 이동

### 스케일 아웃
```bash
kubectl scale deployment <consumer-app> --replicas=<new-count>
```

### 에스컬레이션
- 30분 내 해결 불가 시 → 팀 리더
- 1시간 내 해결 불가 시 → 온콜 엔지니어
```

---

## 참고 자료

- [Apache Kafka Documentation - Monitoring](https://kafka.apache.org/documentation/#monitoring)
- [Confluent Kafka Monitoring Guide](https://docs.confluent.io/platform/current/kafka/monitoring.html)
- [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter)
- [Burrow GitHub Repository](https://github.com/linkedin/Burrow)
- [Kafka UI GitHub Repository](https://github.com/provectus/kafka-ui)
