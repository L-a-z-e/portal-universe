import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Seller Approvals', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/seller-approvals')
  })

  test('should load seller approvals page', async ({ page }) => {
    await expect(page.locator('h1:has-text("Seller Approvals")')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
  })

  test('should display table columns', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })

    await expect(page.locator('th:has-text("Business Name")')).toBeVisible()
    await expect(page.locator('th:has-text("Business No.")')).toBeVisible()
    await expect(page.locator('th:has-text("Reason")')).toBeVisible()
    await expect(page.locator('th:has-text("Applied")')).toBeVisible()
    await expect(page.locator('th:has-text("Actions")')).toBeVisible()
  })

  test('should show empty state or applications', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Loading...')).toBeHidden({ timeout: 10000 })

    const emptyText = page.getByText('No pending applications')
    const firstRow = page.locator('table tbody tr').filter({ has: page.locator('td .font-medium') }).first()

    const isEmpty = await emptyText.isVisible({ timeout: 3000 }).catch(() => false)
    const hasRows = await firstRow.isVisible({ timeout: 3000 }).catch(() => false)
    expect(isEmpty || hasRows).toBeTruthy()
  })

  test('should have approve and reject buttons when applications exist', async ({ page }) => {
    await expect(page.locator('table')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Loading...')).toBeHidden({ timeout: 10000 })

    const isEmpty = await page.getByText('No pending applications').isVisible({ timeout: 3000 }).catch(() => false)

    if (!isEmpty) {
      await expect(page.locator('button:has-text("Approve")').first()).toBeVisible({ timeout: 5000 })
      await expect(page.locator('button:has-text("Reject")').first()).toBeVisible()
    }
  })
})
