# E-commerce Admin 상품 관리 시스템 아키텍처 설계

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2025-01-17 |
| 버전 | 1.0.0 |
| 대상 모듈 | shopping-frontend (React 18) |
| 관련 백엔드 | shopping-service (Spring Boot 3.5.5) |

---

## 1. 설계 목표

### 1.1 핵심 요구사항
- **상품 CRUD 관리**: 상품 등록/수정/삭제/목록 조회
- **권한 기반 접근 제어**: ROLE_ADMIN 권한 검증
- **기존 구조와의 통합**: 현재 고객용 페이지와 공존
- **재사용 가능한 컴포넌트**: 폼, 테이블, 모달 등

### 1.2 비기능 요구사항
- **응답성**: 폼 유효성 검사 실시간 피드백
- **에러 복구**: 네트워크 오류 시 재시도 기능
- **접근성**: 키보드 내비게이션, ARIA 지원

---

## 2. 컴포넌트 아키텍처

### 2.1 컴포넌트 계층 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            App.tsx                                       │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                      ShoppingRouter                                 │ │
│  │  ┌───────────────────────────────────────────────────────────────┐ │ │
│  │  │                    Layout Components                           │ │ │
│  │  │                                                                │ │ │
│  │  │  ┌─────────────────┐  ┌──────────────────────────────────────┐│ │ │
│  │  │  │  Customer Area  │  │          Admin Area                  ││ │ │
│  │  │  │  (기존 구조)     │  │                                      ││ │ │
│  │  │  │                 │  │  ┌──────────────────────────────────┐││ │ │
│  │  │  │  ProductList    │  │  │      AdminLayout                 │││ │ │
│  │  │  │  ProductDetail  │  │  │  ┌────────────────────────────┐ │││ │ │
│  │  │  │  CartPage       │  │  │  │     AdminSidebar           │ │││ │ │
│  │  │  │  CheckoutPage   │  │  │  └────────────────────────────┘ │││ │ │
│  │  │  │  OrderList      │  │  │  ┌────────────────────────────┐ │││ │ │
│  │  │  │  OrderDetail    │  │  │  │   AdminProductListPage     │ │││ │ │
│  │  │  │                 │  │  │  │   AdminProductFormPage     │ │││ │ │
│  │  │  │                 │  │  │  │   (Create/Edit)            │ │││ │ │
│  │  │  │                 │  │  │  └────────────────────────────┘ │││ │ │
│  │  │  │                 │  │  └──────────────────────────────────┘││ │ │
│  │  │  └─────────────────┘  └──────────────────────────────────────┘│ │ │
│  │  └───────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Admin 컴포넌트 상세 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Admin Components Architecture                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Pages (페이지 레벨)                                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │AdminProductList │  │AdminProductForm │  │AdminDashboard   │          │
│  │     Page        │  │     Page        │  │     Page        │          │
│  └────────┬────────┘  └────────┬────────┘  └─────────────────┘          │
│           │                    │                                         │
│  ─────────┴────────────────────┴─────────────────────────────────        │
│                                                                          │
│  Containers (비즈니스 로직)                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │ProductList      │  │ProductForm      │  │DeleteConfirm    │          │
│  │   Container     │  │   Container     │  │   Container     │          │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘          │
│           │                    │                    │                    │
│  ─────────┴────────────────────┴────────────────────┴────────────        │
│                                                                          │
│  UI Components (프레젠테이션)                                            │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │DataTable   │ │FormField   │ │ConfirmModal│ │StatusBadge │            │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘            │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │Pagination  │ │SearchInput │ │Toast       │ │LoadingState│            │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 권한 체크 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Authorization Flow                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  User Request                                                            │
│       │                                                                  │
│       ▼                                                                  │
│  ┌─────────────────────────────────────┐                                │
│  │        RequireAuth Component        │  (Route Guard)                 │
│  │  - isAuthenticated 체크             │                                 │
│  │  - 미인증 시 /login 리다이렉트       │                                 │
│  └──────────────────┬──────────────────┘                                │
│                     │                                                    │
│                     ▼                                                    │
│  ┌─────────────────────────────────────┐                                │
│  │       RequireRole Component         │  (Permission Guard)            │
│  │  - role: ['ROLE_ADMIN'] 체크        │                                 │
│  │  - 권한 없음 시 /403 리다이렉트      │                                 │
│  └──────────────────┬──────────────────┘                                │
│                     │                                                    │
│                     ▼                                                    │
│  ┌─────────────────────────────────────┐                                │
│  │         Admin Pages                 │                                 │
│  │  - AdminProductListPage             │                                 │
│  │  - AdminProductFormPage             │                                 │
│  └─────────────────────────────────────┘                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 라우팅 설계

