# Kafka 파티션 손실 시나리오

Kafka 파티션 데이터가 손실되었을 때의 영향과 복구 방법을 학습합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 데이터 손실 |
| **영향 범위** | 특정 토픽/파티션 |
| **난이도** | ⭐⭐⭐⭐ |
| **예상 시간** | 45분 |

---

## 1. 파티션 손실 원인

### 일반적인 원인

1. **디스크 장애**: 물리 디스크 고장
2. **PVC 삭제**: 실수로 PersistentVolumeClaim 삭제
3. **로그 디렉토리 손상**: 파일 시스템 오류
4. **잘못된 설정**: retention 설정 오류

### 현재 Portal Universe 위험

```yaml
# 현재 설정 (위험)
env:
  - name: KAFKA_LOG_DIRS
    value: "/tmp/kraft-kafka-logs"  # ⚠️ emptyDir, Pod 재시작 시 손실
```

---

## 2. 시뮬레이션

### 주의

> ⚠️ 이 시나리오는 데이터 손실을 발생시킵니다. 테스트 환경에서만 실행하세요.

### Step 1: 테스트 토픽 생성

```bash
# 테스트 토픽 생성
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-topics.sh --create --topic test-partition-loss \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

# 테스트 메시지 발행
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  bash -c 'for i in {1..100}; do echo "test-message-$i"; done | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-partition-loss'
```

### Step 2: 파티션 데이터 확인

```bash
# 메시지 수 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic test-partition-loss
```

### Step 3: Pod 삭제 (데이터 손실)

```bash
# emptyDir 사용 중이므로 Pod 삭제 시 데이터 손실
kubectl delete pod -n portal-universe -l app=kafka

# 새 Pod 시작 대기
kubectl get pods -n portal-universe -l app=kafka -w
```

### Step 4: 데이터 손실 확인

```bash
# 토픽 확인 (토픽은 유지되지만 데이터 없음)
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-topics.sh --describe --topic test-partition-loss --bootstrap-server localhost:9092

# 메시지 없음 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-partition-loss --from-beginning --timeout-ms 5000
```

---

## 3. 개선 방안

### PersistentVolume 사용

```yaml
# k8s/infrastructure/kafka.yaml 수정
spec:
  template:
    spec:
      containers:
        - name: kafka
          volumeMounts:
            - name: kafka-data
              mountPath: /var/kafka-logs
          env:
            - name: KAFKA_LOG_DIRS
              value: "/var/kafka-logs"

  volumeClaimTemplates:
    - metadata:
        name: kafka-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
```

### 복제 설정

```yaml
env:
  - name: KAFKA_DEFAULT_REPLICATION_FACTOR
    value: "3"
  - name: KAFKA_MIN_INSYNC_REPLICAS
    value: "2"
```

---

## 4. 체크리스트

- [ ] 현재 저장소 설정 확인 (emptyDir vs PVC)
- [ ] 테스트 토픽으로 시뮬레이션
- [ ] 데이터 손실 확인
- [ ] PVC 기반 스토리지 개선 계획

---

## 다음 시나리오

[04-kafka-consumer-lag.md](./04-kafka-consumer-lag.md) - Consumer Lag 급증 시나리오
