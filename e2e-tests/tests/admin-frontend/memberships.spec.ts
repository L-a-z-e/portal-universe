import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Membership Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/memberships')
  })

  test('should load membership management page', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })
  })

  test('should display tier configuration with group tabs', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })
    // Wait for loading spinner to disappear before checking section content
    await page.locator('.animate-spin, [class*="spinner"]').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})
    await expect(page.getByText('Tier Configuration')).toBeVisible({ timeout: 15000 })

    const groupButtons = page.locator('section').filter({ hasText: 'Tier Configuration' }).getByRole('button')
    await expect(groupButtons.first()).toBeVisible({ timeout: 5000 })
  })

  test('should switch group tabs and show tier table', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })

    const groupButtons = page.locator('section').filter({ hasText: 'Tier Configuration' }).getByRole('button')
    const count = await groupButtons.count()
    if (count < 2) {
      test.skip()
      return
    }

    await groupButtons.nth(1).click()
    await page.waitForTimeout(1000)

    await expect(page.locator('th:has-text("Tier Key")')).toBeVisible()
  })

  test('should display tier table columns', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })

    await expect(page.locator('th:has-text("Tier Key")')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('th:has-text("Display Name")')).toBeVisible()
    await expect(page.locator('th:has-text("Monthly")')).toBeVisible()
    await expect(page.locator('th:has-text("Yearly")')).toBeVisible()
    await expect(page.locator('th:has-text("Order")')).toBeVisible()
  })

  test('should search users by email', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })

    const searchInput = page.getByPlaceholder('Search by email, username or UUID...')
    await searchInput.fill('test@test.com')
    await page.getByRole('button', { name: 'Search' }).click()
    await page.waitForTimeout(3000)

    const hasSearchResults = await page.locator('ul li').first().isVisible({ timeout: 5000 }).catch(() => false)
    const hasUserCard = await page.locator('text=test@test.com').isVisible({ timeout: 2000 }).catch(() => false)
    expect(hasSearchResults || hasUserCard).toBeTruthy()
  })

  test('should select user from search results and show memberships', async ({ page }) => {
    await expect(page.locator('h1:has-text("Membership Management")')).toBeVisible({ timeout: 15000 })

    const searchInput = page.getByPlaceholder('Search by email, username or UUID...')
    await searchInput.fill('test')
    await page.getByRole('button', { name: 'Search' }).click()
    await page.waitForTimeout(3000)

    const firstResult = page.locator('ul li').first()
    if (await firstResult.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstResult.click()
      await page.waitForTimeout(3000)

      // User info card should be visible with UUID
      await expect(page.locator('.font-mono').first()).toBeVisible({ timeout: 5000 })
    }
  })
})
