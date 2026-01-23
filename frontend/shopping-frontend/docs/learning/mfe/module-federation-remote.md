# Module Federation Remote (React 18)

## 학습 목표
- Remote 앱의 bootstrap 패턴 이해
- Host로부터 Props 수신 및 반응 방식 학습
- 독립 실행(Standalone) 모드 구현 이해

---

## 1. Remote 앱 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SHOPPING FRONTEND (REMOTE)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                         vite.config.ts                              │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │  name: 'shopping-frontend'                                          │    │
│   │  filename: 'remoteEntry.js'                                         │    │
│   │                                                                     │    │
│   │  exposes:                                                           │    │
│   │    './bootstrap': './src/bootstrap.tsx'                             │    │
│   │                                                                     │    │
│   │  remotes:                                                           │    │
│   │    portal: Portal Shell (Host)                                      │    │
│   │                                                                     │    │
│   │  shared: ['react', 'react-dom']                                     │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                        bootstrap.tsx                                │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │                                                                     │    │
│   │  export function mountShoppingApp(                                  │    │
│   │    el: HTMLElement,                                                 │    │
│   │    options: MountOptions                                            │    │
│   │  ): ShoppingAppInstance                                             │    │
│   │                                                                     │    │
│   │  MountOptions:                                                      │    │
│   │    • initialPath: string                                            │    │
│   │    • onNavigate: (path) => void                                     │    │
│   │    • theme: 'light' | 'dark'                                        │    │
│   │                                                                     │    │
│   │  Returns:                                                           │    │
│   │    • onParentNavigate(path)                                         │    │
│   │    • onActivated()                                                  │    │
│   │    • onDeactivated()                                                │    │
│   │    • onThemeChange(theme)                                           │    │
│   │    • unmount()                                                      │    │
│   │                                                                     │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Vite 설정 분석

### 2.1 vite.config.ts

```typescript
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    // chunk 로딩을 위한 base URL 설정 (중요!)
    base: env.VITE_BASE_URL,

    plugins: [
      react(),
      federation({
        // Remote 앱 이름
        name: 'shopping-frontend',

        // 빌드 결과물
        filename: 'remoteEntry.js',

        // Host에서 import 가능한 모듈
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },

        // 필요시 다른 Remote 참조 가능
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL,
        },

        // React 라이브러리 공유
        shared: ['react', 'react-dom'],
      }),
    ],

    server: {
      port: 30002,
      host: '0.0.0.0',
      cors: true,  // CORS 필수
    },

    build: {
      target: 'esnext',
      minify: false,
      cssCodeSplit: true,
    }
  }
})
```

### 2.2 환경 변수

```bash
# .env.development
VITE_BASE_URL=http://localhost:30002/

# .env.production
VITE_BASE_URL=https://shopping.portal-universe.com/
```

---

## 3. Bootstrap 패턴

### 3.1 MountOptions 인터페이스

```typescript
export type MountOptions = {
  /** 초기 경로 */
  initialPath?: string

  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void

  /** 테마 설정 */
  theme?: 'light' | 'dark'
}
```

### 3.2 ShoppingAppInstance 인터페이스

```typescript
export type ShoppingAppInstance = {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void

  /** 앱 언마운트 */
  unmount: () => void

  /** keep-alive activated 콜백 */
  onActivated?: () => void

  /** keep-alive deactivated 콜백 */
  onDeactivated?: () => void

  /** 테마 변경 콜백 */
  onThemeChange?: (theme: 'light' | 'dark') => void
}
```

### 3.3 mountShoppingApp 구현

