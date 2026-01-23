# API Integration

## 학습 목표
- fetch, axios 기반 API 호출 패턴 이해
- Shopping Frontend의 API 클라이언트 구조 분석
- 에러 처리 및 인증 통합 학습

---

## 1. API 클라이언트 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          API CLIENT                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   client.ts      ─────►  Base 설정 (baseURL, headers, interceptors)         │
│   endpoints.ts   ─────►  API 엔드포인트별 함수                               │
│   Custom Hooks   ─────►  useState + useEffect 패턴                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe 코드 분석

### 2.1 API 클라이언트 (api/client.ts)

```tsx
import axios, { AxiosInstance } from 'axios'

// Base URL 설정
const baseURL = import.meta.env.VITE_SHOPPING_API_URL || 'http://localhost:8082/api/v1'

// Axios 인스턴스 생성
const apiClient: AxiosInstance = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 쿠키 포함
})

// Request Interceptor (요청 전 처리)
apiClient.interceptors.request.use(
  (config) => {
    // 인증 토큰 추가
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`)
    return config
  },
  (error) => {
    console.error('[API Request Error]', error)
    return Promise.reject(error)
  }
)

// Response Interceptor (응답 후 처리)
apiClient.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.config.url}`, response.data)
    return response
  },
  (error) => {
    console.error('[API Response Error]', error.response?.data || error.message)

    // 401 Unauthorized - 로그인 페이지로 리다이렉트
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      window.location.href = '/login'
    }

    // 403 Forbidden - 권한 없음 페이지로 리다이렉트
    if (error.response?.status === 403) {
      window.location.href = '/403'
    }

    return Promise.reject(error)
  }
)

export default apiClient
```

### 2.2 API 엔드포인트 (api/endpoints.ts)

```tsx
import apiClient from './client'
import type {
  ApiResponse,
  PageResponse,
  Product,
  Cart,
  Order,
  Coupon,
  UserCoupon,
  TimeDeal
} from '@/types'

// ============================================
// Product API
// ============================================
export const productApi = {
  // 상품 목록 조회
  getProducts: async (page = 0, size = 12, category?: string) => {
    const params: any = { page, size }
    if (category) params.category = category

    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      '/products',
      { params }
    )
    return response.data
  },

  // 상품 검색
  searchProducts: async (keyword: string, page = 0, size = 12) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      '/products/search',
      { params: { keyword, page, size } }
    )
    return response.data
  },

  // 상품 상세 조회
  getProduct: async (productId: number) => {
    const response = await apiClient.get<ApiResponse<Product>>(
      `/products/${productId}`
    )
    return response.data
  }
}

// ============================================
// Cart API
// ============================================
export const cartApi = {
  // 장바구니 조회
  getCart: async () => {
    const response = await apiClient.get<ApiResponse<Cart>>('/cart')
    return response.data
  },

  // 장바구니에 상품 추가
  addItem: async (request: { productId: number; quantity: number }) => {
    const response = await apiClient.post<ApiResponse<Cart>>('/cart/items', request)
    return response.data
  },

  // 장바구니 상품 수량 변경
  updateItemQuantity: async (itemId: number, quantity: number) => {
    const response = await apiClient.put<ApiResponse<Cart>>(
      `/cart/items/${itemId}`,
      { quantity }
    )
    return response.data
  },

  // 장바구니 상품 삭제
  removeItem: async (itemId: number) => {
    const response = await apiClient.delete<ApiResponse<Cart>>(
      `/cart/items/${itemId}`
    )
    return response.data
  },

  // 장바구니 비우기
  clearCart: async () => {
    const response = await apiClient.delete<ApiResponse<void>>('/cart')
    return response.data
  }
}

// ============================================
// Order API
// ============================================
export const orderApi = {
  // 주문 생성
  createOrder: async (request: {
    shippingAddress: AddressRequest
    userCouponId?: number
  }) => {
    const response = await apiClient.post<ApiResponse<Order>>('/orders', request)
    return response.data
  },

  // 주문 목록 조회
  getOrders: async (page = 0, size = 10) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Order>>>(
      '/orders',
      { params: { page, size } }
    )
    return response.data
  },

  // 주문 상세 조회
  getOrder: async (orderNumber: string) => {
    const response = await apiClient.get<ApiResponse<Order>>(
      `/orders/${orderNumber}`
    )
    return response.data
  }
}

// ============================================
// Coupon API
// ============================================
export const couponApi = {
  // 발급 가능한 쿠폰 목록
  getAvailableCoupons: async () => {
    const response = await apiClient.get<ApiResponse<Coupon[]>>(
      '/coupons/available'
    )
    return response.data
  },

  // 사용자 쿠폰 목록
  getUserCoupons: async () => {
    const response = await apiClient.get<ApiResponse<UserCoupon[]>>(
      '/coupons/user'
    )
    return response.data
  },

  // 사용 가능한 사용자 쿠폰 목록
  getAvailableUserCoupons: async () => {
    const response = await apiClient.get<ApiResponse<UserCoupon[]>>(
      '/coupons/user/available'
    )
    return response.data
  },

  // 쿠폰 발급
  issueCoupon: async (couponId: number) => {
    const response = await apiClient.post<ApiResponse<UserCoupon>>(
      `/coupons/${couponId}/issue`
    )
    return response.data
  }
}

// ============================================
// TimeDeal API
// ============================================
export const timeDealApi = {
  // 타임딜 목록 조회
  getTimeDeals: async (params?: {
    status?: string
    page?: number
    size?: number
  }) => {
    const response = await apiClient.get<ApiResponse<PageResponse<TimeDeal>>>(
      '/time-deals',
      { params }
    )
    return response.data
  },

  // 타임딜 상세 조회
  getTimeDeal: async (id: number) => {
    const response = await apiClient.get<ApiResponse<TimeDeal>>(
      `/time-deals/${id}`
    )
    return response.data
  }
}
```

