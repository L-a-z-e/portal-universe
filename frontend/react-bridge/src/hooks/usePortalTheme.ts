/**
 * usePortalTheme - Theme 전용 Hook
 *
 * Embedded 모드: bridge의 themeAdapter 소비 (useSyncExternalStore)
 * Standalone 모드: default theme 상태 반환
 */
import { useMemo } from 'react'
import { createStoreHook } from '../create-store-hook'
import { getAdapter, isBridgeReady } from '../bridge-registry'
import type { ThemeState } from '../types'

const defaultThemeState: ThemeState = {
  isDark: false,
}

const useThemeState = createStoreHook<ThemeState>(() => getAdapter('theme'))

export function usePortalTheme(): ThemeState & {
  toggle: () => void
  initialize: () => void
  isConnected: boolean
} {
  const isReady = isBridgeReady()
  const state = isReady ? useThemeState() : defaultThemeState

  const actions = useMemo(() => {
    if (!isReady) {
      return {
        toggle: () => {},
        initialize: () => {},
      }
    }

    const adapter = getAdapter('theme')
    return {
      toggle: adapter.toggle,
      initialize: adapter.initialize,
    }
  }, [isReady])

  return {
    ...state,
    ...actions,
    isConnected: isReady,
  }
}
