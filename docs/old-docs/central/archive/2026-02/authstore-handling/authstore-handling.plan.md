# AuthStore Cross-Framework Handling Plan

> Feature: authstore-handling
> Phase: Plan
> Created: 2026-02-02
> Author: AI-assisted (reviewed with developer)

## 1. 문제 정의

### 현재 상황

portal-universe는 Module Federation 기반 Polyglot 프론트엔드 아키텍처로, **portal-shell(Vue 3/Pinia)이 인증의 Single Source of Truth**이다.

| App | Framework | 인증 접근 방식 | 문제 |
|-----|-----------|---------------|------|
| portal-shell | Vue 3 + Pinia | 직접 관리 (authStore) | 없음 (원본) |
| blog-frontend | Vue 3 | Pinia 직접 import | 없음 (동일 framework) |
| shopping-frontend | React 18 + Zustand | authAdapter + syncFromPortal + CustomEvent | 중복 코드, 동기화 이슈 |
| prism-frontend | React 18 + Zustand | authAdapter + syncFromPortal + CustomEvent | shopping과 완전 동일 중복 |
| (향후) | Svelte, Angular 등 | ? | 또 다른 중복 발생 예정 |

### 핵심 문제 3가지

**1. 동기화 메커니즘 3중 혼재**

React 리모트가 portal-shell의 인증 상태를 얻기 위해 3가지 채널을 동시에 사용:
- Module Federation dynamic import → `authAdapter.getState()`
- Window globals → `__PORTAL_ACCESS_TOKEN__`, `__POWERED_BY_PORTAL_SHELL__`
- CustomEvent → `portal:auth-changed`

각 채널의 타이밍과 신뢰성이 다르고, 어떤 것이 정답인지 불명확.

**2. React 리모트 간 완전 코드 중복**

shopping-frontend와 prism-frontend에서 **동일 파일이 복사-붙여넣기로 존재**:

| 파일 | 역할 | 중복 |
|------|------|------|
| `stores/authStore.ts` | Zustand 인증 스토어 + syncFromPortal() | 완전 동일 |
| `hooks/usePortalStore.ts` | authAdapter 구독 Hook | 완전 동일 |
| `components/guards/RequireAuth.tsx` | 인증 가드 컴포넌트 | 유사 |
| `api/client.ts` | Axios 토큰 인터셉터 | 유사 |
| `types/portal-modules.d.ts` | 타입 선언 | 완전 동일 |

한 쪽에서 버그를 수정하면 다른 쪽에도 수동으로 반영해야 함. 이미 여러 차례 불일치 발생.

**3. 확장성 부재**

현재 구조에서 새 framework의 리모트가 추가되면:
1. 해당 framework용 authStore를 다시 작성해야 함
2. syncFromPortal 로직을 다시 구현해야 함
3. Window globals 처리를 다시 해야 함
4. 동일한 race condition 버그를 다시 경험

---

## 2. 아키텍처 분석

### AS-IS: 현재 구조

```
portal-shell (Vue 3)
├── Pinia authStore (원본)
├── authService.ts (JWT 관리)
├── storeAdapter.ts (authAdapter 생성)
│   └── Module Federation으로 expose
│
├─── blog-frontend (Vue 3)
│    └── import { useAuthStore } from 'portal/stores'  ← 직접 접근 ✅
│
├─── shopping-frontend (React 18)
│    ├── stores/authStore.ts  ← Zustand 복제본 ❌
│    ├── hooks/usePortalStore.ts  ← adapter 구독 복제 ❌
│    ├── guards/RequireAuth.tsx  ← 인증 가드 복제 ❌
│    └── syncFromPortal() + portal:auth-changed + window globals
│
└─── prism-frontend (React 18)
     ├── stores/authStore.ts  ← 완전 동일 복제 ❌
     ├── hooks/usePortalStore.ts  ← 완전 동일 복제 ❌
     └── (RequireAuth 없음 - 별도 처리)
```

### TO-BE: 3-Layer 아키텍처

