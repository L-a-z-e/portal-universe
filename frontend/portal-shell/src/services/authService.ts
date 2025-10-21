import { UserManager, WebStorageStateStore } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

// 환경변수로 PKCE 제어
const disablePKCE = import.meta.env.VITE_OIDC_DISABLE_PKCE === 'true';

const settings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
  response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
  scope: import.meta.env.VITE_OIDC_SCOPE,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  automaticSilentRenew: true,
  disablePKCE: disablePKCE,
};

console.log(`🔐 OIDC Configuration:`, {
  authority: settings.authority,
  client_id: settings.client_id,
  pkce: disablePKCE ? '❌ Disabled' : '✅ Enabled',
  profile: import.meta.env.VITE_PROFILE,
});

const userManager = new UserManager(settings);

export function login() {
  return userManager.signinRedirect();
}

export function logout() {
  const authStore = useAuthStore();
  authStore.logout();
  return userManager.signoutRedirect();
}

userManager.events.addUserLoaded((user) => {
  console.log('✅ User loaded', user.profile);
  const authStore = useAuthStore();
  if (user.access_token) {
    authStore.login(user.access_token);
  }
});

userManager.events.addAccessTokenExpired(() => {
  console.log('⚠️ Token expired, trying to renew...');
});

userManager.events.addUserSignedOut(() => {
  console.log('👋 User signed out');
  const authStore = useAuthStore();
  authStore.logout();
});

userManager.events.addSilentRenewError((error) => {
  console.error('❌ Silent renew failed:', error);
});

// 메타데이터 로드 확인
userManager.metadataService.getMetadata()
  .then(metadata => {
    console.log('✅ OIDC Metadata loaded successfully');
    console.log('   Issuer:', metadata.issuer);
    console.log('   Authorization Endpoint:', metadata.authorization_endpoint);
  })
  .catch(error => {
    console.error('❌ Failed to load OIDC Metadata:', error);
    console.error('   Authority:', settings.authority);
    console.error('   Please check if auth-service is running and accessible');
  });

export default userManager;