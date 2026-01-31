/**
 * RequireAuth Guard
 * 인증이 필요한 페이지를 보호합니다.
 *
 * 동작 방식:
 * 1. Embedded 모드: Portal Shell의 authStore 동기화 완료 대기
 * 2. Standalone 모드: 로컬 인증 상태 확인
 * 3. 동기화 완료 후 인증 검사 수행
 * 4. Portal Shell의 auth 변경 이벤트 구독 (로그인/로그아웃 자동 반영)
 */
import React, { useEffect, useState, useRef } from 'react'
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

  // 로그인 모달 중복 호출 방지
  const loginRequestedRef = useRef(false)

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

        // sync 후 미인증이면 portal:auth-changed 이벤트를 대기 후 재시도
        const currentState = useAuthStore.getState()
        if (!currentState.isAuthenticated) {
          console.log('[RequireAuth] Not authenticated after sync, waiting for auth-changed...')
          await new Promise<void>((resolve) => {
            const timeout = setTimeout(() => {
              window.removeEventListener('portal:auth-changed', handler)
              resolve()
            }, 5000)
            const handler = async () => {
              clearTimeout(timeout)
              window.removeEventListener('portal:auth-changed', handler)
              try {
                await syncFromPortal()
              } catch { /* ignore */ }
              resolve()
            }
            window.addEventListener('portal:auth-changed', handler, { once: true })
          })
        }

        setIsInitialized(true)
      }
    }
    init()
  }, [syncFromPortal])

  // Portal Shell auth 변경 이벤트 구독 (로그인/로그아웃 시 자동 재동기화)
  useEffect(() => {
    if (!window.__POWERED_BY_PORTAL_SHELL__) return

    const handleAuthChanged = () => {
      console.log('[RequireAuth] Auth state changed, re-syncing...')
      loginRequestedRef.current = false
      syncFromPortal()
    }

    window.addEventListener('portal:auth-changed', handleAuthChanged)
    return () => window.removeEventListener('portal:auth-changed', handleAuthChanged)
  }, [syncFromPortal])

  // 인증되지 않은 경우 로그인 모달 트리거 (side effect를 useEffect로 분리)
  useEffect(() => {
    if (!isInitialized || loading) return
    if (isAuthenticated) {
      loginRequestedRef.current = false
      return
    }

    // Embedded 모드에서 한 번만 로그인 모달 요청
    if (window.__POWERED_BY_PORTAL_SHELL__ && !loginRequestedRef.current) {
      console.warn('[RequireAuth] User not authenticated, triggering login modal')
      loginRequestedRef.current = true
      window.__PORTAL_SHOW_LOGIN__?.()
    }
  }, [isInitialized, loading, isAuthenticated])

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

  // 인증되지 않은 경우: 리다이렉트
  if (!isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />
  }

  return <>{children}</>
}

export default RequireAuth
