# HPA (Horizontal Pod Autoscaler) 설정

부하에 따라 자동으로 Pod를 스케일링하는 HPA를 설정합니다.

---

## 학습 목표

- [ ] HPA의 동작 원리를 이해한다
- [ ] CPU/Memory 기반 HPA를 설정할 수 있다
- [ ] 스케일링 동작을 테스트할 수 있다

---

## 1. HPA란?

### 정의

> HPA는 메트릭(CPU, Memory, 커스텀)을 기반으로 Pod 수를 자동 조절합니다.

### 동작 원리

```
Metrics Server → HPA Controller → Deployment
     ↓                 ↓              ↓
  메트릭 수집    목표값 비교      replicas 조정
```

---

## 2. 사전 요구사항

### Metrics Server 확인

```bash
# Metrics Server 설치 확인
kubectl get deployment metrics-server -n kube-system

# 메트릭 수집 확인
kubectl top pods -n portal-universe
```

### Metrics Server 설치 (필요시)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

---

## 3. HPA 설정

### API Gateway HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: portal-universe
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5분 안정화
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 2
          periodSeconds: 60
      selectPolicy: Max
```

### Auth Service HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
  namespace: portal-universe
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## 4. HPA 적용

```bash
# HPA 생성
kubectl apply -f k8s/hpa/

# HPA 상태 확인
kubectl get hpa -n portal-universe
kubectl describe hpa api-gateway-hpa -n portal-universe
```

---

## 5. 스케일링 테스트

### 부하 생성

```bash
# k6 부하 테스트
k6 run services/load-tests/k6/scenarios/a-shopping-flow.js

# 또는 간단한 부하
kubectl run load-test --image=busybox -n portal-universe --rm -it -- \
  /bin/sh -c "while true; do wget -q -O- http://api-gateway:8080/api/health; done"
```

### 스케일링 관찰

```bash
# HPA 상태 실시간 확인
watch -n 5 'kubectl get hpa -n portal-universe'

# Pod 수 변화 확인
watch -n 2 'kubectl get pods -n portal-universe -l app=api-gateway'
```

---

## 6. 알림 규칙

```yaml
- alert: HPAMaxedOut
  expr: |
    kube_horizontalpodautoscaler_status_current_replicas
    ==
    kube_horizontalpodautoscaler_spec_max_replicas
  for: 15m
  labels:
    severity: warning
  annotations:
    summary: "HPA {{ $labels.horizontalpodautoscaler }}가 최대 replicas 도달"
```

---

## 다음 단계

[03-pdb-setup.md](./03-pdb-setup.md) - PDB로 가용성 보장
