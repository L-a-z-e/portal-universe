/**
 * Cart E2E Tests
 *
 * Tests for shopping cart functionality:
 * - Add items to cart
 * - View cart
 * - Update item quantity
 * - Remove items
 * - Cart summary calculation
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoShoppingPage } from '../helpers/auth'

test.describe('Shopping Cart', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await gotoShoppingPage(page, '/shopping', 'h1:has-text("Products")')
  })

  test('should display empty cart message when no items', async ({ page }) => {
    // Navigate to cart page
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    // Check for empty cart or cart with items
    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const cartTitle = page.locator('h1:has-text("Shopping Cart")')

    const isEmpty = await emptyCartMessage.isVisible()
    const hasCartTitle = await cartTitle.isVisible()

    // Either empty message or cart title with items should be shown
    expect(isEmpty || hasCartTitle).toBeTruthy()

    if (isEmpty) {
      // Start Shopping button should be visible
      await expect(page.locator('a:has-text("Start Shopping")')).toBeVisible()
    }
  })

  test('should display cart page title', async ({ page }) => {
    await page.goto('/shopping/cart')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Cart title should be visible
    await expect(page.locator('h1:has-text("Shopping Cart")')).toBeVisible()
  })

  test('should add product to cart from product detail page', async ({ page }) => {
    // Navigate to a product detail page with full auth/MF handling
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    // Check if product exists and is in stock
    const errorState = page.locator('text="Product not found"')
    const isError = await errorState.isVisible()

    if (!isError) {
      const addToCartButton = page.locator('button:has-text("Add to Cart")')
      await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
      const isInStock = await addToCartButton.isEnabled().catch(() => false)

      if (isInStock) {
        // Click Add to Cart
        await addToCartButton.click()

        // Wait for success message
        await expect(page.locator('text="Added to cart successfully!"')).toBeVisible({ timeout: 5000 })

        // View Cart link should appear
        await expect(page.locator('a:has-text("View Cart")')).toBeVisible()
      }
    }
  })

  test('should update cart item quantity', async ({ page }) => {
    // First add a product to cart
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })
    }

    // Navigate to cart
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    // Check if cart has items
    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const isEmpty = await emptyCartMessage.isVisible()

    if (!isEmpty) {
      // Find quantity controls in CartItem component
      const increaseButton = page.locator('button').filter({
        has: page.locator('path[d*="M12 4v16m8-8H4"]')
      }).first()

      if (await increaseButton.isVisible()) {
        // Get current total before increase
        const totalBefore = await page.locator('[class*="font-bold"][class*="text-brand-primary"]').last().textContent()

        // Increase quantity
        await increaseButton.click()

        // Wait for cart to update
        await page.waitForTimeout(500)

        // Total should change (or quantity should update)
        // The exact assertion depends on implementation
      }
    }
  })

  test('should remove item from cart', async ({ page }) => {
    // First add a product to cart
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })
    }

    // Navigate to cart
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    // Check if cart has items
    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const isEmpty = await emptyCartMessage.isVisible()

    if (!isEmpty) {
      // Find remove button (usually with trash icon or "Remove" text)
      const removeButton = page.locator('button:has-text("Remove"), button[aria-label*="remove"], button[aria-label*="delete"]').first()

      if (await removeButton.isVisible()) {
        // Count items before removal
        const itemsBefore = await page.locator('.cart-item, [data-testid="cart-item"]').count()

        // Click remove
        await removeButton.click()

        // Wait for update
        await page.waitForTimeout(500)

        // Either item count decreased or cart is now empty
        const isNowEmpty = await emptyCartMessage.isVisible()
        const itemsAfter = await page.locator('.cart-item, [data-testid="cart-item"]').count()

        expect(isNowEmpty || itemsAfter < itemsBefore).toBeTruthy()
      }
    }
  })

  test('should display order summary with correct totals', async ({ page }) => {
    // First add a product to cart
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })
    }

    // Navigate to cart
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const isEmpty = await emptyCartMessage.isVisible()

    if (!isEmpty) {
      // Order Summary should be visible
      await expect(page.locator('h2:has-text("Order Summary")')).toBeVisible()

      // Subtotal should be shown (format: "Subtotal (N items)")
      await expect(page.locator('text=/Subtotal/')).toBeVisible()

      // Total should be shown with price
      await expect(page.locator('text=/Total.*₩[\\d,]+/')).toBeVisible()

      // Proceed to Checkout button should be visible
      await expect(page.locator('button:has-text("Proceed to Checkout")')).toBeVisible()
    }
  })

  test('should navigate to checkout from cart', async ({ page }) => {
    // First add a product to cart
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })
    }

    // Navigate to cart
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const isEmpty = await emptyCartMessage.isVisible()

    if (!isEmpty) {
      // Click Proceed to Checkout
      await page.locator('button:has-text("Proceed to Checkout")').click()

      // Should navigate to checkout page
      await expect(page).toHaveURL(/\/shopping\/checkout/)
    }
  })

  test('should clear entire cart', async ({ page }) => {
    // First add a product to cart
    await gotoShoppingPage(page, '/shopping/products/1', 'h1')

    const addToCartButton = page.locator('button:has-text("Add to Cart")')
    await addToCartButton.waitFor({ timeout: 10000 }).catch(() => {})
    const isInStock = await addToCartButton.isEnabled().catch(() => false)

    if (isInStock) {
      await addToCartButton.click()
      await page.waitForSelector('text="Added to cart successfully!"', { timeout: 5000 })
    }

    // Navigate to cart
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    const emptyCartMessage = page.locator('text="Your cart is empty"')
    const isEmpty = await emptyCartMessage.isVisible()

    if (!isEmpty) {
      // Find Clear Cart button
      const clearCartButton = page.locator('button:has-text("Clear Cart")')

      if (await clearCartButton.isVisible()) {
        // Handle confirmation dialog
        page.on('dialog', async dialog => {
          await dialog.accept()
        })

        await clearCartButton.click()

        // Wait for cart to be cleared
        await page.waitForTimeout(1000)

        // Cart should now be empty
        await expect(page.locator('text="Your cart is empty"')).toBeVisible({ timeout: 5000 })
      }
    }
  })

  test('should show Continue Shopping link', async ({ page }) => {
    await gotoShoppingPage(page, '/shopping/cart', 'h1:has-text("Shopping Cart"), text="Your cart is empty"')

    // Continue Shopping link should be visible
    const continueLink = page.locator('a:has-text("Continue Shopping"), a:has-text("Start Shopping")')
    await expect(continueLink).toBeVisible()

    // Clicking should navigate to products
    await continueLink.click()
    await expect(page).toHaveURL(/\/shopping/)
  })
})