### 3.1 라우팅 구조도

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Route Structure                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  /                              (Root)                                   │
│  │                                                                       │
│  ├── /                          ProductListPage (고객)                   │
│  ├── /products                  ProductListPage (고객)                   │
│  ├── /products/:productId       ProductDetailPage (고객)                 │
│  ├── /cart                      CartPage (고객)                          │
│  ├── /checkout                  CheckoutPage (고객)                      │
│  ├── /orders                    OrderListPage (고객)                     │
│  ├── /orders/:orderNumber       OrderDetailPage (고객)                   │
│  │                                                                       │
│  ├── /403                       ForbiddenPage (공통)                     │
│  │                                                                       │
│  └── /admin/*                   [RequireAuth + RequireRole]             │
│       │                                                                  │
│       ├── /admin                AdminDashboard (관리자)                  │
│       ├── /admin/products       AdminProductListPage (관리자)            │
│       ├── /admin/products/new   AdminProductFormPage (생성)              │
│       └── /admin/products/:id   AdminProductFormPage (수정)              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 라우트 정의 코드

```typescript
// src/router/routes.tsx

const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      // ========== Customer Routes ==========
      { index: true, element: <ProductListPage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      { path: 'cart', element: <CartPage /> },
      { path: 'checkout', element: <CheckoutPage /> },
      { path: 'orders', element: <OrderListPage /> },
      { path: 'orders/:orderNumber', element: <OrderDetailPage /> },

      // ========== Error Pages ==========
      { path: '403', element: <ForbiddenPage /> },

      // ========== Admin Routes (Protected) ==========
      {
        path: 'admin',
        element: (
          <RequireAuth>
            <RequireRole roles={['ROLE_ADMIN']}>
              <AdminLayout />
            </RequireRole>
          </RequireAuth>
        ),
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: 'products', element: <AdminProductListPage /> },
          { path: 'products/new', element: <AdminProductFormPage mode="create" /> },
          { path: 'products/:id', element: <AdminProductFormPage mode="edit" /> },
        ],
      },

      // ========== Fallback ==========
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
];
```

### 3.3 Route Guard 컴포넌트

```typescript
// src/components/guards/RequireAuth.tsx

interface RequireAuthProps {
  children: React.ReactNode;
  redirectTo?: string;
}

export const RequireAuth: React.FC<RequireAuthProps> = ({
  children,
  redirectTo = '/login'
}) => {
  const { isAuthenticated, loading } = useAuthStore();
  const location = useLocation();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    // 원래 가려던 경로 저장
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

// src/components/guards/RequireRole.tsx

interface RequireRoleProps {
  children: React.ReactNode;
  roles: string[];
  redirectTo?: string;
}

export const RequireRole: React.FC<RequireRoleProps> = ({
  children,
  roles,
  redirectTo = '/403'
}) => {
  const { user } = useAuthStore();

  const hasRequiredRole = user?.roles?.some(role => roles.includes(role));

  if (!hasRequiredRole) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
};
```

---

## 4. 상태 관리 전략

### 4.1 상태 분류 및 관리 도구

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      State Management Strategy                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Server State (API 데이터)                      │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │ 관리 도구: React Query (TanStack Query) v5                  │  │   │
│  │  │                                                             │  │   │
│  │  │ - 상품 목록/상세 조회                                        │  │   │
│  │  │ - 캐싱, 자동 리프레시, 낙관적 업데이트                        │  │   │
│  │  │ - 로딩/에러 상태 자동 관리                                   │  │   │
│  │  │ - staleTime: 5분, cacheTime: 30분 (목록)                    │  │   │
│  │  │ - staleTime: 1분 (상세)                                     │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Client State (UI 상태)                         │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │ 관리 도구: Zustand                                          │  │   │
│  │  │                                                             │  │   │
│  │  │ - 인증 상태 (authStore) - 기존 유지                          │  │   │
│  │  │ - 테마/UI 설정                                               │  │   │
│  │  │ - 모달 열림/닫힘 상태                                        │  │   │
│  │  │ - 선택된 항목 상태                                           │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Form State (폼 상태)                           │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │ 관리 도구: React Hook Form + Zod                            │  │   │
│  │  │                                                             │  │   │
│  │  │ - 폼 입력값 관리 (Controlled)                                │  │   │
│  │  │ - 실시간 유효성 검사                                         │  │   │
│  │  │ - 에러 메시지 표시                                           │  │   │
│  │  │ - 제출 상태 관리                                             │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 상태 관리 선택 근거

| 상태 유형 | 도구 선택 | 근거 |
|----------|----------|------|
| **Server State** | React Query | 캐싱, 자동 리프레시, 낙관적 업데이트, 로딩/에러 상태 내장 |
| **Auth State** | Zustand | 기존 구조 유지, Portal Shell 연동 필요 |
| **Form State** | React Hook Form | 성능 최적화 (비제어), 풍부한 유효성 검사 API |
| **UI State** | Zustand/Local | 단순 UI 상태는 로컬, 공유 필요 시 Zustand |

### 4.3 React Query 사용 예시

```typescript
// src/hooks/useAdminProducts.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// Query Keys
export const productKeys = {
  all: ['products'] as const,
  lists: () => [...productKeys.all, 'list'] as const,
  list: (filters: ProductFilters) => [...productKeys.lists(), filters] as const,
  details: () => [...productKeys.all, 'detail'] as const,
  detail: (id: number) => [...productKeys.details(), id] as const,
};

// 상품 목록 조회 훅
export const useAdminProducts = (filters: ProductFilters) => {
  return useQuery({
    queryKey: productKeys.list(filters),
    queryFn: () => productApi.getProducts(filters.page, filters.size),
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 30 * 60 * 1000,   // 30분 (기존 cacheTime)
  });
};

// 상품 상세 조회 훅
export const useAdminProduct = (id: number) => {
  return useQuery({
    queryKey: productKeys.detail(id),
    queryFn: () => productApi.getProduct(id),
    enabled: !!id,
    staleTime: 1 * 60 * 1000, // 1분
  });
};

// 상품 생성 mutation
export const useCreateProduct = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ProductCreateRequest) => productApi.createProduct(data),
    onSuccess: () => {
      // 목록 캐시 무효화
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
};

// 상품 수정 mutation (낙관적 업데이트)
export const useUpdateProduct = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProductUpdateRequest }) =>
      productApi.updateProduct(id, data),
    onMutate: async ({ id, data }) => {
      // 진행 중인 쿼리 취소
      await queryClient.cancelQueries({ queryKey: productKeys.detail(id) });

      // 이전 값 스냅샷
      const previousProduct = queryClient.getQueryData(productKeys.detail(id));

      // 낙관적 업데이트
      queryClient.setQueryData(productKeys.detail(id), (old: any) => ({
        ...old,
        data: { ...old?.data, ...data },
      }));

      return { previousProduct };
    },
    onError: (err, variables, context) => {
      // 롤백
      if (context?.previousProduct) {
        queryClient.setQueryData(
          productKeys.detail(variables.id),
          context.previousProduct
        );
      }
    },
    onSettled: (data, error, variables) => {
      // 캐시 무효화
      queryClient.invalidateQueries({ queryKey: productKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
};

// 상품 삭제 mutation
export const useDeleteProduct = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => productApi.deleteProduct(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
};
```

### 4.4 폼 상태 관리 (React Hook Form + Zod)

```typescript
// src/schemas/product.ts

import { z } from 'zod';

export const productFormSchema = z.object({
  name: z
    .string()
    .min(1, '상품명을 입력해주세요')
    .max(100, '상품명은 100자 이하여야 합니다'),
  description: z
    .string()
    .max(2000, '설명은 2000자 이하여야 합니다')
    .optional(),
  price: z
    .number({ invalid_type_error: '가격을 입력해주세요' })
    .min(0, '가격은 0 이상이어야 합니다')
    .max(999999999, '가격이 너무 큽니다'),
  imageUrl: z
    .string()
    .url('올바른 URL 형식이 아닙니다')
    .optional()
    .or(z.literal('')),
  category: z
    .string()
    .optional(),
});

export type ProductFormData = z.infer<typeof productFormSchema>;

// src/components/admin/ProductForm.tsx

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

export const ProductForm: React.FC<ProductFormProps> = ({
  initialData,
  onSubmit,
  isSubmitting
}) => {
  const form = useForm<ProductFormData>({
    resolver: zodResolver(productFormSchema),
    defaultValues: initialData || {
      name: '',
      description: '',
      price: 0,
      imageUrl: '',
      category: '',
    },
  });

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <FormField
        control={form.control}
        name="name"
        render={({ field, fieldState }) => (
          <Input
            {...field}
            label="상품명"
            error={fieldState.error?.message}
          />
        )}
      />
      {/* ... other fields */}
    </form>
  );
};
```

---

## 5. API 통신 계층

### 5.1 API 계층 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       API Communication Layer                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    React Components / Hooks                       │   │
│  │                                                                   │   │
│  │         useAdminProducts()      useCreateProduct()                │   │
│  └────────────────────────────┬─────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    React Query Layer                              │   │
│  │                                                                   │   │
│  │        - Query 캐싱                                               │   │
│  │        - Mutation 관리                                            │   │
│  │        - 로딩/에러 상태                                           │   │
│  └────────────────────────────┬─────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    API Endpoints (기존 유지)                       │   │
│  │                                                                   │   │
│  │  src/api/endpoints.ts                                             │   │
│  │  - productApi.getProducts()                                       │   │
│  │  - productApi.createProduct()                                     │   │
│  │  - productApi.updateProduct()                                     │   │
│  │  - productApi.deleteProduct()                                     │   │
│  └────────────────────────────┬─────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Axios Client (기존 유지)                        │   │
│  │                                                                   │   │
│  │  src/api/client.ts                                                │   │
│  │  - Base URL 설정                                                  │   │
│  │  - Request Interceptor (토큰 자동 첨부)                           │   │
│  │  - Response Interceptor (에러 핸들링)                             │   │
│  └────────────────────────────┬─────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    API Gateway (8080)                             │   │
│  │                                                                   │   │
│  │  - JWT 검증                                                       │   │
│  │  - 라우팅 (/api/v1/shopping/** → shopping-service)               │   │
│  └────────────────────────────┬─────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Shopping Service (8083)                        │   │
│  │                                                                   │   │
│  │  - @PreAuthorize("hasRole('ADMIN')") 검증                        │   │
│  │  - 비즈니스 로직 처리                                             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 에러 핸들링 전략

```typescript
// src/api/errorHandler.ts

