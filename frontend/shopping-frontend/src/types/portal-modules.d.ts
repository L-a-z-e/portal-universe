/**
 * Module Federation 외부 모듈 타입 선언
 * Portal Shell에서 제공하는 모듈들의 타입 정의
 */

// 전역 Window 인터페이스 확장
interface Window {
  __POWERED_BY_PORTAL_SHELL__?: boolean
  __FEDERATION__?: boolean
  __PORTAL_ACCESS_TOKEN__?: string
  __PORTAL_API_CLIENT__?: import('axios').AxiosInstance
  __PORTAL_ON_AUTH_ERROR__?: () => void
}

/**
 * portal/api 모듈 - API 관련 exports
 */
declare module 'portal/api' {
  import type { AxiosInstance, AxiosResponse } from 'axios'

  // API Client
  export const apiClient: AxiosInstance

  // Types
  export interface FieldError {
    field: string
    message: string
    rejectedValue?: unknown
  }

  export interface ErrorDetails {
    code: string
    message: string
    timestamp?: string
    path?: string
    details?: FieldError[]
  }

  export interface ApiResponse<T> {
    success: true
    data: T
    error: null
  }

  export interface ApiErrorResponse {
    success: false
    data: null
    error: ErrorDetails
  }

  // Utilities
  export function getData<T>(response: AxiosResponse<ApiResponse<T>>): T
  export function getErrorDetails(error: unknown): ErrorDetails | null
  export function getErrorMessage(error: unknown): string
  export function getErrorCode(error: unknown): string | null
}

/**
 * portal/stores 모듈 - Store 관련 exports
 */
declare module 'portal/stores' {
  // ============================================
  // Pinia Stores (Vue용)
  // ============================================

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
    memberships: Record<string, string>
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

  // Theme Store
  export type ThemeMode = 'dark' | 'light' | 'system'

  interface ThemeStore {
    isDark: boolean
    mode: ThemeMode
    toggle: () => void
    setMode: (mode: ThemeMode) => void
    applyTheme: () => void
  }

  export function useThemeStore(): ThemeStore

  // ============================================
  // Store Adapters (React 등 다른 프레임워크용)
  // ============================================

  // Theme Store Adapter Types
  export interface ThemeState {
    isDark: boolean
  }

  interface ThemeAdapter {
    getState: () => ThemeState
    subscribe: (callback: (state: ThemeState) => void) => () => void
    toggle: () => void
    initialize: () => void
  }

  // Auth Store Adapter Types
  export interface AuthState {
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

  interface AuthAdapter {
    getState: () => AuthState
    subscribe: (callback: (state: AuthState) => void) => () => void
    hasRole: (role: string) => boolean
    hasAnyRole: (roles: string[]) => boolean
    isServiceAdmin: (service: string) => boolean
    logout: () => void
  }

  export type UnsubscribeFn = () => void

  export const themeAdapter: ThemeAdapter
  export const authAdapter: AuthAdapter
  export const portalStoreAdapter: {
    theme: ThemeAdapter
    auth: AuthAdapter
  }
  export const storeAdapter: typeof portalStoreAdapter
}
