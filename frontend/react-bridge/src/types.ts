/**
 * Generic Store Adapter Interface
 *
 * Portal Shell의 storeAdapter가 이미 이 형태를 만족함.
 * 이 제네릭 인터페이스가 framework-agnostic contract 역할.
 */
export interface StoreAdapter<T> {
  getState: () => T
  subscribe: (callback: (state: T) => void) => () => void
}

/**
 * Auth State (portal-shell storeAdapter의 AuthState와 동일)
 */
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

/**
 * Auth Adapter Actions
 */
export interface AuthActions {
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
  isServiceAdmin: (service: string) => boolean
  logout: () => void
  getAccessToken?: () => string | null
  requestLogin?: (path?: string) => void
}

/**
 * Theme State
 */
export interface ThemeState {
  isDark: boolean
}

/**
 * Theme Adapter Actions
 */
export interface ThemeActions {
  toggle: () => void
  initialize: () => void
}

/**
 * Resolved Adapters Map
 */
export interface ResolvedAdapters {
  auth: StoreAdapter<AuthState> & AuthActions
  theme: StoreAdapter<ThemeState> & ThemeActions
}
