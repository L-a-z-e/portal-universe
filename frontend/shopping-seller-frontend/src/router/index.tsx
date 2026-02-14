import React, { Suspense, lazy, useEffect, useRef } from 'react'
import {
  createBrowserRouter,
  createMemoryRouter,
  RouterProvider,
  Outlet,
  Navigate,
  useLocation
} from 'react-router-dom'
import { Spinner } from '@portal/design-system-react'

type RouterInstance = ReturnType<typeof createBrowserRouter>

// Lazy load pages
const SellerLayout = lazy(() => import('@/components/layout/SellerLayout'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const ProductListPage = lazy(() => import('@/pages/ProductListPage'))
const ProductFormPage = lazy(() => import('@/pages/ProductFormPage'))
const CouponListPage = lazy(() => import('@/pages/CouponListPage'))
const CouponFormPage = lazy(() => import('@/pages/CouponFormPage'))
const TimeDealListPage = lazy(() => import('@/pages/TimeDealListPage'))
const TimeDealFormPage = lazy(() => import('@/pages/TimeDealFormPage'))
const OrderListPage = lazy(() => import('@/pages/OrderListPage'))
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage'))
const DeliveryPage = lazy(() => import('@/pages/DeliveryPage'))
const StockMovementPage = lazy(() => import('@/pages/StockMovementPage'))
const QueuePage = lazy(() => import('@/pages/QueuePage'))
const SettlementPage = lazy(() => import('@/pages/SettlementPage'))
const ProfilePage = lazy(() => import('@/pages/ProfilePage'))
const ForbiddenPage = lazy(() => import('@/pages/error/ForbiddenPage'))

import { RequireAuth } from '@portal/react-bridge'
const RequireRole = lazy(() => import('@/components/guards/RequireRole'))

const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex flex-col items-center gap-4">
      <Spinner size="md" />
      <p className="text-text-meta text-sm">Loading...</p>
    </div>
  </div>
)

let navigationCallback: ((path: string) => void) | null = null
let isAppActive = true

export const setAppActive = (active: boolean) => {
  isAppActive = active
}

export const setNavigationCallback = (callback: ((path: string) => void) | null) => {
  navigationCallback = callback
}

const NavigationSync: React.FC = () => {
  const location = useLocation()
  const prevPathRef = useRef(location.pathname)

  useEffect(() => {
    if (!isAppActive) {
      prevPathRef.current = location.pathname
      return
    }
    if (prevPathRef.current !== location.pathname) {
      prevPathRef.current = location.pathname
      navigationCallback?.(location.pathname)
    }
  }, [location.pathname])

  return null
}

const Layout: React.FC = () => (
  <>
    <NavigationSync />
    <Suspense fallback={<PageLoader />}>
      <Outlet />
    </Suspense>
  </>
)

const SellerWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    <RequireAuth>
      <RequireRole roles={['ROLE_SELLER', 'ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN']}>
        {children}
      </RequireRole>
    </RequireAuth>
  </Suspense>
)

const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: (
          <SellerWrapper>
            <Suspense fallback={<PageLoader />}>
              <SellerLayout />
            </Suspense>
          </SellerWrapper>
        ),
        children: undefined as any,
      },
      {
        path: '/',
        element: (
          <SellerWrapper>
            <Suspense fallback={<PageLoader />}>
              <SellerLayout />
            </Suspense>
          </SellerWrapper>
        ),
        children: [
          { index: true, element: <Suspense fallback={<PageLoader />}><DashboardPage /></Suspense> },
          { path: 'products', element: <Suspense fallback={<PageLoader />}><ProductListPage /></Suspense> },
          { path: 'products/new', element: <Suspense fallback={<PageLoader />}><ProductFormPage /></Suspense> },
          { path: 'products/:id', element: <Suspense fallback={<PageLoader />}><ProductFormPage /></Suspense> },
          { path: 'coupons', element: <Suspense fallback={<PageLoader />}><CouponListPage /></Suspense> },
          { path: 'coupons/new', element: <Suspense fallback={<PageLoader />}><CouponFormPage /></Suspense> },
          { path: 'time-deals', element: <Suspense fallback={<PageLoader />}><TimeDealListPage /></Suspense> },
          { path: 'time-deals/new', element: <Suspense fallback={<PageLoader />}><TimeDealFormPage /></Suspense> },
          { path: 'orders', element: <Suspense fallback={<PageLoader />}><OrderListPage /></Suspense> },
          { path: 'orders/:orderNumber', element: <Suspense fallback={<PageLoader />}><OrderDetailPage /></Suspense> },
          { path: 'deliveries', element: <Suspense fallback={<PageLoader />}><DeliveryPage /></Suspense> },
          { path: 'stock-movements', element: <Suspense fallback={<PageLoader />}><StockMovementPage /></Suspense> },
          { path: 'queue', element: <Suspense fallback={<PageLoader />}><QueuePage /></Suspense> },
          { path: 'settlement', element: <Suspense fallback={<PageLoader />}><SettlementPage /></Suspense> },
          { path: 'profile', element: <Suspense fallback={<PageLoader />}><ProfilePage /></Suspense> },
        ]
      },
      { path: '403', element: <Suspense fallback={<PageLoader />}><ForbiddenPage /></Suspense> },
      { path: '*', element: <Navigate to="/" replace /> }
    ]
  }
]

export const createRouter = (options: {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
}) => {
  const { isEmbedded = false, basePath = '/seller', initialPath = '/' } = options

  if (isEmbedded) {
    return createMemoryRouter(routes, {
      initialEntries: [initialPath],
      initialIndex: 0
    })
  }

  return createBrowserRouter(routes, {
    basename: basePath
  })
}

let routerInstance: RouterInstance | null = null

export const getRouter = () => routerInstance

export const navigateTo = (path: string) => {
  if (routerInstance) {
    routerInstance.navigate(path)
  }
}

export const resetRouter = () => {
  routerInstance = null
  setNavigationCallback(null)
}

interface SellerRouterProps {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
  onNavigate?: (path: string) => void
}

export const SellerRouter: React.FC<SellerRouterProps> = ({
  isEmbedded = false,
  basePath = '/',
  initialPath = '/',
  onNavigate
}) => {
  const routerRef = useRef<RouterInstance | null>(null)

  if (!routerRef.current) {
    routerRef.current = createRouter({ isEmbedded, basePath, initialPath })
    routerInstance = routerRef.current
  }

  useEffect(() => {
    setNavigationCallback(onNavigate || null)
    return () => setNavigationCallback(null)
  }, [onNavigate])

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

export default SellerRouter