```typescript
export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): ShoppingAppInstance {

  // 1. Portal Shell에서 마운트됨 표시
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true

  // 2. 기존 인스턴스 정리
  const existingInstance = instanceRegistry.get(el)
  if (existingInstance) {
    existingInstance.root.unmount()
    instanceRegistry.delete(el)
  }

  // 3. 옵션 추출
  const { initialPath = '/', onNavigate, theme = 'light' } = options

  // 4. React Root 생성
  const root = ReactDOM.createRoot(el)

  // 5. 스타일 태그 마킹 (정리 시 식별용)
  const styleObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeName === 'STYLE') {
          (node as HTMLStyleElement)
            .setAttribute('data-mf-app', 'shopping')
        }
      })
    })
  })
  styleObserver.observe(document.head, { childList: true })

  // 6. 재렌더링 함수 (테마 변경 시 사용)
  let currentTheme = theme
  const rerender = () => {
    root.render(
      <React.StrictMode>
        <App
          initialPath={initialPath}
          theme={currentTheme}
          onNavigate={(path) => {
            const instance = instanceRegistry.get(el)
            if (instance?.isActive) {
              instance.navigateCallback?.(path)
            }
          }}
        />
      </React.StrictMode>
    )
  }

  // 7. 인스턴스 등록
  instanceRegistry.set(el, {
    root,
    navigateCallback: onNavigate,
    styleObserver,
    isActive: true,
    currentTheme,
    rerender
  })

  // 8. data-service 속성 설정 (CSS 선택자용)
  document.documentElement.setAttribute('data-service', 'shopping')

  // 9. 초기 렌더링
  rerender()

  // 10. 앱 인스턴스 반환
  return {
    onParentNavigate: (path: string) => {
      const instance = instanceRegistry.get(el)
      if (instance?.isActive) {
        navigateTo(path)
      }
    },

    onActivated: () => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        instance.isActive = true
        document.documentElement.setAttribute('data-service', 'shopping')
        setAppActive(true)
      }
    },

    onDeactivated: () => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        instance.isActive = false
        setAppActive(false)
      }
    },

    onThemeChange: (newTheme: 'light' | 'dark') => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        currentTheme = newTheme
        instance.currentTheme = newTheme
        instance.rerender()
      }
    },

    unmount: () => {
      const instance = instanceRegistry.get(el)

      // Observer 정리
      instance?.styleObserver?.disconnect()

      // React Root Unmount
      instance?.root.unmount()

      // DOM 정리
      el.innerHTML = ''

      // 스타일 태그 제거
      document.querySelectorAll('style[data-mf-app="shopping"]')
        .forEach(el => el.remove())

      // data-service 속성 제거
      if (document.documentElement.getAttribute('data-service') === 'shopping') {
        document.documentElement.removeAttribute('data-service')
      }

      // 레지스트리에서 제거
      instanceRegistry.delete(el)
    }
  }
}
```

---

## 4. 인스턴스 레지스트리

### 4.1 WeakMap 기반 상태 관리

```typescript
// 전역 상태 대신 WeakMap 사용 (메모리 누수 방지)
const instanceRegistry = new WeakMap<HTMLElement, {
  root: ReactDOM.Root
  navigateCallback: ((path: string) => void) | null
  styleObserver: MutationObserver | null
  isActive: boolean
  currentTheme: 'light' | 'dark'
  rerender: () => void
}>()
```

### 4.2 WeakMap 선택 이유

| 특성 | 설명 |
|------|------|
| **자동 GC** | 컨테이너 엘리먼트가 제거되면 자동으로 메모리 해제 |
| **인스턴스 격리** | 여러 인스턴스가 동시에 존재해도 충돌 없음 |
| **전역 오염 방지** | 전역 변수 사용 최소화 |

---

## 5. Keep-Alive 지원

### 5.1 활성화/비활성화 콜백

```typescript
// Host (Vue)에서 호출
appInstance.onActivated()   // 컴포넌트 활성화 시
appInstance.onDeactivated() // 컴포넌트 비활성화 시
```

### 5.2 구현 세부사항

```typescript
onActivated: () => {
  console.log('🔄 [Shopping] App activated')
  const instance = instanceRegistry.get(el)
  if (instance) {
    instance.isActive = true

    // data-service 복원 (CSS 활성화)
    document.documentElement.setAttribute('data-service', 'shopping')

    // 라우터 동기화 활성화 (약간의 지연)
    setTimeout(() => setAppActive(true), 100)
  }
},

onDeactivated: () => {
  console.log('⏸️ [Shopping] App deactivated')
  const instance = instanceRegistry.get(el)
  if (instance) {
    instance.isActive = false

    // 라우터 동기화 비활성화 (즉시)
    setAppActive(false)
  }
}
```

