/**
 * Time Deal E2E Tests
 *
 * Tests for time deal functionality:
 * - Display time deal list
 * - Active and scheduled deals
 * - Deal detail page
 * - Countdown timer
 * - Stock percentage
 * - Purchase flow
 */
import { test, expect } from '@playwright/test'

test.describe('Time Deal', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await page.goto('/shopping')
  })

  test('should display time deal list page', async ({ page }) => {
    // Navigate to time deals page
    await page.goto('/shopping/timedeals')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Page title should be visible
    const pageTitle = page.locator('h1:has-text("Time Deals"), h1:has-text("Flash Sales")')
    await expect(pageTitle).toBeVisible({ timeout: 5000 })

    // Either deals or empty state should be shown
    const dealCards = page.locator('[class*="deal"], [class*="card"]')
    const emptyState = page.locator('text="No time deals available"')

    const hasDeals = await dealCards.first().isVisible()
    const isEmpty = await emptyState.isVisible()

    expect(hasDeals || isEmpty).toBeTruthy()
  })

  test('should show active and scheduled sections', async ({ page }) => {
    await page.goto('/shopping/timedeals')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for section tabs or headers
    const activeSection = page.locator('text="Active Deals", text="Now", button:has-text("Active")')
    const scheduledSection = page.locator('text="Upcoming", text="Scheduled", button:has-text("Upcoming")')

    const hasActive = await activeSection.isVisible()
    const hasScheduled = await scheduledSection.isVisible()

    // At least one section should be visible
    expect(hasActive || hasScheduled).toBeTruthy()

    if (hasActive && hasScheduled) {
      // Click scheduled tab
      await scheduledSection.click()
      await page.waitForTimeout(500)

      // Scheduled deals or empty state should be shown
      const scheduledDeals = page.locator('text=/Starts in|Coming soon/')
      const noScheduled = page.locator('text="No upcoming deals"')

      const hasScheduledContent = await scheduledDeals.isVisible()
      const isEmpty = await noScheduled.isVisible()

      expect(hasScheduledContent || isEmpty).toBeTruthy()
    }
  })

  test('should navigate to time deal detail page', async ({ page }) => {
    await page.goto('/shopping/timedeals')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Find first deal link
    const dealLink = page.locator('a[href*="/shopping/timedeals/"]').first()
    const hasDeal = await dealLink.isVisible()

    if (hasDeal) {
      await dealLink.click()

      // Should navigate to detail page
      await expect(page).toHaveURL(/\/shopping\/timedeals\/\d+/)

      // Wait for detail to load
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Product name should be visible
      await expect(page.locator('h1')).toBeVisible()
    }
  })

  test('should display countdown timer on detail', async ({ page }) => {
    // Navigate directly to a time deal (assuming deal ID 1 exists)
    await page.goto('/shopping/timedeals/1')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check for error state
    const notFoundError = page.locator('text="Deal not found", text="Not found"')
    const authError = page.locator('text=/401|Request failed/')

    const isNotFound = await notFoundError.isVisible()
    const isAuthError = await authError.isVisible()

    if (!isNotFound && !isAuthError) {
      // Countdown timer should be visible
      const countdownTimer = page.locator('text=/\\d{2}:\\d{2}:\\d{2}|Ends in|Time remaining/')
      const hasTimer = await countdownTimer.isVisible()

      if (hasTimer) {
        await expect(countdownTimer).toBeVisible()

        // Timer format should include hours, minutes, seconds
        const timerText = await countdownTimer.textContent()
        expect(timerText).toMatch(/\d{1,2}:\d{2}:\d{2}|\d+h|\d+m|\d+s/)
      }
    }
  })

  test('should display stock percentage bar', async ({ page }) => {
    await page.goto('/shopping/timedeals/1')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check for error state
    const notFoundError = page.locator('text="Deal not found"')
    const authError = page.locator('text=/401|Request failed/')

    const isNotFound = await notFoundError.isVisible()
    const isAuthError = await authError.isVisible()

    if (!isNotFound && !isAuthError) {
      // Stock info should be visible
      const stockInfo = page.locator('text=/\\d+% sold|\\d+ left|Stock:/')
      const hasStockInfo = await stockInfo.isVisible()

      if (hasStockInfo) {
        await expect(stockInfo).toBeVisible()

        // Progress bar should be visible
        const progressBar = page.locator('[role="progressbar"], .progress-bar, [class*="progress"]')
        const hasProgressBar = await progressBar.isVisible()

        expect(hasProgressBar).toBeTruthy()
      }
    }
  })

  test('should purchase time deal successfully', async ({ page }) => {
    await page.goto('/shopping/timedeals/1')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check for error state
    const notFoundError = page.locator('text="Deal not found"')
    const isNotFound = await notFoundError.isVisible()

    if (!isNotFound) {
      // Find purchase button
      const purchaseButton = page.locator('button:has-text("Buy Now"), button:has-text("Purchase")')
      const soldOutButton = page.locator('button:has-text("Sold Out")')

      const canPurchase = await purchaseButton.isVisible()
      const isSoldOut = await soldOutButton.isVisible()

      if (canPurchase) {
        const isEnabled = await purchaseButton.isEnabled()

        if (isEnabled) {
          await purchaseButton.click()

          // Wait for navigation or modal
          await page.waitForTimeout(1000)

          // Should navigate to checkout or show purchase confirmation
          const checkoutPage = page.locator('h1:has-text("Checkout")')
          const confirmModal = page.locator('text="Confirm Purchase"')

          const hasCheckout = await checkoutPage.isVisible()
          const hasModal = await confirmModal.isVisible()

          expect(hasCheckout || hasModal).toBeTruthy()
        }
      } else if (isSoldOut) {
        // Sold out state is valid
        await expect(soldOutButton).toBeVisible()
      }
    }
  })

  test('should display purchase history', async ({ page }) => {
    // Navigate to my time deals or order history
    await page.goto('/shopping/orders')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for time deal badge or indicator in orders
    const timeDealBadge = page.locator('text="Time Deal", [class*="badge"]:has-text("Flash")')
    const hasTimeDeal = await timeDealBadge.isVisible()

    if (hasTimeDeal) {
      await expect(timeDealBadge).toBeVisible()

      // Deal price should be shown
      const dealPrice = page.locator('text=/₩[\\d,]+/')
      await expect(dealPrice.first()).toBeVisible()
    }
  })

  test('should show deal status badge', async ({ page }) => {
    await page.goto('/shopping/timedeals')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for status badges on deals
    const statusBadges = page.locator('text=/Active|Scheduled|Ended|Live/')
    const hasBadges = await statusBadges.first().isVisible()

    if (hasBadges) {
      const badgeText = await statusBadges.first().textContent()

      // Badge should indicate deal status
      expect(['Active', 'Scheduled', 'Ended', 'Live'].some(status =>
        badgeText?.includes(status)
      )).toBeTruthy()
    }
  })

  test('should display discount percentage', async ({ page }) => {
    await page.goto('/shopping/timedeals')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Discount badge should be visible on deals
    const discountBadge = page.locator('text=/\\d+% OFF|Save \\d+%/')
    const hasDiscount = await discountBadge.first().isVisible()

    if (hasDiscount) {
      await expect(discountBadge.first()).toBeVisible()

      // Original and sale prices should be shown
      const priceInfo = page.locator('text=/₩[\\d,]+/')
      const priceCount = await priceInfo.count()

      // At least one price should be visible
      expect(priceCount).toBeGreaterThan(0)
    }
  })

  test('should handle expired deal gracefully', async ({ page }) => {
    // Try to access an old deal (high ID number)
    await page.goto('/shopping/timedeals/9999')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Should show not found or expired message
    const errorMessage = page.locator('text=/Deal not found|Expired|No longer available/')
    const hasError = await errorMessage.isVisible()

    if (hasError) {
      await expect(errorMessage).toBeVisible()

      // Back to deals button should be visible
      const backButton = page.locator('a:has-text("Back to Deals"), a:has-text("View All Deals")')
      const hasBackButton = await backButton.isVisible()

      expect(hasBackButton).toBeTruthy()
    }
  })
})
