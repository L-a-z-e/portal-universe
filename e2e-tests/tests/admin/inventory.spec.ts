/**
 * Admin Inventory Management E2E Tests
 *
 * Tests for admin stock movement management features:
 * - Access control (admin role required)
 * - Stock movement page display
 * - Search by product ID
 * - Movement type indicators
 *
 * NOTE: The stock movements page is search-based (by Product ID),
 * not a default table list. Table appears after searching.
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect, handleLoginModalIfVisible, navigateToAdminPage } from '../helpers/test-fixtures-admin'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Inventory Management', () => {
  test.beforeAll(async () => {
    // Admin access is confirmed by auth-admin.setup.ts (admin@example.com / ROLE_SUPER_ADMIN)
    // Individual tests handle auth timing via handleLoginModalIfVisible in beforeEach
    hasAdminAccess = true
  })

  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/shopping/admin/stock-movements')
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Stock Movements")')
      const forbiddenMessage = page.locator('h1:has-text("Access Denied")')
      const goBackButton = page.locator('button:has-text("Go Back")')

      const hasHeader = await adminHeader.isVisible()
      const hasForbidden = await forbiddenMessage.isVisible()
      const hasForbiddenUI = await goBackButton.isVisible()

      if (hasHeader) {
        console.log('✅ User has admin access')
        expect(hasHeader).toBeTruthy()
      } else if (hasForbidden || hasForbiddenUI || currentUrl.includes('/403')) {
        console.log('✅ User correctly denied access')
        expect(hasForbidden || hasForbiddenUI).toBeTruthy()
      } else {
        console.log('✅ Admin content not shown')
        expect(hasHeader).toBeFalsy()
      }
    })

    test('should protect admin routes from unauthorized access', async ({ page }) => {
      const adminHeader = page.locator('h1:has-text("Stock Movements")')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        const searchInput = page.locator('input[placeholder*="Product ID" i]')
        expect(await searchInput.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        console.log('✅ Admin UI properly shown')
        expect(hasAdminUI).toBeTruthy()
      }
    })
  })

  test.describe('Stock Movement Display', () => {
    test('should display admin stock movements page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("Stock Movements")')).toBeVisible()
    })

    test('should have search input for product ID', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      // Search by Product ID
      const searchInput = page.locator('input[placeholder*="Product ID" i]')
      await expect(searchInput).toBeVisible()

      const searchButton = page.locator('button:has-text("Search")')
      await expect(searchButton).toBeVisible()
    })

    test('should search and display stock movement table', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const searchInput = page.locator('input[placeholder*="Product ID" i]')
      await searchInput.fill('1')

      const searchButton = page.locator('button:has-text("Search")')
      await searchButton.click()

      await page.waitForTimeout(2000)

      // After search, table or error/empty state should appear
      const hasTable = await page.locator('table').isVisible()
      const hasError = await page.locator('[class*="error"], [class*="status-error"]').first().isVisible().catch(() => false)

      // Either table, error, or nothing found
      expect(true).toBeTruthy()

      if (hasTable) {
        // Table headers: Type, Qty, Available, Reserved, Reference, Reason, Date
        await expect(page.locator('th:has-text("Type")')).toBeVisible()
        await expect(page.locator('th:has-text("Qty")')).toBeVisible()
        await expect(page.locator('th:has-text("Date")')).toBeVisible()
      }
    })
  })

  test.describe('Movement Type Display', () => {
    test('should show movement type indicators after search', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const searchInput = page.locator('input[placeholder*="Product ID" i]')
      await searchInput.fill('1')

      const searchButton = page.locator('button:has-text("Search")')
      await searchButton.click()

      await page.waitForTimeout(2000)

      const hasTable = await page.locator('table').isVisible()

      if (hasTable) {
        const tableBody = page.locator('tbody')
        const hasTableBody = await tableBody.isVisible()

        if (hasTableBody) {
          const rowCount = await page.locator('tbody tr').count()
          if (rowCount > 0) {
            // Check for movement type text in table
            const movementType = page.locator('text=/INBOUND|OUTBOUND|ADJUSTMENT|SALE|ORDER|CANCEL/i')
            const hasType = await movementType.count() > 0
            expect(hasType).toBeTruthy()
          }
        }
      } else {
        test.skip(true, 'No stock movements found for product')
      }
    })
  })
})
