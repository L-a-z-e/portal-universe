/**
 * portal/stores 모듈 타입 선언 (vue-bridge 내부용)
 *
 * vue-bridge는 portal/stores의 authAdapter / themeAdapter를 소비.
 * 이 선언은 Vite Library Mode 빌드 시 타입 체크를 위해 사용.
 */
declare module 'portal/stores' {
  import type { StoreAdapter, AuthState, AuthActions, ThemeState, ThemeActions } from '../types'

  export const authAdapter: StoreAdapter<AuthState> & AuthActions
  export const themeAdapter: StoreAdapter<ThemeState> & ThemeActions

  export type { AuthState }
}
