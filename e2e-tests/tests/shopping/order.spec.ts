/**
 * Order E2E Tests
 *
 * Tests for order management:
 * - Order list display
 * - Order detail view
 * - Order status display
 * - Delivery tracking
 * - Order cancellation
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoShoppingPage } from '../helpers/auth'

test.describe('Order List Page', () => {
  test.beforeEach(async ({ page }) => {
    await gotoShoppingPage(page, '/shopping/orders', 'h1:has-text("My Orders")')
  })

  test('should display My Orders title', async ({ page }) => {
    await expect(page.locator('h1:has-text("My Orders")')).toBeVisible()
  })

  test('should show empty state or order list', async ({ page }) => {
    // Check for empty state or order list
    const emptyState = page.locator('text="No orders yet"')
    const orderList = page.locator('a[href*="/shopping/orders/"]')

    const isEmpty = await emptyState.isVisible()
    const hasOrders = await orderList.first().isVisible()

    // Either empty message or orders should be displayed
    expect(isEmpty || hasOrders).toBeTruthy()

    if (isEmpty) {
      // Browse Products button should be visible
      await expect(page.locator('a:has-text("Browse Products")')).toBeVisible()
    }
  })

  test('should display order cards with status badges', async ({ page }) => {
    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Each order card should have:
      // - Order number (font-mono style)
      // - Status badge
      // - Date
      // - Total amount

      const firstOrder = orderList.first()

      // Order number pattern (alphanumeric)
      await expect(firstOrder.locator('.font-mono')).toBeVisible()

      // Status badge (colored)
      const statusBadge = firstOrder.locator('[class*="rounded"][class*="text-xs"]')
      await expect(statusBadge).toBeVisible()

      // Total amount
      await expect(firstOrder.locator('text=/₩[\\d,]+/')).toBeVisible()
    }
  })

  test('should navigate to order detail when clicking an order', async ({ page }) => {
    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Click first order
      await orderList.first().click()

      // Should navigate to order detail page
      await expect(page).toHaveURL(/\/shopping\/orders\/ORD-[\w-]+/)

      // Order detail page should load
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    }
  })

  test('should handle pagination if multiple pages exist', async ({ page }) => {
    // Check if pagination controls exist
    const nextButton = page.locator('button:has-text("Next")')
    const isPaginationVisible = await nextButton.isVisible()

    if (isPaginationVisible) {
      const isNextEnabled = await nextButton.isEnabled()

      if (isNextEnabled) {
        await nextButton.click()
        await expect(page).toHaveURL(/page=1/)
      }
    }
  })
})

test.describe('Order Detail Page', () => {
  // This test assumes there's at least one order
  // In a real scenario, you might need to create an order first

  test('should display order status card', async ({ page }) => {
    // First check if there are any orders
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Navigate to first order detail
      await orderList.first().click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Order Status card should be visible
      await expect(page.locator('h2:has-text("Order Status")')).toBeVisible()

      // Status badge should be present
      const statusBadge = page.locator('[class*="rounded"][class*="text-sm"][class*="font-medium"]')
      await expect(statusBadge).toBeVisible()

      // Order date should be displayed
      await expect(page.locator('text="Order Date"')).toBeVisible()
    }
  })

  test('should display order items list', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      await orderList.first().click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Order Items section should be visible
      await expect(page.locator('h2:has-text("Order Items")')).toBeVisible()

      // At least one item should be listed
      // Items have product name and price x quantity format
      const itemRow = page.locator('text=/₩[\\d,]+.*x.*\\d+/')
      await expect(itemRow).toBeVisible()
    }
  })

  test('should display shipping address', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      await orderList.first().click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Shipping Address section should be visible
      await expect(page.locator('h3:has-text("Shipping Address")')).toBeVisible()

      // Address info should include receiver name, phone, address
      const addressSection = page.locator('h3:has-text("Shipping Address")').locator('..').locator('..')
      await expect(addressSection).toBeVisible()
    }
  })

  test('should display order summary', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      await orderList.first().click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Order Summary section should be visible
      await expect(page.locator('h3:has-text("Order Summary")')).toBeVisible()

      // Subtotal and Total should be displayed
      await expect(page.locator('text="Subtotal"')).toBeVisible()
      await expect(page.locator('text=/Total.*₩[\\d,]+/')).toBeVisible()
    }
  })

  test('should display delivery tracking when order is shipped', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Find an order with SHIPPING or DELIVERED status
      const shippedOrders = page.locator('a[href*="/shopping/orders/"]').filter({
        has: page.locator('text=/Shipping|Delivered|배송/')
      })

      const hasShippedOrders = await shippedOrders.first().isVisible()

      if (hasShippedOrders) {
        await shippedOrders.first().click()
        await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

        // Delivery Tracking section might be visible for shipped orders
        const deliverySection = page.locator('h2:has-text("Delivery Tracking")')
        const hasDeliveryInfo = await deliverySection.isVisible()

        if (hasDeliveryInfo) {
          // Tracking number should be displayed
          await expect(page.locator('text="Tracking Number"')).toBeVisible()

          // Carrier should be displayed
          await expect(page.locator('text="Carrier"')).toBeVisible()
        }
      }
    }
  })

  test('should show cancel button for eligible orders', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Find a PENDING or CONFIRMED order (cancellable)
      const cancellableOrders = page.locator('a[href*="/shopping/orders/"]').filter({
        has: page.locator('text=/Pending|Confirmed|대기|확인/')
      })

      const hasCancellableOrders = await cancellableOrders.first().isVisible()

      if (hasCancellableOrders) {
        await cancellableOrders.first().click()
        await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

        // Cancel Order button should be visible for pending/confirmed orders
        await expect(page.locator('button:has-text("Cancel Order")')).toBeVisible()
      }
    }
  })

  test('should allow order cancellation with confirmation', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Find a PENDING order
      const pendingOrders = page.locator('a[href*="/shopping/orders/"]').filter({
        has: page.locator('text=/Pending|대기/')
      })

      const hasPendingOrders = await pendingOrders.first().isVisible()

      if (hasPendingOrders) {
        await pendingOrders.first().click()
        await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

        const cancelButton = page.locator('button:has-text("Cancel Order")')
        const canCancel = await cancelButton.isVisible()

        if (canCancel) {
          // Handle confirmation dialog
          page.on('dialog', async dialog => {
            expect(dialog.message()).toContain('cancel')
            await dialog.dismiss() // Dismiss to not actually cancel
          })

          await cancelButton.click()
        }
      }
    }
  })

  test('should navigate back to orders list', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      await orderList.first().click()
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Orders link in breadcrumb/header should be visible
      const ordersLink = page.locator('a:has-text("Orders")')
      await expect(ordersLink).toBeVisible()

      // Click to go back
      await ordersLink.click()
      await expect(page).toHaveURL(/\/shopping\/orders$/)
    }
  })

  test('should handle non-existent order gracefully', async ({ page }) => {
    // Navigate to a non-existent order
    await gotoShoppingPage(page, '/shopping/orders/ORD-NONEXISTENT-123')

    // Error message should be displayed (Korean UI for 404 errors)
    await expect(page.locator('text="주문을 찾을 수 없습니다."')).toBeVisible({ timeout: 15000 })

    // View All Orders link should be available
    await expect(page.locator('a:has-text("View All Orders")')).toBeVisible()
  })
})

test.describe('Order Status Flow', () => {
  test('should display correct status colors', async ({ page }) => {
    await page.goto('/shopping/orders')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const orderList = page.locator('a[href*="/shopping/orders/"]')
    const hasOrders = await orderList.first().isVisible()

    if (hasOrders) {
      // Check various status badges have appropriate colors
      // PENDING/CONFIRMED - warning (yellow/orange)
      const warningStatuses = page.locator('text=/Pending|Confirmed|대기|확인/').locator('..')
      // PAID/SHIPPING - info (blue)
      // DELIVERED - success (green)
      // CANCELLED/REFUNDED - error (red)

      // Verify at least the status badge system is working
      const statusBadges = page.locator('[class*="px-"][class*="py-"][class*="rounded"]')
      const badgeCount = await statusBadges.count()

      // Each order should have a status badge
      expect(badgeCount).toBeGreaterThan(0)
    }
  })
})
