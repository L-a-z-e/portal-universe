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
       *
       * Vue(Pinia) ↔ React(Zustand) 간 상태 공유:
       * 1. window.__PORTAL_ACCESS_TOKEN__ 전역 변수 우선 사용 (가장 안정적)
       * 2. Pinia store 직접 호출 시도 (Pinia는 getState() 없음, 직접 호출)
       */
      syncFromPortal: async () => {
        if (!window.__POWERED_BY_PORTAL_SHELL__) {
          console.log('[Auth] Not in embedded mode, skipping Portal sync')
          return
        }

        set({ loading: true, error: null })

        try {
          // ✅ 방법 1: window 전역 변수에서 토큰 확인 (Portal Shell이 설정)
          const globalToken = window.__PORTAL_ACCESS_TOKEN__

          if (globalToken) {
            console.log('[Auth] Found token in window.__PORTAL_ACCESS_TOKEN__')

            // Pinia store에서 사용자 정보 가져오기 시도
            try {
              const portalAuthModule = await import('portal/authStore')
              // Pinia store는 함수로 호출해야 인스턴스를 얻음 (getState 아님!)
              const usePortalAuthStore = portalAuthModule.useAuthStore
              const portalStore = usePortalAuthStore()

              // Pinia reactive proxy에서 값 추출
              const portalUser = portalStore.user
              const portalIsAuthenticated = portalStore.isAuthenticated

              console.log('[Auth] Pinia store user:', portalUser)
              console.log('[Auth] Pinia store isAuthenticated:', portalIsAuthenticated)

              if (portalUser) {
                // User 정보 매핑 (Pinia PortalUser → Zustand User)
                const mappedUser: User = {
                  id: portalUser.profile?.sub || '',
                  email: portalUser.profile?.email || '',
                  name: portalUser.profile?.name || portalUser.profile?.nickname || '',
                  role: portalUser.authority?.roles?.includes('ROLE_ADMIN') ? 'admin' :
                        portalUser.authority?.roles?.includes('ROLE_USER') ? 'user' : 'guest',
                  avatar: portalUser.profile?.picture
                }

                set({
                  user: mappedUser,
                  isAuthenticated: true,
                  accessToken: globalToken,
                  loading: false
                })

                console.log('[Auth] ✅ Synced from Portal Shell:', mappedUser)
                return
              }
            } catch (piniaError) {
              console.warn('[Auth] Pinia store access failed, using token only:', piniaError)
            }

            // Pinia 실패 시에도 토큰만으로 인증 상태 설정
            set({
              user: { id: '', email: '', name: 'User', role: 'user' },
              isAuthenticated: true,
              accessToken: globalToken,
              loading: false
            })

            console.log('[Auth] ✅ Token synced (minimal user info)')
            return
          }

          // ✅ 방법 2: 토큰이 없으면 Pinia store에서 직접 추출 시도
          console.log('[Auth] No global token, trying Pinia store directly...')

          const portalAuthModule = await import('portal/authStore')
          const usePortalAuthStore = portalAuthModule.useAuthStore
          const portalStore = usePortalAuthStore()

          const portalUser = portalStore.user
          const portalToken = portalUser?._accessToken

          if (portalToken) {
            window.__PORTAL_ACCESS_TOKEN__ = portalToken

            const mappedUser: User = {
              id: portalUser.profile?.sub || '',
              email: portalUser.profile?.email || '',
              name: portalUser.profile?.name || portalUser.profile?.nickname || '',
              role: portalUser.authority?.roles?.includes('ROLE_ADMIN') ? 'admin' :
                    portalUser.authority?.roles?.includes('ROLE_USER') ? 'user' : 'guest',
              avatar: portalUser.profile?.picture
            }

            set({
              user: mappedUser,
              isAuthenticated: true,
              accessToken: portalToken,
              loading: false
            })

            console.log('[Auth] ✅ Synced from Pinia store:', mappedUser)
            return
          }

          // 인증 정보 없음
          console.warn('[Auth] No authentication found in Portal Shell')
          set({ loading: false })

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

export default useAuthStore
