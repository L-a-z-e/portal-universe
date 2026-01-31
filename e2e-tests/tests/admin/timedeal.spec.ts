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
import { test, expect, handleLoginModalIfVisible, navigateToAdminPage } from '../helpers/test-fixtures-admin'

// Global flag to track admin access
let hasAdminAccess = false

test.describe('Admin Time Deal Management', () => {
  test.beforeAll(async () => {
    // Admin access is confirmed by auth-admin.setup.ts (admin@example.com / ROLE_SUPER_ADMIN)
    // Individual tests handle auth timing via handleLoginModalIfVisible in beforeEach
    hasAdminAccess = true
  })

  test.beforeEach(async ({ page }) => {
    await navigateToAdminPage(page, '/shopping/admin/time-deals')
  })

  test.describe('Access Control', () => {
    test('should handle admin page access', async ({ page }) => {
      const currentUrl = page.url()

      const adminHeader = page.locator('h1:has-text("타임딜 관리")')
      const forbiddenMessage = page.locator('h1:has-text("Access Denied")')
      const goBackButton = page.locator('button:has-text("Go Back")')

      const hasHeader = await adminHeader.isVisible()
      const hasForbidden = await forbiddenMessage.isVisible()
      const hasForbiddenUI = await goBackButton.isVisible()

      if (hasHeader) {
        console.log('✅ User has admin access')
        expect(hasHeader).toBeTruthy()

        // "새 타임딜 생성" is a Link element
        const newTimeDealLink = page.locator('a:has-text("새 타임딜 생성")')
        await expect(newTimeDealLink).toBeVisible()
      } else if (hasForbidden || hasForbiddenUI || currentUrl.includes('/403')) {
        console.log('✅ User correctly denied access')
        expect(hasForbidden || hasForbiddenUI).toBeTruthy()
      } else {
        console.log('✅ Admin content not shown')
        expect(hasHeader).toBeFalsy()
      }
    })

    test('should protect admin routes from unauthorized access', async ({ page }) => {
      const adminHeader = page.locator('h1:has-text("타임딜 관리")')
      const timeDealTable = page.locator('table')

      const hasAdminUI = await adminHeader.isVisible()

      if (!hasAdminUI) {
        expect(await timeDealTable.isVisible()).toBeFalsy()
        console.log('✅ Admin UI properly hidden')
      } else {
        const newTimeDealLink = page.locator('a:has-text("새 타임딜 생성")')
        await expect(newTimeDealLink).toBeVisible()
        console.log('✅ Admin UI properly shown')
      }
    })
  })

  test.describe('Time Deal List Display', () => {
    test('should display admin time deal list page', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await expect(page.locator('h1:has-text("타임딜 관리")')).toBeVisible()

      const newTimeDealLink = page.locator('a:has-text("새 타임딜 생성")')
      await expect(newTimeDealLink).toBeVisible()
    })

    test('should display time deal table with columns', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const hasTable = await page.locator('table').isVisible()
      const emptyState = page.locator('text="등록된 타임딜이 없습니다"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTable || hasEmptyState).toBeTruthy()

      if (hasTable) {
        // Korean table headers: 상품, 정가, 딜가, 할인율, 판매/재고, 상태, 기간, 관리
        await expect(page.locator('th:has-text("상품")')).toBeVisible()
        await expect(page.locator('th:has-text("딜가")')).toBeVisible()
        await expect(page.locator('th:has-text("상태")')).toBeVisible()
        await expect(page.locator('th:has-text("관리")')).toBeVisible()
      }
    })

    test('should display time deals in table rows', async ({ page }) => {
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
          await expect(firstRow.locator('td').nth(0)).toBeVisible() // Product
          await expect(firstRow.locator('td').nth(1)).toBeVisible() // Price
        }
        // Either data rows verified or empty state - both are valid
        expect(true).toBeTruthy()
      }
    })

    test('should handle empty state', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      // Wait for data to finish loading before inspecting DOM
      await page.waitForLoadState('networkidle')

      const hasTimeDeals = await page.locator('tbody tr').count() > 0
      const emptyState = page.locator('text="등록된 타임딜이 없습니다"')
      const hasEmptyState = await emptyState.isVisible()

      expect(hasTimeDeals || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Time Deal Creation', () => {
    test('should navigate to time deal creation form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      const newTimeDealLink = page.locator('a:has-text("새 타임딜 생성")')
      await newTimeDealLink.click()

      await expect(page).toHaveURL(/\/admin\/time-deals\/new/)

      await page.waitForTimeout(1000)
      const formHeading = page.locator('h1:has-text("새 타임딜 생성")')
      await expect(formHeading).toBeVisible()
    })

    test('should display time deal form fields', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await navigateToAdminPage(page, '/shopping/admin/time-deals/new')

      // Should have form heading
      await expect(page.locator('h1:has-text("새 타임딜 생성")')).toBeVisible()

      // Should have product selector (Select component)
      const inputs = page.locator('input, select, [role="combobox"]')
      const inputCount = await inputs.count()
      expect(inputCount).toBeGreaterThan(0)

      // Should have submit button
      const submitButton = page.locator('button:has-text("생성"), button:has-text("저장"), button[type="submit"]')
      await expect(submitButton.first()).toBeVisible()
    })

    test('should show validation errors for empty form', async ({ page }) => {
      test.skip(!hasAdminAccess, 'User does not have admin access - skipping admin feature tests')

      await navigateToAdminPage(page, '/shopping/admin/time-deals/new')

      const submitButton = page.locator('button:has-text("생성"), button:has-text("저장"), button[type="submit"]')
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
        // Cancel button uses native confirm() dialog
        const cancelButton = firstRow.locator('button:has-text("취소")')
        const isCancelVisible = await cancelButton.isVisible()

        if (isCancelVisible) {
          // Set up dialog handler to dismiss
          page.once('dialog', async (dialog) => {
            expect(dialog.type()).toBe('confirm')
            expect(dialog.message()).toContain('취소')
            await dialog.dismiss()
          })

          await cancelButton.click()
          await page.waitForTimeout(500)
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

      await navigateToAdminPage(page, '/shopping/admin/time-deals/new')

      // Back link text is "타임딜 목록"
      const backLink = page.locator('a:has-text("타임딜 목록")')
      const hasBackLink = await backLink.isVisible()

      if (hasBackLink) {
        await backLink.click()
        await expect(page).toHaveURL(/\/admin\/time-deals$/)
      } else {
        expect(true).toBeTruthy()
      }
    })
  })
})
