# E-commerce Admin 권한 검증 전략

## 문서 정보
- 작성일: 2026-01-17
- 대상: Shopping Service Admin 기능
- 버전: 1.0

---

## 1. 개요

이 문서는 E-commerce Admin 기능에 대한 종합적인 권한 검증 전략을 정의합니다.
Frontend의 UX 보호(Route Guard, 조건부 렌더링)와 Backend의 실제 권한 검증을 다층 방어(Defense in Depth) 전략으로 구현합니다.

### 1.1 보안 원칙
- **Frontend는 UX만 담당**: 사용자 경험 개선을 위한 조건부 UI 렌더링
- **Backend가 실제 검증**: 모든 권한 검증은 Backend에서 수행 (절대 신뢰 경계)
- **심층 방어(Defense in Depth)**: 다중 계층에서 권한 검증
- **Fail-Safe 기본값**: 권한이 명시되지 않으면 기본적으로 거부

---

## 2. 현재 인증/인가 아키텍처

### 2.1 전체 흐름

```
┌─────────────┐
│  Frontend   │
│ (Vue/React) │
└──────┬──────┘
       │ 1. Login Request
       ▼
┌─────────────────────────┐
│   API Gateway (8080)    │
│  - CORS 처리             │
│  - JWT 검증 (인증만)     │
└───────────┬─────────────┘
            │ 2. JWT Token
            ▼
┌─────────────────────────┐
│  Auth Service (8081)    │
│  - OAuth2 Authorization │
│  - JWT 발급             │
└───────────┬─────────────┘
            │ 3. API Request (Bearer Token)
            ▼
┌─────────────────────────┐
│ Shopping Service (8083) │
│  - JWT 파싱             │
│  - 권한 검증 (인가)     │
│  - 비즈니스 로직 실행   │
└─────────────────────────┘
```

### 2.2 JWT 토큰 구조

```json
{
  "sub": "user@example.com",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "scope": ["read", "write", "openid", "profile"],
  "preferred_username": "admin",
  "name": "Admin User",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### 2.3 역할(Role) 정의

| 역할 | 설명 | 권한 |
|------|------|------|
| ROLE_USER | 일반 사용자 | 상품 조회, 장바구니, 주문 |
| ROLE_ADMIN | 관리자 | 상품 관리, 재고 관리, 배송 관리 |

---

## 3. Backend 권한 검증 전략

### 3.1 현재 구현 상태

#### SecurityConfig 분석

**위치**: `/services/shopping-service/src/.../config/SecurityConfig.java`

**장점**:
- ✅ 명확한 URL 패턴 기반 권한 제어
- ✅ 공개/인증/관리자 경로 3단계 구분
- ✅ OAuth2 Resource Server 구성 완료
- ✅ JWT 자동 검증 및 파싱

**현재 설정**:
```java
// [공개] 누구나 접근 가능
GET /api/shopping/products/**           → permitAll()
GET /api/shopping/categories/**         → permitAll()

// [인증된 사용자] USER 또는 ADMIN
/api/shopping/cart/**                   → hasAnyRole("USER", "ADMIN")
/api/shopping/orders/**                 → hasAnyRole("USER", "ADMIN")
/api/shopping/payments/**               → hasAnyRole("USER", "ADMIN")
GET /api/shopping/deliveries/**         → hasAnyRole("USER", "ADMIN")
GET /api/shopping/inventory/**          → hasAnyRole("USER", "ADMIN")

// [관리자] ADMIN만 접근 가능
POST   /api/shopping/products           → hasRole("ADMIN")
PUT    /api/shopping/products/**        → hasRole("ADMIN")
DELETE /api/shopping/products/**        → hasRole("ADMIN")
POST   /api/shopping/inventory/**       → hasRole("ADMIN")
PUT    /api/shopping/inventory/**       → hasRole("ADMIN")
PUT    /api/shopping/deliveries/**      → hasRole("ADMIN")
POST   /api/shopping/payments/*/refund  → hasRole("ADMIN")
```

### 3.2 추가 권한 검증 필요 항목

#### 3.2.1 Method Level Security (선택적 적용)

특정 비즈니스 로직에서 더 세밀한 제어가 필요한 경우 사용:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // 기본 설정 사용
}
```

**Controller 레벨 적용 예시**:
```java
@RestController
@RequestMapping("/api/shopping/admin")
public class AdminController {

    /**
     * 상품 관리 - ADMIN 전용
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products")
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    /**
     * 재고 대량 업데이트 - ADMIN 전용
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inventory/bulk-update")
    public ApiResponse<Void> bulkUpdateInventory(@RequestBody List<InventoryUpdateRequest> requests) {
        inventoryService.bulkUpdate(requests);
        return ApiResponse.success(null);
    }

    /**
     * 주문 상태 강제 변경 - ADMIN 전용
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders/{orderId}/status")
    public ApiResponse<OrderResponse> forceUpdateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success(orderService.forceUpdateStatus(orderId, request));
    }
}
```

**적용 시기**:
- SecurityConfig의 URL 패턴만으로 충분하면 불필요
- 동적 권한 체크가 필요한 경우 (예: 본인 주문만 조회)
- 복잡한 권한 로직이 필요한 경우 (예: `@PreAuthorize("#userId == authentication.principal.id")`)

