import { UserManager, WebStorageStateStore } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

const settings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
  response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
  scope: import.meta.env.VITE_OIDC_SCOPE,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  automaticSilentRenew: true
};

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
  console.log('User loaded', user);
  const authStore = useAuthStore();
  if (user.access_token) {
    authStore.login(user.access_token);
  }
});

userManager.events.addAccessTokenExpired(() => {
  console.log('Token expired, trying to renew...');
});

export default userManager;