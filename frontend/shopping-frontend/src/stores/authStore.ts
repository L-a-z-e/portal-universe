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
  roles: string[]
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
       *
       * Vue(Pinia) ↔ React(Zustand) 간 상태 공유:
       * - authAdapter 사용 (Framework-agnostic adapter)
       * - Pinia store 직접 호출 불가 (React 환경에서 Vue Pinia hook 사용 불가)
       */
      syncFromPortal: async () => {
        if (!window.__POWERED_BY_PORTAL_SHELL__) {
          console.log('[Auth] Not in embedded mode, skipping Portal sync')
          return
        }

        set({ loading: true, error: null })

        try {
          // ✅ authAdapter 사용 (Framework-agnostic)
          const portalStoresModule = await import('portal/stores')
          // Module Federation의 wrapDefault로 인해 module.default에 있을 수 있음
          const actualModule = (portalStoresModule as any).default || portalStoresModule
          const authAdapter = actualModule.authAdapter

          if (!authAdapter) {
            console.warn('[Auth] authAdapter not found in portal/stores')
            set({ loading: false })
            return
          }

          // authAdapter에서 현재 인증 상태 가져오기
          const authState = authAdapter.getState() as {
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
          console.log('[Auth] authAdapter state:', authState)

          if (authState.isAuthenticated && authState.user) {
            // User 정보 매핑 (AuthAdapter State → Zustand User)
            const mappedUser: User = {
              id: '',
              email: authState.user.email || '',
              name: authState.user.name || authState.user.nickname || authState.displayName,
              roles: authState.roles || [],
              avatar: authState.user.picture
            }

            // 전역 토큰 확인
            const globalToken = window.__PORTAL_ACCESS_TOKEN__

            set({
              user: mappedUser,
              isAuthenticated: true,
              accessToken: globalToken || null,
              loading: false
            })

            console.log('[Auth] ✅ Synced from authAdapter:', mappedUser)
            return
          }

          // 인증 정보 없음
          console.log('[Auth] No authentication found via authAdapter')
          set({ loading: false })

        } catch (error: unknown) {
          const errorMessage = error instanceof Error ? error.message : 'Failed to sync auth state'
          console.warn('[Auth] Failed to sync from Portal:', error)
          set({
            error: errorMessage,
            loading: false
          })
        }
      }
    }),
    { name: 'AuthStore' }
  )
)

export default useAuthStore
