# Bootstrap Pattern

## 학습 목표
- Remote 앱의 진입점 패턴 이해
- MountOptions와 AppInstance 인터페이스 설계
- Shopping Frontend의 bootstrap.tsx 구조 분석

---

## 1. Bootstrap 패턴 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        BOOTSTRAP PATTERN                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   mountApp()      ─────►  Remote 앱을 DOM에 마운트                           │
│   MountOptions    ─────►  Host → Remote 전달 옵션                            │
│   AppInstance     ─────►  Remote → Host 통신 인터페이스                      │
│   unmount()       ─────►  정리 (cleanup)                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 왜 Bootstrap이 필요한가?

**문제**
```tsx
// ❌ 직접 렌더링 (Module Federation 미지원)
ReactDOM.createRoot(document.getElementById('root')!).render(<App />)
```

**해결**
```tsx
// ✅ Bootstrap 함수 export (Host가 호출)
export function mountShoppingApp(el: HTMLElement, options: MountOptions): AppInstance {
  // 1. React Root 생성
  // 2. 옵션에 따라 설정
  // 3. 앱 렌더링
  // 4. 통신 인터페이스 반환
}
```

---

## 2. 인터페이스 설계

### 2.1 MountOptions (Host → Remote)

```tsx
// bootstrap.tsx
export type MountOptions = {
  /** 초기 경로 */
  initialPath?: string

  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void

  /** 테마 설정 */
  theme?: 'light' | 'dark'

  /** 기타 확장 가능 */
  locale?: string
  userId?: string
}
```

### 2.2 AppInstance (Remote → Host)

```tsx
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

---

## 3. Portal Universe 코드 분석

### 3.1 bootstrap.tsx - mountShoppingApp 함수

```tsx
// src/bootstrap.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import type { MountOptions, ShoppingAppInstance } from './types'
import { setNavigationCallback, navigateTo, setAppActive } from './router'

// 인스턴스 레지스트리 (WeakMap으로 메모리 누수 방지)
const instanceRegistry = new WeakMap<HTMLElement, {
  root: ReactDOM.Root
  navigateCallback: ((path: string) => void) | null
  styleObserver: MutationObserver | null
  isActive: boolean
  currentTheme: 'light' | 'dark'
  rerender: () => void
}>()

export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): ShoppingAppInstance {

  // ============================================
  // 1. Portal Shell에서 마운트됨 표시
  // ============================================
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true

  // ============================================
  // 2. 기존 인스턴스 정리
  // ============================================
  const existingInstance = instanceRegistry.get(el)
  if (existingInstance) {
    existingInstance.root.unmount()
    instanceRegistry.delete(el)
  }

  // ============================================
  // 3. 옵션 추출
  // ============================================
  const { initialPath = '/', onNavigate, theme = 'light' } = options

  // ============================================
  // 4. React Root 생성
  // ============================================
  const root = ReactDOM.createRoot(el)

  // ============================================
  // 5. 스타일 태그 마킹 (정리 시 식별용)
  // ============================================
  const styleObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeName === 'STYLE') {
          (node as HTMLStyleElement).setAttribute('data-mf-app', 'shopping')
        }
      })
    })
  })
  styleObserver.observe(document.head, { childList: true })

  // ============================================
  // 6. 재렌더링 함수 (테마 변경 시 사용)
  // ============================================
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

  // ============================================
  // 7. 인스턴스 등록
  // ============================================
  instanceRegistry.set(el, {
    root,
    navigateCallback: onNavigate || null,
    styleObserver,
    isActive: true,
    currentTheme,
    rerender
  })

  // ============================================
  // 8. data-service 속성 설정 (CSS 선택자용)
  // ============================================
  document.documentElement.setAttribute('data-service', 'shopping')

  // ============================================
  // 9. 초기 렌더링
  // ============================================
  setNavigationCallback(onNavigate || null)
  rerender()

  // ============================================
  // 10. 앱 인스턴스 반환
  // ============================================
  return {
    // Parent → Remote 내비게이션
    onParentNavigate: (path: string) => {
      const instance = instanceRegistry.get(el)
      if (instance?.isActive) {
        navigateTo(path)
      }
    },

    // Keep-Alive: 활성화
    onActivated: () => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        instance.isActive = true
        document.documentElement.setAttribute('data-service', 'shopping')
        setAppActive(true)
      }
    },

    // Keep-Alive: 비활성화
    onDeactivated: () => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        instance.isActive = false
        setAppActive(false)
      }
    },

    // 테마 변경
    onThemeChange: (newTheme: 'light' | 'dark') => {
      const instance = instanceRegistry.get(el)
      if (instance) {
        currentTheme = newTheme
        instance.currentTheme = newTheme
        instance.rerender()
      }
    },

    // 언마운트
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

## 4. WeakMap 인스턴스 레지스트리

### 4.1 왜 WeakMap을 사용하는가?

```tsx
// ❌ 전역 변수 사용 (메모리 누수 위험)
let globalInstance: AppInstance | null = null

export function mountApp(el: HTMLElement) {
  globalInstance = { /* ... */ }
}

// ✅ WeakMap 사용 (자동 GC)
const instanceRegistry = new WeakMap<HTMLElement, AppInstance>()

export function mountApp(el: HTMLElement) {
  const instance = { /* ... */ }
  instanceRegistry.set(el, instance)
}

// el이 제거되면 자동으로 instance도 GC됨
```

