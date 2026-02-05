# 🔌 Module Federation

> Micro Frontend 아키텍처와 Module Federation을 학습합니다.

**난이도**: ⭐⭐⭐⭐ (고급)
**학습 시간**: 60분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Micro Frontend 개념 이해하기
- [ ] Module Federation 동작 원리 파악하기
- [ ] Host와 Remote 구조 이해하기
- [ ] 의존성 공유 메커니즘 이해하기
- [ ] Portal Shell과의 통합 방법 알기

---

## 1️⃣ Micro Frontend란?

### 기존 Monolith Frontend

```
┌──────────────────────────────┐
│      Single Frontend App      │
│                              │
│  ┌─────────┬─────────┬────┐  │
│  │  Blog   │ Shopping│Auth│  │
│  └─────────┴─────────┴────┘  │
│                              │
│  - 하나의 배포 단위           │
│  - 하나의 저장소              │
│  - 전체 빌드 필요             │
└──────────────────────────────┘
```

### Micro Frontend

```
┌──────────────────────────────┐
│      Portal Shell (Host)     │
│                              │
│  ┌─────────┐ ┌─────────┐    │
│  │  Blog   │ │ Shopping│    │
│  │ (Vue 3) │ │(React 18)│   │
│  └─────────┘ └─────────┘    │
│                              │
│  - 독립 배포                  │
│  - 독립 저장소                │
│  - 독립 개발팀                │
└──────────────────────────────┘
```

### 장점

- ✅ **독립 배포**: 각 팀이 독립적으로 배포
- ✅ **기술 자유**: Vue, React, Angular 혼용 가능
- ✅ **팀 확장성**: 팀별로 독립 개발
- ✅ **점진적 마이그레이션**: 레거시를 점진적으로 교체

### 단점

- ❌ **복잡도 증가**: 설정과 관리 복잡
- ❌ **중복 의존성**: 여러 버전의 라이브러리
- ❌ **런타임 오버헤드**: 네트워크 요청 증가

---

## 2️⃣ Module Federation 개념

### Webpack Module Federation

Module Federation은 Webpack 5에서 도입된 기능으로, 여러 독립적인 빌드가 **런타임에 코드를 공유**할 수 있게 합니다.

### 핵심 용어

**Host (Shell)**
- 다른 앱을 불러오는 주 애플리케이션
- Portal Shell이 Host 역할

**Remote**
- Host에 의해 로드되는 독립 애플리케이션
- Shopping Frontend, Blog Frontend가 Remote 역할

**Exposes**
- Remote가 외부에 노출하는 모듈

**Remotes**
- Host가 사용하는 Remote 앱 목록

**Shared**
- Host와 Remote가 공유하는 의존성

---

## 3️⃣ Shopping Frontend 설정

### Vite 설정

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shopping',  // Remote 이름
      filename: 'remoteEntry.js',  // 진입점 파일명

      // 외부에 노출할 모듈
      exposes: {
        './App': './src/bootstrap'  // bootstrap.tsx 노출
      },

      // Host와 공유할 의존성
      shared: [
        'react',
        'react-dom',
        'react-router-dom'
      ]
    })
  ],

  build: {
    target: 'esnext',
    minify: false,
    cssCodeSplit: false
  }
});
```

### Entry Points

```tsx
// src/index.tsx
// 비동기 로드를 위한 동적 import
import('./bootstrap');

// src/bootstrap.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Portal에서 호출할 mount 함수
export function mount(
  el: HTMLElement,
  portalContext?: {
    apiClient?: any;
    authStore?: any;
    theme?: string;
  }
) {
  const root = ReactDOM.createRoot(el);

  root.render(
    <React.StrictMode>
      <App
        apiClient={portalContext?.apiClient}
        authStore={portalContext?.authStore}
      />
    </React.StrictMode>
  );

  // Unmount 함수 반환
  return () => {
    root.unmount();
  };
}

// Standalone 모드 지원 (개발 시)
if (import.meta.env.DEV && document.getElementById('root')) {
  mount(document.getElementById('root')!);
}

