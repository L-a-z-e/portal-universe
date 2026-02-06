# Shopping Frontend 구현 가이드

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [타입 정의](#3-타입-정의)
4. [API 레이어](#4-api-레이어)
5. [상태 관리 (Zustand)](#5-상태-관리-zustand)
6. [라우터 설정](#6-라우터-설정)
7. [페이지 컴포넌트](#7-페이지-컴포넌트)
8. [공통 컴포넌트](#8-공통-컴포넌트)
9. [스타일링](#9-스타일링)
10. [Module Federation 통합](#10-module-federation-통합)
11. [실행 및 테스트](#11-실행-및-테스트)

---

## 1. 프로젝트 개요

### 1.1 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18.2.0 | UI 라이브러리 |
| TypeScript | 5.9 | 타입 안정성 |
| Vite | 7.x | 빌드 도구 |
| React Router | 7.1.5 | 클라이언트 라우팅 |
| Zustand | 5.0.3 | 상태 관리 |
| Axios | 1.12.2 | HTTP 클라이언트 |
| TailwindCSS | 3.x | 유틸리티 CSS |
| Module Federation | - | 마이크로 프론트엔드 |

### 1.2 주요 기능

```
Shopping Frontend
├── 상품 목록 조회 (검색, 페이지네이션)
├── 상품 상세 보기 (재고 확인)
├── 장바구니 관리 (추가, 수량 변경, 삭제)
├── 주문/결제 프로세스 (3단계 체크아웃)
├── 주문 내역 조회
└── 배송 추적
```

### 1.3 실행 모드

| 모드 | 설명 | URL |
|------|------|-----|
| **Standalone** | 독립 실행 (개발/테스트) | http://localhost:30002 |
| **Embedded** | Portal Shell에 통합 | http://localhost:30000/shopping |

---

## 2. 프로젝트 구조

```
shopping-frontend/
├── src/
│   ├── api/
│   │   ├── client.ts              # Axios 인스턴스 설정
│   │   └── endpoints.ts           # API 엔드포인트 함수
│   │
│   ├── components/
│   │   ├── ProductCard.tsx        # 상품 카드 컴포넌트
│   │   └── CartItem.tsx           # 장바구니 아이템 컴포넌트
│   │
│   ├── pages/
│   │   ├── ProductListPage.tsx    # 상품 목록 페이지
│   │   ├── ProductDetailPage.tsx  # 상품 상세 페이지
│   │   ├── CartPage.tsx           # 장바구니 페이지
│   │   ├── CheckoutPage.tsx       # 체크아웃 페이지
│   │   ├── OrderListPage.tsx      # 주문 목록 페이지
│   │   └── OrderDetailPage.tsx    # 주문 상세 페이지
│   │
│   ├── router/
│   │   └── index.tsx              # React Router 설정
│   │
│   ├── stores/
│   │   ├── cartStore.ts           # 장바구니 상태 관리
│   │   └── authStore.ts           # 인증 상태 (Portal 연동)
│   │
│   ├── styles/
│   │   └── index.scss             # 전역 스타일
│   │
│   ├── types/
│   │   └── index.ts               # TypeScript 타입 정의
│   │
│   ├── App.tsx                    # 루트 컴포넌트
│   ├── bootstrap.tsx              # Module Federation 진입점
│   └── main.tsx                   # Standalone 진입점
│
├── index.html                     # HTML 템플릿
├── vite.config.ts                 # Vite 설정
├── tailwind.config.js             # Tailwind 설정
├── postcss.config.js              # PostCSS 설정
├── tsconfig.json                  # TypeScript 설정
└── env.d.ts                       # 환경변수 타입
```

---

## 3. 타입 정의

### 3.1 API 응답 타입

```typescript
// src/types/index.ts

/**
 * 공통 API 응답 래퍼
 * Backend의 ApiResponse<T>와 매칭
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
}

/**
 * 페이지네이션 응답
 * Spring Data의 Page<T>와 매칭
 */
export interface PaginatedResponse<T> {
  content: T[];           // 데이터 목록
  totalElements: number;  // 전체 개수
  totalPages: number;     // 전체 페이지 수
  size: number;           // 페이지 크기
  number: number;         // 현재 페이지 (0-based)
  first: boolean;         // 첫 페이지 여부
  last: boolean;          // 마지막 페이지 여부
  empty: boolean;         // 빈 페이지 여부
}
```

### 3.2 도메인 타입

```typescript
// 상품
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  category?: string;
  createdAt: string;
  updatedAt: string;
}

// 재고
export interface Inventory {
  id: number;
  productId: number;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;  // quantity - reservedQuantity
}

// 장바구니
export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;   // 스냅샷 (상품명 변경 대비)
  price: number;         // 스냅샷 (가격 변경 대비)
  quantity: number;
  subtotal: number;      // price * quantity
}
```

### 3.3 주문/배송 타입

```typescript
// 주문 상태 (Backend Enum과 매칭)
export type OrderStatus =
  | 'PENDING'     // 주문 대기
  | 'CONFIRMED'   // 주문 확인
  | 'PAID'        // 결제 완료
  | 'SHIPPING'    // 배송 중
  | 'DELIVERED'   // 배송 완료
  | 'CANCELLED'   // 취소됨
  | 'REFUNDED';   // 환불됨

// 상태 한글 레이블
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: '주문 대기',
  CONFIRMED: '주문 확인',
  PAID: '결제 완료',
  SHIPPING: '배송 중',
  DELIVERED: '배송 완료',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료'
};

// 주문
export interface Order {
  id: number;
  orderNumber: string;         // UUID 기반
  userId: number;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  shippingAddress: ShippingAddress;
  cancelReason?: string;
  cancelledAt?: string;
  createdAt: string;
  updatedAt: string;
}

// 배송 주소 (Embedded Value Object)
export interface ShippingAddress {
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  address1: string;
  address2?: string;
}
```

---

## 4. API 레이어

### 4.1 Axios 클라이언트

```typescript
// src/api/client.ts
import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosError } from 'axios';

/**
 * API Base URL 결정
 * - Embedded: Portal Shell 프록시 사용
 * - Standalone: 직접 API Gateway 호출
 */
const getBaseUrl = (): string => {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

  if (isEmbedded) {
    // Portal Shell이 /api/** 프록시 설정
    return '';
  }

  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
};

/**
 * Axios 인스턴스 생성
 */
const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: getBaseUrl(),
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    },
    withCredentials: true  // 쿠키 전송 (CORS)
  });

  // Request Interceptor: JWT 토큰 주입
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = localStorage.getItem('accessToken');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response Interceptor: 에러 처리
  client.interceptors.response.use(
    (response) => {
      console.log(`[API] Response:`, response.status);
      return response;
    },
    (error: AxiosError) => {
      console.error(`[API] Error:`, error.response?.status, error.message);

      // 401: 인증 실패 → 로그인 페이지로
      if (error.response?.status === 401) {
        localStorage.removeItem('accessToken');
        // Portal Shell의 로그인으로 리다이렉트
        if (window.__POWERED_BY_PORTAL_SHELL__) {
          window.location.href = '/login';
        }
      }

      return Promise.reject(error);
    }
  );

  return client;
};

export const apiClient = createApiClient();
```

### 4.2 API 엔드포인트

```typescript
// src/api/endpoints.ts
import { apiClient } from './client';
import type {
  ApiResponse, PaginatedResponse,
  Product, Inventory, Cart, Order, Payment, Delivery
} from '@/types';

// ============================================
// Product API
// ============================================
export const productApi = {
  /**
   * 상품 목록 조회
   * GET /api/v1/shopping/products
   */
  getProducts: async (params?: {
    page?: number;
    size?: number;
    keyword?: string;
    category?: string;
  }) => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Product>>>(
      '/api/v1/shopping/products',
      { params }
    );
    return response.data;
  },

  /**
   * 상품 상세 조회
   * GET /api/v1/shopping/products/{id}
   */
  getProduct: async (id: number) => {
    const response = await apiClient.get<ApiResponse<Product>>(
      `/api/v1/shopping/products/${id}`
    );
    return response.data;
  }
};

// ============================================
// Cart API
// ============================================
export const cartApi = {
  /**
   * 장바구니 조회
   */
  getCart: async () => {
    const response = await apiClient.get<ApiResponse<Cart>>(
      '/api/v1/shopping/cart'
    );
    return response.data;
  },

  /**
   * 장바구니에 상품 추가
   */
  addItem: async (data: { productId: number; quantity: number }) => {
    const response = await apiClient.post<ApiResponse<Cart>>(
      '/api/v1/shopping/cart/items',
      data
    );
    return response.data;
  },

  /**
   * 장바구니 상품 수량 변경
   */
  updateItemQuantity: async (itemId: number, quantity: number) => {
    const response = await apiClient.put<ApiResponse<Cart>>(
      `/api/v1/shopping/cart/items/${itemId}`,
      { quantity }
    );
    return response.data;
  },

  /**
   * 장바구니 상품 삭제
   */
  removeItem: async (itemId: number) => {
    const response = await apiClient.delete<ApiResponse<void>>(
      `/api/v1/shopping/cart/items/${itemId}`
    );
    return response.data;
  },

  /**
   * 장바구니 비우기
   */
  clearCart: async () => {
    const response = await apiClient.delete<ApiResponse<void>>(
      '/api/v1/shopping/cart'
    );
    return response.data;
  }
};

// ============================================
// Order API
// ============================================
export const orderApi = {
  /**
   * 주문 생성
   * Saga 패턴으로 처리됨:
   * Reserve Stock → Payment → Deduct Stock → Create Delivery → Confirm
   */
  createOrder: async (data: CreateOrderRequest) => {
    const response = await apiClient.post<ApiResponse<Order>>(
      '/api/v1/shopping/orders',
      data
    );
    return response.data;
  },

  /**
   * 주문 목록 조회
   */
  getOrders: async (params?: { page?: number; size?: number }) => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Order>>>(
      '/api/v1/shopping/orders',
      { params }
    );
    return response.data;
  },

  /**
   * 주문 상세 조회
   */
  getOrder: async (orderNumber: string) => {
    const response = await apiClient.get<ApiResponse<Order>>(
      `/api/v1/shopping/orders/${orderNumber}`
    );
    return response.data;
  },

  /**
   * 주문 취소
   * 보상 트랜잭션 발생:
   * Release Stock ← Refund Payment ← Cancel Delivery
   */
  cancelOrder: async (orderNumber: string, data: { reason: string }) => {
    const response = await apiClient.post<ApiResponse<Order>>(
      `/api/v1/shopping/orders/${orderNumber}/cancel`,
      data
    );
    return response.data;
  }
};
```

---

## 5. 상태 관리 (Zustand)

### 5.1 장바구니 스토어

```typescript
// src/stores/cartStore.ts
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { cartApi } from '@/api/endpoints';
import type { Cart, CartItem } from '@/types';

interface CartState {
  // 상태
  cart: Cart | null;
  loading: boolean;
  error: string | null;

  // 액션
  fetchCart: () => Promise<void>;
  addItem: (productId: number, quantity: number) => Promise<void>;
  updateItemQuantity: (itemId: number, quantity: number) => Promise<void>;
  removeItem: (itemId: number) => Promise<void>;
  clearCart: () => Promise<void>;

  // 계산된 값
  getTotalItems: () => number;
  getTotalPrice: () => number;

  // 유틸리티
  reset: () => void;
}

export const useCartStore = create<CartState>()(
  devtools(
    persist(
      (set, get) => ({
        // 초기 상태
        cart: null,
        loading: false,
        error: null,

        // 장바구니 조회
        fetchCart: async () => {
          set({ loading: true, error: null });
          try {
            const response = await cartApi.getCart();
            if (response.success) {
              set({ cart: response.data });
            } else {
              set({ error: response.message || 'Failed to fetch cart' });
            }
          } catch (err: any) {
            set({ error: err.message });
          } finally {
            set({ loading: false });
          }
        },

        // 상품 추가
        addItem: async (productId, quantity) => {
          set({ loading: true, error: null });
          try {
            const response = await cartApi.addItem({ productId, quantity });
            if (response.success) {
              set({ cart: response.data });
            }
          } catch (err: any) {
            set({ error: err.message });
            throw err;  // 호출자에게 에러 전파
          } finally {
            set({ loading: false });
          }
        },

        // 수량 변경
        updateItemQuantity: async (itemId, quantity) => {
          // Optimistic Update: UI 먼저 업데이트
          const prevCart = get().cart;
          if (prevCart) {
            set({
              cart: {
                ...prevCart,
                items: prevCart.items.map(item =>
                  item.id === itemId
                    ? { ...item, quantity, subtotal: item.price * quantity }
                    : item
                )
              }
            });
          }

          try {
            const response = await cartApi.updateItemQuantity(itemId, quantity);
            if (response.success) {
              set({ cart: response.data });
            }
          } catch (err: any) {
            // 실패 시 롤백
            set({ cart: prevCart, error: err.message });
          }
        },

        // 상품 삭제
        removeItem: async (itemId) => {
          const prevCart = get().cart;
          if (prevCart) {
            // Optimistic Update
            set({
              cart: {
                ...prevCart,
                items: prevCart.items.filter(item => item.id !== itemId)
              }
            });
          }

          try {
            await cartApi.removeItem(itemId);
          } catch (err: any) {
            set({ cart: prevCart, error: err.message });
          }
        },

        // 장바구니 비우기
        clearCart: async () => {
          try {
            await cartApi.clearCart();
            set({ cart: null });
          } catch (err: any) {
            set({ error: err.message });
          }
        },

        // 총 상품 수
        getTotalItems: () => {
          const cart = get().cart;
          if (!cart) return 0;
          return cart.items.reduce((sum, item) => sum + item.quantity, 0);
        },

        // 총 금액
        getTotalPrice: () => {
          const cart = get().cart;
          if (!cart) return 0;
          return cart.items.reduce((sum, item) => sum + item.subtotal, 0);
        },

        // 리셋
        reset: () => set({ cart: null, loading: false, error: null })
      }),
      {
        name: 'shopping-cart-storage',  // localStorage 키
        partialize: (state) => ({ cart: state.cart })  // cart만 저장
      }
    ),
    { name: 'CartStore' }  // DevTools 이름
  )
);
```

### 5.2 인증 스토어 (Portal 연동)

```typescript
// src/stores/authStore.ts
import { create } from 'zustand';

interface User {
  id: string;
  email: string;
  name: string;
  roles: string[];
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  accessToken: string | null;

  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
  syncWithPortal: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,
  accessToken: null,

  setAuth: (user, token) => {
    localStorage.setItem('accessToken', token);
    set({ isAuthenticated: true, user, accessToken: token });
  },

  clearAuth: () => {
    localStorage.removeItem('accessToken');
    set({ isAuthenticated: false, user: null, accessToken: null });
  },

  // Portal Shell의 authStore와 동기화
  syncWithPortal: async () => {
    if (!window.__POWERED_BY_PORTAL_SHELL__) return;

    try {
      // Module Federation으로 Portal의 authStore import
      const { useAuthStore: usePortalAuth } = await import('portal/authStore');
      const portalAuth = usePortalAuth.getState();

      if (portalAuth.isAuthenticated && portalAuth.user) {
        set({
          isAuthenticated: true,
          user: portalAuth.user,
          accessToken: portalAuth.accessToken
        });
      }
    } catch (err) {
      console.warn('[Shopping] Failed to sync with Portal authStore:', err);
    }
  }
}));
```

---

## 6. 라우터 설정

### 6.1 라우트 정의

```typescript
// src/router/index.tsx
import React, { Suspense, lazy, useEffect, useRef } from 'react';
import {
  createBrowserRouter,
  createMemoryRouter,
  RouterProvider,
  Outlet,
  Navigate,
  useLocation,
  type Router
} from 'react-router-dom';

// 코드 스플리팅: 페이지 단위 Lazy Load
const ProductListPage = lazy(() => import('@/pages/ProductListPage'));
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'));
const CartPage = lazy(() => import('@/pages/CartPage'));
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'));
const OrderListPage = lazy(() => import('@/pages/OrderListPage'));
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage'));

// 로딩 컴포넌트
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
  </div>
);

// 라우트 정의
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <ProductListPage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      { path: 'cart', element: <CartPage /> },
      { path: 'checkout', element: <CheckoutPage /> },
      { path: 'orders', element: <OrderListPage /> },
      { path: 'orders/:orderNumber', element: <OrderDetailPage /> },
      { path: '*', element: <Navigate to="/" replace /> }
    ]
  }
];

// 레이아웃 (네비게이션 동기화 포함)
const Layout: React.FC = () => (
  <>
    <NavigationSync />
    <Suspense fallback={<PageLoader />}>
      <Outlet />
    </Suspense>
  </>
);
```

### 6.2 네비게이션 동기화

```typescript
// 네비게이션 콜백 (Host에게 경로 변경 알림)
let navigationCallback: ((path: string) => void) | null = null;

export const setNavigationCallback = (cb: ((path: string) => void) | null) => {
  navigationCallback = cb;
};

/**
 * 네비게이션 동기화 컴포넌트
 * - 내부 라우트 변경 시 Host(Portal Shell)에게 알림
 */
const NavigationSync: React.FC = () => {
  const location = useLocation();
  const prevPathRef = useRef(location.pathname);

  useEffect(() => {
    if (prevPathRef.current !== location.pathname) {
      console.log(`[Shopping Router] ${prevPathRef.current} → ${location.pathname}`);
      prevPathRef.current = location.pathname;

      // Host에게 경로 변경 알림
      navigationCallback?.(location.pathname);
    }
  }, [location.pathname]);

  return null;
};
```

### 6.3 라우터 생성

```typescript
// 라우터 인스턴스
let routerInstance: Router | null = null;

/**
 * 외부에서 프로그래매틱 네비게이션
 * (Host → Remote 네비게이션 처리)
 */
export const navigateTo = (path: string) => {
  if (routerInstance) {
    routerInstance.navigate(path);
  }
};

/**
 * 라우터 생성 함수
 */
export const createRouter = (options: {
  isEmbedded?: boolean;
  basePath?: string;
  initialPath?: string;
}) => {
  const { isEmbedded = false, basePath = '/shopping', initialPath = '/' } = options;

  if (isEmbedded) {
    // Embedded 모드: Memory Router
    // URL 변경 없이 내부 상태로만 라우팅
    return createMemoryRouter(routes, {
      initialEntries: [initialPath],
      initialIndex: 0
    });
  }

  // Standalone 모드: Browser Router
  return createBrowserRouter(routes, {
    basename: basePath
  });
};

/**
 * Router Provider 컴포넌트
 */
interface ShoppingRouterProps {
  isEmbedded?: boolean;
  basePath?: string;
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

export const ShoppingRouter: React.FC<ShoppingRouterProps> = ({
  isEmbedded = false,
  basePath = '/',
  initialPath = '/',
  onNavigate
}) => {
  const routerRef = useRef<Router | null>(null);

  // 라우터 생성 (최초 1회)
  if (!routerRef.current) {
    routerRef.current = createRouter({ isEmbedded, basePath, initialPath });
    routerInstance = routerRef.current;
  }

  // 네비게이션 콜백 설정
  useEffect(() => {
    setNavigationCallback(onNavigate || null);
    return () => setNavigationCallback(null);
  }, [onNavigate]);

  // Host로부터 경로 변경 수신
  useEffect(() => {
    if (routerRef.current && initialPath) {
      const currentPath = routerRef.current.state.location.pathname;
      if (currentPath !== initialPath) {
        routerRef.current.navigate(initialPath);
      }
    }
  }, [initialPath]);

  return <RouterProvider router={routerRef.current} />;
};
```

---

## 7. 페이지 컴포넌트

### 7.1 ProductListPage - 상품 목록

```typescript
// src/pages/ProductListPage.tsx
const ProductListPage: React.FC = () => {
  // ============================================
  // State
  // ============================================
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 페이지네이션
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 12;

  // 검색
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');

  // ============================================
  // Data Fetching
  // ============================================
  useEffect(() => {
    const fetchProducts = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await productApi.getProducts({
          page,
          size: pageSize,
          keyword: keyword || undefined
        });

        if (response.success) {
          setProducts(response.data.content);
          setTotalPages(response.data.totalPages);
        } else {
          setError(response.message || 'Failed to fetch products');
        }
      } catch (err: any) {
        setError(err.message || 'Failed to fetch products');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [page, keyword]);  // page나 keyword 변경 시 재조회

  // ============================================
  // Event Handlers
  // ============================================
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);  // 검색 시 첫 페이지로 리셋
    setKeyword(searchInput);
  };

  const handleClearSearch = () => {
    setSearchInput('');
    setKeyword('');
    setPage(0);
  };

  // ============================================
  // Render
  // ============================================
  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">Products</h1>
        <Link to="/cart" className="...">
          Cart ({cartItemCount})
        </Link>
      </div>

      {/* 검색 폼 */}
      <form onSubmit={handleSearch} className="flex gap-2">
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search products..."
          className="flex-1 px-4 py-2 bg-bg-input border border-border-default rounded-lg"
        />
        <button type="submit" className="btn-primary">Search</button>
        {keyword && (
          <button type="button" onClick={handleClearSearch} className="btn-secondary">
            Clear
          </button>
        )}
      </form>

      {/* 로딩/에러/결과 */}
      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={error} onRetry={() => setPage(0)} />
      ) : products.length === 0 ? (
        <EmptyState message="No products found" />
      ) : (
        <>
          {/* 상품 그리드 */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          {/* 페이지네이션 */}
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
};
```

### 7.2 ProductDetailPage - 상품 상세

```typescript
// src/pages/ProductDetailPage.tsx
const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();

  // State
  const [product, setProduct] = useState<Product | null>(null);
  const [inventory, setInventory] = useState<Inventory | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);

  // Zustand
  const addToCart = useCartStore((state) => state.addItem);

  // 데이터 로딩
  useEffect(() => {
    const fetchData = async () => {
      if (!productId) return;

      setLoading(true);
      try {
        // 상품과 재고를 병렬로 조회
        const [productRes, inventoryRes] = await Promise.all([
          productApi.getProduct(Number(productId)),
          inventoryApi.getInventory(Number(productId))
        ]);

        if (productRes.success) setProduct(productRes.data);
        if (inventoryRes.success) setInventory(inventoryRes.data);
      } catch (err) {
        console.error('Failed to fetch product:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [productId]);

  // 장바구니 추가
  const handleAddToCart = async () => {
    if (!product) return;

    setAdding(true);
    try {
      await addToCart(product.id, quantity);
      // 성공 시 장바구니로 이동 제안
      if (window.confirm('Added to cart! Go to cart?')) {
        navigate('/cart');
      }
    } catch (err) {
      alert('Failed to add to cart');
    } finally {
      setAdding(false);
    }
  };

  // 재고 상태 표시
  const getStockStatus = () => {
    if (!inventory) return { text: 'Checking...', color: 'text-text-meta' };
    const available = inventory.availableQuantity;

    if (available <= 0) return { text: 'Out of Stock', color: 'text-status-error' };
    if (available <= 5) return { text: `Only ${available} left!`, color: 'text-status-warning' };
    return { text: 'In Stock', color: 'text-status-success' };
  };

  // 렌더링
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
      {/* 이미지 */}
      <div className="aspect-square bg-bg-subtle rounded-lg" />

      {/* 정보 */}
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-text-heading">{product.name}</h1>
        <p className="text-3xl font-bold text-brand-primary">
          {formatPrice(product.price)}
        </p>

        {/* 재고 상태 */}
        <p className={getStockStatus().color}>{getStockStatus().text}</p>

        {/* 수량 선택 */}
        <div className="flex items-center gap-4">
          <span className="text-text-body">Quantity:</span>
          <div className="flex items-center border border-border-default rounded-lg">
            <button onClick={() => setQuantity(q => Math.max(1, q - 1))}>-</button>
            <span className="w-12 text-center">{quantity}</span>
            <button onClick={() => setQuantity(q => q + 1)}>+</button>
          </div>
        </div>

        {/* 장바구니 추가 */}
        <button
          onClick={handleAddToCart}
          disabled={adding || inventory?.availableQuantity === 0}
          className="w-full btn-primary py-4 text-lg"
        >
          {adding ? 'Adding...' : 'Add to Cart'}
        </button>

        {/* 설명 */}
        <div className="prose">
          <h3>Description</h3>
          <p>{product.description}</p>
        </div>
      </div>
    </div>
  );
};
```

### 7.3 CheckoutPage - 멀티 스텝 체크아웃

```typescript
// src/pages/CheckoutPage.tsx
type CheckoutStep = 'address' | 'payment' | 'complete';

const CheckoutPage: React.FC = () => {
  const navigate = useNavigate();

  // 현재 단계
  const [step, setStep] = useState<CheckoutStep>('address');

  // 장바구니
  const cart = useCartStore((state) => state.cart);
  const clearCart = useCartStore((state) => state.clearCart);

  // 주문 데이터
  const [order, setOrder] = useState<Order | null>(null);

  // 폼 데이터
  const [addressForm, setAddressForm] = useState<ShippingAddress>({
    receiverName: '',
    receiverPhone: '',
    zipCode: '',
    address1: '',
    address2: ''
  });

  const [paymentForm, setPaymentForm] = useState({
    method: 'CARD' as PaymentMethod,
    cardNumber: '',
    expiryDate: '',
    cvv: ''
  });

  // ============================================
  // Step 1: 주문 생성 (배송지 입력 후)
  // ============================================
  const handleCreateOrder = async () => {
    if (!cart || cart.items.length === 0) {
      alert('Cart is empty');
      return;
    }

    try {
      const response = await orderApi.createOrder({
        shippingAddress: addressForm,
        items: cart.items.map(item => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      });

      if (response.success) {
        setOrder(response.data);
        setStep('payment');  // 다음 단계로
      } else {
        alert(response.message || 'Failed to create order');
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create order');
    }
  };

  // ============================================
  // Step 2: 결제 처리
  // ============================================
  const handlePayment = async () => {
    if (!order) return;

    try {
      const response = await paymentApi.processPayment({
        orderNumber: order.orderNumber,
        method: paymentForm.method,
        amount: order.totalAmount,
        cardInfo: paymentForm.method === 'CARD' ? {
          cardNumber: paymentForm.cardNumber,
          expiryDate: paymentForm.expiryDate,
          cvv: paymentForm.cvv
        } : undefined
      });

      if (response.success) {
        // 장바구니 비우기
        await clearCart();
        setStep('complete');  // 완료 화면
      } else {
        alert(response.message || 'Payment failed');
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Payment failed');
    }
  };

  // ============================================
  // Render
  // ============================================
  return (
    <div className="max-w-4xl mx-auto">
      {/* 스텝 인디케이터 */}
      <StepIndicator current={step} steps={['address', 'payment', 'complete']} />

      {/* Step 1: 배송지 입력 */}
      {step === 'address' && (
        <AddressForm
          data={addressForm}
          onChange={setAddressForm}
          onNext={handleCreateOrder}
          onBack={() => navigate('/cart')}
        />
      )}

      {/* Step 2: 결제 */}
      {step === 'payment' && order && (
        <PaymentForm
          order={order}
          data={paymentForm}
          onChange={setPaymentForm}
          onSubmit={handlePayment}
          onBack={() => setStep('address')}
        />
      )}

      {/* Step 3: 완료 */}
      {step === 'complete' && order && (
        <OrderComplete
          order={order}
          onViewOrders={() => navigate('/orders')}
          onContinueShopping={() => navigate('/')}
        />
      )}
    </div>
  );
};
```

### 7.4 OrderDetailPage - 배송 추적

```typescript
// src/pages/OrderDetailPage.tsx
const OrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>();

  // State
  const [order, setOrder] = useState<Order | null>(null);
  const [delivery, setDelivery] = useState<Delivery | null>(null);
  const [loading, setLoading] = useState(true);

  // 데이터 로딩
  useEffect(() => {
    const fetchData = async () => {
      if (!orderNumber) return;

      try {
        const orderRes = await orderApi.getOrder(orderNumber);
        if (orderRes.success) {
          setOrder(orderRes.data);

          // 배송 중인 경우 배송 정보 조회
          if (['PAID', 'SHIPPING', 'DELIVERED'].includes(orderRes.data.status)) {
            try {
              const deliveryRes = await deliveryApi.getDeliveryByOrder(orderNumber);
              if (deliveryRes.success) {
                setDelivery(deliveryRes.data);
              }
            } catch (err) {
              console.warn('Delivery info not available');
            }
          }
        }
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [orderNumber]);

  // 주문 취소
  const handleCancelOrder = async () => {
    if (!order || !confirm('Cancel this order?')) return;

    try {
      const response = await orderApi.cancelOrder(order.orderNumber, {
        reason: 'Cancelled by customer'
      });
      if (response.success) {
        setOrder(response.data);
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel');
    }
  };

  // 취소 가능 여부
  const canCancel = order?.status === 'PENDING' || order?.status === 'CONFIRMED';

  return (
    <div className="space-y-6">
      {/* 주문 상태 */}
      <OrderStatusCard order={order} onCancel={canCancel ? handleCancelOrder : undefined} />

      {/* 주문 상품 */}
      <OrderItemsCard items={order.items} />

      {/* 배송 추적 타임라인 */}
      {delivery && (
        <DeliveryTrackingCard delivery={delivery} />
      )}

      {/* 배송지/결제 정보 */}
      <div className="grid grid-cols-2 gap-6">
        <ShippingAddressCard address={order.shippingAddress} />
        <OrderSummaryCard order={order} />
      </div>
    </div>
  );
};

// 배송 추적 타임라인
const DeliveryTrackingCard: React.FC<{ delivery: Delivery }> = ({ delivery }) => (
  <div className="bg-bg-card border border-border-default rounded-lg p-6">
    <h2 className="text-lg font-bold mb-4">Delivery Tracking</h2>

    {/* 기본 정보 */}
    <div className="text-sm space-y-2 mb-6">
      <p>Tracking: <span className="font-mono">{delivery.trackingNumber}</span></p>
      <p>Carrier: {delivery.carrier}</p>
      {delivery.estimatedDeliveryDate && (
        <p>Expected: {formatDate(delivery.estimatedDeliveryDate)}</p>
      )}
    </div>

    {/* 타임라인 */}
    <div className="space-y-4">
      {delivery.history.map((event, index) => (
        <div key={event.id} className="flex gap-4">
          {/* 점과 선 */}
          <div className="relative">
            <div className={`w-3 h-3 rounded-full ${
              index === 0 ? 'bg-brand-primary' : 'bg-border-default'
            }`} />
            {index < delivery.history.length - 1 && (
              <div className="absolute top-3 left-1/2 -translate-x-1/2 w-0.5 h-full bg-border-default" />
            )}
          </div>

          {/* 내용 */}
          <div>
            <p className="font-medium">{DELIVERY_STATUS_LABELS[event.status]}</p>
            {event.location && <p className="text-xs text-text-meta">{event.location}</p>}
            <p className="text-xs text-text-placeholder">{formatDate(event.createdAt)}</p>
          </div>
        </div>
      ))}
    </div>
  </div>
);
```

---

## 8. 공통 컴포넌트

### 8.1 ProductCard

```typescript
// src/components/ProductCard.tsx
interface ProductCardProps {
  product: Product;
}

const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
  const navigate = useNavigate();
  const addToCart = useCartStore((state) => state.addItem);
  const [adding, setAdding] = useState(false);

  const handleAddToCart = async (e: React.MouseEvent) => {
    e.stopPropagation();  // 카드 클릭 이벤트 방지
    setAdding(true);
    try {
      await addToCart(product.id, 1);
    } catch (err) {
      alert('Failed to add to cart');
    } finally {
      setAdding(false);
    }
  };

  return (
    <div
      onClick={() => navigate(`/products/${product.id}`)}
      className="product-card cursor-pointer group"
    >
      {/* 이미지 */}
      <div className="aspect-square bg-bg-subtle relative overflow-hidden">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} className="object-cover" />
        ) : (
          <div className="flex items-center justify-center h-full text-text-placeholder">
            No Image
          </div>
        )}

        {/* 재고 배지 */}
        {product.stockQuantity <= 0 && (
          <div className="absolute top-2 right-2 px-2 py-1 bg-status-error text-white text-xs rounded">
            Out of Stock
          </div>
        )}
        {product.stockQuantity > 0 && product.stockQuantity <= 5 && (
          <div className="absolute top-2 right-2 px-2 py-1 bg-status-warning text-white text-xs rounded">
            Only {product.stockQuantity} left
          </div>
        )}
      </div>

      {/* 정보 */}
      <div className="p-4">
        <h3 className="font-medium text-text-heading line-clamp-2 group-hover:text-brand-primary">
          {product.name}
        </h3>
        <p className="text-lg font-bold text-brand-primary mt-2">
          {formatPrice(product.price)}
        </p>

        {/* 장바구니 추가 버튼 */}
        <button
          onClick={handleAddToCart}
          disabled={adding || product.stockQuantity <= 0}
          className="w-full mt-3 btn-secondary text-sm"
        >
          {adding ? 'Adding...' : 'Add to Cart'}
        </button>
      </div>
    </div>
  );
};
```

### 8.2 CartItem

```typescript
// src/components/CartItem.tsx
interface CartItemProps {
  item: CartItemType;
  onQuantityChange: (quantity: number) => void;
  onRemove: () => void;
}

const CartItem: React.FC<CartItemProps> = ({ item, onQuantityChange, onRemove }) => {
  return (
    <div className="flex items-center gap-4 p-4 bg-bg-card border border-border-default rounded-lg">
      {/* 이미지 */}
      <div className="w-20 h-20 bg-bg-subtle rounded-lg flex-shrink-0" />

      {/* 정보 */}
      <div className="flex-1 min-w-0">
        <h3 className="font-medium text-text-heading truncate">{item.productName}</h3>
        <p className="text-sm text-text-meta">{formatPrice(item.price)}</p>
      </div>

      {/* 수량 조절 */}
      <div className="flex items-center border border-border-default rounded-lg">
        <button
          onClick={() => onQuantityChange(Math.max(1, item.quantity - 1))}
          className="px-3 py-1 hover:bg-bg-hover"
        >
          -
        </button>
        <span className="w-10 text-center">{item.quantity}</span>
        <button
          onClick={() => onQuantityChange(item.quantity + 1)}
          className="px-3 py-1 hover:bg-bg-hover"
        >
          +
        </button>
      </div>

      {/* 소계 */}
      <div className="text-right w-28">
        <p className="font-bold text-text-heading">{formatPrice(item.subtotal)}</p>
      </div>

      {/* 삭제 */}
      <button
        onClick={onRemove}
        className="p-2 text-text-meta hover:text-status-error"
        aria-label="Remove item"
      >
        <TrashIcon className="w-5 h-5" />
      </button>
    </div>
  );
};
```

---

## 9. 스타일링

### 9.1 Design System 통합

```scss
// src/styles/index.scss

/* 1. Design System CSS 변수 로드 */
@import '@portal/design-system/style.css';

/* 2. Tailwind 레이어 */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 3. 다크모드 스크롤바 */
[data-theme="dark"] {
  ::-webkit-scrollbar {
    width: 8px;
  }
  ::-webkit-scrollbar-track {
    background: var(--color-bg-subtle);
  }
  ::-webkit-scrollbar-thumb {
    background: var(--color-border-default);
    border-radius: 4px;
  }
}

/* 4. Shopping 전용 스타일 */
[data-service="shopping"] {
  /* 상품 카드 */
  .product-card {
    @apply bg-bg-card border border-border-default rounded-lg overflow-hidden;
    @apply hover:shadow-lg transition-all;
    &:hover {
      border-color: color-mix(in srgb, var(--color-brand-primary) 30%, transparent);
    }
  }

  /* 버튼 */
  .btn-primary {
    @apply bg-brand-primary text-white px-4 py-2 rounded-lg;
    @apply transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
    &:hover:not(:disabled) {
      background-color: color-mix(in srgb, var(--color-brand-primary) 90%, black);
    }
  }

  .btn-secondary {
    @apply bg-bg-subtle text-text-body px-4 py-2 rounded-lg;
    @apply hover:bg-bg-hover transition-colors;
  }

  /* 상태 배지 */
  .status-badge {
    @apply px-2 py-0.5 rounded text-xs font-medium;

    &.pending { @apply bg-status-warning-bg text-status-warning; }
    &.success { @apply bg-status-success-bg text-status-success; }
    &.error { @apply bg-status-error-bg text-status-error; }
  }
}
```

### 9.2 Tailwind 설정

```javascript
// tailwind.config.js
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],

  // 다크모드: data-theme 속성으로 전환
  darkMode: ['selector', '[data-theme="dark"]'],

  theme: {
    extend: {
      colors: {
        // 브랜드 컬러 (Design System 변수)
        'brand-primary': 'var(--color-brand-primary)',
        'brand-secondary': 'var(--color-brand-secondary)',

        // 배경
        'bg-page': 'var(--color-bg-page)',
        'bg-card': 'var(--color-bg-card)',
        'bg-subtle': 'var(--color-bg-subtle)',
        'bg-hover': 'var(--color-bg-hover)',
        'bg-input': 'var(--color-bg-input)',

        // 텍스트
        'text-heading': 'var(--color-text-heading)',
        'text-body': 'var(--color-text-body)',
        'text-meta': 'var(--color-text-meta)',
        'text-placeholder': 'var(--color-text-placeholder)',

        // 테두리
        'border-default': 'var(--color-border-default)',
        'border-strong': 'var(--color-border-strong)',

        // 상태
        'status-success': 'var(--color-status-success)',
        'status-success-bg': 'var(--color-status-success-bg)',
        'status-warning': 'var(--color-status-warning)',
        'status-warning-bg': 'var(--color-status-warning-bg)',
        'status-error': 'var(--color-status-error)',
        'status-error-bg': 'var(--color-status-error-bg)',
      },
    },
  },
  plugins: [],
}
```

---

## 10. Module Federation 통합

### 10.1 bootstrap.tsx - 진입점

```typescript
// src/bootstrap.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { navigateTo } from './router';
import './styles/index.scss';

/**
 * Mount 옵션 (Host와의 계약)
 */
export type MountOptions = {
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

/**
 * 앱 인스턴스 (Host가 제어)
 */
export type ShoppingAppInstance = {
  onParentNavigate: (path: string) => void;
  unmount: () => void;
}

let root: ReactDOM.Root | null = null;

/**
 * Shopping 앱 마운트 함수
 * Portal Shell이 이 함수를 호출하여 앱을 로드
 */
export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): ShoppingAppInstance {
  console.group('🚀 [Shopping] Mounting app in EMBEDDED mode');

  if (!el) {
    throw new Error('[Shopping] Mount element is required');
  }

  const { initialPath = '/', onNavigate } = options;
  console.log('Initial path:', initialPath);

  // data-service 속성 설정 (CSS 선택자 활성화)
  document.documentElement.setAttribute('data-service', 'shopping');

  // React 앱 마운트
  root = ReactDOM.createRoot(el);
  root.render(
    <React.StrictMode>
      <App initialPath={initialPath} onNavigate={onNavigate} />
    </React.StrictMode>
  );

  console.log('✅ App mounted successfully');
  console.groupEnd();

  return {
    // Host → Remote 네비게이션
    onParentNavigate: (path: string) => {
      console.log(`📥 [Shopping] Parent navigation: ${path}`);
      navigateTo(path);
    },

    // 정리
    unmount: () => {
      console.group('🔄 [Shopping] Unmounting');

      root?.unmount();
      root = null;
      el.innerHTML = '';

      // CSS 정리
      document.querySelectorAll('style').forEach((style) => {
        if (style.textContent?.includes('[data-service="shopping"]')) {
          style.remove();
        }
      });

      // 속성 정리
      if (document.documentElement.getAttribute('data-service') === 'shopping') {
        document.documentElement.removeAttribute('data-service');
      }

      console.log('✅ Cleanup completed');
      console.groupEnd();
    }
  };
}

export default { mountShoppingApp };
```

### 10.2 Vite 설정

```typescript
// vite.config.ts
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';
import { resolve } from 'path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [
      react(),
      federation({
        name: 'shopping-frontend',
        filename: 'remoteEntry.js',

        // 외부로 노출할 모듈
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },

        // Host와 공유하는 라이브러리
        shared: ['react', 'react-dom']
      }),
    ],

    resolve: {
      alias: {
        '@portal/design-system/style.css': resolve(__dirname, '../design-system/dist/design-system.css'),
        '@': resolve(__dirname, './src'),
      },
    },

    css: {
      postcss: './postcss.config.js'
    },

    server: {
      port: 30002,
      host: '0.0.0.0',
      cors: true,
    },

    build: {
      target: 'esnext',
      rollupOptions: {
        // Portal 모듈은 런타임에 제공됨
        external: ['portal/themeStore', 'portal/authStore', 'portal/apiClient'],
      },
    },
  };
});
```

### 10.3 App.tsx - 테마 동기화

```typescript
// src/App.tsx
interface AppProps {
  theme?: 'light' | 'dark';
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

function App({ theme = 'light', initialPath = '/', onNavigate }: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
  const [themeStore, setThemeStore] = useState<any>(null);

  // 테마 동기화
  const updateDataTheme = (isDark: boolean) => {
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  };

  useEffect(() => {
    // data-service 속성 설정
    document.documentElement.setAttribute('data-service', 'shopping');

    // 초기 테마 설정
    updateDataTheme(theme === 'dark');

    if (isEmbedded) {
      // Embedded: Portal Shell의 themeStore 연동
      import('portal/themeStore')
        .then(({ useThemeStore }) => {
          const store = useThemeStore();
          setThemeStore(store);
          updateDataTheme(store.isDark);
        })
        .catch((err) => {
          console.warn('Failed to load themeStore:', err);
        });
    } else {
      // Standalone: MutationObserver로 dark 클래스 감지
      const observer = new MutationObserver(() => {
        const isDark = document.documentElement.classList.contains('dark');
        updateDataTheme(isDark);
      });

      observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class']
      });

      return () => observer.disconnect();
    }
  }, [theme, isEmbedded]);

  return (
    <div className="min-h-screen bg-bg-page">
      {/* Standalone 헤더 */}
      {!isEmbedded && <Header />}

      {/* 메인 콘텐츠 */}
      <main className={isEmbedded ? 'py-4' : 'py-8'}>
        <div className="max-w-7xl mx-auto px-6">
          <ShoppingRouter
            isEmbedded={isEmbedded}
            initialPath={initialPath}
            onNavigate={onNavigate}
          />
        </div>
      </main>

      {/* Standalone 푸터 */}
      {!isEmbedded && <Footer />}
    </div>
  );
}
```

---

## 11. 실행 및 테스트

### 11.1 개발 서버 실행

```bash
# 전체 워크스페이스 설치
cd frontend
npm install --legacy-peer-deps

# Shopping Frontend만 실행 (Standalone)
npm run dev:shopping
# → http://localhost:30002

# 전체 앱 실행 (Portal Shell + Remotes)
npm run dev
# → http://localhost:30000 (Portal Shell)
# → http://localhost:30000/shopping (Shopping 라우트)
```

### 11.2 빌드

```bash
# Shopping Frontend 빌드
npm run build:shopping

# 빌드 결과 확인
ls -la shopping-frontend/dist/
# → index.html
# → assets/remoteEntry.js  (Module Federation 진입점)
# → assets/*.js, *.css
```

### 11.3 테스트 시나리오

```
1. 상품 목록 테스트
   - 페이지 로딩
   - 검색 기능
   - 페이지네이션
   - 상품 카드 클릭 → 상세 페이지

2. 장바구니 테스트
   - 상품 추가
   - 수량 변경
   - 상품 삭제
   - 총 금액 계산
   - LocalStorage 영속성

3. 체크아웃 테스트
   - 배송지 입력
   - 주문 생성 (Saga 트리거)
   - 결제 처리
   - 주문 완료 확인

4. Module Federation 테스트
   - Portal Shell에서 /shopping 접근
   - 테마 동기화 (Light/Dark)
   - 라우팅 동기화 (Host ↔ Remote)
   - 앱 언마운트 시 CSS 정리
```

---

## 요약

### 핵심 아키텍처

```
bootstrap.tsx (Module Federation 진입점)
    │
    └── App.tsx (테마 동기화, 모드 감지)
            │
            └── ShoppingRouter (Memory/Browser Router)
                    │
                    ├── ProductListPage ──┐
                    ├── ProductDetailPage │
                    ├── CartPage          ├── API Layer (axios)
                    ├── CheckoutPage      │
                    ├── OrderListPage     │
                    └── OrderDetailPage ──┘
                                          │
                                          └── Zustand Store (cartStore, authStore)
```

### 파일별 역할

| 파일 | 역할 |
|------|------|
| `bootstrap.tsx` | Module Federation 진입점, mount/unmount |
| `main.tsx` | Standalone 모드 진입점 |
| `App.tsx` | 루트 컴포넌트, 테마 동기화 |
| `router/index.tsx` | 라우팅 설정, 네비게이션 동기화 |
| `stores/cartStore.ts` | 장바구니 상태 (Zustand + persist) |
| `api/client.ts` | Axios 인스턴스, 인터셉터 |
| `api/endpoints.ts` | API 함수 정의 |
| `types/index.ts` | TypeScript 타입 정의 |
| `styles/index.scss` | Design System + Tailwind |

### Backend 연동 포인트

| Frontend | Backend | 패턴 |
|----------|---------|------|
| 상품 목록 | GET /products | Pagination |
| 장바구니 | Cart CRUD | Optimistic Update |
| 주문 생성 | POST /orders | Saga Orchestration |
| 주문 취소 | POST /orders/{id}/cancel | Compensation |
| 배송 추적 | GET /deliveries | History 조회 |
