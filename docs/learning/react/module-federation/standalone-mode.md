# Standalone Mode

## 학습 목표
- 독립 실행 모드의 필요성과 구현 방법 이해
- Embedded 모드와 Standalone 모드의 차이점 분석
- Shopping Frontend의 듀얼 모드 지원 구조 학습

---

## 1. Standalone Mode 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        STANDALONE MODE                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   독립 개발        ─────►  Portal Shell 없이 개발 가능                       │
│   독립 테스트      ─────►  Remote 앱 단독 테스트                             │
│   독립 배포        ─────►  마이크로프론트엔드로 별도 배포 가능                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 왜 Standalone Mode가 필요한가?

| 필요성 | 설명 |
|--------|------|
| **개발 속도** | Portal Shell 없이 빠르게 개발/테스트 |
| **팀 독립성** | Shopping 팀이 독립적으로 작업 가능 |
| **E2E 테스트** | 전체 플로우 테스트 가능 |
| **디버깅** | 단순화된 환경에서 디버깅 용이 |
| **마이그레이션** | 기존 SPA에서 MFE로 점진적 전환 |

---

## 2. 모드 감지

### 2.1 Embedded vs Standalone

**Portal Universe 코드**
```tsx
// main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.css'

// Standalone 모드 확인
const isEmbedded = (window as any).__POWERED_BY_PORTAL_SHELL__

if (!isEmbedded) {
  // ✅ Standalone 모드: 독립 실행
  console.log('🔧 [Shopping] Running in Standalone mode')

  const container = document.getElementById('root')!
  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <App initialPath="/" theme="light" />
    </React.StrictMode>
  )
} else {
  // ✅ Embedded 모드: bootstrap.tsx의 mountShoppingApp()이 호출됨
  console.log('🔧 [Shopping] Running in Embedded mode (will be mounted by Portal Shell)')
}
```

### 2.2 플래그 설정

```tsx
// bootstrap.tsx
export function mountShoppingApp(el: HTMLElement, options: MountOptions) {
  // Portal Shell에서 마운트됨을 표시
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true

  // ...
}
```

---

## 3. App 컴포넌트 듀얼 모드 지원

### 3.1 조건부 UI 렌더링

**Portal Universe 코드 (App.tsx)**
```tsx
function App({ theme = 'light', initialPath = '/', onNavigate }: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true

  return (
    <div className="min-h-screen bg-bg-page">
      {/* ============================================ */}
      {/* Standalone 모드에서만 헤더 표시 */}
      {/* ============================================ */}
      {!isEmbedded && (
        <header className="bg-bg-card border-b border-border-default sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              {/* Logo */}
              <div className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
                  <span className="text-white font-bold text-lg">S</span>
                </div>
                <span className="text-xl font-bold text-text-heading">Shopping</span>
              </div>

              {/* Nav */}
              <nav className="flex items-center gap-6">
                <a href="/" className="text-text-body hover:text-brand-primary font-medium transition-colors">
                  🛍️ Products
                </a>
                <a href="/cart" className="text-text-body hover:text-brand-primary font-medium transition-colors">
                  🛒 Cart
                </a>
                <a href="/orders" className="text-text-body hover:text-brand-primary font-medium transition-colors">
                  📦 Orders
                </a>
              </nav>

              {/* Mode Badge */}
              <div className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
                📦 Standalone
              </div>
            </div>
          </div>
        </header>
      )}

      {/* ============================================ */}
      {/* Main Content */}
      {/* ============================================ */}
      <main className={isEmbedded ? 'py-4' : 'py-8'}>
        <div className="max-w-7xl mx-auto px-6">
          <ShoppingRouter
            isEmbedded={isEmbedded}
            initialPath={initialPath}
            onNavigate={onNavigate}
          />
        </div>
      </main>

      {/* ============================================ */}
      {/* Standalone 모드에서만 푸터 표시 */}
      {/* ============================================ */}
      {!isEmbedded && (
        <footer className="bg-bg-card border-t border-border-default mt-auto">
          <div className="max-w-7xl mx-auto px-4 py-6 text-center">
            <p className="text-sm text-text-meta">
              © 2025 Portal Universe Shopping. All rights reserved.
            </p>
          </div>
        </footer>
      )}
    </div>
  )
}
```

---

## 4. Router 모드 전환

### 4.1 BrowserRouter vs MemoryRouter

**Portal Universe 코드 (router/index.tsx)**
```tsx
export const createRouter = (options: {
  isEmbedded?: boolean
  basePath?: string
  initialPath?: string
}) => {
  const { isEmbedded = false, basePath = '/shopping', initialPath = '/' } = options

  if (isEmbedded) {
    // ✅ Embedded 모드: MemoryRouter
    // Portal Shell이 URL을 관리하고, Shopping은 내부 상태만 관리
    return createMemoryRouter(routes, {
      initialEntries: [initialPath],
      initialIndex: 0
    })
  }

  // ✅ Standalone 모드: BrowserRouter
  // 브라우저 URL을 직접 관리
  return createBrowserRouter(routes, {
    basename: basePath // /shopping으로 시작
  })
}
```

### 4.2 URL 구조 차이

| 모드 | Router | URL 예시 |
|------|--------|----------|
| **Standalone** | BrowserRouter | `http://localhost:30002/shopping/products/1` |
| **Embedded** | MemoryRouter | `http://localhost:30000/shopping/products/1` (Portal Shell URL) |

---

## 5. 인증 처리

### 5.1 Embedded 모드: Portal authStore 사용

