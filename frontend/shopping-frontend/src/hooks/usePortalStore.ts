/**
 * usePortalStore - React Hooks for Portal Shell Store Adapters
 *
 * Portal Shell의 Pinia 스토어를 React에서 사용하기 위한 커스텀 훅
 * Module Federation을 통해 storeAdapter를 동적 import하여 사용
 */

import { useState, useEffect, useCallback } from 'react'

// ============================================
// Type Definitions (storeAdapter와 동일)
// ============================================

interface ThemeState {
  isDark: boolean
}

interface AuthState {
  isAuthenticated: boolean
  displayName: string
  isAdmin: boolean
  user: {
    email?: string
    username?: string
    name?: string
    nickname?: string
    picture?: string
  } | null
}

interface ThemeAdapter {
  getState: () => ThemeState
  subscribe: (callback: (state: ThemeState) => void) => () => void
  toggle: () => void
  initialize: () => void
}

interface AuthAdapter {
  getState: () => AuthState
  subscribe: (callback: (state: AuthState) => void) => () => void
  hasRole: (role: string) => boolean
  logout: () => void
}

// ============================================
// usePortalTheme Hook
// ============================================

export function usePortalTheme() {
  const [theme, setTheme] = useState<ThemeState>({ isDark: false })
  const [adapter, setAdapter] = useState<ThemeAdapter | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    // Standalone 모드에서는 adapter 사용하지 않음
    if (!window.__POWERED_BY_PORTAL_SHELL__) {
      setLoading(false)
      return
    }

    // Portal Shell에서 storeAdapter 동적 import
    import('portal/stores')
      .then((module) => {
        const themeAdapter = module.themeAdapter as ThemeAdapter
        setAdapter(themeAdapter)

        // 초기 상태 설정
        setTheme(themeAdapter.getState())

        // 변경 구독
        const unsubscribe = themeAdapter.subscribe((newState) => {
          setTheme(newState)
        })

        setLoading(false)

        return () => unsubscribe()
      })
      .catch((err) => {
        console.error('[usePortalTheme] Failed to load storeAdapter:', err)
        setError(err)
        setLoading(false)
      })
  }, [])

  const toggle = useCallback(() => {
    if (adapter) {
      adapter.toggle()
    }
  }, [adapter])

  return {
    isDark: theme.isDark,
    toggle,
    loading,
    error,
    isConnected: adapter !== null
  }
}

// ============================================
// usePortalAuth Hook
// ============================================

export function usePortalAuth() {
  const [auth, setAuth] = useState<AuthState>({
    isAuthenticated: false,
    displayName: 'Guest',
    isAdmin: false,
    user: null
  })
  const [adapter, setAdapter] = useState<AuthAdapter | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    // Standalone 모드에서는 adapter 사용하지 않음
    if (!window.__POWERED_BY_PORTAL_SHELL__) {
      setLoading(false)
      return
    }

    // Portal Shell에서 storeAdapter 동적 import
    import('portal/stores')
      .then((module) => {
        const authAdapter = module.authAdapter as AuthAdapter
        setAdapter(authAdapter)

        // 초기 상태 설정
        setAuth(authAdapter.getState())

        // 변경 구독
        const unsubscribe = authAdapter.subscribe((newState) => {
          setAuth(newState)
        })

        setLoading(false)

        return () => unsubscribe()
      })
      .catch((err) => {
        console.error('[usePortalAuth] Failed to load storeAdapter:', err)
        setError(err)
        setLoading(false)
      })
  }, [])

  const hasRole = useCallback((role: string): boolean => {
    if (adapter) {
      return adapter.hasRole(role)
    }
    return false
  }, [adapter])

  const logout = useCallback(() => {
    if (adapter) {
      adapter.logout()
    }
  }, [adapter])

  return {
    ...auth,
    hasRole,
    logout,
    loading,
    error,
    isConnected: adapter !== null
  }
}

// ============================================
// Combined Hook (Optional)
// ============================================

export function usePortalStore() {
  const theme = usePortalTheme()
  const auth = usePortalAuth()

  return {
    theme,
    auth
  }
}

export default usePortalStore
