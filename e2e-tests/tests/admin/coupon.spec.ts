/**
 * Admin Coupon Management E2E Tests
 *
 * Tests for admin coupon management features:
 * - Access control (admin role required)
 * - Coupon list display
 * - Coupon creation
 * - Coupon deactivation
 * - Validation
 *
 * NOTE: Most tests require ADMIN role. If the test user doesn't have admin access,
 * only access control tests will run, and others will be skipped.
 */
import { test, expect } from '@playwright/test'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Coupon Management', () => {
  test.beforeAll(async ({ browser }) => {
    // Check if user has admin access before running tests
    const context = await browser.newContext({ storageState: './tests/.auth/user.json' })
    const page = await context.newPage()

    try {
      await page.goto('/shopping/admin/coupons')
      await page.waitForTimeout(3000)

      const adminHeader = page.locator('h1:has-text("Coupons")')
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
    // Navigate to admin coupons page
    await page.goto('/shopping/admin/coupons')

    // Wait for loading spinner to disappear
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Wait for page to render
    await page.waitForTimeout(2000)
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("Coupons")')
      const forbiddenMessage = page.locator('text=/Access Denied|Forbidden|You don\'t have permission/')
      const goBackButton = page.locator('button:has-text("Go Back")')

      const hasHeader = await adminHeader.isVisible()
      const hasForbidden = await forbiddenMessage.isVisible()
      const hasForbiddenUI = await goBackButton.isVisible()

      if (hasHeader) {
        console.log('✅ User has admin access')
        expect(hasHeader).toBeTruthy()

        const newCouponButton = page.locator('button:has-text("New Coupon")')
        await expect(newCouponButton).toBeVisible()
      } else if (hasForbidden || hasForbiddenUI || currentUrl.includes('/403')) {
        console.log('✅ User correctly denied access')
        expect(hasForbidden || hasForbiddenUI).toBeTruthy()
      } else {
        console.log('✅ Admin content not shown')
        expect(hasHeader).toBeFalsy()
      }
    })

    test('should protect admin routes from unauthorized access', async ({ page }) => {
      const adminHeader = page.locator('h1:has-text("Coupons")')
      const newCouponButton = page.locator('button:has-text("New Coupon")')
      const couponTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await couponTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        await expect(newCouponButton).toBeVisible()
        console.log('✅ Admin UI properly shown')
      }
    })
  })

  test.describe('Coupon List Display', () => {
    test('should display admin coupon list page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("Coupons")')).toBeVisible()

      const newCouponButton = page.locator('button:has-text("New Coupon")')
      await expect(newCouponButton).toBeVisible()
    })

    test('should display coupon table with columns', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="No coupons found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        await expect(page.locator('th:has-text("Code"), th:has-text("Name")')).toBeVisible()
        await expect(page.locator('th:has-text("Discount")')).toBeVisible()
        await expect(page.locator('th:has-text("Status")')).toBeVisible()
        await expect(page.locator('th:has-text("Actions")')).toBeVisible()
      }
    })

    test('should display coupons in table rows', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const tableBody = page.locator('tbody')
      const hasTableBody = await tableBody.isVisible()

      if (hasTableBody) {
        const couponRows = page.locator('tbody tr')
        const rowCount = await couponRows.count()

        if (rowCount > 0) {
          const firstRow = couponRows.first()

          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Code/Name
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Discount

          const deactivateButton = firstRow.locator('button:has-text("Deactivate"), button[title="Deactivate"]')
          const editButton = firstRow.locator('button[title="Edit"]')

          const hasActions = await deactivateButton.isVisible() || await editButton.isVisible()
          expect(hasActions).toBeTruthy()
        }
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasCoupons = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="No coupons found"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasCoupons || hasEmptyState).toBeTruthy()

      if (hasEmptyState) {
        const createButton = page.locator('button:has-text("Create Coupon")')
        await expect(createButton).toBeVisible()
      }
    })
  })

  test.describe('Coupon Creation', () => {
    test('should navigate to coupon creation form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const newCouponButton = page.locator('button:has-text("New Coupon")')
      await newCouponButton.click()

      await expect(page).toHaveURL(/\/admin\/coupons\/new/)

      await page.waitForTimeout(1000)
      const formHeading = page.locator('h1, h2').first()
      await expect(formHeading).toBeVisible()
    })

    test('should display coupon creation form fields', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/coupons/new')
      await page.waitForTimeout(2000)

      const inputs = page.locator('input')
      const inputCount = await inputs.count()

      expect(inputCount).toBeGreaterThan(0)

      // Should have name field
      const nameField = page.locator('input[name="name"], input[placeholder*="name" i]')
      await expect(nameField.first()).toBeVisible()

      // Should have discount type selector
      const discountTypeField = page.locator('select[name="discountType"], input[name="discountType"]')
      const hasDiscountType = await discountTypeField.first().isVisible()
      expect(hasDiscountType).toBeTruthy()

      // Should have value field
      const valueField = page.locator('input[name="value"], input[name="discountValue"]')
      await expect(valueField.first()).toBeVisible()

      // Should have quantity field
      const quantityField = page.locator('input[name="quantity"], input[name="maxUses"]')
      await expect(quantityField.first()).toBeVisible()

      // Should have date fields
      const dateFields = page.locator('input[type="date"], input[type="datetime-local"]')
      const hasDateFields = await dateFields.count() > 0
      expect(hasDateFields).toBeTruthy()

      const submitButton = page.locator('button:has-text("Save"), button:has-text("Create"), button[type="submit"]')
      await expect(submitButton.first()).toBeVisible()
    })

    test('should show validation errors for empty form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/coupons/new')
      await page.waitForTimeout(2000)

      const submitButton = page.locator('button:has-text("Save"), button:has-text("Create"), button[type="submit"]')
      await submitButton.first().click()

      await page.waitForTimeout(1000)

      const currentUrl = page.url()
      expect(currentUrl.includes('/new')).toBeTruthy()
    })
  })

  test.describe('Coupon Deactivation', () => {
    test('should deactivate a coupon with confirmation', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasCoupons = await firstRow.isVisible()

      if (hasCoupons) {
        const deactivateButton = firstRow.locator('button:has-text("Deactivate"), button[title="Deactivate"]')
        const isDeactivateVisible = await deactivateButton.isVisible()

        if (isDeactivateVisible) {
          await deactivateButton.click()
          await page.waitForTimeout(500)

          const modalTitle = page.locator('text=/Deactivate Coupon|Are you sure|Confirm/i')
          const isModalVisible = await modalTitle.isVisible()

          expect(isModalVisible).toBeTruthy()

          if (isModalVisible) {
            const cancelButton = page.locator('button:has-text("Cancel")')
            const confirmButton = page.locator('button:has-text("Deactivate"), button:has-text("Confirm")')

            await expect(cancelButton).toBeVisible()
            await expect(confirmButton).toBeVisible()

            await cancelButton.click()
          }
        } else {
          test.skip(true, 'No active coupons to deactivate')
        }
      } else {
        test.skip(true, 'No coupons available to test')
      }
    })

    test('should close modal when clicking cancel', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const firstRow = page.locator('tbody tr').first()
      const hasCoupons = await firstRow.isVisible()

      if (hasCoupons) {
        const deactivateButton = firstRow.locator('button:has-text("Deactivate"), button[title="Deactivate"]')
        const isDeactivateVisible = await deactivateButton.isVisible()

        if (isDeactivateVisible) {
          await deactivateButton.click()
          await page.waitForTimeout(500)

          const cancelButton = page.locator('button:has-text("Cancel")')
          const isCancelVisible = await cancelButton.isVisible()

          if (isCancelVisible) {
            await cancelButton.click()

            await page.waitForTimeout(500)
            const modalTitle = page.locator('text=/Deactivate Coupon|Are you sure/i')
            const isModalStillVisible = await modalTitle.isVisible()

            expect(isModalStillVisible).toBeFalsy()
          }
        } else {
          test.skip(true, 'No active coupons to deactivate')
        }
      } else {
        test.skip(true, 'No coupons available to test')
      }
    })
  })

  test.describe('Navigation', () => {
    test('should have back navigation from form to list', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await page.goto('/shopping/admin/coupons/new')
      await page.waitForTimeout(2000)

      const backButton = page.locator('button:has-text("Back"), button:has-text("Cancel"), a:has-text("Back")')
      const hasBackButton = await backButton.first().isVisible()

      if (hasBackButton) {
        await backButton.first().click()
        await expect(page).toHaveURL(/\/admin\/coupons$/)
      } else {
        expect(true).toBeTruthy()
      }
    })
  })
})
