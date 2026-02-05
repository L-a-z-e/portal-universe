---
id: api-portal-shell-store-adapter
title: Portal Shell Store Adapter
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Documenter Agent
tags: [api, portal-shell, react, store-adapter, module-federation, pinia, useSyncExternalStore]
related:
  - api-portal-shell-auth-store
  - api-portal-shell-theme-store
---

# Portal Shell Store Adapter

> React 등 Vue 외 프레임워크에서 Pinia Store를 사용하기 위한 Framework-Agnostic Adapter

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/stores` |
| **Export 이름** | `themeAdapter`, `authAdapter`, `portalStoreAdapter` |
| **주요 용도** | React `useSyncExternalStore`와 호환되는 인터페이스 제공 |
| **지원 프레임워크** | React 18+, Preact, Solid.js (useSyncExternalStore 지원 프레임워크) |

---

## 🎯 주요 기능

### 1. React useSyncExternalStore 호환
- `getState()` - 현재 상태 스냅샷 반환
- `subscribe(callback)` - 상태 변경 구독
- 참조 안정성 보장 (Object.is 비교 대응)

### 2. Theme Store Adapter
- `themeAdapter.getState()` - 테마 상태 조회
- `themeAdapter.subscribe(callback)` - 테마 변경 구독
- `themeAdapter.toggle()` - 테마 전환
- `themeAdapter.initialize()` - 테마 초기화

### 3. Auth Store Adapter
- `authAdapter.getState()` - 인증 상태 조회
- `authAdapter.subscribe(callback)` - 인증 상태 변경 구독
- `authAdapter.hasRole(role)` - 역할 확인
- `authAdapter.logout()` - 로그아웃
- `authAdapter.getAccessToken()` - 토큰 조회

---

## 📦 타입 정의

### ThemeState

```typescript
interface ThemeState {
  isDark: boolean;
}
```

### AuthState

```typescript
interface AuthState {
  isAuthenticated: boolean;
  displayName: string;
  isAdmin: boolean;
  isSeller: boolean;
  roles: string[];
  memberships: Record<string, string>;
  user: {
    uuid?: string;
    email?: string;
    username?: string;
    name?: string;
    nickname?: string;
    picture?: string;
  } | null;
}
```

### UnsubscribeFn

```typescript
type UnsubscribeFn = () => void;
```

---

## 🔹 Theme Adapter

### themeAdapter.getState

```typescript
function getState(): ThemeState
```

현재 테마 상태를 반환합니다.

**Returns:** `ThemeState` - 테마 상태 스냅샷

**특징:**
- 참조 안정성 보장: `isDark` 값이 동일하면 같은 객체 반환
- React의 `Object.is` 비교에 최적화

**예시:**

```typescript
import { themeAdapter } from 'portal/stores';

const state = themeAdapter.getState();
console.log('Dark mode:', state.isDark);
```

---

### themeAdapter.subscribe

```typescript
function subscribe(callback: (state: ThemeState) => void): UnsubscribeFn
```

테마 상태 변경을 구독합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `callback` | `(state: ThemeState) => void` | ✅ | 상태 변경 시 호출될 콜백 |

**Returns:** `UnsubscribeFn` - 구독 해제 함수

**주의:**
- `immediate: false` 설정: React useSyncExternalStore는 subscribe 중 동기 콜백 호출을 허용하지 않음
- React는 `getState()`로 초기값을 읽음

**예시:**

```typescript
import { themeAdapter } from 'portal/stores';

const unsubscribe = themeAdapter.subscribe((state) => {
  console.log('테마 변경:', state.isDark ? 'Dark' : 'Light');
});

// 구독 해제
unsubscribe();
```

---

### themeAdapter.toggle

```typescript
function toggle(): void
```

테마를 전환합니다.

**예시:**

```typescript
import { themeAdapter } from 'portal/stores';

themeAdapter.toggle();
```

---

### themeAdapter.initialize

```typescript
function initialize(): void
```

테마를 초기화합니다. (localStorage에서 복원)

**예시:**

```typescript
import { themeAdapter } from 'portal/stores';