```tsx
// App.tsx
function App(props: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true

  useEffect(() => {
    if (isEmbedded) {
      // Portal Shell의 authStore 동기화
      const authStore = useAuthStore.getState()
      authStore.syncFromPortal().then(() => {
        console.log('[Shopping] Portal Shell authStore synced')
      })
    }
  }, [isEmbedded])

  return <div>...</div>
}
```

### 5.2 Standalone 모드: 자체 인증

```tsx
// Standalone 모드에서는 자체 로그인 구현
function LoginPage() {
  const { login } = useAuthStore()

  const handleLogin = async (username: string, password: string) => {
    try {
      await login(username, password)
      navigate('/')
    } catch (error) {
      alert('Login failed')
    }
  }

  return <form onSubmit={handleLogin}>...</form>
}
```

---

## 6. 테마 관리

### 6.1 Embedded 모드: Portal 테마 구독

```tsx
// App.tsx
function App({ theme = 'light' }: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true
  const portalTheme = usePortalTheme()

  // Embedded 모드에서 Portal 테마 우선 사용
  const isDark = isEmbedded && portalTheme.isConnected
    ? portalTheme.isDark
    : theme === 'dark'

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark)
  }, [isDark])

  return <div>...</div>
}
```

### 6.2 Standalone 모드: 자체 테마 관리

```tsx
function ThemeToggle() {
  const [theme, setTheme] = useState<'light' | 'dark'>('light')

  const toggleTheme = () => {
    const newTheme = theme === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
    document.documentElement.classList.toggle('dark', newTheme === 'dark')
  }

  return (
    <button onClick={toggleTheme}>
      {theme === 'light' ? '🌙 Dark' : '☀️ Light'}
    </button>
  )
}
```

---

## 7. 개발 서버 설정

### 7.1 package.json scripts

```json
{
  "scripts": {
    "dev": "vite --port 30002",
    "dev:standalone": "VITE_MODE=standalone vite --port 30002",
    "build": "tsc && vite build",
    "preview": "vite preview --port 30002"
  }
}
```

### 7.2 환경 변수

```bash
# .env.development
VITE_BASE_URL=http://localhost:30002/
VITE_SHOPPING_API_URL=http://localhost:8082/api/v1
VITE_PORTAL_SHELL_REMOTE_URL=http://localhost:30000/assets/remoteEntry.js

# .env.production
VITE_BASE_URL=https://shopping.portal-universe.com/
VITE_SHOPPING_API_URL=https://api.portal-universe.com/shopping/v1
VITE_PORTAL_SHELL_REMOTE_URL=https://portal.portal-universe.com/assets/remoteEntry.js
```

---

## 8. E2E 테스트

### 8.1 Standalone 모드에서 Playwright 테스트

```ts
// e2e/shopping.spec.ts
import { test, expect } from '@playwright/test'

test.describe('Shopping App - Standalone Mode', () => {
  test.beforeEach(async ({ page }) => {
    // Standalone 모드로 접속
    await page.goto('http://localhost:30002/shopping')
  })

  test('should display product list', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Products')
    await expect(page.locator('.product-card')).toHaveCount(12)
  })

  test('should navigate to product detail', async ({ page }) => {
    await page.click('.product-card:first-child')
    await expect(page).toHaveURL(/\/shopping\/products\/\d+/)
    await expect(page.locator('.product-detail')).toBeVisible()
  })

  test('should add product to cart', async ({ page }) => {
    await page.click('.product-card:first-child')
    await page.click('button:has-text("Add to Cart")')
    await expect(page.locator('.cart-count')).toContainText('1')
  })
})
```

---

## 9. 배포 전략

### 9.1 듀얼 배포

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DEPLOYMENT                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Standalone        ─────►  https://shopping.portal-universe.com            │
│   (독립 배포)                                                                │
│                                                                              │
│   Embedded          ─────►  https://portal.portal-universe.com/shopping     │
│   (Portal Shell 통합)                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Nginx 설정 예시

```nginx
# Standalone 배포
server {
  server_name shopping.portal-universe.com;
  root /var/www/shopping-frontend/dist;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /assets {
    expires 1y;
    add_header Cache-Control "public, immutable";
  }
}

# Embedded (Portal Shell)
server {
  server_name portal.portal-universe.com;
  root /var/www/portal-shell/dist;

  location / {
    try_files $uri $uri/ /index.html;
  }

  # Remote Entry 제공
  location /shopping-assets {
    alias /var/www/shopping-frontend/dist/assets;
    add_header Access-Control-Allow-Origin *;
  }
}
```

---

## 10. 핵심 정리

| 측면 | Embedded 모드 | Standalone 모드 |
|------|---------------|-----------------|
| **실행** | bootstrap.tsx | main.tsx |
| **Router** | MemoryRouter | BrowserRouter |
| **URL** | Portal Shell 관리 | 자체 관리 |
| **인증** | Portal authStore | 자체 인증 |
| **테마** | Portal themeStore | 자체 테마 |
| **배포** | Portal Shell 통합 | 독립 배포 |
| **용도** | 프로덕션 통합 | 개발/테스트 |

---

## 11. 실습 과제

다음 시나리오를 Standalone 모드로 구현하세요:

```
1. 로컬에서 Shopping Frontend 독립 실행
2. 브라우저에서 http://localhost:30002/shopping 접속
3. 헤더에 "Standalone" 뱃지 확인
4. 상품 목록 → 상품 상세 → 장바구니 추가 플로우 테스트
5. 다크 모드 토글 테스트
```

---

## 다음 학습

- [Bootstrap Pattern](./bootstrap-pattern.md)
- [Portal Integration](./portal-integration.md)
- [Module Federation Remote](./module-federation-remote.md)