---

## 3. Custom Hooks 패턴

### 3.1 useProducts Hook

```tsx
// hooks/useProducts.ts
import { useState, useEffect, useCallback } from 'react'
import { productApi } from '@/api/endpoints'
import type { Product, PageResponse } from '@/types'

export function useProducts(params?: {
  page?: number
  size?: number
  category?: string
}) {
  const [data, setData] = useState<PageResponse<Product> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchProducts = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)

      const response = await productApi.getProducts(
        params?.page ?? 0,
        params?.size ?? 12,
        params?.category
      )

      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch products'))
    } finally {
      setIsLoading(false)
    }
  }, [params?.page, params?.size, params?.category])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  return { data, isLoading, error, refetch: fetchProducts }
}
```

### 3.2 컴포넌트에서 사용

```tsx
function ProductListPage() {
  const [currentPage, setCurrentPage] = useState(0)
  const { data, isLoading, error, refetch } = useProducts({
    page: currentPage,
    size: 12
  })

  if (isLoading) return <Spinner />
  if (error) return <ErrorMessage error={error} onRetry={refetch} />

  return (
    <div>
      <div className="grid grid-cols-3 gap-6">
        {data?.content.map(product => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      <Pagination
        currentPage={currentPage}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setCurrentPage}
      />
    </div>
  )
}
```

---

## 4. Mutation 패턴

### 4.1 POST/PUT/DELETE 요청

```tsx
// hooks/useCreateOrder.ts
import { useState, useCallback } from 'react'
import { orderApi } from '@/api/endpoints'
import type { Order, AddressRequest } from '@/types'

export function useCreateOrder() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const createOrder = useCallback(async (request: {
    shippingAddress: AddressRequest
    userCouponId?: number
  }) => {
    try {
      setIsLoading(true)
      setError(null)

      const response = await orderApi.createOrder(request)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to create order')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: createOrder, isPending: isLoading, error }
}
```

### 4.2 사용 예시

```tsx
function CheckoutPage() {
  const { mutateAsync, isPending, error } = useCreateOrder()
  const navigate = useNavigate()

  const handleCreateOrder = async () => {
    try {
      const order = await mutateAsync({
        shippingAddress: address,
        userCouponId: selectedCoupon?.id
      })

      alert('Order created successfully!')
      navigate(`/orders/${order.orderNumber}`)
    } catch (error) {
      // 에러는 이미 상태에 저장됨
      console.error('Order creation failed', error)
    }
  }

  return (
    <div>
      <button onClick={handleCreateOrder} disabled={isPending}>
        {isPending ? 'Creating...' : 'Create Order'}
      </button>
      {error && <p className="text-red-500">{error.message}</p>}
    </div>
  )
}
```

---

## 5. 에러 처리 패턴

### 5.1 API 응답 타입

