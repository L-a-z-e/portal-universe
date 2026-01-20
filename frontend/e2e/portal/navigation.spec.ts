import { test, expect } from '@playwright/test'

test.describe('Portal Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('should display the home page', async ({ page }) => {
    await expect(page).toHaveTitle(/portal/i)
  })

  test('should have navigation links', async ({ page }) => {
    const nav = page.locator('nav')
    await expect(nav).toBeVisible()
  })

  test('should navigate to blog section', async ({ page }) => {
    const blogLink = page.getByRole('link', { name: /blog/i })

    if (await blogLink.isVisible()) {
      await blogLink.click()
      await expect(page).toHaveURL(/blog/)
    }
  })

  test('should navigate to shopping section', async ({ page }) => {
    const shoppingLink = page.getByRole('link', { name: /shopping/i })

    if (await shoppingLink.isVisible()) {
      await shoppingLink.click()
      await expect(page).toHaveURL(/shopping/)
    }
  })
})
