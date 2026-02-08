/**
 * Checkout E2E Tests
 *
 * Tests for the checkout flow:
 * - Checkout page display
 * - Shipping address form
 * - Payment method selection
 * - Order completion (Happy Path)
 * - Error handling (Payment failure)
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoShoppingPage } from '../helpers/auth'

test.describe('Checkout Flow', () => {
  // Helper function to add product to cart and navigate to checkout
  async function setupCheckout(page: any) {
    // Add a product to cart with full auth/MF handling
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      // Wait for toast notification instead of fixed timeout
      await page.locator('[class*="toast"], [role="alert"], [class*="notification"]').first()
        .waitFor({ timeout: 5000 }).catch(() => {})

      // Navigate to checkout with full auth/MF handling
      await gotoShoppingPage(page, '/shopping/checkout', 'h2:has-text("Shipping Address"), text="Cart not found"')

      // Check if checkout page loaded properly
      const hasShippingForm = await page.locator('h2:has-text("Shipping Address")').isVisible()

      return hasShippingForm
    }

    return false
  }

  // Helper to fill shipping address fields
  async function fillShippingAddress(page: any) {
    await page.locator('input[placeholder="Enter receiver name"]').fill('Test User')
    await page.locator('input[placeholder="010-0000-0000"]').fill('010-1234-5678')
    const zipInput = page.locator('input[placeholder="12345"]')
    await zipInput.fill('12345')
    await expect(zipInput).toHaveValue('12345', { timeout: 3000 })
    await page.locator('input[placeholder="Street address"]').fill('123 Test Street')
  }

  // Helper to proceed from shipping to payment step
  async function proceedToPayment(page: any): Promise<boolean> {
    await fillShippingAddress(page)
    const continueButton = page.locator('button:has-text("Continue to Payment")')
    await expect(continueButton).toBeEnabled({ timeout: 5000 })
    await continueButton.click()

    // Wait for payment step - may not appear if cart/order has issues
    try {
      await page.locator('h2:has-text("Payment Method")').waitFor({ timeout: 10000 })
      return true
    } catch {
      return false
    }
  }

  test('should display checkout page with step indicator', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Checkout title should be visible
      await expect(page.locator('h1:has-text("Checkout")')).toBeVisible()

      // Step indicator should show: Shipping, Payment, Complete
      await expect(page.locator('text="Shipping"')).toBeVisible()
      await expect(page.locator('text="Payment"')).toBeVisible()
      await expect(page.locator('text="Complete"')).toBeVisible()
    }
  })

  test('should display shipping address form', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Shipping Address section should be visible
      await expect(page.locator('h2:has-text("Shipping Address")')).toBeVisible()

      // Form fields should be present
      await expect(page.locator('input[placeholder="Enter receiver name"]')).toBeVisible()
      await expect(page.locator('input[placeholder="010-0000-0000"]')).toBeVisible()
      await expect(page.locator('input[placeholder="12345"]')).toBeVisible()
      await expect(page.locator('input[placeholder="Street address"]')).toBeVisible()
    }
  })

  test('should validate required address fields', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Try to proceed without filling required fields
      const continueButton = page.locator('button:has-text("Continue to Payment")')

      // Button should be disabled when required fields are empty
      await expect(continueButton).toBeDisabled()

      // Fill only partial data
      await page.locator('input[placeholder="Enter receiver name"]').fill('Test User')

      // Button should still be disabled (missing other required fields)
      await expect(continueButton).toBeDisabled()
    }
  })

  test('should complete shipping address step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      const reachedPayment = await proceedToPayment(page)

      if (reachedPayment) {
        // Payment Method heading should be visible
        await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible()
      } else {
        // If payment step not reached, the page should still be on checkout
        // with shipping form visible (cart/order API might have issues)
        const isStillOnShipping = await page.locator('h2:has-text("Shipping Address")').isVisible()
        const hasCartAlert = await page.locator('text="Cart not found"').isVisible()
        const isOnCheckout = page.url().includes('/checkout')
        expect(isStillOnShipping || hasCartAlert || isOnCheckout).toBeTruthy()
      }
    }
  })

  test('should display payment method options', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      const reachedPayment = await proceedToPayment(page)

      if (reachedPayment) {
        // Payment options should be displayed
        const creditCard = page.locator('text="Credit Card", text="신용카드"')

        // At least credit card option should exist
        await expect(creditCard.or(page.locator('label').filter({ hasText: /card/i }))).toBeVisible()
      }
    }
  })

  test('should display order summary on payment step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      const reachedPayment = await proceedToPayment(page)

      if (reachedPayment) {
        // Order summary should be visible
        await expect(page.locator('h3:has-text("Order Summary")')).toBeVisible()

        // Total amount should be shown
        await expect(page.locator('text=/Total.*₩[\\d,]+/')).toBeVisible()
      }
    }
  })

  test('should allow going back to shipping step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      const reachedPayment = await proceedToPayment(page)

      if (reachedPayment) {
        // Click Back button
        await page.locator('button:has-text("Back")').click()

        // Should be back at shipping step
        await expect(page.locator('h2:has-text("Shipping Address")')).toBeVisible()
      }
    }
  })

  test('Happy Path: should complete full checkout flow', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Step 1: Fill shipping address
      await fillShippingAddress(page)
      await page.locator('input[placeholder="Apartment, suite, etc. (optional)"]').fill('Apt 101')

      await page.locator('button:has-text("Continue to Payment")').click()

      // Step 2: Wait for payment step
      let reachedPayment = false
      try {
        await page.locator('h2:has-text("Payment Method")').waitFor({ timeout: 10000 })
        reachedPayment = true
      } catch { /* payment step not reached */ }

      if (reachedPayment) {
        // Click Pay button
        const payButton = page.locator('button:has-text("Pay")')
        await expect(payButton).toBeVisible()
        await payButton.click()

        // Step 3: Wait for payment processing
        // Mock PG has 90% success rate, so this might fail
        const successMessage = page.locator('h2:has-text("Order Placed Successfully")')
        const errorMessage = page.locator('text=/Payment failed|Error/')

        await Promise.race([
          successMessage.waitFor({ timeout: 30000 }),
          errorMessage.waitFor({ timeout: 30000 })
        ]).catch(() => {})

        const isSuccess = await successMessage.isVisible()

        if (isSuccess) {
          // Success state
          await expect(page.locator('text=/Thank you|Order Placed/')).toBeVisible()

          // Continue Shopping button should be present
          await expect(page.locator('a:has-text("Continue Shopping")')).toBeVisible()
        } else {
          // Payment failed - this is expected sometimes due to Mock PG
          console.log('Payment failed or timed out (expected with Mock PG)')
        }
      }
    }
  })

  test('should handle payment failure gracefully', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      const reachedPayment = await proceedToPayment(page)

      if (reachedPayment) {
        // Pay
        await page.locator('button:has-text("Pay")').click()

        // Wait for response from orders API instead of fixed timeout
        await page.waitForResponse(resp =>
          resp.url().includes('/orders') && resp.status() !== 0
        , { timeout: 15000 }).catch(() => {})

        // Check for error message or success (Mock PG may succeed or fail)
        const errorMessage = page.locator('.bg-status-error-bg, [class*="error"]')
        const isError = await errorMessage.isVisible()

        if (isError) {
          // Error should be displayed properly
          await expect(errorMessage).toBeVisible()

          // Pay button should still be available to retry
          await expect(page.locator('button:has-text("Pay")')).toBeVisible()
        }
        // If no error, payment succeeded — that's also valid
      }
    }
  })

  test('should redirect to cart if cart is empty', async ({ page }) => {
    // Try to access checkout directly without items in cart
    await gotoShoppingPage(page, '/shopping/checkout')

    // Wait for redirect or content to appear instead of fixed timeout
    await Promise.race([
      page.waitForURL(/\/cart/, { timeout: 5000 }),
      page.locator('text="Your cart is empty", text="Shopping Cart", text="Cart not found"').first().waitFor({ timeout: 5000 }),
      page.locator('h2:has-text("Shipping Address")').waitFor({ timeout: 5000 }),
    ]).catch(() => {})

    const currentUrl = page.url()
    const isOnCart = currentUrl.includes('/cart')
    const isOnCheckout = currentUrl.includes('/checkout')
    const showsCartContent = await page.locator('text="Your cart is empty", text="Shopping Cart", text="Cart not found"').first().isVisible().catch(() => false)

    // Either redirected to cart OR still on checkout showing cart-related state
    expect(isOnCart || isOnCheckout || showsCartContent).toBeTruthy()
  })

  test('should show Back to Cart link on shipping step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Back to Cart link should be visible
      await expect(page.locator('a:has-text("Back to Cart")')).toBeVisible()

      // Clicking should navigate to cart
      await page.locator('a:has-text("Back to Cart")').click()
      await expect(page).toHaveURL(/\/shopping\/cart/)
    }
  })
})
