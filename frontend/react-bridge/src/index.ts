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

// Components
export { RequireAuth } from './components/RequireAuth'

// API
export { createPortalApiClient } from './api/create-api-client'
