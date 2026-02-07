import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin')
  })

  test('should load dashboard page', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })
  })

  test('should display 4 KPI cards', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })

    await expect(page.getByText('Total Users')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Role Assignments')).toBeVisible()
    await expect(page.getByText('Active Memberships')).toBeVisible()
    await expect(page.getByText('Pending Approvals')).toBeVisible()
  })

  test('should display Role Distribution section', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Role Distribution')).toBeVisible({ timeout: 10000 })
  })

  test('should display Membership Overview section', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Membership Overview')).toBeVisible({ timeout: 10000 })
  })

  test('should display Recent Activity section', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Recent Activity')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('th:has-text("Event")')).toBeVisible()
    await expect(page.locator('th:has-text("Time")')).toBeVisible()
  })

  test('should navigate via Quick Links', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Quick Links')).toBeVisible({ timeout: 10000 })

    // Scope to Quick Links section to avoid sidebar button conflict
    const quickLinks = page.locator('h2:has-text("Quick Links")').locator('..')
    await quickLinks.getByRole('button', { name: 'Users' }).click()
    await expect(page.locator('h1:has-text("User Management")')).toBeVisible({ timeout: 15000 })
  })

  test('should show error state with retry on API failure', async ({ page }) => {
    await page.route('**/api/v1/admin/rbac/dashboard', (route) =>
      route.fulfill({ status: 500, contentType: 'application/json', body: '{"success":false}' }),
    )
    await page.goto('/admin')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(5000)

    await expect(page.getByText('Failed to load dashboard data')).toBeVisible({ timeout: 15000 })
    await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible()
  })
})
