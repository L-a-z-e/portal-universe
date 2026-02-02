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
  isSeller: boolean
  roles: string[]
  memberships: Record<string, string>
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
  hasAnyRole: (roles: string[]) => boolean
  isServiceAdmin: (service: string) => boolean
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

    let isMounted = true
    let unsubscribe: (() => void) | undefined

    // Portal Shell에서 storeAdapter 동적 import
    import('portal/stores')
      .then((module) => {
        if (!isMounted) return

        // Module Federation의 wrapDefault로 인해 module.default에 있을 수 있음
        const rawModule = module as typeof module & { default?: typeof module }
        const actualModule = rawModule.default ?? rawModule
        const themeAdapter = actualModule.themeAdapter as ThemeAdapter

        if (!themeAdapter) {
          throw new Error('themeAdapter not found in portal/stores')
        }

        setAdapter(themeAdapter)
        setTheme(themeAdapter.getState())

        unsubscribe = themeAdapter.subscribe((newState) => {
          setTheme(newState)
        })

        setLoading(false)
      })
      .catch((err) => {
        if (!isMounted) return
        console.error('[usePortalTheme] Failed to load storeAdapter:', err)
        setError(err)
        setLoading(false)
      })

    return () => {
      isMounted = false
      unsubscribe?.()
    }
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
    isSeller: false,
    roles: [],
    memberships: {},
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

    let isMounted = true
    let unsubscribe: (() => void) | undefined

    // Portal Shell에서 storeAdapter 동적 import
    import('portal/stores')
      .then((module) => {
        if (!isMounted) return

        const rawModule = module as typeof module & { default?: typeof module }
        const actualModule = rawModule.default ?? rawModule
        const authAdapter = actualModule.authAdapter as AuthAdapter

        if (!authAdapter) {
          throw new Error('authAdapter not found in portal/stores')
        }

        setAdapter(authAdapter)
        setAuth(authAdapter.getState())

        unsubscribe = authAdapter.subscribe((newState) => {
          setAuth(newState)
        })

        setLoading(false)
      })
      .catch((err) => {
        if (!isMounted) return
        console.error('[usePortalAuth] Failed to load storeAdapter:', err)
        setError(err)
        setLoading(false)
      })

    return () => {
      isMounted = false
      unsubscribe?.()
    }
  }, [])

  const hasRole = useCallback((role: string): boolean => {
    if (adapter) {
      return adapter.hasRole(role)
    }
    return false
  }, [adapter])

  const hasAnyRole = useCallback((roles: string[]): boolean => {
    if (adapter) {
      return adapter.hasAnyRole(roles)
    }
    return false
  }, [adapter])

  const isServiceAdmin = useCallback((service: string): boolean => {
    if (adapter) {
      return adapter.isServiceAdmin(service)
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
    hasAnyRole,
    isServiceAdmin,
    logout,
    loading,
    error,
    isConnected: adapter !== null
  }
}

// ============================================
// Combined Hook
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
