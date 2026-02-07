import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin User Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/users')
  })

  test('should load user management page', async ({ page }) => {
    await expect(page.locator('h1:has-text("User Management")')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
  })

  test('should display user table with columns', async ({ page }) => {
    await expect(page.locator('h1:has-text("User Management")')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('th').filter({ hasText: /^User$/ })).toBeVisible({ timeout: 10000 })
    await expect(page.locator('th:has-text("Username")')).toBeVisible()
    await expect(page.locator('th:has-text("Nickname")')).toBeVisible()
    await expect(page.locator('th:has-text("Status")')).toBeVisible()
    await expect(page.locator('th:has-text("Created")')).toBeVisible()
  })

  test('should search users', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    const searchInput = page.getByPlaceholder('Search by email, username, or nickname...')
    await searchInput.fill('admin')
    await searchInput.press('Enter')
    await page.waitForTimeout(2000)

    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 })
  })

  test('should display pagination', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText(/Page \d+ of \d+/)).toBeVisible({ timeout: 10000 })
    await expect(page.getByRole('button', { name: 'Prev' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Next' })).toBeVisible()
  })

  test('should select user and show detail panel', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.locator('table tbody tr').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByText('Assigned Roles')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Effective Permissions')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Memberships' })).toBeVisible()
  })

  test('should show role assignment controls', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.locator('table tbody tr').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByRole('button', { name: 'Assign' })).toBeVisible({ timeout: 10000 })
  })
})
