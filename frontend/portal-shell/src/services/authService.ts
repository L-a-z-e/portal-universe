import { UserManager, WebStorageStateStore } from "oidc-client-ts";
import { useAuthStore } from "../store/auth.ts";

const settings = {
  authority: 'http://localhost:8080/auth-service',
  // authority: 'http://api-gateway:8080/auth-service',
  client_id: 'portal-client',
  redirect_uri: 'http://localhost:50000/callback',
  // redirect_uri: 'http://host.docker.internal:50000/callback',
  post_logout_redirect_uri: 'http://localhost:50000',
  // post_logout_redirect_uri: 'http://host.docker.internal:50000',
  response_type: 'code',
  scope: 'openid profile read write',
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