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
  useLocation,
  useNavigate,
  type Router
} from 'react-router-dom'

// Lazy load pages for code splitting
const ProductListPage = lazy(() => import('@/pages/ProductListPage'))
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'))
const CartPage = lazy(() => import('@/pages/CartPage'))
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'))
const OrderListPage = lazy(() => import('@/pages/OrderListPage'))
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage'))

// Loading fallback component
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex flex-col items-center gap-4">
      <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
      <p className="text-text-meta text-sm">Loading...</p>
    </div>
  </div>
)

// Error boundary component
const ErrorFallback: React.FC<{ error?: Error }> = ({ error }) => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="text-center">
      <h2 className="text-xl font-bold text-status-error mb-2">
        Something went wrong
      </h2>
      <p className="text-text-meta text-sm">
        {error?.message || 'Failed to load page'}
      </p>
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

// Route definitions
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <ProductListPage />
      },
      {
        path: 'products',
        element: <ProductListPage />
      },
      {
        path: 'products/:productId',
        element: <ProductDetailPage />
      },
      {
        path: 'cart',
        element: <CartPage />
      },
      {
        path: 'checkout',
        element: <CheckoutPage />
      },
      {
        path: 'orders',
        element: <OrderListPage />
      },
      {
        path: 'orders/:orderNumber',
        element: <OrderDetailPage />
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
let routerInstance: Router | null = null

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
  const routerRef = useRef<Router | null>(null)

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
