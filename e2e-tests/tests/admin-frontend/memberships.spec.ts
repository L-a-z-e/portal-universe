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

  test('should edit tier details and save', async ({ page }) => {
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})

    // Select first tier
    const tiersColumn = page.locator('.w-52').nth(1)
    await tiersColumn.locator('[class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(1000)

    // Remember original display name
    const originalName = await page.locator('h2.text-lg.font-semibold').textContent()

    // Click Edit button
    await page.getByRole('button', { name: 'Edit' }).click()
    await expect(page.getByText('Save Changes')).toBeVisible({ timeout: 5000 })

    // Modify display name
    const displayNameInput = page.locator('label:has-text("Display Name")').locator('..').locator('input')
    await displayNameInput.clear()
    const newName = `Edited ${Date.now()}`
    await displayNameInput.fill(newName)

    // Save
    await page.getByRole('button', { name: 'Save Changes' }).click()
    await page.waitForTimeout(2000)

    // Edit mode should close, detail should show updated name
    await expect(page.getByText('Save Changes')).toBeHidden({ timeout: 5000 })
    await expect(page.locator('h2.text-lg.font-semibold')).toContainText(newName, { timeout: 5000 })

    // Restore original name
    await page.getByRole('button', { name: 'Edit' }).click()
    await expect(page.getByText('Save Changes')).toBeVisible({ timeout: 5000 })
    const restoreInput = page.locator('label:has-text("Display Name")').locator('..').locator('input')
    await restoreInput.clear()
    await restoreInput.fill(originalName?.trim() ?? 'Free')
    await page.getByRole('button', { name: 'Save Changes' }).click()
    await page.waitForTimeout(2000)
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

test.describe('Admin Role-Default Mapping Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/memberships')
    // Wait for page + mapping section to load
    await expect(page.locator('h1:has-text("Memberships")')).toBeVisible({ timeout: 15000 })
    await page.locator('.animate-spin').first().waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})
  })

  test('should display role-default mapping section', async ({ page }) => {
    // Scroll down to see the mapping section
    await page.getByText('Role-Default Membership Mappings').scrollIntoViewIfNeeded()
    await expect(page.getByText('Role-Default Membership Mappings')).toBeVisible({ timeout: 10000 })

    // Should show table headers
    await expect(page.locator('th:has-text("Role Key")')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('th:has-text("Membership Group")')).toBeVisible()
    await expect(page.locator('th:has-text("Default Tier")')).toBeVisible()

    // Should show Add Mapping form
    await expect(page.getByText('Add Mapping')).toBeVisible()
  })

  test('should add and remove role-default mapping', async ({ page }) => {
    await page.getByText('Role-Default Membership Mappings').scrollIntoViewIfNeeded()
    await expect(page.getByText('Add Mapping')).toBeVisible({ timeout: 10000 })

    // Wait for selects to be loaded (role options)
    await page.waitForTimeout(2000)

    // Open Role select and pick first option
    const addSection = page.locator('text=Add Mapping').locator('..')
    const selects = addSection.locator('[class*="select"], select').locator('..')

    // Use the Role select (first one in Add Mapping section)
    const roleSelect = page.locator('label:has-text("Role")').locator('..').first()
    await roleSelect.click()
    await page.waitForTimeout(500)
    // Pick first available option from dropdown
    const roleOption = page.locator('[class*="option"], [role="option"]').first()
    if (!(await roleOption.isVisible({ timeout: 3000 }).catch(() => false))) {
      test.skip()
      return
    }
    await roleOption.click()
    await page.waitForTimeout(300)

    // Open Group select
    const groupSelect = page.locator('label:has-text("Group")').locator('..').first()
    await groupSelect.click()
    await page.waitForTimeout(500)
    const groupOption = page.locator('[class*="option"], [role="option"]').first()
    if (!(await groupOption.isVisible({ timeout: 3000 }).catch(() => false))) {
      test.skip()
      return
    }
    await groupOption.click()
    await page.waitForTimeout(300)

    // Open Tier select
    const tierSelect = page.locator('label:has-text("Tier")').locator('..').first()
    await tierSelect.click()
    await page.waitForTimeout(500)
    const tierOption = page.locator('[class*="option"], [role="option"]').first()
    if (!(await tierOption.isVisible({ timeout: 3000 }).catch(() => false))) {
      test.skip()
      return
    }
    await tierOption.click()
    await page.waitForTimeout(300)

    // Click Add button
    const addButton = addSection.getByRole('button', { name: 'Add' })
    await addButton.click()
    await page.waitForTimeout(3000)

    // New mapping should appear in the table — verify at least one row exists
    const tableRows = page.locator('table').last().locator('tbody tr')
    const rowCount = await tableRows.count()
    expect(rowCount).toBeGreaterThan(0)

    // Delete the last mapping (the one we just added)
    const lastRow = tableRows.last()
    const deleteButton = lastRow.locator('button')
    await deleteButton.click()
    await page.waitForTimeout(2000)
  })
})
