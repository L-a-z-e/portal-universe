# E2E 테스트 (End-to-End Testing)

## 학습 목표

- E2E 테스트의 정의와 목적 이해
- Playwright를 사용한 브라우저 자동화 테스트 작성법
- 사용자 시나리오 기반 테스트 설계 방법
- Fixture와 Page Object Model 패턴 활용
- 반응형 디자인 및 다양한 브라우저 환경 테스트

## 테스트 피라미드에서의 위치

```
        /\
       /  \      E2E Tests (소수, 느림, 비싸다) ← 여기!
      /____\
     /      \    Integration Tests (중간, 적당한 속도)
    /________\
   /          \  Unit Tests (다수, 빠름, 저렴하다)
  /__________\
```

**E2E 테스트의 특징:**
- 실제 사용자 관점에서 전체 시스템을 검증
- 프론트엔드 + 백엔드 + 데이터베이스 모두 포함
- 가장 느리고 유지보수 비용이 높음
- 가장 높은 신뢰도 제공
- Critical Path(핵심 사용자 여정)에 집중

## E2E 테스트란?

E2E(End-to-End) 테스트는 사용자가 애플리케이션을 실제로 사용하는 것처럼 전체 시스템을 검증하는 테스트입니다.

**단위 테스트 vs 통합 테스트 vs E2E 테스트:**

| 구분 | 단위 테스트 | 통합 테스트 | E2E 테스트 |
|------|------------|------------|-----------|
| 범위 | 단일 함수/클래스 | 여러 컴포넌트 | 전체 시스템 |
| 관점 | 개발자 | 개발자 | 사용자 |
| 환경 | 메모리 | DB/외부 시스템 | 실제 브라우저 |
| 속도 | 밀리초 | 초 | 초~분 |
| 비용 | 낮음 | 중간 | 높음 |
| 신뢰도 | 낮음 | 중간 | 높음 |

**E2E 테스트가 필요한 경우:**
- 핵심 사용자 여정 (회원가입, 로그인, 결제 등)
- 복잡한 사용자 상호작용 (드래그 앤 드롭, 파일 업로드)
- 크로스 브라우저 호환성
- 반응형 디자인 검증
- 중요한 비즈니스 시나리오

## Playwright란?

Playwright는 Microsoft에서 개발한 현대적인 웹 테스트 자동화 프레임워크입니다.

**주요 특징:**
- ✅ 크로스 브라우저 지원 (Chrome, Firefox, Safari, Edge)
- ✅ 자동 대기 (Auto-wait) - 안정적인 테스트
- ✅ 병렬 실행 지원
- ✅ 스크린샷/비디오 녹화
- ✅ Network Interception (Mock API)
- ✅ 모바일 에뮬레이션
- ✅ TypeScript 네이티브 지원

**Playwright vs Selenium:**

| 기능 | Playwright | Selenium |
|------|-----------|----------|
| 속도 | 빠름 | 느림 |
| 안정성 | 높음 (Auto-wait) | 낮음 (명시적 대기) |
| API | 모던하고 직관적 | 레거시 |
| 병렬 실행 | 내장 | 추가 설정 필요 |
| 네트워크 제어 | 강력 | 제한적 |

## Playwright 기본 구조

### 설치 및 설정

```bash
npm init playwright@latest

# 또는 기존 프로젝트에 추가
npm install -D @playwright/test
npx playwright install
```

**playwright.config.ts:**

```typescript
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e/tests',
  timeout: 30000,
  expect: {
    timeout: 5000
  },
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:30000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    port: 30000,
    reuseExistingServer: !process.env.CI,
  },
})
```

### 기본 테스트 작성

```typescript
import { test, expect } from '@playwright/test'

test('홈페이지가 로드된다', async ({ page }) => {
  // 페이지 이동
  await page.goto('/')

  // 제목 확인
  await expect(page).toHaveTitle(/Portal Universe/)

  // 요소 존재 확인
  const heading = page.locator('h1')
  await expect(heading).toBeVisible()
  await expect(heading).toHaveText('Welcome')
})
```

## Playwright 핵심 개념

### 1. Locators - 요소 찾기

