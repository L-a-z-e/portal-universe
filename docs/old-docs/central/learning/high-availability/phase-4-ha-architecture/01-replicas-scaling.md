# Replicas 확장

서비스 가용성을 위한 기본적인 replicas 확장을 학습합니다.

---

## 학습 목표

- [ ] replicas의 의미와 효과를 이해한다
- [ ] 적절한 replicas 수를 결정할 수 있다
- [ ] Rolling Update로 무중단 배포를 할 수 있다

---

## 1. 왜 replicas > 1 인가?

### 단일 인스턴스의 문제

```yaml
spec:
  replicas: 1  # ⚠️ 단일 장애점
```

- Pod 재시작 시 서비스 중단
- 무중단 배포 불가
- 부하 분산 불가

### 이중화의 효과

```yaml
spec:
  replicas: 2  # ✅ 기본 이중화
```

- 한 Pod 장애 시에도 서비스 지속
- Rolling Update로 무중단 배포
- 부하 분산

---

## 2. 권장 replicas 수

| 서비스 유형 | 최소 | 권장 | 이유 |
|------------|------|------|------|
| API Gateway | 2 | 3 | 진입점, 높은 가용성 필요 |
| 인증 서비스 | 2 | 2-3 | 핵심 기능 |
| 비즈니스 서비스 | 2 | 2 | 기본 이중화 |
| 배치 서비스 | 1 | 1 | 단일 실행 필요 |

---

## 3. 적용 방법

### 방법 1: kubectl 명령어

```bash
# 즉시 확장
kubectl scale deployment api-gateway -n portal-universe --replicas=2
kubectl scale deployment auth-service -n portal-universe --replicas=2
kubectl scale deployment shopping-service -n portal-universe --replicas=2

# 확인
kubectl get deployments -n portal-universe
```

### 방법 2: YAML 수정

```yaml
# k8s/services/api-gateway.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 2  # 1 → 2로 변경
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 추가 생성 가능 Pod 수
      maxUnavailable: 0  # 항상 최소 replicas 유지
```

---

## 4. Rolling Update 설정

### 무중단 배포 전략

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 동시에 1개 추가 생성 가능
      maxUnavailable: 0  # 항상 최소 replicas 유지 (무중단)
```

### 배포 과정

```
replicas: 2, maxSurge: 1, maxUnavailable: 0

시작:   [v1] [v1]           (2개 Running)
Step 1: [v1] [v1] [v2]      (v2 생성)
Step 2: [v1] [v2] [v2]      (v1 하나 종료)
Step 3: [v2] [v2]           (완료)
```

---

## 5. Pod Anti-Affinity

같은 서비스의 Pod가 다른 노드에 배치되도록 설정합니다.

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
                topologyKey: kubernetes.io/hostname
```

---

## 6. 확인 방법

```bash
# Pod 분산 확인
kubectl get pods -n portal-universe -l app=api-gateway -o wide

# 노드별 Pod 확인
kubectl get pods -n portal-universe -o wide | grep -E "NODE|api-gateway"
```

---

## 7. 주의사항

### 리소스 확인

```bash
# replicas 증가 전 노드 리소스 확인
kubectl top nodes
kubectl describe nodes | grep -A5 "Allocated resources"
```

### Stateless 확인

- 세션: Redis 등 외부 저장소 사용
- 파일: 공유 스토리지 또는 객체 스토리지 사용
- 상태: 데이터베이스에 저장

---

## 다음 단계

[02-hpa-setup.md](./02-hpa-setup.md) - HPA로 자동 스케일링 설정
