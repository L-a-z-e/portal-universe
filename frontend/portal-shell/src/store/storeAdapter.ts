/**
 * Store Adapter - Framework-Agnostic Wrapper
 *
 * Pinia 스토어를 순수 JavaScript 함수로 래핑하여
 * React 등 다른 프레임워크에서도 사용 가능하게 함
 *
 * 사용처:
 * - Shopping Frontend (React)
 * - 향후 다른 React/Svelte 등 Remote 앱
 */

import { watch } from 'vue'
import { useThemeStore } from './theme'
import { useAuthStore } from './auth'

// ============================================
// Type Definitions
// ============================================

export interface ThemeState {
  isDark: boolean
}

export interface AuthState {
  isAuthenticated: boolean
  displayName: string
  isAdmin: boolean
  isSeller: boolean
  roles: string[]
  memberships: Record<string, string>
  user: {
    uuid?: string
    email?: string
    username?: string
    name?: string
    nickname?: string
    picture?: string
  } | null
}

export type UnsubscribeFn = () => void

// ============================================
// Theme Store Adapter
// ============================================

export const themeAdapter = {
  /**
   * 현재 테마 상태 반환
   */
  getState: (): ThemeState => {
    const store = useThemeStore()
    return { isDark: store.isDark }
  },

  /**
   * 테마 상태 변경 구독
   * @param callback 상태 변경 시 호출될 콜백
   * @returns 구독 해제 함수
   */
  subscribe: (callback: (state: ThemeState) => void): UnsubscribeFn => {
    const store = useThemeStore()

    const unwatch = watch(
      () => store.isDark,
      (isDark) => callback({ isDark }),
      { immediate: true }
    )

    return unwatch
  },

  /**
   * 테마 토글
   */
  toggle: (): void => {
    const store = useThemeStore()
    store.toggle()
  },

  /**
   * 테마 초기화 (localStorage에서 복원)
   */
  initialize: (): void => {
    const store = useThemeStore()
    store.initialize()
  }
}

// ============================================
// Auth Store Adapter
// ============================================

export const authAdapter = {
  /**
   * 현재 인증 상태 반환
   */
  getState: (): AuthState => {
    const store = useAuthStore()

    return {
      isAuthenticated: store.isAuthenticated,
      displayName: store.displayName,
      isAdmin: store.isAdmin,
      isSeller: store.isSeller,
      roles: store.user?.authority.roles || [],
      memberships: store.user?.authority.memberships || {},
      user: store.user ? {
        uuid: store.user.profile.sub,
        email: store.user.profile.email,
        username: store.user.profile.username,
        name: store.user.profile.name,
        nickname: store.user.profile.nickname,
        picture: store.user.profile.picture
      } : null
    }
  },

  /**
   * 인증 상태 변경 구독
   * @param callback 상태 변경 시 호출될 콜백
   * @returns 구독 해제 함수
   */
  subscribe: (callback: (state: AuthState) => void): UnsubscribeFn => {
    const store = useAuthStore()

    const unwatch = watch(
      () => store.user,
      () => callback(authAdapter.getState()),
      { immediate: true, deep: true }
    )

    return unwatch
  },

  /**
   * 역할 확인
   */
  hasRole: (role: string): boolean => {
    const store = useAuthStore()
    return store.hasRole(role)
  },

  /**
   * 복수 역할 중 하나 이상 보유 여부
   */
  hasAnyRole: (roles: string[]): boolean => {
    const store = useAuthStore()
    return store.hasAnyRole(roles)
  },

  /**
   * 특정 서비스의 admin 여부
   */
  isServiceAdmin: (service: string): boolean => {
    const store = useAuthStore()
    return store.isServiceAdmin(service)
  },

  /**
   * 로그아웃
   */
  logout: (): void => {
    const store = useAuthStore()
    store.logout()
  }
}

// ============================================
// Combined Adapter Export
// ============================================

export const portalStoreAdapter = {
  theme: themeAdapter,
  auth: authAdapter
}

export default portalStoreAdapter
