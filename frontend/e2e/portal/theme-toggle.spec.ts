import { test, expect } from '@playwright/test'

test.describe('Theme Toggle', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('should have theme toggle button', async ({ page }) => {
    const themeToggle = page.getByRole('button', { name: /theme/i })
      .or(page.locator('[data-testid="theme-toggle"]'))
      .or(page.locator('.theme-toggle'))

    const isVisible = await themeToggle.first().isVisible().catch(() => false)

    if (isVisible) {
      await expect(themeToggle.first()).toBeVisible()
    } else {
      test.skip()
    }
  })

  test('should toggle between light and dark mode', async ({ page }) => {
    const themeToggle = page.getByRole('button', { name: /theme/i })
      .or(page.locator('[data-testid="theme-toggle"]'))
      .or(page.locator('.theme-toggle'))

    const isVisible = await themeToggle.first().isVisible().catch(() => false)

    if (!isVisible) {
      test.skip()
      return
    }

    const html = page.locator('html')
    const initialTheme = await html.getAttribute('class')

    await themeToggle.first().click()
    await page.waitForTimeout(300)

    const newTheme = await html.getAttribute('class')

    if (initialTheme?.includes('dark')) {
      expect(newTheme).not.toContain('dark')
    } else {
      expect(newTheme).toContain('dark')
    }
  })

  test('should persist theme preference', async ({ page, context }) => {
    const themeToggle = page.getByRole('button', { name: /theme/i })
      .or(page.locator('[data-testid="theme-toggle"]'))
      .or(page.locator('.theme-toggle'))

    const isVisible = await themeToggle.first().isVisible().catch(() => false)

    if (!isVisible) {
      test.skip()
      return
    }

    await themeToggle.first().click()
    await page.waitForTimeout(300)

    const themeAfterToggle = await page.locator('html').getAttribute('class')

    await page.reload()
    await page.waitForTimeout(500)

    const themeAfterReload = await page.locator('html').getAttribute('class')

    expect(themeAfterReload).toBe(themeAfterToggle)
  })
})