**권장사항**:
- **현재 구조에서는 SecurityConfig의 URL 패턴만으로 충분**
- Method Security는 필요시 점진적으로 추가

#### 3.2.2 Resource Owner 검증 (본인 확인)

사용자가 자신의 리소스만 접근하도록 보장:

```java
@Service
public class OrderService {

    /**
     * 주문 조회 - 본인 주문만 허용
     */
    public OrderResponse getOrder(Long orderId, String currentUserEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserEmail().equals(currentUserEmail)) {
            throw new CustomBusinessException(ShoppingErrorCode.UNAUTHORIZED_ORDER_ACCESS);
        }

        return OrderResponse.from(order);
    }
}
```

**Controller에서 현재 사용자 정보 추출**:
```java
@GetMapping("/orders/{orderId}")
public ApiResponse<OrderResponse> getOrder(
        @PathVariable Long orderId,
        @AuthenticationPrincipal Jwt jwt) {
    String userEmail = jwt.getSubject();
    return ApiResponse.success(orderService.getOrder(orderId, userEmail));
}
```

### 3.3 에러 코드 정의

**위치**: `services/shopping-service/src/.../exception/ShoppingErrorCode.java`

```java
public enum ShoppingErrorCode implements ErrorCode {

    // 인증 관련 (4xx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "S401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "S403", "접근 권한이 없습니다"),
    UNAUTHORIZED_ORDER_ACCESS(HttpStatus.FORBIDDEN, "S403-01", "본인의 주문만 조회할 수 있습니다"),
    UNAUTHORIZED_PAYMENT_ACCESS(HttpStatus.FORBIDDEN, "S403-02", "본인의 결제만 조회할 수 있습니다"),

    // Admin 권한 관련
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "S403-10", "관리자 권한이 필요합니다"),
    INSUFFICIENT_PRIVILEGES(HttpStatus.FORBIDDEN, "S403-11", "권한이 부족합니다"),

    // 비즈니스 로직 관련 (404, 409)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "상품을 찾을 수 없습니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "주문을 찾을 수 없습니다"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "S003", "재고가 부족합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    // Constructor, Getter 생략
}
```

### 3.4 Backend 권한 검증 체크리스트

#### ✅ 필수 구현 완료
- [x] OAuth2 Resource Server 구성
- [x] JWT 자동 검증 및 파싱
- [x] URL 패턴 기반 권한 제어 (SecurityConfig)
- [x] 공개/인증/관리자 3단계 경로 분리
- [x] GlobalExceptionHandler를 통한 예외 처리

#### ⚠️ 추가 구현 권장
- [ ] ShoppingErrorCode에 권한 관련 에러코드 추가
- [ ] Resource Owner 검증 로직 (본인 주문/결제 확인)
- [ ] Admin 전용 API 엔드포인트 분리 (`/api/shopping/admin/*`)
- [ ] 권한 없음(403) 응답 로깅 강화

#### 🔄 선택적 구현
- [ ] Method Level Security (`@PreAuthorize`) - 필요시 추가
- [ ] 역할 기반 동적 권한 체크 - 복잡한 비즈니스 로직에만 적용
- [ ] Admin 활동 감사(Audit) 로깅 - 규정 준수 필요시

---

## 4. Frontend 권한 검증 전략 (UX 보호)

### 4.1 설계 원칙

> **중요**: Frontend 권한 체크는 UX 개선을 위한 것이며, 보안을 보장하지 않습니다.
> 실제 권한 검증은 반드시 Backend에서 수행되어야 합니다.

**목적**:
- 권한 없는 사용자에게 불필요한 UI 숨김
- 권한 없는 페이지 접근 시 친절한 안내 제공
- 불필요한 API 호출 감소

**구현 방식**:
1. **Route Guard**: 페이지 레벨 접근 제어
2. **Component Guard**: 컴포넌트 레벨 조건부 렌더링
3. **에러 핸들링**: 401/403 응답 처리

### 4.2 인증 상태 관리 구조

#### 4.2.1 Portal Shell (Vue + Pinia)

**위치**: `/frontend/portal-shell/src/store/auth.ts`

```typescript
export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<PortalUser | null>(null);

  // Getters
  const isAuthenticated = computed(() => user.value !== null);
  const isAdmin = computed(() => hasRole('ROLE_ADMIN'));

  // Methods
  const hasRole = (role: string): boolean => {
    return user.value?.authority.roles.includes(role) || false;
  };

  return {
    user,
    isAuthenticated,
    isAdmin,
    hasRole,
    setUser,
    logout,
  };
});
```

**JWT 파싱 로직**: `/frontend/portal-shell/src/utils/jwt.ts`

#### 4.2.2 Shopping Frontend (React + Zustand)

**위치**: `/frontend/shopping-frontend/src/stores/authStore.ts`

**동기화 모드**:
- **Embedded Mode**: Portal Shell의 authStore와 동기화
- **Standalone Mode**: 로컬 상태 관리 (개발 전용)