export interface ApiError {
  code: string;
  message: string;
  status: number;
  timestamp: string;
  details?: Record<string, string[]>;
}

// 에러 코드별 사용자 메시지
export const ERROR_MESSAGES: Record<string, string> = {
  // Common
  C001: '잘못된 요청입니다',
  C002: '인증이 필요합니다',
  C003: '권한이 없습니다',
  C004: '리소스를 찾을 수 없습니다',

  // Shopping
  S001: '상품을 찾을 수 없습니다',
  S002: '재고가 부족합니다',
  S003: '이미 존재하는 상품입니다',

  // Network
  NETWORK_ERROR: '네트워크 오류가 발생했습니다',
  TIMEOUT: '요청 시간이 초과되었습니다',
  UNKNOWN: '알 수 없는 오류가 발생했습니다',
};

// src/hooks/useErrorHandler.ts

export const useErrorHandler = () => {
  const { toast } = useToast();

  const handleError = useCallback((error: unknown) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data as ApiError;

      // 인증 에러는 별도 처리 (authStore에서 처리)
      if (status === 401) return;

      // 권한 에러
      if (status === 403) {
        toast({
          type: 'error',
          message: '이 작업을 수행할 권한이 없습니다',
        });
        return;
      }

      // 비즈니스 에러
      if (data?.code) {
        toast({
          type: 'error',
          message: ERROR_MESSAGES[data.code] || data.message,
        });
        return;
      }

      // 네트워크 에러
      if (!error.response) {
        toast({
          type: 'error',
          message: ERROR_MESSAGES.NETWORK_ERROR,
        });
        return;
      }
    }

    // 기타 에러
    toast({
      type: 'error',
      message: ERROR_MESSAGES.UNKNOWN,
    });
  }, [toast]);

  return { handleError };
};
```

### 5.3 Admin API 엔드포인트 확장

```typescript
// src/api/endpoints.ts (기존 파일에 추가)