---

## 6. 테마 동기화

### 6.1 Host → Remote 테마 전파

```typescript
// Host (Vue)
watch(() => themeStore.mode, (newTheme) => {
  shoppingAppInstance?.onThemeChange?.(newTheme)
})

// Remote (React)
onThemeChange: (newTheme: 'light' | 'dark') => {
  console.log(`🎨 Theme changed to: ${newTheme}`)
  const instance = instanceRegistry.get(el)
  if (instance) {
    currentTheme = newTheme
    instance.currentTheme = newTheme
    instance.rerender()  // 새 테마로 재렌더링
  }
}
```

### 6.2 App 컴포넌트에서 테마 적용

```tsx
// App.tsx
interface AppProps {
  initialPath?: string
  theme?: 'light' | 'dark'
  onNavigate?: (path: string) => void
}

function App({ initialPath, theme = 'light', onNavigate }: AppProps) {
  useEffect(() => {
    // 테마에 따라 CSS 클래스 적용
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return (
    <div data-theme={theme}>
      <Router />
    </div>
  )
}
```

---

## 7. 스타일 격리

### 7.1 스타일 태그 마킹

```typescript
// 마운트 시: 스타일 태그에 마커 추가
const styleObserver = new MutationObserver((mutations) => {
  mutations.forEach((mutation) => {
    mutation.addedNodes.forEach((node) => {
      if (node.nodeName === 'STYLE' &&
          !(node as HTMLStyleElement).hasAttribute('data-mf-app')) {
        (node as HTMLStyleElement).setAttribute('data-mf-app', 'shopping')
      }
    })
  })
})
styleObserver.observe(document.head, { childList: true })
```

### 7.2 언마운트 시 스타일 정리

```typescript
unmount: () => {
  // 마커 기반 스타일 태그 제거
  document.querySelectorAll('style[data-mf-app="shopping"]')
    .forEach(el => el.remove())

  // link 태그 중 Shopping CSS 제거
  document.querySelectorAll('link[rel="stylesheet"]')
    .forEach((linkTag) => {
      const href = linkTag.getAttribute('href') || ''
      if (href.includes(':30002/')) {
        linkTag.remove()
      }
    })

  // data-service 속성 정리
  if (document.documentElement.getAttribute('data-service') === 'shopping') {
    document.documentElement.removeAttribute('data-service')
  }
}
```

---

## 8. 독립 실행 모드

### 8.1 main.tsx (Standalone)

```tsx
// src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

// Standalone 모드 확인
const isEmbedded = (window as any).__POWERED_BY_PORTAL_SHELL__

if (!isEmbedded) {
  // 독립 실행
  const container = document.getElementById('root')!
  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <App initialPath="/" theme="light" />
    </React.StrictMode>
  )
}
```

### 8.2 모드 감지

```typescript
// 어디서든 실행 모드 확인
const isEmbedded = () => (window as any).__POWERED_BY_PORTAL_SHELL__ === true

// 사용 예
if (isEmbedded()) {
  // Host와 통신
} else {
  // 독립 동작
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **bootstrap** | Remote 앱의 마운트 함수 export |
| **MountOptions** | Host → Remote 전달 옵션 |
| **AppInstance** | Remote → Host 통신 인터페이스 |
| **WeakMap** | 인스턴스별 상태 격리 |
| **data-mf-app** | 스타일 태그 마킹 (정리용) |
| **data-service** | CSS 선택자 활성화 |
| **Keep-Alive** | onActivated/onDeactivated 콜백 |
| **Standalone** | `__POWERED_BY_PORTAL_SHELL__` 플래그 확인 |

---

## 다음 학습

- [Zustand 상태 관리](../react/zustand-state.md)
- [React Router v6 통합](../react/react-router-v6.md)
- [Host-Remote 상태 동기화](./portal-integration.md)
