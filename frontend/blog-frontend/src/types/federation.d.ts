// blog-frontend/src/types/federation.d.ts

declare module 'portal_shell/authStore' {
  export const useAuthStore: () => {
    isAuthenticated: import('vue').ComputedRef<boolean>;
    user: import('vue').ComputedRef<{ name: string; email: string } | null>;
  }
}

declare module 'portal_shell/themeStore' {
  export const useThemeStore: () => {
    isDark: import('vue').Ref<boolean>;
    toggle: () => void;
    initialize: () => void;
  }
}