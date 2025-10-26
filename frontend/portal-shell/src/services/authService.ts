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

// ==================== 공개 함수 ====================

export function login() {
  return userManager.signinRedirect();
}

export function logout() {
  const authStore = useAuthStore();
  authStore.logout();
  return userManager.signoutRedirect();
}

// ==================== 이벤트 핸들러 ====================

/**
 * User Loaded (중복 방지)
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
 */
userManager.events.addAccessTokenExpiring(() => {
  console.log('⏰ Token expiring soon, auto-renewing...');
});

/**
 * Access Token Expired
 */
userManager.events.addAccessTokenExpired(() => {
  console.log('❌ Token expired');
  const authStore = useAuthStore();
  authStore.logout();
});

/**
 * User Signed Out
 */
userManager.events.addUserSignedOut(() => {
  console.log('👋 User signed out');
  const authStore = useAuthStore();
  authStore.logout();
});

/**
 * Silent Renew Error
 */
userManager.events.addSilentRenewError((error) => {
  console.group('❌ Silent renew failed');
  console.error('Error:', error.message);
  console.groupEnd();
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

export default userManager;