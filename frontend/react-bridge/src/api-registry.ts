/**
 * API Registry - portal/api의 apiClient를 resolve하여 캐싱
 *
 * bridge-registry.ts와 동일한 module singleton 패턴.
 * portal/api의 apiClient는 토큰 자동 갱신, 401 재시도, 429 재시도를 포함한 완전판.
 */
import type { AxiosInstance } from 'axios'

let portalApiClient: AxiosInstance | null = null
let resolvePromise: Promise<void> | null = null

/**
 * portal/api에서 apiClient를 resolve
 * 실패해도 throw하지 않음 (warn만) - bridge 초기화에 영향 없음
 */
export function initPortalApi(): Promise<void> {
  if (portalApiClient) return Promise.resolve()
  if (resolvePromise) return resolvePromise

  resolvePromise = import('portal/api')
    .then((module) => {
      const raw = module as Record<string, unknown>
      const actual = (raw.default ?? raw) as Record<string, unknown>
      const client = actual.apiClient as AxiosInstance | undefined

      if (!client) {
        console.warn('[api-registry] apiClient not found in portal/api')
        return
      }

      portalApiClient = client
      console.log('[api-registry] portal/api apiClient resolved')
    })
    .catch((err) => {
      console.warn('[api-registry] Failed to load portal/api:', err)
      resolvePromise = null // allow retry
    })

  return resolvePromise
}

/**
 * portal/api의 apiClient 반환
 * 미초기화 또는 실패 시 null 반환 (caller가 local fallback 사용)
 */
export function getPortalApiClient(): AxiosInstance | null {
  return portalApiClient
}