```typescript
interface User {
  id: string;
  email: string;
  name: string;
  role: 'guest' | 'user' | 'admin';
  avatar?: string;
}

export const useAuthStore = create<AuthState>()(
  devtools((set, get) => ({
    user: null,
    isAuthenticated: false,
    accessToken: null,

    setUser: (user: User | null) => {
      set({
        user,
        isAuthenticated: user !== null
      });
    },

    syncFromPortal: async () => {
      // Portal Shell의 authStore import
      const { useAuthStore: usePortalAuthStore } = await import('portal/authStore');
      const portalAuth = usePortalAuthStore.getState();

      set({
        user: portalAuth.user,
        isAuthenticated: portalAuth.isAuthenticated,
        accessToken: portalAuth.accessToken,
      });
    },
  }))
);
```

### 4.3 Route Guard 구현

#### 4.3.1 Shopping Frontend (React Router)

**파일 구조**:
```
shopping-frontend/src/
├── router/
│   ├── index.tsx           # 라우터 설정
│   └── guards.tsx          # Route Guard (신규)
├── pages/
│   └── admin/              # Admin 전용 페이지 (신규)
│       ├── ProductManagementPage.tsx
│       ├── InventoryManagementPage.tsx
│       └── OrderManagementPage.tsx
└── components/
    └── auth/
        ├── ProtectedRoute.tsx    # HOC (신규)
        └── UnauthorizedPage.tsx  # 권한 없음 페이지 (신규)
```

**구현 방안**: HOC (Higher-Order Component) 패턴

**이유**:
- React Router v6와 자연스럽게 통합
- 컴포넌트 재사용성 향상
- 명시적인 권한 선언

**shopping-frontend/src/components/auth/ProtectedRoute.tsx**:
```tsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'user' | 'admin';
  redirectTo?: string;
}

/**
 * 권한 기반 Route Guard HOC
 *
 * @param children - 보호할 컴포넌트
 * @param requiredRole - 필요한 역할 ('user' | 'admin')
 * @param redirectTo - 권한 없을 시 리다이렉트 경로 (기본: /unauthorized)
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRole = 'user',
  redirectTo = '/unauthorized'
}) => {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  // 1. 인증 체크
  if (!isAuthenticated) {
    console.warn('[ProtectedRoute] Not authenticated, redirecting to login');
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 2. 역할 체크
  if (requiredRole === 'admin' && user?.role !== 'admin') {
    console.warn('[ProtectedRoute] Insufficient privileges, redirecting to unauthorized');
    return <Navigate to={redirectTo} replace />;
  }

  // 3. 권한 확인 완료
  return <>{children}</>;
};
```

**shopping-frontend/src/components/auth/UnauthorizedPage.tsx**:
```tsx
import React from 'react';
import { useNavigate } from 'react-router-dom';

export const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg-base">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
        <div className="mb-6">
          <svg
            className="w-16 h-16 mx-auto text-status-error"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>

        <h1 className="text-2xl font-bold text-text-title mb-2">
          접근 권한이 없습니다
        </h1>

        <p className="text-text-body mb-6">
          이 페이지에 접근하려면 관리자 권한이 필요합니다.
        </p>

        <div className="flex gap-3 justify-center">
          <button
            onClick={() => navigate('/')}
            className="px-6 py-2 bg-brand-primary text-white rounded hover:bg-brand-secondary transition"
          >
            홈으로 이동
          </button>

          <button
            onClick={() => navigate(-1)}
            className="px-6 py-2 border border-border-default rounded hover:bg-bg-subtle transition"
          >
            이전 페이지
          </button>
        </div>
      </div>
    </div>
  );
};
```

**shopping-frontend/src/router/index.tsx** (수정):
```tsx
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { UnauthorizedPage } from '@/components/auth/UnauthorizedPage';

// Lazy load admin pages
const ProductManagementPage = lazy(() => import('@/pages/admin/ProductManagementPage'));
const InventoryManagementPage = lazy(() => import('@/pages/admin/InventoryManagementPage'));
const OrderManagementPage = lazy(() => import('@/pages/admin/OrderManagementPage'));

const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      // 공개 경로
      {
        index: true,
        element: <ProductListPage />
      },
      {
        path: 'products/:productId',
        element: <ProductDetailPage />
      },

      // 인증된 사용자 경로
      {
        path: 'cart',
        element: (
          <ProtectedRoute requiredRole="user">
            <CartPage />
          </ProtectedRoute>
        )
      },
      {
        path: 'orders',
        element: (
          <ProtectedRoute requiredRole="user">
            <OrderListPage />
          </ProtectedRoute>
        )
      },

      // 관리자 전용 경로
      {
        path: 'admin',
        children: [
          {
            path: 'products',
            element: (
              <ProtectedRoute requiredRole="admin">
                <ProductManagementPage />
              </ProtectedRoute>
            )
          },
          {
            path: 'inventory',
            element: (
              <ProtectedRoute requiredRole="admin">
                <InventoryManagementPage />
              </ProtectedRoute>
            )
          },
          {
            path: 'orders',
            element: (
              <ProtectedRoute requiredRole="admin">
                <OrderManagementPage />
              </ProtectedRoute>
            )
          }
        ]
      },

      // 권한 없음 페이지
      {
        path: 'unauthorized',
        element: <UnauthorizedPage />
      },

      // Fallback
      {
        path: '*',
        element: <Navigate to="/" replace />
      }
    ]
  }
];
```

