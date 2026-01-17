interface ImportMetaEnv {
  readonly VITE_PROFILE: string
  readonly VITE_API_BASE_URL: string;
  readonly VITE_PORTAL_REMOTE_URL: string;
  readonly VITE_SHOPPING_REMOTE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Module Federation 관련 글로벌 타입
declare global {
  interface Window {
    /** Portal Shell에서 임베디드 모드로 실행 중인지 여부 */
    __POWERED_BY_PORTAL_SHELL__?: boolean;
    /** Module Federation 사용 여부 */
    __FEDERATION__?: boolean;
  }
}

// Portal Shell Remote Module 타입
declare module 'portal/themeStore' {
  export interface ThemeStore {
    isDark: boolean;
    setIsDark: (isDark: boolean) => void;
    toggleTheme: () => void;
  }
  export function useThemeStore(): ThemeStore;
}

declare module 'portal/authStore' {
  export interface User {
    id: string;
    email: string;
    name: string;
    roles: string[];
  }
  export interface AuthStore {
    isAuthenticated: boolean;
    user: User | null;
    accessToken: string | null;
    login: () => Promise<void>;
    logout: () => Promise<void>;
  }
  export function useAuthStore(): AuthStore;
}

declare module 'portal/apiClient' {
  import { AxiosInstance } from 'axios';
  export const apiClient: AxiosInstance;
}

export {}