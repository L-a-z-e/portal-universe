/**
 * usePortalTheme - Theme 전용 Hook
 *
 * Embedded 모드: bridge의 themeAdapter 소비 (useSyncExternalStore)
 * Standalone 모드: default theme 상태 반환
 */
import { useMemo, useSyncExternalStore } from 'react'
import { getAdapter, isBridgeReady } from '../bridge-registry'
import type { ThemeState } from '../types'

const defaultThemeState: ThemeState = {
  isDark: false,
}

/** No-op adapter for standalone/uninitialized mode */
const noopSubscribe = () => () => {}
const getDefaultState = () => defaultThemeState

export function usePortalTheme(): ThemeState & {
  toggle: () => void
  initialize: () => void
  isConnected: boolean
} {
  const isReady = isBridgeReady()

  // getSnapshot을 useMemo로 안정화하여 동일 값일 때 같은 참조 반환
  // adapter.getState()가 매번 새 객체를 반환하더라도 안전
  const getSnapshot = useMemo(() => {
    if (!isReady) return getDefaultState
    const adapter = getAdapter('theme')
    let cached: ThemeState | undefined
    return () => {
      const next = adapter.getState()
      if (cached && cached.isDark === next.isDark) return cached
      cached = next
      return next
    }
  }, [isReady])

  const subscribe = useMemo(
    () => (isReady ? getAdapter('theme').subscribe : noopSubscribe),
    [isReady],
  )

  const state = useSyncExternalStore(subscribe, getSnapshot, getDefaultState)

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
