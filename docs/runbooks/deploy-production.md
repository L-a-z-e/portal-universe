---
id: runbook-deploy-prod
title: Production 배포 절차
type: runbook
status: current
created: 2026-01-19
updated: 2026-01-19
author: DevOps Team
tags: [deploy, production, kubernetes]
---

# Production 배포 Runbook

## 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 30분 |
| **필요 권한** | K8s cluster admin, Docker registry access |
| **영향 범위** | 전체 서비스 |

## 사전 조건

- [ ] QA 승인 완료
- [ ] Staging 테스트 통과
- [ ] 배포 공지 완료
- [ ] 모든 CI 파이프라인 통과

## Backend 배포 순서

> **중요**: 서비스 간 의존성으로 인해 아래 순서를 반드시 준수해야 합니다.

```
1. config-service (설정 서버)
2. auth-service
3. shopping-service, blog-service (병렬 가능)
4. notification-service
5. api-gateway (마지막)
```

## 절차

### Step 1: 현재 상태 확인

```bash
# 현재 배포 버전 확인
kubectl get deployments -n portal-universe -o wide

# Pod 상태 확인
kubectl get pods -n portal-universe
```

**예상 결과**: 모든 Pod이 Running 상태

### Step 2: Config Service 배포

```bash
# 이미지 버전 업데이트
kubectl set image deployment/config-service \
  config-service=your-registry/config-service:v1.0.0 \
  -n portal-universe

# 롤아웃 상태 확인
kubectl rollout status deployment/config-service -n portal-universe
```

**예상 결과**: `deployment "config-service" successfully rolled out`

### Step 3: Auth Service 배포

```bash
kubectl set image deployment/auth-service \
  auth-service=your-registry/auth-service:v1.0.0 \
  -n portal-universe

kubectl rollout status deployment/auth-service -n portal-universe
```

**예상 결과**: `deployment "auth-service" successfully rolled out`

### Step 4: Shopping/Blog Service 배포 (병렬)

```bash
# Shopping Service
kubectl set image deployment/shopping-service \
  shopping-service=your-registry/shopping-service:v1.0.0 \
  -n portal-universe &

# Blog Service
kubectl set image deployment/blog-service \
  blog-service=your-registry/blog-service:v1.0.0 \
  -n portal-universe &

# 롤아웃 대기
kubectl rollout status deployment/shopping-service -n portal-universe
kubectl rollout status deployment/blog-service -n portal-universe
```

**예상 결과**: 두 서비스 모두 successfully rolled out

### Step 5: Notification Service 배포

```bash
kubectl set image deployment/notification-service \
  notification-service=your-registry/notification-service:v1.0.0 \
  -n portal-universe

kubectl rollout status deployment/notification-service -n portal-universe
```

### Step 6: API Gateway 배포

```bash
kubectl set image deployment/api-gateway \
  api-gateway=your-registry/api-gateway:v1.0.0 \
  -n portal-universe

kubectl rollout status deployment/api-gateway -n portal-universe
```

### Step 7: 헬스 체크

```bash
# API Gateway Health Check
curl -s http://api-gateway.portal-universe.svc.cluster.local:8080/actuator/health | jq .

# 각 서비스 Health Check
for svc in auth-service shopping-service blog-service notification-service; do
  echo "Checking $svc..."
  curl -s "http://$svc.portal-universe.svc.cluster.local/actuator/health" | jq .status
done
```

**예상 결과**: 모든 서비스가 `{"status": "UP"}` 반환

## Frontend 배포 순서

```
1. design-system
2. portal-shell (Host)
3. blog-frontend, shopping-frontend (Remote, 병렬 가능)
```

### Step 8: Frontend 배포

```bash
# Design System
kubectl set image deployment/design-system \
  design-system=your-registry/design-system:v1.0.0 \
  -n portal-universe

# Portal Shell
kubectl set image deployment/portal-shell \
  portal-shell=your-registry/portal-shell:v1.0.0 \
  -n portal-universe

# Remote Modules (병렬)
kubectl set image deployment/blog-frontend \
  blog-frontend=your-registry/blog-frontend:v1.0.0 \
  -n portal-universe &

kubectl set image deployment/shopping-frontend \
  shopping-frontend=your-registry/shopping-frontend:v1.0.0 \
  -n portal-universe &

wait
```

### Step 9: E2E 검증

```bash
# 메인 페이지 접근 확인
curl -s -o /dev/null -w "%{http_code}" https://portal.example.com/

# 로그인 API 확인
curl -s -o /dev/null -w "%{http_code}" https://portal.example.com/api/v1/auth/health
```

**예상 결과**: HTTP 200

## 문제 발생 시

[Rollback Runbook](./rollback-procedure.md) 참고

## 에스컬레이션

1. DevOps Lead: TBD
2. Backend Lead: TBD
3. Frontend Lead: TBD
