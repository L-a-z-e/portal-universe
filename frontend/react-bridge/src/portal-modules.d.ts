/**
 * Module Federation 외부 모듈 타입 선언
 */

declare module 'portal/stores' {
  import type { StoreAdapter, AuthState, AuthActions, ThemeState, ThemeActions } from './types'

  export const authAdapter: StoreAdapter<AuthState> & AuthActions
  export const themeAdapter: StoreAdapter<ThemeState> & ThemeActions
  export const portalStoreAdapter: {
    theme: typeof themeAdapter
    auth: typeof authAdapter
  }
}

interface Window {
  __POWERED_BY_PORTAL_SHELL__?: boolean
  __PORTAL_ACCESS_TOKEN__?: string
  __PORTAL_GET_ACCESS_TOKEN__?: () => string | null
  __PORTAL_API_CLIENT__?: unknown
  __PORTAL_ON_AUTH_ERROR__?: () => void
  __PORTAL_SHOW_LOGIN__?: () => void
}
