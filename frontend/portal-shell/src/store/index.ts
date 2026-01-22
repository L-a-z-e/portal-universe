// portal-shell/src/store/index.ts
// Store 모듈 통합 export

// Pinia Stores (Vue용)
export { useAuthStore } from './auth';
export { useThemeStore, type ThemeMode } from './theme';

// Store Adapter (React 등 다른 프레임워크용)
export {
  themeAdapter,
  authAdapter,
  portalStoreAdapter,
  type ThemeState,
  type AuthState,
  type UnsubscribeFn,
} from './storeAdapter';

// Default export
export { default as storeAdapter } from './storeAdapter';
