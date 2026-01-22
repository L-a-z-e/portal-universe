# Suspense & Lazy Loading

## 학습 목표
- React Suspense와 lazy() 개념 이해
- Code Splitting을 통한 번들 크기 최적화
- Shopping Frontend의 Lazy Loading 전략 분석

---

## 1. Code Splitting 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CODE SPLITTING                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   초기 로딩 속도 개선  ─────►  필요한 코드만 먼저 로드                        │
│   번들 크기 감소      ─────►  각 페이지별로 분리                              │
│   사용자 경험 향상    ─────►  빠른 Time-to-Interactive                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 번들 크기 문제

```tsx
// ❌ 모든 코드가 하나의 번들에 포함
import ProductListPage from './pages/ProductListPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
// ... 20개 이상의 페이지

// bundle.js: 2MB (사용자가 첫 로드에 모두 다운로드)
```

```tsx
// ✅ 각 페이지를 별도 번들로 분리
const ProductListPage = lazy(() => import('./pages/ProductListPage'))
const ProductDetailPage = lazy(() => import('./pages/ProductDetailPage'))
// ...

// main.js: 200KB
// ProductListPage.chunk.js: 150KB (방문 시 로드)
// ProductDetailPage.chunk.js: 100KB (방문 시 로드)
```

---

## 2. React.lazy() 기본

### 2.1 동적 import

```tsx
import { lazy } from 'react'

// ❌ 정적 import (번들에 포함)
import ProductListPage from './pages/ProductListPage'

// ✅ 동적 import (lazy loading)
const ProductListPage = lazy(() => import('./pages/ProductListPage'))
```

### 2.2 Suspense로 감싸기

```tsx
import { Suspense, lazy } from 'react'

const ProductListPage = lazy(() => import('./pages/ProductListPage'))

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <ProductListPage />
    </Suspense>
  )
}
```

---

## 3. Portal Universe 코드 분석

### 3.1 router/index.tsx - 페이지 Lazy Loading

```tsx
import React, { Suspense, lazy } from 'react'

// ✅ 모든 페이지를 lazy loading
const ProductListPage = lazy(() => import('@/pages/ProductListPage'))
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'))
const CartPage = lazy(() => import('@/pages/CartPage'))
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'))
const OrderListPage = lazy(() => import('@/pages/OrderListPage'))
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage'))

// Coupon pages
const CouponListPage = lazy(() => import('@/pages/coupon/CouponListPage'))

// TimeDeal pages
const TimeDealListPage = lazy(() => import('@/pages/timedeal/TimeDealListPage'))
const TimeDealDetailPage = lazy(() => import('@/pages/timedeal/TimeDealDetailPage'))

// Admin pages
const AdminLayout = lazy(() => import('@/components/layout/AdminLayout'))
const AdminProductListPage = lazy(() => import('@/pages/admin/AdminProductListPage'))
const AdminProductFormPage = lazy(() => import('@/pages/admin/AdminProductFormPage'))
// ...

// Guards
const RequireAuth = lazy(() => import('@/components/guards/RequireAuth'))
const RequireRole = lazy(() => import('@/components/guards/RequireRole'))
```

### 3.2 Loading Fallback 컴포넌트

```tsx
// Loading fallback component
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex flex-col items-center gap-4">
      <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
      <p className="text-text-meta text-sm">Loading...</p>
    </div>
  </div>
)
```

### 3.3 Route 정의

```tsx
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<PageLoader />}>
            <ProductListPage />
          </Suspense>
        )
      },
      {
        path: 'products/:productId',
        element: (
          <Suspense fallback={<PageLoader />}>
            <ProductDetailPage />
          </Suspense>
        )
      },
      {
        path: 'checkout',
        element: (
          <Suspense fallback={<PageLoader />}>
            <CheckoutPage />
          </Suspense>
        )
      }
    ]
  }
]
```

### 3.4 Layout 컴포넌트

```tsx
// Layout component with navigation sync
const Layout: React.FC = () => (
  <>
    <NavigationSync /> {/* 공통 컴포넌트 */}
    <Suspense fallback={<PageLoader />}>
      <Outlet /> {/* 자식 라우트가 여기에 Lazy 로드됨 */}
    </Suspense>
  </>
)
```

---

## 4. 향상된 Loading 컴포넌트

### 4.1 Skeleton UI

```tsx
function ProductListSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="bg-bg-card rounded-lg p-4 animate-pulse">
          <div className="w-full h-48 bg-bg-subtle rounded mb-4" />
          <div className="h-6 bg-bg-subtle rounded mb-2 w-3/4" />
          <div className="h-4 bg-bg-subtle rounded mb-4 w-1/2" />
          <div className="h-10 bg-bg-subtle rounded w-full" />
        </div>
      ))}
    </div>
  )
}

// 사용
<Suspense fallback={<ProductListSkeleton />}>
  <ProductListPage />
</Suspense>
```

### 4.2 Progressive Loading

```tsx
function ProgressiveLoader() {
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 90) return prev
        return prev + 10
      })
    }, 200)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="min-h-[400px] flex items-center justify-center">
      <div className="w-64">
        <div className="mb-4 text-center text-text-meta">
          Loading... {progress}%
        </div>
        <div className="h-2 bg-bg-subtle rounded-full overflow-hidden">
          <div
            className="h-full bg-brand-primary transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>
    </div>
  )
}
```

---

## 5. 중첩 Suspense

### 5.1 계층적 Loading

