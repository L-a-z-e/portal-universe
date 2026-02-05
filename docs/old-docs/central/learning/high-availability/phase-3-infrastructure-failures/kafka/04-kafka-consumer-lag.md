# Kafka Consumer Lag 급증 시나리오

Consumer가 Producer를 따라잡지 못해 Lag이 급증하는 상황을 시뮬레이션합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 처리 지연 |
| **영향 범위** | 이벤트 처리 지연 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. Consumer Lag이란?

### 정의

```
Consumer Lag = 최신 Offset - Consumer가 읽은 Offset
```

### 영향

| Lag 수준 | 영향 | 심각도 |
|---------|------|--------|
| 0-100 | 정상 | 🟢 |
| 100-1000 | 약간 지연 | 🟡 |
| 1000-10000 | 심각한 지연 | 🟠 |
| 10000+ | 처리 불가 | 🔴 |

---

## 2. Consumer Lag 원인

1. **Consumer 성능 저하**: 처리 로직 병목
2. **Producer 급증**: 트래픽 스파이크
3. **Consumer 다운**: 인스턴스 감소
4. **네트워크 지연**: 브로커 통신 문제

---

## 3. 시뮬레이션

### Step 1: 현재 Lag 확인

```bash
# Consumer Group 목록
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Lag 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
```

### Step 2: Consumer 중지 (Lag 유발)

```bash
# Notification Service 중지 (Consumer 역할)
kubectl scale deployment notification-service -n portal-universe --replicas=0
```

### Step 3: 메시지 대량 발행

```bash
# 대량 메시지 발행 (테스트용)
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  bash -c 'for i in {1..10000}; do echo "{\"type\":\"test\",\"id\":$i}"; done | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic notifications'
```

### Step 4: Lag 확인

```bash
# Lag 급증 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-service
```

### Step 5: 복구

```bash
# Consumer 재시작
kubectl scale deployment notification-service -n portal-universe --replicas=1

# Lag 감소 확인 (시간 소요)
watch -n 5 'kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-service 2>/dev/null | tail -5'
```

---

## 4. 알림 설정

```yaml
- alert: KafkaConsumerLag
  expr: kafka_consumergroup_lag > 1000
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Consumer {{ $labels.consumergroup }} Lag 증가"
    description: "Topic {{ $labels.topic }}의 Lag: {{ $value }}"

- alert: KafkaConsumerLagCritical
  expr: kafka_consumergroup_lag > 10000
  for: 2m
  labels:
    severity: critical
```

---

## 5. 개선 방안

### Consumer 스케일링

```bash
# Consumer 인스턴스 증가
kubectl scale deployment notification-service --replicas=3
```

### 파티션 수 조정

```bash
# 파티션 증가 (Consumer 병렬 처리)
kafka-topics.sh --alter --topic notifications --partitions 6 --bootstrap-server localhost:9092
```

---

## 6. 체크리스트

- [ ] Consumer Lag 확인 방법 숙지
- [ ] Lag 급증 시뮬레이션
- [ ] 알림 규칙 설정
- [ ] 복구 및 스케일링 방법 확인

---

## 다음 섹션

[Redis 장애 시나리오](../redis/01-redis-oom.md)