```typescript
// Playwright의 권장 방식 (우선순위 순)

// 1. Role 기반 (접근성 향상)
await page.getByRole('button', { name: 'Submit' })
await page.getByRole('textbox', { name: 'Email' })
await page.getByRole('link', { name: 'Learn more' })

// 2. Test ID (가장 안정적)
await page.getByTestId('submit-button')

// 3. Label (폼 요소)
await page.getByLabel('Password')

// 4. Placeholder
await page.getByPlaceholder('Enter your email')

// 5. Text
await page.getByText('Welcome back')

// 6. CSS Selector (최후의 수단)
await page.locator('.btn-primary')
await page.locator('#user-menu')

// 체이닝
await page
  .locator('.product-card')
  .filter({ hasText: 'Laptop' })
  .getByRole('button', { name: 'Add to cart' })
  .click()
```

### 2. Actions - 사용자 동작

```typescript
// 클릭
await page.getByRole('button', { name: 'Login' }).click()
await page.locator('.menu').click({ force: true }) // 가려진 요소 클릭

// 입력
await page.getByLabel('Email').fill('user@example.com')
await page.getByLabel('Password').type('secret123') // 타이핑 시뮬레이션

// 선택
await page.getByLabel('Country').selectOption('Korea')

// 체크박스/라디오
await page.getByLabel('Terms').check()
await page.getByLabel('Terms').uncheck()

// 파일 업로드
await page.getByLabel('Upload').setInputFiles('path/to/file.pdf')

// 호버
await page.getByText('Menu').hover()

// 드래그 앤 드롭
await page.locator('.draggable').dragTo(page.locator('.dropzone'))

// 키보드
await page.keyboard.press('Enter')
await page.keyboard.type('Hello World')
await page.keyboard.down('Shift')
```

### 3. Assertions - 검증

```typescript
// 가시성
await expect(page.getByText('Welcome')).toBeVisible()
await expect(page.getByText('Loading')).toBeHidden()

// 텍스트
await expect(page.locator('h1')).toHaveText('Portal Universe')
await expect(page.locator('.error')).toContainText('Invalid')

// 속성
await expect(page.getByRole('button')).toBeEnabled()
await expect(page.getByRole('button')).toBeDisabled()
await expect(page.locator('input')).toHaveAttribute('type', 'email')
await expect(page.locator('input')).toHaveValue('user@example.com')

// CSS
await expect(page.locator('.alert')).toHaveClass(/error/)
await expect(page.locator('.box')).toHaveCSS('background-color', 'rgb(255, 0, 0)')

// URL
await expect(page).toHaveURL(/\/dashboard/)
await expect(page).toHaveTitle(/Dashboard/)

// 개수
await expect(page.getByRole('listitem')).toHaveCount(10)

// 스크린샷 비교
await expect(page).toHaveScreenshot('homepage.png')
```

### 4. Waiting - 대기

```typescript
// Playwright는 자동으로 대기하지만, 필요 시 명시적 대기

// 요소가 나타날 때까지
await page.waitForSelector('.modal')

// 네트워크 요청 대기
await page.waitForResponse(resp =>
  resp.url().includes('/api/users') && resp.status() === 200
)

// 로딩 상태 대기
await page.waitForLoadState('networkidle')

// 커스텀 조건
await page.waitForFunction(() => window.dataLoaded === true)

// 타임아웃
await page.waitForTimeout(1000) // 가능하면 사용 지양
```

## Portal Universe E2E 테스트 분석

### 예제 1: 피드 기능 테스트 (feed.spec.ts)

**위치:** `frontend/blog-frontend/e2e/tests/feed.spec.ts`

#### 1. 로그인 상태에 따른 UI 변화 테스트

```typescript
test.describe('Feed Tab Visibility', () => {
  test('should display feed tab when logged in', async ({ page }) => {
    await mockLogin(page)
    await page.goto('/blog')

    // 피드 탭 확인
    const feedTab = page.locator('[data-testid="feed-tab"]')
    await expect(feedTab).toBeVisible()
  })

  test('should not display feed tab when not logged in', async ({ page }) => {
    await mockLogout(page)
    await page.goto('/blog')

    // 피드 탭이 보이지 않아야 함
    const feedTab = page.locator('[data-testid="feed-tab"]')
    await expect(feedTab).not.toBeVisible()
  })
})
```

**학습 포인트:**
- `test.describe`로 관련 테스트 그룹화
- `mockLogin/mockLogout` 헬퍼 함수로 인증 상태 제어
- `data-testid`를 사용한 안정적인 선택자

#### 2. URL 상태 관리 테스트

