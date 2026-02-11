// drive-frontend/src/types/federation.d.ts

/**
 * portal/api 모듈 - API 관련 exports
 */
declare module 'portal/api' {
  import type { AxiosInstance, AxiosResponse } from 'axios';

  // API Client
  export const apiClient: AxiosInstance;

  // Types
  export interface FieldError {
    field: string;
    message: string;
    rejectedValue?: unknown;
  }

  export interface ErrorDetails {
    code: string;
    message: string;
    timestamp?: string;
    path?: string;
    details?: FieldError[];
  }

  export interface ApiResponse<T> {
    success: true;
    data: T;
    error: null;
  }

  export interface ApiErrorResponse {
    success: false;
    data: null;
    error: ErrorDetails;
  }

  // Utilities
  export function getData<T>(response: AxiosResponse<ApiResponse<T>>): T;
  export function getErrorDetails(error: unknown): ErrorDetails | null;
  export function getErrorMessage(error: unknown): string;
  export function getErrorCode(error: unknown): string | null;
}

/**
 * portal/stores 모듈 - Store 관련 exports
 */
declare module 'portal/stores' {
  import type { ComputedRef, Ref } from 'vue';

  // Auth Store
  export const useAuthStore: () => {
    isAuthenticated: ComputedRef<boolean>;
    user: ComputedRef<{
      profile?: {
        sub?: string;
        email?: string;
        name?: string;
        nickname?: string;
        picture?: string;
      };
      authority?: {
        roles?: string[];
        memberships?: Record<string, string>;
      };
    } | null>;
    displayName: ComputedRef<string>;
    isAdmin: ComputedRef<boolean>;
    isSeller: ComputedRef<boolean>;
    hasRole: (role: string) => boolean;
    hasAnyRole: (roles: string[]) => boolean;
    isServiceAdmin: (service: string) => boolean;
    getMembershipTier: (service: string) => string;
  };

  // Theme Store
  export type ThemeMode = 'dark' | 'light' | 'system';

  export const useThemeStore: () => {
    isDark: Ref<boolean>;
    mode: Ref<ThemeMode>;
    toggle: () => void;
    setMode: (mode: ThemeMode) => void;
    applyTheme: () => void;
    initialize: () => void;
  };
}
