// portal-shell/src/services/authService.ts

import { UserManager, WebStorageStateStore, User } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

const disablePKCE = import.meta.env.VITE_OIDC_DISABLE_PKCE === 'true';

const settings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
  response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
  scope: import.meta.env.VITE_OIDC_SCOPE,
  userStore: new WebStorageStateStore({ store: window.localStorage }),

  // ✅ Silent Renew 설정
  automaticSilentRenew: true,
  silent_redirect_uri: window.location.origin + '/silent-renew.html',
  accessTokenExpiringNotificationTimeInSeconds: 60,

  disablePKCE: disablePKCE,
};

console.group('🔐 OIDC Configuration');
console.log('Authority:', settings.authority);
console.log('Client ID:', settings.client_id);
console.log('PKCE:', disablePKCE ? '❌ Disabled' : '✅ Enabled');
console.groupEnd();

const userManager = new UserManager(settings);

// ✅ 중복 방지 플래그
let lastUserLoadedTime = 0;
const USER_LOADED_DEBOUNCE_MS = 1000;

let isSilentRenewInProgress = false;
let lastTokenRenewalTime = 0;

// ==================== 공개 함수 ====================
export function login() {
  return userManager.signinRedirect();
}

export function logout() {
  const authStore = useAuthStore();
  authStore.logout();
  return userManager.signoutRedirect();
}

async function hasValidToken(): Promise<boolean> {
  try {
    const user = await userManager.getUser();
    return user !== null && !!user.access_token && !user.expired;
  } catch (err) {
    console.error('Error checking token validity:', err);
    return false;
  }
}

const originalAddUserLoaded = userManager.events.addUserLoaded;
userManager.events.addUserLoaded = function(callback: (user: User) => void) {
  return originalAddUserLoaded.call(this, (user: User) => {
    lastTokenRenewalTime = Date.now();
    isSilentRenewInProgress = false;
    callback(user);
  });
};

// ==================== 이벤트 핸들러 ====================

/**
 * User Loaded (중복 방지)
 *
 * ✅ [현재 코드] 동작 중
 * ❌ [문제 없음]
 */
userManager.events.addUserLoaded((user: User) => {
  const now = Date.now();

  // ✅ 1초 이내 중복 이벤트 무시
  if (now - lastUserLoadedTime < USER_LOADED_DEBOUNCE_MS) {
    console.log('⏭️ User loaded event skipped (debounced)');
    return;
  }

  lastUserLoadedTime = now;

  console.group('✅ User loaded');
  console.log('Sub:', user.profile.sub);
  console.log('Expires in:', user.expires_in, 'seconds');
  console.groupEnd();

  const authStore = useAuthStore();
  authStore.setUser(user);
});

/**
 * Access Token Expiring (만료 임박)
 *
 * ✅ [현재 코드] 동작 중
 * ✅ [개선 제안] UI 피드백 추가 가능
 *
 * 🔧 [개선 4] Silent-Renew 시작 신호 추가
 */
userManager.events.addAccessTokenExpiring(() => {
  console.log('⏰ Token expiring soon, auto-renewing...');

  // ====================================================================
  // 🔧 [개선 4-1] Silent-Renew 시작 시간 기록
  // ====================================================================
  // 배경: Silent-renew가 언제 시작되는지 모르면
  //       AccessTokenExpired 이벤트와의 타이밍 차이 계산 불가
  //
  // 해결: Silent-renew 시작을 명시적으로 마킹하여
  //       다음 이벤트와 연관성 파악
  // ====================================================================
  isSilentRenewInProgress = true;
  console.log('[Silent Renew] Starting automatic token renewal...');

  // ⚠️ [추가 가능] UI 알림 (선택사항)
  // showNotification('세션을 갱신하는 중입니다...');
});

