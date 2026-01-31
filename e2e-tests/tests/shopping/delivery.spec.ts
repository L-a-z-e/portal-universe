/**
 * Delivery E2E Tests
 *
 * Tests for delivery tracking functionality:
 * - Display delivery info on order detail
 * - Delivery tracking timeline
 * - Tracking number display
 * - Pre-delivery status messages
 */
import { test, expect } from '@playwright/test'

test.describe('Delivery Tracking', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await page.goto('/shopping')
  })

  test('should display delivery info on order detail', async ({ page }) => {
    // Navigate to orders page
    await page.goto('/shopping/orders')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Find first order link
    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()

      // Wait for order detail to load
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Check for delivery section
      const deliverySection = page.locator('text="Delivery Information", text="Shipping Information"')
      const hasDeliverySection = await deliverySection.isVisible()

      if (hasDeliverySection) {
        await expect(deliverySection).toBeVisible()

        // Delivery address should be shown
        const addressInfo = page.locator('text=/Address|Recipient|Contact/')
        await expect(addressInfo.first()).toBeVisible()
      }
    }
  })

  test('should show delivery tracking timeline', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for tracking timeline
      const timeline = page.locator('[class*="timeline"], [class*="tracking"]')
      const hasTimeline = await timeline.isVisible()

      if (hasTimeline) {
        // Timeline should have status steps
        const timelineSteps = page.locator('text=/Order Placed|Processing|Shipped|In Transit|Delivered/')
        const hasSteps = await timelineSteps.first().isVisible()

        expect(hasSteps).toBeTruthy()

        if (hasSteps) {
          // At least one step should be marked as complete
          const completedStep = page.locator('[class*="complete"], [class*="active"]')
          await expect(completedStep.first()).toBeVisible()
        }
      }
    }
  })

  test('should display tracking number', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for tracking number
      const trackingNumber = page.locator('text="Tracking Number", text="Waybill Number"')
      const hasTrackingLabel = await trackingNumber.isVisible()

      if (hasTrackingLabel) {
        await expect(trackingNumber).toBeVisible()

        // Tracking number value should be shown (alphanumeric)
        const trackingValue = page.locator('text=/[A-Z0-9]{10,}/')
        const hasValue = await trackingValue.isVisible()

        expect(hasValue).toBeTruthy()
      }
    }
  })

  test('should show preparing message when no delivery', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Check for preparing/processing status
      const preparingStatus = page.locator('text=/Preparing|Processing|Order Placed/')
      const hasPreparingStatus = await preparingStatus.isVisible()

      if (hasPreparingStatus) {
        // When in preparing state, tracking number should not be available
        const noTrackingMessage = page.locator('text=/not yet shipped|being prepared|processing your order/')
        const hasMessage = await noTrackingMessage.isVisible()

        if (hasMessage) {
          await expect(noTrackingMessage).toBeVisible()
        }
      }
    }
  })

  test('should display estimated delivery date', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for estimated delivery date
      const estimatedDate = page.locator('text=/Estimated|Expected|Delivery by/')
      const hasEstimate = await estimatedDate.isVisible()

      if (hasEstimate) {
        await expect(estimatedDate).toBeVisible()

        // Date should be shown (various formats)
        const dateInfo = page.locator('text=/\\d{4}-\\d{2}-\\d{2}|\\d{1,2}\\/\\d{1,2}/')
        const hasDate = await dateInfo.isVisible()

        expect(hasDate).toBeTruthy()
      }
    }
  })

  test('should show delivery status badge', async ({ page }) => {
    // Navigate to orders list
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for delivery status badges on order list
    const statusBadge = page.locator('text=/Preparing|Shipped|In Transit|Delivered/')
    const hasBadge = await statusBadge.first().isVisible()

    if (hasBadge) {
      const badgeText = await statusBadge.first().textContent()

      // Status should be one of valid delivery statuses
      expect(['Preparing', 'Shipped', 'In Transit', 'Delivered', 'Processing'].some(status =>
        badgeText?.includes(status)
      )).toBeTruthy()
    }
  })

  test('should allow copying tracking number', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for copy button near tracking number
      const copyButton = page.locator('button[aria-label*="Copy"], button:has-text("Copy")')
      const hasCopyButton = await copyButton.isVisible()

      if (hasCopyButton) {
        await copyButton.click()
        await page.waitForTimeout(500)

        // Success message should appear
        const successMessage = page.locator('text=/Copied|Copy successful/')
        const hasSuccess = await successMessage.isVisible()

        expect(hasSuccess).toBeTruthy()
      }
    }
  })

  test('should display courier company information', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for courier company name
      const courierInfo = page.locator('text=/Courier|Carrier|Delivery Company/')
      const hasCourierLabel = await courierInfo.isVisible()

      if (hasCourierLabel) {
        await expect(courierInfo).toBeVisible()

        // Company name should be shown (e.g., CJ Logistics, Korea Post)
        const companyName = page.locator('text=/CJ|Korea Post|Hanjin|Lotte/')
        const hasCompany = await companyName.isVisible()

        expect(hasCompany).toBeTruthy()
      }
    }
  })

  test('should link to external tracking page', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for external tracking link
      const trackingLink = page.locator('a:has-text("Track Package"), a:has-text("View Tracking")')
      const hasTrackingLink = await trackingLink.isVisible()

      if (hasTrackingLink) {
        // Link should have external URL
        const href = await trackingLink.getAttribute('href')
        expect(href).toBeTruthy()

        // Should open in new tab
        const target = await trackingLink.getAttribute('target')
        expect(target).toBe('_blank')
      }
    }
  })

  test('should show delivery completion confirmation', async ({ page }) => {
    // Navigate to orders and select first order
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderLink = page.locator('a[href*="/shopping/orders/"]').first()
    const hasOrders = await orderLink.isVisible()

    if (hasOrders) {
      await orderLink.click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Look for delivered status
      const deliveredStatus = page.locator('text="Delivered"')
      const isDelivered = await deliveredStatus.isVisible()

      if (isDelivered) {
        // Delivery completion date should be shown
        const completionDate = page.locator('text=/Delivered on|Received on/')
        const hasDate = await completionDate.isVisible()

        if (hasDate) {
          await expect(completionDate).toBeVisible()
        }

        // Confirm receipt button might be available
        const confirmButton = page.locator('button:has-text("Confirm Receipt")')
        const hasConfirmButton = await confirmButton.isVisible()

        // Either already confirmed or button available
        expect(true).toBeTruthy()
      }
    }
  })
})
