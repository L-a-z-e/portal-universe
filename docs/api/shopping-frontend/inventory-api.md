---
id: api-shopping-inventory
title: Shopping Inventory API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, inventory, sse, admin]
related: [api-shopping-types, api-shopping-product]
---

# Shopping Inventory API

> 재고 조회 및 관리 API (SSE 실시간 업데이트, 공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/inventory` |
| **인증** | Bearer Token (관리자 기능만 ADMIN 권한 필요) |
| **SSE 지원** | Server-Sent Events (실시간 재고 업데이트) |
| **엔드포인트** | `inventoryApi`, `inventoryStreamApi`, `stockMovementApi` |

---

## 공개 API (inventoryApi)

### 재고 조회

```typescript
getInventory(productId: number): Promise<ApiResponse<Inventory>>
```

**Endpoint**: `GET /api/v1/shopping/inventory/{productId}`

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "productId": 1,
    "availableQuantity": 45,
    "reservedQuantity": 5,
    "totalQuantity": 50,
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-02-06T10:00:00Z"
  }
}
```

---

### 여러 상품 재고 조회

```typescript
getInventories(productIds: number[]): Promise<ApiResponse<Inventory[]>>
```

**Endpoint**: `POST /api/v1/shopping/inventory/batch`

**Request Body**

```json
{
  "productIds": [1, 2, 3, 5]
}
```

**Response**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "productId": 1,
      "availableQuantity": 45,
      "reservedQuantity": 5,
      "totalQuantity": 50
    },
    {
      "id": 2,
      "productId": 2,
      "availableQuantity": 28,
      "reservedQuantity": 2,
      "totalQuantity": 30
    }
  ]
}
```

---

### 재고 초기화 (관리자 - 신규 상품)

```typescript
initializeInventory(productId: number, data: InventoryUpdateRequest): Promise<ApiResponse<Inventory>>
```

**Endpoint**: `POST /api/v1/shopping/inventory/{productId}`

**Request Body**

```json
{
  "quantity": 100,
  "reason": "신규 상품 재고 초기화"
}
```

---

### 재고 추가 (관리자)

```typescript
addStock(productId: number, data: InventoryUpdateRequest): Promise<ApiResponse<Inventory>>
```

**Endpoint**: `PUT /api/v1/shopping/inventory/{productId}/add`

**Request Body**

```json
{
  "quantity": 50,
  "reason": "재입고"
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "productId": 1,
    "availableQuantity": 95,
    "reservedQuantity": 5,
    "totalQuantity": 100,
    "updatedAt": "2026-02-06T11:00:00Z"
  }
}
```

---

## Inventory Stream API (SSE)

### SSE 구독 URL 생성

```typescript
getStreamUrl(productIds: number[]): string
```

실시간 재고 업데이트를 위한 SSE URL을 생성합니다.

**URL**: `/api/v1/shopping/inventory/stream?productIds=1&productIds=2&productIds=3`

---

## Stock Movement API (Admin)

### 재고 이동 이력 조회

```typescript
getMovements(productId: number, page = 0, size = 20): Promise<ApiResponse<PagedResponse<StockMovement>>>
```

**Endpoint**: `GET /api/v1/shopping/inventory/{productId}/movements?page=0&size=20`

**Response**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "inventoryId": 1,
        "productId": 1,
        "movementType": "INBOUND",
        "quantity": 50,
        "previousAvailable": 45,
        "afterAvailable": 95,
        "previousReserved": 5,
        "afterReserved": 5,
        "referenceType": "PURCHASE_ORDER",
        "referenceId": "PO-001",
        "reason": "재입고",
        "performedBy": "admin",
        "createdAt": "2026-02-06T11:00:00Z"
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

**Movement Types**

| Type | 설명 |
|------|------|
| `INITIAL` | 초기 재고 설정 |
| `RESERVE` | 재고 예약 (장바구니, 주문) |
| `DEDUCT` | 재고 차감 (주문 완료) |
| `RELEASE` | 예약 해제 (주문 취소) |
| `INBOUND` | 입고 |
| `RETURN` | 반품 |
| `ADJUSTMENT` | 재고 조정 |

---

## React Hooks

### useInventoryStream

SSE 기반 실시간 재고 업데이트 Hook

```typescript
import { useInventoryStream } from '@/hooks/useInventoryStream'

