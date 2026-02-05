---
id: api-portal-shell-auth-store
title: Portal Shell Auth Store
type: api
status: current
version: v2
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [api, portal-shell, pinia, auth, module-federation, jwt, social-login]
related:
  - api-portal-shell-api-client
  - api-portal-shell-store-adapter
---

# Portal Shell Auth Store

> Module Federation을 통해 Remote 모듈에 제공되는 인증 상태 관리 Pinia Store

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/stores` |
| **Export 이름** | `useAuthStore` |
| **Store 라이브러리** | Pinia |
| **Store ID** | `auth` |
| **주요 기능** | 로그인/로그아웃, 사용자 정보, 권한 확인, 멤버십 관리 |

---

## 🎯 주요 기능

### 1. 인증 처리
- 이메일/비밀번호 로그인 (`login`)
- 소셜 로그인 (`socialLogin` - Google, Naver, Kakao)
- 로그아웃 (`logout`)
- 인증 상태 확인 및 복원 (`checkAuth`)

### 2. 사용자 정보 관리
- PortalUser 타입의 사용자 정보 저장
- JWT 토큰 파싱 및 저장
- 토큰 갱신 (`updateAccessToken`)

### 3. 권한 확인
- 역할(Role) 기반 권한 확인 (`hasRole`, `hasAnyRole`)
- 시스템 관리자 확인 (`isAdmin`)
- 서비스별 관리자 확인 (`isServiceAdmin`)
- 판매자 여부 확인 (`isSeller`)

### 4. 멤버십 관리
- 서비스별 멤버십 티어 조회 (`getMembershipTier`)

### 5. UI 통합
- 로그인 모달 요청 (`requestLogin`)
- 사용자 표시 이름 제공 (`displayName`)

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
  roles: string[];                      // 역할 (SUPER_ADMIN, SERVICE_ADMIN:BLOG, SELLER, USER)
  scopes: string[];                     // OAuth2 Scope (read, write)
  memberships: Record<string, string>;  // 서비스별 멤버십 티어 (예: { blog: 'PREMIUM', shopping: 'FREE' })
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

### loading

```typescript
loading: boolean
```

로그인/로그아웃 처리 중 여부.

---

### showLoginModal

```typescript
showLoginModal: boolean
```

로그인 모달 표시 여부. `requestLogin()`으로 제어됨.

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

시스템 관리자 역할 여부를 반환합니다.

내부적으로 `hasAnyRole(['SUPER_ADMIN', 'ROLE_ADMIN'])` 호출.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.isAdmin) {
  console.log('시스템 관리자 권한이 있습니다.');
}
```

---

### isSeller

```typescript
isSeller: ComputedRef<boolean>
```

판매자 역할 여부를 반환합니다.

내부적으로 `hasRole('SELLER')` 호출.

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.isSeller) {
  console.log('판매자 권한이 있습니다.');
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
| `role` | string | ✅ | 확인할 역할 (예: `SUPER_ADMIN`, `SERVICE_ADMIN:BLOG`, `SELLER`, `USER`) |

**Returns:** 역할을 가지고 있으면 `true`, 아니면 `false`

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.hasRole('SUPER_ADMIN')) {
  console.log('슈퍼 관리자입니다.');
}

if (authStore.hasRole('SELLER')) {
  console.log('판매자입니다.');
}
```

---

### hasAnyRole

```typescript
hasAnyRole(roles: string[]): boolean
```

여러 역할 중 하나 이상을 가지고 있는지 확인합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `roles` | string[] | ✅ | 확인할 역할 목록 |

**Returns:** 하나 이상의 역할을 가지고 있으면 `true`, 아니면 `false`

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.hasAnyRole(['SUPER_ADMIN', 'SERVICE_ADMIN:BLOG'])) {
  console.log('블로그 관리 권한이 있습니다.');
}
```

---

### isServiceAdmin

```typescript
isServiceAdmin(service: string): boolean
```

특정 서비스의 관리자 권한이 있는지 확인합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `service` | string | ✅ | 서비스 이름 (예: `'blog'`, `'shopping'`) |

**Returns:** 해당 서비스의 관리자 또는 슈퍼 관리자이면 `true`, 아니면 `false`

**동작:**
- `SERVICE_ADMIN:{SERVICE}` 또는 `SUPER_ADMIN` 역할 확인

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

if (authStore.isServiceAdmin('blog')) {
  console.log('블로그 관리자입니다.');
}

if (authStore.isServiceAdmin('shopping')) {
  console.log('쇼핑몰 관리자입니다.');
}
```

---

### getMembershipTier

```typescript
getMembershipTier(service: string): string
```

특정 서비스의 멤버십 티어를 조회합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `service` | string | ✅ | 서비스 이름 (예: `'blog'`, `'shopping'`) |

**Returns:** 멤버십 티어 (예: `'FREE'`, `'PREMIUM'`, `'VIP'`)

**기본값:** 해당 서비스의 멤버십이 없으면 `'FREE'` 반환

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

const blogTier = authStore.getMembershipTier('blog');
console.log('블로그 멤버십:', blogTier);  // 'PREMIUM'

const shoppingTier = authStore.getMembershipTier('shopping');
console.log('쇼핑 멤버십:', shoppingTier);  // 'FREE'

// 조건부 기능 제공
if (authStore.getMembershipTier('blog') === 'PREMIUM') {
  console.log('프리미엄 기능 제공');
}
```

