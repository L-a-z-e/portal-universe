import { defineStore } from 'pinia';
import { computed, ref } from "vue";
import type { User } from 'oidc-client-ts';
import { parseJwtPayload } from '../utils/jwt';
import type { PortalUser, UserProfile, UserAuthority } from '../types/user';

/**
 * Pinia 스토어: 인증(Authentication) 상태를 전역적으로 관리합니다.
 * 로그인한 사용자 정보, 역할, 인증 여부 등을 저장하고 관련 액션을 제공합니다.
 */
export const useAuthStore = defineStore('auth', () => {
  // ===============================================
  // State: 스토어의 상태 (반응형 데이터)
  // ===============================================
  const user = ref<PortalUser | null>(null);

  // ===============================================
  // Getters: 상태를 기반으로 한 계산된 속성
  // ===============================================

  /**
   * 현재 로그인 되어 있는지 여부를 반환합니다.
   */
  const isAuthenticated = computed(() => user.value !== null);

  /**
   * UI에 표시할 사용자 이름을 반환합니다.
   * 우선순위: nickname > username > name > email
   */
  const displayName = computed(() => {
    if (!user.value) return 'Guest';
    const p = user.value.profile;
    return p.nickname || p.username || p.name || p.email;
  });

  /**
   * 사용자가 특정 역할을 가지고 있는지 확인합니다.
   * @param role 확인할 역할 이름 (예: 'ROLE_ADMIN')
   */
  const hasRole = (role: string): boolean => {
    return user.value?.authority.roles.includes(role) || false;
  };

  /**
   * 사용자가 관리자(Admin)인지 여부를 반환합니다.
   */
  const isAdmin = computed(() => hasRole('ROLE_ADMIN'));

  // ===============================================
  // Actions: 상태를 변경하는 메서드
  // ===============================================

  /**
   * oidc-client-ts의 User 객체를 받아 스토어의 상태를 설정합니다.
   * Access Token의 페이로드를 파싱하여 애플리케이션에서 사용하기 쉬운 PortalUser 형태로 가공합니다.
   * @param oidcUser oidc-client-ts로부터 받은 User 객체
   */
  function setUser(oidcUser: User) {
    console.group('🔄 [Auth Store] Setting user');
    try {
      const payload = parseJwtPayload(oidcUser.access_token);
      if (!payload) {
        throw new Error('Invalid JWT payload');
      }

      // JWT 페이로드로부터 UserProfile 객체 생성
      const profile: UserProfile = {
        sub: payload.sub,
        email: payload.sub, // 현재는 sub를 email로 사용
        username: payload.preferred_username || payload.username,
        name: payload.name,
        nickname: payload.nickname,
        picture: payload.picture,
        emailVerified: payload.email_verified,
        locale: payload.locale || 'ko',
        timezone: payload.zoneinfo,
      };

      // JWT 페이로드로부터 UserAuthority 객체 생성
      const authority: UserAuthority = {
        roles: Array.isArray(payload.roles) ? payload.roles : (payload.roles ? [payload.roles] : []),
        scopes: Array.isArray(payload.scope) ? payload.scope : (payload.scope ? payload.scope.split(' ') : []),
      };

      // 최종적으로 애플리케이션에서 사용할 PortalUser 객체 생성
      user.value = {
        profile,
        authority,
        preferences: {
          theme: 'light', // 기본값
          language: profile.locale || 'ko',
          notifications: true, // 기본값
        },
        _accessToken: oidcUser.access_token,
        _refreshToken: oidcUser.refresh_token,
        _expiresAt: oidcUser.expires_at,
        _issuedAt: Math.floor(Date.now() / 1000),
      };

      console.log('✅ User set successfully');

    } catch (error) {
      console.error('❌ Failed to set user:', error);
      user.value = null; // 에러 발생 시 사용자 정보 초기화
    } finally {
      console.groupEnd();
    }
  }

  /**
   * 사용자 상태를 초기화하여 로그아웃 처리합니다.
   */
  function logout() {
    console.log('👋 [Auth Store] Logging out');
    user.value = null;
  }

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