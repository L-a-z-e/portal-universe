---
id: api-shopping-client
title: Shopping API Client
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Documenter Agent
tags: [api, shopping, frontend, client, react, module-federation]
related: []
---

# Shopping API Client

> shopping-frontend의 API 클라이언트 설정 및 사용법 (React 18 기반)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping` |
| **인증** | Bearer Token (JWT) |
| **Content-Type** | `application/json` |
| **클라이언트** | Axios |
| **상태 관리** | Zustand (장바구니), useState + useEffect (기타) |

shopping-frontend는 API Gateway를 통해 shopping-service와 통신합니다. 모든 요청은 JWT 토큰을 포함해야 하며, axios interceptor를 통해 자동으로 헤더에 추가됩니다.

---

## API 클라이언트 구조

### 파일 구조

```
src/
├── api/
│   ├── client.ts          # API 클라이언트 설정
│   └── endpoints.ts       # 엔드포인트 함수 정의
├── hooks/                 # Custom React Hooks (API 호출)
├── stores/
│   └── cartStore.ts       # Zustand Store (장바구니)
└── types/
    ├── index.ts           # 메인 타입 정의
    └── admin.ts           # 관리자 타입
```

---

## API Client 설정

### Module Federation 지원

shopping-frontend는 **Embedded** 모드와 **Standalone** 모드를 모두 지원합니다.

```typescript
// src/api/client.ts
import axios, { AxiosInstance } from 'axios'
import { getPortalApiClient } from '@portal/react-bridge'

/**
 * API Client 반환
 * - Embedded: portal/api의 apiClient 사용 (토큰 갱신, 401/429 재시도)
 * - Standalone: local fallback client 사용
 */
export const getApiClient = (): AxiosInstance => {
  return getPortalApiClient() ?? getLocalClient()
}
```

### Embedded 모드 (Host에서 실행)

- `@portal/react-bridge`를 통해 portal/api의 `apiClient`를 공유
- 토큰 자동 갱신, 401/429 재시도 기능 내장
- `getPortalApiClient()` → portal/api의 axios 인스턴스

### Standalone 모드 (독립 실행)

- Local Axios 인스턴스 생성 (lazy)
- 토큰은 `window.__PORTAL_GET_ACCESS_TOKEN__()`에서 가져옴
- 401 에러 시 `window.__PORTAL_ON_AUTH_ERROR__()` 호출

```typescript
const localClient = axios.create({
  baseURL: getBaseUrl(),
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true
})

// Request Interceptor: 토큰 자동 첨부
localClient.interceptors.request.use((config) => {
  const token = getAdapter('auth').getAccessToken?.() ??
                window.__PORTAL_GET_ACCESS_TOKEN__?.() ??
                window.__PORTAL_ACCESS_TOKEN__
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response Interceptor: 에러 핸들링
localClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.__PORTAL_ON_AUTH_ERROR__?.()
    }
    return Promise.reject(error)
  }
)
```

---

## API 엔드포인트

### 엔드포인트 정의 패턴

```typescript
// src/api/endpoints.ts
import { getApiClient } from './client'
import type { ApiResponse, PagedResponse, Product } from '@/types'

const API_PREFIX = '/api/v1/shopping'

export const productApi = {
  getProducts: async (page = 0, size = 12, category?: string) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    if (category) params.append('category', category)
    const response = await getApiClient().get<ApiResponse<PagedResponse<Product>>>(
      `${API_PREFIX}/products?${params}`
    )
    return response.data
  },

  getProduct: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  }
}
```

### 엔드포인트 그룹

| 그룹 | 경로 |
|------|------|
| productApi | `/api/v1/shopping/products` |
| adminProductApi | `/api/v1/shopping/admin/products` |
| inventoryApi | `/api/v1/shopping/inventory` |
| cartApi | `/api/v1/shopping/cart` |
| orderApi | `/api/v1/shopping/orders` |
| paymentApi | `/api/v1/shopping/payments` |
| deliveryApi | `/api/v1/shopping/deliveries` |
| couponApi | `/api/v1/shopping/coupons` |
| timeDealApi | `/api/v1/shopping/time-deals` |
| queueApi | `/api/v1/shopping/queue` |
| searchApi | `/api/v1/shopping/search` |
| inventoryStreamApi | `/api/v1/shopping/inventory/stream` |
| productReviewApi | `/api/v1/shopping/products/{id}/with-reviews` |

---

## React Hooks 패턴

### Custom Hook 구조

shopping-frontend는 **useState + useEffect** 패턴을 사용합니다 (React Query 미사용).

```typescript
// src/hooks/useProducts.ts
import { useState, useEffect, useCallback } from 'react'
import { productApi } from '@/api/endpoints'
import type { Product, PagedResponse } from '@/types'

