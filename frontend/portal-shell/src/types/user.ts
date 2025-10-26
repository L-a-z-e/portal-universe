/**
 * @file user.ts
 * @description 애플리케이션 전체에서 사용되는 사용자 관련 타입들을 정의합니다.
 */

/**
 * OIDC 토큰의 페이로드로부터 파싱된 기본적인 사용자 프로필 정보입니다.
 */
export interface UserProfile {
  sub: string;                    // 사용자 ID (일반적으로 email과 동일)
  email: string;                  // 이메일
  username?: string;              // 사용자명
  name?: string;                  // 전체 이름
  nickname?: string;              // 닉네임
  picture?: string;               // 프로필 이미지 URL
  phone?: string;                 // 전화번호
  emailVerified?: boolean;        // 이메일 인증 여부
  locale?: string;                // 언어 설정 (예: 'ko', 'en')
  timezone?: string;              // 타임존
}

/**
 * 사용자의 권한과 관련된 정보입니다.
 */
export interface UserAuthority {
  roles: string[];                // 역할 목록 (예: ['ROLE_ADMIN', 'ROLE_USER'])
  scopes: string[];               // 허용된 OAuth2 스코프 목록 (예: ['read', 'write'])
}

/**
 * 사용자의 개인화 설정 정보입니다.
 */
export interface UserPreferences {
  theme: 'light' | 'dark';        // 테마 설정
  language: string;               // 언어 설정
  notifications: boolean;         // 알림 수신 여부
}

/**
 * 애플리케이션 내부에서 사용되는 최종 사용자 객체 타입입니다.
 * 프로필, 권한, 설정 및 내부 관리용 토큰 정보를 포함합니다.
 */
export interface PortalUser {
  profile: UserProfile;
  authority: UserAuthority;
  preferences: UserPreferences;

  // --- 내부 관리용 정보 ---
  _accessToken: string;   // JWT Access Token
  _refreshToken?: string;  // Refresh Token
  _expiresAt?: number;     // Access Token 만료 시간 (Unix Timestamp)
  _issuedAt: number;      // Access Token 발급 시간 (Unix Timestamp)
}