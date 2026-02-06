/**
 * API Client Configuration
 *
 * Embedded 모드: portal/api의 apiClient 사용 (토큰 자동 갱신, 401/429 재시도 포함)
 * Standalone 모드: local fallback client 사용
 */
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { getPortalApiClient, isBridgeReady, getAdapter } from '@portal/react-bridge'

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
 * Standalone용 local Axios 인스턴스 (lazy 생성)
 */
let localClient: AxiosInstance | null = null

const getLocalClient = (): AxiosInstance => {
  if (localClient) return localClient

  localClient = axios.create({
    baseURL: getBaseUrl(),
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    },
    withCredentials: true
  })

  // Request Interceptor: 토큰 자동 첨부
  localClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      let token: string | null | undefined = null

      if (isBridgeReady()) {
        token = getAdapter('auth').getAccessToken?.()
      }

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

  // Response Interceptor: 에러 핸들링
  localClient.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      const status = error.response?.status

      const apiError = (error.response?.data as Record<string, unknown>)?.error as Record<string, unknown> | undefined
      if (apiError?.message) {
        error.message = apiError.message as string
      }

      if (status === 401) {
        console.warn('[Shopping API] Unauthorized - token may be expired')
        window.__PORTAL_ON_AUTH_ERROR__?.()
      }

      return Promise.reject(error)
    }
  )

  return localClient
}

/**
 * API Client 반환
 * portal/api가 있으면 완전판 사용 (토큰 갱신, 401/429 재시도),
 * 없으면 local fallback (Standalone 모드)
 */
export const getApiClient = (): AxiosInstance => {
  return getPortalApiClient() ?? getLocalClient()
}

export default getApiClient
