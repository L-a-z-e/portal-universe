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

// React Router v7 Router 타입
type RouterInstance = ReturnType<typeof createBrowserRouter>

// Lazy load pages for code splitting
const ProductListPage = lazy(() => import('@/pages/ProductListPage'))
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'))
const CartPage = lazy(() => import('@/pages/CartPage'))
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'))
const OrderListPage = lazy(() => import('@/pages/OrderListPage'))
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage'))

// Admin pages
const AdminLayout = lazy(() => import('@/components/layout/AdminLayout'))
const AdminProductListPage = lazy(() => import('@/pages/admin/AdminProductListPage'))
const AdminProductFormPage = lazy(() => import('@/pages/admin/AdminProductFormPage'))
const ForbiddenPage = lazy(() => import('@/pages/error/ForbiddenPage'))

// Guards
const RequireAuth = lazy(() => import('@/components/guards/RequireAuth'))
const RequireRole = lazy(() => import('@/components/guards/RequireRole'))

// Loading fallback component
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex flex-col items-center gap-4">
      <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
      <p className="text-text-meta text-sm">Loading...</p>
    </div>
  </div>
)

// Navigation listener context
let navigationCallback: ((path: string) => void) | null = null

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
    if (prevPathRef.current !== location.pathname) {
      console.log(`📍 [Shopping Router] Path changed: ${prevPathRef.current} → ${location.pathname}`)
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

// Admin Wrapper - Guards를 렌더링 시점에 처리
const AdminWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    <RequireAuth>
      <RequireRole roles={['admin']}>
        {children}
      </RequireRole>
    </RequireAuth>
  </Suspense>
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
          <Suspense fallback={<PageLoader />}>
            <CartPage />
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
      },
      {
        path: 'orders',
        element: (
          <Suspense fallback={<PageLoader />}>
            <OrderListPage />
          </Suspense>
        )
      },
      {
        path: 'orders/:orderNumber',
        element: (
          <Suspense fallback={<PageLoader />}>
            <OrderDetailPage />
          </Suspense>
        )
      },
      {
        path: '403',
        element: (
          <Suspense fallback={<PageLoader />}>
            <ForbiddenPage />
          </Suspense>
        )
      },
      {
        // Admin routes
        path: 'admin',
        element: (
          <AdminWrapper>
            <Suspense fallback={<PageLoader />}>
              <AdminLayout />
            </Suspense>
          </AdminWrapper>
        ),
        children: [
          {
            path: 'products',
            element: (
              <Suspense fallback={<PageLoader />}>
                <AdminProductListPage />
              </Suspense>
            )
          },
          {
            path: 'products/new',
            element: (
              <Suspense fallback={<PageLoader />}>
                <AdminProductFormPage />
              </Suspense>
            )
          },
          {
            path: 'products/:id',
            element: (
              <Suspense fallback={<PageLoader />}>
                <AdminProductFormPage />
              </Suspense>
            )
          }
        ]
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
    console.log(`📥 [Shopping Router] Navigating to: ${path}`)
    routerInstance.navigate(path)
  } else {
    console.error('❌ [Shopping Router] Router not initialized')
  }
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
        console.log(`📥 [Shopping Router] Parent navigation: ${currentPath} → ${initialPath}`)
        routerRef.current.navigate(initialPath)
      }
    }
  }, [initialPath])

  return <RouterProvider router={routerRef.current} />
}

export default ShoppingRouter
