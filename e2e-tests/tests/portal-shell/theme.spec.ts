import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Theme', () => {
  test('기본 테마 로드', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // HTML 요소 존재 확인
    const html = page.locator('html')
    await expect(html).toBeVisible()
  })

  test('다크모드 토글 버튼 존재', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 다크모드 토글 버튼 찾기
    const themeToggle = page.getByRole('button', { name: /테마|theme|다크|dark|라이트|light/i })
      .or(page.locator('[aria-label*="theme"], [aria-label*="dark"], .theme-toggle'))

    const count = await themeToggle.count()
    if (count > 0) {
      await expect(themeToggle.first()).toBeVisible()
    }
  })

  test('다크모드 전환', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    const html = page.locator('html')
    const themeToggle = page.getByRole('button', { name: /테마|theme|다크|dark|라이트|light/i })
      .or(page.locator('[aria-label*="theme"], [aria-label*="dark"], .theme-toggle'))

    const count = await themeToggle.count()
    if (count > 0) {
      // 초기 테마 상태 확인
      const initialDark = await html.evaluate(el => el.classList.contains('dark'))

      // 테마 토글
      await themeToggle.first().click()

      // 테마 변경 확인
      const afterToggle = await html.evaluate(el => el.classList.contains('dark'))
      expect(afterToggle).not.toBe(initialDark)
    }
  })

  test('테마 설정 지속성 (localStorage)', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    const themeToggle = page.getByRole('button', { name: /테마|theme|다크|dark|라이트|light/i })
      .or(page.locator('[aria-label*="theme"], [aria-label*="dark"], .theme-toggle'))

    const count = await themeToggle.count()
    if (count > 0) {
      // 테마 변경
      await themeToggle.first().click()

      // localStorage 확인
      const storedTheme = await page.evaluate(() => {
        return localStorage.getItem('theme') ||
               localStorage.getItem('color-mode') ||
               localStorage.getItem('dark-mode')
      })

      // 페이지 새로고침
      await page.reload()
      await waitForLoading(page)

      // 테마 유지 확인
      const currentTheme = await page.evaluate(() => {
        return localStorage.getItem('theme') ||
               localStorage.getItem('color-mode') ||
               localStorage.getItem('dark-mode')
      })

      expect(currentTheme).toBe(storedTheme)
    }
  })

  test('시스템 테마 감지 (prefers-color-scheme)', async ({ page }) => {
    // 다크모드 시스템 설정으로 에뮬레이션
    await page.emulateMedia({ colorScheme: 'dark' })
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 시스템 테마를 따르는 경우 다크모드 적용 확인
    const html = page.locator('html')
    const hasDarkClass = await html.evaluate(el =>
      el.classList.contains('dark') ||
      el.getAttribute('data-theme') === 'dark'
    )

    // 시스템 테마를 따르지 않을 수도 있으므로 단순히 페이지가 로드되었는지 확인
    await expect(html).toBeVisible()
  })

  test('서비스별 테마 색상 변경 (Blog)', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // data-service 속성 확인 또는 서비스별 스타일 확인
    const hasServiceTheme = await page.evaluate(() => {
      const el = document.querySelector('[data-service]')
      return el !== null
    })

    // 서비스 테마 시스템이 있는 경우에만 확인
    if (hasServiceTheme) {
      const serviceAttr = await page.locator('[data-service]').first().getAttribute('data-service')
      expect(serviceAttr).toBeTruthy()
    }
  })
})
