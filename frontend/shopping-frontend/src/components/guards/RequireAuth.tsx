/**
 * RequireAuth Guard
 * 인증이 필요한 페이지를 보호합니다.
 *
 * 동작 방식:
 * 1. Embedded 모드: Portal Shell의 authStore 동기화 완료 대기
 * 2. Standalone 모드: 로컬 인증 상태 확인
 * 3. 동기화 완료 후 인증 검사 수행
 */
import React, { useEffect, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

interface RequireAuthProps {
  children: React.ReactNode
  redirectTo?: string
}

export const RequireAuth: React.FC<RequireAuthProps> = ({
  children,
  redirectTo = '/'
}) => {
  const { isAuthenticated, loading, syncFromPortal } = useAuthStore()
  const location = useLocation()

  // 동기화 완료 여부 (Embedded 모드에서 중요)
  const [isInitialized, setIsInitialized] = useState(
    // Standalone 모드는 동기화 필요 없음
    !window.__POWERED_BY_PORTAL_SHELL__
  )

  // Embedded 모드에서 Portal Shell 인증 상태 동기화
  useEffect(() => {
    const init = async () => {
      if (window.__POWERED_BY_PORTAL_SHELL__) {
        console.log('[RequireAuth] Syncing auth from Portal Shell...')
        try {
          await syncFromPortal()
          console.log('[RequireAuth] Auth sync completed')
        } catch (err) {
          console.warn('[RequireAuth] Auth sync failed:', err)
        }
        setIsInitialized(true)
      }
    }
    init()
  }, [syncFromPortal])

  // 동기화 대기 중 또는 로딩 중
  if (!isInitialized || loading) {
    return (
      <div className="min-h-[400px] flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-text-meta text-sm">Checking authentication...</p>
        </div>
      </div>
    )
  }

  // 인증되지 않은 경우: Embedded 모드에서는 로그인 모달 트리거, Standalone은 리다이렉트
  if (!isAuthenticated) {
    if (window.__POWERED_BY_PORTAL_SHELL__) {
      console.warn('[RequireAuth] User not authenticated, triggering login modal')
      window.__PORTAL_SHOW_LOGIN__?.()
    }
    return <Navigate to={redirectTo} state={{ from: location }} replace />
  }

  return <>{children}</>
}

export default RequireAuth
