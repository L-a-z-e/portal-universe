# Module Federation Host (Vue 3)

## 학습 목표
- Vite Plugin Federation을 활용한 Host 앱 구성 이해
- Remote 앱 로딩 및 마운트 방식 학습
- Host에서 공유하는 리소스 (API Client, Store) 이해

---

## 1. Module Federation 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PORTAL UNIVERSE MFE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                    PORTAL SHELL (HOST)                              │    │
│   │                       Vue 3 + Pinia                                 │    │
│   │                       Port: 30000                                   │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │                                                                     │    │
│   │   EXPOSES:                          REMOTES:                        │    │
│   │   ├── ./api (apiClient)             ├── blog → :30001               │    │
│   │   └── ./stores (authStore, theme)   └── shopping → :30002           │    │
│   │                                                                     │    │
│   │   SHARED: vue, pinia, axios                                         │    │
│   │                                                                     │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                        │                      │                              │
│                        ▼                      ▼                              │
│   ┌───────────────────────────┐   ┌───────────────────────────┐            │
│   │     BLOG (REMOTE)         │   │   SHOPPING (REMOTE)       │            │
│   │       Vue 3               │   │      React 18             │            │
│   │     Port: 30001           │   │    Port: 30002            │            │
│   ├───────────────────────────┤   ├───────────────────────────┤            │
│   │ EXPOSES:                  │   │ EXPOSES:                  │            │
│   │ └── ./bootstrap           │   │ └── ./bootstrap           │            │
│   │                           │   │                           │            │
│   │ SHARED: vue, pinia        │   │ SHARED: react, react-dom  │            │
│   └───────────────────────────┘   └───────────────────────────┘            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Vite 설정 분석

### 2.1 vite.config.ts

```typescript
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation"

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      federation({
        // Host 앱 이름
        name: 'portal',

        // 빌드 결과물 파일명
        filename: 'shellEntry.js',

        // Remote 앱 URL (환경 변수로 관리)
        remotes: {
          blog: env.VITE_BLOG_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL,
        },

        // Host가 제공하는 모듈 (Remote에서 import 가능)
        exposes: {
          './api': './src/api/index.ts',
          './stores': './src/store/index.ts',
        },

        // 공유 라이브러리 (중복 로딩 방지)
        shared: ['vue', 'pinia', 'axios'],
      })
    ],

    server: {
      port: 30000,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        }
      }
    },

    build: {
      minify: false,
      target: 'esnext',
    }
  }
})
```

### 2.2 환경 변수 (.env)

```bash
# Remote 앱 URL
VITE_BLOG_REMOTE_URL=http://localhost:30001/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://localhost:30002/assets/remoteEntry.js

# API Gateway
VITE_API_BASE_URL=http://localhost:8080
```

---

## 3. Remote 앱 로딩

### 3.1 동적 Import

```typescript
// Remote 앱은 런타임에 동적으로 로딩
const BlogApp = defineAsyncComponent(() =>
  import('blog/bootstrap').then(m => m.default)
)

const ShoppingApp = defineAsyncComponent(() =>
  import('shopping/bootstrap')
)
```

### 3.2 Remote 앱 마운트

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// Remote 앱 인스턴스 타입
interface RemoteAppInstance {
  onParentNavigate: (path: string) => void
  unmount: () => void
  onActivated?: () => void
  onDeactivated?: () => void
  onThemeChange?: (theme: 'light' | 'dark') => void
}

const containerRef = ref<HTMLElement | null>(null)
let appInstance: RemoteAppInstance | null = null

onMounted(async () => {
  if (!containerRef.value) return

  // 동적 import로 Remote 앱 로딩
  const { mountShoppingApp } = await import('shopping/bootstrap')

  // 마운트 옵션 전달
  appInstance = mountShoppingApp(containerRef.value, {
    initialPath: '/products',
    theme: 'light',
    onNavigate: (path) => {
      // Remote에서 발생한 네비게이션을 Host 라우터에 반영
      router.push(`/shopping${path}`)
    }
  })
})

onUnmounted(() => {
  // 정리 작업
  appInstance?.unmount()
})
</script>

<template>
  <div ref="containerRef" class="remote-app-container" />
</template>
```

---

## 4. Host가 제공하는 리소스

### 4.1 API Client (./api)

```typescript
// src/api/index.ts
import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'

// 공유 axios 인스턴스
export const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 요청 인터셉터: JWT 토큰 자동 첨부
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 응답 인터셉터: 토큰 갱신, 에러 처리
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 토큰 갱신 로직
    }
    return Promise.reject(error)
  }
)

export default apiClient
```

### 4.2 Store Adapter (./stores)

```typescript
// src/store/storeAdapter.ts
import { useAuthStore } from './auth'
import { useThemeStore } from './theme'

export interface AuthState {
  isAuthenticated: boolean
  user: User | null
  accessToken: string | null
}

export interface ThemeState {
  mode: 'light' | 'dark'
}

export type UnsubscribeFn = () => void

/**
 * Auth Store Adapter
 * React 등 비-Vue 앱에서 Pinia 스토어 사용 가능
 */
