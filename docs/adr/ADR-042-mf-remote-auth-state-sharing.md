# ADR-042: Module Federation Remote에서의 인증 상태 공유

**Status**: Accepted
**Date**: 2026-02-15
**Author**: Laze
**Supersedes**: MF Remote의 직접 Pinia Store 공유 패턴

## Context

Portal Universe는 Module Federation 기반 마이크로 프론트엔드 아키텍처를 사용한다. portal-shell (Vue 3 Host)은 `authStore` (Pinia), `apiClient` (axios)를 expose하며, blog-frontend (Vue 3 Remote)는 Embedded 모드에서 portal-shell의 인증 상태(로그인 여부, 사용자 정보, 권한)에 접근해야 한다.

기존 방식은 portal-shell의 `useAuthStore()`를 MF `portal/stores`로 공유하여 직접 호출하는 것이었으나, blog-frontend가 자체적으로 `createPinia()`를 호출하여 followStore, searchStore를 관리하면서 **Dual Pinia Instance** 문제가 발생했다. Pinia의 `shared: { singleton: true }`는 라이브러리 코드를 공유하지만, 각 앱의 `createPinia()` 호출로 생성된 인스턴스는 별도로 생성된다. 결과적으로 blog-frontend에서 `useAuthStore()` 호출 시 portal-shell의 Pinia 인스턴스가 아닌 자체 Pinia 인스턴스에서 빈 authStore를 생성하여, 로그인 상태임에도 인증 필요 UI가 표시되지 않는 문제가 발생했다.

## Decision

portal-shell의 `authAdapter` (framework-agnostic store adapter)를 blog-frontend의 Vue composable `usePortalAuth`로 감싸서 인증 상태를 공유한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| A. Host Pinia 공유 + 로컬 Pinia 분리 | authStore를 직접 사용 가능, portal-shell 코드 변경 불필요 | Pinia가 단일 activePinia만 지원하여 `useStore()` 호출마다 인스턴스 관리 필요. App context 외부에서 수동 바인딩 필요. 복잡도 높음 |
| B. authAdapter 기반 composable (선택) | portal-shell 변경 불필요. React remote와 동일 패턴. framework-agnostic. module-level singleton으로 성능 최적 | authStore 기능을 composable에서 재노출 필요 |
| C. window 전역 토큰 + 이벤트 | 가장 단순 | JWT 직접 파싱은 보안 안티패턴. JWT 구조 변경에 취약. reactive하지 않음 |
| D. portal:auth-changed CustomEvent | 완전한 decoupling | 초기 상태 동기화 문제. 이벤트 기반 상태 관리의 일관성 문제 |

## Rationale

- portal-shell에 이미 `storeAdapter.ts`가 존재하여 authAdapter를 React remote에 제공 중이므로, Vue remote에서도 동일한 adapter를 사용하면 **프레임워크 간 일관성**이 보장됨
- authAdapter는 `getState()`로 초기 상태를 동기적으로 가져오고, `subscribe()`로 이후 변경을 reactive하게 추적하여 **초기 상태 동기화 문제가 없음**
- blog-frontend의 자체 Pinia (followStore, searchStore)와 **충돌 없이** 독립적으로 동작
- static import (`import { authAdapter } from 'portal/stores'`)로 **모듈 로드 시 즉시 초기화** → 컴포넌트 렌더링 전에 인증 상태 확보
- module-level singleton ref로 **모든 컴포넌트가 동일한 reactive state 공유** → 메모리 효율적

## Trade-offs

**장점**:
- portal-shell 코드 변경 불필요 (기존 authAdapter 그대로 사용)
- React remote와 동일한 패턴으로 MF 인증 아키텍처 통일
- Pinia 인스턴스 독립성 유지 (blog-frontend 고유 store 영향 없음)
- 동기적 초기화로 FOUC(Flash of Unauthenticated Content) 방지

**단점 및 완화**:
- authStore의 기능을 usePortalAuth composable에서 수동으로 재노출해야 함 → (완화: authAdapter가 이미 필요한 메서드를 모두 제공하므로 1:1 매핑으로 충분)
- 새로운 auth 관련 기능 추가 시 authAdapter + usePortalAuth 양쪽 업데이트 필요 → (완화: authAdapter 인터페이스가 안정적이며 자주 변경되지 않음)

## Implementation

**핵심 구현 파일**:
- `frontend/blog-frontend/src/composables/usePortalAuth.ts` - authAdapter wrapper composable (신규)
- `frontend/blog-frontend/src/types/federation.d.ts` - portal/stores 모듈 타입 (authAdapter, AuthState)
- `frontend/blog-frontend/src/router/index.ts` - Auth Guard에서 getPortalAuthState() 사용
- `frontend/blog-frontend/src/bootstrap.ts` - unmount 시 disposePortalAuth() 호출
- `frontend/portal-shell/src/store/storeAdapter.ts` - authAdapter 원본 (변경 없음)

**아키텍처 패턴**:
```typescript
// portal-shell (변경 없음)
export const authAdapter = {
  getState: () => authStore.value,
  subscribe: (listener) => watch(authStore, listener),
  login: (credentials) => authStore.value.login(credentials),
  logout: () => authStore.value.logout()
};

// blog-frontend (신규)
const authState = ref(authAdapter.getState());
authAdapter.subscribe((newState) => { authState.value = newState; });

export const usePortalAuth = () => ({
  isAuthenticated: computed(() => authState.value.isAuthenticated),
  user: computed(() => authState.value.user),
  login: authAdapter.login,
  logout: authAdapter.logout
});
```

**Scope**:
- 이 ADR은 Vue MF remote (blog-frontend)에 적용
- React MF remote (shopping-frontend, prism-frontend)는 이미 `@portal/react-bridge`를 통해 authAdapter 사용 중
- 향후 추가되는 Vue MF remote (admin-frontend, drive-frontend)도 동일한 usePortalAuth 패턴 적용 권장

## References

- ADR-035: Polyglot Authentication Standardization (MF 간 인증 표준화)
- `frontend/portal-shell/src/store/storeAdapter.ts`: authAdapter 원본 구현
- `@portal/react-bridge/src/api-registry.ts`: React remote의 authAdapter 사용 예

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-15 | 초안 작성 | Laze |
