/**
 * Bridge Registry - Module-level Singleton
 *
 * React Context 대신 module singleton을 사용하는 이유:
 * - MF boundary에서 Context가 깨질 수 있음 (React 인스턴스 불일치)
 * - storeAdapter 자체가 이미 module singleton 패턴
 */
import type { ResolvedAdapters } from './types'

let resolvedAdapters: ResolvedAdapters | null = null
let resolvePromise: Promise<void> | null = null
let initError: Error | null = null

/**
 * Bridge 초기화 - portal/stores에서 adapter를 resolve
 */
export function initBridge(): Promise<void> {
  if (resolvedAdapters) return Promise.resolve()
  if (resolvePromise) return resolvePromise

  resolvePromise = import('portal/stores')
    .then((module) => {
      const raw = module as Record<string, unknown>
      const actual = (raw.default ?? raw) as Record<string, unknown>

      const authAdapter = actual.authAdapter as ResolvedAdapters['auth']
      const themeAdapter = actual.themeAdapter as ResolvedAdapters['theme']

      if (!authAdapter) throw new Error('authAdapter not found in portal/stores')
      if (!themeAdapter) throw new Error('themeAdapter not found in portal/stores')

      resolvedAdapters = { auth: authAdapter, theme: themeAdapter }
    })
    .catch((err) => {
      initError = err instanceof Error ? err : new Error(String(err))
      resolvePromise = null // allow retry
      throw initError
    })

  return resolvePromise
}

/**
 * 등록된 adapter 가져오기
 */
export function getAdapter<K extends keyof ResolvedAdapters>(key: K): ResolvedAdapters[K] {
  if (!resolvedAdapters) throw new Error(`Bridge not initialized. Call initBridge() first.`)
  return resolvedAdapters[key]
}

/**
 * Bridge 초기화 완료 여부
 */
export function isBridgeReady(): boolean {
  return resolvedAdapters !== null
}

/**
 * 초기화 에러 반환
 */
export function getBridgeError(): Error | null {
  return initError
}
