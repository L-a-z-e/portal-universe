# Portal Shell API 문서

> Module Federation을 통해 Remote 모듈에 제공되는 API 명세

---

## 📋 개요

Portal Shell은 Module Federation을 통해 다음 API를 Remote 모듈에 노출합니다:

| Export | 설명 | 문서 |
|--------|------|------|
| `./api` | API Client 및 유틸리티 함수 | [api-client.md](./api-client.md), [api-utils.md](./api-utils.md) |
| `./stores` | Pinia Stores + React용 Adapter | [auth-store.md](./auth-store.md), [theme-store.md](./theme-store.md), [store-adapter.md](./store-adapter.md) |

---

## 📚 API 목록

### [API Client](./api-client.md)

HTTP 요청을 위한 사전 구성된 Axios 인스턴스.

**주요 기능:**
- 자동 Bearer Token 주입 및 자동 갱신
- 401 응답 시 토큰 refresh 후 재시도
- 429 Rate Limit 재시도 (최대 3회)
- Base URL 환경변수 기반 설정
- 10초 Timeout

**Import:**
```typescript
import { apiClient } from 'portal/api';
```

---

### [API Utils](./api-utils.md)

API 응답 및 에러 처리 유틸리티 함수.

**주요 기능:**
- `getData` - ApiResponse에서 data 추출
- `getErrorDetails` - Backend 에러 정보 추출
- `getErrorMessage` - 사용자 친화적 에러 메시지
- `getErrorCode` - 에러 코드 추출

**Import:**
```typescript
import { getData, getErrorDetails, getErrorMessage, getErrorCode } from 'portal/api';
```

---

### [Auth Store](./auth-store.md)

사용자 인증 및 권한 관리를 위한 Pinia Store.

**주요 기능:**
- 로그인 상태 확인 (`isAuthenticated`)
- 사용자 정보 조회 (`user`, `displayName`)
- 역할 기반 권한 확인 (`hasRole`, `hasAnyRole`, `isServiceAdmin`)
- 로그인/로그아웃 처리 (`login`, `socialLogin`, `logout`)
- 멤버십 티어 조회 (`getMembershipTier`)

**Import:**
```typescript
import { useAuthStore } from 'portal/stores';
```

---

### [Theme Store](./theme-store.md)

Light/Dark/System 모드 전환을 위한 Pinia Store.

**주요 기능:**
- 다크 모드 전환 (`toggle`, `setMode`)
- 시스템 테마 자동 감지
- 테마 설정 영속화 (localStorage)
- 앱 시작 시 테마 복원 (`initialize`)

**Import:**
```typescript
import { useThemeStore } from 'portal/stores';
```

---

### [Store Adapter](./store-adapter.md)

React 등 Vue 외 프레임워크에서 Pinia Store를 사용하기 위한 Adapter.

**주요 기능:**
- `themeAdapter` - React useSyncExternalStore 호환
- `authAdapter` - React useSyncExternalStore 호환
- `portalStoreAdapter` - 통합 Adapter

**Import:**
```typescript
import { themeAdapter, authAdapter, portalStoreAdapter } from 'portal/stores';
```

---

## 🚀 빠른 시작

### 1. Vue Remote에서 API 사용

```typescript
// blog-frontend/src/api/blogApi.ts
import { apiClient, getData } from 'portal/api';
import { useAuthStore } from 'portal/stores';

export const getPosts = async () => {
  const authStore = useAuthStore();

  if (!authStore.isAuthenticated) {
    throw new Error('로그인이 필요합니다.');
  }

  const response = await apiClient.get('/api/v1/blog/posts');
  return getData(response);  // ApiResponse<T>에서 data 추출
};
```

---

### 2. Vue 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useAuthStore, useThemeStore } from 'portal/stores';
import { apiClient } from 'portal/api';
import { ref, onMounted } from 'vue';

const authStore = useAuthStore();
const themeStore = useThemeStore();
const posts = ref([]);

onMounted(async () => {
  themeStore.initialize();

  if (authStore.isAuthenticated) {
    const response = await apiClient.get('/api/v1/blog/posts');
    posts.value = response.data.data.content;
  }
});
</script>

<template>
  <div>
    <button @click="themeStore.toggle()">
      {{ themeStore.isDark ? '🌙' : '☀️' }}
    </button>

    <div v-if="authStore.isAuthenticated">
      <h1>환영합니다, {{ authStore.displayName }}님!</h1>

      <ul>
        <li v-for="post in posts" :key="post.id">
          {{ post.title }}
        </li>
      </ul>
    </div>

    <div v-else>
      <p>로그인이 필요합니다.</p>
    </div>
  </div>
</template>
```

---

### 3. React Remote에서 Store 사용

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
  };
}
```

---

## ⚙️ vite.config.ts 설정

Remote 모듈에서 Portal Shell의 API를 사용하려면 Module Federation 설정이 필요합니다.

### Vue 3 Remote (blog-frontend)

```typescript
// blog-frontend/vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'blog',
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.ts',
      },
      remotes: {
        portal: 'http://localhost:30000/assets/shellEntry.js',
      },
      shared: ['vue', 'pinia', 'axios'],
    }),
  ],
});
```

### React 18 Remote (shopping-frontend)

```typescript
// shopping-frontend/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shopping',
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.tsx',
      },
      remotes: {
        portal: 'http://localhost:30000/assets/shellEntry.js',
      },
      shared: ['react', 'react-dom', 'react-dom/client', 'axios'],
    }),
  ],
});
```

---

## 🔗 관련 문서

### Module Federation 가이드
- [Module Federation 개요](../../architecture/portal-shell/module-federation.md)
- [Module Federation 통합 가이드](../../guides/development/module-federation-guide.md)

### 아키텍처 문서
- [프론트엔드 아키텍처](../../architecture/portal-shell/system-overview.md)

---

## 📝 문서 버전 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | 2026-01-18 | 최초 작성 |
| v2 | 2026-02-06 | Module Federation 경로 수정 (./api, ./stores), API Utils 추가, Store Adapter 추가 |

---

**최종 업데이트**: 2026-02-06
