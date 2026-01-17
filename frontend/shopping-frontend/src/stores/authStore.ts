/**
 * Auth Store (Zustand)
 *
 * 인증 상태 관리
 * - Embedded 모드: Portal Shell의 authStore 연동
 * - Standalone 모드: 로컬 상태 관리
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

interface User {
  id: string
  email: string
  name: string
  role: 'guest' | 'user' | 'admin'
  avatar?: string
}

interface AuthState {
  // State
  user: User | null
  isAuthenticated: boolean
  accessToken: string | null
  loading: boolean
  error: string | null

  // Actions
  setUser: (user: User | null) => void
  setAccessToken: (token: string | null) => void
  logout: () => void
  syncFromPortal: () => Promise<void>
}

const initialState = {
  user: null,
  isAuthenticated: false,
  accessToken: null,
  loading: false,
  error: null
}

export const useAuthStore = create<AuthState>()(
  devtools(
    (set, get) => ({
      ...initialState,

      /**
       * 사용자 설정
       */
      setUser: (user: User | null) => {
        set({
          user,
          isAuthenticated: user !== null
        })
      },

      /**
       * 액세스 토큰 설정
       */
      setAccessToken: (token: string | null) => {
        set({ accessToken: token })
        // 전역 토큰도 업데이트
        if (token) {
          window.__PORTAL_ACCESS_TOKEN__ = token
          localStorage.setItem('access_token', token)
        } else {
          delete window.__PORTAL_ACCESS_TOKEN__
          localStorage.removeItem('access_token')
        }
      },

      /**
       * 로그아웃
       */
      logout: () => {
        set(initialState)
        delete window.__PORTAL_ACCESS_TOKEN__
        localStorage.removeItem('access_token')
      },

      /**
       * Portal Shell에서 인증 상태 동기화 (Embedded 모드)
       */
      syncFromPortal: async () => {
        if (!window.__POWERED_BY_PORTAL_SHELL__) {
          console.log('[Auth] Not in embedded mode, skipping Portal sync')
          return
        }

        set({ loading: true, error: null })

        try {
          // Portal Shell의 authStore import
          const { useAuthStore: usePortalAuthStore } = await import('portal/authStore')
          const portalAuth = usePortalAuthStore.getState()

          console.log('[Auth] Syncing from Portal Shell:', portalAuth)

          // 상태 동기화
          set({
            user: portalAuth.user,
            isAuthenticated: portalAuth.isAuthenticated,
            accessToken: portalAuth.accessToken,
            loading: false
          })

          // 전역 토큰 설정
          if (portalAuth.accessToken) {
            window.__PORTAL_ACCESS_TOKEN__ = portalAuth.accessToken
          }
        } catch (error: any) {
          console.warn('[Auth] Failed to sync from Portal:', error)
          set({
            error: error.message || 'Failed to sync auth state',
            loading: false
          })
        }
      }
    }),
    { name: 'AuthStore' }
  )
)

// Portal authStore 타입 선언
declare module 'portal/authStore' {
  export const useAuthStore: {
    getState: () => {
      user: User | null
      isAuthenticated: boolean
      accessToken: string | null
    }
    subscribe: (listener: (state: any) => void) => () => void
  }
}

export default useAuthStore
