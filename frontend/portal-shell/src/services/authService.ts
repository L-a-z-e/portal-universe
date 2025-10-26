/**
 * @file authService.ts
 * @description oidc-client-ts 라이브러리를 사용하여 OIDC(OpenID Connect) 인증 로직을 처리합니다.
 * UserManager 인스턴스를 생성하고, 로그인/로그아웃 함수를 내보내며, 인증 관련 이벤트를 처리합니다.
 */

import { UserManager, WebStorageStateStore, User } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

// Vite 환경 변수에서 OIDC 설정을 가져옵니다.
const settings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
  response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
  scope: import.meta.env.VITE_OIDC_SCOPE,
  // 사용자 세션 정보를 localStorage에 저장합니다.
  userStore: new WebStorageStateStore({ store: window.localStorage }),

  // --- Silent Renew (자동 토큰 갱신) 설정 ---
  automaticSilentRenew: true, // 토큰 만료가 임박했을 때 자동으로 갱신 시도
  silent_redirect_uri: window.location.origin + '/silent-renew.html', // 자동 갱신을 처리할 숨겨진 페이지
  accessTokenExpiringNotificationTimeInSeconds: 60, // 토큰 만료 60초 전에 갱신 이벤트 발생
};

console.group('🔐 OIDC Configuration');
console.log('Authority:', settings.authority);
console.log('Client ID:', settings.client_id);
console.groupEnd();

// UserManager 인스턴스를 생성합니다. 이 인스턴스가 OIDC 관련 모든 작업을 관리합니다.
const userManager = new UserManager(settings);

// ===============================================
// Public Functions
// ===============================================

/**
 * OIDC 로그인 프로세스를 시작합니다.
 * 인증 서버의 로그인 페이지로 리다이렉트합니다.
 */
export function login() {
  return userManager.signinRedirect();
}

/**
 * OIDC 로그아웃 프로세스를 시작합니다.
 * 로컬 세션을 지우고, 인증 서버에 로그아웃을 요청한 후 post_logout_redirect_uri로 리다이렉트합니다.
 */
export function logout() {
  const authStore = useAuthStore();
  authStore.logout();
  return userManager.signoutRedirect();
}

// ===============================================
// Event Handlers
// oidc-client-ts에서 발생하는 이벤트를 구독하여 상태를 관리합니다.
// ===============================================

/**
 * 사용자 정보가 로드되었을 때(로그인 성공, 자동 갱신 성공 등) 호출됩니다.
 */
userManager.events.addUserLoaded((user: User) => {
  console.log('✅ User loaded', user);
  const authStore = useAuthStore();
  authStore.setUser(user);
});

/**
 * Access Token 만료가 임박했을 때 호출됩니다.
 * automaticSilentRenew가 true이므로, 이 이벤트 직후 자동 갱신이 시도됩니다.
 */
userManager.events.addAccessTokenExpiring(() => {
  console.log('⏰ Token expiring soon, auto-renewing...');
});

/**
 * Access Token이 만료되었을 때 호출됩니다.
 * 자동 갱신에 실패한 경우 등에 해당하며, 이 경우 사용자를 로그아웃 처리합니다.
 */
userManager.events.addAccessTokenExpired(() => {
  console.log('❌ Token expired, logging out.');
  const authStore = useAuthStore();
  authStore.logout();
});

/**
 * 사용자가 다른 탭 등에서 로그아웃했을 때 호출됩니다.
 */
userManager.events.addUserSignedOut(() => {
  console.log('👋 User signed out from another tab.');
  const authStore = useAuthStore();
  authStore.logout();
});

/**
 * 자동 토큰 갱신(Silent Renew)에 실패했을 때 호출됩니다.
 */
userManager.events.addSilentRenewError((error) => {
  console.error('❌ Silent renew failed:', error);
});

export default userManager;
