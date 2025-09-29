import { defineStore } from 'pinia';
import {computed, ref} from "vue";
import { parseJwtPayload } from '../utils/jwt';

export const useAuthStore = defineStore('auth', () => {
  // === State ===
  const accessToken = ref<string | null>(null);
  const user = ref<{name: string; email: string} | null>(null);

  // === Getters ===
  const isAuthenticated = computed(() => !!accessToken.value);
  // === Setters ===

  // === Actions ===
  function login(token: string | null | undefined) {
    const payload = token ? parseJwtPayload(token) : null;

    if (payload && token) {
      accessToken.value = token;
      user.value = {
        name: payload.username || 'Unknown User',
        email: payload.sub
      };
    } else {
      logout();
    }
  }

  function logout() {
    accessToken.value = null;
    user.value = null;
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    login,
    logout,
  };
})