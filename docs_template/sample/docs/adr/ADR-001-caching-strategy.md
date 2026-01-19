---
id: ADR-001
title: 캐싱 전략
type: adr
status: accepted
created: 2026-01-18
updated: 2026-01-18
author: Architecture Team
decision_date: 2026-01-18
tags: [caching, redis, performance]
related:
  - PRD-001
---

# ADR-001: 캐싱 전략

## 📋 메타데이터

| 항목 | 내용 |
|------|------|
| **상태** | ✅ Accepted |
| **결정일** | 2026-01-18 |
| **검토자** | Tech Lead, Backend Team |

## 📌 Context (배경)

상품 목록 조회 API가 초당 10,000건 이상의 요청을 처리해야 합니다. 현재 DB 직접 조회 방식으로는 성능 요구사항을 충족할 수 없습니다.

## 🎯 Decision Drivers (결정 요인)

1. 응답 시간 200ms 이하 필요
2. 높은 읽기/쓰기 비율 (100:1)
3. 데이터 일관성보다 가용성 우선
4. 운영 복잡도 최소화

## 🔄 Considered Options (검토한 대안)

### Option 1: Redis Cache-Aside
- 장점: 구현 간단, 검증된 패턴, 유연한 TTL
- 단점: Cache Miss 시 지연, 수동 무효화 필요

### Option 2: CDN Edge Caching
- 장점: 글로벌 분산, 매우 빠른 응답
- 단점: 무효화 어려움, 비용 높음

### Option 3: 애플리케이션 로컬 캐시
- 장점: 매우 빠름, 네트워크 비용 없음
- 단점: 서버간 일관성 문제, 메모리 제한

## ✅ Decision (최종 결정)

**Redis Cache-Aside 패턴 선택**

```
읽기 흐름:
1. Redis 조회 → 2. Hit면 반환 → 3. Miss면 DB 조회 → 4. Redis 저장

쓰기 흐름:
1. DB 업데이트 → 2. Redis 삭제 (무효화)
```

## 📊 Consequences (영향)

### 긍정적
- 응답 시간 200ms → 20ms (90% 개선)
- DB 부하 80% 감소
- 팀 내 Redis 운영 경험 활용

### 부정적
- Redis 장애 시 DB 부하 집중
- TTL 설정에 따른 데이터 정합성 지연 (최대 5분)

## 🔗 관련 문서
- [PRD-001: 상품 관리](../prd/PRD-001-product-management.md)
