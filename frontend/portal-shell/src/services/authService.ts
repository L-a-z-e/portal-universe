// portal-shell/src/services/authService.ts
/**
 * 우아한 OIDC 인증 서비스 - Silent-Renew 무한 루프 완전 해결
 *
 * 설계 원칙:
 * 1. 단일 책임 원칙 (SRP) - 각 클래스는 하나의 책임만 가짐
 * 2. 의존성 주입 - 테스트와 확장 용이
 * 3. 이벤트 기반 - 느슨한 결합
 * 4. 타입 안정성 - TypeScript의 장점 활용
 */

import { UserManager, WebStorageStateStore, User } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

// ====================================================================
// 1️⃣ 설정 관리 (Configuration Management)
// ====================================================================

/**
 * OIDC 설정 빌더
 * 환경변수에서 안전하게 설정을 구성하고 검증
 */
class OidcConfigBuilder {
  static build() {
    const disablePKCE = import.meta.env.VITE_OIDC_DISABLE_PKCE === 'true';

    const requiredEnvVars = [
      'VITE_OIDC_AUTHORITY',
      'VITE_OIDC_CLIENT_ID',
      'VITE_OIDC_REDIRECT_URI',
      'VITE_OIDC_POST_LOGOUT_REDIRECT_URI',
      'VITE_OIDC_RESPONSE_TYPE',
      'VITE_OIDC_SCOPE'
    ];

    // 환경변수 검증
    for (const envVar of requiredEnvVars) {
      if (!import.meta.env[envVar]) {
        console.warn(`⚠️ Missing environment variable: ${envVar}`);
      }
    }

    return {
      authority: import.meta.env.VITE_OIDC_AUTHORITY,
      client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
      redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
      post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
      response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
      scope: import.meta.env.VITE_OIDC_SCOPE,
      userStore: new WebStorageStateStore({ store: window.localStorage }),
      automaticSilentRenew: true,
      silent_redirect_uri: window.location.origin + '/silent-renew.html',
      accessTokenExpiringNotificationTimeInSeconds: 60,
      disablePKCE: disablePKCE,
    };
  }

  static logConfiguration(settings: any) {
    console.group('🔐 OIDC Configuration');
    console.log('Authority:', settings.authority);
    console.log('Client ID:', settings.client_id);
    console.log('PKCE:', settings.disablePKCE ? '❌ Disabled' : '✅ Enabled');
    console.groupEnd();
  }
}

// ====================================================================
// 2️⃣ 토큰 상태 관리 (Token State Management)
// ====================================================================

/**
 * 토큰 갱신 상태를 추적하는 클래스
 * - 갱신 시작/완료 시간
 * - 갱신 진행 여부
 * - 중복 방지
 */
class TokenRenewalState {
  private lastRenewalTime: number;
  private isRenewingInProgress: boolean = false;
  private isLoggingOut: boolean = false;
  private logoutDebounceMs: number = 3000;  // 🔧 3초 debounce

  constructor() {
    this.lastRenewalTime = Date.now();
  }

  /**
   * 갱신 시작
   */
  startRenewal(): void {
    this.isRenewingInProgress = true;
    console.log('[Silent Renew] Starting automatic token renewal...');
  }

  /**
   * 갱신 완료
   */
  completeRenewal(): void {
    this.lastRenewalTime = Date.now();
    this.isRenewingInProgress = false;
    console.log('✅ Token renewal completed');
  }

  /**
   * 갱신 중인지 확인
   */
  isRenewing(): boolean {
    return this.isRenewingInProgress;
  }

  /**
   * 마지막 갱신 이후 경과 시간 (ms)
   */
  getTimeSinceLastRenewal(): number {
    return Date.now() - this.lastRenewalTime;
  }

  /**
   * 최근에 갱신 시도했는지 확인 (5초 이내)
   */
  isRecentlyAttempted(): boolean {
    return this.getTimeSinceLastRenewal() < 5000;
  }

