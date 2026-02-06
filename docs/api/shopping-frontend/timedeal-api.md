---
id: api-shopping-timedeal
title: Shopping TimeDeal API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, timedeal, admin]
related: [api-shopping-types, api-shopping-queue]
---

# Shopping TimeDeal API

> 타임딜 조회 및 구매 API (공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/time-deals` |
| **인증** | Bearer Token (필수) |
| **엔드포인트** | `timeDealApi`, `adminTimeDealApi` |

---

## 공개 API (timeDealApi)

### 진행 중인 타임딜 목록 조회

```typescript
getActiveTimeDeals(): Promise<ApiResponse<TimeDeal[]>>
```

**Endpoint**: `GET /api/v1/shopping/time-deals`

**Response**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "봄맞이 도서 특가",
      "description": "베스트셀러 특별 할인",
      "status": "ACTIVE",
      "startsAt": "2026-02-06T00:00:00Z",
      "endsAt": "2026-02-07T23:59:59Z",
      "products": [
        {
          "id": 1,
          "productId": 1,
          "productName": "스프링 부트 완벽 가이드",
          "originalPrice": 35000,
          "dealPrice": 28000,
          "discountRate": 20,
          "dealQuantity": 100,
          "soldQuantity": 45,
          "remainingQuantity": 55,
          "maxPerUser": 2,
          "available": true
        }
      ],
      "createdAt": "2026-02-05T10:00:00Z"
    }
  ]
}
```

---

### 타임딜 상세 조회

```typescript
getTimeDeal(id: number): Promise<ApiResponse<TimeDeal>>
```

**Endpoint**: `GET /api/v1/shopping/time-deals/{id}`

---

### 타임딜 구매

```typescript
purchaseTimeDeal(timeDealProductId: number, quantity: number): Promise<ApiResponse<unknown>>
```

**Endpoint**: `POST /api/v1/shopping/time-deals/purchase`

**Request Body**

```json
{
  "timeDealProductId": 1,
  "quantity": 2
}
```

**Error (품절)**

```json
{
  "success": false,
  "code": "TIMEDEAL_SOLD_OUT",
  "message": "타임딜 상품이 품절되었습니다."
}
```

---

### 내 타임딜 구매 내역 조회

```typescript
getMyPurchases(): Promise<ApiResponse<TimeDealPurchase[]>>
```

**Endpoint**: `GET /api/v1/shopping/time-deals/my/purchases`

**Response**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "timeDealProductId": 1,
      "productName": "스프링 부트 완벽 가이드",
      "quantity": 2,
      "purchasePrice": 28000,
      "totalPrice": 56000,
      "purchasedAt": "2026-02-06T10:30:00Z"
    }
  ]
}
```

---

## 관리자 API (adminTimeDealApi)

### 타임딜 목록 조회 (Admin)

```typescript
getTimeDeals(page = 0, size = 10): Promise<ApiResponse<PagedResponse<TimeDeal>>>
```

**Endpoint**: `GET /api/v1/shopping/admin/time-deals?page=0&size=10`

---

### 타임딜 상세 조회

```typescript
getTimeDeal(id: number): Promise<ApiResponse<TimeDeal>>
```

**Endpoint**: `GET /api/v1/shopping/admin/time-deals/{id}`

---

### 타임딜 생성 (Admin)

```typescript
createTimeDeal(data: TimeDealCreateRequest): Promise<ApiResponse<TimeDeal>>
```

**Endpoint**: `POST /api/v1/shopping/admin/time-deals`

**Request Body**

```json
{
  "name": "여름맞이 도서 특가",
  "description": "여름 베스트셀러 할인",
  "startsAt": "2026-06-01T00:00:00Z",
  "endsAt": "2026-06-07T23:59:59Z",
  "products": [
    {
      "productId": 1,
      "dealPrice": 25000,
      "dealQuantity": 200,
      "maxPerUser": 3
    }
  ]
}
```

---

### 타임딜 취소 (Admin)

```typescript
cancelTimeDeal(id: number): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/admin/time-deals/{id}`

---

## React Hooks

### useActiveTimeDeals

```typescript
import { useActiveTimeDeals } from '@/hooks/useTimeDeals'

const { data, isLoading, error, refetch } = useActiveTimeDeals()
```

### useTimeDeal

```typescript
import { useTimeDeal } from '@/hooks/useTimeDeals'

const { data, isLoading, error } = useTimeDeal(id)
```

### usePurchaseTimeDeal

```typescript
import { usePurchaseTimeDeal } from '@/hooks/useTimeDeals'

const { mutateAsync, isPending } = usePurchaseTimeDeal()

const handlePurchase = async (id: number, quantity: number) => {
  try {
    await mutateAsync(id, quantity)
    alert('구매 완료')
  } catch (error) {
    alert('구매 실패')
  }
}
```

### useTimeDealCountdown

남은 시간 계산 Hook (클라이언트 사이드)

```typescript
import { useTimeDealCountdown } from '@/hooks/useTimeDeals'

const { hours, minutes, seconds, isExpired } = useTimeDealCountdown(timeDeal.endsAt)

return (
  <div>
    {isExpired ? '종료됨' : `${hours}:${minutes}:${seconds} 남음`}
  </div>
)
```

---

## Helper Functions

### 할인율 계산

```typescript
import { calculateDiscountRate } from '@/hooks/useTimeDeals'

const discountRate = calculateDiscountRate(originalPrice, dealPrice)
// Math.round(((originalPrice - dealPrice) / originalPrice) * 100)
```

### 재고 비율 계산

```typescript
import { calculateStockPercentage } from '@/hooks/useTimeDeals'

const percentage = calculateStockPercentage(soldQuantity, dealQuantity)
// Math.round((sold / total) * 100)
```

---

## 타입 정의

```typescript
export type TimeDealStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'SOLD_OUT' | 'CANCELLED'

export interface TimeDealProduct {
  id: number
  productId: number
  productName: string
  originalPrice: number
  dealPrice: number
  discountRate: number
  dealQuantity: number
  soldQuantity: number
  remainingQuantity: number
  maxPerUser: number
  available: boolean
}

export interface TimeDeal {
  id: number
  name: string
  description: string
  status: TimeDealStatus
  startsAt: string
  endsAt: string
  products: TimeDealProduct[]
  createdAt: string
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `TIMEDEAL_NOT_FOUND` | 404 | 타임딜을 찾을 수 없음 |
| `TIMEDEAL_SOLD_OUT` | 400 | 타임딜 상품 품절 |
| `TIMEDEAL_ENDED` | 400 | 타임딜 종료 |
| `TIMEDEAL_EXCEED_MAX_PER_USER` | 400 | 최대 구매 수량 초과 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Queue API](./queue-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
