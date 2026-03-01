/**
 * React Router Configuration
 *
 * Shopping Frontend 라우팅 설정
 * Blog와 유사한 패턴으로 Parent 내비게이션 연동 지원
 */
import React, { Suspense, lazy, useEffect, useRef } from 'react'
import {
  createBrowserRouter,
  createMemoryRouter,
  RouterProvider,
  Outlet,
  Navigate,
  useLocation
} from 'react-router-dom'
import { Spinner } from '@portal/design-react'

// React Router v7 Router 타입
type RouterInstance = ReturnType<typeof createBrowserRouter>

// Lazy load pages for code splitting
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
const TimeDealPurchasesPage = lazy(() => import('@/pages/timedeal/TimeDealPurchasesPage'))

// Queue pages
const QueueWaitingPage = lazy(() => import('@/pages/queue/QueueWaitingPage'))

// Guards
import { RequireAuth } from '@portal/react-bridge'

// Loading fallback component
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex flex-col items-center gap-4">
      <Spinner size="md" />
      <p className="text-text-meta text-sm">Loading...</p>
    </div>
  </div>
)

// Navigation listener context
let navigationCallback: ((path: string) => void) | null = null

// App active state for keep-alive support
let isAppActive = true

/**
 * Set app active state (for keep-alive activated/deactivated)
 * When deactivated, NavigationSync will skip callback invocation
 */
export const setAppActive = (active: boolean) => {
  isAppActive = active
}

/**
 * Set navigation callback for parent communication
 */
export const setNavigationCallback = (callback: ((path: string) => void) | null) => {
  navigationCallback = callback
}

/**
 * Navigation Sync Component
 * 라우트 변경 시 Parent에게 알림
 */
const NavigationSync: React.FC = () => {
  const location = useLocation()
  const prevPathRef = useRef(location.pathname)

  useEffect(() => {
    // Skip callback if app is not active (deactivated by keep-alive)
    if (!isAppActive) {
      prevPathRef.current = location.pathname // Sync state only
      return
    }

    if (prevPathRef.current !== location.pathname) {
      prevPathRef.current = location.pathname
      navigationCallback?.(location.pathname)
    }
  }, [location.pathname])

  return null
}

// Layout component with navigation sync
const Layout: React.FC = () => (
  <>
    <NavigationSync />
    <Suspense fallback={<PageLoader />}>
      <Outlet />
    </Suspense>
  </>
)

// Route definitions - element에 JSX 사용
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
        path: 'products',
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
        path: 'cart',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <CartPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'checkout',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <CheckoutPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'orders',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <OrderListPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'orders/:orderNumber',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <OrderDetailPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'coupons',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <CouponListPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'time-deals',
        element: (
          <Suspense fallback={<PageLoader />}>
            <TimeDealListPage />
          </Suspense>
        )
      },
      {
        path: 'time-deals/:id',
        element: (
          <Suspense fallback={<PageLoader />}>
            <TimeDealDetailPage />
          </Suspense>
        )
      },
      {
        path: 'time-deals/purchases',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <TimeDealPurchasesPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'queue/:eventType/:eventId',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <QueueWaitingPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        // Fallback for unknown routes
        path: '*',
        element: <Navigate to="/" replace />
      }
    ]
  }
]

/**
 * Create router based on environment
 *
 * - Embedded mode: MemoryRouter (Portal Shell이 라우팅 관리)
 * - Standalone mode: BrowserRouter
 */
export const createRouter = (options: {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
}) => {
  const { isEmbedded = false, basePath = '/shopping', initialPath = '/' } = options

  if (isEmbedded) {
    // Memory router for embedded mode
    // Portal Shell이 URL을 관리하고, Shopping은 내부 상태만 관리
    return createMemoryRouter(routes, {
      initialEntries: [initialPath],
      initialIndex: 0
    })
  }

  // Browser router for standalone mode
  return createBrowserRouter(routes, {
    basename: basePath
  })
}

// Router instance cache for navigation control
let routerInstance: RouterInstance | null = null

/**
 * Get current router instance
 */
export const getRouter = () => routerInstance

/**
 * Navigate programmatically (for parent navigation sync)
 */
export const navigateTo = (path: string) => {
  if (routerInstance) {
    routerInstance.navigate(path)
  } else {
    console.error('❌ [Shopping Router] Router not initialized')
  }
}

/**
 * 🆕 Reset router instance (for cleanup on unmount)
 * 다음 마운트 시 새로운 라우터가 생성되도록 인스턴스 초기화
 */
export const resetRouter = () => {
  routerInstance = null
  setNavigationCallback(null)
}

// Router Provider component
interface ShoppingRouterProps {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
  onNavigate?: (path: string) => void
}

export const ShoppingRouter: React.FC<ShoppingRouterProps> = ({
  isEmbedded = false,
  basePath = '/',
  initialPath = '/',
  onNavigate
}) => {
  // Create router (only once)
  const routerRef = useRef<RouterInstance | null>(null)

  if (!routerRef.current) {
    routerRef.current = createRouter({ isEmbedded, basePath, initialPath })
    routerInstance = routerRef.current
  }

  // Set navigation callback
  useEffect(() => {
    setNavigationCallback(onNavigate || null)
    return () => setNavigationCallback(null)
  }, [onNavigate])

  // Handle parent navigation (initialPath changes)
  useEffect(() => {
    if (routerRef.current && initialPath) {
      const currentPath = routerRef.current.state.location.pathname
      if (currentPath !== initialPath) {
        routerRef.current.navigate(initialPath)
      }
    }
  }, [initialPath])

  return <RouterProvider router={routerRef.current} />
}

export default ShoppingRouter
