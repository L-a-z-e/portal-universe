# Kafka 메모리 부족 (OOM) 시나리오

Kafka가 메모리 부족으로 OOM(Out of Memory) 상황이 되었을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 리소스 고갈 |
| **영향 범위** | Kafka 서비스 전체 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 현재 설정 분석

### 리소스 설정

**파일**: `k8s/infrastructure/kafka.yaml`

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### OOM 발생 조건

- 힙 메모리 + OS 메모리 > 1Gi
- 대량 메시지 적재
- 많은 파티션/토픽
- 느린 Consumer로 인한 메시지 누적

---

## 2. 예상 영향

### OOM 발생 시 증상

1. **Kafka 프로세스 강제 종료**
2. **Pod 상태**: OOMKilled
3. **재시작 반복**: CrashLoopBackOff
4. **데이터 무결성**: 커밋되지 않은 메시지 유실 가능

### 타임라인

```
메모리 사용률 증가 → 90% 도달 → OOM Killer 실행 → Pod 재시작
    ↓                  ↓             ↓              ↓
  정상 동작         지연 발생      즉시 종료      복구 시도
```

---

## 3. 장애 주입 절차

### Step 1: 현재 메모리 사용량 확인

```bash
# Pod 메모리 사용량
kubectl top pod -n portal-universe -l app=kafka

# Kafka 힙 설정 확인
kubectl exec -it $(kubectl get pod -n portal-universe -l app=kafka -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  env | grep -i heap
```

### Step 2: 메모리 제한 낮추기

```bash
# 메모리 제한을 256Mi로 낮춤 (OOM 유발)
kubectl patch deployment kafka -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"kafka","resources":{"limits":{"memory":"256Mi"}}}]}}}}'
```

### Step 3: OOM 관찰

```bash
# Pod 상태 모니터링
kubectl get pods -n portal-universe -l app=kafka -w

# 예상 출력:
# NAME       READY   STATUS      RESTARTS   AGE
# kafka-xxx  1/1     Running     0          10s
# kafka-xxx  0/1     OOMKilled   0          30s
# kafka-xxx  0/1     CrashLoopBackOff   1   45s

# OOM 이벤트 확인
kubectl describe pod -n portal-universe -l app=kafka | grep -A5 "Last State"
```

### Step 4: 복구

```bash
# 원래 메모리 제한으로 복원
kubectl patch deployment kafka -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"kafka","resources":{"limits":{"memory":"1Gi"}}}]}}}}'

# 복구 확인
kubectl get pods -n portal-universe -l app=kafka -w
```

---

## 4. 결과 분석

### OOMKilled 확인 방법

```bash
# Pod 상태 확인
kubectl get pod <pod-name> -n portal-universe -o jsonpath='{.status.containerStatuses[0].lastState.terminated.reason}'
# 출력: OOMKilled

# 이벤트 확인
kubectl get events -n portal-universe --field-selector reason=OOMKilled
```

### 메트릭 패턴

```
container_memory_usage_bytes:
  정상: ~600MB
  OOM 직전: ~1GB (limit)
  OOM 후: 0 (Pod 종료)

kube_pod_container_status_restarts_total:
  OOM 발생마다 증가
```

---

## 5. 개선 방안

### 1. 적절한 메모리 설정

```yaml
resources:
  requests:
    memory: "1Gi"    # 최소 필요량
  limits:
    memory: "2Gi"    # 여유 확보

env:
  - name: KAFKA_HEAP_OPTS
    value: "-Xms512m -Xmx1g"  # 힙 크기 명시
```

### 2. 메모리 모니터링 알림

```yaml
- alert: KafkaHighMemoryUsage
  expr: |
    container_memory_usage_bytes{container="kafka"}
    /
    container_spec_memory_limit_bytes{container="kafka"}
    > 0.85
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Kafka 메모리 사용량 85% 초과"
```

### 3. 메시지 보존 정책

```yaml
env:
  - name: KAFKA_LOG_RETENTION_HOURS
    value: "24"  # 오래된 메시지 자동 삭제
  - name: KAFKA_LOG_RETENTION_BYTES
    value: "1073741824"  # 1GB 제한
```

---

## 6. 체크리스트

- [ ] OOM 발생 조건 이해
- [ ] OOMKilled 상태 확인 방법 숙지
- [ ] 메모리 제한 설정 방법 확인
- [ ] 복구 절차 실행
- [ ] 결과 문서화

---

## 다음 시나리오

[03-kafka-partition-loss.md](./03-kafka-partition-loss.md) - Kafka 파티션 손실 시나리오
