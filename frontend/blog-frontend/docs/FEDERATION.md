# Module Federation Configuration

## 개요

Blog Frontend는 Module Federation(Webpack 5의 기능, Vite에서는 `@originjs/vite-plugin-federation`으로 지원)을 통해 Portal Shell(Host)에 동적으로 로드되는 Remote 모듈입니다.

## Vite Federation 설정

### vite.config.ts

```typescript
import federation from "@originjs/vite-plugin-federation";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      federation({
        // Remote 이름
        name: 'blog',

        // 다른 Remote 참조
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL,
        },

        // 내보낸 모듈
        exposes: {
          './bootstrap': './src/bootstrap.ts'  // 부트스트랩 함수
        },

        // 공유 라이브러리
        shared: ['vue', 'pinia', 'axios']
      })
    ]
  }
});
```

### 환경 변수

`.env.local` 또는 `.env.dev`에서 다음을 설정합니다:

```env
# Portal Shell (Host)의 Remote Entry URL
VITE_PORTAL_SHELL_REMOTE_URL=http://localhost:30000

# Shopping Frontend (다른 Remote)의 Remote Entry URL
VITE_SHOPPING_REMOTE_URL=http://localhost:30002
```

### Build Output

Vite Federation은 빌드 시 다음 파일을 생성합니다:

```
dist/
├── remoteEntry.js              # Federation 진입점 (자동 생성)
├── __federation_expose_*       # 내보낸 모듈들
├── blog-frontend.js            # 메인 번들
├── blog-frontend.css           # 스타일
└── index.html
```

## 공유 라이브러리

### shared: ['vue', 'pinia', 'axios']

공유 라이브러리를 지정하면 중복을 피할 수 있습니다:

| 라이브러리 | 이유 | 영향 |
|----------|------|------|
| **vue** | 상태 공유, 컴포넌트 호환성 | Portal Shell과 동일 버전 사용 |
| **pinia** | 상태 관리 공유 (authStore) | 같은 인스턴스 사용 가능 |
| **axios** | API 클라이언트 공유 | 인터셉터, 기본 설정 공유 |

### 공유 라이브러리 사용 예

```typescript
// 1. Portal Shell의 authStore 접근
import { useAuthStore } from 'portal/authStore';
const authStore = useAuthStore();

// 2. Portal Shell의 apiClient 접근
import apiClient from 'portal/apiClient';
const { data } = await apiClient.get('/api/blog/posts');

// 3. 공유 라이브러리 직접 사용
import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
```

## 내보낸 모듈

### exposes: { './bootstrap': './src/bootstrap.ts' }

Blog Frontend가 외부(Portal Shell)에 제공하는 모듈입니다.

### Bootstrap 함수

```typescript
// src/bootstrap.ts

export type MountOptions = {
  initialPath?: string;           // 초기 경로
  onNavigate?: (path: string) => void;  // 경로 변경 콜백
};

export type BlogAppInstance = {
  router: Router;                 // Vue Router 인스턴스
  onParentNavigate: (path: string) => void;  // Parent 네비게이션 수신
  unmount: () => void;            // 앱 언마운트
};

export function mountBlogApp(
  el: HTMLElement,
  options: MountOptions = {}
): BlogAppInstance {
  // 앱 마운트 로직
}
```

### Portal Shell에서의 사용

```typescript
// Portal Shell (Host)
import { mountBlogApp } from 'blog/bootstrap';

// Blog 모듈 마운트
const blogContainer = document.getElementById('blog-container');
const blogApp = mountBlogApp(blogContainer, {
  initialPath: '/list',
  onNavigate: (path) => {
    // Blog 내부 네비게이션을 Host URL에 반영
    window.history.pushState({}, '', `/blog${path}`);
  }
});

// 나중에 언마운트 필요 시
blogApp.unmount();

// Parent에서 Blog로 네비게이션
blogApp.onParentNavigate('/123');  // 게시글 상세 페이지로
```

## Remote Entry (remoteEntry.js)

### 동적 로드

Portal Shell이 Runtime에 Remote Entry를 로드하는 방식:

```typescript
// Portal Shell (Host)
const script = document.createElement('script');
script.src = 'http://localhost:30001/remoteEntry.js';
script.onload = () => {
  // remoteEntry.js가 로드되면 window에 federation 정보 추가됨
  import('blog/bootstrap').then(m => {
    const { mountBlogApp } = m;
    const app = mountBlogApp(container);
  });
};
document.body.appendChild(script);
```

### 버전 관리

```typescript
// package.json의 버전을 자동으로 관리
"version": "0.0.0",

// Build 시 버전 정보가 remoteEntry.js에 포함됨
// Portal Shell에서 호환성 확인 가능
```

## 통신 (Host ↔ Remote)

### 1. Portal Shell → Blog 통신

#### 초기 설정
```typescript
window.__POWERED_BY_PORTAL_SHELL__ = true;  // Embedded 모드 플래그
```

#### 네비게이션
```typescript
blogApp.onParentNavigate(path);  // Blog 내부 라우터 업데이트
```

#### 상태 공유
```typescript
// authStore (공유)
const authStore = useAuthStore();
console.log(authStore.isAuthenticated);

// apiClient (공유)
const response = await apiClient.get('/api/blog/posts');
```

### 2. Blog → Portal Shell 통신

