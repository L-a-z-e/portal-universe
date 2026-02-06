---
id: runbook-rollback
title: Rollback 절차
type: runbook
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [rollback, production, kubernetes]
---

# Rollback Runbook

## 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 10분 |
| **필요 권한** | K8s cluster admin |
| **영향 범위** | 롤백 대상 서비스 |

## 사전 조건

- [ ] 문제 서비스 식별 완료
- [ ] 이전 버전 이미지 확인

## 절차

### Step 1: 롤백 대상 확인

```bash
# 배포 히스토리 확인
kubectl rollout history deployment/<service-name> -n portal-universe

# 특정 리비전 상세 확인
kubectl rollout history deployment/<service-name> -n portal-universe --revision=2
```

**예상 결과**: 이전 리비전 정보 확인

### Step 2: 롤백 실행

#### 방법 1: 이전 버전으로 롤백

```bash
# 바로 이전 버전으로 롤백
kubectl rollout undo deployment/<service-name> -n portal-universe
```

#### 방법 2: 특정 리비전으로 롤백

```bash
# 특정 리비전으로 롤백
kubectl rollout undo deployment/<service-name> -n portal-universe --to-revision=2
```

**예상 결과**: `deployment.apps/<service-name> rolled back`

### Step 3: 롤백 상태 확인

```bash
# 롤아웃 상태 확인
kubectl rollout status deployment/<service-name> -n portal-universe

# Pod 상태 확인
kubectl get pods -n portal-universe -l app=<service-name>
```

**예상 결과**: 모든 Pod이 Running 상태

### Step 4: 헬스 체크

```bash
curl -s http://<service-name>.portal-universe.svc.cluster.local/actuator/health | jq .
```

**예상 결과**: `{"status": "UP"}`

## 전체 서비스 롤백 (비상 시)

```bash
# 모든 서비스 롤백 (역순)
for svc in api-gateway notification-service shopping-service blog-service auth-service config-service; do
  echo "Rolling back $svc..."
  kubectl rollout undo deployment/$svc -n portal-universe
done
```

## Frontend 롤백

```bash
# Frontend 서비스 롤백 (역순)
for svc in shopping-frontend blog-frontend portal-shell design-system; do
  echo "Rolling back $svc..."
  kubectl rollout undo deployment/$svc -n portal-universe
done
```

## 문제 발생 시

롤백이 실패하거나 문제가 지속되면:

1. 에스컬레이션 연락처로 즉시 연락
2. 장애 대응 매뉴얼 참고: [incident-response.md](./incident-response.md)

## 에스컬레이션

1. DevOps Lead: TBD
2. Backend Lead: TBD
