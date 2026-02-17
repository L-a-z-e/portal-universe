import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Membership Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/memberships')
  })

  test('should load memberships page with 3-column layout', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    // Wait for loading to complete
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})
    // Groups column (w-52) should have items
    const groupItems = page.locator('.w-52').first().locator('[class*="cursor-pointer"]')
    await expect(groupItems.first()).toBeVisible({ timeout: 10000 })
  })

  test('should select group and show tiers', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // First group should be auto-selected, tiers column should have items
    const tiersColumn = page.locator('.w-52').nth(1)
    const tierItems = tiersColumn.locator('[class*="cursor-pointer"]')
    await expect(tierItems.first()).toBeVisible({ timeout: 10000 })
  })

  test('should select tier and show detail', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // Click first tier
    const tiersColumn = page.locator('.w-52').nth(1)
    const firstTier = tiersColumn.locator('[class*="cursor-pointer"]').first()
    await firstTier.click()
    await page.waitForTimeout(1000)

    // Detail panel should show tier info
    await expect(page.locator('h3:has-text("Info")')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('Tier Key')).toBeVisible()
    await expect(page.getByText('Membership Group')).toBeVisible()
  })

  test('should edit tier details', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // Select first tier
    const tiersColumn = page.locator('.w-52').nth(1)
    await tiersColumn.locator('[class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(1000)

    // Click Edit button
    await page.getByRole('button', { name: 'Edit' }).click()
    await expect(page.getByText('Save Changes')).toBeVisible({ timeout: 5000 })

    // Should show edit form with Display Name, prices, Sort Order
    await expect(page.locator('label:has-text("Display Name")')).toBeVisible()
    await expect(page.locator('label:has-text("Sort Order")')).toBeVisible()

    // Cancel edit
    await page.getByRole('button', { name: 'Cancel' }).click()
    await expect(page.getByText('Save Changes')).toBeHidden({ timeout: 3000 })
  })

  test('should create new tier', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // Click "Add Tier" button
    await page.getByRole('button', { name: 'Add Tier' }).click()
    await expect(page.getByText('Create New Tier')).toBeVisible({ timeout: 5000 })

    // Fill form
    const tierKey = `E2E_TIER_${Date.now()}`
    await page.getByPlaceholder('e.g. PREMIUM').fill(tierKey)
    await page.getByPlaceholder('e.g. Premium').fill('E2E Test Tier')

    // Create
    await page.getByRole('button', { name: 'Create', exact: true }).click()
    await page.waitForTimeout(3000)

    // Form should close and tier should appear in list
    await expect(page.getByText('Create New Tier')).toBeHidden({ timeout: 5000 })
    await expect(page.locator(`text=${tierKey}`)).toBeVisible({ timeout: 5000 })
  })

  test('should delete tier', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // First create a tier to delete
    await page.getByRole('button', { name: 'Add Tier' }).click()
    await expect(page.getByText('Create New Tier')).toBeVisible({ timeout: 5000 })

    const tierKey = `E2E_DEL_${Date.now()}`
    await page.getByPlaceholder('e.g. PREMIUM').fill(tierKey)
    await page.getByPlaceholder('e.g. Premium').fill('Delete Me')
    await page.getByRole('button', { name: 'Create', exact: true }).click()
    await page.waitForTimeout(3000)

    // The newly created tier should be selected
    await expect(page.getByRole('button', { name: 'Delete' })).toBeVisible({ timeout: 5000 })

    // Click Delete
    await page.getByRole('button', { name: 'Delete' }).click()
    // Confirm deletion
    await expect(page.getByText('Are you sure')).toBeVisible({ timeout: 3000 })
    await page.getByRole('button', { name: 'Confirm Delete' }).click()
    await page.waitForTimeout(3000)

    // Tier should be removed
    await expect(page.locator(`text=${tierKey}`)).toBeHidden({ timeout: 5000 })
  })
})