```
Layer 1: Contract (framework-agnostic interfaces)
┌──────────────────────────────────────────────┐
│ @portal/auth-contract                        │
│ - PortalAuthBridge interface                 │
│ - AuthState, AuthUser types                  │
│ - 의존성: 0개 (순수 TypeScript)              │
└──────────────────────────────────────────────┘
            ▲                    ▲
            │                    │
Layer 2: Bridge (구현체)          │
┌──────────────────────┐        │
│ portal-shell          │        │
│ storeAdapter.ts       │        │
│ implements            │        │
│ PortalAuthBridge      │        │
│ (Module Fed expose)   │        │
└──────────────────────┘        │
                                │
Layer 3: Consumer (framework-specific thin wrapper)
┌──────────────────────┐  ┌────────────┐  ┌──────────────┐
│ @portal/react-auth   │  │ Vue: 불필요 │  │ (향후)        │
│ - useAuth() hook     │  │ Pinia 직접  │  │ @portal/     │
│ - AuthProvider       │  │ 접근 유지   │  │ svelte-auth  │
│ - RequireAuth guard  │  └────────────┘  │ angular-auth │
│ - createApiInterceptor                  │ etc.         │
└──────────────────────┘                  └──────────────┘
        ▲                        ▲
        │                        │
┌───────┴──────┐  ┌──────────────┴─────┐
│ shopping-fe  │  │ prism-fe           │
│ (소비만)      │  │ (소비만)            │
│ 중복코드 0    │  │ 중복코드 0          │
└──────────────┘  └────────────────────┘
```

---

## 3. 해결 방안

### Layer 1: @portal/auth-contract

**위치**: `frontend/common/auth-contract/`

순수 TypeScript 인터페이스만 정의. 모든 layer가 이 contract에 의존.

```typescript
// PortalAuthBridge - 모든 framework가 소비하는 단일 인터페이스
interface PortalAuthBridge {
  // State
  getState(): AuthState
  getAccessToken(): string | null

  // Subscription (framework-agnostic observer)
  subscribe(callback: (state: AuthState) => void): UnsubscribeFn

  // Actions
  requestLogin(redirectPath?: string): void
  logout(): void

  // Role checks
  hasRole(role: string): boolean
  hasAnyRole(roles: string[]): boolean
  isServiceAdmin(service: string): boolean
}

interface AuthState {
  isAuthenticated: boolean
  user: AuthUser | null
  displayName: string
  roles: string[]
  memberships: Record<string, string>
}

interface AuthUser {
  id: string
  email: string
  name: string
  nickname?: string
  avatar?: string
}
```

**핵심 원칙**: 이 인터페이스가 바뀌지 않는 한, 모든 consumer는 안전.

### Layer 2: Portal Shell Bridge 구현

**위치**: `frontend/portal-shell/src/store/storeAdapter.ts` (기존 파일 개선)

현재 `authAdapter`가 이미 이 역할을 하고 있지만, **PortalAuthBridge 인터페이스를 명시적으로 implement**하도록 formalize.

변경 사항:
1. `authAdapter`가 `PortalAuthBridge` 인터페이스를 구현하도록 타입 명시
2. `getAccessToken()` 메서드 추가 (현재 window global로만 노출)
3. `requestLogin()` 메서드 추가 (현재 `__PORTAL_SHOW_LOGIN__`으로만 노출)
4. Module Federation expose 경로: `portal/auth` (기존 `portal/stores`에서 분리)

**기존 `portal/stores`와의 호환성**: `portal/stores`에서도 re-export하여 하위 호환 유지.

### Layer 3: @portal/react-auth

**위치**: `frontend/common/react-auth/`

React 전용 thin wrapper. 의존성: `react`, `@portal/auth-contract`.

#### 3-1. AuthProvider

```typescript
// Module Federation에서 PortalAuthBridge를 resolve하고 Context로 제공
<AuthProvider
  mode="embedded" | "standalone"
  fallback={<LoadingSpinner />}
>
  <App />
</AuthProvider>
```

내부 동작:
- `embedded` 모드: `import('portal/auth')` → `PortalAuthBridge` 획득
- `standalone` 모드: 제공된 `StandaloneAuthBridge` 사용 (또는 guest 모드)
- resolve 완료 전: fallback 렌더링
- resolve 실패: error boundary 또는 standalone fallback

#### 3-2. useAuth() Hook

```typescript
const { isAuthenticated, user, displayName, roles, logout, hasRole } = useAuth()
```

내부 동작:
- AuthProvider의 Context에서 `PortalAuthBridge` 획득
- `bridge.subscribe()`로 상태 변경 감지
- `useSyncExternalStore` 패턴으로 React 렌더 사이클과 동기화

#### 3-3. RequireAuth Guard

```typescript
<RequireAuth fallback={<LoginRedirect />}>
  <ProtectedPage />
</RequireAuth>
```

#### 3-4. createApiInterceptor

```typescript
// Axios 인터셉터에 토큰 자동 주입
const client = axios.create({ baseURL })
createApiInterceptor(client) // bridge.getAccessToken() 사용
```

---

## 4. 마이그레이션 영향 분석

### 삭제되는 파일 (중복 제거)