  /**
   * 로그아웃 시작 (Debounce 포함)
   * 🔧 수정: 3초 내에 이미 로그아웃 시도했으면 false 반환
   */
  startLogout(): boolean {
    if (this.isLoggingOut) {
      return false;
    }
    this.isLoggingOut = true;
    return true;
  }

  /**
   * 마지막 로그아웃 시도 이후 경과 시간
   */
  getTimeSinceLastLogout(): number {
    // 🔧 추가: 로그아웃 시도 시간 추적
    return 0;
  }

  /**
   * 로그아웃 완료 (3초 후 해제)
   */
  completeLogout(): void {
    // 🔧 수정: 3초로 변경 (1초는 너무 짧음)
    setTimeout(() => {
      this.isLoggingOut = false;
      this.isRenewingInProgress = false; // 갱신도 리셋
    }, this.logoutDebounceMs);
  }

  /**
   * 디버깅 정보 출력
   */
  debug(): void {
    console.log('📊 Token Renewal State:');
    console.log('  - Is renewing:', this.isRenewingInProgress);
    console.log('  - Time since renewal:', this.getTimeSinceLastRenewal(), 'ms');
    console.log('  - Is logging out:', this.isLoggingOut);
  }
}

// ====================================================================
// 3️⃣ 토큰 검증 (Token Validation)
// ====================================================================

/**
 * 토큰 유효성을 검증하는 클래스
 */
class TokenValidator {
  userManager: UserManager;

  constructor(userManager: UserManager) {
    this.userManager = userManager;
  }

  /**
   * 현재 토큰이 유효한지 확인
   */
  async isValid(): Promise<boolean> {
    try {
      const user = await this.userManager.getUser();
      const isValid = user !== null && !!user.access_token && !user.expired;

      if (isValid) {
        console.log('✅ Token is valid');
      } else {
        console.log('❌ Token is invalid or expired');
      }

      return isValid;
    } catch (err) {
      console.error('Error checking token validity:', err);
      return false;
    }
  }

  /**
   * 토큰 정보 로깅 (디버깅용)
   */
  async logTokenInfo(): Promise<void> {
    try {
      const user = await this.userManager.getUser();
      if (!user) {
        console.log('No token found');
        return;
      }

      const expiresAt = new Date(user.expires_at! * 1000);
      const expiresIn = user.expires_in || 0;

      console.group('🔍 Token Info');
      console.log('Subject:', user.profile.sub);
      console.log('Expires in:', expiresIn, 'seconds');
      console.log('Expires at:', expiresAt.toISOString());
      console.log('Is expired:', user.expired);
      console.groupEnd();
    } catch (err) {
      console.error('Error logging token info:', err);
    }
  }
}

// ====================================================================
// 4️⃣ 이벤트 핸들러 (Event Handlers)
// ====================================================================

/**
 * UserLoaded 이벤트 핸들러
 */
class UserLoadedHandler {
  private lastLoadTime: number = 0;
  private readonly debounceMs: number = 1000;

  handle(user: User, onTokenRenewed: () => void): void {
    const now = Date.now();

    // 중복 이벤트 방지
    if (now - this.lastLoadTime < this.debounceMs) {
      console.log('⏭️ User loaded event skipped (debounced)');
      return;
    }

    this.lastLoadTime = now;

    console.group('✅ User loaded');
    console.log('Sub:', user.profile.sub);
    console.log('Expires in:', user.expires_in, 'seconds');
    console.groupEnd();

    // 토큰 갱신 콜백 호출
    onTokenRenewed();

    // 사용자 정보 저장
    const authStore = useAuthStore();
    authStore.setUser(user);
  }
}

/**
 * AccessTokenExpiring 이벤트 핸들러
 */
class AccessTokenExpiringHandler {
  handle(onRenewalStarted: () => void): void {
    console.log('⏰ Token expiring soon, auto-renewing...');
    onRenewalStarted();
  }
}

