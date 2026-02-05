---
id: api-portal-shell-auth-store
title: Portal Shell Auth Store
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-30
author: Documenter Agent
tags: [api, portal-shell, pinia, auth, module-federation]
related:
  - api-portal-shell-api-client
---

# Portal Shell Auth Store

> Module Federation을 통해 Remote 모듈에 제공되는 인증 상태 관리 Pinia Store

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/stores` |
| **Store 라이브러리** | Pinia |
| **Store ID** | `auth` |
| **주요 기능** | 로그인 상태, 사용자 정보, 권한 확인 |

---

## 🎯 주요 기능

### 1. 사용자 정보 관리
- PortalUser 타입의 사용자 정보 저장
- JWT 토큰 파싱 및 저장

### 2. 권한 확인
- 역할(Role) 기반 권한 확인
- Admin 여부 확인

### 3. 인증 상태
- 로그인 여부 확인
- 사용자 표시 이름 제공

---

## 📦 타입 정의

### PortalUser

```typescript
interface PortalUser {
  profile: UserProfile;
  authority: UserAuthority;
  preferences: UserPreferences;

  // 토큰 정보 (내부 관리용)
  _accessToken: string;
  _refreshToken?: string;
  _expiresAt?: number;
  _issuedAt: number;
}
```

### UserProfile

```typescript
interface UserProfile {
  sub: string;                    // 사용자 ID (email)
  email: string;                  // 이메일
  username?: string;              // 사용자명
  name?: string;                  // 전체 이름
  nickname?: string;              // 닉네임
  picture?: string;               // 프로필 이미지 URL
  phone?: string;                 // 전화번호
  emailVerified?: boolean;        // 이메일 인증 여부
  locale?: string;                // 언어 (ko, en)
  timezone?: string;              // 타임존
}
```

### UserAuthority

```typescript
interface UserAuthority {
  roles: string[];                // 역할 (ROLE_ADMIN, ROLE_USER)
  scopes: string[];               // OAuth2 Scope (read, write)
}
```

### UserPreferences

```typescript
interface UserPreferences {
  theme: 'light' | 'dark';        // 테마
  language: string;               // 언어
  notifications: boolean;         // 알림 수신
}
```

---

## 🔹 State

### user

```typescript
user: PortalUser | null
```

현재 로그인한 사용자 정보. 로그인하지 않았으면 `null`.

---

## 🔹 Getters

### isAuthenticated

```typescript
isAuthenticated: ComputedRef<boolean>
```

로그인 여부를 반환합니다.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.isAuthenticated) {
  console.log('로그인 상태입니다.');
} else {
  console.log('로그아웃 상태입니다.');
}
```

---

### displayName

```typescript
displayName: ComputedRef<string>
```

사용자 표시 이름을 반환합니다.

**우선순위**: `nickname > username > name > email`

로그아웃 상태일 경우 `'Guest'` 반환.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

console.log(`환영합니다, ${authStore.displayName}님!`);
// 출력: 환영합니다, 홍길동님!
```

---

### isAdmin

```typescript
isAdmin: ComputedRef<boolean>
```

Admin 역할 여부를 반환합니다.

내부적으로 `hasRole('ROLE_ADMIN')` 호출.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.isAdmin) {
  console.log('관리자 권한이 있습니다.');
}
```

---

## 🔹 Methods

### hasRole

```typescript
hasRole(role: string): boolean
```

특정 역할(Role)을 가지고 있는지 확인합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `role` | string | ✅ | 확인할 역할 (예: `ROLE_ADMIN`, `ROLE_USER`) |

**Returns:** 역할을 가지고 있으면 `true`, 아니면 `false`

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.hasRole('ROLE_ADMIN')) {
  console.log('관리자입니다.');
}

if (authStore.hasRole('ROLE_USER')) {
  console.log('일반 사용자입니다.');
}
```

---

## 🔹 Actions

### setUser

```typescript
setUser(oidcUser: User): void
```

OIDC 클라이언트에서 받은 사용자 정보로 Store를 설정합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `oidcUser` | User (from oidc-client-ts) | ✅ | OIDC User 객체 |

**동작:**
1. JWT Access Token 파싱
2. UserProfile, UserAuthority, UserPreferences 생성
3. PortalUser 객체로 조립하여 저장

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';
import { UserManager } from 'oidc-client-ts';

const authStore = useAuthStore();
const userManager = new UserManager({ /* config */ });

userManager.signinRedirectCallback().then((oidcUser) => {
  authStore.setUser(oidcUser);
  console.log('로그인 성공!');
});
```

---

### logout

```typescript
logout(): void
```

사용자 정보를 초기화하고 로그아웃 처리합니다.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