```tsx
function ProductDetailPage() {
  return (
    <div>
      {/* 메인 콘텐츠 */}
      <Suspense fallback={<ProductDetailSkeleton />}>
        <ProductDetail />
      </Suspense>

      {/* 추천 상품 (독립적으로 로드) */}
      <Suspense fallback={<RecommendationSkeleton />}>
        <RecommendedProducts />
      </Suspense>

      {/* 리뷰 섹션 (독립적으로 로드) */}
      <Suspense fallback={<ReviewsSkeleton />}>
        <ProductReviews />
      </Suspense>
    </div>
  )
}
```

### 5.2 조건부 Lazy Loading

```tsx
import { Suspense, lazy } from 'react'

// Heavy 컴포넌트는 lazy loading
const HeavyChart = lazy(() => import('./HeavyChart'))

function Dashboard({ showChart }: Props) {
  return (
    <div>
      <h1>Dashboard</h1>

      {/* 필요할 때만 로드 */}
      {showChart && (
        <Suspense fallback={<div>Loading chart...</div>}>
          <HeavyChart />
        </Suspense>
      )}
    </div>
  )
}
```

---

## 6. 에러 처리

### 6.1 ErrorBoundary + Suspense

```tsx
import { ErrorBoundary } from 'react-error-boundary'
import { Suspense, lazy } from 'react'

const ProductList = lazy(() => import('./ProductList'))

function ErrorFallback({ error, resetErrorBoundary }: any) {
  return (
    <div className="p-6 text-center">
      <h2 className="text-xl font-bold mb-2">Failed to load</h2>
      <p className="text-text-meta mb-4">{error.message}</p>
      <button onClick={resetErrorBoundary} className="btn-primary">
        Retry
      </button>
    </div>
  )
}

function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <Suspense fallback={<PageLoader />}>
        <ProductList />
      </Suspense>
    </ErrorBoundary>
  )
}
```

### 6.2 Chunk Load Error 처리

```tsx
// vite.config.ts
export default defineConfig({
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui': ['@portal/design-system-react']
        }
      }
    }
  }
})
```

```tsx
// App.tsx
import { ErrorBoundary } from 'react-error-boundary'

function App() {
  return (
    <ErrorBoundary
      onError={(error) => {
        if (error.name === 'ChunkLoadError') {
          // Chunk 로드 실패 시 페이지 새로고침
          window.location.reload()
        }
      }}
      FallbackComponent={ErrorFallback}
    >
      <Router />
    </ErrorBoundary>
  )
}
```

---

## 7. Preloading 전략

### 7.1 Link에서 Preload

```tsx
import { lazy } from 'react'

const ProductDetailPage = lazy(() => import('./pages/ProductDetailPage'))

function ProductCard({ product }: Props) {
  const handleMouseEnter = () => {
    // 마우스 오버 시 미리 로드
    import('./pages/ProductDetailPage')
  }

  return (
    <Link
      to={`/products/${product.id}`}
      onMouseEnter={handleMouseEnter}
    >
      <div className="card">{product.name}</div>
    </Link>
  )
}
```

### 7.2 Route 기반 Prefetch

```tsx
function useRouterPrefetch() {
  useEffect(() => {
    // 현재 페이지의 링크들을 미리 로드
    const links = document.querySelectorAll('a[href^="/products/"]')
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const href = (entry.target as HTMLAnchorElement).getAttribute('href')
          if (href?.startsWith('/products/')) {
            import('./pages/ProductDetailPage')
          }
        }
      })
    })

    links.forEach((link) => observer.observe(link))

    return () => observer.disconnect()
  }, [])
}
```

---

## 8. 번들 분석

### 8.1 Vite Bundle Analyzer

```bash
npm install -D rollup-plugin-visualizer
```

```ts
// vite.config.ts
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig({
  plugins: [
    react(),
    visualizer({
      open: true,
      gzipSize: true,
      brotliSize: true
    })
  ]
})
```

### 8.2 분석 결과 예시

```
dist/
├── index.html (2KB)
├── assets/
│   ├── index-abc123.js (150KB) ← Main bundle
│   ├── ProductListPage-def456.js (80KB)
│   ├── ProductDetailPage-ghi789.js (60KB)
│   ├── CheckoutPage-jkl012.js (100KB)
│   └── vendor-mno345.js (300KB) ← React + libs
```

---

## 9. 최적화 체크리스트

- [ ] 모든 Route 컴포넌트 lazy loading 적용
- [ ] 큰 외부 라이브러리는 동적 import
- [ ] Suspense fallback에 Skeleton UI 제공
- [ ] ErrorBoundary로 Chunk Load Error 처리
- [ ] 자주 방문하는 페이지는 preload
- [ ] 번들 분석 도구로 크기 확인

---

## 10. 핵심 정리

| 개념 | 설명 |
|------|------|
| `lazy()` | 동적 import로 컴포넌트 분리 |
| `<Suspense>` | Loading fallback 표시 |
| **Code Splitting** | 번들을 여러 청크로 분리 |
| **Skeleton UI** | 로딩 중 레이아웃 미리 표시 |
| **Preloading** | 사용자 행동 예측하여 미리 로드 |
| **ErrorBoundary** | Chunk 로드 실패 처리 |

---

## 다음 학습

- [Error Boundary](./error-boundary.md)
- [Render Optimization](./render-optimization.md)
- [Module Federation](../mfe/module-federation-remote.md)
