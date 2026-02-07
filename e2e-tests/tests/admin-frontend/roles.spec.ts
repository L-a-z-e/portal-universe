import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Role Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/roles')
  })

  test('should load role management page', async ({ page }) => {
    await expect(page.locator('h1:has-text("Role Management")')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
  })

  test('should display role table columns', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await expect(page.locator('th:has-text("Role Key")')).toBeVisible()
    await expect(page.locator('th:has-text("Display Name")')).toBeVisible()
    await expect(page.locator('th:has-text("Scope")')).toBeVisible()
    await expect(page.locator('th:has-text("Parent")')).toBeVisible()
    await expect(page.locator('th:has-text("System")')).toBeVisible()
    await expect(page.locator('th:has-text("Status")')).toBeVisible()
  })

  test('should filter roles by search', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    const searchInput = page.getByPlaceholder('Search roles...')
    await searchInput.fill('SUPER')
    await page.waitForTimeout(500)

    await expect(page.locator('table tbody tr').first()).toContainText('SUPER')
  })

  test('should select role and show detail panel', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.locator('table tbody tr').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByText('Permissions')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Info')).toBeVisible()
  })

  test('should open create role form', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.getByRole('button', { name: 'Create Role' }).click()
    await expect(page.getByText('Create New Role')).toBeVisible({ timeout: 5000 })
  })

  test('should create a new role', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.getByRole('button', { name: 'Create Role' }).click()
    await expect(page.getByText('Create New Role')).toBeVisible({ timeout: 5000 })

    const roleName = `ROLE_E2E_TEST_${Date.now()}`
    await page.getByPlaceholder('ROLE_CUSTOM_NAME').fill(roleName)
    await page.getByPlaceholder('Custom Role Name').fill('E2E Test Role')

    await page.getByRole('button', { name: 'Create', exact: true }).click()
    await page.waitForTimeout(3000)

    await expect(page.getByText('Create New Role')).toBeHidden({ timeout: 5000 })
    await expect(page.locator(`text=${roleName}`)).toBeVisible({ timeout: 5000 })
  })

  test('should protect system roles from deactivation', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    const systemRow = page.locator('table tbody tr').filter({ hasText: 'System' }).first()
    if (!(await systemRow.isVisible({ timeout: 3000 }).catch(() => false))) {
      test.skip()
      return
    }
    await systemRow.click()
    await page.waitForTimeout(2000)

    await expect(page.getByRole('button', { name: 'Deactivate' })).toBeHidden()
  })

  test('should show permission assignment controls', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await page.locator('table tbody tr').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByText('Permissions')).toBeVisible({ timeout: 10000 })
    await expect(page.getByRole('button', { name: 'Assign' })).toBeVisible()
  })
})
