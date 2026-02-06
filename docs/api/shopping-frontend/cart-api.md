---
id: api-shopping-cart
title: Shopping Cart API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, cart, zustand]
related: [api-shopping-types, api-shopping-order]
---

# Shopping Cart API

> 장바구니 관리 API (Zustand Store 기반)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/cart` |
| **인증** | Bearer Token (필수) |
| **상태 관리** | Zustand Store (`useCartStore`) |
| **엔드포인트** | `cartApi` |

---

## API 엔드포인트

### 장바구니 조회

```typescript
getCart(): Promise<ApiResponse<Cart>>
```

**Endpoint**

```http
GET /api/v1/shopping/cart
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "status": "ACTIVE",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "스프링 부트 완벽 가이드",
        "price": 35000,
        "quantity": 2,
        "addedAt": "2026-02-06T10:00:00Z"
      }
    ],
    "totalAmount": 70000,
    "itemCount": 1,
    "totalQuantity": 2,
    "createdAt": "2026-02-06T09:00:00Z",
    "updatedAt": "2026-02-06T10:00:00Z"
  }
}
```

---

### 장바구니에 상품 추가

```typescript
addItem(data: AddCartItemRequest): Promise<ApiResponse<Cart>>
```

**Endpoint**

```http
POST /api/v1/shopping/cart/items
Content-Type: application/json
Authorization: Bearer {token}

{
  "productId": 10,
  "quantity": 2
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `productId` | number | ✅ | 상품 ID | 유효한 상품 ID |
| `quantity` | number | ✅ | 수량 | 1 이상, 재고 이하 |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "status": "ACTIVE",
    "items": [
      {
        "id": 2,
        "productId": 10,
        "productName": "React 완벽 가이드",
        "price": 38000,
        "quantity": 2,
        "addedAt": "2026-02-06T10:30:00Z"
      }
    ],
    "totalAmount": 76000,
    "itemCount": 1,
    "totalQuantity": 2
  }
}
```

**Error (재고 부족)**

```json
{
  "success": false,
  "code": "OUT_OF_STOCK",
  "message": "재고가 부족합니다."
}
```

---

### 장바구니 항목 수량 변경

```typescript
updateItem(itemId: number, data: UpdateCartItemRequest): Promise<ApiResponse<Cart>>
```

**Endpoint**

```http
PUT /api/v1/shopping/cart/items/{itemId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "quantity": 5
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `quantity` | number | ✅ | 새 수량 (1 이상) |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "items": [
      {
        "id": 1,
        "quantity": 5
      }
    ],
    "totalAmount": 175000,
    "totalQuantity": 5
  }
}
```

---

### 장바구니 항목 삭제

```typescript
removeItem(itemId: number): Promise<ApiResponse<Cart>>
```

**Endpoint**

```http
DELETE /api/v1/shopping/cart/items/{itemId}
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "items": [],
    "totalAmount": 0,
    "itemCount": 0,
    "totalQuantity": 0
  }
}
```

---

### 장바구니 비우기

```typescript
clearCart(): Promise<ApiResponse<void>>
```

**Endpoint**

```http
DELETE /api/v1/shopping/cart
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "message": "장바구니가 비워졌습니다."
}
```

---

### 장바구니 체크아웃

```typescript
checkout(): Promise<ApiResponse<Cart>>
```

장바구니 상태를 ACTIVE에서 CHECKED_OUT으로 변경합니다.

**Endpoint**

```http
POST /api/v1/shopping/cart/checkout
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "CHECKED_OUT",
    "items": [/*...*/],
    "totalAmount": 70000
  }
}
```

---

## Zustand Store (useCartStore)

장바구니는 Zustand로 전역 상태 관리합니다.

### Store 구조

```typescript
// src/stores/cartStore.ts
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import { cartApi } from '@/api/endpoints'
import type { Cart } from '@/types'

interface CartState {
  // State
  cart: Cart | null
  loading: boolean
  error: string | null
  itemCount: number
  totalAmount: number

  // Actions
  fetchCart: () => Promise<void>
  addItem: (productId: number, productName: string, price: number, quantity: number) => Promise<void>
  updateItemQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clearCart: () => Promise<void>
  reset: () => void
}