// Admin Product API
export const adminProductApi = {
  /**
   * 상품 목록 조회 (Admin용 - 추가 필드 포함)
   */
  getProducts: async (params: {
    page?: number;
    size?: number;
    keyword?: string;
    category?: string;
    status?: 'ACTIVE' | 'INACTIVE';
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  }) => {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) {
        searchParams.append(key, String(value));
      }
    });

    const response = await getApiClient().get<ApiResponse<PagedResponse<Product>>>(
      `${API_PREFIX}/admin/products?${searchParams}`
    );
    return response.data;
  },

  /**
   * 상품 생성
   */
  createProduct: async (data: ProductCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products`,
      data
    );
    return response.data;
  },

  /**
   * 상품 수정
   */
  updateProduct: async (id: number, data: ProductUpdateRequest) => {
    const response = await getApiClient().put<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products/${id}`,
      data
    );
    return response.data;
  },

  /**
   * 상품 삭제
   */
  deleteProduct: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/products/${id}`
    );
    return response.data;
  },

  /**
   * 상품 상태 변경 (활성/비활성)
   */
  updateProductStatus: async (id: number, status: 'ACTIVE' | 'INACTIVE') => {
    const response = await getApiClient().patch<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products/${id}/status`,
      { status }
    );
    return response.data;
  },

  /**
   * 상품 일괄 삭제
   */
  deleteProducts: async (ids: number[]) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/products/batch`,
      { data: { ids } }
    );
    return response.data;
  },
};
```