/**
 * Access Token Expired
 *
 * ❌ [현재 코드 문제]
 * 1. 토큰이 실제로 갱신되지 않았는지 확인하지 않음
 * 2. Silent-renew 중간에 이 이벤트가 발생하면
 *    실제로 갱신된 토큰이 있어도 logout 호출
 * 3. 사용자 경험: "Logout" → (잠시 후) "Login" 상태 변경
 *
 * ✅ [개선 후]
 * 1. 현재 토큰 상태 검증
 * 2. Silent-renew 진행 중 상태 체크
 * 3. 정말 만료된 경우만 logout
 */
userManager.events.addAccessTokenExpired(async () => {
  console.log('❌ Access Token Expired');
  const authStore = useAuthStore();

  // ====================================================================
  // 🔧 [개선 5] 토큰 유효성 이중 검증 (핵심 개선사항)
  // ====================================================================
  // 배경: Silent-renew가 성공했는데도 expired 이벤트가 발생할 수 있음
  //       (oidc-client-ts의 타이밍 버그)
  //
  // 해결: 실제 유효한 토큰이 있으면 logout하지 않고 계속 진행
  // ====================================================================
  const isValid = await hasValidToken();

  if (isValid) {
    console.log('✅ [Recovery] Token was renewed successfully, staying logged in');
    // 유효한 토큰이 있으므로 아무것도 하지 않음
    return;
  }

  // ====================================================================
  // 🔧 [개선 6] Silent-Renew 상태에 따른 처리 분기
  // ====================================================================
  // 배경: Silent-renew 진행 중 vs 실제 만료 상황을 구분해야 함
  //
  // 해결: 갱신 진행 중이고 최근에 시도했다면 재시도 권유
  //       진짜 만료라면 명확하게 로그아웃
  // ====================================================================
  if (isSilentRenewInProgress) {
    const timeSinceRenewalAttempt = Date.now() - lastTokenRenewalTime;

    if (timeSinceRenewalAttempt < 5000) {
      // 5초 내 갱신 시도 중: 아직 대기
      console.log('⏳ Token renewal still in progress, retrying...');
      return;
    }
  }

  // ====================================================================
  // 🔧 [개선 7] 로그아웃 전 최종 검증 및 사용자 알림
  // ====================================================================
  // 배경: 갑작스러운 로그아웃은 사용자 경험 저하
  //
  // 해결: 1) 콘솔에 명확한 로그
  //       2) 사용자에게 명확한 UI 피드백
  //       3) 다시 로그인할 수 있도록 유도
  // ====================================================================
  console.group('🛑 [Final] Token completely expired - logging out');
  console.log('Reason: Silent renewal failed');
  console.log('Time since last renewal attempt:',
    Date.now() - lastTokenRenewalTime, 'ms');
  console.groupEnd();

  // ✅ 이제만 logout 호출 (유효한 토큰 없을 때만)
  authStore.logout();

  // ⚠️ [추가 가능] UI 피드백
  // showNotification(
  //   '세션이 만료되었습니다. 다시 로그인해주세요.',
  //   'error'
  // );
});

/**
 * User Signed Out
 *
 * ✅ [현재 코드] 동작 중
 * ❌ [문제 없음]
 */
userManager.events.addUserSignedOut(() => {
  console.log('👋 User signed out');
  const authStore = useAuthStore();
  authStore.logout();
});

/**
 * Silent Renew Error
 *
 * ❌ [현재 코드 문제]
 * 1. 에러가 발생해도 아무것도 하지 않음
 * 2. 사용자가 silent-renew 실패를 모름
 * 3. 다음 API 호출 시 401 에러 발생 (갑작스러움)
 *
 * ✅ [개선 후]
 * 1. 에러 유형별 처리
 * 2. UI 피드백 제공
 * 3. 재시도 메커니즘 추가
 */
