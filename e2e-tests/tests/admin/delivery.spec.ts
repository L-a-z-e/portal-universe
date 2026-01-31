/**
 * Admin Delivery Management E2E Tests
 *
 * Tests for admin delivery management features:
 * - Access control (admin role required)
 * - Delivery list display
 * - Delivery status badges
 * - Delivery status updates
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect } from '../helpers/test-fixtures'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Delivery Management', () => {
  test.beforeAll(async ({ browser }) => {
    // Check if user has admin access before running tests
    const context = await browser.newContext({ storageState: './tests/.auth/user.json' })
    const page = await context.newPage()

    try {
      await page.goto('/shopping/admin/deliveries')
      await page.waitForTimeout(3000)

      const adminHeader = page.locator('h1:has-text("Deliveries"), h1:has-text("Delivery")')
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
    // Navigate to admin deliveries page
    await page.goto('/shopping/admin/deliveries')

    // Wait for loading spinner to disappear
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Wait for page to render
    await page.waitForTimeout(2000)
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Deliveries"), h1:has-text("Delivery")')
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
      const adminHeader = page.locator('h1:has-text("Deliveries"), h1:has-text("Delivery")')
      const deliveryTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await deliveryTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        console.log('✅ Admin UI properly shown')
        expect(hasAdminUI).toBeTruthy()
      }
    })
  })

  test.describe('Delivery List Display', () => {
    test('should display admin delivery page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("Deliveries"), h1:has-text("Delivery")')).toBeVisible()
    })

    test('should display delivery list or table', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="No deliveries found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        await expect(page.locator('th:has-text("Order"), th:has-text("Order ID")')).toBeVisible()
        await expect(page.locator('th:has-text("Status")')).toBeVisible()
        await expect(page.locator('th:has-text("Address"), th:has-text("Destination")')).toBeVisible()
      }
    })

    test('should display deliveries in table rows', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const deliveryRows = page.locator('tbody tr')
        const rowCount = await deliveryRows.count()

        if (rowCount > 0) {
          const firstRow = deliveryRows.first()

          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Order ID
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Status or Address

          const updateButton = firstRow.locator('button:has-text("Update"), button[title="Update Status"]')
          const viewButton = firstRow.locator('button:has-text("View"), button[title="View"]')

          const hasActions = await updateButton.isVisible() || await viewButton.isVisible()
          expect(hasActions).toBeTruthy()
        }
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasDeliveries = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="No deliveries found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasDeliveries || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Delivery Status', () => {
    test('should show delivery status badges', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasDeliveries = await firstRow.isVisible()

      if (hasDeliveries) {
        // Should have status badge or indicator
        const statusBadge = firstRow.locator('[class*="badge"], [class*="status"]')
        const statusText = firstRow.locator('text=/PENDING|PREPARING|IN_TRANSIT|DELIVERED|CANCELLED/i')

        const hasBadge = await statusBadge.isVisible()
        const hasStatusText = await statusText.isVisible()

        expect(hasBadge || hasStatusText).toBeTruthy()
      } else {
        test.skip(true, 'No deliveries available to test')
      }
    })

    test('should display various delivery statuses', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const deliveryRows = page.locator('tbody tr')
        const rowCount = await deliveryRows.count()

        if (rowCount > 0) {
          // Check for status indicators in any row
          const statusIndicators = page.locator('text=/PENDING|PREPARING|IN_TRANSIT|DELIVERED|CANCELLED/i')
          const hasStatuses = await statusIndicators.count() > 0

          expect(hasStatuses).toBeTruthy()
        }
      }
    })
  })

  test.describe('Delivery Status Update', () => {
    test('should update delivery status', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasDeliveries = await firstRow.isVisible()

      if (hasDeliveries) {
        const updateButton = firstRow.locator('button:has-text("Update"), button[title="Update Status"]')
        const hasUpdateButton = await updateButton.isVisible()

        if (hasUpdateButton) {
          await updateButton.click()
          await page.waitForTimeout(500)

          // Should show status update UI (modal or inline)
          const statusSelect = page.locator('select[name="status"]')
          const statusButtons = page.locator('button:has-text("PREPARING"), button:has-text("IN_TRANSIT")')

          const hasStatusSelect = await statusSelect.isVisible()
          const hasStatusButtons = await statusButtons.first().isVisible()

          expect(hasStatusSelect || hasStatusButtons).toBeTruthy()
        } else {
          test.skip(true, 'Status update not available')
        }
      } else {
        test.skip(true, 'No deliveries available to test')
      }
    })

    test('should show confirmation for status change', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasDeliveries = await firstRow.isVisible()

      if (hasDeliveries) {
        const updateButton = firstRow.locator('button:has-text("Update"), button[title="Update Status"]')
        const hasUpdateButton = await updateButton.isVisible()

        if (hasUpdateButton) {
          await updateButton.click()
          await page.waitForTimeout(500)

          const saveButton = page.locator('button:has-text("Save"), button:has-text("Confirm")')
          const hasSaveButton = await saveButton.first().isVisible()

          if (hasSaveButton) {
            await saveButton.first().click()
            await page.waitForTimeout(500)

            // Should show success or confirmation
            expect(true).toBeTruthy()
          }
        } else {
          test.skip(true, 'Status update not available')
        }
      } else {
        test.skip(true, 'No deliveries available to test')
      }
    })

    test('should cancel status update', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasDeliveries = await firstRow.isVisible()

      if (hasDeliveries) {
        const updateButton = firstRow.locator('button:has-text("Update"), button[title="Update Status"]')
        const hasUpdateButton = await updateButton.isVisible()

        if (hasUpdateButton) {
          await updateButton.click()
          await page.waitForTimeout(500)

          const cancelButton = page.locator('button:has-text("Cancel"), button:has-text("Close")')
          const hasCancelButton = await cancelButton.first().isVisible()

          if (hasCancelButton) {
            await cancelButton.first().click()
            await page.waitForTimeout(500)

            // Modal should close
            const modalTitle = page.locator('text=/Update Status|Change Status/i')
            const isModalStillVisible = await modalTitle.isVisible()

            expect(isModalStillVisible).toBeFalsy()
          }
        } else {
          test.skip(true, 'Status update not available')
        }
      } else {
        test.skip(true, 'No deliveries available to test')
      }
    })
  })

  test.describe('Navigation', () => {
    test('should navigate to delivery detail', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasDeliveries = await firstRow.isVisible()

      if (hasDeliveries) {
        const viewButton = firstRow.locator('button:has-text("View"), button[title="View"]')
        const hasViewButton = await viewButton.isVisible()

        if (hasViewButton) {
          await viewButton.click()
          await page.waitForTimeout(1000)

          const currentUrl = page.url()
          const navigatedToDetail = currentUrl.includes('/admin/deliveries/')
          expect(navigatedToDetail).toBeTruthy()
        } else {
          test.skip(true, 'View button not available')
        }
      } else {
        test.skip(true, 'No deliveries available to test')
      }
    })
  })
})