export const useCartStore = create<CartState>()(
  devtools((set, get) => ({
    cart: null,
    loading: false,
    error: null,
    itemCount: 0,
    totalAmount: 0,

    fetchCart: async () => {
      set({ loading: true, error: null })
      try {
        const response = await cartApi.getCart()
        const cart = response.data
        set({
          cart,
          itemCount: cart.itemCount,
          totalAmount: cart.totalAmount,
          loading: false
        })
      } catch (error) {
        // 404: 장바구니 없음 (신규 사용자)
        if (error.response?.status === 404) {
          set({ cart: emptyCart(), itemCount: 0, totalAmount: 0, loading: false })
        } else {
          set({ error: 'Failed to fetch cart', loading: false })
        }
      }
    },

    addItem: async (productId, productName, price, quantity) => {
      set({ loading: true, error: null })
      try {
        const response = await cartApi.addItem({ productId, quantity })
        const cart = response.data
        set({
          cart,
          itemCount: cart.itemCount,
          totalAmount: cart.totalAmount,
          loading: false
        })
      } catch (error) {
        set({ error: 'Failed to add item', loading: false })
        throw error
      }
    },

    updateItemQuantity: async (itemId, quantity) => {
      set({ loading: true, error: null })
      try {
        const response = await cartApi.updateItem(itemId, { quantity })
        const cart = response.data
        set({
          cart,
          itemCount: cart.itemCount,
          totalAmount: cart.totalAmount,
          loading: false
        })
      } catch (error) {
        set({ error: 'Failed to update item', loading: false })
        throw error
      }
    },

    removeItem: async (itemId) => {
      set({ loading: true, error: null })
      try {
        const response = await cartApi.removeItem(itemId)
        const cart = response.data
        set({
          cart,
          itemCount: cart.itemCount,
          totalAmount: cart.totalAmount,
          loading: false
        })
      } catch (error) {
        set({ error: 'Failed to remove item', loading: false })
        throw error
      }
    },

    clearCart: async () => {
      set({ loading: true, error: null })
      try {
        await cartApi.clearCart()
        set({ cart: emptyCart(), itemCount: 0, totalAmount: 0, loading: false })
      } catch (error) {
        set({ error: 'Failed to clear cart', loading: false })
        throw error
      }
    },

    reset: () => {
      set({ cart: null, loading: false, error: null, itemCount: 0, totalAmount: 0 })
    }
  }), { name: 'CartStore' })
)
```

---

## 사용 예시

### 장바구니 조회

```typescript
import { useEffect } from 'react'
import { useCartStore } from '@/stores/cartStore'

export function CartButton() {
  const { itemCount, fetchCart } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  return (
    <button>
      장바구니 ({itemCount})
    </button>
  )
}
```

### 장바구니에 상품 추가

```typescript
import { useCartStore } from '@/stores/cartStore'

export function ProductCard({ product }: { product: Product }) {
  const { addItem } = useCartStore()
  const [quantity, setQuantity] = useState(1)

  const handleAddToCart = async () => {
    try {
      await addItem(product.id, product.name, product.price, quantity)
      alert('장바구니에 추가되었습니다')
    } catch (error) {
      alert('추가 실패')
    }
  }

  return (
    <div>
      <h3>{product.name}</h3>
      <p>{product.price}원</p>
      <input
        type="number"
        min="1"
        value={quantity}
        onChange={(e) => setQuantity(Number(e.target.value))}
      />
      <button onClick={handleAddToCart}>장바구니 담기</button>
    </div>
  )
}
```

### 장바구니 페이지

```typescript
import { useEffect } from 'react'
import { useCartStore } from '@/stores/cartStore'

export function CartPage() {
  const { cart, loading, fetchCart, updateItemQuantity, removeItem } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  if (loading) return <div>로딩 중...</div>
  if (!cart || cart.items.length === 0) return <div>장바구니가 비어있습니다</div>

  return (
    <div>
      <h1>장바구니</h1>
      {cart.items.map((item) => (
        <div key={item.id}>
          <h3>{item.productName}</h3>
          <p>가격: {item.price}원</p>
          <input
            type="number"
            min="1"
            value={item.quantity}
            onChange={(e) => updateItemQuantity(item.id, Number(e.target.value))}
          />
          <button onClick={() => removeItem(item.id)}>삭제</button>
        </div>
      ))}
      <div>
        <h2>총 금액: {cart.totalAmount}원</h2>
        <button>주문하기</button>
      </div>
    </div>
  )
}
```

### 수량 변경

```typescript
const { updateItemQuantity } = useCartStore()

const handleQuantityChange = async (itemId: number, newQuantity: number) => {
  try {
    await updateItemQuantity(itemId, newQuantity)
  } catch (error) {
    alert('수량 변경 실패')
  }
}
```

### 항목 삭제

```typescript
const { removeItem } = useCartStore()

const handleRemove = async (itemId: number) => {
  if (!confirm('장바구니에서 삭제하시겠습니까?')) return

  try {
    await removeItem(itemId)
  } catch (error) {
    alert('삭제 실패')
  }
}
```

### 장바구니 비우기

```typescript
const { clearCart } = useCartStore()

const handleClearCart = async () => {
  if (!confirm('장바구니를 모두 비우시겠습니까?')) return

  try {
    await clearCart()
    alert('장바구니가 비워졌습니다')
  } catch (error) {
    alert('실패')
  }
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `CART_NOT_FOUND` | 404 | 장바구니를 찾을 수 없음 (신규 사용자) |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 항목을 찾을 수 없음 |
| `OUT_OF_STOCK` | 400 | 재고 부족 |
| `INVALID_QUANTITY` | 400 | 유효하지 않은 수량 |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없음 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Order API](./order-api.md)
- [Product API](./product-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