---

## 6. 폴더 구조 제안

```
frontend/shopping-frontend/
├── src/
│   ├── api/
│   │   ├── client.ts              # Axios 인스턴스 (기존)
│   │   ├── endpoints.ts           # API 함수들 (기존 + admin 추가)
│   │   └── errorHandler.ts        # 에러 처리 유틸리티 [신규]
│   │
│   ├── components/
│   │   ├── common/                # 공용 컴포넌트 [신규]
│   │   │   ├── DataTable.tsx      # 데이터 테이블
│   │   │   ├── Pagination.tsx     # 페이지네이션
│   │   │   ├── SearchInput.tsx    # 검색 입력
│   │   │   ├── ConfirmModal.tsx   # 확인 모달
│   │   │   ├── Toast.tsx          # 토스트 알림
│   │   │   ├── LoadingSpinner.tsx # 로딩 스피너
│   │   │   └── StatusBadge.tsx    # 상태 뱃지
│   │   │
│   │   ├── form/                  # 폼 컴포넌트 [신규]
│   │   │   ├── FormField.tsx      # 폼 필드 래퍼
│   │   │   ├── Input.tsx          # 텍스트 입력
│   │   │   ├── TextArea.tsx       # 텍스트 영역
│   │   │   ├── Select.tsx         # 선택 박스
│   │   │   ├── NumberInput.tsx    # 숫자 입력
│   │   │   └── ImageUpload.tsx    # 이미지 업로드
│   │   │
│   │   ├── guards/                # Route Guards [신규]
│   │   │   ├── RequireAuth.tsx    # 인증 필수 가드
│   │   │   └── RequireRole.tsx    # 권한 필수 가드
│   │   │
│   │   ├── layout/                # 레이아웃 [신규]
│   │   │   ├── AdminLayout.tsx    # Admin 레이아웃
│   │   │   └── AdminSidebar.tsx   # Admin 사이드바
│   │   │
│   │   ├── admin/                 # Admin 전용 컴포넌트 [신규]
│   │   │   ├── ProductTable.tsx   # 상품 테이블
│   │   │   ├── ProductForm.tsx    # 상품 폼
│   │   │   └── ProductStats.tsx   # 상품 통계
│   │   │
│   │   ├── ProductCard.tsx        # (기존)
│   │   └── CartItem.tsx           # (기존)
│   │
│   ├── hooks/                     # 커스텀 훅 [신규]
│   │   ├── useAdminProducts.ts    # Admin 상품 CRUD 훅
│   │   ├── useErrorHandler.ts     # 에러 핸들링 훅
│   │   ├── useToast.ts            # 토스트 훅
│   │   └── useConfirm.ts          # 확인 모달 훅
│   │
│   ├── pages/
│   │   ├── admin/                 # Admin 페이지 [신규]
│   │   │   ├── AdminDashboardPage.tsx
│   │   │   ├── AdminProductListPage.tsx
│   │   │   └── AdminProductFormPage.tsx
│   │   │
│   │   ├── error/                 # 에러 페이지 [신규]
│   │   │   ├── ForbiddenPage.tsx
│   │   │   └── NotFoundPage.tsx
│   │   │
│   │   ├── ProductListPage.tsx    # (기존)
│   │   ├── ProductDetailPage.tsx  # (기존)
│   │   ├── CartPage.tsx           # (기존)
│   │   ├── CheckoutPage.tsx       # (기존)
│   │   ├── OrderListPage.tsx      # (기존)
│   │   └── OrderDetailPage.tsx    # (기존)
│   │
│   ├── router/
│   │   ├── index.tsx              # 라우터 설정 (수정 필요)
│   │   └── routes.tsx             # 라우트 정의 [신규]
│   │
│   ├── schemas/                   # Zod 스키마 [신규]
│   │   └── product.ts             # 상품 폼 스키마
│   │
│   ├── stores/
│   │   ├── authStore.ts           # (기존)
│   │   ├── cartStore.ts           # (기존)
│   │   └── uiStore.ts             # UI 상태 (모달 등) [신규]
│   │
│   ├── types/
│   │   ├── index.ts               # (기존)
│   │   └── admin.ts               # Admin 전용 타입 [신규]
│   │
│   ├── utils/                     # 유틸리티 [신규]
│   │   ├── format.ts              # 포맷팅 함수
│   │   └── validation.ts          # 유효성 검사 헬퍼
│   │
│   ├── App.tsx                    # (기존)
│   ├── bootstrap.tsx              # (기존)
│   ├── index.tsx                  # (기존)
│   └── main.tsx                   # (기존)
│
├── package.json                   # 의존성 추가 필요
└── vite.config.ts                 # (기존)
```