```typescript
test.describe('Feed Tab Selection', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should navigate to feed tab via URL', async ({ page }) => {
    await page.goto('/blog?tab=feed')

    const feedTab = page.locator('[data-testid="feed-tab"]')
    await expect(feedTab).toHaveAttribute('data-active', 'true')
  })

  test('should update URL when clicking feed tab', async ({ page }) => {
    await page.goto('/blog')

    const feedTab = page.locator('[data-testid="feed-tab"]')
    await feedTab.click()

    await page.waitForTimeout(500)

    // URL이 업데이트되었는지 확인
    await expect(page).toHaveURL(/tab=feed/)
  })
})
```

**학습 포인트:**
- `beforeEach`로 공통 setup
- URL 파라미터 기반 라우팅 검증
- 사용자 상호작용 후 URL 변화 확인

#### 3. 무한 스크롤 테스트

```typescript
test.describe('Feed Infinite Scroll', () => {
  test('should load more posts when scrolling to bottom', async ({ page }) => {
    await mockLogin(page)
    await page.goto('/blog?tab=feed')
    await page.waitForTimeout(1000)

    const posts = page.locator('[data-testid="post-card"]')
    const initialCount = await posts.count()

    if (initialCount > 0) {
      // 페이지 하단으로 스크롤
      await page.evaluate(() =>
        window.scrollTo(0, document.body.scrollHeight))

      await page.waitForTimeout(1500)

      const newCount = await posts.count()

      // 더 많은 포스트가 로드되거나 같아야 함 (더 이상 없을 경우)
      expect(newCount).toBeGreaterThanOrEqual(initialCount)
    }
  })

  test('should show loading indicator while fetching', async ({ page }) => {
    await mockLogin(page)
    await page.goto('/blog?tab=feed')
    await page.waitForTimeout(1000)

    await page.evaluate(() =>
      window.scrollTo(0, document.body.scrollHeight))

    const loadingIndicator = page.locator('[data-testid="loading-more"]')
    // 로딩 인디케이터가 짧게 나타남
    await page.waitForTimeout(500)
  })
})
```

**학습 포인트:**
- `page.evaluate`로 JavaScript 코드 실행
- 스크롤 이벤트 시뮬레이션
- 동적 콘텐츠 로딩 검증

#### 4. 반응형 디자인 테스트

```typescript
test.describe('Feed Responsiveness', () => {
  test('should display feed correctly on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })

    await mockLogin(page)
    await page.goto('/blog?tab=feed')
    await page.waitForTimeout(1000)

    const feedTab = page.locator('[data-testid="feed-tab"]')
    await expect(feedTab).toBeVisible()

    const posts = page.locator('[data-testid="post-card"]')
    const postCount = await posts.count()

    if (postCount > 0) {
      const firstPost = posts.first()
      const box = await firstPost.boundingBox()

      if (box) {
        // 포스트가 모바일 너비에 맞는지 확인
        expect(box.width).toBeLessThanOrEqual(375)
      }
    }
  })

  test('should display feed correctly on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await mockLogin(page)
    await page.goto('/blog?tab=feed')

    const posts = page.locator('[data-testid="post-card"]')
    const emptyFeed = page.locator('[data-testid="empty-feed"]')

    const postCount = await posts.count()
    const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

    expect(postCount > 0 || hasEmptyState).toBeTruthy()
  })
})
```

**학습 포인트:**
- `setViewportSize`로 다양한 화면 크기 테스트
- `boundingBox`로 요소 크기/위치 검증
- 반응형 레이아웃 검증

#### 5. 에러 처리 테스트

```typescript
test.describe('Feed Loading States', () => {
  test('should show error message on feed load failure', async ({ page }) => {
    // 네트워크 실패 시뮬레이션
    await page.route('**/api/blog/posts/feed*', route => route.abort())

    await mockLogin(page)
    await page.goto('/blog?tab=feed')
    await page.waitForTimeout(1000)

    // 에러 메시지 또는 재시도 버튼 확인
    const errorMessage = page.locator('[data-testid="feed-error"]')
    const retryButton = page.locator('[data-testid="retry-button"]')

    const hasError = await errorMessage.isVisible().catch(() => false)
    const hasRetry = await retryButton.isVisible().catch(() => false)

    expect(hasError || hasRetry).toBeTruthy()
  })

  test('should retry loading when clicking retry button', async ({ page }) => {
    let failCount = 0

    // 첫 요청은 실패, 이후 성공
    await page.route('**/api/blog/posts/feed*', route => {
      failCount++
      if (failCount === 1) {
        route.abort()
      } else {
        route.continue()
      }
    })

    await mockLogin(page)
    await page.goto('/blog?tab=feed')
    await page.waitForTimeout(1000)

    const retryButton = page.locator('[data-testid="retry-button"]')

    if (await retryButton.isVisible()) {
      await retryButton.click()
      await page.waitForTimeout(1000)

      // 피드가 로드되어야 함
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    }
  })
})
```

