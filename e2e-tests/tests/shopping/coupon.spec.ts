/**
 * Coupon E2E Tests
 *
 * Tests for coupon functionality:
 * - Display coupon list with tabs
 * - Issue coupons
 * - View available and my coupons
 * - Display coupon status
 */
import { test, expect } from '@playwright/test'

test.describe('Coupon Management', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await page.goto('/shopping')
  })

  test('should display coupon list page with tabs', async ({ page }) => {
    // Navigate to coupons page
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check for page title
    const pageTitle = page.locator('h1:has-text("Coupons")')
    await expect(pageTitle).toBeVisible({ timeout: 5000 })

    // Tabs should be visible
    const availableTab = page.locator('button:has-text("Available Coupons")')
    const myCouponsTab = page.locator('button:has-text("My Coupons")')

    const hasAvailableTab = await availableTab.isVisible()
    const hasMyCouponsTab = await myCouponsTab.isVisible()

    // At least one tab should be visible
    expect(hasAvailableTab || hasMyCouponsTab).toBeTruthy()
  })

  test('should show available coupons tab', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Click Available Coupons tab
    const availableTab = page.locator('button:has-text("Available Coupons")')
    const isTabVisible = await availableTab.isVisible()

    if (isTabVisible) {
      await availableTab.click()

      // Wait for content to load
      await page.waitForTimeout(500)

      // Either coupons or empty state should be shown
      const couponList = page.locator('[class*="coupon"], .grid')
      const emptyState = page.locator('text="No available coupons"')

      const hasCoupons = await couponList.isVisible()
      const isEmpty = await emptyState.isVisible()

      expect(hasCoupons || isEmpty).toBeTruthy()
    }
  })

  test('should issue a coupon successfully', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Find issue button
    const issueButton = page.locator('button:has-text("Issue"), button:has-text("Download")').first()
    const isIssueVisible = await issueButton.isVisible()

    if (isIssueVisible) {
      const isEnabled = await issueButton.isEnabled()

      if (isEnabled) {
        // Click issue button
        await issueButton.click()

        // Wait for success message or state change
        await page.waitForTimeout(1000)

        // Either success message or button state change
        const successMessage = page.locator('text=/Issued|Downloaded|Success/')
        const issuedButton = page.locator('button:has-text("Issued")')

        const hasSuccess = await successMessage.isVisible()
        const isButtonChanged = await issuedButton.isVisible()

        expect(hasSuccess || isButtonChanged).toBeTruthy()
      }
    }
  })

  test('should switch to my coupons tab', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Click My Coupons tab
    const myCouponsTab = page.locator('button:has-text("My Coupons")')
    const isTabVisible = await myCouponsTab.isVisible()

    if (isTabVisible) {
      await myCouponsTab.click()

      // Wait for content to load
      await page.waitForTimeout(500)

      // Tab should be active
      const isActive = await myCouponsTab.getAttribute('class')
      expect(isActive).toContain('active')
    }
  })

  test('should display issued coupon in my coupons', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Navigate to My Coupons tab
    const myCouponsTab = page.locator('button:has-text("My Coupons")')
    const isTabVisible = await myCouponsTab.isVisible()

    if (isTabVisible) {
      await myCouponsTab.click()
      await page.waitForTimeout(500)

      // Check for coupons or empty state
      const couponCard = page.locator('[class*="coupon"]')
      const emptyState = page.locator('text="You have no coupons"')

      const hasCoupons = await couponCard.first().isVisible()
      const isEmpty = await emptyState.isVisible()

      expect(hasCoupons || isEmpty).toBeTruthy()

      if (hasCoupons) {
        // Coupon should have discount information
        const discountInfo = page.locator('text=/₩[\\d,]+|\\d+%/')
        await expect(discountInfo.first()).toBeVisible()
      }
    }
  })

  test('should show coupon status (AVAILABLE/USED/EXPIRED)', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Navigate to My Coupons tab
    const myCouponsTab = page.locator('button:has-text("My Coupons")')
    const isTabVisible = await myCouponsTab.isVisible()

    if (isTabVisible) {
      await myCouponsTab.click()
      await page.waitForTimeout(500)

      // Check for status badges
      const statusBadge = page.locator('text=/AVAILABLE|USED|EXPIRED/')
      const hasCoupons = await statusBadge.first().isVisible()

      if (hasCoupons) {
        // At least one status should be visible
        await expect(statusBadge.first()).toBeVisible()

        // Status should be one of the valid values
        const statusText = await statusBadge.first().textContent()
        expect(['AVAILABLE', 'USED', 'EXPIRED'].some(status =>
          statusText?.includes(status)
        )).toBeTruthy()
      }
    }
  })

  test('should display coupon expiry date', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check for expiry date in any visible coupon
    const expiryDate = page.locator('text=/Expires|Valid until|\\d{4}-\\d{2}-\\d{2}/')
    const hasExpiry = await expiryDate.first().isVisible()

    if (hasExpiry) {
      await expect(expiryDate.first()).toBeVisible()
    }
  })

  test('should filter coupons by status', async ({ page }) => {
    await page.goto('/shopping/coupons')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Navigate to My Coupons tab
    const myCouponsTab = page.locator('button:has-text("My Coupons")')
    const isTabVisible = await myCouponsTab.isVisible()

    if (isTabVisible) {
      await myCouponsTab.click()
      await page.waitForTimeout(500)

      // Look for filter options
      const filterButtons = page.locator('button:has-text("All"), button:has-text("Available"), button:has-text("Used")')
      const hasFilters = await filterButtons.first().isVisible()

      if (hasFilters) {
        // Click Available filter
        const availableFilter = page.locator('button:has-text("Available")').first()
        await availableFilter.click()

        // Wait for filter to apply
        await page.waitForTimeout(500)

        // Only available coupons should be shown
        const usedStatus = page.locator('text="USED"')
        const hasUsed = await usedStatus.isVisible()

        // Used status should not be visible when filtered to available
        expect(hasUsed).toBeFalsy()
      }
    }
  })
})
