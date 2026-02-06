---
id: TS-20260118-001
title: Redis 연결 타임아웃
type: troubleshooting
status: resolved
created: 2026-01-18
updated: 2026-01-18
author: Laze
severity: critical
resolved: true
affected_services: [product-service, order-service]
tags: [redis, timeout, connection]
---

# TS-20260118-001: Redis 연결 타임아웃

## 📋 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **발생일** | 2026-01-18 09:30 |
| **해결일** | 2026-01-18 10:15 |
| **영향 서비스** | product-service, order-service |

## 🚨 증상 (Symptoms)
- 상품 조회 API 응답 지연 (10초 이상)
- 에러 로그: `Redis connection timeout after 5000ms`
- 모니터링: Redis CPU 사용률 95%

## 🔍 원인 분석 (Root Cause)
1. 대량 프로모션으로 인한 트래픽 급증 (평소 대비 5배)
2. Redis 커넥션 풀 고갈 (max: 50, 사용: 50)
3. 느린 쿼리로 인한 커넥션 점유 시간 증가

## ✅ 해결 방법 (Solution)

### 즉시 조치
```bash
# 1. Redis 커넥션 풀 확장
kubectl set env deployment/product-service REDIS_MAX_CONNECTIONS=200

# 2. 서비스 재시작
kubectl rollout restart deployment/product-service
```

### 영구 조치
- `application.yml` 커넥션 풀 설정 변경
- Redis Cluster 스케일 아웃 (3 → 6 노드)

## 🛡️ 재발 방지 (Prevention)
1. 커넥션 풀 사용량 알람 추가 (80% 임계값)
2. 대량 트래픽 예상 시 사전 스케일 아웃 절차 수립

## 📚 학습 포인트
- Redis 커넥션 풀 모니터링 중요성
- 프로모션 전 부하 테스트 필요성
