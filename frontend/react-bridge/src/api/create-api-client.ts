/**
 * createPortalApiClient - Bridge 기반 Token 주입 Axios Factory
 *
 * bridge가 초기화되어 있으면 adapter에서 토큰을 가져오고,
 * fallback으로 window globals 사용.
 */
import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosError } from 'axios'
import { isBridgeReady, getAdapter } from '../bridge-registry'

export function createPortalApiClient(baseURL?: string): AxiosInstance {
  const client = axios.create({
    baseURL,
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true,
  })

  // Request Interceptor: 토큰 자동 주입
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      let token: string | null | undefined = null

      if (isBridgeReady()) {
        const authAdapter = getAdapter('auth')
        token = authAdapter.getAccessToken?.()
      }

      // Fallback: window globals
      if (!token) {
        token = window.__PORTAL_GET_ACCESS_TOKEN__?.() ?? window.__PORTAL_ACCESS_TOKEN__
      }

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }

      return config
    },
    (error: AxiosError) => Promise.reject(error)
  )

  // Response Interceptor: 401 처리
  client.interceptors.response.use(
    undefined,
    (error: AxiosError) => {
      if (error.response?.status === 401) {
        window.__PORTAL_ON_AUTH_ERROR__?.()
      }
      return Promise.reject(error)
    }
  )

  return client
}
