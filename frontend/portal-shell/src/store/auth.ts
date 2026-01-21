// portal-shell/src/store/auth.ts

import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { authService, type UserInfo } from '../services/authService';
import type { PortalUser, UserProfile, UserAuthority } from '../types/user';

export const useAuthStore = defineStore('auth', () => {
  // ==================== State ====================
  const user = ref<PortalUser | null>(null);
  const loading = ref(false);

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
   * Check if user is admin
   */
  const isAdmin = computed(() => hasRole('ROLE_ADMIN'));

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

      console.log('✅ [Auth Store] Login successful');
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
    } catch (error) {
      console.error('❌ [Auth Store] Logout error:', error);
      // Still clear user on error
      user.value = null;
      delete window.__PORTAL_ACCESS_TOKEN__;
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

  // ==================== Return ====================
  return {
    // State
    user,
    loading,

    // Getters
    isAuthenticated,
    displayName,
    isAdmin,

    // Methods
    hasRole,

    // Actions
    login,
    socialLogin,
    logout,
    checkAuth,
    setAuthenticated,
    setUser,
  };
});
