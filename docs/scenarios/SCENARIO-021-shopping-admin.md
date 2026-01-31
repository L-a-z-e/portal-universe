---
id: SCENARIO-021
title: Admin 쇼핑 관리 (쿠폰/타임딜/주문/배송/재고)
type: scenario
status: current
created: 2026-01-31
updated: 2026-01-31
author: Laze
tags:
  - admin
  - coupon
  - time-deal
  - order
  - delivery
  - inventory
  - e2e
related:
  - SCENARIO-016-shopping-coupon
  - SCENARIO-018-shopping-timedeal
  - SCENARIO-019-shopping-delivery
---

# SCENARIO-021: Admin 쇼핑 관리

## Overview

관리자가 **쿠폰 생성/관리**, **타임딜 생성/관리**, **주문 상태 변경**, **배송 상태 관리**, **재고 이동 이력 조회**를 수행하는 E2E 시나리오입니다.

---

## Actors

| Actor | 역할 | 책임 |
|-------|------|------|
| **관리자** | 쇼핑 관리자 | ROLE_SHOPPING_ADMIN 또는 ROLE_SUPER_ADMIN |
| **Shopping Service** | 비즈니스 로직 | Admin API 처리 |

---

## Preconditions

- 관리자 계정으로 로그인된 상태
- ROLE_SHOPPING_ADMIN 또는 ROLE_SUPER_ADMIN 권한 보유

---

## Flow

### 시나리오 A: 쿠폰 관리

```
1. /admin/coupons 페이지 접속
2. 쿠폰 목록 확인 (페이징)
3. "쿠폰 생성" 버튼 클릭 → /admin/coupons/new
4. 쿠폰 정보 입력 (이름, 할인유형, 할인값, 수량, 유효기간)
5. 생성 완료 → 목록으로 리다이렉트
6. 쿠폰 비활성화 (삭제 버튼)
```

### 시나리오 B: 타임딜 관리

```
1. /admin/time-deals 페이지 접속
2. 타임딜 목록 확인
3. "타임딜 생성" 버튼 클릭 → /admin/time-deals/new
4. 타임딜 정보 입력 (상품, 할인가, 수량, 시작/종료 시간)
5. 생성 완료 → 목록으로 리다이렉트
6. 타임딜 취소 (삭제 버튼)
```

### 시나리오 C: 주문 관리

```
1. /admin/orders 페이지 접속
2. 주문 목록 확인 (상태 필터, 키워드 검색)
3. 주문 클릭 → /admin/orders/:orderNumber 상세
4. 주문 상태 변경 (CONFIRMED → SHIPPING → DELIVERED)
5. 결제 정보 확인
```

### 시나리오 D: 배송 관리

```
1. /admin/deliveries 페이지 접속
2. 배송 목록 확인
3. 배송 상태 변경 (PREPARING → SHIPPED → IN_TRANSIT → DELIVERED)
```

### 시나리오 E: 재고 이동 이력

```
1. /admin/stock-movements 페이지 접속
2. 재고 이동 이력 목록 확인
3. 이동 유형별 필터링 (입고, 출고, 조정)
```

---

## API Endpoints

### 쿠폰 Admin
| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/admin/coupons` | 쿠폰 목록 |
| POST | `/admin/coupons` | 쿠폰 생성 |
| DELETE | `/admin/coupons/{couponId}` | 쿠폰 비활성화 |

### 타임딜 Admin
| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/admin/time-deals` | 타임딜 목록 |
| POST | `/admin/time-deals` | 타임딜 생성 |
| DELETE | `/admin/time-deals/{timeDealId}` | 타임딜 취소 |

### 주문 Admin
| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/admin/orders` | 주문 목록 |
| GET | `/admin/orders/{orderNumber}` | 주문 상세 |
| PUT | `/admin/orders/{orderNumber}/status` | 상태 변경 |

### 배송 Admin
| HTTP | Endpoint | 설명 |
|------|----------|------|
| PUT | `/deliveries/{trackingNumber}/status` | 배송 상태 변경 |

### 재고
| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/inventory/{productId}` | 재고 조회 |
| GET | `/inventory/{productId}/movements` | 이동 이력 |

---

## E2E Test Cases

### Admin Coupon
| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-021-A01 | 쿠폰 목록 페이지 로드 | P1 |
| TC-021-A02 | 쿠폰 생성 폼 표시 및 제출 | P1 |
| TC-021-A03 | 쿠폰 비활성화 | P2 |

### Admin TimeDeal
| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-021-B01 | 타임딜 목록 페이지 로드 | P1 |
| TC-021-B02 | 타임딜 생성 폼 표시 및 제출 | P1 |
| TC-021-B03 | 타임딜 취소 | P2 |

### Admin Order
| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-021-C01 | 주문 목록 및 필터링 | P1 |
| TC-021-C02 | 주문 상세 표시 | P1 |
| TC-021-C03 | 주문 상태 변경 | P1 |

### Admin Delivery
| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-021-D01 | 배송 목록 표시 | P1 |
| TC-021-D02 | 배송 상태 변경 | P1 |

### Admin Inventory
| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-021-E01 | 재고 이동 이력 표시 | P1 |

---

## Verification Points

- [ ] 모든 Admin 페이지 접근 권한 검증
- [ ] CRUD 폼 유효성 검사
- [ ] 목록 페이징 동작
- [ ] 상태 변경 즉시 반영
- [ ] 비인가 접근 시 403 리다이렉트