| 파일 | App | 대체 |
|------|-----|------|
| `stores/authStore.ts` | shopping-frontend | `@portal/react-auth` useAuth() |
| `stores/authStore.ts` | prism-frontend | `@portal/react-auth` useAuth() |
| `hooks/usePortalStore.ts` (auth 부분) | shopping-frontend | useAuth() |
| `hooks/usePortalStore.ts` (auth 부분) | prism-frontend | useAuth() |
| `components/guards/RequireAuth.tsx` | shopping-frontend | `@portal/react-auth` RequireAuth |

### 수정되는 파일

| 파일 | App | 변경 내용 |
|------|-----|----------|
| `storeAdapter.ts` | portal-shell | PortalAuthBridge 인터페이스 implement |
| `vite.config.ts` | portal-shell | `portal/auth` expose 추가 |
| `bootstrap.tsx` | shopping, prism | AuthProvider 래핑 |
| `api/client.ts` | shopping, prism | createApiInterceptor 사용 |
| `App.tsx` | shopping, prism | authStore → useAuth() 전환 |

### 변경 없는 파일

| 파일 | App | 사유 |
|------|-----|------|
| `store/auth.ts` | portal-shell | 원본 Pinia store 유지 |
| `services/authService.ts` | portal-shell | JWT 관리 유지 |
| 모든 파일 | blog-frontend | Pinia 직접 접근 유지 (변경 불필요) |
| auth-service 백엔드 | 전체 | API 변경 없음 |

---

## 5. Window Globals 정리 전략

현재 5개의 Window globals가 인증에 관여:

| Global | 현재 용도 | TO-BE |
|--------|----------|-------|
| `__POWERED_BY_PORTAL_SHELL__` | 임베디드 모드 판단 | AuthProvider의 mode prop으로 대체. 하위 호환 위해 당분간 유지 |
| `__PORTAL_ACCESS_TOKEN__` | 토큰 공유 | `bridge.getAccessToken()`으로 대체. 하위 호환 위해 당분간 유지 |
| `__PORTAL_GET_ACCESS_TOKEN__` | 토큰 getter | `bridge.getAccessToken()`으로 통합 |
| `__PORTAL_SHOW_LOGIN__` | 로그인 요청 | `bridge.requestLogin()`으로 대체 |
| `__PORTAL_ON_AUTH_ERROR__` | 401 처리 | `createApiInterceptor` 내부에서 `bridge.requestLogin()` 호출 |

**전략**: 즉시 삭제하지 않고, 새 API와 병행 → deprecated 마킹 → 다음 PDCA에서 제거.

---

## 6. Standalone 모드 전략

현재 standalone 모드는 사실상 "인증 없는 guest 모드"인데, 이를 개선:

```typescript
// bootstrap.tsx에서 mode 결정
const mode = window.__POWERED_BY_PORTAL_SHELL__ ? 'embedded' : 'standalone'

<AuthProvider mode={mode}>
  <App />
</AuthProvider>
```

- **embedded**: Module Federation에서 PortalAuthBridge 로드
- **standalone**: guest 상태로 시작 (향후 직접 auth-service 연동 가능)

---

## 7. 디렉토리 구조

```
frontend/common/
├── auth-contract/                # Layer 1: 인터페이스
│   ├── package.json              # name: @portal/auth-contract, deps: 없음
│   └── src/
│       ├── types.ts              # AuthState, AuthUser, PortalAuthBridge
│       └── index.ts
│
├── react-auth/                   # Layer 3: React consumer
│   ├── package.json              # name: @portal/react-auth, deps: react
│   └── src/
│       ├── AuthContext.ts        # React Context 정의
│       ├── AuthProvider.tsx      # Bridge resolution + Context provider
│       ├── useAuth.ts            # useSyncExternalStore 기반 hook
│       ├── RequireAuth.tsx       # 인증 가드 컴포넌트
│       ├── createApiInterceptor.ts  # Axios 토큰 인터셉터
│       ├── standalone.ts         # StandaloneAuthBridge (guest mode)
│       └── index.ts
│
└── (향후 필요시)
    ├── svelte-auth/
    ├── angular-auth/
    └── vanilla-auth/             # Web Component용
```

---

## 8. 커밋 전략 (6 commits)

1. `feat(common): create @portal/auth-contract with PortalAuthBridge interface`
   - auth-contract 패키지 생성, 인터페이스/타입 정의
   - pnpm workspace 등록

2. `refactor(shell): implement PortalAuthBridge in storeAdapter`
   - storeAdapter가 PortalAuthBridge interface 구현
   - getAccessToken(), requestLogin() 메서드 추가
   - `portal/auth` Module Federation expose 추가
   - 기존 `portal/stores` 하위 호환 유지

