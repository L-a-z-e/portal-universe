/**
 * usePortalTheme - Vue Composable for Portal Theme State
 *
 * portal-shell의 themeAdapter를 통해 테마 상태를 reactive하게 소비.
 * usePortalAuth와 동일한 패턴: adapter subscribe → Vue ref 동기화.
 */
import { ref, computed } from 'vue'
import { themeAdapter } from 'portal/stores'
import type { ThemeState } from '../types'

// Module-level singleton: 모든 컴포넌트가 같은 reactive state를 공유
const _state = ref<ThemeState>(themeAdapter.getState())

const _unsubscribe = themeAdapter.subscribe((newState: ThemeState) => {
  _state.value = newState
})

/** MF app unmount 시 호출 */
export function disposePortalTheme(): void {
  _unsubscribe()
  _state.value = { isDark: false }
}

export function usePortalTheme() {
  return {
    isDark: computed(() => _state.value.isDark),
    toggle: () => themeAdapter.toggle(),
    initialize: () => themeAdapter.initialize(),
  }
}