**학습 포인트:**
- `page.route`로 네트워크 요청 가로채기
- 에러 시나리오 시뮬레이션
- 재시도 로직 검증

## Fixture 패턴

Fixture는 테스트에서 공통적으로 사용하는 설정을 재사용하는 패턴입니다.

### 인증 Fixture

**fixtures/auth.ts:**

```typescript
import { Page } from '@playwright/test'

export async function mockLogin(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'mock-token-12345')
    localStorage.setItem('user', JSON.stringify({
      id: 1,
      username: 'testuser',
      email: 'test@example.com'
    }))
  })
}

export async function mockLogout(page: Page) {
  await page.addInitScript(() => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user')
  })
}
```

**사용:**

```typescript
import { mockLogin } from '../fixtures/auth'

test('로그인 상태에서만 접근 가능', async ({ page }) => {
  await mockLogin(page)
  await page.goto('/dashboard')

  await expect(page).toHaveURL(/\/dashboard/)
})
```

### 커스텀 Fixture

```typescript
import { test as base } from '@playwright/test'

type MyFixtures = {
  authenticatedPage: Page
  adminPage: Page
}

export const test = base.extend<MyFixtures>({
  authenticatedPage: async ({ page }, use) => {
    await mockLogin(page)
    await use(page)
  },

  adminPage: async ({ page }, use) => {
    await mockAdminLogin(page)
    await use(page)
  },
})

// 사용
test('일반 사용자 테스트', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/dashboard')
  // ...
})

test('관리자 테스트', async ({ adminPage }) => {
  await adminPage.goto('/admin')
  // ...
})
```

## Page Object Model (POM)

페이지의 구조와 동작을 캡슐화하여 테스트 코드의 재사용성과 유지보수성을 높이는 패턴입니다.

### POM 구현

**pages/LoginPage.ts:**

```typescript
import { Page, Locator } from '@playwright/test'

export class LoginPage {
  readonly page: Page
  readonly emailInput: Locator
  readonly passwordInput: Locator
  readonly submitButton: Locator
  readonly errorMessage: Locator

  constructor(page: Page) {
    this.page = page
    this.emailInput = page.getByLabel('Email')
    this.passwordInput = page.getByLabel('Password')
    this.submitButton = page.getByRole('button', { name: 'Login' })
    this.errorMessage = page.locator('[data-testid="error-message"]')
  }

  async goto() {
    await this.page.goto('/login')
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email)
    await this.passwordInput.fill(password)
    await this.submitButton.click()
  }

  async getErrorMessage() {
    return await this.errorMessage.textContent()
  }
}
```

**pages/DashboardPage.ts:**

```typescript
export class DashboardPage {
  readonly page: Page
  readonly welcomeMessage: Locator
  readonly logoutButton: Locator

  constructor(page: Page) {
    this.page = page
    this.welcomeMessage = page.locator('[data-testid="welcome-message"]')
    this.logoutButton = page.getByRole('button', { name: 'Logout' })
  }

  async goto() {
    await this.page.goto('/dashboard')
  }

  async logout() {
    await this.logoutButton.click()
  }

  async getWelcomeText() {
    return await this.welcomeMessage.textContent()
  }
}
```

**사용:**

```typescript
import { test, expect } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage'
import { DashboardPage } from '../pages/DashboardPage'

test('로그인 후 대시보드 접근', async ({ page }) => {
  const loginPage = new LoginPage(page)
  const dashboardPage = new DashboardPage(page)

  await loginPage.goto()
  await loginPage.login('user@example.com', 'password123')

  await expect(page).toHaveURL(/\/dashboard/)
  await expect(dashboardPage.welcomeMessage).toContainText('Welcome back')
})

test('잘못된 자격증명으로 로그인 실패', async ({ page }) => {
  const loginPage = new LoginPage(page)

  await loginPage.goto()
  await loginPage.login('wrong@example.com', 'wrongpass')

  const error = await loginPage.getErrorMessage()
  expect(error).toContain('Invalid credentials')
})
```