```tsx
// types/index.ts
export interface ApiResponse<T> {
  success: boolean
  data: T
  error?: {
    code: string
    message: string
  }
  timestamp: string
}
```

### 5.2 에러 처리

```tsx
// hooks/useCoupons.ts
const fetchCoupons = useCallback(async () => {
  try {
    setIsLoading(true)
    setError(null)

    const response = await couponApi.getAvailableCoupons()

    if (response.success) {
      setData(response.data)
    } else {
      // API는 성공했지만 비즈니스 로직 실패
      throw new Error(response.error?.message || 'Failed to fetch coupons')
    }
  } catch (e: any) {
    // Axios 에러 처리
    if (e.response) {
      // 서버 응답 있음 (4xx, 5xx)
      setError(new Error(e.response.data?.error?.message || e.message))
    } else if (e.request) {
      // 요청은 보냈지만 응답 없음
      setError(new Error('No response from server'))
    } else {
      // 요청 설정 중 에러
      setError(new Error(e.message))
    }
  } finally {
    setIsLoading(false)
  }
}, [])
```

---

## 6. 낙관적 업데이트 (Optimistic Update)

### 6.1 장바구니 수량 변경

```tsx
export const useCartStore = create<CartState>((set, get) => ({
  cart: null,

  updateItemQuantity: async (itemId: number, quantity: number) => {
    const originalCart = get().cart

    try {
      // 1. 즉시 UI 업데이트 (낙관적)
      set(state => ({
        cart: state.cart ? {
          ...state.cart,
          items: state.cart.items.map(item =>
            item.id === itemId ? { ...item, quantity } : item
          )
        } : null
      }))

      // 2. 서버에 요청
      const response = await cartApi.updateItemQuantity(itemId, quantity)

      // 3. 서버 응답으로 상태 동기화
      set({ cart: response.data })
    } catch (error) {
      // 4. 실패 시 원래 상태로 롤백
      set({ cart: originalCart })
      throw error
    }
  }
}))
```

---

## 7. 폴링 (Polling) 패턴

### 7.1 Queue 상태 폴링

```tsx
// hooks/useQueue.ts
export function useQueue(eventType: string, eventId: number) {
  const [status, setStatus] = useState<QueueStatus | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkStatus = useCallback(async () => {
    try {
      const response = await queueApi.getStatus(eventType, eventId)
      setStatus(response.data)

      // 대기열 통과 시 폴링 중지
      if (response.data.status === 'PASSED') {
        return true
      }
    } catch (error) {
      console.error('Failed to check queue status', error)
    }
    return false
  }, [eventType, eventId])

  useEffect(() => {
    setIsLoading(true)
    checkStatus().finally(() => setIsLoading(false))

    // 5초마다 폴링
    const interval = setInterval(async () => {
      const passed = await checkStatus()
      if (passed) {
        clearInterval(interval)
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [checkStatus])

  return { status, isLoading, refetch: checkStatus }
}
```

---

## 8. 파일 업로드

### 8.1 FormData 사용

```tsx
async function uploadProductImage(productId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<ApiResponse<{ imageUrl: string }>>(
    `/products/${productId}/image`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )

  return response.data
}

// 컴포넌트에서 사용
function ProductImageUpload({ productId }: Props) {
  const [uploading, setUploading] = useState(false)

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    try {
      setUploading(true)
      const result = await uploadProductImage(productId, file)
      alert('Image uploaded: ' + result.data.imageUrl)
    } catch (error) {
      alert('Upload failed')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <input type="file" onChange={handleFileChange} disabled={uploading} />
      {uploading && <Spinner />}
    </div>
  )
}
```

---

## 9. 핵심 정리

| 패턴 | 설명 |
|------|------|
| **apiClient** | Axios 인스턴스 (baseURL, interceptors) |
| **endpoints** | API 함수 모음 |
| **Custom Hooks** | useState + useEffect 패턴 |
| **Mutation** | POST/PUT/DELETE 요청 |
| **낙관적 업데이트** | UI 먼저 업데이트 후 동기화 |
| **폴링** | setInterval로 주기적 조회 |
| **에러 처리** | try-catch + 상태 관리 |

---

## 다음 학습

- [Custom Hooks](./custom-hooks.md)
- [Zustand 상태 관리](./zustand-state.md)
- [Product List 구현](../shopping/product-list.md)
