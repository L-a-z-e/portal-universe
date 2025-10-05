declare module 'portal_shell/authStore' {
  export const useAuthStore: () => {
    isAuthenticated: import('vue').ComputedRef<boolean>;
    user: import('vue').ComputedRef<{ name: string; email: string } | null>;
  }
}