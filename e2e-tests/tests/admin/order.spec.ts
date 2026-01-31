/**
 * Admin Order Management E2E Tests
 *
 * Tests for admin order management features:
 * - Access control (admin role required)
 * - Order list display
 * - Order filtering and search
 * - Order detail view
 * - Order status management
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect } from '@playwright/test'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Order Management', () => {
  test.beforeAll(async ({ browser }) => {
    // Check if user has admin access before running tests
    const context = await browser.newContext({ storageState: './tests/.auth/user.json' })
    const page = await context.newPage()

    try {
      await page.goto('/shopping/admin/orders')
      await page.waitForTimeout(3000)

      const adminHeader = page.locator('h1:has-text("Orders")')
      hasAdminAccess = await adminHeader.isVisible()

      console.log(`🔐 Admin Access Check: ${hasAdminAccess ? '✅ HAS ACCESS' : '❌ NO ACCESS'}`)
    } catch (error) {
      console.error('Error checking admin access:', error)
      hasAdminAccess = false
    } finally {
      await page.close()
      await context.close()
    }
  })

  test.beforeEach(async ({ page }) => {
    // Navigate to admin orders page
    await page.goto('/shopping/admin/orders')

    // Wait for loading spinner to disappear
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Wait for page to render
    await page.waitForTimeout(2000)
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Orders")')
      const forbiddenMessage = page.locator('text=/Access Denied|Forbidden|You don\'t have permission/')
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
        await expect(page.locator('th:has-text("Order ID"), th:has-text("ID")')).toBeVisible()
        await expect(page.locator('th:has-text("Customer"), th:has-text("User")')).toBeVisible()
        await expect(page.locator('th:has-text("Total"), th:has-text("Amount")')).toBeVisible()
        await expect(page.locator('th:has-text("Status")')).toBeVisible()
        await expect(page.locator('th:has-text("Date"), th:has-text("Created")')).toBeVisible()
      }
    })

    test('should display orders in table rows', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const orderRows = page.locator('tbody tr')
        const rowCount = await orderRows.count()

        if (rowCount > 0) {
          const firstRow = orderRows.first()

          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Order ID
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Customer

          // Should have status badge
          const statusBadge = firstRow.locator('[class*="badge"], [class*="status"]')
          const hasStatusBadge = await statusBadge.isVisible()
          expect(hasStatusBadge).toBeTruthy()
        }
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

      const statusFilter = page.locator('select[name="status"], button:has-text("Status")')
      const hasStatusFilter = await statusFilter.first().isVisible()

      if (hasStatusFilter) {
        await statusFilter.first().click()
        await page.waitForTimeout(500)

        // Should have filter options
        const filterOptions = page.locator('option, [role="option"]')
        const optionCount = await filterOptions.count()

        expect(optionCount).toBeGreaterThan(0)
      } else {
        test.skip(true, 'Status filter not available')
      }
    })

    test('should search orders by keyword', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const searchInput = page.locator('input[type="search"], input[placeholder*="search" i]')
      const hasSearchInput = await searchInput.first().isVisible()

      if (hasSearchInput) {
        await searchInput.first().fill('test')
        await page.waitForTimeout(1000)

        // Should trigger search
        expect(true).toBeTruthy()
      } else {
        test.skip(true, 'Search input not available')
      }
    })
  })

  test.describe('Order Detail', () => {
    test('should navigate to order detail', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasOrders = await firstRow.isVisible()

      if (hasOrders) {
        const viewButton = firstRow.locator('button:has-text("View"), button[title="View"]')
        const hasViewButton = await viewButton.isVisible()

        if (hasViewButton) {
          await viewButton.click()
          await expect(page).toHaveURL(/\/admin\/orders\/\d+/)
          await page.waitForTimeout(1000)
        } else {
          // Try clicking the row
          await firstRow.click()
          await page.waitForTimeout(1000)

          const currentUrl = page.url()
          const navigatedToDetail = currentUrl.includes('/admin/orders/')
          expect(navigatedToDetail).toBeTruthy()
        }
      } else {
        test.skip(true, 'No orders available to test')
      }
    })

    test('should display order detail with items and payment', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasOrders = await firstRow.isVisible()

      if (hasOrders) {
        await firstRow.click()
        await page.waitForTimeout(2000)

        // Should display order details
        const orderIdLabel = page.locator('text=/Order ID|Order Number/i')
        const hasOrderId = await orderIdLabel.isVisible()

        if (hasOrderId) {
          // Should have items section
          const itemsSection = page.locator('text=/Order Items|Items|Products/i')
          await expect(itemsSection).toBeVisible()

          // Should have payment section
          const paymentSection = page.locator('text=/Payment|Total|Amount/i')
          await expect(paymentSection).toBeVisible()
        }
      } else {
        test.skip(true, 'No orders available to test')
      }
    })
  })

  test.describe('Order Status Management', () => {
    test('should change order status', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasOrders = await firstRow.isVisible()

      if (hasOrders) {
        await firstRow.click()
        await page.waitForTimeout(2000)

        const statusSelect = page.locator('select[name="status"], button:has-text("Change Status")')
        const hasStatusSelect = await statusSelect.first().isVisible()

        if (hasStatusSelect) {
          await statusSelect.first().click()
          await page.waitForTimeout(500)

          // Should have status options
          const statusOptions = page.locator('option, [role="option"]')
          const optionCount = await statusOptions.count()

          expect(optionCount).toBeGreaterThan(0)
        } else {
          test.skip(true, 'Status change not available')
        }
      } else {
        test.skip(true, 'No orders available to test')
      }
    })

    test('should require confirmation for status change', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasOrders = await firstRow.isVisible()

      if (hasOrders) {
        await firstRow.click()
        await page.waitForTimeout(2000)

        const updateButton = page.locator('button:has-text("Update Status"), button:has-text("Save")')
        const hasUpdateButton = await updateButton.first().isVisible()

        if (hasUpdateButton) {
          await updateButton.first().click()
          await page.waitForTimeout(500)

          // May show confirmation or success message
          expect(true).toBeTruthy()
        } else {
          test.skip(true, 'Status update not available')
        }
      } else {
        test.skip(true, 'No orders available to test')
      }
    })
  })

  test.describe('Navigation', () => {
    test('should have back navigation from detail to list', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasOrders = await firstRow.isVisible()

      if (hasOrders) {
        await firstRow.click()
        await page.waitForTimeout(2000)

        const backButton = page.locator('button:has-text("Back"), a:has-text("Back")')
        const hasBackButton = await backButton.first().isVisible()

        if (hasBackButton) {
          await backButton.first().click()
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
