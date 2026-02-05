# Kafka 브로커 다운 시나리오

Kafka 브로커가 다운되었을 때의 영향을 분석하고 대응 방법을 학습합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 인프라 - 메시지 브로커 |
| **영향 범위** | 이벤트 기반 통신 전체 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30-45분 |

---

## 1. 현재 설정 분석

### Portal Universe Kafka 설정

**파일**: `k8s/infrastructure/kafka.yaml`

```yaml
spec:
  replicas: 1  # ⚠️ 단일 브로커 (SPOF)

env:
  - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
    value: "1"  # ⚠️ 복제 없음
  - name: KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
    value: "1"  # ⚠️ 복제 없음
  - name: KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
    value: "1"
```

### 문제점

| 설정 | 현재 | 위험 |
|------|------|------|
| 브로커 수 | 1 | 단일 장애점 |
| Replication Factor | 1 | 데이터 복제 없음 |
| Min ISR | 1 | 내구성 보장 없음 |

---

## 2. 예상 영향

### 영향받는 서비스

```
[Kafka 다운]
    │
    ├── Shopping Service (Producer)
    │   └── 주문 이벤트 발행 실패
    │       └── TimeoutException
    │
    ├── Notification Service (Consumer)
    │   └── 이벤트 수신 중단
    │       └── 알림 미발송
    │
    └── Prism Service (Producer/Consumer)
        └── AI 작업 이벤트 처리 중단
```

### 사용자 영향

| 기능 | 영향 | 심각도 |
|------|------|--------|
| 주문 완료 후 알림 | 미발송 | 중 |
| 재고 업데이트 이벤트 | 지연/유실 | 중 |
| 비동기 작업 | 중단 | 중 |
| 동기 API (주문, 결제) | 정상 | - |

---

## 3. 정상 상태 가설

```yaml
steady_state:
  - name: "Kafka 가용성"
    metric: "kafka_brokers > 0"
    expected: true

  - name: "Producer 성공률"
    metric: "kafka_producer_record_send_total{result='success'} / kafka_producer_record_send_total"
    expected: ">= 0.99"

  - name: "Consumer Lag"
    metric: "kafka_consumergroup_lag"
    expected: "< 1000"
```

---

## 4. 장애 주입 절차

### Step 1: 사전 준비

```bash
# 현재 상태 확인
kubectl get pods -n portal-universe -l app=kafka
# NAME       READY   STATUS    RESTARTS   AGE
# kafka-xxx  1/1     Running   0          1d

# Kafka 브로커 상태 확인
kubectl exec -it kafka-xxx -n portal-universe -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092 2>&1 | head -5

# 토픽 목록 확인
kubectl exec -it kafka-xxx -n portal-universe -- \
  kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Step 2: 모니터링 준비

```bash
# 터미널 1: Pod 모니터링
watch -n 2 'kubectl get pods -n portal-universe -l app=kafka'

# 터미널 2: Shopping Service 로그
kubectl logs -f deployment/shopping-service -n portal-universe | grep -i kafka

# 터미널 3: Notification Service 로그
kubectl logs -f deployment/notification-service -n portal-universe | grep -i kafka
```

### Step 3: 장애 주입

```bash
# Kafka Pod 삭제
kubectl delete pod -n portal-universe -l app=kafka

# 삭제 시간 기록
echo "Kafka Pod 삭제: $(date)"
```

### Step 4: 영향 관찰

예상 로그 패턴:

**Shopping Service (Producer)**:
```
WARN  [Producer clientId=shopping-producer] Connection to node 1 could not be established.
ERROR [Producer clientId=shopping-producer] Expiring 3 record(s) for orders-0: 30000 ms has passed since batch creation
org.apache.kafka.common.errors.TimeoutException: Expiring 3 record(s) for orders-0
```

**Notification Service (Consumer)**:
```
WARN  [Consumer clientId=notification-consumer] Connection to node 1 could not be established.
INFO  [Consumer clientId=notification-consumer] Disconnected from node 1
```

### Step 5: 복구 확인

```bash
# 새 Pod 상태 확인
kubectl get pods -n portal-universe -l app=kafka -w
# NAME       READY   STATUS              RESTARTS   AGE
# kafka-xxx  0/1     ContainerCreating   0          5s
# kafka-xxx  1/1     Running             0          45s

