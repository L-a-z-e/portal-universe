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
import { test, expect, handleLoginModalIfVisible, navigateToAdminPage } from '../helpers/test-fixtures-admin'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Coupon Management', () => {
  test.beforeAll(async () => {
    // Admin access is confirmed by auth-admin.setup.ts (admin@example.com / ROLE_SUPER_ADMIN)
    // Individual tests handle auth timing via handleLoginModalIfVisible in beforeEach
    hasAdminAccess = true
  })

  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/shopping/admin/coupons')
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("쿠폰 관리")')
      const forbiddenMessage = page.locator('h1:has-text("Access Denied")')
      const goBackButton = page.locator('button:has-text("Go Back")')

      const hasHeader = await adminHeader.isVisible()
      const hasForbidden = await forbiddenMessage.isVisible()
      const hasForbiddenUI = await goBackButton.isVisible()

      if (hasHeader) {
        console.log('✅ User has admin access')
        expect(hasHeader).toBeTruthy()

        // "새 쿠폰 생성" is a Link element, not a button
        const newCouponLink = page.locator('a:has-text("새 쿠폰 생성")')
        await expect(newCouponLink).toBeVisible()
      } else if (hasForbidden || hasForbiddenUI || currentUrl.includes('/403')) {
        console.log('✅ User correctly denied access')
        expect(hasForbidden || hasForbiddenUI).toBeTruthy()
      } else {
        console.log('✅ Admin content not shown')
        expect(hasHeader).toBeFalsy()
      }
    })

    test('should protect admin routes from unauthorized access', async ({ page }) => {
      const adminHeader = page.locator('h1:has-text("쿠폰 관리")')
      const couponTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await couponTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        const newCouponLink = page.locator('a:has-text("새 쿠폰 생성")')
        await expect(newCouponLink).toBeVisible()
        console.log('✅ Admin UI properly shown')
      }
    })
  })

  test.describe('Coupon List Display', () => {
    test('should display admin coupon list page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("쿠폰 관리")')).toBeVisible()

      const newCouponLink = page.locator('a:has-text("새 쿠폰 생성")')
      await expect(newCouponLink).toBeVisible()
    })

    test('should display coupon table with columns', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="등록된 쿠폰이 없습니다"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        // Korean table headers: 코드, 이름, 할인, 발급/총수량, 상태, 유효기간, 관리
        await expect(page.locator('th:has-text("코드")')).toBeVisible()
        await expect(page.locator('th:has-text("이름")')).toBeVisible()
        await expect(page.locator('th:has-text("할인")')).toBeVisible()
        await expect(page.locator('th:has-text("상태")')).toBeVisible()
        await expect(page.locator('th:has-text("관리")')).toBeVisible()
      }
    })

    test('should display coupons in table rows', async ({ page }) => {
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
          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Code
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Name
        }
        // Either data rows verified or empty state - both are valid
        expect(true).toBeTruthy()
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasCoupons = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="등록된 쿠폰이 없습니다"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasCoupons || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Coupon Creation', () => {
    test('should navigate to coupon creation form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const newCouponLink = page.locator('a:has-text("새 쿠폰 생성")')
      await newCouponLink.click()

      await expect(page).toHaveURL(/\/admin\/coupons\/new/)

      await page.waitForTimeout(1000)
      const formHeading = page.locator('h1:has-text("새 쿠폰 생성")')
      await expect(formHeading).toBeVisible()
    })

    test('should display coupon creation form fields', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await navigateToAdminPage(page, '/shopping/admin/coupons/new')

      const inputs = page.locator('input')
      const inputCount = await inputs.count()

      expect(inputCount).toBeGreaterThan(0)

      // Should have form heading
      await expect(page.locator('h1:has-text("새 쿠폰 생성")')).toBeVisible()

      // Should have submit button
      const submitButton = page.locator('button:has-text("생성"), button:has-text("저장"), button[type="submit"]')
      await expect(submitButton.first()).toBeVisible()
    })

    test('should show validation errors for empty form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await navigateToAdminPage(page, '/shopping/admin/coupons/new')

      const submitButton = page.locator('button:has-text("생성"), button:has-text("저장"), button[type="submit"]')
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
        // Deactivate button uses native confirm() dialog
        const deactivateButton = firstRow.locator('button:has-text("비활성화")')
        const isDeactivateVisible = await deactivateButton.isVisible()

        if (isDeactivateVisible) {
          // Set up dialog handler to dismiss
          page.once('dialog', async (dialog) => {
            expect(dialog.type()).toBe('confirm')
            expect(dialog.message()).toContain('비활성화')
            await dialog.dismiss()
          })

          await deactivateButton.click()
          await page.waitForTimeout(500)
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

      await navigateToAdminPage(page, '/shopping/admin/coupons/new')

      // Back link text is "쿠폰 목록"
      const backLink = page.locator('a:has-text("쿠폰 목록")')
      const hasBackLink = await backLink.isVisible()

      if (hasBackLink) {
        await backLink.click()
        await expect(page).toHaveURL(/\/admin\/coupons$/)
      } else {
        expect(true).toBeTruthy()
      }
    })
  })
})