3. `feat(common): create @portal/react-auth package`
   - AuthProvider, useAuth, RequireAuth, createApiInterceptor 구현
   - StandaloneAuthBridge (guest mode) 구현
   - pnpm workspace 등록

4. `refactor(shopping-fe): migrate to @portal/react-auth`
   - stores/authStore.ts 삭제
   - hooks/usePortalStore.ts에서 auth 로직 제거
   - RequireAuth.tsx 삭제 → @portal/react-auth 것으로 대체
   - api/client.ts → createApiInterceptor 사용
   - bootstrap.tsx에 AuthProvider 래핑

5. `refactor(prism-fe): migrate to @portal/react-auth`
   - stores/authStore.ts 삭제
   - hooks/usePortalStore.ts에서 auth 로직 제거
   - bootstrap.tsx에 AuthProvider 래핑
   - api/client.ts → createApiInterceptor 사용

6. `chore(common): deprecate window globals and update type declarations`
   - portal-modules.d.ts 통합 (auth-contract에서 단일 관리)
   - Window globals에 @deprecated JSDoc 추가
   - usePortalStore.ts에서 theme만 남기도록 정리

---

## 9. 리스크 및 대응

| 리스크 | 심각도 | 대응 |
|--------|--------|------|
| Module Federation에서 pnpm workspace 패키지 resolve 실패 | 높음 | auth-contract은 TS source 직접 import (빌드 없이 사용). react-auth는 pre-build 후 dist 참조 |
| React Context가 Module Federation 경계에서 깨짐 | 중간 | Context 대신 module-level singleton + useSyncExternalStore 패턴 사용 고려 |
| Standalone 모드에서 인증 불가 | 낮음 | StandaloneAuthBridge가 guest mode 제공. 향후 직접 auth-service 연동 가능 |
| 기존 코드와의 하위 호환성 | 중간 | Window globals 즉시 삭제 안 함. portal/stores re-export 유지 |
| usePortalStore에서 theme과 auth 분리 시 기존 사용처 영향 | 낮음 | usePortalTheme은 그대로 유지. usePortalAuth만 useAuth로 대체 |

---

## 10. 검증

### 빌드 검증
- [ ] `pnpm --filter @portal/auth-contract build` (또는 noEmit check)
- [ ] `pnpm --filter @portal/react-auth build`
- [ ] `pnpm --filter portal-shell build`
- [ ] `pnpm --filter shopping-frontend build`
- [ ] `pnpm --filter prism-frontend build`
- [ ] `pnpm --filter blog-frontend build` (영향 없음 확인)

### 기능 검증
- [ ] Portal Shell 로그인 → shopping-frontend에서 인증 상태 동기화
- [ ] Portal Shell 로그인 → prism-frontend에서 인증 상태 동기화
- [ ] Portal Shell 로그아웃 → 양쪽 리모트에서 로그아웃 상태 반영
- [ ] shopping-frontend standalone 모드 기동 (guest mode)
- [ ] prism-frontend standalone 모드 기동 (guest mode)
- [ ] blog-frontend 변경 없음 확인
- [ ] API 요청 시 토큰 자동 주입 동작

### Lint
- [ ] shopping-frontend ESLint 0 errors
- [ ] prism-frontend ESLint 0 errors
- [ ] auth-contract, react-auth ESLint 0 errors

---

## 11. 스코프 제외

| 항목 | 사유 |
|------|------|
| Window globals 완전 삭제 | 하위 호환성. 다음 PDCA에서 처리 |
| Standalone 모드에서 직접 로그인 | auth-service 직접 호출 구현 필요. 별도 PDCA |
| blog-frontend 마이그레이션 | Vue/Pinia 직접 접근으로 문제 없음 |
| Token refresh 로직 개선 | 현재 portal-shell에서 정상 동작 중. 별도 이슈 |
| SSR 지원 | 현재 CSR only. 별도 PDCA |

---

## 12. 이 접근이 우아한 이유

1. **Contract-First**: 인터페이스가 변하지 않는 한 어떤 framework든 안전하게 소비 가능
2. **Bridge는 하나, Consumer는 N개**: 새 framework 추가 시 thin wrapper만 만들면 됨
3. **Zero Breaking Change**: Window globals 즉시 삭제 안 하고 deprecation 경로 제공
4. **Pinia 직접 접근 보존**: Vue 리모트(blog)는 아무 변경 없음
5. **중복 코드 완전 제거**: React 리모트의 authStore, usePortalStore(auth), RequireAuth 삭제
6. **테스트 용이**: MockAuthBridge로 어떤 framework든 unit test 가능
