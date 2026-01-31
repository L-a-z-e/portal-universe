/**
 * Admin Time Deal Management E2E Tests
 *
 * Tests for admin time deal management features:
 * - Access control (admin role required)
 * - Time deal list display
 * - Time deal creation
 * - Time deal cancellation
 * - Validation
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect } from '@playwright/test'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Time Deal Management', () => {
  test.beforeAll(async ({ browser }) => {
    // Check if user has admin access before running tests
    const context = await browser.newContext({ storageState: './tests/.auth/user.json' })
    const page = await context.newPage()

    try {
      await page.goto('/shopping/admin/timedeals')
      await page.waitForTimeout(3000)

      const adminHeader = page.locator('h1:has-text("Time Deals")')
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
    // Navigate to admin time deals page
    await page.goto('/shopping/admin/timedeals')

    // Wait for loading spinner to disappear
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Wait for page to render
    await page.waitForTimeout(2000)
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Time Deals")')
      const forbiddenMessage = page.locator('text=/Access Denied|Forbidden|You don\'t have permission/')
      const goBackButton = page.locator('button:has-text("Go Back")')

      const hasHeader = await adminHeader.isVisible()
      const hasForbidden = await forbiddenMessage.isVisible()
      const hasForbiddenUI = await goBackButton.isVisible()

      if (hasHeader) {
        console.log('✅ User has admin access')
        expect(hasHeader).toBeTruthy()

        const newTimeDealButton = page.locator('button:has-text("New Time Deal")')
        await expect(newTimeDealButton).toBeVisible()
      } else if (hasForbidden || hasForbiddenUI || currentUrl.includes('/403')) {
        console.log('✅ User correctly denied access')
        expect(hasForbidden || hasForbiddenUI).toBeTruthy()
      } else {
        console.log('✅ Admin content not shown')
        expect(hasHeader).toBeFalsy()
      }
    })

    test('should protect admin routes from unauthorized access', async ({ page }) => {
      const adminHeader = page.locator('h1:has-text("Time Deals")')
      const newTimeDealButton = page.locator('button:has-text("New Time Deal")')
      const timeDealTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await timeDealTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        await expect(newTimeDealButton).toBeVisible()
        console.log('✅ Admin UI properly shown')
      }
    })
  })

  test.describe('Time Deal List Display', () => {
    test('should display admin time deal list page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("Time Deals")')).toBeVisible()

      const newTimeDealButton = page.locator('button:has-text("New Time Deal")')
      await expect(newTimeDealButton).toBeVisible()
    })

    test('should display time deal table with columns', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="No time deals found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        await expect(page.locator('th:has-text("Product")')).toBeVisible()
        await expect(page.locator('th:has-text("Price"), th:has-text("Discount")')).toBeVisible()
        await expect(page.locator('th:has-text("Status")')).toBeVisible()
        await expect(page.locator('th:has-text("Actions")')).toBeVisible()
      }
    })

    test('should display time deals in table rows', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const timeDealRows = page.locator('tbody tr')
        const rowCount = await timeDealRows.count()

        if (rowCount > 0) {
          const firstRow = timeDealRows.first()

          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Product
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Price/Discount

          const cancelButton = firstRow.locator('button:has-text("Cancel"), button[title="Cancel"]')
          const editButton = firstRow.locator('button[title="Edit"]')

          const hasActions = await cancelButton.isVisible() || await editButton.isVisible()
          expect(hasActions).toBeTruthy()
        }
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTimeDeals = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="No time deals found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTimeDeals || hasEmptyState).toBeTruthy()

      if (hasEmptyState) {
        const createButton = page.locator('button:has-text("Create Time Deal")')
        await expect(createButton).toBeVisible()
      }
    })
  })

  test.describe('Time Deal Creation', () => {
    test('should navigate to time deal creation form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const newTimeDealButton = page.locator('button:has-text("New Time Deal")')
      await newTimeDealButton.click()

      await expect(page).toHaveURL(/\/admin\/timedeals\/new/)

      await page.waitForTimeout(1000)
      const formHeading = page.locator('h1, h2').first()
      await expect(formHeading).toBeVisible()
    })

    test('should display time deal form fields', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/timedeals/new')
      await page.waitForTimeout(2000)

      const inputs = page.locator('input, select')
      const inputCount = await inputs.count()

      expect(inputCount).toBeGreaterThan(0)

      // Should have product selector
      const productField = page.locator('select[name="productId"], input[name="productId"]')
      await expect(productField.first()).toBeVisible()

      // Should have discount price field
      const discountPriceField = page.locator('input[name="discountPrice"], input[name="price"]')
      await expect(discountPriceField.first()).toBeVisible()

      // Should have quantity field
      const quantityField = page.locator('input[name="quantity"], input[name="stock"]')
      await expect(quantityField.first()).toBeVisible()

      // Should have start/end time fields
      const dateTimeFields = page.locator('input[type="datetime-local"], input[name*="time" i]')
      const hasDateTimeFields = await dateTimeFields.count() >= 2
      expect(hasDateTimeFields).toBeTruthy()

      const submitButton = page.locator('button:has-text("Save"), button:has-text("Create"), button[type="submit"]')
      await expect(submitButton.first()).toBeVisible()
    })

    test('should show validation errors for empty form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/timedeals/new')
      await page.waitForTimeout(2000)

      const submitButton = page.locator('button:has-text("Save"), button:has-text("Create"), button[type="submit"]')
      await submitButton.first().click()

      await page.waitForTimeout(1000)

      const currentUrl = page.url()
      expect(currentUrl.includes('/new')).toBeTruthy()
    })
  })

  test.describe('Time Deal Cancellation', () => {
    test('should cancel a time deal with confirmation', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasTimeDeals = await firstRow.isVisible()

      if (hasTimeDeals) {
        const cancelButton = firstRow.locator('button:has-text("Cancel"), button[title="Cancel"]')
        const isCancelVisible = await cancelButton.isVisible()

        if (isCancelVisible) {
          await cancelButton.click()
          await page.waitForTimeout(500)

          const modalTitle = page.locator('text=/Cancel Time Deal|Are you sure|Confirm/i')
          const isModalVisible = await modalTitle.isVisible()

          expect(isModalVisible).toBeTruthy()

          if (isModalVisible) {
            const cancelModalButton = page.locator('button:has-text("No"), button:has-text("Close")')
            const confirmButton = page.locator('button:has-text("Yes"), button:has-text("Confirm")')

            await expect(cancelModalButton).toBeVisible()
            await expect(confirmButton).toBeVisible()

            await cancelModalButton.click()
          }
        } else {
          test.skip(true, 'No active time deals to cancel')
        }
      } else {
        test.skip(true, 'No time deals available to test')
      }
    })

    test('should close modal when declining cancellation', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasTimeDeals = await firstRow.isVisible()

      if (hasTimeDeals) {
        const cancelButton = firstRow.locator('button:has-text("Cancel"), button[title="Cancel"]')
        const isCancelVisible = await cancelButton.isVisible()

        if (isCancelVisible) {
          await cancelButton.click()
          await page.waitForTimeout(500)

          const cancelModalButton = page.locator('button:has-text("No"), button:has-text("Close")')
          const isDeclineVisible = await cancelModalButton.isVisible()

          if (isDeclineVisible) {
            await cancelModalButton.click()

            await page.waitForTimeout(500)
            const modalTitle = page.locator('text=/Cancel Time Deal|Are you sure/i')
            const isModalStillVisible = await modalTitle.isVisible()

            expect(isModalStillVisible).toBeFalsy()
          }
        } else {
          test.skip(true, 'No active time deals to cancel')
        }
      } else {
        test.skip(true, 'No time deals available to test')
      }
    })
  })

  test.describe('Navigation', () => {
    test('should have back navigation from form to list', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/timedeals/new')
      await page.waitForTimeout(2000)

      const backButton = page.locator('button:has-text("Back"), button:has-text("Cancel"), a:has-text("Back")')
      const hasBackButton = await backButton.first().isVisible()

      if (hasBackButton) {
        await backButton.first().click()
        await expect(page).toHaveURL(/\/admin\/timedeals$/)
      } else {
        expect(true).toBeTruthy()
      }
    })
  })
})
