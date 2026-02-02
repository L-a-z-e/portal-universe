/**
 * usePortalAuth - Auth 전용 Hook
 *
 * Embedded 모드: bridge의 authAdapter 소비 (useSyncExternalStore)
 * Standalone 모드: guest 상태 반환
 */
import { useMemo } from 'react'
import { createStoreHook } from '../create-store-hook'
import { getAdapter, isBridgeReady } from '../bridge-registry'
import type { AuthState, AuthActions } from '../types'

const defaultAuthState: AuthState = {
  isAuthenticated: false,
  displayName: 'Guest',
  isAdmin: false,
  isSeller: false,
  roles: [],
  memberships: {},
  user: null,
}

const useAuthState = createStoreHook<AuthState>(() => getAdapter('auth'))

export function usePortalAuth(): AuthState & {
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
  isServiceAdmin: (service: string) => boolean
  logout: () => void
  getAccessToken: () => string | null
  requestLogin: (path?: string) => void
} {
  const isReady = isBridgeReady()

  // Standalone 또는 bridge 미초기화 시 default 반환
  const state = isReady ? useAuthState() : defaultAuthState

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
      requestLogin: adapter.requestLogin ?? ((path?: string) => {
        window.__PORTAL_SHOW_LOGIN__?.()
      }),
    }
  }, [isReady])

  return { ...state, ...actions }
}
