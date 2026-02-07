import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Audit Log', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/audit-log')
  })

  test('should load audit log page', async ({ page }) => {
    await expect(page.locator('h1:has-text("Audit Log")')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
  })

  test('should display audit log table columns', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await expect(page.locator('th:has-text("Time")')).toBeVisible()
    await expect(page.locator('th:has-text("Event")')).toBeVisible()
    await expect(page.locator('th:has-text("Actor")')).toBeVisible()
    await expect(page.locator('th:has-text("Target")')).toBeVisible()
    await expect(page.locator('th:has-text("Details")')).toBeVisible()
  })

  test('should show log entries after loading', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Loading...')).toBeHidden({ timeout: 10000 })

    const rows = page.locator('table tbody tr')
    await expect(rows.first()).toBeVisible({ timeout: 5000 })
  })

  test('should have user UUID filter input', async ({ page }) => {
    await expect(page.locator('h1:has-text("Audit Log")')).toBeVisible({ timeout: 15000 })

    const filterInput = page.getByPlaceholder('Filter by User UUID (optional)')
    await expect(filterInput).toBeVisible()
    await expect(page.locator('button:has-text("Filter")')).toBeVisible()
  })

  test('should filter logs by user UUID', async ({ page }) => {
    await expect(page.locator('h1:has-text("Audit Log")')).toBeVisible({ timeout: 15000 })

    const filterInput = page.getByPlaceholder('Filter by User UUID (optional)')
    await filterInput.fill('00000000-0000-0000-0000-000000000000')
    await page.locator('button:has-text("Filter")').click()
    await page.waitForTimeout(2000)

    await expect(page.locator('button:has-text("Clear")')).toBeVisible({ timeout: 5000 })
  })

  test('should clear filter', async ({ page }) => {
    await expect(page.locator('h1:has-text("Audit Log")')).toBeVisible({ timeout: 15000 })

    const filterInput = page.getByPlaceholder('Filter by User UUID (optional)')
    await filterInput.fill('test-uuid')
    await page.locator('button:has-text("Filter")').click()
    await page.waitForTimeout(2000)

    await page.locator('button:has-text("Clear")').click()
    await page.waitForTimeout(2000)

    await expect(page.locator('button:has-text("Clear")')).toBeHidden({ timeout: 5000 })
    await expect(filterInput).toHaveValue('')
  })

  test('should handle pagination when multiple pages exist', async ({ page }) => {
    await expect(page.locator('h1:has-text("Audit Log")')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Loading...')).toBeHidden({ timeout: 10000 })

    const prevBtn = page.locator('button:has-text("Prev")')
    const hasPagination = await prevBtn.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasPagination) {
      await expect(page.getByText(/\d+ \/ \d+/)).toBeVisible()
      await expect(prevBtn).toBeDisabled()
    }
  })
})
