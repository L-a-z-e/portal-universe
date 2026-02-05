# Multi-AZ 배포

여러 가용 영역에 분산 배포하여 재해 복구 능력을 확보합니다.

---

## 학습 목표

- [ ] 가용 영역(AZ) 개념을 이해한다
- [ ] Pod를 여러 AZ에 분산하는 방법을 이해한다
- [ ] Zone 장애에 대응하는 아키텍처를 설계할 수 있다

---

## 1. 가용 영역(Availability Zone)이란?

### 정의

- 독립된 전원, 네트워크, 냉각을 갖춘 데이터센터(또는 데이터센터 클러스터)
- 한 AZ 장애가 다른 AZ에 영향 없음
- 같은 리전 내 AZ 간 지연시간 < 2ms

### AWS 예시

```
Region: ap-northeast-2 (서울)
├── AZ: ap-northeast-2a
├── AZ: ap-northeast-2b
└── AZ: ap-northeast-2c
```

---

## 2. Multi-AZ 분산

### Pod Anti-Affinity (Zone 기반)

```yaml
spec:
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: api-gateway
                topologyKey: topology.kubernetes.io/zone  # Zone 분산
```

### Topology Spread Constraints

```yaml
spec:
  template:
    spec:
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: api-gateway
```

---

## 3. 데이터 계층 Multi-AZ

### MySQL

```
Primary (AZ-A) → Replica (AZ-B)
                 Replica (AZ-C)
```

### Redis Sentinel

```
Master (AZ-A)
Slave (AZ-B)
Slave (AZ-C)
Sentinel (각 AZ에 1개)
```

### Kafka

```
Broker 1 (AZ-A)
Broker 2 (AZ-B)
Broker 3 (AZ-C)

rack.id로 AZ 인식 → ISR이 여러 AZ에 분산
```

---

## 4. Kind에서의 제한

> Kind는 단일 머신에서 실행되므로 실제 Multi-AZ 테스트는 불가능합니다.
> 프로덕션 환경(EKS, GKE, AKS)에서 적용하세요.

### 시뮬레이션 (노드 레이블)

```bash
# 노드에 Zone 레이블 추가 (시뮬레이션)
kubectl label node kind-worker topology.kubernetes.io/zone=zone-a
kubectl label node kind-worker2 topology.kubernetes.io/zone=zone-b
```

---

## 5. 효과

| 항목 | Single-AZ | Multi-AZ |
|------|-----------|----------|
| AZ 장애 | 전체 중단 | 부분 영향 |
| 가용성 | ~99.9% | ~99.99% |
| 비용 | 낮음 | 높음 (데이터 전송) |

---

## Phase 4 완료!

다음 단계로 진행합니다.

[Phase 5: 운영 가이드](../phase-5-operations/01-runbook-template.md)
