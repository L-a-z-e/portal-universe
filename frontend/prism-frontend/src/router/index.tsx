/**
 * React Router Configuration for Prism Frontend
 *
 * AI Agent Orchestration 라우팅 설정
 * Shopping과 유사한 패턴으로 Parent 내비게이션 연동 지원
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
const BoardListPage = lazy(() => import('@/pages/BoardListPage'))
const BoardPage = lazy(() => import('@/pages/BoardPage'))
const AgentsPage = lazy(() => import('@/pages/AgentsPage'))
const ProvidersPage = lazy(() => import('@/pages/ProvidersPage'))
const AppLayout = lazy(() => import('@/components/Layout'))

// Guards
import { RequireAuth } from '@/components/guards/RequireAuth'

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

// App active state for keep-alive support
let isAppActive = true

/**
 * Set app active state (for keep-alive activated/deactivated)
 */
export const setAppActive = (active: boolean) => {
  console.log(`🔄 [Prism Router] setAppActive: ${active}`)
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
      console.log(`⏸️ [Prism Router] Skipping navigation sync (inactive): ${location.pathname}`)
      prevPathRef.current = location.pathname
      return
    }

    if (prevPathRef.current !== location.pathname) {
      console.log(`📍 [Prism Router] Path changed: ${prevPathRef.current} → ${location.pathname}`)
      prevPathRef.current = location.pathname
      navigationCallback?.(location.pathname)
    }
  }, [location.pathname])

  return null
}

// Layout wrapper with navigation sync
const LayoutWithSync: React.FC = () => (
  <>
    <NavigationSync />
    <Suspense fallback={<PageLoader />}>
      <AppLayout />
    </Suspense>
  </>
)

// Route definitions
const routes = [
  {
    path: '/',
    element: <LayoutWithSync />,
    children: [
      {
        index: true,
        element: <Navigate to="/boards" replace />
      },
      {
        path: 'boards',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <BoardListPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'boards/:boardId',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <BoardPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'agents',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <AgentsPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        path: 'providers',
        element: (
          <RequireAuth>
            <Suspense fallback={<PageLoader />}>
              <ProvidersPage />
            </Suspense>
          </RequireAuth>
        )
      },
      {
        // Fallback for unknown routes
        path: '*',
        element: <Navigate to="/boards" replace />
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
  const { isEmbedded = false, basePath = '/prism', initialPath = '/' } = options

  if (isEmbedded) {
    // Memory router for embedded mode
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
    console.log(`📥 [Prism Router] Navigating to: ${path}`)
    routerInstance.navigate(path)
  } else {
    console.error('❌ [Prism Router] Router not initialized')
  }
}

/**
 * Reset router instance (for cleanup on unmount)
 */
export const resetRouter = () => {
  console.log('🔄 [Prism Router] Resetting router instance')
  routerInstance = null
  setNavigationCallback(null)
}

// Router Provider component
interface PrismRouterProps {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
  onNavigate?: (path: string) => void
}

export const PrismRouter: React.FC<PrismRouterProps> = ({
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
        console.log(`📥 [Prism Router] Parent navigation: ${currentPath} → ${initialPath}`)
        routerRef.current.navigate(initialPath)
      }
    }
  }, [initialPath])

  return <RouterProvider router={routerRef.current} />
}

export default PrismRouter