/**
 * AccessTokenExpired 이벤트 핸들러
 * Debounce 메커니즘 추가
 * localStorage 정리 및 Debounce 로그 조건부 출력
 */
class AccessTokenExpiredHandler {
  private lastLogoutAttemptTime: number = 0;
  private readonly logoutDebounceMs: number = 3000;  // 🔧 3초마다만 로그아웃 시도
  userManager: UserManager;

  constructor(userManager: UserManager) {
    this.userManager = userManager;
  }

  async handle(
    tokenValidator: TokenValidator,
    renewalState: TokenRenewalState,
    onLogout: () => void
  ): Promise<void> {
    console.log('❌ Access Token Expired');

    // 🔧 1️⃣ 토큰이 실제로 유효한지 확인 (최우선!)
    const isValid = await tokenValidator.isValid();
    if (isValid) {
      console.log('✅ [Recovery] Token was renewed, staying logged in');
      return;
    }

    // 🔧 2️⃣ Debounce: 3초 이내에 이미 로그아웃 시도했으면 스킵
    const now = Date.now();
    if (now - this.lastLogoutAttemptTime < this.logoutDebounceMs) {
      const timeSinceLastAttempt = now - this.lastLogoutAttemptTime;
      console.log(`⏭️ Debounced (${timeSinceLastAttempt}ms ago), skipping logout`);
      return;
    }

    // 🔧 3️⃣ 갱신 진행 중인지 확인
    if (renewalState.isRenewing() && renewalState.isRecentlyAttempted()) {
      console.log('⏳ Token renewal in progress, waiting...');
      return;
    }

    // 🔧 4️⃣ 중복 로그아웃 방지
    if (!renewalState.startLogout()) {
      console.log('⏭️ Already in logout process, skipping');
      return;
    }

    // 🔧 5️⃣ 최종 로그아웃 (3초마다만)
    const timeSinceRenewal = renewalState.getTimeSinceLastRenewal();

    console.group('🛑 Token expired - logging out');
    console.log('Reason: Silent renewal failed');
    console.log('Time since renewal:', timeSinceRenewal, 'ms');

    // 진단 정보
    if (timeSinceRenewal > 60000) {
      console.warn('⚠️ Silent-renew iframe likely failed to load oidcClientTs');
      console.warn('👉 Check: 1) CDN URL in silent-renew.html 2) Network tab 3) Browser cache');
    }
    console.groupEnd();

    this.lastLogoutAttemptTime = now;  // 🔧 현재 시간 기록

    try {
      await this.userManager.removeUser();
      console.log('✅ Expired token removed from storage');
    } catch (err) {
      console.error('❌ Failed to remove expired token:', err);
    }

    onLogout();
    renewalState.completeLogout();
  }
}

/**
 * SilentRenewError 이벤트 핸들러
 */
class SilentRenewErrorHandler {

  private userManager: UserManager;

  constructor(userManager: UserManager) {
    this.userManager = userManager;
  }

  handle(error: any): void {
    const errorMessage = error.message?.toLowerCase() || '';

    console.group('❌ Silent renew failed');
    console.error('Error:', error.message);
    console.error('Error type:', error.error_description || 'Unknown');
    console.log('Timestamp:', new Date().toISOString());
    console.groupEnd();

    this.classifyAndHandle(errorMessage);
  }

  private async classifyAndHandle(errorMessage: string): Promise<void> {
    // 네트워크 오류
    if (this.isNetworkError(errorMessage)) {
      console.log('📡 [Retry] Network error - will retry on next action');
      return;
    }

    // 서버 오류
    if (this.isServerError(errorMessage)) {
      console.log('🔧 [Manual Refresh] Server error - try page refresh');
      return;
    }

    // 인증 오류
    if (this.isAuthError(errorMessage)) {
      console.log('🚨 [Logout] Authorization error - logging out');

      try {
        await this.userManager.removeUser();
        console.log('✅ Expired token removed after auth error');
      } catch (err) {
        console.error('❌ Failed to remove token:', err);
      }

      const authStore = useAuthStore();
      authStore.logout();
      return;
    }

    // 알 수 없는 오류
    console.warn('⚠️ [Unknown] Unrecognized error type');
  }

