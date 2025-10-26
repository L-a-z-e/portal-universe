/**
 * 사용자 기본 정보
 */
export interface UserProfile {
  sub: string;                    // 사용자 ID (email)
  email: string;                  // 이메일
  username?: string;              // 사용자명
  name?: string;                  // 전체 이름
  nickname?: string;              // 닉네임
  picture?: string;               // 프로필 이미지 URL
  phone?: string;                 // 전화번호
  emailVerified?: boolean;        // 이메일 인증 여부
  locale?: string;                // 언어 (ko, en)
  timezone?: string;              // 타임존
}

/**
 * 사용자 권한 정보
 */
export interface UserAuthority {
  roles: string[];                // 역할 (ROLE_ADMIN, ROLE_USER)
  scopes: string[];               // OAuth2 Scope (read, write)
}

/**
 * 사용자 설정
 */
export interface UserPreferences {
  theme: 'light' | 'dark';        // 테마
  language: string;               // 언어
  notifications: boolean;         // 알림 수신
}

/**
 * Portal Universe 사용자
 */
export interface PortalUser {
  profile: UserProfile;
  authority: UserAuthority;
  preferences: UserPreferences;

  // 토큰 정보 (내부 관리용)
  _accessToken: string;
  _refreshToken?: string;
  _expiresAt?: number;
  _issuedAt: number;
}
