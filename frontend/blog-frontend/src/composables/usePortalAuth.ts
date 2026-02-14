/**
 * usePortalAuth - Vue Composable for Portal Auth State
 *
 * portal-shell의 authAdapter를 통해 인증 상태를 reactive하게 소비.
 * blog-frontend는 자체 Pinia 인스턴스를 사용하므로 useAuthStore() 대신
 * framework-agnostic adapter를 Vue ref로 감싸서 사용한다.
 *
 * Static import로 모듈 로드 시 즉시 초기화 (동기).
 * PostListPage 등 기존 코드가 이미 portal/stores를 static import하므로
 * 동일한 MF 모듈 해석 방식을 따른다.
 */
import { ref, computed } from 'vue';
import { authAdapter, type AuthState } from 'portal/stores';

// Module-level singleton: 모든 컴포넌트가 같은 reactive state를 공유
const _state = ref<AuthState>(authAdapter.getState());

const _unsubscribe = authAdapter.subscribe((newState: AuthState) => {
  _state.value = newState;
});

/** MF app unmount 시 호출 (bootstrap.ts에서 사용) */
export function disposePortalAuth(): void {
  _unsubscribe();
  _state.value = {
    isAuthenticated: false,
    displayName: 'Guest',
    isAdmin: false,
    isSeller: false,
    roles: [],
    memberships: {},
    user: null,
  };
}

/**
 * auth adapter에서 현재 인증 상태를 동기적으로 조회.
 * 컴포넌트 밖(router guard 등)에서 사용.
 */
export function getPortalAuthState(): AuthState {
  return authAdapter.getState();
}

export function usePortalAuth() {
  return {
    isAuthenticated: computed(() => _state.value.isAuthenticated),
    displayName: computed(() => _state.value.displayName),
    isAdmin: computed(() => _state.value.isAdmin),
    isSeller: computed(() => _state.value.isSeller),
    roles: computed(() => _state.value.roles),
    user: computed(() => _state.value.user),
    userUuid: computed(() => _state.value.user?.uuid ?? null),
    // Actions
    hasRole: (role: string) => authAdapter.hasRole(role),
    hasAnyRole: (roles: string[]) => authAdapter.hasAnyRole(roles),
    logout: () => authAdapter.logout(),
    requestLogin: (path?: string) => authAdapter.requestLogin(path),
  };
}