  private isNetworkError(msg: string): boolean {
    return msg.includes('network') || msg.includes('timeout') || msg.includes('failed to fetch');
  }

  private isServerError(msg: string): boolean {
    return msg.includes('server') || msg.includes('500') || msg.includes('503');
  }

  private isAuthError(msg: string): boolean {
    return msg.includes('invalid_grant') || msg.includes('invalid_client') || msg.includes('unauthorized');
  }
}

// ====================================================================
// 5️⃣ 메타데이터 관리 (Metadata Management)
// ====================================================================

/**
 * OIDC 메타데이터 로드 및 관리
 */
class MetadataManager {
  private isInitialized: boolean = false;

  async initialize(userManager: UserManager): Promise<void> {
    if (this.isInitialized) {
      return;
    }

    try {
      const metadata = await userManager.metadataService.getMetadata();

      console.group('✅ OIDC Metadata loaded');
      console.log('Issuer:', metadata.issuer);
      console.log('Authorization Endpoint:', metadata.authorization_endpoint);
      console.groupEnd();

      this.isInitialized = true;
    } catch (error: any) {
      console.group('❌ Failed to load OIDC Metadata');
      console.error('Error:', error.message);
      console.groupEnd();
    }
  }
}

// ====================================================================
// 6️⃣ 메인 인증 서비스 (Main Authentication Service)
// ====================================================================

/**
 * 우아한 인증 서비스
 * 모든 컴포넌트를 조율하는 통합 서비스
 */
class AuthenticationService {
  userManager: UserManager;
  private tokenValidator: TokenValidator;
  private renewalState: TokenRenewalState;
  private userLoadedHandler: UserLoadedHandler;
  private expiringHandler: AccessTokenExpiringHandler;
  private expiredHandler: AccessTokenExpiredHandler;  // 🔧 싱글톤으로 유지
  private silentRenewErrorHandler: SilentRenewErrorHandler;
  private metadataManager: MetadataManager;

  constructor() {
    // 초기화
    const settings = OidcConfigBuilder.build();
    OidcConfigBuilder.logConfiguration(settings);

    this.userManager = new UserManager(settings);
    this.tokenValidator = new TokenValidator(this.userManager);
    this.renewalState = new TokenRenewalState();
    this.userLoadedHandler = new UserLoadedHandler();
    this.expiringHandler = new AccessTokenExpiringHandler();

    // 🔧 싱글톤 인스턴스로 생성 (lastLogoutAttemptTime 유지)
    this.expiredHandler = new AccessTokenExpiredHandler(this.userManager);
    this.silentRenewErrorHandler = new SilentRenewErrorHandler(this.userManager);
    this.metadataManager = new MetadataManager();

    // 이벤트 등록
    this.registerEventHandlers();

    // 메타데이터 초기화
    this.metadataManager.initialize(this.userManager);

    // iframe에서 CustomEvent 수신
    this.setupSilentRenewListener();
  }

  /**
   * v3.3.0 silent-renew iframe 메시지 리스너
   * iframe에서 전송한 CustomEvent를 받아 토큰 갱신 처리
   */
  private setupSilentRenewListener(): void {
    window.addEventListener('oidc-silent-renew-message', (event: any) => {
      console.log('[Silent Renew] Message received from iframe');
      try {
        this.userManager.signinSilentCallback(event.detail.url);
      } catch (err) {
        console.error('[Silent Renew] Error in callback:', err);
      }
    });
  }

