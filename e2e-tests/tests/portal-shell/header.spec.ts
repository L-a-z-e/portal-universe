import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { defaultTestUser } from '../../fixtures/auth'

test.describe('Portal Shell - Sidebar Navigation', () => {
  test('사이드바 렌더링', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 요소 확인 (Portal Universe 타이틀)
    const sidebar = page.getByText('Portal Universe')
    await expect(sidebar.first()).toBeVisible()
  })

  test('로고 표시 및 홈 링크', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // Portal Universe 로고/타이틀 찾기
    const logo = page.getByText('Portal Universe').first()
    await expect(logo).toBeVisible()

    // Home 클릭 시 홈으로 이동
    const homeLink = page.locator('nav, aside').getByText('Home')
    await homeLink.click()
    await expect(page).toHaveURL(routes.portal.home)
  })

  test('비로그인 상태 - 로그인 버튼 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 Login 버튼
    const loginButton = page.getByText('Login')
    await expect(loginButton.first()).toBeVisible()
  })

  test('로그인 상태 확인 (Mock)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.portal.home)
    await waitForLoading(authenticatedPage)

    // Mock 로그인 상태 확인 - Logout 버튼이 보이면 로그인 상태
    await expect(authenticatedPage.getByRole('button', { name: /Logout/i })).toBeVisible({ timeout: 10000 })
  })

  test('서비스 메뉴 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Blog, Shopping, Prism 메뉴
    const blogMenu = page.locator('nav, aside').getByText('Blog')
    const shoppingMenu = page.locator('nav, aside').getByText('Shopping')
    const prismMenu = page.locator('nav, aside').getByText('Prism')

    await expect(blogMenu.first()).toBeVisible()
    await expect(shoppingMenu.first()).toBeVisible()
    await expect(prismMenu.first()).toBeVisible()
  })

  test('Status 메뉴 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Status 메뉴
    const statusMenu = page.getByText('Status')
    await expect(statusMenu.first()).toBeVisible()
  })

  test('Settings 메뉴 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // Settings 메뉴
    const settingsMenu = page.getByText('Settings')
    await expect(settingsMenu.first()).toBeVisible()
  })

  test('사이드바 스크롤 시에도 유지', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // 페이지 스크롤
    await page.evaluate(() => window.scrollBy(0, 500))

    // 사이드바가 여전히 보이는지 확인
    const sidebar = page.getByText('Portal Universe')
    await expect(sidebar.first()).toBeVisible()
  })
})
