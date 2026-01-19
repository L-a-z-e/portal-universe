---
id: runbook-scale-out
title: Scale-out 절차
type: runbook
status: current
created: 2026-01-19
updated: 2026-01-19
author: DevOps Team
tags: [scale, kubernetes, production]
---

# Scale-out Runbook

## 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 15분 |
| **필요 권한** | K8s cluster admin |
| **영향 범위** | 스케일 대상 서비스 |

## 사전 조건

- [ ] 클러스터 리소스 여유 확인
- [ ] 현재 Pod 상태 정상 확인
- [ ] 트래픽 패턴 분석 완료

## 절차

### Step 1: 현재 상태 확인

```bash
# 현재 replica 수 확인
kubectl get deployment -n portal-universe

# 노드 리소스 확인
kubectl top nodes

# 현재 Pod 리소스 사용량
kubectl top pods -n portal-universe
```

**예상 결과**: 현재 배포 상태 및 리소스 사용량 확인

### Step 2: 클러스터 리소스 여유 확인

```bash
# 노드별 할당 가능한 리소스 확인
kubectl describe nodes | grep -A 5 "Allocatable"

# 네임스페이스 리소스 사용량
kubectl resource-quota -n portal-universe
```

**예상 결과**: 스케일아웃에 충분한 리소스 여유 확인

### Step 3: Manual Scale-out

#### 단일 서비스 스케일아웃

```bash
# 특정 서비스 replica 증가
kubectl scale deployment/<service-name> -n portal-universe --replicas=3

# 스케일아웃 상태 확인
kubectl rollout status deployment/<service-name> -n portal-universe
```

#### 여러 서비스 동시 스케일아웃

```bash
# Backend 서비스 일괄 스케일아웃
for svc in auth-service shopping-service blog-service notification-service; do
  kubectl scale deployment/$svc -n portal-universe --replicas=3
done
```

**예상 결과**: 새로운 Pod이 Running 상태로 전환

### Step 4: HPA (Horizontal Pod Autoscaler) 설정

#### HPA 생성

```bash
kubectl autoscale deployment/<service-name> \
  -n portal-universe \
  --min=2 \
  --max=10 \
  --cpu-percent=70
```

#### HPA 상태 확인

```bash
kubectl get hpa -n portal-universe
```

**예상 결과**: HPA가 생성되고 TARGETS 컬럼에 현재 CPU 사용률 표시

### Step 5: 스케일아웃 검증

```bash
# 새 Pod 상태 확인
kubectl get pods -n portal-universe -l app=<service-name> -o wide

# 서비스 엔드포인트 확인 (모든 Pod이 포함되어야 함)
kubectl get endpoints <service-name> -n portal-universe

# 헬스체크
for pod in $(kubectl get pods -n portal-universe -l app=<service-name> -o name); do
  kubectl exec -n portal-universe $pod -- curl -s localhost:8080/actuator/health | jq .status
done
```

**예상 결과**: 모든 새 Pod이 Running 상태이고 Endpoint에 등록됨

## 권장 Replica 수

| 서비스 | 기본 | 피크 시 | 최대 |
|--------|------|---------|------|
| api-gateway | 2 | 4 | 8 |
| auth-service | 2 | 4 | 6 |
| shopping-service | 2 | 5 | 10 |
| blog-service | 2 | 3 | 6 |
| notification-service | 2 | 3 | 5 |

## HPA 권장 설정

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: shopping-service-hpa
  namespace: portal-universe
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: shopping-service
  minReplicas: 2
  maxReplicas: 10
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
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 120
```

## Scale-in (축소)

```bash
# Manual Scale-in
kubectl scale deployment/<service-name> -n portal-universe --replicas=2

# HPA 삭제 (수동 관리로 전환)
kubectl delete hpa <hpa-name> -n portal-universe
```

## 문제 발생 시

### Pod이 Pending 상태

```bash
# Pod 상세 확인
kubectl describe pod <pod-name> -n portal-universe

# 이벤트 확인
kubectl get events -n portal-universe --field-selector reason=FailedScheduling
```

**원인**: 클러스터 리소스 부족
**대응**: 노드 추가 또는 리소스 request/limit 조정

## 에스컬레이션

1. DevOps Lead: TBD
2. Backend Lead: TBD