#### 4.3.2 Portal Shell (Vue Router)

Portal Shell에서는 Shopping Frontend를 Module Federation으로 로드하므로,
Shopping Frontend 자체의 Route Guard가 동작합니다.

추가로 Portal Shell 레벨에서 메뉴 표시 여부를 제어:

**portal-shell/src/components/Navigation.vue**:
```vue
<script setup lang="ts">
import { useAuthStore } from '@/store/auth';

const authStore = useAuthStore();
</script>

<template>
  <nav>
    <router-link to="/">Home</router-link>
    <router-link to="/blog">Blog</router-link>
    <router-link to="/shopping">Shopping</router-link>

    <!-- 관리자만 표시 -->
    <router-link
      v-if="authStore.isAdmin"
      to="/shopping/admin"
      class="admin-menu"
    >
      Admin
    </router-link>
  </nav>
</template>
```

### 4.4 컴포넌트 레벨 조건부 렌더링

#### 4.4.1 권한 기반 UI 컴포넌트 (React)

**shopping-frontend/src/components/auth/RequireRole.tsx**:
```tsx
import React from 'react';
import { useAuthStore } from '@/stores/authStore';

interface RequireRoleProps {
  children: React.ReactNode;
  role: 'user' | 'admin';
  fallback?: React.ReactNode;
}

/**
 * 역할 기반 조건부 렌더링 컴포넌트
 */
export const RequireRole: React.FC<RequireRoleProps> = ({
  children,
  role,
  fallback = null
}) => {
  const { user } = useAuthStore();

  if (role === 'admin' && user?.role !== 'admin') {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
```

**사용 예시**:
```tsx
import { RequireRole } from '@/components/auth/RequireRole';

const ProductCard: React.FC<{ product: Product }> = ({ product }) => {
  return (
    <div className="product-card">
      <h3>{product.name}</h3>
      <p>{product.price}</p>

      {/* 관리자만 편집/삭제 버튼 표시 */}
      <RequireRole role="admin">
        <div className="admin-actions">
          <button onClick={() => handleEdit(product.id)}>
            Edit
          </button>
          <button onClick={() => handleDelete(product.id)}>
            Delete
          </button>
        </div>
      </RequireRole>
    </div>
  );
};
```

#### 4.4.2 Custom Hook (React)

**shopping-frontend/src/hooks/useAuth.ts**:
```typescript
import { useAuthStore } from '@/stores/authStore';

export const useAuth = () => {
  const { user, isAuthenticated } = useAuthStore();

  const hasRole = (role: 'user' | 'admin'): boolean => {
    if (!user) return false;
    if (role === 'admin') return user.role === 'admin';
    return user.role === 'user' || user.role === 'admin';
  };

  const isAdmin = (): boolean => {
    return hasRole('admin');
  };

  return {
    user,
    isAuthenticated,
    hasRole,
    isAdmin,
  };
};
```

**사용 예시**:
```tsx
const ProductListPage: React.FC = () => {
  const { isAdmin } = useAuth();

  return (
    <div>
      <h1>Products</h1>

      {isAdmin() && (
        <button onClick={() => navigate('/admin/products/new')}>
          Add New Product
        </button>
      )}

      <ProductList />
    </div>
  );
};
```

### 4.5 API 호출 에러 처리

#### 4.5.1 Axios Interceptor 전역 설정

**shopping-frontend/src/utils/apiClient.ts**:
```typescript
import axios, { AxiosError } from 'axios';
import { useAuthStore } from '@/stores/authStore';

// API 클라이언트 생성
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
});

// Request Interceptor - JWT 토큰 자동 첨부
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor - 에러 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const { response } = error;

    if (!response) {
      // 네트워크 에러
      console.error('[API Client] Network error:', error.message);
      return Promise.reject({
        code: 'NETWORK_ERROR',
        message: '네트워크 연결을 확인해주세요',
      });
    }

    const { status, data } = response;

    switch (status) {
      case 401:
        // Unauthorized - 토큰 없음 또는 만료
        console.warn('[API Client] Unauthorized - redirecting to login');
        useAuthStore.getState().logout();
        window.location.href = '/login';
        break;

      case 403:
        // Forbidden - 권한 없음
        console.warn('[API Client] Forbidden:', data?.message);
        return Promise.reject({
          code: data?.code || 'FORBIDDEN',
          message: data?.message || '접근 권한이 없습니다',
        });

      case 404:
        // Not Found
        return Promise.reject({
          code: data?.code || 'NOT_FOUND',
          message: data?.message || '요청한 리소스를 찾을 수 없습니다',
        });

      case 500:
        // Internal Server Error
        console.error('[API Client] Server error:', data);
        return Promise.reject({
          code: data?.code || 'INTERNAL_SERVER_ERROR',
          message: data?.message || '서버 오류가 발생했습니다',
        });

      default:
        return Promise.reject({
          code: data?.code || 'UNKNOWN_ERROR',
          message: data?.message || '알 수 없는 오류가 발생했습니다',
        });
    }

    return Promise.reject(error);
  }
);

// 타입 정의
interface ApiErrorResponse {
  code: string;
  message: string;
  data?: any;
}
```

