import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading, waitForNetworkIdle } from '../../utils/wait'

test.describe('Portal Shell - Navigation', () => {
  test('메인 페이지 로드', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 페이지가 정상 로드되었는지 확인
    await expect(page).toHaveURL(routes.portal.home)
  })

  test('네비게이션 메뉴 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 네비게이션 링크 확인 (텍스트 기반)
    const blogLink = page.getByText('Blog', { exact: true })
      .or(page.locator('nav').getByText(/blog/i))
    const shoppingLink = page.getByText('Shopping', { exact: true })
      .or(page.locator('nav').getByText(/shopping/i))

    await expect(blogLink.first()).toBeVisible()
    await expect(shoppingLink.first()).toBeVisible()
  })

  test('Blog 서비스 네비게이션', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 Blog 링크 클릭
    const blogLink = page.locator('nav, aside').getByText('Blog', { exact: true }).first()
    await blogLink.click()

    // Blog 페이지로 이동 확인
    await expect(page).toHaveURL(/\/blog/)
    await waitForLoading(page)
  })

  test('Shopping 서비스 네비게이션', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 사이드바 Shopping 링크 클릭
    const shoppingLink = page.locator('nav, aside').getByText('Shopping', { exact: true }).first()
    await shoppingLink.click()

    // Shopping 페이지로 이동 확인
    await expect(page).toHaveURL(/\/shopping/)
    await waitForLoading(page)
  })

  test('브라우저 뒤로가기/앞으로가기', async ({ page }) => {
    // 홈 -> Blog -> Shopping 순서로 이동
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    await page.goto(routes.blog.home)
    await waitForLoading(page)

    await page.goto(routes.shopping.home)
    await waitForLoading(page)

    // 뒤로가기
    await page.goBack()
    await expect(page).toHaveURL(/\/blog/)

    // 앞으로가기
    await page.goForward()
    await expect(page).toHaveURL(/\/shopping/)
  })

  test('404 페이지 처리', async ({ page }) => {
    await page.goto('/non-existent-page-12345')

    // 404 또는 에러 메시지 확인
    const notFound = page.getByText('404')
    await expect(notFound.first()).toBeVisible()
  })

  test('모바일 네비게이션 메뉴 (반응형)', async ({ page }) => {
    // 모바일 뷰포트 설정
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 햄버거 메뉴 버튼 확인
    const menuButton = page.getByRole('button', { name: /메뉴|menu/i })
      .or(page.locator('.hamburger, .menu-toggle, [aria-label*="menu"]'))

    const menuCount = await menuButton.count()
    if (menuCount > 0) {
      await expect(menuButton.first()).toBeVisible()

      // 메뉴 열기
      await menuButton.first().click()

      // 네비게이션 링크 표시 확인
      const blogLink = page.getByRole('link', { name: /blog|블로그/i })
      await expect(blogLink).toBeVisible()
    }
  })
})
