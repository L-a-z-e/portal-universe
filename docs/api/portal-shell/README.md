# Portal Shell API 문서

> Module Federation을 통해 Remote 모듈에 제공되는 API 명세

---

## 📋 개요

Portal Shell은 Module Federation을 통해 다음 API를 Remote 모듈에 노출합니다:

| Export | 설명 | 문서 |
|--------|------|------|
| `./apiClient` | Axios 인스턴스 (자동 인증 토큰 주입) | [api-client.md](./api-client.md) |
| `./authStore` | 인증 상태 관리 Pinia Store | [auth-store.md](./auth-store.md) |
| `./themeStore` | 테마 상태 관리 Pinia Store | [theme-store.md](./theme-store.md) |

---

## 📚 API 목록

### [API Client](./api-client.md)

HTTP 요청을 위한 사전 구성된 Axios 인스턴스.

**주요 기능:**
- 자동 Bearer Token 주입 (Request Interceptor)
- 401 응답 시 자동 로그아웃 (Response Interceptor)
- Base URL 환경변수 기반 설정
- 10초 Timeout

**Import:**
```typescript
import apiClient from 'portal-shell/apiClient';
```

---

### [Auth Store](./auth-store.md)

사용자 인증 및 권한 관리를 위한 Pinia Store.

**주요 기능:**
- 로그인 상태 확인 (`isAuthenticated`)
- 사용자 정보 조회 (`user`, `displayName`)
- 역할 기반 권한 확인 (`hasRole`, `isAdmin`)
- 로그인/로그아웃 처리

**Import:**
```typescript
import { useAuthStore } from 'portal-shell/authStore';
```

---

### [Theme Store](./theme-store.md)

Light/Dark 모드 전환을 위한 Pinia Store.

**주요 기능:**
- 다크 모드 전환 (`toggle`)
- 테마 설정 영속화 (localStorage)
- 앱 시작 시 테마 복원 (`initialize`)

**Import:**
```typescript
import { useThemeStore } from 'portal-shell/themeStore';
```

---

## 🚀 빠른 시작

### 1. Remote 모듈에서 API 사용

```typescript
// blog-frontend/src/api/blogApi.ts
import apiClient from 'portal-shell/apiClient';
import { useAuthStore } from 'portal-shell/authStore';

export const getPosts = async () => {
  const authStore = useAuthStore();

  if (!authStore.isAuthenticated) {
    throw new Error('로그인이 필요합니다.');
  }

  const response = await apiClient.get('/api/v1/blog/posts');
  return response.data;
};
```

---

### 2. Vue 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useAuthStore } from 'portal-shell/authStore';
import { useThemeStore } from 'portal-shell/themeStore';
import apiClient from 'portal-shell/apiClient';
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

## ⚙️ vite.config.ts 설정

Remote 모듈에서 Portal Shell의 API를 사용하려면 Module Federation 설정이 필요합니다.

```typescript
// blog-frontend/vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'blog-frontend',
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.ts',
      },
      remotes: {
        'portal-shell': 'http://localhost:30000/assets/remoteEntry.js',
      },
      shared: {
        vue: {
          singleton: true,
        },
        pinia: {
          singleton: true,
        },
        'vue-router': {
          singleton: true,
        },
      },
    }),
  ],
});
```

---

## 🔗 관련 문서

### Module Federation 가이드
- [Module Federation 개요](../../architecture/portal-shell/module-federation.md)
- [Remote 모듈 개발 가이드](../../guides/development/adding-remote.md)

### 아키텍처 문서
- [프론트엔드 아키텍처](../../architecture/portal-shell/system-overview.md)

---

## 📝 문서 버전 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | 2026-01-18 | 최초 작성 |

---

**최종 업데이트**: 2026-01-18