export const authAdapter = {
  getState: (): AuthState => {
    const store = useAuthStore()
    return {
      isAuthenticated: store.isAuthenticated,
      user: store.user,
      accessToken: store.accessToken
    }
  },

  subscribe: (callback: (state: AuthState) => void): UnsubscribeFn => {
    const store = useAuthStore()
    return store.$subscribe((mutation, state) => {
      callback({
        isAuthenticated: state.isAuthenticated,
        user: state.user,
        accessToken: state.accessToken
      })
    })
  },

  actions: {
    login: (credentials: LoginRequest) => useAuthStore().login(credentials),
    logout: () => useAuthStore().logout(),
    refreshToken: () => useAuthStore().refreshToken()
  }
}

/**
 * Theme Store Adapter
 */
export const themeAdapter = {
  getState: (): ThemeState => {
    const store = useThemeStore()
    return { mode: store.mode }
  },

  subscribe: (callback: (state: ThemeState) => void): UnsubscribeFn => {
    const store = useThemeStore()
    return store.$subscribe((mutation, state) => {
      callback({ mode: state.mode })
    })
  },

  actions: {
    toggle: () => useThemeStore().toggle(),
    setMode: (mode: 'light' | 'dark') => useThemeStore().setMode(mode)
  }
}
```

---

## 5. Keep-Alive 지원

### 5.1 Vue KeepAlive와 Remote 앱

```vue
<template>
  <KeepAlive>
    <component
      :is="currentRemoteComponent"
      :key="remoteKey"
      @activated="handleActivated"
      @deactivated="handleDeactivated"
    />
  </KeepAlive>
</template>

<script setup lang="ts">
const handleActivated = () => {
  // Remote 앱에게 활성화 알림
  appInstance?.onActivated?.()
}

const handleDeactivated = () => {
  // Remote 앱에게 비활성화 알림
  appInstance?.onDeactivated?.()
}
</script>
```

### 5.2 테마 동기화

```typescript
// Host에서 테마 변경 시 Remote에 전파
watch(() => themeStore.mode, (newTheme) => {
  appInstance?.onThemeChange?.(newTheme)
})
```

---

## 6. 에러 처리

### 6.1 Remote 로딩 실패 처리

```vue
<template>
  <Suspense>
    <template #default>
      <RemoteApp />
    </template>
    <template #fallback>
      <LoadingSpinner message="앱 로딩 중..." />
    </template>
  </Suspense>

  <ErrorBoundary v-if="loadError">
    <div class="error-container">
      <p>원격 앱을 불러올 수 없습니다.</p>
      <button @click="retry">다시 시도</button>
    </div>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'

const loadError = ref(false)

onErrorCaptured((error) => {
  if (error.message.includes('Failed to fetch')) {
    loadError.value = true
    return false // 에러 전파 중단
  }
})
</script>
```

---

## 7. 공유 라이브러리 전략

### 7.1 Shared 설정

```typescript
federation({
  shared: ['vue', 'pinia', 'axios']
})
```

| 라이브러리 | 공유 이유 |
|------------|----------|
| `vue` | Vue Remote 앱과 버전 일치 |
| `pinia` | Store 공유 |
| `axios` | API Client 공유 |

### 7.2 React Remote의 경우

```typescript
// Shopping Frontend (React)
federation({
  shared: ['react', 'react-dom']
})
// Vue는 공유하지 않음 - React와 Vue는 독립적으로 로드
```

---

## 8. 라우팅 통합

### 8.1 Host 라우터 설정

```typescript
// router/index.ts
const routes = [
  { path: '/', component: HomePage },
  { path: '/blog/:pathMatch(.*)*', component: BlogContainer },
  { path: '/shopping/:pathMatch(.*)*', component: ShoppingContainer },
]
```

### 8.2 Remote 경로 동기화

```typescript
// Host → Remote
watch(() => route.path, (newPath) => {
  if (newPath.startsWith('/shopping')) {
    const remotePath = newPath.replace('/shopping', '') || '/'
    appInstance?.onParentNavigate(remotePath)
  }
})

// Remote → Host (onNavigate 콜백)
const mountOptions = {
  onNavigate: (path: string) => {
    router.push(`/shopping${path}`)
  }
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Host** | Portal Shell (Vue 3) - 전체 레이아웃 관리 |
| **Remote** | Blog (Vue), Shopping (React) - 독립 마이크로앱 |
| **exposes** | Host가 제공하는 모듈 (api, stores) |
| **remotes** | Host가 로딩하는 Remote 앱 |
| **shared** | 중복 로딩 방지를 위한 공유 라이브러리 |
| **bootstrap** | Remote 앱의 마운트 함수 |
| **Keep-Alive** | Vue의 컴포넌트 캐싱 + Remote 활성화/비활성화 |

---

## 다음 학습

- [Module Federation Remote (React)](../../shopping-frontend/docs/learning/mfe/module-federation-remote.md)
- [Host-Remote 상태 동기화](./shared-state.md)
- [테마 동기화](./theme-synchronization.md)