---

## 7. 핵심 인터페이스/타입 정의

### 7.1 Admin 전용 타입

```typescript
// src/types/admin.ts

// ============================================
// Product Admin Types
// ============================================

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';

export interface AdminProduct extends Product {
  status: ProductStatus;
  stockQuantity: number;
  reservedQuantity: number;
  salesCount: number;
  viewCount: number;
  lastModifiedBy?: string;
  lastModifiedAt?: string;
}

export interface ProductFilters {
  page: number;
  size: number;
  keyword?: string;
  category?: string;
  status?: ProductStatus;
  sortBy?: 'name' | 'price' | 'createdAt' | 'salesCount';
  sortOrder?: 'asc' | 'desc';
}

export interface ProductCreateRequest {
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  category?: string;
  initialStock?: number;
}

export interface ProductUpdateRequest {
  name?: string;
  description?: string;
  price?: number;
  imageUrl?: string;
  category?: string;
}

// ============================================
// UI State Types
// ============================================

export interface ModalState {
  isOpen: boolean;
  type: 'create' | 'edit' | 'delete' | 'confirm' | null;
  data?: any;
}

export interface TableState {
  selectedIds: number[];
  sortColumn: string | null;
  sortOrder: 'asc' | 'desc';
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

// ============================================
// Form Types
// ============================================

export interface FormFieldProps<T = string> {
  name: string;
  label: string;
  value: T;
  onChange: (value: T) => void;
  error?: string;
  disabled?: boolean;
  required?: boolean;
  placeholder?: string;
  helpText?: string;
}

export interface SelectOption<T = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

// ============================================
// Table Types
// ============================================

export interface TableColumn<T> {
  key: keyof T | string;
  header: string;
  width?: string | number;
  sortable?: boolean;
  render?: (value: any, row: T) => React.ReactNode;
}

export interface TableAction<T> {
  label: string;
  icon?: React.ReactNode;
  onClick: (row: T) => void;
  variant?: 'primary' | 'danger' | 'default';
  visible?: (row: T) => boolean;
}

// ============================================
// API Response Types
// ============================================

export interface AdminProductListResponse {
  content: AdminProduct[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ProductStatsResponse {
  totalProducts: number;
  activeProducts: number;
  outOfStockProducts: number;
  totalSales: number;
  averagePrice: number;
}
```

