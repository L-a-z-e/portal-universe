// Types
export type {
  StoreAdapter,
  AuthState,
  AuthActions,
  ThemeState,
  ThemeActions,
  ResolvedAdapters,
} from './types'

// Bridge Registry
export { initBridge, isBridgeReady, getAdapter, getBridgeError } from './bridge-registry'

// Factory
export { createStoreHook } from './create-store-hook'

// Provider
export { PortalBridgeProvider } from './PortalBridgeProvider'

// Hooks
export { usePortalAuth } from './hooks/usePortalAuth'
export { usePortalTheme } from './hooks/usePortalTheme'
export { usePortalBridge } from './hooks/usePortalBridge'
export { useIsEmbedded } from './hooks/useIsEmbedded'

// Components
export { RequireAuth } from './components/RequireAuth'

// API Registry
export { initPortalApi, getPortalApiClient } from './api-registry'