## 모범 사례

### 1. 테스트 격리

```typescript
// ✅ 각 테스트는 독립적으로 실행되어야 함
test('테스트 1', async ({ page }) => {
  // 완전히 새로운 컨텍스트
})

test('테스트 2', async ({ page }) => {
  // 테스트 1의 영향을 받지 않음
})

// ❌ 테스트 간 의존성
test('사용자 생성', async ({ page }) => {
  // 사용자 생성
})

test('사용자 로그인', async ({ page }) => {
  // 이전 테스트에 의존 - 나쁨!
})
```

### 2. data-testid 사용

```tsx
// ✅ 프로덕션 코드에 data-testid 추가
<button data-testid="submit-button" className="btn-primary">
  Submit
</button>

// 테스트
await page.getByTestId('submit-button').click()

// ❌ CSS 클래스나 구조에 의존
await page.locator('.btn-primary').click() // 클래스 변경 시 깨짐
```

### 3. 명시적 대기보다 암시적 대기

```typescript
// ❌ 고정 시간 대기 (Flaky Test)
await page.waitForTimeout(2000)

// ✅ 조건 기반 대기
await expect(page.locator('.modal')).toBeVisible()
await page.waitForResponse(resp => resp.url().includes('/api/data'))
```

### 4. 비즈니스 로직과 UI 로직 분리

```typescript
// ❌ UI 세부사항에 집중
test('버튼이 파란색이다', async ({ page }) => {
  await expect(page.locator('button')).toHaveCSS('color', 'blue')
})

// ✅ 사용자 관점에서 작성
test('로그인 후 대시보드로 이동한다', async ({ page }) => {
  await loginPage.login('user@example.com', 'password')
  await expect(page).toHaveURL(/\/dashboard/)
})
```

### 5. 스크린샷/비디오 활용

```typescript
test('결제 프로세스', async ({ page }) => {
  await page.goto('/checkout')

  // 중요한 단계마다 스크린샷
  await page.screenshot({ path: 'checkout-start.png' })

  await page.getByRole('button', { name: 'Pay' }).click()

  await page.screenshot({ path: 'payment-completed.png' })
})

// playwright.config.ts에서 자동 설정
use: {
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
  trace: 'on-first-retry',
}
```

### 6. API Mocking

```typescript
test('API 에러 시나리오', async ({ page }) => {
  // 특정 API를 Mock
  await page.route('**/api/products', route => {
    route.fulfill({
      status: 500,
      body: JSON.stringify({ error: 'Internal Server Error' })
    })
  })

  await page.goto('/products')

  await expect(page.getByText('Failed to load products')).toBeVisible()
})

test('성공 응답 Mock', async ({ page }) => {
  await page.route('**/api/products', route => {
    route.fulfill({
      status: 200,
      body: JSON.stringify([
        { id: 1, name: 'Product 1' },
        { id: 2, name: 'Product 2' }
      ])
    })
  })

  await page.goto('/products')

  await expect(page.getByText('Product 1')).toBeVisible()
})
```

## 고급 패턴

### 1. 병렬 실행

```typescript
// playwright.config.ts
export default defineConfig({
  fullyParallel: true, // 모든 테스트 병렬 실행
  workers: 4, // 워커 수
})

// 특정 테스트는 순차 실행
test.describe.serial('순차 실행 그룹', () => {
  test('첫 번째', async ({ page }) => { })
  test('두 번째', async ({ page }) => { })
})
```

### 2. 재시도 로직

```typescript
// playwright.config.ts
export default defineConfig({
  retries: process.env.CI ? 2 : 0, // CI에서 2번 재시도
})

// 특정 테스트만 재시도
test('불안정한 테스트', async ({ page }) => {
  test.setTimeout(60000) // 타임아웃 증가
  // ...
})
```

### 3. 조건부 테스트

```typescript
test('Safari에서만 실행', async ({ page, browserName }) => {
  test.skip(browserName !== 'webkit', 'Safari only')
  // ...
})

test('모바일에서만 실행', async ({ page, viewport }) => {
  test.skip(!viewport || viewport.width > 768, 'Mobile only')
  // ...
})
```

## 실전 시나리오

### 1. 회원가입 플로우

