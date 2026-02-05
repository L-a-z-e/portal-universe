---
id: runbook-deploy-prod
title: Production 배포 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: DevOps Team
tags: [deploy, production]
---

# 🚀 Production 배포 Runbook

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 30분 |
| **필요 권한** | DevOps, Admin |
| **영향 범위** | 전체 서비스 |

## ✅ 사전 조건
- [ ] QA 승인 완료
- [ ] Staging 테스트 통과
- [ ] 배포 공지 완료

## 🔄 절차

### Step 1: 배포 준비
```bash
# 현재 버전 확인
kubectl get deployment -n production -o wide
```
**예상 결과**: 현재 배포된 버전 확인

### Step 2: 이미지 태그 확인
```bash
# 배포할 이미지 태그 확인
docker images | grep shopping-service
```

### Step 3: 배포 실행
```bash
# Rolling Update 배포
kubectl set image deployment/product-service \
  product-service=shopping-service:v1.2.0 \
  -n production
```

### Step 4: 배포 상태 확인
```bash
# 롤아웃 상태 확인
kubectl rollout status deployment/product-service -n production
```
**예상 결과**: `deployment "product-service" successfully rolled out`

### Step 5: 헬스 체크
```bash
curl -s https://api.example.com/health | jq .
```
**예상 결과**: `{"status": "UP"}`

## ⚠️ 문제 발생 시
[Rollback Runbook](./rollback.md) 참고

## 📞 에스컬레이션
1. DevOps Lead: 010-1234-5678
2. Backend Lead: 010-2345-6789