export function ProductList({ products }: { products: Product[] }) {
  const productIds = products.map(p => p.id)
  const { updates, isConnected, getUpdate } = useInventoryStream({
    productIds,
    enabled: true
  })

  return (
    <div>
      {isConnected && <span>🔴 실시간 재고 업데이트 중</span>}
      {products.map((product) => {
        const inventory = getUpdate(product.id)
        const currentStock = inventory?.available ?? product.stockQuantity

        return (
          <div key={product.id}>
            <h3>{product.name}</h3>
            <p>재고: {currentStock}개</p>
            {inventory && <small>실시간 업데이트됨</small>}
          </div>
        )
      })}
    </div>
  )
}
```

**Hook Options**

```typescript
interface UseInventoryStreamOptions {
  productIds: number[]
  enabled?: boolean  // SSE 연결 활성화 여부
}
```

**Return Values**

```typescript
{
  updates: Map<number, InventoryUpdate>  // productId → InventoryUpdate 맵
  isConnected: boolean                   // SSE 연결 상태
  error: Error | null                    // 에러
  getUpdate: (productId: number) => InventoryUpdate | null  // 특정 상품 재고 조회
}
```

---

### useAdminStockMovements

재고 이동 이력 조회 Hook (Admin)

```typescript
import { useAdminStockMovements } from '@/hooks/useAdminStockMovements'

export function StockHistoryPage({ productId }: { productId: number }) {
  const { data, isLoading, error } = useAdminStockMovements({
    productId,
    page: 0,
    size: 20
  })

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러: {error.message}</div>

  return (
    <div>
      <h2>재고 이동 이력</h2>
      <table>
        <thead>
          <tr>
            <th>날짜</th>
            <th>유형</th>
            <th>수량</th>
            <th>변경 전</th>
            <th>변경 후</th>
            <th>사유</th>
          </tr>
        </thead>
        <tbody>
          {data?.content.map((movement) => (
            <tr key={movement.id}>
              <td>{movement.createdAt}</td>
              <td>{movement.movementType}</td>
              <td>{movement.quantity}</td>
              <td>{movement.previousAvailable}</td>
              <td>{movement.afterAvailable}</td>
              <td>{movement.reason}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
```

---

## SSE 이벤트

### message 이벤트

```javascript
const url = inventoryStreamApi.getStreamUrl([1, 2, 3])
const eventSource = new EventSource(url)

eventSource.onmessage = (event) => {
  const update = JSON.parse(event.data)
  // update: InventoryUpdate
  console.log('재고 업데이트:', update)
}
```

**이벤트 데이터**

```json
{
  "productId": 1,
  "available": 45,
  "reserved": 5,
  "timestamp": "2026-02-06T12:00:00Z"
}
```

---

## 타입 정의

```typescript
export interface Inventory {
  id: number
  productId: number
  availableQuantity: number
  reservedQuantity: number
  totalQuantity: number
  createdAt: string
  updatedAt?: string
}

export interface InventoryUpdateRequest {
  quantity: number
  reason?: string
}

export interface InventoryUpdate {
  productId: number
  available: number
  reserved: number
  timestamp: string
}

export type MovementType =
  | 'INITIAL'
  | 'RESERVE'
  | 'DEDUCT'
  | 'RELEASE'
  | 'INBOUND'
  | 'RETURN'
  | 'ADJUSTMENT'

export interface StockMovement {
  id: number
  inventoryId: number
  productId: number
  movementType: MovementType
  quantity: number
  previousAvailable: number
  afterAvailable: number
  previousReserved: number
  afterReserved: number
  referenceType?: string
  referenceId?: string
  reason?: string
  performedBy?: string
  createdAt: string
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `INVENTORY_NOT_FOUND` | 404 | 재고 정보를 찾을 수 없음 |
| `OUT_OF_STOCK` | 400 | 재고 부족 |
| `INVALID_QUANTITY` | 400 | 유효하지 않은 수량 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Product API](./product-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
