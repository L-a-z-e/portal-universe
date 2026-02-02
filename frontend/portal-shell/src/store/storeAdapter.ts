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

// 스냅샷 캐시: useSyncExternalStore는 Object.is로 비교하므로
// 값이 동일하면 같은 참조를 반환해야 무한 렌더링을 방지함
let _themeSnapshot: ThemeState | null = null

export const themeAdapter = {
  /**
   * 현재 테마 상태 반환 (참조 안정성 보장)
   */
  getState: (): ThemeState => {
    const store = useThemeStore()
    const isDark = store.isDark
    if (_themeSnapshot && _themeSnapshot.isDark === isDark) {
      return _themeSnapshot
    }
    _themeSnapshot = { isDark }
    return _themeSnapshot
  },

  /**
   * 테마 상태 변경 구독
   * @param callback 상태 변경 시 호출될 콜백
   * @returns 구독 해제 함수
   */
  subscribe: (callback: (state: ThemeState) => void): UnsubscribeFn => {
    const store = useThemeStore()

    // NOTE: immediate: false 필수 — useSyncExternalStore는
    // subscribe 중 동기 콜백 호출을 허용하지 않음 (React Error #185)
    // React는 getState()로 초기값을 읽음
    const unwatch = watch(
      () => store.isDark,
      (isDark) => {
        _themeSnapshot = { isDark }
        callback(_themeSnapshot)
      },
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

// 스냅샷 캐시: 참조 안정성 보장
let _authSnapshot: AuthState | null = null
let _authUserRef: unknown = undefined // Pinia store.user 참조 추적

function buildAuthState(store: ReturnType<typeof useAuthStore>): AuthState {
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
}

export const authAdapter = {
  /**
   * 현재 인증 상태 반환 (참조 안정성 보장)
   */
  getState: (): AuthState => {
    const store = useAuthStore()

    // 주요 primitive 필드로 변경 감지 (Object.is 비교 대응)
    if (_authSnapshot &&
      _authSnapshot.isAuthenticated === store.isAuthenticated &&
      _authSnapshot.displayName === store.displayName &&
      _authSnapshot.isAdmin === store.isAdmin &&
      _authSnapshot.isSeller === store.isSeller &&
      _authUserRef === store.user
    ) {
      return _authSnapshot
    }

    _authUserRef = store.user
    _authSnapshot = buildAuthState(store)
    return _authSnapshot
  },

  /**
   * 인증 상태 변경 구독
   * @param callback 상태 변경 시 호출될 콜백
   * @returns 구독 해제 함수
   */
  subscribe: (callback: (state: AuthState) => void): UnsubscribeFn => {
    const store = useAuthStore()

    // NOTE: immediate: false 필수 — useSyncExternalStore는
    // subscribe 중 동기 콜백 호출을 허용하지 않음 (React Error #185)
    // React는 getState()로 초기값을 읽음
    const unwatch = watch(
      () => store.user,
      () => {
        _authUserRef = store.user
        _authSnapshot = buildAuthState(store)
        callback(_authSnapshot)
      },
      { deep: true }
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
  },

  /**
   * 현재 access token 반환
   */
  getAccessToken: (): string | null => {
    const store = useAuthStore()
    return store.user?._accessToken ?? window.__PORTAL_ACCESS_TOKEN__ ?? null
  },

  /**
   * 로그인 모달 요청 (Remote 앱에서 호출)
   * @param path 로그인 후 리다이렉트할 경로
   */
  requestLogin: (path?: string): void => {
    const store = useAuthStore()
    store.requestLogin(path)
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
