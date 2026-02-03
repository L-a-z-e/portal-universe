import { test, expect } from '../../fixtures/base'
import { mockLogin, mockLogout, defaultTestUser } from '../../fixtures/auth'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Authentication', () => {
  test('사이드바 로그인 버튼 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 Login 버튼 확인
    const loginButton = page.locator('nav, aside').getByText('Login')
    await expect(loginButton).toBeVisible()
  })

  test('로그인 버튼 클릭 시 로그인 모달/페이지', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Login 버튼 클릭
    const loginButton = page.locator('nav, aside').getByText('Login')
    await loginButton.click()

    // 로그인 모달 또는 OAuth 리다이렉트 확인
    await page.waitForTimeout(1000)

    // OAuth 로그인 또는 모달 확인
    const oauthLogin = page.getByText(/google|github|kakao|naver|oauth|로그인/i)
    const loginForm = page.locator('form, [role="dialog"]')

    const hasOAuth = (await oauthLogin.count()) > 0
    const hasForm = (await loginForm.count()) > 0
    const urlChanged = !page.url().includes('localhost:30000/')

    expect(hasOAuth || hasForm || urlChanged).toBeTruthy()
  })

  test('비로그인 상태 - 로그인 버튼 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Login 버튼이 사이드바에 보임
    const loginButton = page.getByText('Login')
    await expect(loginButton.first()).toBeVisible()
  })

  test('로그인 상태 유지 확인 (Mock)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.portal.home)
    await waitForLoading(authenticatedPage)

    // Mock 로그인 후 UI 변화 확인
    // 로그인 상태면 Logout 버튼이 보이고 Login 버튼은 안보임
    await expect(authenticatedPage.getByRole('button', { name: /Logout/i })).toBeVisible({ timeout: 10000 })
  })

  test('로그아웃 후 비로그인 상태 확인 (Mock)', async ({ authenticatedPage }) => {
    // 로그인 상태로 시작
    await authenticatedPage.goto(routes.portal.home)
    await waitForLoading(authenticatedPage)

    // 로그아웃 버튼 클릭
    const logoutButton = authenticatedPage.getByRole('button', { name: /Logout/i })
    await expect(logoutButton).toBeVisible({ timeout: 10000 })
    await logoutButton.click()

    // 로그인 버튼이 다시 보이는지 확인
    await expect(authenticatedPage.getByRole('button', { name: /Login/i })).toBeVisible({ timeout: 10000 })
  })

  test('Settings 버튼 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Settings 버튼 확인
    const settingsButton = page.getByText('Settings')
    await expect(settingsButton.first()).toBeVisible()
  })

  test('실제 로그인 - DataInitializer 테스트 계정', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 Login 버튼 클릭
    const loginButton = page.locator('nav, aside').getByText('Login')
    await loginButton.click()

    // 로그인 모달이 열릴 때까지 대기
    const modal = page.locator('[role="dialog"], .modal, form')
    await expect(modal.first()).toBeVisible({ timeout: 5000 })

    // 이메일 입력 (label="이메일")
    const emailInput = page.getByLabel(/이메일/i).or(page.locator('input[type="email"]'))
    await emailInput.fill(defaultTestUser.email)

    // 비밀번호 입력 (label="비밀번호")
    const passwordInput = page.getByLabel(/비밀번호/i).or(page.locator('input[type="password"]'))
    await passwordInput.fill(defaultTestUser.password)

    // 로그인 버튼 클릭 (submit 버튼, exact match)
    const submitButton = page.getByRole('button', { name: '로그인', exact: true })
    await submitButton.click()

    // 로그인 성공 확인 - Logout 버튼이 나타나는지 확인
    const logoutButton = page.getByText('Logout')
    await expect(logoutButton).toBeVisible({ timeout: 10000 })

    // 사용자 이름이 사이드바에 표시되는지 확인
    const username = page.locator('nav, aside').getByText(defaultTestUser.nickname)
      .or(page.locator('nav, aside').getByText('테스트유저'))
    await expect(username.first()).toBeVisible()
  })
})