### 7.2 Route Guard 타입

```typescript
// src/types/auth.ts

export interface User {
  id: string;
  email: string;
  name: string;
  roles: string[];
  avatar?: string;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  accessToken: string | null;
  loading: boolean;
  error: string | null;
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN';

export interface ProtectedRouteProps {
  children: React.ReactNode;
  roles?: UserRole[];
  redirectTo?: string;
  fallback?: React.ReactNode;
}
```

### 7.3 컴포넌트 Props 인터페이스

```typescript
// src/types/components.ts

// DataTable
export interface DataTableProps<T> {
  data: T[];
  columns: TableColumn<T>[];
  actions?: TableAction<T>[];
  loading?: boolean;
  emptyMessage?: string;
  selectable?: boolean;
  selectedIds?: number[];
  onSelectionChange?: (ids: number[]) => void;
  onSort?: (column: string, order: 'asc' | 'desc') => void;
  sortColumn?: string;
  sortOrder?: 'asc' | 'desc';
}

// Pagination
export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  showPageNumbers?: number;
  disabled?: boolean;
}

// ConfirmModal
export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string | React.ReactNode;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'default';
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  loading?: boolean;
}

// ProductForm
export interface ProductFormProps {
  mode: 'create' | 'edit';
  initialData?: Partial<ProductFormData>;
  onSubmit: (data: ProductFormData) => void | Promise<void>;
  onCancel: () => void;
  isSubmitting?: boolean;
}

// AdminLayout
export interface AdminLayoutProps {
  children?: React.ReactNode;
}

// AdminSidebar
export interface SidebarItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  badge?: string | number;
  children?: SidebarItem[];
}

export interface AdminSidebarProps {
  items: SidebarItem[];
  currentPath: string;
  onNavigate: (path: string) => void;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}
```

---

## 8. 의존성 추가 요구사항

### 8.1 package.json 추가 의존성

```json
{
  "dependencies": {
    // 기존 유지
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^7.1.5",
    "axios": "^1.12.2",
    "zustand": "^5.0.3",

    // 신규 추가
    "@tanstack/react-query": "^5.60.0",
    "react-hook-form": "^7.53.0",
    "@hookform/resolvers": "^3.9.0",
    "zod": "^3.23.0"
  },
  "devDependencies": {
    // 기존 유지
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "typescript": "~5.9.3",
    "vite": "^7.1.12",

    // 신규 추가 (선택)
    "@tanstack/react-query-devtools": "^5.60.0"
  }
}
```

---

## 9. 구현 우선순위

### Phase 1: 기반 구축 (1-2일)
1. Route Guards 구현 (RequireAuth, RequireRole)
2. Admin 라우트 추가
3. AdminLayout, AdminSidebar 컴포넌트
4. React Query 설정

### Phase 2: 상품 목록 (2-3일)
1. DataTable 컴포넌트
2. Pagination 컴포넌트
3. AdminProductListPage
4. useAdminProducts 훅