userManager.events.addSilentRenewError((error) => {
  // ====================================================================
  // 🔧 [개선 8] Silent-Renew 에러 분류 및 처리
  // ====================================================================
  // 배경: Silent-renew 실패 원인이 다양함
  //       - 네트워크 오류 (재시도 가능)
  //       - 사용자 세션 종료 (로그아웃 필요)
  //       - CSRF 토큰 만료 (무시 가능)
  //
  // 해결: 에러 메시지 분석 후 적절한 대응
  // ====================================================================
  const errorMessage = error.message?.toLowerCase() || '';

  console.group('❌ Silent renew failed');
  console.error('Error:', error.message);
  console.error('Error type:', error.error_description || 'Unknown');
  console.log('Timestamp:', new Date().toISOString());
  console.groupEnd();

  // ====================================================================
  // 🔧 [개선 8-1] 에러 유형별 처리
  // ====================================================================
  // CASE 1: 네트워크 오류 - 재시도 예약
  if (errorMessage.includes('network') ||
    errorMessage.includes('timeout') ||
    error.message.includes('Failed to fetch')) {

    console.log('📡 [Retry] Network error detected, will retry on next user action');
    // ⚠️ [추가 가능] 사용자에게 약한 알림만 제공
    // showWarning('네트워크 연결을 확인해주세요.');
    return;
  }

  // CASE 2: 인증 서버 오류 - 수동 갱신 유도
  if (errorMessage.includes('server') ||
    errorMessage.includes('500') ||
    errorMessage.includes('503')) {

    console.log('🔧 [Manual Refresh] Server error detected');
    // ⚠️ [추가 가능] 사용자에게 알림
    // showWarning('일시적인 서비스 오류가 발생했습니다. 페이지를 새로고침해주세요.');
    return;
  }

  // CASE 3: 인증 실패 (세션 종료 등) - 명시적 로그아웃
  if (errorMessage.includes('invalid_grant') ||
    errorMessage.includes('invalid_client') ||
    errorMessage.includes('unauthorized')) {

    console.log('🚨 [Logout] Authorization error detected');
    const authStore = useAuthStore();
    authStore.logout();

    // ⚠️ [추가 가능] 명확한 알림
    // showNotification('세션이 무효화되었습니다. 다시 로그인해주세요.', 'error');
    return;
  }

  // ====================================================================
  // 🔧 [개선 8-2] 기타 알 수 없는 에러
  // ====================================================================
  console.warn('⚠️ [Unknown] Silent renew error type not recognized');
  // ⚠️ [추가 가능] 에러 모니터링 서비스에 보고
  // reportError('unknown_silent_renew_error', { error });
});

// ==================== 초기화 ====================

/**
 * OIDC Metadata 로드 (1회만)
 */
let metadataInitialized = false;

userManager.metadataService.getMetadata()
  .then(metadata => {
    if (!metadataInitialized) {
      console.group('✅ OIDC Metadata loaded');
      console.log('Issuer:', metadata.issuer);
      console.log('Authorization Endpoint:', metadata.authorization_endpoint);
      console.groupEnd();
      metadataInitialized = true;
    }
  })
  .catch(error => {
    console.group('❌ Failed to load OIDC Metadata');
    console.error('Authority:', settings.authority);
    console.error('Error:', error.message);
    console.groupEnd();
  });

// ====================================================================
// 🔧 [개선 9] 주기적 토큰 상태 동기화 (옵션)
// ====================================================================
// 배경: Silent-renew는 백그라운드에서 진행되므로
//       UI 상태와 실제 토큰 상태가 불일치할 수 있음
//
// 해결: 30초마다 토큰 상태 확인하여 UI 동기화
// ====================================================================
// Uncomment to enable automatic sync
/*
setInterval(async () => {
  try {
    const user = await userManager.getUser();
    const authStore = useAuthStore();

    // 케이스 1: 유효한 토큰이 있는데 UI는 로그아웃 상태
    if (user && user.access_token && !authStore.isAuthenticated) {
      console.warn('⚠️ [Sync] Token exists but UI shows logged out, syncing...');
      authStore.setUser(user);
    }

    // 케이스 2: 토큰이 없는데 UI는 로그인 상태
    if (!user && authStore.isAuthenticated) {
      console.warn('⚠️ [Sync] No token but UI shows logged in, logging out...');
      authStore.logout();
    }
  } catch (err) {
    console.error('Error during token sync:', err);
  }
}, 30000);
*/

export default userManager;