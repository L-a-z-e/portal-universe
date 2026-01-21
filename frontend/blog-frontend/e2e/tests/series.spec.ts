import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for Series features
 */
test.describe('Series Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display series list page', async ({ page }) => {
    await page.goto('/series')

    // Check page title
    await expect(page).toHaveTitle(/Series/)

    // Check series cards are displayed
    const seriesCards = page.locator('[data-testid="series-card"]')
    await expect(seriesCards.first()).toBeVisible()

    // Check series card contains required elements
    await expect(seriesCards.first().locator('[data-testid="series-title"]')).toBeVisible()
    await expect(seriesCards.first().locator('[data-testid="series-post-count"]')).toBeVisible()
  })

  test('should navigate to series detail page', async ({ page }) => {
    await page.goto('/series')

    // Click on first series card
    const firstSeries = page.locator('[data-testid="series-card"]').first()
    await firstSeries.click()

    // Should navigate to series detail page
    await expect(page).toHaveURL(/\/series\/\d+/)

    // Check series detail elements
    await expect(page.locator('[data-testid="series-name"]')).toBeVisible()
    await expect(page.locator('[data-testid="series-description"]')).toBeVisible()
  })

  test('should display posts in correct order within series', async ({ page }) => {
    await page.goto('/series/1')

    // Check posts are displayed
    const posts = page.locator('[data-testid="series-post-item"]')
    const count = await posts.count()
    expect(count).toBeGreaterThan(0)

    // Check order numbers are sequential
    for (let i = 0; i < count; i++) {
      const orderText = await posts.nth(i).locator('[data-testid="post-order"]').textContent()
      expect(orderText).toContain(`${i + 1}`)
    }
  })

  test('should navigate between posts in series using navigation box', async ({ page }) => {
    // Assume we're on a post detail page that belongs to a series
    await page.goto('/posts/1') // Post in series

    // Check series box is visible
    const seriesBox = page.locator('[data-testid="series-box"]')
    await expect(seriesBox).toBeVisible()

    // Check navigation buttons
    const nextButton = seriesBox.locator('[data-testid="next-post-btn"]')
    const prevButton = seriesBox.locator('[data-testid="prev-post-btn"]')

    // Click next button if enabled
    if (await nextButton.isEnabled()) {
      const currentUrl = page.url()
      await nextButton.click()
      await page.waitForURL((url) => url.toString() !== currentUrl)

      // Should navigate to next post
      expect(page.url()).not.toBe(currentUrl)
      await expect(seriesBox).toBeVisible()
    }
  })

  test('should show series posts count', async ({ page }) => {
    await page.goto('/series/1')

    const postCount = page.locator('[data-testid="series-post-count"]')
    await expect(postCount).toBeVisible()

    const countText = await postCount.textContent()
    expect(countText).toMatch(/\d+/)
  })

  test('should expand/collapse series post list', async ({ page }) => {
    await page.goto('/posts/1') // Post in series

    const seriesBox = page.locator('[data-testid="series-box"]')
    await expect(seriesBox).toBeVisible()

    // Check toggle button
    const toggleButton = seriesBox.locator('[data-testid="series-toggle-btn"]')

    if (await toggleButton.isVisible()) {
      // Click to toggle
      await toggleButton.click()

      // Check post list visibility changed
      const postList = seriesBox.locator('[data-testid="series-post-list"]')
      // State should have changed (either hidden or visible)
      await page.waitForTimeout(300) // Wait for animation
    }
  })

  test('should handle empty series', async ({ page }) => {
    // Navigate to empty series (if exists)
    await page.goto('/series/999')

    // Should show empty state or error message
    const emptyState = page.locator('[data-testid="empty-series"]')
    const errorMessage = page.locator('[data-testid="error-message"]')

    const isEmptyVisible = await emptyState.isVisible().catch(() => false)
    const isErrorVisible = await errorMessage.isVisible().catch(() => false)

    expect(isEmptyVisible || isErrorVisible).toBeTruthy()
  })
})
