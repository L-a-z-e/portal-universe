/**
 * API Client Configuration
 *
 * Axios 인스턴스를 생성하고 interceptor를 설정합니다.
 * Host(Portal Shell)에서 주입된 apiClient를 사용할 수도 있습니다.
 */
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'

// API Base URL 설정 (환경별)
const getBaseUrl = (): string => {
  // 환경변수로 설정된 경우
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL
  }

  // 개발 환경: Gateway로 프록시
  if (import.meta.env.DEV) {
    return 'http://localhost:8080'
  }

  // 프로덕션: 같은 도메인의 Gateway
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
      // 1. Host에서 주입된 토큰 확인
      const portalToken = window.__PORTAL_ACCESS_TOKEN__

      // 2. localStorage에서 토큰 확인
      const localToken = localStorage.getItem('access_token')

      const token = portalToken || localToken

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }

      // 요청 로깅 (개발 환경)
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
    (response) => {
      return response
    },
    (error: AxiosError) => {
      const status = error.response?.status

      if (status === 401) {
        // 인증 만료 - 토큰 갱신 또는 로그인 페이지로 이동
        console.warn('[API] Unauthorized - token may be expired')

        // Host에게 인증 만료 알림
        if (window.__PORTAL_ON_AUTH_ERROR__) {
          window.__PORTAL_ON_AUTH_ERROR__()
        }
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
  // Portal Shell에서 주입된 apiClient가 있으면 사용
  if (window.__PORTAL_API_CLIENT__) {
    return window.__PORTAL_API_CLIENT__ as AxiosInstance
  }
  return apiClient
}

export default apiClient
