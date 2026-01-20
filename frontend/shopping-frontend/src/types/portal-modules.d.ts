/**
 * Module Federation 외부 모듈 타입 선언
 * Portal Shell에서 제공하는 모듈들의 타입 정의
 */

// 전역 Window 인터페이스 확장
interface Window {
  __POWERED_BY_PORTAL_SHELL__?: boolean
  __PORTAL_ACCESS_TOKEN__?: string
}

declare module 'portal/themeStore' {
  interface ThemeStore {
    isDark: boolean
    toggleTheme: () => void
  }

  export function useThemeStore(): ThemeStore
}

declare module 'portal/authStore' {
  // Portal Shell의 실제 PortalUser 구조
  interface UserProfile {
    sub: string
    email: string
    username?: string
    name?: string
    nickname?: string
    picture?: string
    emailVerified?: boolean
    locale?: string
    timezone?: string
  }

  interface UserAuthority {
    roles: string[]
    scopes: string[]
  }

  interface PortalUser {
    profile: UserProfile
    authority: UserAuthority
    preferences: {
      theme: string
      language: string
      notifications: boolean
    }
    _accessToken: string
    _refreshToken?: string
    _expiresAt?: number
    _issuedAt?: number
  }

  interface AuthState {
    user: PortalUser | null
    isAuthenticated: boolean
  }

  interface AuthStore {
    (): AuthState
    getState?: () => AuthState
    subscribe?: (listener: (state: AuthState) => void) => () => void
  }

  export const useAuthStore: AuthStore
}

declare module 'portal/apiClient' {
  import type { AxiosInstance } from 'axios'
  export const apiClient: AxiosInstance
}

declare module 'portal/storeAdapter' {
  // Theme Store Adapter Types
  interface ThemeState {
    isDark: boolean
  }

  interface ThemeAdapter {
    getState: () => ThemeState
    subscribe: (callback: (state: ThemeState) => void) => () => void
    toggle: () => void
    initialize: () => void
  }

  // Auth Store Adapter Types
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

  interface AuthAdapter {
    getState: () => AuthState
    subscribe: (callback: (state: AuthState) => void) => () => void
    hasRole: (role: string) => boolean
    logout: () => void
  }

  export const themeAdapter: ThemeAdapter
  export const authAdapter: AuthAdapter
  export const portalStoreAdapter: {
    theme: ThemeAdapter
    auth: AuthAdapter
  }
}
