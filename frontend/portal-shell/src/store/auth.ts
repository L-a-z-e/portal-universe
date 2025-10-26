import { defineStore } from 'pinia';
import { computed, ref } from "vue";
import type { User } from 'oidc-client-ts';
import { parseJwtPayload } from '../utils/jwt';
import type { PortalUser, UserProfile, UserAuthority } from '../types/user';

export const useAuthStore = defineStore('auth', () => {
  // ==================== State ====================
  const user = ref<PortalUser | null>(null);

  // ==================== Getters ====================

  /**
   * 로그인 여부
   */
  const isAuthenticated = computed(() => user.value !== null);

  /**
   * 사용자 표시 이름
   * 우선순위: nickname > username > name > email
   */
  const displayName = computed(() => {
    if (!user.value) return 'Guest';

    const p = user.value.profile;
    return p.nickname || p.username || p.name || p.email;
  });

  /**
   * 역할 확인
   */
  const hasRole = (role: string): boolean => {
    return user.value?.authority.roles.includes(role) || false;
  };

  /**
   * Admin 여부
   */
  const isAdmin = computed(() => hasRole('ROLE_ADMIN'));

  // ==================== Actions ====================

  /**
   * OIDC User로 Store 설정
   */
  function setUser(oidcUser: User) {
    console.group('🔄 [Auth Store] Setting user');

    try {
      const payload = parseJwtPayload(oidcUser.access_token);
      console.log('JWT Payload:', payload);

      if (!payload) {
        // 오류 처리
        console.error('❌ Invalid JWT payload. Logging out.');
        logout(); // 강제 로그아웃
        return;
      }

      // ✅ UserProfile 생성
      const profile: UserProfile = {
        sub: payload.sub,
        email: payload.sub,  // 현재는 sub가 email
        username: payload.preferred_username || payload.username,
        name: payload.name,
        nickname: payload.nickname,
        picture: payload.picture,
        emailVerified: payload.email_verified,
        locale: payload.locale || 'ko',
        timezone: payload.zoneinfo,
      };

      // ✅ UserAuthority 생성
      const authority: UserAuthority = {
        roles: Array.isArray(payload.roles) ? payload.roles :
          payload.roles ? [payload.roles] : [],
        scopes: Array.isArray(payload.scope) ? payload.scope :
          payload.scope ? payload.scope.split(' ') : [],
      };

      // ✅ PortalUser 생성
      user.value = {
        profile,
        authority,
        preferences: {
          theme: 'light',
          language: profile.locale || 'ko',
          notifications: true,
        },
        _accessToken: oidcUser.access_token,
        _refreshToken: oidcUser.refresh_token,
        _expiresAt: oidcUser.expires_at,
        _issuedAt: Math.floor(Date.now() / 1000),
      };

      console.log('✅ User set successfully');
      console.log('   Display name:', displayName.value);
      console.log('   Roles:', authority.roles);
      console.log('   Scopes:', authority.scopes);
      console.log('   Expires at:', oidcUser.expires_at !== undefined ? new Date(oidcUser.expires_at * 1000).toLocaleString() : 0);
    } catch (error) {
      console.error('❌ Failed to set user:', error);
      user.value = null;
    }

    console.groupEnd();
  }

  /**
   * 로그아웃
   */
  function logout() {
    console.log('👋 [Auth Store] Logging out');
    user.value = null;
  }

  // ==================== Return ====================
  return {
    // State
    user,

    // Getters
    isAuthenticated,
    displayName,
    isAdmin,

    // Methods
    hasRole,

    // Actions
    setUser,
    logout,
  };
});