---

## 🔹 Actions

### login

```typescript
async login(email: string, password: string): Promise<void>
```

이메일과 비밀번호로 로그인합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `email` | string | ✅ | 사용자 이메일 |
| `password` | string | ✅ | 비밀번호 |

**동작:**
1. authService.login() 호출
2. JWT 토큰 파싱하여 사용자 정보 추출
3. PortalUser 생성 및 저장
4. 로그인 모달 닫기
5. redirectPath가 있으면 해당 경로로 이동
6. `portal:auth-changed` 이벤트 발생 (React 앱 동기화)

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

try {
  await authStore.login('user@example.com', 'password123');
  console.log('로그인 성공!');
} catch (error) {
  console.error('로그인 실패:', error);
}
```

---

### socialLogin

```typescript
socialLogin(provider: 'google' | 'naver' | 'kakao'): void
```

소셜 로그인 페이지로 리다이렉트합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `provider` | `'google'` \| `'naver'` \| `'kakao'` | ✅ | 소셜 로그인 제공자 |

**동작:**
- authService.socialLogin()을 호출하여 OAuth2 인증 페이지로 리다이렉트

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

// Google 로그인
authStore.socialLogin('google');

// Naver 로그인
authStore.socialLogin('naver');

// Kakao 로그인
authStore.socialLogin('kakao');
```

---

### logout

```typescript
async logout(): Promise<void>
```

사용자 정보를 초기화하고 로그아웃 처리합니다.

**동작:**
1. authService.logout() 호출
2. user를 null로 설정
3. `window.__PORTAL_ACCESS_TOKEN__` 삭제
4. `portal:auth-changed` 이벤트 발생 (React 앱 동기화)

**예시:**

```typescript
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

await authStore.logout();
console.log('로그아웃되었습니다.');
```

---

### checkAuth

```typescript
async checkAuth(): Promise<void>
```

인증 상태를 확인하고 토큰이 있으면 사용자 정보를 복원합니다.

**동작:**
1. authService.autoRefreshIfNeeded() 호출 (토큰 만료 시 갱신)
2. JWT에서 사용자 정보 추출
3. 유효한 토큰이 있으면 PortalUser 복원
4. 없으면 user를 null로 설정
5. `portal:auth-changed` 이벤트 발생

**사용 시점:**
- 앱 초기화 시 (main.ts)
- 페이지 새로고침 시

**예시:**

```typescript
// main.ts
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();
await authStore.checkAuth();  // 앱 시작 시 호출

app.mount('#app');
```

---

### updateAccessToken

```typescript
updateAccessToken(newAccessToken: string): void
```

프로필 또는 멤버십 변경 후 새로운 Access Token으로 갱신합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `newAccessToken` | string | ✅ | Backend에서 반환한 새 Access Token |

**사용 시점:**
- 프로필 수정 후 Backend가 새 토큰 반환
- 멤버십 업그레이드 후 Backend가 새 토큰 반환

**동작:**
1. 새 토큰으로 JWT 파싱
2. PortalUser 재생성
3. `window.__PORTAL_ACCESS_TOKEN__` 갱신

**예시:**

```typescript
import { apiClient } from 'portal/api';
import { useAuthStore } from 'portal/stores';

const authStore = useAuthStore();

// 프로필 수정
const response = await apiClient.put('/api/v1/users/profile', {
  nickname: 'NewNickname'
});

// Backend가 새 토큰 반환 시
const newToken = response.headers['x-new-access-token'];
if (newToken) {
  authStore.updateAccessToken(newToken);
}
```

---

### requestLogin

```typescript
requestLogin(path?: string): void
```

로그인 모달을 표시합니다. (Router Guard에서 사용)

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `path` | string | ❌ | 로그인 후 리다이렉트할 경로 |

**동작:**
1. `redirectPath`에 경로 저장
2. `showLoginModal`을 true로 설정

**예시:**

```typescript
// router/index.ts
import { useAuthStore } from 'portal/stores';

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    authStore.requestLogin(to.fullPath);  // 로그인 후 원래 경로로 이동
    next(false);
  } else {
    next();
  }
});
```

---

### setUser

```typescript
setUser(userInfo: UserInfo | null): void
```

외부 소스(OAuth2 callback 등)에서 받은 사용자 정보로 Store를 설정합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `userInfo` | `UserInfo \| null` | ✅ | authService의 UserInfo 객체 |

**동작:**
1. JWT Access Token 파싱
2. UserProfile, UserAuthority, UserPreferences 생성
3. PortalUser 객체로 조립하여 저장

**예시:**

```typescript
// OAuth2Callback.vue
import { useAuthStore } from 'portal/stores';
import { authService } from '@/services/authService';

const authStore = useAuthStore();

// OAuth2 callback 처리 후
const userInfo = authService.getUserInfo();
authStore.setUser(userInfo);
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
- [Store Adapter](./store-adapter.md) - React 통합용 Adapter

---

**최종 업데이트**: 2026-02-06

---

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | 2026-01-18 | 최초 작성 |
| v2 | 2026-02-06 | login/socialLogin/checkAuth/updateAccessToken/requestLogin 추가, hasAnyRole/isServiceAdmin/getMembershipTier 추가, isSeller getter 추가, memberships 필드 추가 |