themeAdapter.initialize();
```

---

## 🔹 Auth Adapter

### authAdapter.getState

```typescript
function getState(): AuthState
```

현재 인증 상태를 반환합니다.

**Returns:** `AuthState` - 인증 상태 스냅샷

**특징:**
- 참조 안정성 보장: primitive 필드가 동일하면 같은 객체 반환
- `user` 객체 참조도 추적하여 불필요한 렌더링 방지

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

const state = authAdapter.getState();
console.log('로그인 상태:', state.isAuthenticated);
console.log('사용자명:', state.displayName);
```

---

### authAdapter.subscribe

```typescript
function subscribe(callback: (state: AuthState) => void): UnsubscribeFn
```

인증 상태 변경을 구독합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `callback` | `(state: AuthState) => void` | ✅ | 상태 변경 시 호출될 콜백 |

**Returns:** `UnsubscribeFn` - 구독 해제 함수

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

const unsubscribe = authAdapter.subscribe((state) => {
  console.log('인증 상태 변경:', state.isAuthenticated);
});

// 구독 해제
unsubscribe();
```

---

### authAdapter.hasRole

```typescript
function hasRole(role: string): boolean
```

특정 역할을 가지고 있는지 확인합니다.

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

if (authAdapter.hasRole('SUPER_ADMIN')) {
  console.log('슈퍼 관리자입니다.');
}
```

---

### authAdapter.hasAnyRole

```typescript
function hasAnyRole(roles: string[]): boolean
```

여러 역할 중 하나 이상을 가지고 있는지 확인합니다.

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

if (authAdapter.hasAnyRole(['SUPER_ADMIN', 'SERVICE_ADMIN:BLOG'])) {
  console.log('블로그 관리 권한이 있습니다.');
}
```

---

### authAdapter.isServiceAdmin

```typescript
function isServiceAdmin(service: string): boolean
```

특정 서비스의 관리자 권한이 있는지 확인합니다.

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

if (authAdapter.isServiceAdmin('shopping')) {
  console.log('쇼핑몰 관리자입니다.');
}
```

---

### authAdapter.logout

```typescript
function logout(): void
```

로그아웃합니다.

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

authAdapter.logout();
```

---

### authAdapter.getAccessToken

```typescript
function getAccessToken(): string | null
```

현재 Access Token을 반환합니다.

**Returns:** Access Token 문자열 또는 null

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

const token = authAdapter.getAccessToken();
if (token) {
  console.log('토큰:', token);
}
```

---

### authAdapter.requestLogin

```typescript
function requestLogin(path?: string): void
```

로그인 모달을 요청합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `path` | string | ❌ | 로그인 후 리다이렉트할 경로 |

**예시:**

```typescript
import { authAdapter } from 'portal/stores';

authAdapter.requestLogin('/shopping/cart');
```

---

## 🔹 Portal Store Adapter (통합)

### portalStoreAdapter

```typescript
const portalStoreAdapter = {
  theme: themeAdapter,
  auth: authAdapter
};
```

Theme과 Auth Adapter를 통합한 객체.

**예시:**

```typescript
import { portalStoreAdapter } from 'portal/stores';

// Theme
portalStoreAdapter.theme.toggle();

// Auth
const authState = portalStoreAdapter.auth.getState();
```

---

## 🔹 React에서 사용하기

### 1. Custom Hook 생성

```typescript
// shopping-frontend/src/hooks/usePortalTheme.ts
import { useSyncExternalStore } from 'react';
import { themeAdapter } from 'portal/stores';

export function usePortalTheme() {
  const themeState = useSyncExternalStore(
    themeAdapter.subscribe,
    themeAdapter.getState
  );

  return {
    isDark: themeState.isDark,
    toggle: themeAdapter.toggle,
    initialize: themeAdapter.initialize,
  };
}
```

```typescript
// shopping-frontend/src/hooks/usePortalAuth.ts
import { useSyncExternalStore } from 'react';
import { authAdapter } from 'portal/stores';

export function usePortalAuth() {
  const authState = useSyncExternalStore(
    authAdapter.subscribe,
    authAdapter.getState
  );

  return {
    ...authState,
    logout: authAdapter.logout,
    hasRole: authAdapter.hasRole,
    hasAnyRole: authAdapter.hasAnyRole,
    isServiceAdmin: authAdapter.isServiceAdmin,
    getAccessToken: authAdapter.getAccessToken,
    requestLogin: authAdapter.requestLogin,
  };
}
```

---