# Kafka Ready 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092 2>&1 | head -3

# 서비스 정상화 확인
kubectl logs deployment/shopping-service -n portal-universe --tail=20 | grep -i kafka
```

---

## 5. 결과 분석

### 예상 타임라인

| 시간 | 이벤트 |
|------|--------|
| T+0s | Kafka Pod 삭제 |
| T+5s | Producer 연결 실패 시작 |
| T+10s | Consumer Disconnect |
| T+30s | Producer TimeoutException |
| T+45s | 새 Kafka Pod Running |
| T+60s | Producer/Consumer 재연결 |
| T+90s | 정상화 |

### 메트릭 변화

```
요청 전: kafka_brokers = 1
장애 중: kafka_brokers = 0
복구 후: kafka_brokers = 1

Producer 성공률:
요청 전: 100%
장애 중: 0%
복구 후: 100%

Consumer Lag:
요청 전: 0
장애 중: 증가
복구 후: 점진적 감소
```

---

## 6. 복구 절차 (Runbook)

### 자동 복구 (Kubernetes)

Kubernetes가 자동으로 새 Pod를 생성합니다.

```bash
# 상태 확인
kubectl get pods -n portal-universe -l app=kafka -w

# 예상 시간: 30-60초
```

### 수동 복구

자동 복구 실패 시:

```bash
# 1. Deployment 재시작
kubectl rollout restart deployment/kafka -n portal-universe

# 2. 복구 확인
kubectl rollout status deployment/kafka -n portal-universe

# 3. Kafka 상태 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### 데이터 복구

메시지 유실 가능성 확인:

```bash
# Consumer Lag 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
```

---

## 7. 개선 방안

### 즉시 적용 가능

**1. Producer 재시도 설정 확인**

```java
// application.yml
spring:
  kafka:
    producer:
      retries: 3
      acks: all
      properties:
        retry.backoff.ms: 1000
        delivery.timeout.ms: 120000
```

**2. Consumer 재연결 설정**

```yaml
spring:
  kafka:
    consumer:
      properties:
        reconnect.backoff.ms: 1000
        reconnect.backoff.max.ms: 10000
```

### Phase 4에서 적용

**3-노드 Kafka 클러스터 구성**

[→ Phase 4: Kafka 복제](../../phase-4-ha-architecture/04-kafka-replication.md)

```yaml
# 목표 설정
replicas: 3
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: "3"
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: "3"
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: "2"
```

---

## 8. 관련 알림 규칙

```yaml
# monitoring/prometheus/rules/kafka-alerts.yml
groups:
  - name: kafka-alerts
    rules:
      - alert: KafkaBrokerDown
        expr: kafka_brokers < 1
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "Kafka 브로커 다운"
          description: "Kafka 브로커가 없습니다. 이벤트 발행/소비 불가."

      - alert: KafkaProducerErrors
        expr: rate(kafka_producer_record_error_total[5m]) > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Kafka Producer 에러 발생"
```

---

## 9. 체크리스트

### 실험 전

- [ ] Kafka Pod Running 상태 확인
- [ ] Producer/Consumer 정상 동작 확인
- [ ] 모니터링 대시보드 준비
- [ ] 롤백 명령어 준비

### 실험 중

- [ ] Pod 상태 변화 관찰
- [ ] 로그 에러 패턴 확인
- [ ] 메트릭 변화 기록

### 실험 후

- [ ] 새 Pod Running 확인
- [ ] Producer/Consumer 재연결 확인
- [ ] Consumer Lag 정상화 확인
- [ ] 결과 문서화

---

## 핵심 정리

1. **단일 Kafka 브로커**는 SPOF입니다
2. **Producer TimeoutException**이 30초 후 발생합니다
3. **Kubernetes 자동 복구**는 약 45-60초 소요됩니다
4. **메시지 유실 가능성**이 있습니다
5. **3-노드 클러스터**로 개선해야 합니다

---

## 다음 시나리오

[02-kafka-memory-oom.md](./02-kafka-memory-oom.md) - Kafka 메모리 부족 시나리오
