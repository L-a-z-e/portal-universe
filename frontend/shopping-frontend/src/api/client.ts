/**
 * API Client Configuration
 *
 * @portal/react-bridge의 createPortalApiClient를 기반으로
 * shopping-specific 설정을 추가합니다.
 */
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { isBridgeReady, getAdapter } from '@portal/react-bridge'

// API Base URL 설정 (환경별)
const getBaseUrl = (): string => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL
  }

  if (import.meta.env.DEV) {
    return 'http://localhost:8080'
  }

  return ''
}

/**
 * Axios 인스턴스 생성
 */
const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: getBaseUrl(),
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    },
    withCredentials: true
  })

  // Request Interceptor: 토큰 자동 첨부
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      let token: string | null | undefined = null

      // Bridge에서 토큰 가져오기 (우선)
      if (isBridgeReady()) {
        token = getAdapter('auth').getAccessToken?.()
      }

      // Fallback: window globals
      if (!token) {
        token = window.__PORTAL_GET_ACCESS_TOKEN__?.() ?? window.__PORTAL_ACCESS_TOKEN__
      }

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }

      if (import.meta.env.DEV) {
        console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`)
      }

      return config
    },
    (error: AxiosError) => {
      console.error('[API] Request error:', error)
      return Promise.reject(error)
    }
  )

  // Response Interceptor: 에러 핸들링
  client.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      const status = error.response?.status

      // ApiResponse 에러 형식 파싱
      const apiError = (error.response?.data as Record<string, unknown>)?.error as Record<string, unknown> | undefined
      if (apiError?.message) {
        error.message = apiError.message as string
      }

      if (status === 401) {
        console.warn('[API] Unauthorized - token may be expired')
        window.__PORTAL_ON_AUTH_ERROR__?.()
      } else if (status === 403) {
        console.warn('[API] Forbidden - insufficient permissions')
      } else if (status === 404) {
        console.warn('[API] Not Found')
      } else if (status && status >= 500) {
        console.error('[API] Server Error:', error.response?.data)
      }

      return Promise.reject(error)
    }
  )

  return client
}

// 싱글톤 인스턴스
export const apiClient = createApiClient()

// Host에서 apiClient가 주입된 경우 사용
export const getApiClient = (): AxiosInstance => {
  if (window.__PORTAL_API_CLIENT__) {
    return window.__PORTAL_API_CLIENT__ as AxiosInstance
  }
  return apiClient
}

export default apiClient