### 4.2 장점

| 특성 | 설명 |
|------|------|
| **자동 GC** | 컨테이너 엘리먼트가 제거되면 자동으로 메모리 해제 |
| **인스턴스 격리** | 여러 인스턴스가 동시에 존재해도 충돌 없음 |
| **전역 오염 방지** | 전역 변수 사용 최소화 |

---

## 5. 스타일 격리

### 5.1 스타일 태그 마킹

```tsx
const styleObserver = new MutationObserver((mutations) => {
  mutations.forEach((mutation) => {
    mutation.addedNodes.forEach((node) => {
      if (node.nodeName === 'STYLE' &&
          !(node as HTMLStyleElement).hasAttribute('data-mf-app')) {
        // Shopping 앱의 스타일 태그에 마커 추가
        (node as HTMLStyleElement).setAttribute('data-mf-app', 'shopping')
      }
    })
  })
})
styleObserver.observe(document.head, { childList: true })
```

### 5.2 언마운트 시 정리

```tsx
unmount: () => {
  // 1. 마커 기반 스타일 태그 제거
  document.querySelectorAll('style[data-mf-app="shopping"]')
    .forEach(el => el.remove())

  // 2. link 태그 중 Shopping CSS 제거
  document.querySelectorAll('link[rel="stylesheet"]')
    .forEach((linkTag) => {
      const href = linkTag.getAttribute('href') || ''
      if (href.includes(':30002/')) {
        linkTag.remove()
      }
    })

  // 3. data-service 속성 정리
  if (document.documentElement.getAttribute('data-service') === 'shopping') {
    document.documentElement.removeAttribute('data-service')
  }
}
```

---

## 6. 독립 실행 모드

### 6.1 main.tsx (Standalone)

```tsx
// src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.css'

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

### 6.2 모드 감지

```tsx
// App.tsx
function App(props: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true

  return (
    <div>
      {/* Standalone 모드에서만 헤더 표시 */}
      {!isEmbedded && (
        <header>
          <h1>Shopping App</h1>
        </header>
      )}

      {/* 메인 콘텐츠 */}
      <ShoppingRouter {...props} />
    </div>
  )
}
```

---

## 7. 테마 동기화

### 7.1 Host → Remote 테마 전파

```tsx
// Host (Portal Shell - Vue)
watch(() => themeStore.mode, (newTheme) => {
  shoppingAppInstance?.onThemeChange?.(newTheme)
})

// Remote (Shopping - React)
onThemeChange: (newTheme: 'light' | 'dark') => {
  console.log(`🎨 Theme changed to: ${newTheme}`)
  const instance = instanceRegistry.get(el)
  if (instance) {
    currentTheme = newTheme
    instance.currentTheme = newTheme
    instance.rerender() // 새 테마로 재렌더링
  }
}
```

### 7.2 App 컴포넌트에서 테마 적용

```tsx
// App.tsx
function App({ theme = 'light' }: AppProps) {
  useEffect(() => {
    // 테마에 따라 CSS 클래스 적용
    document.documentElement.classList.toggle('dark', theme === 'dark')

    // data-theme 속성 동기화
    document.documentElement.setAttribute(
      'data-theme',
      theme === 'dark' ? 'dark' : 'light'
    )
  }, [theme])

  return <div data-theme={theme}>...</div>
}
```

---

## 8. Keep-Alive 지원

### 8.1 활성화/비활성화 콜백

```tsx
// Host (Vue keep-alive)
<keep-alive>
  <component :is="currentRemoteApp" @activated="handleActivated" @deactivated="handleDeactivated" />
</keep-alive>

function handleActivated() {
  shoppingAppInstance?.onActivated?.()
}

function handleDeactivated() {
  shoppingAppInstance?.onDeactivated?.()
}
```

### 8.2 Remote 구현

```tsx
onActivated: () => {
  console.log('🔄 [Shopping] App activated')
  const instance = instanceRegistry.get(el)
  if (instance) {
    instance.isActive = true

    // data-service 복원 (CSS 활성화)
    document.documentElement.setAttribute('data-service', 'shopping')

    // 라우터 동기화 활성화
    setTimeout(() => setAppActive(true), 100)
  }
},

onDeactivated: () => {
  console.log('⏸️ [Shopping] App deactivated')
  const instance = instanceRegistry.get(el)
  if (instance) {
    instance.isActive = false

    // 라우터 동기화 비활성화
    setAppActive(false)
  }
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| `mountApp()` | Remote 앱의 진입점 함수 |
| `MountOptions` | Host → Remote 전달 옵션 |
| `AppInstance` | Remote → Host 통신 인터페이스 |
| `WeakMap` | 인스턴스별 상태 격리 (메모리 안전) |
| `data-mf-app` | 스타일 태그 마킹 (정리용) |
| `data-service` | CSS 선택자 활성화 |
| `rerender()` | 테마 변경 시 재렌더링 |
| `unmount()` | 정리 (cleanup) |

---

## 다음 학습

- [Portal Integration](./portal-integration.md)
- [Standalone Mode](./standalone-mode.md)
- [Module Federation Remote](./module-federation-remote.md)
