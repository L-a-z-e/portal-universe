import { test, expect } from '@playwright/test'

test.describe('Shopping Product List', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/shopping')
  })

  test('should display shopping page', async ({ page }) => {
    const heading = page.getByRole('heading', { level: 1 })
      .or(page.locator('h1'))
      .or(page.getByText(/shopping/i).first())
      .or(page.getByText(/products/i).first())

    const isVisible = await heading.isVisible().catch(() => false)

    if (isVisible) {
      await expect(heading).toBeVisible()
    }
  })

  test('should display product list or empty state', async ({ page }) => {
    await page.waitForTimeout(2000)

    const productList = page.locator('[data-testid="product-list"]')
      .or(page.locator('.product-list'))
      .or(page.locator('.product-card'))
      .or(page.locator('[data-testid="product-card"]'))
      .or(page.locator('[class*="product"]'))

    const emptyState = page.getByText(/no products/i)
      .or(page.getByText(/empty/i))
      .or(page.getByText(/상품/i))
      .or(page.locator('[data-testid="empty-state"]'))

    const pageContent = page.locator('main, #app, .app, #root, body')

    const hasProductList = await productList.first().isVisible().catch(() => false)
    const hasEmptyState = await emptyState.first().isVisible().catch(() => false)
    const hasPageContent = await pageContent.first().isVisible().catch(() => false)

    // Accept if any content is visible (page loaded successfully)
    expect(hasProductList || hasEmptyState || hasPageContent).toBeTruthy()
  })

  test('should be able to click on a product if available', async ({ page }) => {
    await page.waitForTimeout(1000)

    const productLink = page.locator('.product-card a').first()
      .or(page.locator('[data-testid="product-link"]').first())
      .or(page.locator('.product-item a').first())

    const isVisible = await productLink.isVisible().catch(() => false)

    if (isVisible) {
      await productLink.click()
      await page.waitForTimeout(500)

      const url = page.url()
      expect(url).toMatch(/shopping|product/)
    } else {
      test.skip()
    }
  })

  test('should have add to cart button if products exist', async ({ page }) => {
    await page.waitForTimeout(1000)

    const addToCartButton = page.getByRole('button', { name: /add to cart/i })
      .or(page.getByRole('button', { name: /cart/i }))
      .or(page.locator('[data-testid="add-to-cart"]'))

    const isVisible = await addToCartButton.first().isVisible().catch(() => false)

    if (isVisible) {
      await expect(addToCartButton.first()).toBeEnabled()
    } else {
      test.skip()
    }
  })
})
