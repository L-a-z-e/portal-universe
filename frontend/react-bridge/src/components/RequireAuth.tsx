/**
 * RequireAuth - 인증 가드 (race condition 해결)
 *
 * PortalBridgeProvider가 이미 bridge 초기화를 보장하므로
 * 5초 timeout hack이 필요 없음.
 */
import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { usePortalAuth } from '../hooks/usePortalAuth'
import { usePortalBridge } from '../hooks/usePortalBridge'

interface RequireAuthProps {
  children: ReactNode
  fallback?: ReactNode
  redirectTo?: string
}

export function RequireAuth({ children, fallback, redirectTo = '/' }: RequireAuthProps) {
  const { isAuthenticated, requestLogin } = usePortalAuth()
  const bridgeReady = usePortalBridge()
  const location = useLocation()
  const loginRequestedRef = useRef(false)

  useEffect(() => {
    if (!bridgeReady) return
    if (isAuthenticated) {
      loginRequestedRef.current = false
      return
    }

    // Embedded 모드에서 한 번만 로그인 모달 요청
    if (window.__POWERED_BY_PORTAL_SHELL__ && !loginRequestedRef.current) {
      loginRequestedRef.current = true
      requestLogin(location.pathname)
    }
  }, [bridgeReady, isAuthenticated, requestLogin, location.pathname])

  if (!bridgeReady) {
    return <>{fallback ?? (
      <div className="min-h-[400px] flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-text-meta text-sm">Checking authentication...</p>
        </div>
      </div>
    )}</>
  }

  if (!isAuthenticated) {
    if (window.__POWERED_BY_PORTAL_SHELL__) {
      // Embedded 모드: 리다이렉트 대신 login modal 트리거됨, fallback 표시
      return <>{fallback ?? null}</>
    }
    return <Navigate to={redirectTo} state={{ from: location }} replace />
  }

  return <>{children}</>
}
