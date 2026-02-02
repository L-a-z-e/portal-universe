/**
 * createStoreHook - OCP Factory (핵심)
 *
 * useSyncExternalStore를 사용하여 React 18 concurrent mode에서도
 * tearing 없는 견고한 동기화를 제공.
 *
 * 새 스토어(settings, serviceStatus 등) 추가 시:
 * 1. storeAdapter.ts에 새 adapter 추가 (portal-shell)
 * 2. bridge-registry.ts에 adapter 등록
 * 3. createStoreHook()으로 hook 생성 — 기존 코드 수정 0
 */
import { useSyncExternalStore } from 'react'
import type { StoreAdapter } from './types'

export function createStoreHook<T>(
  getAdapter: () => StoreAdapter<T>,
  serverSnapshot?: () => T
): () => T {
  return () => {
    const adapter = getAdapter()
    return useSyncExternalStore(
      adapter.subscribe,
      adapter.getState,
      serverSnapshot ?? adapter.getState
    )
  }
}
