// blog-frontend/src/api/index.ts

import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '@/types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Axios 인스턴스 생성
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 요청 인터셉터: OIDC 토큰 자동 추가
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const oidcStorage = localStorage.getItem(
      'oidc.user:http://localhost:8080/auth-service:portal-client'
    );

    if (oidcStorage) {
      try {
        const user = JSON.parse(oidcStorage);
        const token = user.access_token;

        if (token) {
          console.log('[API] Adding Authorization header');
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (error) {
        console.error('[API] Failed to parse OIDC storage:', error);
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * 응답 인터셉터: 에러 처리
 */
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError<ErrorResponse>) => {
    if (error.response) {
      console.error('[API] Error Response:', {
        status: error.response.status,
        data: error.response.data,
      });
    } else if (error.request) {
      console.error('[API] No response received:', error.request);
    } else {
      console.error('[API] Request setup error:', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;