#### 4.5.2 에러 토스트 표시 (React)

**shopping-frontend/src/hooks/useApiError.ts**:
```typescript
import { useCallback } from 'react';
import { toast } from 'react-toastify'; // 또는 다른 토스트 라이브러리

interface ApiError {
  code: string;
  message: string;
}

export const useApiError = () => {
  const handleError = useCallback((error: ApiError) => {
    // 에러 코드별 처리
    switch (error.code) {
      case 'S403':
      case 'S403-10':
        toast.error('관리자 권한이 필요합니다', {
          position: 'top-right',
          autoClose: 3000,
        });
        break;

      case 'S403-01':
        toast.error('본인의 주문만 조회할 수 있습니다');
        break;

      case 'NETWORK_ERROR':
        toast.error('네트워크 연결을 확인해주세요');
        break;

      default:
        toast.error(error.message || '오류가 발생했습니다');
    }
  }, []);

  return { handleError };
};
```

**사용 예시**:
```tsx
import { useApiError } from '@/hooks/useApiError';

const ProductManagementPage: React.FC = () => {
  const { handleError } = useApiError();

  const handleDeleteProduct = async (productId: number) => {
    try {
      await apiClient.delete(`/api/shopping/products/${productId}`);
      toast.success('상품이 삭제되었습니다');
    } catch (error) {
      handleError(error as ApiError);
    }
  };

  return (
    // ...
  );
};
```

### 4.6 Frontend 권한 검증 체크리스트

#### ✅ 필수 구현
- [ ] ProtectedRoute HOC 컴포넌트 구현
- [ ] UnauthorizedPage 구현
- [ ] Admin 라우트에 ProtectedRoute 적용
- [ ] Axios Interceptor 401/403 처리
- [ ] useAuth Hook 구현

#### 🎨 UX 개선
- [ ] RequireRole 컴포넌트 구현 (조건부 렌더링)
- [ ] Navigation에서 Admin 메뉴 조건부 표시
- [ ] 에러 토스트 메시지 구현
- [ ] Loading/Skeleton UI 추가

#### 🧪 테스ト
- [ ] Route Guard 단위 테스트
- [ ] 권한 없음 시나리오 E2E 테스트
- [ ] 401/403 에러 처리 테스트

---

## 5. 에러 처리 플로우

### 5.1 전체 흐름도

```
┌─────────────────────────────────────────────────────┐
│                    Frontend                          │
│  1. API 호출 (Bearer Token 포함)                    │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway (8080)                      │
│  2. JWT 검증                                         │
│     - 토큰 없음/만료 → 401 Unauthorized             │
│     - 토큰 유효 → Backend로 전달                    │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          Shopping Service (8083)                     │
│  3. 권한 검증 (SecurityConfig)                       │
│     - URL 패턴 매칭                                  │
│     - hasRole('ADMIN') 체크                         │
│     - 권한 없음 → 403 Forbidden                     │
│     - 권한 있음 → 비즈니스 로직 실행                │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          GlobalExceptionHandler                      │
│  4. 예외 변환 → ApiResponse                         │
│     {                                                │
│       "success": false,                              │
│       "code": "S403",                                │
│       "message": "접근 권한이 없습니다",             │
│       "data": null                                   │
│     }                                                │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          Axios Interceptor (Frontend)                │
│  5. 에러 처리                                        │
│     - 401 → 로그아웃 + 로그인 페이지 리다이렉트     │
│     - 403 → 에러 토스트 표시                        │
│     - 404, 500 → 에러 토스트 표시                   │
└─────────────────────────────────────────────────────┘
```

### 5.2 상태 코드별 처리 전략

| 상태 코드 | 의미 | Frontend 동작 | Backend 동작 |
|-----------|------|---------------|--------------|
| **200** | 성공 | 정상 처리 | 정상 응답 |
| **401** | 인증 실패 | 로그아웃 → 로그인 페이지 | API Gateway에서 JWT 검증 실패 |
| **403** | 권한 없음 | 에러 토스트 표시 | SecurityConfig에서 권한 체크 실패 |
| **404** | 리소스 없음 | 에러 토스트 표시 | Entity 조회 실패 |
| **409** | 충돌 | 에러 토스트 표시 | 비즈니스 로직 위반 (예: 재고 부족) |
| **500** | 서버 오류 | 에러 토스트 표시 | 예상치 못한 예외 |

### 5.3 사용자 친화적 에러 메시지

#### 5.3.1 Backend 에러 메시지

**원칙**:
- 기술적 세부사항 노출 금지
- 사용자가 이해하기 쉬운 언어 사용
- 해결 방법 제시

**예시**:
```java
// ❌ 나쁜 예
"Access Denied: User does not have ROLE_ADMIN"

// ✅ 좋은 예
"관리자 권한이 필요합니다"
```

#### 5.3.2 Frontend 에러 메시지 매핑

