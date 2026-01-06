// blog-frontend/src/types/federation.d.ts

declare module 'portal/authStore' {
  export const useAuthStore: () => {
    isAuthenticated: import('vue').ComputedRef<boolean>;
    user: import('vue').ComputedRef<{ name: string; email: string } | null>;
  }
}

declare module 'portal/themeStore' {
  export const useThemeStore: () => {
    isDark: import('vue').Ref<boolean>;
    toggle: () => void;
    initialize: () => void;
  }
}

declare module 'portal/apiClient' {
  import type { AxiosInstance } from 'axios';
  const apiClient: AxiosInstance;
  export default apiClient;
}