// src/main.tsx (Standalone 전용)
import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
```

---

## 4️⃣ Portal Shell 설정

### Host 설정

```typescript
// portal-shell/vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'portal-shell',

      // Remote 앱 목록
      remotes: {
        blog: 'http://localhost:30001/assets/remoteEntry.js',
        shopping: 'http://localhost:30002/assets/remoteEntry.js'
      },

      // Host도 모듈 노출 가능
      exposes: {
        './apiClient': './src/api/client',
        './authStore': './src/stores/authStore'
      },

      // 의존성 공유
      shared: [
        'vue',
        'vue-router',
        'pinia',
        'axios'
      ]
    })
  ]
});
```

### Remote 로드

```vue
<!-- portal-shell/src/pages/ShoppingPage.vue -->
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { apiClient } from '@/api/client';

const containerRef = ref<HTMLElement | null>(null);
const authStore = useAuthStore();
let unmount: (() => void) | null = null;

onMounted(async () => {
  if (!containerRef.value) return;

  try {
    // Remote 동적 import
    const module = await import('shopping/App');

    // mount 함수 호출
    unmount = module.mount(containerRef.value, {
      apiClient,
      authStore,
      theme: 'shopping'
    });
  } catch (error) {
    console.error('Failed to load shopping app:', error);
  }
});

onUnmounted(() => {
  // 컴포넌트 언마운트 시 정리
  if (unmount) {
    unmount();
  }
});
</script>

<template>
  <div ref="containerRef" class="shopping-container"></div>
</template>
```

---

## 5️⃣ 의존성 공유

### Singleton 패턴

```typescript
// vite.config.ts
shared: {
  react: {
    singleton: true,  // 하나의 인스턴스만 사용
    requiredVersion: '^18.3.1'
  },
  'react-dom': {
    singleton: true,
    requiredVersion: '^18.3.1'
  }
}
```

### 버전 충돌 처리

```
Host: React 18.3.1
Remote A: React 18.3.1  → ✅ Host의 React 사용
Remote B: React 18.2.0  → ⚠️ 호환 가능하면 Host 버전 사용
Remote C: React 17.0.0  → ❌ 자체 버전 로드 (fallback)
```

### 공유 전략

```typescript
shared: {
  // 전략 1: 모두 공유
  react: {
    singleton: true,
    eager: true  // 즉시 로드
  },

  // 전략 2: 선택적 공유
  lodash: {
    singleton: false,  // 각자 사용
    requiredVersion: false  // 버전 체크 안함
  },

  // 전략 3: 버전 범위 지정
  axios: {
    singleton: true,
    requiredVersion: '^1.0.0'
  }
}
```

---

## 6️⃣ 컨텍스트 공유

### API Client 주입

```tsx
// portal-shell이 제공하는 apiClient
// shopping-frontend/src/App.tsx
interface AppProps {
  apiClient?: any;
  authStore?: any;
}

function App({ apiClient, authStore }: AppProps) {
  // Portal의 apiClient 사용 또는 기본값
  const api = apiClient || createDefaultApiClient();

  return (
    <ApiClientContext.Provider value={api}>
      <RouterProvider router={router} />
    </ApiClientContext.Provider>
  );
}

// shopping-frontend/src/hooks/useApi.ts
import { useContext } from 'react';
import { ApiClientContext } from '@/contexts/ApiClientContext';

export function useApi() {
  return useContext(ApiClientContext);
}

// 컴포넌트에서 사용
function ProductList() {
  const api = useApi();

  useEffect(() => {
    api.get('/products').then(setProducts);
  }, []);

  // ...
}
```

### Auth Store 공유

```tsx
// shopping-frontend/src/stores/authStore.ts
import { create } from 'zustand';

// Portal에서 주입받거나 자체 생성
let sharedAuthStore: any = null;

export function initAuthStore(portalAuthStore?: any) {
  if (portalAuthStore) {
    sharedAuthStore = portalAuthStore;
  } else {
    sharedAuthStore = create((set) => ({
      user: null,
      isAuthenticated: false,
      login: (user) => set({ user, isAuthenticated: true }),
      logout: () => set({ user: null, isAuthenticated: false })
    }));
  }
  return sharedAuthStore;
}

export function useAuthStore() {
  if (!sharedAuthStore) {
    throw new Error('Auth store not initialized');
  }
  return sharedAuthStore();
}

// bootstrap.tsx에서 초기화
export function mount(el: HTMLElement, portalContext?: any) {
  initAuthStore(portalContext?.authStore);

  // ...
}
```

---

## 7️⃣ 개발과 배포

### 개발 모드

```bash
# Terminal 1: Portal Shell
cd frontend/portal-shell
pnpm dev  # http://localhost:30000

# Terminal 2: Shopping Frontend
cd frontend/shopping-frontend
pnpm dev  # http://localhost:30002