```typescript
const ERROR_MESSAGES: Record<string, string> = {
  // 인증 관련
  'S401': '로그인이 필요합니다',
  'S403': '접근 권한이 없습니다',
  'S403-01': '본인의 주문만 조회할 수 있습니다',
  'S403-10': '관리자 권한이 필요합니다',

  // 비즈니스 로직
  'S001': '상품을 찾을 수 없습니다',
  'S002': '주문을 찾을 수 없습니다',
  'S003': '재고가 부족합니다',

  // 기본
  'NETWORK_ERROR': '네트워크 연결을 확인해주세요',
  'UNKNOWN_ERROR': '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요',
};
```

### 5.4 로깅 전략

#### 5.4.1 Backend 로깅

**GlobalExceptionHandler에 로깅 추가**:
```java
@ExceptionHandler(AccessDeniedException.class)
protected ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
        AccessDeniedException e,
        HttpServletRequest request) {

    // 권한 위반 로깅 (보안 감사)
    log.warn("Access Denied - Path: {}, Method: {}, User: {}, Error: {}",
        request.getRequestURI(),
        request.getMethod(),
        SecurityContextHolder.getContext().getAuthentication().getName(),
        e.getMessage()
    );

    ErrorCode errorCode = ShoppingErrorCode.FORBIDDEN;
    ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
    return new ResponseEntity<>(response, errorCode.getStatus());
}
```

#### 5.4.2 Frontend 로깅

**에러 발생 시 로깅**:
```typescript
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const { response } = error;

    // 에러 로깅 (개발/프로덕션 구분)
    if (import.meta.env.DEV) {
      console.group('🚨 API Error');
      console.error('URL:', error.config?.url);
      console.error('Method:', error.config?.method);
      console.error('Status:', response?.status);
      console.error('Data:', response?.data);
      console.groupEnd();
    }

    // 프로덕션에서는 로그 수집 서비스로 전송
    // 예: Sentry, LogRocket 등

    return Promise.reject(error);
  }
);
```

---

## 6. 보안 체크리스트

### 6.1 OWASP Top 10 대응

| 취약점 | 대응 방안 | 상태 |
|--------|-----------|------|
| **A01: Broken Access Control** | SecurityConfig URL 패턴, Method Security | ✅ 구현 완료 |
| **A02: Cryptographic Failures** | HTTPS 강제, JWT 서명 검증 | ✅ 구현 완료 |
| **A03: Injection** | Prepared Statement, JPA (SQL Injection 방지) | ✅ 구현 완료 |
| **A04: Insecure Design** | 심층 방어(Defense in Depth) 전략 | ✅ 설계 완료 |
| **A05: Security Misconfiguration** | CSRF 비활성화 (Stateless), CORS 설정 | ✅ 구현 완료 |
| **A06: Vulnerable Components** | Dependabot, 정기적 의존성 업데이트 | ⚠️ 주기적 점검 필요 |
| **A07: Identification & Auth Failures** | OAuth2, JWT, 토큰 만료 처리 | ✅ 구현 완료 |
| **A08: Software & Data Integrity** | Docker 이미지 서명, CI/CD 파이프라인 보안 | 🔄 검토 필요 |
| **A09: Security Logging Failures** | GlobalExceptionHandler 로깅, Audit Log | ⚠️ 개선 필요 |
| **A10: SSRF** | API Gateway에서 외부 URL 필터링 | ✅ 구현 완료 |

### 6.2 권한 검증 보안 원칙

#### 원칙 1: 최소 권한 원칙 (Principle of Least Privilege)
- 사용자에게 필요한 최소한의 권한만 부여
- 기본값은 거부(Deny by Default)
- 명시적으로 허용된 경로만 접근 가능

#### 원칙 2: 심층 방어 (Defense in Depth)
- Frontend: UX 보호 (Route Guard, 조건부 렌더링)
- API Gateway: JWT 검증 (인증)
- Shopping Service: 권한 검증 (인가)
- Business Logic: Resource Owner 검증 (본인 확인)

#### 원칙 3: Fail-Safe
- JWT 파싱 실패 → 401 Unauthorized
- 권한 없음 → 403 Forbidden
- 예외 발생 → 500 Internal Server Error (기술 정보 노출 금지)

#### 원칙 4: 보안 감사 가능성 (Auditability)
- 모든 권한 위반 로깅
- 민감한 작업(Admin 활동) 별도 Audit Log
- 로그 레벨: 401 → INFO, 403 → WARN, 500 → ERROR

### 6.3 추가 보안 조치

#### 6.3.1 CSRF (Cross-Site Request Forgery) 방지

**현재 상태**:
- SecurityConfig에서 CSRF 비활성화됨
- 이유: Stateless JWT 기반 인증 (세션 쿠키 미사용)

**권장사항**:
- JWT 토큰을 Cookie가 아닌 Authorization Header로 전송 (현재 구현)
- SameSite 쿠키 속성 사용 (필요시)

#### 6.3.2 XSS (Cross-Site Scripting) 방지

**Frontend**:
- React의 기본 XSS 방어 활용 (dangerouslySetInnerHTML 사용 금지)
- 사용자 입력 출력 시 자동 이스케이프

**Backend**:
- ApiResponse에서 HTML 이스케이프 처리 (필요시)

#### 6.3.3 Rate Limiting

**API Gateway 레벨 적용 권장**:
```yaml
# api-gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: shopping-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 초당 10개 요청
                redis-rate-limiter.burstCapacity: 20  # 최대 20개 버스트
```