export function useProducts(filters: { page: number; size: number; category?: string }) {
  const [data, setData] = useState<PagedResponse<Product> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchProducts = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await productApi.getProducts(filters.page, filters.size, filters.category)
      setData(response.data)
    } catch (err) {
      setError(err as Error)
    } finally {
      setIsLoading(false)
    }
  }, [filters.page, filters.size, filters.category])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  return { data, isLoading, error, refetch: fetchProducts }
}
```

### Mutation Hook 패턴

```typescript
export function useCreateProduct() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const createProduct = useCallback(async (data: ProductFormData) => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.createProduct(data)
      return response.data
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    mutateAsync: createProduct,
    isPending: isLoading,
    error
  }
}
```

---

## Zustand Store (장바구니)

장바구니는 Zustand로 전역 상태 관리합니다.

```typescript
// src/stores/cartStore.ts
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import { cartApi } from '@/api/endpoints'
import type { Cart } from '@/types'

interface CartState {
  cart: Cart | null
  loading: boolean
  error: string | null
  itemCount: number
  totalAmount: number

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
        set({ error: 'Failed to fetch cart', loading: false })
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
    }
  }), { name: 'CartStore' })
)
```

### 컴포넌트에서 사용

```typescript
import { useCartStore } from '@/stores/cartStore'

export function CartButton() {
  const { itemCount, fetchCart } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  return <button>장바구니 ({itemCount})</button>
}
```

---

## 사용 예시

### 상품 목록 조회

```typescript
import { useAdminProducts } from '@/hooks/useAdminProducts'

export function ProductListPage() {
  const [filters, setFilters] = useState({
    page: 0,
    size: 20,
    keyword: '',
    category: '',
    sortBy: 'createdAt' as const,
    sortOrder: 'desc' as const
  })

  const { data, isLoading, error, refetch } = useAdminProducts(filters)

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러: {error.message}</div>

  return (
    <div>
      {data?.data.content.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  )
}
```

### 상품 생성 (Mutation)

```typescript
import { useCreateProduct } from '@/hooks/useAdminProducts'

export function ProductCreateModal() {
  const { mutateAsync, isPending } = useCreateProduct()

  const handleSubmit = async (formData: ProductFormData) => {
    try {
      const product = await mutateAsync(formData)
      console.log('생성된 상품:', product)
    } catch (error) {
      console.error('생성 실패:', error)
    }
  }

  return (
    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(formData) }}>
      {/* form fields */}
      <button type="submit" disabled={isPending}>
        {isPending ? '생성 중...' : '상품 생성'}
      </button>
    </form>
  )
}
```

### 장바구니 사용

```typescript
import { useCartStore } from '@/stores/cartStore'

export function ProductDetailPage() {
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
    <button onClick={handleAddToCart}>장바구니 담기</button>
  )
}
```

### SSE (Server-Sent Events) 사용

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
      {isConnected && <span>실시간 재고 연결됨</span>}
      {products.map(product => {
        const inventory = getUpdate(product.id)
        return (
          <div key={product.id}>
            <h3>{product.name}</h3>
            <p>재고: {inventory?.available ?? product.stockQuantity}</p>
          </div>
        )
      })}
    </div>
  )
}
```

---

## 에러 처리

### 일반적인 에러 응답

```typescript
interface ApiErrorResponse {
  success: false
  code: string
  message: string
  data: null | {
    errors?: FieldError[]
  }
  timestamp: string
}
```

### 에러 핸들링 예시

```typescript
try {
  await productApi.createProduct(data)
} catch (error) {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data
    if (apiError.code === 'S002') {
      alert('재고가 부족합니다')
    } else {
      alert(apiError.message || '오류가 발생했습니다')
    }
  }
}
```

---

## 인증 및 권한

### JWT 토큰

모든 API 요청은 JWT Bearer 토큰을 포함합니다:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 권한 레벨

| 레벨 | 접근 가능 API |
|------|---------------|
| **USER** | 조회, 장바구니, 주문 생성, 쿠폰 발급, 타임딜 구매 |
| **ADMIN** | 상품/재고 관리, 배송 상태 변경, 쿠폰/타임딜 생성, 대기열 관리 |

---

## 관련 문서

- [Product API](./product-api.md)
- [Inventory API](./inventory-api.md)
- [Cart API](./cart-api.md)
- [Order API](./order-api.md)
- [Coupon API](./coupon-api.md)
- [TimeDeal API](./timedeal-api.md)
- [Queue API](./queue-api.md)
- [Search API](./search-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
