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
  interface User {
    id: string
    email: string
    name: string
    role: 'guest' | 'user' | 'admin'
    avatar?: string
  }

  interface AuthState {
    user: User | null
    isAuthenticated: boolean
    accessToken: string | null
  }

  interface AuthStore {
    (): AuthState
    getState: () => AuthState
    subscribe: (listener: (state: AuthState) => void) => () => void
  }

  export const useAuthStore: AuthStore
}

declare module 'portal/apiClient' {
  import type { AxiosInstance } from 'axios'
  export const apiClient: AxiosInstance
}