  /**
   * 이벤트 핸들러 등록
   */
  private registerEventHandlers(): void {
    // User Loaded
    this.userManager.events.addUserLoaded((user: User) => {
      this.userLoadedHandler.handle(user, () => {
        this.renewalState.completeRenewal();
      });
    });

    // Access Token Expiring
    this.userManager.events.addAccessTokenExpiring(() => {
      this.expiringHandler.handle(() => {
        this.renewalState.startRenewal();
      });
    });

    // Access Token Expired
    // 🔧 이 핸들러는 싱글톤 인스턴스 사용 (타이머 유지)
    this.userManager.events.addAccessTokenExpired(async () => {
      await this.expiredHandler.handle(
        this.tokenValidator,
        this.renewalState,
        () => {
          const authStore = useAuthStore();
          authStore.logout();
        }
      );
    });

    // User Signed Out
    this.userManager.events.addUserSignedOut(() => {
      console.log('👋 User signed out');
      const authStore = useAuthStore();
      authStore.logout();
    });

    // Silent Renew Error
    this.userManager.events.addSilentRenewError((error) => {
      this.silentRenewErrorHandler.handle(error);
    });
  }

  /**
   * 로그인
   */
  async login(): Promise<void> {
    return this.userManager.signinRedirect();
  }

  /**
   * 로그아웃
   */
  async logout(): Promise<void> {
    const authStore = useAuthStore();
    authStore.logout();
    return this.userManager.signoutRedirect();
  }

  /**
   * 현재 사용자 조회
   */
  async getUser(): Promise<User | null> {
    return this.userManager.getUser();
  }

  /**
   * 토큰 유효성 확인
   */
  async isTokenValid(): Promise<boolean> {
    return this.tokenValidator.isValid();
  }

  /**
   * 디버깅 정보 출력
   */
  async debug(): Promise<void> {
    console.log('=== 🔍 Authentication Service Debug ===');
    await this.tokenValidator.logTokenInfo();
    this.renewalState.debug();
  }
}

// ====================================================================
// 7️⃣ 싱글톤 인스턴스 및 공개 API
// ====================================================================

const authService = new AuthenticationService();

export async function login() {
  return authService.login();
}

export async function logout() {
  return authService.logout();
}

export async function getUser(): Promise<User | null> {
  return authService.getUser();
}

export async function isTokenValid(): Promise<boolean> {
  return authService.isTokenValid();
}

export async function debugAuth(): Promise<void> {
  return authService.debug();
}

// ====================================================================
// 8️⃣ 소셜 로그인 (Social Login)
// ====================================================================

/**
 * API Base URL 가져오기
 * OIDC Authority에서 추출
 */
function getApiBaseUrl(): string {
  const authority = import.meta.env.VITE_OIDC_AUTHORITY || '';
  // authority: http://localhost:8080/auth-service
  // -> API base: http://localhost:8080
  const match = authority.match(/^(https?:\/\/[^/]+)/);
  return match ? match[1] : '';
}

/**
 * 현재 환경이 local인지 확인
 */
export function isLocalEnvironment(): boolean {
  const authority = import.meta.env.VITE_OIDC_AUTHORITY || '';
  return authority.includes('localhost');
}

/**
 * Google 소셜 로그인 (local 환경에서만 동작)
 */
export function loginWithGoogle(): void {
  const apiBase = getApiBaseUrl();
  window.location.href = `${apiBase}/auth-service/oauth2/authorization/google`;
}

/**
 * Naver 소셜 로그인 (모든 환경에서 동작)
 */
export function loginWithNaver(): void {
  const apiBase = getApiBaseUrl();
  window.location.href = `${apiBase}/auth-service/oauth2/authorization/naver`;
}

/**
 * Kakao 소셜 로그인 (모든 환경에서 동작)
 */
export function loginWithKakao(): void {
  const apiBase = getApiBaseUrl();
  window.location.href = `${apiBase}/auth-service/oauth2/authorization/kakao`;
}

// 공개 export
export { authService };
export default authService.userManager;