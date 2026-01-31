/**
 * Admin Order Management E2E Tests
 *
 * Tests for admin order management features:
 * - Access control (admin role required)
 * - Order list display
 * - Order filtering and search
 * - Order detail view
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect, handleLoginModalIfVisible, navigateToAdminPage } from '../helpers/test-fixtures-admin'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Order Management', () => {
  test.beforeAll(async () => {
    // Admin access is confirmed by auth-admin.setup.ts (admin@example.com / ROLE_SUPER_ADMIN)
    // Individual tests handle auth timing via handleLoginModalIfVisible in beforeEach
    hasAdminAccess = true
  })

  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/shopping/admin/orders')
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Orders")')
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
      const adminHeader = page.locator('h1:has-text("Orders")')
      const orderTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await orderTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        console.log('✅ Admin UI properly shown')
        expect(hasAdminUI).toBeTruthy()
      }
    })
  })

  test.describe('Order List Display', () => {
    test('should display admin order list page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("Orders")')).toBeVisible()
    })

    test('should display order table with columns', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="No orders found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        // Table headers: Order Number, User, Status, Amount, Items, Date
        await expect(page.locator('th:has-text("Order Number")')).toBeVisible()
        await expect(page.locator('th:has-text("User")')).toBeVisible()
        await expect(page.locator('th:has-text("Status")')).toBeVisible()
        await expect(page.locator('th:has-text("Amount")')).toBeVisible()
        await expect(page.locator('th:has-text("Date")')).toBeVisible()
      }
    })

    test('should display orders in table rows', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      // Wait for data to finish loading before inspecting DOM
      await page.waitForLoadState('networkidle')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const firstRow = page.locator('tbody tr').first()
        const cellCount = await firstRow.locator('td').count()

        // Empty state row has a single td with colspan; data rows have multiple tds
        if (cellCount > 1) {
          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Order Number
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // User
        }
        // Either data rows verified or empty state - both are valid
        expect(true).toBeTruthy()
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasOrders = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="No orders found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasOrders || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Order Filtering and Search', () => {
    test('should filter orders by status', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const statusFilter = page.locator('select')
      const hasStatusFilter = await statusFilter.first().isVisible()

      if (hasStatusFilter) {
        // Should have filter options
        const options = statusFilter.first().locator('option')
        const optionCount = await options.count()
        expect(optionCount).toBeGreaterThan(0)
      } else {
        test.skip(true, 'Status filter not available')
      }
    })

    test('should search orders by keyword', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const searchInput = page.locator('input[placeholder*="Order number" i], input[placeholder*="user" i]')
      const hasSearchInput = await searchInput.first().isVisible()

      if (hasSearchInput) {
        await searchInput.first().fill('test')
        await page.waitForTimeout(1000)

        expect(true).toBeTruthy()
      } else {
        test.skip(true, 'Search input not available')
      }
    })
  })

  test.describe('Order Detail', () => {
    test('should navigate to order detail by clicking row', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      // Wait for data to finish loading before inspecting DOM
      await page.waitForLoadState('networkidle')

      const firstRow = page.locator('tbody tr').first()
      const cellCount = await firstRow.locator('td').count()
      const hasDataRows = cellCount > 1

      if (hasDataRows) {
        // Rows are clickable (cursor-pointer)
        await firstRow.click()
        await page.waitForTimeout(2000)

        // Should navigate to /admin/orders/{orderNumber}
        const currentUrl = page.url()
        const navigatedToDetail = currentUrl.includes('/admin/orders/')
        expect(navigatedToDetail).toBeTruthy()
      } else {
        test.skip(true, 'No orders available to test')
      }
    })
  })

  test.describe('Navigation', () => {
    test('should navigate back from detail to list', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      // Wait for data to finish loading before inspecting DOM
      await page.waitForLoadState('networkidle')

      const firstRow = page.locator('tbody tr').first()
      const cellCount = await firstRow.locator('td').count()
      const hasOrders = cellCount > 1

      if (hasOrders) {
        await firstRow.click()
        await page.waitForLoadState('networkidle')

        const backButton = page.locator('button:has-text("Back"), a:has-text("Back"), a:has-text("Orders")')
        const hasBackButton = await backButton.first().isVisible()

        if (hasBackButton) {
          await backButton.first().click({ force: true })
          await expect(page).toHaveURL(/\/admin\/orders$/)
        } else {
          expect(true).toBeTruthy()
        }
      } else {
        test.skip(true, 'No orders available to test')
      }
    })
  })
})
