import { test, expect, navigateToAdminPage } from '../helpers/test-fixtures-admin'

test.describe('Admin Role Management', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/admin/roles')
  })

  test('should load roles page with role list cards', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    // Role list panel (w-80) should have at least one clickable role card
    const roleCards = page.locator('.w-80 [class*="cursor-pointer"]')
    await expect(roleCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('should filter roles by search', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    const searchInput = page.getByPlaceholder('Search roles...')
    await searchInput.fill('SUPER')
    await page.waitForTimeout(500)

    const roleCards = page.locator('.w-80 [class*="cursor-pointer"]')
    const count = await roleCards.count()
    expect(count).toBeGreaterThan(0)
    await expect(roleCards.first()).toContainText('SUPER')
  })

  test('should select role and show detail panel', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    await page.locator('.w-80 [class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByText('Own Permissions')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Included Roles')).toBeVisible()
    await expect(page.getByText('Effective Roles')).toBeVisible()
    await expect(page.locator('h3:has-text("Info")')).toBeVisible()
  })

  test('should create a new role', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
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
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    // Find a System-badged role in the card list
    const systemRole = page.locator('.w-80 [class*="cursor-pointer"]').filter({ hasText: 'System' }).first()
    if (!(await systemRole.isVisible({ timeout: 3000 }).catch(() => false))) {
      test.skip()
      return
    }
    await systemRole.click()
    await page.waitForTimeout(2000)
    // System roles should not have Deactivate button
    await expect(page.getByRole('button', { name: 'Deactivate' })).toBeHidden()
  })

  test('should show DAG visualization section', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    await page.locator('.w-80 [class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(2000)

    // DAG section header should be visible
    await expect(page.getByText('Role Hierarchy (DAG)')).toBeVisible({ timeout: 10000 })
    // Check SVG renders or "No hierarchy data available" message
    const svg = page.locator('[data-testid="role-dag-svg"]')
    const noData = page.getByText('No hierarchy data available')
    const hasSvg = await svg.isVisible({ timeout: 3000 }).catch(() => false)
    const hasNoData = await noData.isVisible({ timeout: 1000 }).catch(() => false)
    expect(hasSvg || hasNoData).toBeTruthy()
  })

  test('should show resolved permissions section', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    await page.locator('.w-80 [class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(2000)

    await expect(page.getByText('Resolved Permissions')).toBeVisible({ timeout: 10000 })
    // Should show permission rows with Own/Inherited badges or empty message
    const hasPerms = await page.locator('td').filter({ hasText: /Own|Inherited/ }).first()
      .isVisible({ timeout: 3000 }).catch(() => false)
    const noPerms = await page.getByText('No resolved permissions')
      .isVisible({ timeout: 1000 }).catch(() => false)
    expect(hasPerms || noPerms).toBeTruthy()
  })

  test('should show default memberships section for selected role', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    await page.locator('.w-80 [class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(2000)

    // Default Memberships section should be visible
    await expect(page.getByText('Default Memberships')).toBeVisible({ timeout: 10000 })
    // Should show mapping tags or empty message
    const hasMappings = await page.locator('text=→').first()
      .isVisible({ timeout: 3000 }).catch(() => false)
    const noMappings = await page.getByText('No default membership mappings')
      .isVisible({ timeout: 1000 }).catch(() => false)
    expect(hasMappings || noMappings).toBeTruthy()
  })

  test('should navigate to role by clicking DAG node', async ({ page }) => {
    await expect(page.locator('h1:has-text("Roles")')).toBeVisible({ timeout: 15000 })
    await page.locator('.w-80 [class*="cursor-pointer"]').first().click()
    await page.waitForTimeout(2000)

    // Expand DAG if collapsed
    const dagHeader = page.getByText('Role Hierarchy (DAG)')
    await expect(dagHeader).toBeVisible({ timeout: 10000 })

    const svg = page.locator('[data-testid="role-dag-svg"]')
    if (!(await svg.isVisible({ timeout: 3000 }).catch(() => false))) {
      // DAG may be collapsed or have no data — skip
      test.skip()
      return
    }

    // Get the current selected role name from detail panel h2 heading
    // (Using h2 instead of .font-mono to avoid matching role list items)
    const detailHeading = page.locator('h2.text-lg.font-semibold')
    const currentRoleName = await detailHeading.textContent()

    // Click a different node in the DAG
    const nodes = svg.locator('g.cursor-pointer')
    const nodeCount = await nodes.count()
    if (nodeCount < 2) {
      test.skip()
      return
    }

    // Get current role key from the detail panel paragraph (p tag, not div in the list)
    const currentRoleKey = await page.locator('p.font-mono.text-xs').first().textContent()

    // Find a node that is not the currently selected one
    for (let i = 0; i < nodeCount; i++) {
      const nodeText = await nodes.nth(i).locator('text').textContent()
      if (nodeText && nodeText !== currentRoleKey?.trim() && !nodeText.endsWith('...')) {
        await nodes.nth(i).click()
        await page.waitForTimeout(2000)
        // Detail panel heading should now show a different role
        const newRoleName = await detailHeading.textContent()
        expect(newRoleName?.trim()).not.toBe(currentRoleName?.trim())
        return
      }
    }
    // Only one unique non-truncated node — can't test navigation
    test.skip()
  })
})
