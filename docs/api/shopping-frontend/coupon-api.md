---
id: api-shopping-coupon
title: Shopping Coupon API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, coupon, admin]
related: [api-shopping-types, api-shopping-order]
---

# Shopping Coupon API

> 쿠폰 발급 및 관리 API (공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/coupons` |
| **인증** | Bearer Token (필수) |
| **엔드포인트** | `couponApi`, `adminCouponApi` |

---

## 공개 API (couponApi)

### 발급 가능한 쿠폰 목록 조회

```typescript
getAvailableCoupons(): Promise<ApiResponse<Coupon[]>>
```

**Endpoint**: `GET /api/v1/shopping/coupons`

**Response**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "WELCOME2026",
      "name": "신규 가입 쿠폰",
      "description": "신규 가입 고객 환영 쿠폰",
      "discountType": "FIXED",
      "discountValue": 5000,
      "minimumOrderAmount": 30000,
      "maximumDiscountAmount": null,
      "totalQuantity": 1000,
      "issuedQuantity": 150,
      "remainingQuantity": 850,
      "status": "ACTIVE",
      "startsAt": "2026-01-01T00:00:00Z",
      "expiresAt": "2026-12-31T23:59:59Z",
      "createdAt": "2026-01-01T00:00:00Z"
    }
  ]
}
```

---

### 쿠폰 발급

```typescript
issueCoupon(couponId: number): Promise<ApiResponse<UserCoupon>>
```

**Endpoint**: `POST /api/v1/shopping/coupons/{couponId}/issue`

**Response**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "couponId": 1,
    "couponCode": "WELCOME2026",
    "couponName": "신규 가입 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000,
    "minimumOrderAmount": 30000,
    "status": "AVAILABLE",
    "issuedAt": "2026-02-06T10:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
  }
}
```

**Error (쿠폰 소진)**

```json
{
  "success": false,
  "code": "COUPON_EXHAUSTED",
  "message": "쿠폰이 모두 소진되었습니다."
}
```

---

### 내 쿠폰 목록 조회

```typescript
getUserCoupons(): Promise<ApiResponse<UserCoupon[]>>
```

**Endpoint**: `GET /api/v1/shopping/coupons/my`

---

### 사용 가능한 내 쿠폰 목록 조회

```typescript
getAvailableUserCoupons(): Promise<ApiResponse<UserCoupon[]>>
```

**Endpoint**: `GET /api/v1/shopping/coupons/my/available`

---

## 관리자 API (adminCouponApi)

### 쿠폰 목록 조회 (Admin)

```typescript
getCoupons(page = 1, size = 10): Promise<ApiResponse<PagedResponse<Coupon>>>
```

**Endpoint**: `GET /api/v1/shopping/admin/coupons?page=1&size=10`

---

### 쿠폰 상세 조회

```typescript
getCoupon(id: number): Promise<ApiResponse<Coupon>>
```

**Endpoint**: `GET /api/v1/shopping/admin/coupons/{id}`

---

### 쿠폰 생성 (Admin)

```typescript
createCoupon(data: CouponCreateRequest): Promise<ApiResponse<Coupon>>
```

**Endpoint**: `POST /api/v1/shopping/admin/coupons`

**Request Body**

```json
{
  "code": "SPRING2026",
  "name": "봄맞이 할인 쿠폰",
  "description": "봄맞이 특별 할인",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "minimumOrderAmount": 50000,
  "maximumDiscountAmount": 10000,
  "totalQuantity": 500,
  "startsAt": "2026-03-01T00:00:00Z",
  "expiresAt": "2026-03-31T23:59:59Z"
}
```

---

### 쿠폰 비활성화 (Admin)

```typescript
deactivateCoupon(id: number): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/admin/coupons/{id}`

---

## React Hooks

### useAvailableCoupons

```typescript
import { useAvailableCoupons } from '@/hooks/useCoupons'

const { data, isLoading, error, refetch } = useAvailableCoupons()
```

### useIssueCoupon

```typescript
import { useIssueCoupon } from '@/hooks/useCoupons'

const { mutateAsync, isPending } = useIssueCoupon()

const handleIssue = async (couponId: number) => {
  try {
    const userCoupon = await mutateAsync(couponId)
    alert('쿠폰이 발급되었습니다')
  } catch (error) {
    alert('발급 실패')
  }
}
```

### useAdminCoupons

```typescript
import { useAdminCoupons } from '@/hooks/useAdminCoupons'

const { data, isLoading, error } = useAdminCoupons({ page: 0, size: 10 })
```

---

## Helper Functions

### 할인 금액 계산

```typescript
import { calculateDiscount } from '@/hooks/useCoupons'

const discount = calculateDiscount(coupon, orderAmount)
// FIXED: discountValue 그대로 반환
// PERCENTAGE: orderAmount * discountValue / 100
```

### 쿠폰 적용 가능 여부 확인

```typescript
import { canApplyCoupon } from '@/hooks/useCoupons'

const canApply = canApplyCoupon(coupon, orderAmount)
// minimumOrderAmount 검증
```

---

## 타입 정의

```typescript
export type DiscountType = 'FIXED' | 'PERCENTAGE'
export type CouponStatus = 'ACTIVE' | 'INACTIVE' | 'EXPIRED' | 'EXHAUSTED'
export type UserCouponStatus = 'AVAILABLE' | 'USED' | 'EXPIRED'

export interface Coupon {
  id: number
  code: string
  name: string
  description?: string
  discountType: DiscountType
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  totalQuantity: number
  issuedQuantity: number
  remainingQuantity: number
  status: CouponStatus
  startsAt: string
  expiresAt: string
  createdAt: string
}

export interface UserCoupon {
  id: number
  couponId: number
  couponCode: string
  couponName: string
  discountType: DiscountType
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  status: UserCouponStatus
  issuedAt: string
  expiresAt: string
  usedAt?: string
  usedOrderId?: number
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없음 |
| `COUPON_EXHAUSTED` | 400 | 쿠폰 소진 |
| `COUPON_EXPIRED` | 400 | 쿠폰 만료 |
| `COUPON_ALREADY_ISSUED` | 400 | 이미 발급된 쿠폰 |
| `COUPON_NOT_STARTED` | 400 | 아직 시작되지 않은 쿠폰 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Order API](./order-api.md)
- [공통 타입 정의](./types.md)

---

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-08 | 페이지네이션 기본값 수정: page 0 → 1 (ADR-031 정합) |
| 2026-02-06 | 최초 작성 |

---

**최종 업데이트**: 2026-02-08
