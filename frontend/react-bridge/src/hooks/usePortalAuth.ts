/**
 * usePortalAuth - Auth 전용 Hook
 *
 * Embedded 모드: bridge의 authAdapter 소비 (useSyncExternalStore)
 * Standalone 모드: guest 상태 반환
 */
import { useMemo, useSyncExternalStore } from 'react'
import { getAdapter, isBridgeReady } from '../bridge-registry'
import type { AuthState } from '../types'

const defaultAuthState: AuthState = {
  isAuthenticated: false,
  displayName: 'Guest',
  isAdmin: false,
  isSeller: false,
  roles: [],
  memberships: {},
  user: null,
}

/** No-op adapter for standalone/uninitialized mode */
const noopSubscribe = () => () => {}
const getDefaultState = () => defaultAuthState

export function usePortalAuth(): AuthState & {
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
  isServiceAdmin: (service: string) => boolean
  logout: () => void
  getAccessToken: () => string | null
  requestLogin: (path?: string) => void
} {
  const isReady = isBridgeReady()

  // getSnapshot을 useMemo로 안정화하여 동일 값일 때 같은 참조 반환
  // adapter.getState()가 매번 새 객체를 반환하더라도 안전
  const getSnapshot = useMemo(() => {
    if (!isReady) return getDefaultState
    const adapter = getAdapter('auth')
    let cached: AuthState | undefined
    return () => {
      const next = adapter.getState()
      if (
        cached &&
        cached.isAuthenticated === next.isAuthenticated &&
        cached.displayName === next.displayName &&
        cached.isAdmin === next.isAdmin &&
        cached.isSeller === next.isSeller &&
        cached.user?.uuid === next.user?.uuid
      ) {
        return cached
      }
      cached = next
      return next
    }
  }, [isReady])

  const subscribe = useMemo(
    () => (isReady ? getAdapter('auth').subscribe : noopSubscribe),
    [isReady],
  )

  const state = useSyncExternalStore(subscribe, getSnapshot, getDefaultState)

  const actions = useMemo(() => {
    if (!isReady) {
      return {
        hasRole: () => false,
        hasAnyRole: () => false,
        isServiceAdmin: () => false,
        logout: () => {},
        getAccessToken: () => null,
        requestLogin: () => {},
      }
    }

    const adapter = getAdapter('auth')
    return {
      hasRole: adapter.hasRole,
      hasAnyRole: adapter.hasAnyRole,
      isServiceAdmin: adapter.isServiceAdmin,
      logout: adapter.logout,
      getAccessToken: adapter.getAccessToken ?? (() => window.__PORTAL_ACCESS_TOKEN__ ?? null),
      requestLogin: adapter.requestLogin ?? ((_path?: string) => {
        window.__PORTAL_SHOW_LOGIN__?.()
      }),
    }
  }, [isReady])

  return { ...state, ...actions }
}