authStore.logout();
console.log('로그아웃되었습니다.');
```

---

## 🔹 Remote 모듈에서 사용하기

### 1. Vue 3 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useAuthStore } from 'portal/stores';
import { computed } from 'vue';

const authStore = useAuthStore();

const isLoggedIn = computed(() => authStore.isAuthenticated);
const userName = computed(() => authStore.displayName);
const canEdit = computed(() => authStore.hasRole('ROLE_EDITOR'));
</script>

<template>
  <div>
    <p v-if="isLoggedIn">환영합니다, {{ userName }}님!</p>
    <p v-else>로그인이 필요합니다.</p>

    <button v-if="canEdit">게시물 수정</button>
  </div>
</template>
```

---

### 2. TypeScript 파일에서 사용

```typescript
// blog-frontend/src/composables/usePostPermission.ts
import { useAuthStore } from 'portal/stores';
import { computed } from 'vue';

export const usePostPermission = () => {
  const authStore = useAuthStore();

  const canCreate = computed(() => {
    return authStore.isAuthenticated;
  });

  const canEdit = computed(() => {
    return authStore.hasRole('ROLE_EDITOR') || authStore.hasRole('ROLE_ADMIN');
  });

  const canDelete = computed(() => {
    return authStore.hasRole('ROLE_ADMIN');
  });

  return {
    canCreate,
    canEdit,
    canDelete,
  };
};
```

---

### 3. 라우터 가드에서 사용

```typescript
// blog-frontend/src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from 'portal/stores';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/admin',
      component: () => import('../views/AdminPage.vue'),
      meta: { requiresAdmin: true }
    }
  ]
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    alert('관리자 권한이 필요합니다.');
    next('/');
  } else {
    next();
  }
});

export default router;
```

---

## 🔹 사용 예시

### 조건부 렌더링

```vue
<script setup lang="ts">
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();
</script>

<template>
  <div>
    <!-- 로그인 상태에 따라 다른 UI 표시 -->
    <div v-if="authStore.isAuthenticated">
      <h1>환영합니다, {{ authStore.displayName }}님!</h1>

      <!-- 역할별 기능 -->
      <button v-if="authStore.hasRole('ROLE_EDITOR')">
        글쓰기
      </button>

      <button v-if="authStore.isAdmin">
        관리자 페이지
      </button>

      <button @click="authStore.logout()">
        로그아웃
      </button>
    </div>

    <div v-else>
      <h1>로그인이 필요합니다.</h1>
      <button>로그인</button>
    </div>
  </div>
</template>
```

---

### 프로필 정보 표시

```vue
<script setup lang="ts">
import { useAuthStore } from 'portal/stores';
import { computed } from 'vue';

const authStore = useAuthStore();

const profile = computed(() => authStore.user?.profile);
const authority = computed(() => authStore.user?.authority);
</script>

<template>
  <div v-if="profile" class="user-profile">
    <img v-if="profile.picture" :src="profile.picture" alt="프로필" />

    <div class="info">
      <h2>{{ profile.nickname || profile.name }}</h2>
      <p>{{ profile.email }}</p>

      <div class="roles">
        <span v-for="role in authority?.roles" :key="role" class="badge">
          {{ role }}
        </span>
      </div>
    </div>
  </div>
</template>
```

---

## ⚠️ 주의사항

### 1. Remote 모듈에서 독자적인 Auth Store 생성 금지

```typescript
// ❌ 나쁜 예: Remote에서 독립된 auth store 생성
import { defineStore } from 'pinia';

export const useMyAuthStore = defineStore('myAuth', {
  // ...
});

// ✅ 좋은 예: Shell의 authStore 사용
import { useAuthStore } from 'portal/stores';
```

**이유**: Shell의 authStore를 사용해야 인증 상태가 전역적으로 동기화됨

---

### 2. 토큰 직접 접근 금지

```typescript
// ❌ 나쁜 예: 토큰에 직접 접근
const authStore = useAuthStore();
const token = authStore.user?._accessToken;

// ✅ 좋은 예: apiClient 사용 (자동으로 토큰 주입)
import { apiClient } from 'portal/api';
await apiClient.get('/api/v1/posts');
```

**이유**: `_accessToken`은 내부 관리용이며, apiClient가 자동으로 주입함

---

### 3. 사용자 정보 null 체크

```typescript
// ❌ 나쁜 예: null 체크 없이 접근
const email = authStore.user.profile.email; // 에러 가능

// ✅ 좋은 예: Optional chaining 사용
const email = authStore.user?.profile?.email;

// ✅ 또는 isAuthenticated로 먼저 확인
if (authStore.isAuthenticated) {
  const email = authStore.user.profile.email;
}
```

---

## 🔗 관련 문서

- [API Client](./api-client.md) - HTTP 요청 클라이언트
- [Theme Store API](./theme-store.md) - 테마 상태 관리

---

**최종 업데이트**: 2026-01-30