```typescript
test('전체 회원가입 플로우', async ({ page }) => {
  // 1. 회원가입 페이지로 이동
  await page.goto('/signup')

  // 2. 폼 입력
  await page.getByLabel('Email').fill('newuser@example.com')
  await page.getByLabel('Password').fill('SecurePass123!')
  await page.getByLabel('Confirm Password').fill('SecurePass123!')
  await page.getByLabel('Terms').check()

  // 3. 제출
  await page.getByRole('button', { name: 'Sign Up' }).click()

  // 4. 이메일 인증 페이지로 이동 확인
  await expect(page).toHaveURL(/\/verify-email/)
  await expect(page.getByText('Verification email sent')).toBeVisible()

  // 5. 이메일 확인 (Mock)
  await page.evaluate(() => {
    localStorage.setItem('email_verified', 'true')
  })

  // 6. 로그인 페이지로 이동
  await page.goto('/login')
  await page.getByLabel('Email').fill('newuser@example.com')
  await page.getByLabel('Password').fill('SecurePass123!')
  await page.getByRole('button', { name: 'Login' }).click()

  // 7. 대시보드 접근 확인
  await expect(page).toHaveURL(/\/dashboard/)
})
```

### 2. 쇼핑몰 구매 플로우

```typescript
test('상품 검색부터 결제까지', async ({ page }) => {
  await mockLogin(page)

  // 1. 상품 검색
  await page.goto('/')
  await page.getByPlaceholder('Search products').fill('Laptop')
  await page.keyboard.press('Enter')

  // 2. 상품 선택
  await page.getByText('MacBook Pro').click()
  await expect(page).toHaveURL(/\/products\/\d+/)

  // 3. 장바구니 추가
  await page.getByRole('button', { name: 'Add to Cart' }).click()
  await expect(page.getByText('Added to cart')).toBeVisible()

  // 4. 장바구니로 이동
  await page.getByTestId('cart-icon').click()
  await expect(page).toHaveURL(/\/cart/)

  // 5. 수량 변경
  await page.getByTestId('quantity-increase').click()
  await expect(page.getByTestId('quantity-value')).toHaveText('2')

  // 6. 체크아웃
  await page.getByRole('button', { name: 'Checkout' }).click()
  await expect(page).toHaveURL(/\/checkout/)

  // 7. 배송 정보 입력
  await page.getByLabel('Address').fill('123 Main St')
  await page.getByLabel('City').fill('Seoul')
  await page.getByLabel('Postal Code').fill('12345')

  // 8. 결제 정보 입력
  await page.getByLabel('Card Number').fill('4111111111111111')
  await page.getByLabel('Expiry').fill('12/25')
  await page.getByLabel('CVV').fill('123')

  // 9. 주문 완료
  await page.getByRole('button', { name: 'Place Order' }).click()
  await expect(page).toHaveURL(/\/order-confirmation/)
  await expect(page.getByText('Order successful')).toBeVisible()
})
```

## CI/CD 통합

### GitHub Actions 예제

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: 18

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright
        run: npx playwright install --with-deps

      - name: Run E2E tests
        run: npm run test:e2e

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 30
```

## 체크리스트

E2E 테스트 작성 전:
- [ ] 이 시나리오는 정말 E2E 테스트가 필요한가?
- [ ] 핵심 사용자 여정에 해당하는가?
- [ ] 단위/통합 테스트로 커버할 수 없는가?

E2E 테스트 작성 후:
- [ ] 테스트가 사용자 관점에서 작성되었는가?
- [ ] 테스트가 독립적으로 실행되는가?
- [ ] `data-testid`를 사용하여 안정적인 선택자를 사용하는가?
- [ ] 고정 시간 대기(`waitForTimeout`)를 최소화했는가?
- [ ] 에러 시나리오도 테스트했는가?
- [ ] 다양한 화면 크기를 고려했는가?

## 관련 문서

- [단위 테스트 (Unit Testing)](./unit-testing.md)
- [통합 테스트 (Integration Testing)](./integration-testing.md)
- [SCENARIO-015: 피드 기능](../../../scenarios/SCENARIO-015-feed.md)
- [UI/UX 패턴 (TBD)](../../patterns/ui-ux-patterns.md)

## 참고 자료

- [Playwright Documentation](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Page Object Model](https://playwright.dev/docs/pom)
- [Testing Library Guiding Principles](https://testing-library.com/docs/guiding-principles/)
- [Google Testing Blog](https://testing.googleblog.com/)
