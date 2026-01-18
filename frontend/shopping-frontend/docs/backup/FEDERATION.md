# Module Federation 상세 가이드

Shopping Frontend의 Module Federation 설정 및 Portal Shell과의 통신 방식을 설명합니다.

## Module Federation 개요

Module Federation은 Webpack 5 이상에서 제공하는 기능으로, 런타임에 여러 독립적인 JavaScript 애플리케이션(Remote)을 호스트(Host)에 로드할 수 있게 합니다.

## Vite 설정 (vite.config.ts)

```typescript
federation({
  // Host가 이 앱을 식별하는 이름
  name: 'shopping-frontend',

  // Module Federation entry point 파일명
  filename: 'remoteEntry.js',

  // Host에게 노출할 모듈들
  exposes: {
    './bootstrap': './src/bootstrap.tsx'
  },

  // Host와 공유할 의존성 (중복 로드 방지)
  shared: [
    'react',      // React 라이브러리 공유
    'react-dom'   // React DOM 라이브러리 공유
  ]
})
```

## Bootstrap 엔트리포인트 (src/bootstrap.tsx)

```typescript
import React, { Suspense } from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.scss'

interface RemoteAppProps {
  theme?: 'light' | 'dark'
  userRole?: 'guest' | 'user' | 'admin'
  locale?: string
  onNavigate?: (path: string) => void
  [key: string]: any
}

/**
 * Module Federation이 호출하는 export default 함수
 */
export default async (props: RemoteAppProps = {}) => {
  // DOM 노드 생성/찾기
  const root = document.getElementById('shopping-root') ||
               document.createElement('div')
  root.id = 'shopping-root'
  document.body.appendChild(root)

  return {
    /**
     * mount: Host가 remote를 화면에 표시할 때 호출
     */
    mount() {
      const rootElement = ReactDOM.createRoot(root)
      rootElement.render(
        <Suspense fallback={<div>Loading...</div>}>
          <App {...props} />
        </Suspense>
      )
    },

    /**
     * unmount: Host가 remote를 제거할 때 호출
     */
    unmount() {
      ReactDOM.createRoot(root).unmount()
    }
  }
}
```

## Portal Shell (Host) 설정

### 모듈 로더

Portal Shell에서 Shopping Frontend를 로드합니다:

```typescript
// portal-shell/src/moduleLoader.ts
interface RemoteConfig {
  scope: string
  module: string
  shareScope: string
  url: string
}

export const shoppingRemote: RemoteConfig = {
  scope: 'shopping',
  module: './bootstrap',
  shareScope: 'default',
  url: import.meta.env.VITE_SHOPPING_URL || 'http://localhost:30002/dist/'
}

export const loadRemote = async (config: RemoteConfig) => {
  await __webpack_init_sharing__('default')

  const container = window[config.scope]

  if (!container.__initialized) {
    await container.init(__webpack_share_scopes__.default)
    container.__initialized = true
  }

  const factory = await container.get(config.module)
  return factory()
}
```

### 원격 앱 사용

```typescript
// portal-shell/src/routes.tsx
import { lazy, Suspense } from 'react'
import { loadRemote, shoppingRemote } from './moduleLoader'

const ShoppingRemote = lazy(async () => {
  const app = await loadRemote(shoppingRemote)
  return { default: app }
})

export const routes = [
  {
    path: '/shopping/*',
    element: (
      <Suspense fallback={<LoadingPage />}>
        <ShoppingRemote
          theme={themeStore.isDark ? 'dark' : 'light'}
          userRole={authStore.user?.role}
          locale={localeStore.locale}
          onNavigate={(path) => navigate(path)}
        />
      </Suspense>
    )
  }
]
```

## 환경별 설정

### 개발 환경

```bash
# shopping-frontend .env
VITE_API_BASE_URL=http://localhost:8080/api/v1

# portal-shell .env
VITE_SHOPPING_URL=http://localhost:30002/dist/
```

### Docker 환경

```yaml
shopping:
  environment:
    VITE_API_BASE_URL: http://api-gateway:8080/api/v1

portal-shell:
  environment:
    VITE_SHOPPING_URL: http://shopping:30002/dist/
```

### Kubernetes 환경

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: shopping-frontend-config
data:
  VITE_API_BASE_URL: "http://api-gateway.default.svc.cluster.local:8080/api/v1"

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: portal-shell-config
data:
  VITE_SHOPPING_URL: "http://shopping-frontend.default.svc.cluster.local:30002/dist/"
```

## 공유 모듈

### Shared Dependencies

```typescript
// vite.config.ts
shared: [
  'react',
  'react-dom',
  // 필요에 따라 추가
  // '@tanstack/react-query',
  // 'zustand'
]
```

### Portal Shell의 Shared Store 접근

```typescript
// src/stores/authStore.ts
export const syncFromPortal = async () => {
  try {
    const { useAuthStore } = await import('portal/authStore')
    const portalAuth = useAuthStore.getState()

    set({
      user: portalAuth.user,
      isAuthenticated: portalAuth.isAuthenticated,
      accessToken: portalAuth.accessToken
    })
  } catch (error) {
    console.warn('Failed to sync from Portal:', error)
  }
}
```

## 빌드 결과물

```
dist/
├── remoteEntry.js              # Module Federation manifest
├── index.html                  # Standalone HTML
├── assets/
│   ├── index-[hash].js         # 메인 번들
│   ├── bootstrap-[hash].js     # Bootstrap 번들
│   └── [component]-[hash].js   # 컴포넌트별 번들
└── manifest.json               # 에셋 매니페스트
```

## 트러블슈팅

### 1. remoteEntry.js 로드 실패

```bash
# 빌드 확인
ls frontend/shopping-frontend/dist/remoteEntry.js

# URL 확인
curl http://localhost:30002/dist/remoteEntry.js
```

### 2. CORS 에러

```typescript
// vite.config.ts
server: {
  cors: true
}
```

### 3. Shared Dependency 버전 불일치

```typescript
// vite.config.ts
shared: {
  react: {
    singleton: true,
    strictVersion: false,
    requiredVersion: '^18.0.0'
  }
}
```

### 4. Props 동기화 실패

```typescript
// bootstrap.tsx에서 Props 로깅
export default async (props) => {
  console.log('Props received:', props)
}

// App.tsx에서 Props 수신 확인
function App(props) {
  console.log('App props:', props)
}
```

## 배포 가이드

### 단계 1: Shopping Frontend 빌드

```bash
npm run build
```

### 단계 2: remoteEntry.js 배포

```
https://shopping-api.example.com/dist/remoteEntry.js
```

### 단계 3: Portal Shell 설정

```bash
# .env
VITE_SHOPPING_URL=https://shopping-api.example.com/dist/
```

### 단계 4: Portal Shell 빌드 및 배포

```bash
npm run build
```

## 참고 자료

- [Webpack Module Federation](https://webpack.js.org/concepts/module-federation/)
- [@originjs/vite-plugin-federation](https://github.com/originjs/vite-plugin-federation)
- [Micro Frontends Architecture](https://micro-frontends.org/)
