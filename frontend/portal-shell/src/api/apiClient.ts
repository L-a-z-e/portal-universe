// portal-shell/src/api/apiClient.ts
import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { authService } from '../services/authService';
import type { ApiErrorResponse } from './types';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request Interceptor
 * - Automatically attach access token to requests
 * - Auto-refresh token if expired
 */
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    try {
      // Try to auto-refresh if token is expired
      await authService.autoRefreshIfNeeded();
    } catch (error) {
      console.warn('[API Client] Auto-refresh failed:', error);
      // Continue with the request even if refresh fails
    }

    // Attach access token if available
    const token = authService.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    console.error('[API Client] Request interceptor error:', error);
    return Promise.reject(error);
  }
);

/**
 * Response Interceptor
 * - Handle 401 Unauthorized errors
 * - Attempt token refresh and retry request
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        console.log('[API Client] 401 detected, attempting token refresh...');

        // Try to refresh token
        const newToken = await authService.refresh();

        // Update request header with new token
        originalRequest.headers.Authorization = `Bearer ${newToken}`;

        // Retry original request
        return apiClient.request(originalRequest);

      } catch (refreshError) {
        console.error('[API Client] Token refresh failed:', refreshError);

        // Clear tokens and redirect to login
        authService.clearTokens();

        // Redirect to home page (which will show login modal)
        if (typeof window !== 'undefined') {
          window.location.href = '/?login=required';
        }

        return Promise.reject(refreshError);
      }
    }

    // Backend 에러 메시지 파싱
    const backendError = (error.response?.data as ApiErrorResponse | undefined)?.error;
    if (backendError) {
      // 에러 객체에 백엔드 정보 추가
      error.message = backendError.message;
      (error as any).code = backendError.code;
      (error as any).errorDetails = backendError;
    }

    console.error('[API Client] Response error:', {
      status: error.response?.status,
      code: backendError?.code,
      message: backendError?.message || error.message,
    });

    return Promise.reject(error);
  }
);

export default apiClient;