### 2. 컴포넌트에서 사용

```tsx
// shopping-frontend/src/components/Header.tsx
import { usePortalAuth } from '@/hooks/usePortalAuth';
import { usePortalTheme } from '@/hooks/usePortalTheme';

export function Header() {
  const auth = usePortalAuth();
  const theme = usePortalTheme();

  return (
    <header>
      <button onClick={theme.toggle}>
        {theme.isDark ? '🌙' : '☀️'}
      </button>

      {auth.isAuthenticated ? (
        <div>
          <span>환영합니다, {auth.displayName}님</span>
          <button onClick={auth.logout}>로그아웃</button>
        </div>
      ) : (
        <button onClick={() => auth.requestLogin()}>로그인</button>
      )}
    </header>
  );
}
```

---

### 3. 권한 확인

```tsx
// shopping-frontend/src/components/AdminPanel.tsx
import { usePortalAuth } from '@/hooks/usePortalAuth';

export function AdminPanel() {
  const auth = usePortalAuth();

  if (!auth.isServiceAdmin('shopping')) {
    return <div>접근 권한이 없습니다.</div>;
  }

  return (
    <div>
      <h1>쇼핑몰 관리자 패널</h1>
      {/* ... */}
    </div>
  );
}
```

---

### 4. 초기화

```tsx
// shopping-frontend/src/bootstrap.tsx
import { createRoot } from 'react-dom/client';
import { themeAdapter } from 'portal/stores';
import App from './App';

// 테마 초기화
themeAdapter.initialize();

const root = createRoot(document.getElementById('root')!);
root.render(<App />);
```

---

## ⚠️ 주의사항

### 1. React Error #185 방지

```typescript
// ❌ 나쁜 예: subscribe 중 동기 콜백 호출
export const themeAdapter = {
  subscribe: (callback) => {
    const unwatch = watch(
      () => store.isDark,
      callback,
      { immediate: true }  // 동기 콜백 호출 → Error #185
    );
    return unwatch;
  }
};

// ✅ 좋은 예: immediate: false (기본값)
export const themeAdapter = {
  subscribe: (callback) => {
    const unwatch = watch(
      () => store.isDark,
      callback
      // immediate: false가 기본값
    );
    return unwatch;
  }
};
```

**이유:** React `useSyncExternalStore`는 `subscribe` 함수 내에서 동기적으로 `callback`을 호출하는 것을 허용하지 않음.

---

### 2. 참조 안정성 보장

```typescript
// Adapter 내부 구현 (참고용)
let _themeSnapshot: ThemeState | null = null;

export const themeAdapter = {
  getState: (): ThemeState => {
    const store = useThemeStore();
    const isDark = store.isDark;

    // 값이 동일하면 같은 참조 반환 → 불필요한 렌더링 방지
    if (_themeSnapshot && _themeSnapshot.isDark === isDark) {
      return _themeSnapshot;
    }

    _themeSnapshot = { isDark };
    return _themeSnapshot;
  }
};
```

**이유:** React `useSyncExternalStore`는 `Object.is`로 이전 값과 비교함. 참조가 동일하면 리렌더링하지 않음.

---

### 3. Vue Reactivity 주의

```typescript
// ❌ 나쁜 예: Reactive 객체를 그대로 반환
export const authAdapter = {
  getState: () => {
    const store = useAuthStore();
    return store.user;  // Vue Reactive Proxy → React에서 오작동
  }
};

// ✅ 좋은 예: Plain 객체로 변환
export const authAdapter = {
  getState: () => {
    const store = useAuthStore();
    return {
      isAuthenticated: store.isAuthenticated,
      displayName: store.displayName,
      // ... (primitive 값 또는 plain 객체)
    };
  }
};
```

**이유:** Vue의 Reactive Proxy는 React에서 예상치 못한 동작을 유발할 수 있음.

---

## 🔗 관련 문서

- [Auth Store API](./auth-store.md) - 인증 상태 관리
- [Theme Store API](./theme-store.md) - 테마 상태 관리
- [API Client](./api-client.md) - HTTP 요청 클라이언트

---

## 📚 참고 자료

- [React useSyncExternalStore](https://react.dev/reference/react/useSyncExternalStore)
- [Pinia](https://pinia.vuejs.org/)

---

**최종 업데이트**: 2026-02-06
