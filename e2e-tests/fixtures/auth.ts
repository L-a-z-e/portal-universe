import { Page } from '@playwright/test'

export interface TestUser {
  id: string
  email: string
  password: string
  username: string
  nickname: string
  token: string
}

/**
 * DataInitializer에서 생성되는 실제 테스트 계정
 * auth-service가 local/docker 프로필로 실행 시 자동 생성됨
 */
export const defaultTestUser: TestUser = {
  id: 'test-user-1',
  email: 'test@test.com',
  password: 'test1234',
  username: '테스트사용자',
  nickname: '테스트사용자',
  token: 'mock-jwt-token',
}

export const adminTestUser: TestUser = {
  id: 'admin-user-1',
  email: 'admin@test.com',
  password: 'admin1234',
  username: '관리자',
  nickname: '관리자',
  token: 'mock-admin-token',
}

/**
 * 페이지가 유효한 URL인지 확인하고 필요시 이동
 */
async function ensureValidPage(page: Page, baseURL: string): Promise<void> {
  const currentUrl = page.url()
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto(baseURL)
  }
}

/**
 * Base64URL 인코딩 (JWT용)
 */
function base64UrlEncode(str: string): string {
  return Buffer.from(str)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

/**
 * Mock JWT 토큰 생성
 */
function createMockJwt(user: TestUser, roles: string[] = ['ROLE_USER']): string {
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = base64UrlEncode(JSON.stringify({
    sub: user.id,
    email: user.email,
    username: user.username,
    name: user.nickname,
    nickname: user.nickname,
    roles,
    scopes: ['read', 'write'],
    memberships: {},
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600,
  }))
  return `${header}.${payload}.mock-signature`
}

/**
 * Mock 로그인 - API 인터셉트 + UI 로그인 수행
 */
export async function mockLogin(
  page: Page,
  user: TestUser = defaultTestUser
): Promise<void> {
  const mockToken = createMockJwt(user)

  // 로그인 API 모킹
  await page.route('**/auth-service/api/v1/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: mockToken,
          refreshToken: 'mock-refresh-token',
          expiresIn: 3600
        }
      })
    })
  })

  // 토큰 갱신 API 모킹
  await page.route('**/auth-service/api/v1/auth/refresh', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: mockToken,
          refreshToken: 'mock-refresh-token',
          expiresIn: 3600
        }
      })
    })
  })

  // 홈페이지로 이동
  await page.goto('/')
  await page.waitForLoadState('domcontentloaded')

  // 로그인 버튼 클릭하여 로그인 모달 열기
  const loginButton = page.getByRole('button', { name: /login/i })
  if (await loginButton.isVisible({ timeout: 3000 }).catch(() => false)) {
    await loginButton.click()

    // 로그인 모달에서 로그인 수행
    const emailInput = page.getByPlaceholder(/email|이메일/i)
    const passwordInput = page.getByPlaceholder(/password|비밀번호/i)

    if (await emailInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await emailInput.fill(user.email)
      await passwordInput.fill(user.password)
      await page.getByRole('button', { name: /로그인|login/i }).first().click()

      // 로그인 완료 대기
      await page.waitForTimeout(500)
    }
  }
}

/**
 * Mock 로그아웃 - localStorage 클리어
 */
export async function mockLogout(page: Page, baseURL: string = 'http://localhost:30000'): Promise<void> {
  await ensureValidPage(page, baseURL)
  await page.evaluate(() => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user')
  })
}

/**
 * 실제 로그인 (UI 통해)
 */
export async function loginViaUI(
  page: Page,
  email: string,
  password: string
): Promise<void> {
  await page.goto('/login')
  await page.getByLabel(/이메일|email/i).fill(email)
  await page.getByLabel(/비밀번호|password/i).fill(password)
  await page.getByRole('button', { name: /로그인|login/i }).click()
  await page.waitForURL('/', { timeout: 10000 })
}

/**
 * 로그아웃 (UI 통해)
 */
export async function logoutViaUI(page: Page): Promise<void> {
  await page.getByRole('button', { name: /프로필|profile/i }).click()
  await page.getByRole('menuitem', { name: /로그아웃|logout/i }).click()
  await page.waitForURL(/login/, { timeout: 10000 })
}
