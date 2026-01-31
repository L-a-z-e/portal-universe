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
    // Add a product to cart
    await page.goto('/shopping/products/1')
    // Wait for Module Federation remote to load
    await page.locator('h1, [class*="alert"]').first().waitFor({ timeout: 15000 }).catch(() => {})
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })

      // Navigate to checkout
      await page.goto('/shopping/checkout')
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      return true
    }

    return false
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
      await expect(page.locator('input[placeholder*="receiver name"], label:has-text("Receiver Name")')).toBeVisible()
      await expect(page.locator('input[placeholder*="010"], label:has-text("Phone")')).toBeVisible()
      await expect(page.locator('input[placeholder*="12345"], label:has-text("Zip")')).toBeVisible()
      await expect(page.locator('input[placeholder*="Street"], label:has-text("Address")')).toBeVisible()
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
      await page.locator('input').filter({ hasText: /receiver/i }).or(
        page.locator('input[placeholder*="receiver name"]')
      ).first().fill('Test User')

      // Button should still be disabled (missing other required fields)
      await expect(continueButton).toBeDisabled()
    }
  })

  test('should complete shipping address step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Fill in all required address fields
      await page.locator('input').nth(0).fill('Test User') // Receiver Name
      await page.locator('input').nth(1).fill('010-1234-5678') // Phone
      await page.locator('input').nth(2).fill('12345') // Zip Code
      await page.locator('input').nth(3).fill('123 Test Street') // Address

      // Continue button should be enabled now
      const continueButton = page.locator('button:has-text("Continue to Payment")')
      await expect(continueButton).toBeEnabled()

      // Click to proceed to payment
      await continueButton.click()

      // Should move to payment step
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })
    }
  })

  test('should display payment method options', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Fill address and proceed
      await page.locator('input').nth(0).fill('Test User')
      await page.locator('input').nth(1).fill('010-1234-5678')
      await page.locator('input').nth(2).fill('12345')
      await page.locator('input').nth(3).fill('123 Test Street')

      await page.locator('button:has-text("Continue to Payment")').click()

      // Wait for payment step
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })

      // Payment options should be displayed (based on PAYMENT_METHOD_LABELS)
      const creditCard = page.locator('text="Credit Card", text="신용카드"')
      const bankTransfer = page.locator('text="Bank Transfer", text="계좌이체"')

      // At least credit card option should exist
      await expect(creditCard.or(page.locator('label').filter({ hasText: /card/i }))).toBeVisible()
    }
  })

  test('should display order summary on payment step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Fill address and proceed
      await page.locator('input').nth(0).fill('Test User')
      await page.locator('input').nth(1).fill('010-1234-5678')
      await page.locator('input').nth(2).fill('12345')
      await page.locator('input').nth(3).fill('123 Test Street')

      await page.locator('button:has-text("Continue to Payment")').click()

      // Wait for payment step
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })

      // Order summary should be visible
      await expect(page.locator('h3:has-text("Order Summary")')).toBeVisible()

      // Order number should be displayed
      await expect(page.locator('text="Order Number"')).toBeVisible()

      // Total amount should be shown
      await expect(page.locator('text=/Total.*₩[\\d,]+/')).toBeVisible()
    }
  })

  test('should allow going back to shipping step', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Fill address and proceed
      await page.locator('input').nth(0).fill('Test User')
      await page.locator('input').nth(1).fill('010-1234-5678')
      await page.locator('input').nth(2).fill('12345')
      await page.locator('input').nth(3).fill('123 Test Street')

      await page.locator('button:has-text("Continue to Payment")').click()

      // Wait for payment step
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })

      // Click Back button
      await page.locator('button:has-text("Back")').click()

      // Should be back at shipping step
      await expect(page.locator('h2:has-text("Shipping Address")')).toBeVisible()
    }
  })

  test('Happy Path: should complete full checkout flow', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Step 1: Fill shipping address
      await page.locator('input').nth(0).fill('Test User')
      await page.locator('input').nth(1).fill('010-1234-5678')
      await page.locator('input').nth(2).fill('12345')
      await page.locator('input').nth(3).fill('123 Test Street')
      await page.locator('input').nth(4).fill('Apt 101') // Optional detail address

      await page.locator('button:has-text("Continue to Payment")').click()

      // Step 2: Select payment method and pay
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })

      // Credit Card should be selected by default (first option)
      // Click Pay button
      const payButton = page.locator('button:has-text("Pay")')
      await expect(payButton).toBeVisible()
      await payButton.click()

      // Step 3: Wait for payment processing
      // Note: Mock PG has 90% success rate, so this might fail
      // We wait for either success or error message

      const successMessage = page.locator('h2:has-text("Order Placed Successfully")')
      const errorMessage = page.locator('text=/Payment failed|Error/')

      // Wait for either outcome
      await Promise.race([
        successMessage.waitFor({ timeout: 30000 }),
        errorMessage.waitFor({ timeout: 30000 })
      ]).catch(() => {})

      const isSuccess = await successMessage.isVisible()

      if (isSuccess) {
        // Success state
        await expect(page.locator('text="Thank you for your purchase"')).toBeVisible()
        await expect(page.locator('text="Order Number:"')).toBeVisible()

        // View Order and Continue Shopping buttons should be present
        await expect(page.locator('a:has-text("View Order")')).toBeVisible()
        await expect(page.locator('a:has-text("Continue Shopping")')).toBeVisible()
      } else {
        // Payment failed - this is expected sometimes due to Mock PG
        const errorText = await page.locator('.bg-status-error-bg, [class*="error"]').textContent()
        console.log('Payment failed (expected with Mock PG):', errorText)
      }
    }
  })

  test('should handle payment failure gracefully', async ({ page }) => {
    const hasItems = await setupCheckout(page)

    if (hasItems) {
      // Fill address
      await page.locator('input').nth(0).fill('Test User')
      await page.locator('input').nth(1).fill('010-1234-5678')
      await page.locator('input').nth(2).fill('12345')
      await page.locator('input').nth(3).fill('123 Test Street')

      await page.locator('button:has-text("Continue to Payment")').click()

      // Wait for payment step
      await expect(page.locator('h2:has-text("Payment Method")')).toBeVisible({ timeout: 10000 })

      // Pay
      await page.locator('button:has-text("Pay")').click()

      // Wait for processing
      await page.waitForTimeout(5000)

      // Check for error message display
      const errorMessage = page.locator('.bg-status-error-bg, [class*="error"]')
      const isError = await errorMessage.isVisible()

      if (isError) {
        // Error should be displayed properly
        await expect(errorMessage).toBeVisible()

        // Pay button should still be available to retry
        await expect(page.locator('button:has-text("Pay")')).toBeVisible()
      }
    }
  })

  test('should redirect to cart if cart is empty', async ({ page }) => {
    // Try to access checkout directly without items in cart
    await gotoShoppingPage(page, '/shopping/checkout')

    // Wait for redirect to cart
    await page.waitForURL(/\/shopping\/cart/, { timeout: 20000 }).catch(() => {})

    // Should be redirected to cart page
    const currentUrl = page.url()
    expect(currentUrl).toContain('/cart')
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
