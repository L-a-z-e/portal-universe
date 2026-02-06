---
id: SCENARIO-019
title: 주문 후 배송 상태 확인
type: scenario
status: current
created: 2026-01-31
updated: 2026-01-31
author: Laze
tags:
  - delivery
  - tracking
  - order
  - e2e
related:
  - SCENARIO-002-order-saga
---

# SCENARIO-019: 주문 후 배송 상태 확인

## Overview

사용자가 **주문 완료 후 배송 상태를 확인**하고, **배송 추적 타임라인**을 통해 현재 배송 진행 상황을 모니터링하는 E2E 시나리오입니다.

---

## Actors

| Actor | 역할 | 책임 |
|-------|------|------|
| **사용자** | 주문자 | 배송 상태 조회 |
| **Shopping Service** | 비즈니스 로직 | 배송 정보 관리 |

---

## Preconditions

- 사용자에게 CONFIRMED 이상 상태의 주문이 1개 이상 존재
- 해당 주문에 배송 정보가 생성된 상태

---

## Flow

### 메인 시나리오: 주문 상세에서 배송 추적

```
1. 사용자가 /orders 페이지 접속
2. 주문 목록에서 배송 중인 주문 클릭
3. 주문 상세 페이지(/orders/:orderNumber)에서 배송 정보 섹션 확인
4. 운송장 번호 표시 확인
5. 배송 상태 타임라인 표시 (PREPARING → SHIPPED → IN_TRANSIT → DELIVERED)
6. 현재 배송 단계 강조 표시
```

### 대안 시나리오: 배송 정보 미생성

```
1. 주문 상세 페이지 접속
2. 배송 정보가 아직 없는 경우
3. "배송 준비 중" 메시지 표시
```

---

## API Endpoints

| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/deliveries/order/{orderNumber}` | 주문 번호로 배송 조회 |
| GET | `/deliveries/{trackingNumber}` | 운송장으로 배송 조회 |

---

## E2E Test Cases

| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-019-01 | 주문 상세에서 배송 정보 표시 | P1 |
| TC-019-02 | 배송 추적 타임라인 렌더링 | P1 |
| TC-019-03 | 배송 미생성 시 안내 메시지 | P2 |
| TC-019-04 | 운송장 번호 표시 | P2 |

---

## Verification Points

- [ ] OrderDetailPage 내 배송 섹션 렌더링
- [ ] deliveryApi.getDeliveryByOrder() 호출 확인
- [ ] 배송 타임라인 UI
- [ ] 배송 미생성 시 빈 상태 처리