# Terminal 3: Blog Frontend
cd frontend/blog-frontend
pnpm dev  # http://localhost:30001
```

### Standalone 모드

```bash
# Shopping Frontend만 독립 실행
cd frontend/shopping-frontend
pnpm dev

# 브라우저에서 http://localhost:30002 접속
# Portal 없이 단독으로 개발 가능
```

### 프로덕션 빌드

```bash
# 1. Remote 앱들 빌드
cd frontend/shopping-frontend
pnpm build  # dist/ 생성

cd frontend/blog-frontend
pnpm build

# 2. Host 빌드
cd frontend/portal-shell
pnpm build

# 3. 배포
# Remote: dist/assets/remoteEntry.js를 CDN에 배포
# Host: dist/를 웹 서버에 배포
```

### 환경별 Remote URL

```typescript
// portal-shell/vite.config.ts
const remoteBaseUrl = process.env.NODE_ENV === 'production'
  ? 'https://cdn.example.com'
  : 'http://localhost';

export default defineConfig({
  plugins: [
    federation({
      remotes: {
        shopping: `${remoteBaseUrl}:30002/assets/remoteEntry.js`,
        blog: `${remoteBaseUrl}:30001/assets/remoteEntry.js`
      }
    })
  ]
});
```

---

## 8️⃣ 트러블슈팅

### 문제 1: 의존성 중복

**증상**: React가 두 번 로드됨

**해결**:
```typescript
// singleton: true 설정
shared: {
  react: { singleton: true },
  'react-dom': { singleton: true }
}
```

### 문제 2: Remote 로드 실패

**증상**: "Failed to fetch dynamically imported module"

**해결**:
```typescript
// 1. CORS 설정 확인
// 2. Remote URL이 올바른지 확인
// 3. remoteEntry.js가 빌드되었는지 확인
```

### 문제 3: Type 에러

**증상**: Remote 모듈의 타입을 찾을 수 없음

**해결**:
```typescript
// shopping-frontend/src/types/federation.d.ts
declare module 'shopping/App' {
  export function mount(
    el: HTMLElement,
    context?: any
  ): () => void;
}
```

### 문제 4: 개발 시 HMR 안됨

**증상**: 변경사항이 즉시 반영되지 않음

**해결**:
```bash
# 각 앱을 별도로 실행하고 새로고침
# 또는 hmr: false 설정
```

---

## ✍️ 실습 과제

### 과제 1: Standalone 모드 확인 (기초)

Shopping Frontend를 Standalone 모드로 실행해보세요:

```bash
# 요구사항:
# 1. pnpm dev로 실행
# 2. http://localhost:30002 접속
# 3. Portal 없이 동작하는지 확인
# 4. 라우팅이 정상 작동하는지 확인
```

### 과제 2: Context 주입 확인 (중급)

Portal Shell에서 Shopping Frontend로 데이터를 전달해보세요:

```tsx
// 요구사항:
// 1. Portal Shell에서 theme 전달
// 2. Shopping Frontend에서 theme 받아서 적용
// 3. console.log로 주입된 값 확인
```

### 과제 3: 새 Remote 추가 (고급)

새로운 Micro Frontend 앱을 추가해보세요:

```
# 요구사항:
# 1. notification-frontend 생성
# 2. vite.config.ts 설정
# 3. Portal Shell에 등록
# 4. /notifications 경로에서 로드
# 5. 알림 목록 표시
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Micro Frontend의 개념을 이해한다
- [ ] Module Federation의 동작 원리를 안다
- [ ] Host와 Remote의 역할을 이해한다
- [ ] 의존성 공유 메커니즘을 안다
- [ ] mount/unmount 함수의 역할을 이해한다
- [ ] Standalone 모드와 통합 모드의 차이를 안다
- [ ] 개발과 배포 프로세스를 이해한다

---

## 📚 추가 학습

### 다음 단계

1. **성능 최적화**
   - Code Splitting
   - Lazy Loading
   - Preloading

2. **에러 처리**
   - Remote 로드 실패 시 Fallback
   - Error Boundary

3. **테스트**
   - Remote 앱 단위 테스트
   - 통합 테스트

4. **CI/CD**
   - 독립 배포 파이프라인
   - Remote 버전 관리

---

**이전**: [← 스타일링 (Tailwind CSS)](./06-styling.md)
**완료**: [학습 가이드 홈으로 →](./README.md)
