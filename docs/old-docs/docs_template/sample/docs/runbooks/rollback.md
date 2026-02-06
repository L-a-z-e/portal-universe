---
id: runbook-rollback
title: 롤백 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [rollback, emergency]
---

# ⏪ Rollback Runbook

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 15분 |
| **필요 권한** | DevOps |

## 🔄 절차

### Step 1: 롤백 히스토리 확인
```bash
kubectl rollout history deployment/product-service -n production
```

### Step 2: 롤백 실행
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/product-service -n production

# 또는 특정 버전으로 롤백
kubectl rollout undo deployment/product-service -n production --to-revision=2
```

### Step 3: 롤백 상태 확인
```bash
kubectl rollout status deployment/product-service -n production
```

### Step 4: 헬스 체크
```bash
curl -s https://api.example.com/health | jq .
```
