// portal-shell/src/store/auth.ts

import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { authService, type UserInfo } from '../services/authService';
import type { PortalUser, UserProfile, UserAuthority } from '../types/user';
import router from '../router';

export const useAuthStore = defineStore('auth', () => {
  // ==================== State ====================
  const user = ref<PortalUser | null>(null);
  const loading = ref(false);
  const showLoginModal = ref(false);
  const redirectPath = ref<string | null>(null);

  // ==================== Getters ====================

  /**
   * Login status
   */
  const isAuthenticated = computed(() => user.value !== null);

  /**
   * Display name
   * Priority: nickname > username > name > email
   */
  const displayName = computed(() => {
    if (!user.value) return 'Guest';

    const p = user.value.profile;
    return p.nickname || p.username || p.name || p.email;
  });

  /**
   * Check if user has a specific role
   */
  const hasRole = (role: string): boolean => {
    return user.value?.authority.roles.includes(role) || false;
  };

  /**
   * Check if user has any of the specified roles
   */
  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  };

  /**
   * Check if user is a system-wide admin (SUPER_ADMIN)
   * 하위 호환: ROLE_ADMIN도 허용
   */
  const isAdmin = computed(() =>
    hasAnyRole(['ROLE_SUPER_ADMIN', 'ROLE_ADMIN'])
  );

  /**
   * Check if user is a service-specific admin
   */
  const isServiceAdmin = (service: string): boolean => {
    const serviceAdminRole = `ROLE_${service.toUpperCase()}_ADMIN`;
    return hasAnyRole([serviceAdminRole, 'ROLE_SUPER_ADMIN']);
  };

  /**
   * Check if user is a seller
   */
  const isSeller = computed(() => hasRole('ROLE_SELLER'));

  /**
   * Get user's membership tier for a service
   */
  const getMembershipTier = (service: string): string => {
    return user.value?.authority.memberships[service] || 'FREE';
  };

  // ==================== Actions ====================

  /**
   * Login with email and password
   */
  async function login(email: string, password: string): Promise<void> {
    loading.value = true;
    try {
      console.log('[Auth Store] Logging in:', email);

      const response = await authService.login(email, password);

      // Extract user info from JWT
      const userInfo = authService.getUserInfo();
      if (userInfo) {
        setUserFromInfo(userInfo, response.accessToken);
      }

      showLoginModal.value = false;
      console.log('✅ [Auth Store] Login successful');

      // Redirect to the originally requested path if any
      if (redirectPath.value) {
        const path = redirectPath.value;
        redirectPath.value = null;
        router.push(path);
      }

      // Notify Remote apps (React Zustand) of auth state change
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } catch (error) {
      console.error('❌ [Auth Store] Login failed:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Social login (redirect)
   */
  function socialLogin(provider: 'google' | 'naver' | 'kakao'): void {
    console.log(`[Auth Store] Redirecting to ${provider} login`);
    authService.socialLogin(provider);
  }

  /**
   * Logout
   */
  async function logout(): Promise<void> {
    loading.value = true;
    try {
      console.log('[Auth Store] Logging out');

      await authService.logout();
      user.value = null;

      // Clear global token
      delete window.__PORTAL_ACCESS_TOKEN__;

      console.log('✅ [Auth Store] Logout successful');

      // Notify Remote apps (React Zustand) of auth state change
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } catch (error) {
      console.error('❌ [Auth Store] Logout error:', error);
      // Still clear user on error
      user.value = null;
      delete window.__PORTAL_ACCESS_TOKEN__;
      window.dispatchEvent(new CustomEvent('portal:auth-changed'));
    } finally {
      loading.value = false;
    }
  }

  /**
   * Check authentication status and restore user if token exists
   */
  async function checkAuth(): Promise<void> {
    console.log('[Auth Store] Checking authentication status');

    try {
      // Try to refresh token if expired
      await authService.autoRefreshIfNeeded();

      const userInfo = authService.getUserInfo();
      if (userInfo && authService.isAuthenticated()) {
        const accessToken = authService.getAccessToken();
        if (accessToken) {
          setUserFromInfo(userInfo, accessToken);
          console.log('✅ [Auth Store] User restored from token');
        }
      } else {
        user.value = null;
        console.log('[Auth Store] No valid token found');
      }
    } catch (error) {
      console.error('❌ [Auth Store] Auth check failed:', error);
      user.value = null;
      authService.clearTokens();
    }
  }

  /**
   * Set user from UserInfo
   */
  function setUserFromInfo(userInfo: UserInfo, accessToken: string): void {
    console.group('🔄 [Auth Store] Setting user from UserInfo');

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

      console.log('✅ User set successfully');
      console.log('   Display name:', displayName.value);
      console.log('   Roles:', authority.roles);
      console.log('   Scopes:', authority.scopes);
    } catch (error) {
      console.error('❌ Failed to set user:', error);
      user.value = null;
    }

    console.groupEnd();
  }

  /**
   * Set authenticated status (for OAuth2 callback)
   */
  function setAuthenticated(value: boolean): void {
    if (!value) {
      user.value = null;
      authService.clearTokens();
    }
  }

  /**
   * Set user from external source (OAuth2 callback)
   */
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

    console.log('✅ [Auth Store] Access token updated after profile/membership change');
  }

  /**
   * Request login modal to be shown (used by navigation guard)
   */
  function requestLogin(path?: string): void {
    redirectPath.value = path ?? null;
    showLoginModal.value = true;
  }

  // ==================== Return ====================
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
