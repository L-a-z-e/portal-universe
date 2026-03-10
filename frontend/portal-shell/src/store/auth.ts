// portal-shell/src/store/auth.ts

import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { authService, type UserInfo } from '../services/authService';
import type { PortalUser, UserProfile, UserAuthority } from '../types/user';
import { ROLES, serviceAdminRole as buildServiceAdminRole } from '../constants/roles';
import router from '../router';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<PortalUser | null>(null);
  const loading = ref(false);
  const showLoginModal = ref(false);
  const redirectPath = ref<string | null>(null);

  const isAuthenticated = computed(() => user.value !== null);

  // Priority: nickname > username > name > email
  const displayName = computed(() => {
    if (!user.value) return 'Guest';

    const p = user.value.profile;
    return p.nickname || p.username || p.name || p.email;
  });

  const hasRole = (role: string): boolean => {
    return user.value?.authority.roles.includes(role) || false;
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  };

  // 하위 호환: ROLE_ADMIN도 허용
  const isAdmin = computed(() =>
    hasAnyRole([ROLES.SUPER_ADMIN, ROLES.ADMIN])
  );

  const isServiceAdmin = (service: string): boolean => {
    return hasAnyRole([buildServiceAdminRole(service.toUpperCase()), ROLES.SUPER_ADMIN]);
  };

  const isSeller = computed(() => hasRole(ROLES.SELLER));

  const getMembershipTier = (service: string): string => {
    return user.value?.authority.memberships[service] || 'FREE';
  };

  async function login(email: string, password: string): Promise<void> {
    loading.value = true;
    try {

      const response = await authService.login(email, password);

      // Extract user info from JWT
      const userInfo = authService.getUserInfo();
      if (userInfo) {
        setUserFromInfo(userInfo, response.accessToken);
      }

      showLoginModal.value = false;

      // Redirect after login: saved path or dashboard
      const path = redirectPath.value || '/dashboard';
      redirectPath.value = null;
      router.push(path);

      // Notify Remote apps (React Zustand) of auth state change
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } catch (error) {
      console.error('[Auth Store] Login failed:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }

  function socialLogin(provider: 'google' | 'naver' | 'kakao'): void {
    authService.socialLogin(provider);
  }

  async function logout(): Promise<void> {
    loading.value = true;
    try {

      await authService.logout();
      user.value = null;

      // Clear global token
      delete window.__PORTAL_ACCESS_TOKEN__;


      // Notify Remote apps (React Zustand) of auth state change
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } catch (error) {
      console.error('[Auth Store] Logout error:', error);
      // Still clear user on error
      user.value = null;
      delete window.__PORTAL_ACCESS_TOKEN__;
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } finally {
      loading.value = false;
    }
  }

  async function checkAuth(): Promise<void> {

    try {
      // Try to refresh token if expired
      await authService.autoRefreshIfNeeded();

      const userInfo = authService.getUserInfo();
      if (userInfo && authService.isAuthenticated()) {
        const accessToken = authService.getAccessToken();
        if (accessToken) {
          setUserFromInfo(userInfo, accessToken);
        }
      } else {
        user.value = null;
      }
    } catch (error) {
      console.error('[Auth Store] Auth check failed:', error);
      user.value = null;
      authService.clearTokens();
    }

    // Remote 앱(React Zustand)에 auth 상태 변경 알림
    window.dispatchEvent(new CustomEvent('portal:auth-changed'));
  }

  function setUserFromInfo(userInfo: UserInfo, accessToken: string): void {
    console.group('[Auth Store] Setting user from UserInfo');

    try {
      // Create UserProfile
      const profile: UserProfile = {
        sub: userInfo.uuid,
        email: userInfo.email,
        username: userInfo.username,
        name: userInfo.name,
        nickname: userInfo.nickname,
        picture: userInfo.picture,
        emailVerified: true, // Assume verified for JWT
        locale: 'ko',
        timezone: undefined,
      };

      // Create UserAuthority
      const authority: UserAuthority = {
        roles: userInfo.roles,
        scopes: userInfo.scopes,
        memberships: userInfo.memberships || {},
      };

      // Create PortalUser
      user.value = {
        profile,
        authority,
        preferences: {
          theme: 'light',
          language: 'ko',
          notifications: true,
        },
        _accessToken: accessToken,
        _refreshToken: undefined, // Refresh token is stored in authService
        _expiresAt: undefined,
        _issuedAt: Math.floor(Date.now() / 1000),
      };

      // Set global token for remote apps
      window.__PORTAL_ACCESS_TOKEN__ = accessToken;

    } catch (error) {
      console.error('[Auth Store] Failed to set user:', error);
      user.value = null;
    }

    console.groupEnd();
  }

  function setAuthenticated(value: boolean): void {
    if (!value) {
      user.value = null;
      authService.clearTokens();
    }
  }

  function setUser(userInfo: UserInfo | null): void {
    if (!userInfo) {
      user.value = null;
      return;
    }

    const accessToken = authService.getAccessToken();
    if (accessToken) {
      setUserFromInfo(userInfo, accessToken);
    }
  }

  /**
   * Update access token after profile/membership change.
   * Called when backend returns a new token in the API response.
   */
  function updateAccessToken(newAccessToken: string): void {
    const userInfo = (() => {
      // Temporarily set new token to extract user info
      const prevToken = authService.getAccessToken();
      authService.setAccessTokenOnly(newAccessToken);
      const info = authService.getUserInfo();
      if (!info && prevToken) {
        // Rollback on failure
        authService.setAccessTokenOnly(prevToken);
      }
      return info;
    })();

    if (userInfo) {
      setUserFromInfo(userInfo, newAccessToken);
    }

  }

  function requestLogin(path?: string): void {
    redirectPath.value = path ?? null;
    showLoginModal.value = true;
  }

  return {
    // State
    user,
    loading,
    showLoginModal,

    // Getters
    isAuthenticated,
    displayName,
    isAdmin,
    isSeller,

    // Methods
    hasRole,
    hasAnyRole,
    isServiceAdmin,
    getMembershipTier,

    // Actions
    login,
    socialLogin,
    logout,
    checkAuth,
    setAuthenticated,
    setUser,
    updateAccessToken,
    requestLogin,
  };
});