### Phase 3: 상품 폼 (2-3일)
1. FormField 컴포넌트들
2. 상품 생성/수정 폼
3. AdminProductFormPage
4. Zod 스키마 및 유효성 검사

### Phase 4: 부가 기능 (1-2일)
1. 삭제 확인 모달
2. Toast 알림
3. 에러 핸들링 개선
4. 일괄 작업 (선택적)

---

## 10. 결론 및 다음 단계

### 10.1 아키텍처 설계 요약

| 영역 | 선택 | 근거 |
|------|------|------|
| **라우팅** | React Router v6 + Route Guards | 기존 구조 활용, 중첩 라우트 지원 |
| **상태 관리** | React Query + Zustand + React Hook Form | 관심사 분리, 각 영역에 최적화된 도구 |
| **폼 관리** | React Hook Form + Zod | 타입 안전성, 선언적 유효성 검사 |
| **컴포넌트** | Container/Presentational 패턴 | 재사용성, 테스트 용이성 |
| **API** | 기존 Axios 기반 + React Query 래핑 | 기존 코드 활용, 캐싱/상태 관리 개선 |

### 10.2 다음 단계

1. **백엔드 API 확장**: Admin 전용 엔드포인트 추가 (필요 시)
2. **디자인 시스템 확장**: Admin UI 컴포넌트 스타일 정의
3. **E2E 테스트**: Playwright로 Admin 플로우 테스트 작성
4. **권한 관리 고도화**: 세분화된 권한 체계 (필요 시)

---

## 부록 A: 데이터 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Admin Product Create Flow                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  [User]                                                                  │
│    │                                                                     │
│    ▼                                                                     │
│  ┌─────────────────┐                                                    │
│  │ ProductFormPage │                                                    │
│  │   (Container)   │                                                    │
│  └────────┬────────┘                                                    │
│           │ form.handleSubmit(onSubmit)                                 │
│           ▼                                                             │
│  ┌─────────────────┐                                                    │
│  │  ProductForm    │                                                    │
│  │  (Presentation) │                                                    │
│  │                 │                                                    │
│  │  - React Hook   │                                                    │
│  │    Form         │                                                    │
│  │  - Zod          │                                                    │
│  │    validation   │                                                    │
│  └────────┬────────┘                                                    │
│           │ validated data                                              │
│           ▼                                                             │
│  ┌─────────────────┐                                                    │
│  │useCreateProduct │                                                    │
│  │   (Mutation)    │                                                    │
│  └────────┬────────┘                                                    │
│           │ productApi.createProduct(data)                              │
│           ▼                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │  Axios Client   │───▶│   API Gateway   │───▶│Shopping Service │     │
│  │  + JWT Token    │    │  (JWT 검증)      │    │ (ROLE_ADMIN)   │     │
│  └─────────────────┘    └─────────────────┘    └────────┬────────┘     │
│                                                          │              │
│           ┌──────────────────────────────────────────────┘              │
│           │ ApiResponse<Product>                                        │
│           ▼                                                             │
│  ┌─────────────────┐                                                    │
│  │  React Query    │                                                    │
│  │  onSuccess:     │                                                    │
│  │  - invalidate   │                                                    │
│  │    queries      │                                                    │
│  │  - show toast   │                                                    │
│  │  - navigate     │                                                    │
│  └─────────────────┘                                                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 부록 B: 백엔드 API 보안 설정 권장사항

현재 `ProductController`에는 명시적인 `@PreAuthorize` 어노테이션이 없습니다. Admin 전용 API에 대한 보안 강화를 권장합니다.

```java
// ProductController.java 수정 권장

@RestController
@RequestMapping("/api/v1/shopping")
@RequiredArgsConstructor
public class ProductController {

    // 고객용 - 인증 불필요
    @GetMapping("/products")
    public ApiResponse<PagedResponse<ProductResponse>> getProducts(...) { }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> getProduct(...) { }

    // Admin 전용 - 인증 + 권한 필요
    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> createProduct(...) { }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> updateProduct(...) { }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProduct(...) { }
}
```