#### 네비게이션 콜백
```typescript
const blogApp = mountBlogApp(container, {
  onNavigate: (path) => {
    // Blog 내부 경로 변경 감지
    console.log('Blog navigated to:', path);
    // Host에서 필요한 처리
  }
});
```

#### 상태 변경 감지
```typescript
// authStore는 공유되므로 변경이 자동 동기화됨
const authStore = useAuthStore();
watch(
  () => authStore.isAuthenticated,
  (newVal) => {
    if (!newVal) {
      // 로그아웃되었을 때 처리
    }
  }
);
```

## CSS 격리 및 관리

### 문제점

Blog Frontend의 CSS가 Portal Shell에 오염될 수 있습니다:

```html
<!-- 문제: Blog의 <style> 태그가 <head>에 남음 -->
<head>
  <style>/* Portal CSS */</style>
  <style>/* Blog CSS */ [data-service="blog"] { ... }</style>  <!-- 남겨짐 -->
</head>
```

### 해결책

#### 1. bootstrap.ts에서 CSS 정리

```typescript
unmount: () => {
  // 1. Vue 앱 언마운트
  app.unmount();
  
  // 2. <style> 태그 정리
  const styleTags = document.querySelectorAll('style');
  styleTags.forEach((tag) => {
    const content = tag.textContent || '';
    if (content.includes('[data-service="blog"]') ||
        content.includes('blog-')) {
      tag.remove();
    }
  });
  
  // 3. <link> 태그 정리
  const linkTags = document.querySelectorAll('link[rel="stylesheet"]');
  linkTags.forEach((tag) => {
    const href = tag.getAttribute('href') || '';
    if (href.includes('blog')) {
      tag.remove();
    }
  });
  
  // 4. data-service 속성 정리
  if (document.documentElement.getAttribute('data-service') === 'blog') {
    document.documentElement.removeAttribute('data-service');
  }
}
```

#### 2. Scoped CSS 사용

```vue
<style scoped>
/* 자동으로 Blog 컴포넌트로 범위 제한됨 */
.post-card { ... }
</style>
```

#### 3. CSS Modules

```vue
<script setup>
import styles from './PostCard.module.css';
</script>

<template>
  <div :class="styles.card">...</div>
</template>
```

## 환경별 빌드

### Dev 모드

```bash
npm run build:dev
```

```typescript
// vite.config.ts (mode === 'dev')
remotes: {
  portal: 'http://localhost:30000',
  shopping: 'http://localhost:30002',
}
```

### Docker 모드

```bash
npm run build:docker
```

```env
# .env.docker
VITE_PORTAL_SHELL_REMOTE_URL=http://portal-shell:8080
VITE_SHOPPING_REMOTE_URL=http://shopping-frontend:8080
```

### Kubernetes 모드

```bash
npm run build:k8s
```

```env
# .env.k8s
VITE_PORTAL_SHELL_REMOTE_URL=https://portal-shell.example.com
VITE_SHOPPING_REMOTE_URL=https://shopping-frontend.example.com
```

## 트러블슈팅

### 1. "Cannot find module 'portal/apiClient'"

**원인**: Portal Shell이 아직 로드되지 않음

**해결책**:
```typescript
// bootstrap.ts에서만 Portal 모듈 import
// main.ts (Standalone 모드)에서는 import 안 함

if (isEmbedded) {
  // Portal 모듈 사용 가능
  const authStore = useAuthStore();
} else {
  // Portal 모듈 사용 불가 (에러 발생)
}
```

### 2. "remoteEntry.js not found"

**원인**: Remote가 실행되지 않았거나 URL이 잘못됨

**해결책**:
```bash
# 1. Blog Frontend 개발 서버 실행
cd frontend/blog-frontend
npm run dev

# 2. Console에서 네트워크 탭 확인
# http://localhost:30001/remoteEntry.js 로드 확인
```

### 3. "Shared dependency 'vue' not satisfied"

**원인**: Host와 Remote의 vue 버전 불일치

**해결책**:
```json
// frontend/blog-frontend/package.json
"vue": "^3.5.21",  // Portal Shell과 동일 버전
```

### 4. CSS가 제거되지 않음

**원인**: CSS 선택자가 정확하지 않음

**해결책**: bootstrap.ts의 unmount 함수에서 CSS 선택자 추가

```typescript
styleTags.forEach((tag) => {
  const content = tag.textContent || '';
  if (content.includes('blog-') ||              // Blog 특화
      content.includes('toastui-editor') ||     // Toast UI
      content.includes('prism')) {               // Prism.js
    tag.remove();
  }
});
```

## 성능 최적화

### 1. 공유 라이브러리 최적화

```typescript
// 무겁지만 자주 사용되는 것만 shared로 설정
shared: {
  'vue': { singleton: true },  // 단일 인스턴스
  'pinia': { singleton: true },
  'axios': { singleton: true },
  '@portal/design-system': { singleton: true },  // 추가 공유 가능
}
```

### 2. 번들 크기 최적화

```bash
# 번들 분석
npm run build:analyze

# 불필요한 의존성 제거
npm list --depth=0
```

### 3. Remote Entry 캐싱

```typescript
// Portal Shell에서
const remoteURL = 'http://localhost:30001/remoteEntry.js?v=' + Date.now();
// v 파라미터로 캐시 무효화
```

## 관련 문서

- [README.md](./README.md) - 모듈 개요
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
- [COMPONENTS.md](./COMPONENTS.md) - 컴포넌트 가이드
