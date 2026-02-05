---
id: runbook-scale-out
title: 서비스 스케일 아웃
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [scale, performance]
---

# 📈 Scale Out Runbook

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 10분 |
| **필요 권한** | DevOps |

## 🔄 절차

### Step 1: 현재 상태 확인
```bash
kubectl get pods -n production -l app=product-service
kubectl top pods -n production -l app=product-service
```

### Step 2: 스케일 아웃 실행
```bash
kubectl scale deployment/product-service --replicas=6 -n production
```

### Step 3: Pod 상태 확인
```bash
kubectl get pods -n production -l app=product-service -w
```
**예상 결과**: 모든 Pod이 `Running` 상태

### Step 4: 부하 분산 확인
```bash
kubectl get endpoints product-service -n production
```
