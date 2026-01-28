// portal-shell/src/api/apiClient.ts
import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { authService } from '../services/authService';
import type { ApiErrorResponse } from './types';

const MAX_RATE_LIMIT_RETRIES = 3;
const DEFAULT_RETRY_DELAY_MS = 1000;

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _rateLimitRetryCount?: number;
}

function parseRetryAfter(headers?: Record<string, unknown>): number {
  const retryAfter = headers?.['retry-after'];
  if (!retryAfter) return DEFAULT_RETRY_DELAY_MS;
  const seconds = Number(retryAfter);
  return Number.isFinite(seconds) && seconds > 0
    ? seconds * 1000
    : DEFAULT_RETRY_DELAY_MS;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

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
 * - Handle 429 Too Many Requests with retry (Retry-After header)
 * - Handle 401 Unauthorized with token refresh
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableConfig;

    // Handle 429 Too Many Requests
    if (error.response?.status === 429) {
      const retryCount = originalRequest._rateLimitRetryCount ?? 0;
      if (retryCount < MAX_RATE_LIMIT_RETRIES) {
        originalRequest._rateLimitRetryCount = retryCount + 1;
        const waitMs = parseRetryAfter(error.response.headers as Record<string, unknown>);
        console.warn(
          `[API Client] 429 rate limited, retry ${retryCount + 1}/${MAX_RATE_LIMIT_RETRIES} after ${waitMs}ms`
        );
        await delay(waitMs);
        return apiClient.request(originalRequest);
      }
      console.error('[API Client] 429 rate limit retries exhausted');
    }

    // Handle 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        console.log('[API Client] 401 detected, attempting token refresh...');
        const newToken = await authService.refresh();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient.request(originalRequest);
      } catch (refreshError) {
        console.error('[API Client] Token refresh failed:', refreshError);
        authService.clearTokens();
        if (typeof window !== 'undefined') {
          window.location.href = '/?login=required';
        }
        return Promise.reject(refreshError);
      }
    }

    // Backend 에러 메시지 파싱
    const backendError = (error.response?.data as ApiErrorResponse | undefined)?.error;
    if (backendError) {
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