#### 6.3.4 JWT 토큰 보안

**현재 구현**:
- ✅ 서명 검증 (JWK Set)
- ✅ Issuer 검증
- ✅ 만료 시간 검증

**추가 권장사항**:
- Refresh Token Rotation (토큰 갱신 시 이전 토큰 무효화)
- Token Revocation (강제 로그아웃)

---

## 7. 테스트 전략

### 7.1 Backend 권한 검증 테스트

#### 7.1.1 SecurityConfig 테스트

**위치**: `services/shopping-service/src/test/.../config/SecurityConfigTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("공개 경로는 인증 없이 접근 가능")
    void publicEndpoints_NoAuth_Success() throws Exception {
        mockMvc.perform(get("/api/shopping/products"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 경로는 ADMIN 역할 필요")
    @WithMockUser(roles = "USER")
    void adminEndpoints_UserRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 경로는 ADMIN 역할로 접근 가능")
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_AdminRole_Success() throws Exception {
        mockMvc.perform(post("/api/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":1000}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 보호된 경로 접근 시 401")
    void protectedEndpoints_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/shopping/cart"))
            .andExpect(status().isUnauthorized());
    }
}
```

#### 7.1.2 JWT 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("유효한 JWT 토큰으로 접근 가능")
    void validJwtToken_Success() throws Exception {
        String jwt = generateTestJwt("user@example.com", List.of("ROLE_USER"));

        mockMvc.perform(get("/api/shopping/cart")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 접근 시 401")
    void expiredJwtToken_Unauthorized() throws Exception {
        String jwt = generateExpiredTestJwt("user@example.com");

        mockMvc.perform(get("/api/shopping/cart")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isUnauthorized());
    }

    private String generateTestJwt(String subject, List<String> roles) {
        // JWT 생성 로직 (테스트용)
        // ...
    }
}
```

### 7.2 Frontend 권한 검증 테스트

#### 7.2.1 ProtectedRoute 단위 테스트

**위치**: `frontend/shopping-frontend/src/components/auth/__tests__/ProtectedRoute.test.tsx`

```tsx
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../ProtectedRoute';
import { useAuthStore } from '@/stores/authStore';

jest.mock('@/stores/authStore');

describe('ProtectedRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('인증되지 않은 사용자는 로그인 페이지로 리다이렉트', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      isAuthenticated: false,
      user: null,
    });

    render(
      <BrowserRouter>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </BrowserRouter>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('일반 사용자는 user 권한 페이지 접근 가능', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      isAuthenticated: true,
      user: { role: 'user' },
    });

    render(
      <BrowserRouter>
        <ProtectedRoute requiredRole="user">
          <div>User Content</div>
        </ProtectedRoute>
      </BrowserRouter>
    );

    expect(screen.getByText('User Content')).toBeInTheDocument();
  });

  it('일반 사용자는 admin 권한 페이지 접근 불가', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      isAuthenticated: true,
      user: { role: 'user' },
    });

    render(
      <BrowserRouter>
        <ProtectedRoute requiredRole="admin">
          <div>Admin Content</div>
        </ProtectedRoute>
      </BrowserRouter>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });

  it('관리자는 모든 페이지 접근 가능', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      isAuthenticated: true,
      user: { role: 'admin' },
    });

    render(
      <BrowserRouter>
        <ProtectedRoute requiredRole="admin">
          <div>Admin Content</div>
        </ProtectedRoute>
      </BrowserRouter>
    );

    expect(screen.getByText('Admin Content')).toBeInTheDocument();
  });
});
```

#### 7.2.2 E2E 테스트 (Playwright)

**위치**: `e2e-tests/tests/shopping/admin-auth.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('Shopping Admin Authorization', () => {

  test('일반 사용자는 Admin 페이지 접근 불가', async ({ page }) => {
    // 일반 사용자로 로그인
    await page.goto('http://localhost:30000/login');
    await page.fill('input[name="email"]', 'user@example.com');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Admin 페이지 접근 시도
    await page.goto('http://localhost:30000/shopping/admin/products');

    // Unauthorized 페이지로 리다이렉트 확인
    await expect(page).toHaveURL(/.*unauthorized/);
    await expect(page.getByText('접근 권한이 없습니다')).toBeVisible();
  });

  test('관리자는 Admin 페이지 접근 가능', async ({ page }) => {
    // 관리자로 로그인
    await page.goto('http://localhost:30000/login');
    await page.fill('input[name="email"]', 'admin@example.com');
    await page.fill('input[name="password"]', 'admin');
    await page.click('button[type="submit"]');

    // Admin 페이지 접근
    await page.goto('http://localhost:30000/shopping/admin/products');

    // 페이지 접근 성공 확인
    await expect(page.getByText('Product Management')).toBeVisible();
  });

  test('일반 사용자는 상품 생성 API 호출 시 403 에러', async ({ request }) => {
    // 일반 사용자 토큰 획득
    const loginResponse = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: {
        email: 'user@example.com',
        password: 'password',
      },
    });
    const { access_token } = await loginResponse.json();

    // 상품 생성 API 호출
    const response = await request.post('http://localhost:8080/api/shopping/products', {
      headers: {
        Authorization: `Bearer ${access_token}`,
      },
      data: {
        name: 'Test Product',
        price: 1000,
      },
    });

    // 403 Forbidden 확인
    expect(response.status()).toBe(403);
    const body = await response.json();
    expect(body.code).toBe('S403');
  });
});
```

### 7.3 테스트 체크리스트

#### Backend
- [ ] SecurityConfig URL 패턴 테스트
- [ ] JWT 검증 테스트 (유효/만료/없음)
- [ ] 역할별 권한 테스트 (USER/ADMIN)
- [ ] Resource Owner 검증 테스트 (본인 확인)
- [ ] GlobalExceptionHandler 에러 응답 테스트

#### Frontend
- [ ] ProtectedRoute 단위 테스트
- [ ] useAuth Hook 테스트
- [ ] Axios Interceptor 401/403 처리 테스트
- [ ] 조건부 렌더링 테스트 (RequireRole)

#### E2E
- [ ] 일반 사용자 권한 시나리오
- [ ] 관리자 권한 시나리오
- [ ] 권한 없음 페이지 리다이렉트
- [ ] 로그아웃 후 보호된 페이지 접근

---

## 8. 구현 로드맵

### Phase 1: Backend 권한 강화 (우선순위: 높음)
- [ ] **Day 1**: ShoppingErrorCode에 권한 관련 에러코드 추가
- [ ] **Day 2**: Resource Owner 검증 로직 구현 (OrderService, PaymentService)
- [ ] **Day 3**: GlobalExceptionHandler에 AccessDeniedException 핸들러 추가
- [ ] **Day 4**: SecurityConfig 테스트 작성

### Phase 2: Frontend Route Guard 구현 (우선순위: 높음)
- [ ] **Day 5**: ProtectedRoute HOC 컴포넌트 구현
- [ ] **Day 6**: UnauthorizedPage 구현
- [ ] **Day 7**: Admin 라우트에 ProtectedRoute 적용
- [ ] **Day 8**: Axios Interceptor 401/403 처리 강화

### Phase 3: UX 개선 (우선순위: 중간)
- [ ] **Day 9**: RequireRole 컴포넌트 구현
- [ ] **Day 10**: useAuth Hook 구현
- [ ] **Day 11**: 에러 토스트 메시지 구현
- [ ] **Day 12**: Navigation Admin 메뉴 조건부 표시

### Phase 4: 테스트 작성 (우선순위: 중간)
- [ ] **Day 13**: Backend 권한 검증 단위 테스트
- [ ] **Day 14**: Frontend ProtectedRoute 단위 테스트
- [ ] **Day 15**: E2E 권한 시나리오 테스트

### Phase 5: 보안 감사 및 문서화 (우선순위: 낮음)
- [ ] **Day 16**: Admin 활동 Audit Log 구현
- [ ] **Day 17**: 로깅 전략 적용 및 점검
- [ ] **Day 18**: 보안 체크리스트 최종 검토
- [ ] **Day 19**: API 문서에 권한 정보 추가

---

## 9. 참고 자료

### 9.1 관련 파일 경로

#### Backend
- `services/shopping-service/src/.../config/SecurityConfig.java`
- `services/shopping-service/src/.../exception/ShoppingErrorCode.java`
- `services/common-library/src/.../exception/GlobalExceptionHandler.java`
- `services/api-gateway/src/.../config/SecurityConfig.java`

#### Frontend
- `frontend/shopping-frontend/src/stores/authStore.ts`
- `frontend/shopping-frontend/src/router/index.tsx`
- `frontend/portal-shell/src/store/auth.ts`
- `frontend/portal-shell/src/utils/jwt.ts`

### 9.2 관련 문서
- Spring Security 공식 문서: https://docs.spring.io/spring-security/reference/
- OAuth2 Resource Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/
- React Router Protected Routes: https://reactrouter.com/en/main/start/overview
- OWASP Top 10: https://owasp.org/www-project-top-ten/

### 9.3 프로젝트 컨텍스트
- CLAUDE.md: 프로젝트 전체 구조 및 아키텍처
- 에러 처리 아키텍처: ErrorCode → CustomBusinessException → GlobalExceptionHandler

---

## 10. 결론

이 문서에서 정의한 권한 검증 전략은 **심층 방어(Defense in Depth)** 원칙을 따릅니다:

1. **Frontend**: UX 개선을 위한 Route Guard 및 조건부 렌더링
2. **API Gateway**: JWT 토큰 검증 (인증)
3. **Shopping Service**: URL 패턴 기반 권한 검증 (인가)
4. **Business Logic**: Resource Owner 검증 (본인 확인)

**핵심 원칙**:
- Frontend는 편의를 위한 것이며, Backend가 실제 보안 경계입니다.
- 권한이 명시되지 않으면 기본적으로 거부합니다.
- 모든 권한 위반은 로깅되어 감사 가능합니다.

**다음 단계**:
1. Phase 1 (Backend 권한 강화)부터 시작
2. 각 Phase 완료 후 테스트 작성
3. E2E 테스트로 전체 플로우 검증

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-01-17
**작성자**: Security Agent
**검토자**: (검토 필요)
