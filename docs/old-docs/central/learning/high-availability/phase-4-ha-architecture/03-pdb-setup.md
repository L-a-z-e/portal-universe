# PDB (Pod Disruption Budget) 설정

유지보수 중에도 최소 가용성을 보장하는 PDB를 설정합니다.

---

## 학습 목표

- [ ] PDB의 목적과 동작을 이해한다
- [ ] 적절한 PDB를 설정할 수 있다
- [ ] Drain 시 PDB 동작을 확인할 수 있다

---

## 1. PDB란?

### 정의

> PDB는 자발적 중단(Voluntary Disruption) 시 최소한의 Pod 수를 보장합니다.

### 자발적 중단 vs 비자발적 중단

| 유형 | 예시 | PDB 적용 |
|------|------|----------|
| **자발적** | 노드 drain, 업그레이드 | ✅ 적용 |
| **비자발적** | OOM, 하드웨어 장애 | ❌ 미적용 |

---

## 2. PDB 설정

### API Gateway PDB

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: api-gateway-pdb
  namespace: portal-universe
spec:
  minAvailable: 1  # 최소 1개 항상 가용
  selector:
    matchLabels:
      app: api-gateway
```

### 또는 maxUnavailable 사용

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: auth-service-pdb
  namespace: portal-universe
spec:
  maxUnavailable: 1  # 최대 1개까지 중단 허용
  selector:
    matchLabels:
      app: auth-service
```

---

## 3. 권장 설정

| 서비스 | replicas | PDB 설정 | 의미 |
|--------|----------|----------|------|
| api-gateway | 3 | minAvailable: 2 | 2개 이상 유지 |
| auth-service | 2 | minAvailable: 1 | 1개 이상 유지 |
| shopping | 2 | maxUnavailable: 1 | 1개 중단 허용 |

---

## 4. 적용 및 확인

```bash
# PDB 적용
kubectl apply -f k8s/pdb/

# PDB 상태 확인
kubectl get pdb -n portal-universe
```

출력 예시:
```
NAME              MIN AVAILABLE   MAX UNAVAILABLE   ALLOWED DISRUPTIONS   AGE
api-gateway-pdb   2               N/A               1                     1m
```

---

## 5. 테스트: 노드 Drain

```bash
# 노드 Drain 시도
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# PDB 위반 시 대기
# "Cannot evict pod as it would violate the pod's disruption budget"
```

---

## 6. 주의사항

### replicas와 PDB 관계

```yaml
# ❌ 잘못된 설정 (항상 0 disruptions allowed)
replicas: 1
minAvailable: 1

# ✅ 올바른 설정
replicas: 2
minAvailable: 1  # 1개 eviction 가능
```

---

## 다음 단계

[04-kafka-replication.md](./04-kafka-replication.md) - Kafka 클러스터 구성
