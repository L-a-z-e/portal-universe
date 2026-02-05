# Kafka 3-노드 클러스터 구성

Kafka를 3-노드 클러스터로 구성하여 고가용성을 확보합니다.

---

## 학습 목표

- [ ] Kafka 복제 개념을 이해한다
- [ ] 3-노드 클러스터를 구성할 수 있다
- [ ] 복제 설정을 검증할 수 있다

---

## 1. Kafka 복제 개념

### Replication Factor

```
Topic: orders
Partition 0: [Broker1(Leader)] [Broker2] [Broker3]
Partition 1: [Broker2(Leader)] [Broker3] [Broker1]
Partition 2: [Broker3(Leader)] [Broker1] [Broker2]

Replication Factor = 3 (각 파티션이 3개 브로커에 복제)
```

### ISR (In-Sync Replicas)

동기화된 복제본 집합. Min ISR = 2이면 최소 2개 동기화 필요.

---

## 2. 3-노드 클러스터 설정

### StatefulSet 사용 (권장)

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: portal-universe
spec:
  serviceName: kafka
  replicas: 3
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: apache/kafka:4.1.0
          env:
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "3"
            - name: KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
              value: "3"
            - name: KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
              value: "2"
            - name: KAFKA_DEFAULT_REPLICATION_FACTOR
              value: "3"
            - name: KAFKA_MIN_INSYNC_REPLICAS
              value: "2"
          volumeMounts:
            - name: kafka-data
              mountPath: /var/kafka-logs
  volumeClaimTemplates:
    - metadata:
        name: kafka-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
```

### Headless Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: portal-universe
spec:
  clusterIP: None  # Headless
  selector:
    app: kafka
  ports:
    - port: 9092
      name: plaintext
```

---

## 3. 복제 검증

```bash
# 토픽 복제 확인
kubectl exec -it kafka-0 -n portal-universe -- \
  kafka-topics.sh --describe --bootstrap-server localhost:9092

# ISR 확인
# Isr: 1,2,3 (3개 브로커 모두 동기화됨)
```

---

## 4. 장애 테스트

```bash
# 브로커 1개 다운
kubectl delete pod kafka-1 -n portal-universe

# 서비스 지속 확인
kubectl exec -it kafka-0 -n portal-universe -- \
  kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test

# 리더 재선출 확인
kubectl exec -it kafka-0 -n portal-universe -- \
  kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic test
```

---

## 5. 효과

| 항목 | 단일 노드 | 3-노드 클러스터 |
|------|----------|----------------|
| 브로커 장애 | 서비스 중단 | 자동 Failover |
| 데이터 내구성 | 유실 위험 | 복제로 보장 |
| 처리량 | 제한적 | 확장 가능 |

---

## 다음 단계

[05-redis-sentinel.md](./05-redis-sentinel.md) - Redis Sentinel 구성
