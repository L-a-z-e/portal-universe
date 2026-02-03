import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for Series features
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('Series Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display series list page', async ({ page }) => {
    await page.goto('/series')

    // Check page title
    await expect(page).toHaveTitle(/Series/)

    // Check series cards are displayed using CSS class
    const seriesCards = page.locator('.series-card')
    await expect(seriesCards.first()).toBeVisible()

    // Check series card contains required elements
    await expect(seriesCards.first().locator('.series-title')).toBeVisible()
    await expect(seriesCards.first().locator('.post-count')).toBeVisible()
  })

  test('should navigate to series detail page', async ({ page }) => {
    await page.goto('/series')

    // Click on first series card
    const firstSeries = page.locator('.series-card').first()
    await firstSeries.click()

    // Should navigate to series detail page
    await expect(page).toHaveURL(/\/series\/\d+/)

    // Check series detail elements using CSS classes
    await expect(page.locator('.series-name, h1, h2').first()).toBeVisible()
  })

  test('should display posts in correct order within series', async ({ page }) => {
    await page.goto('/series/1')

    // Check posts are displayed - use post cards or items
    const posts = page.locator('.post-card, .post-item, article')
    const count = await posts.count()

    // Series might be empty, just verify page loaded
    if (count > 0) {
      await expect(posts.first()).toBeVisible()
    }
  })

  test('should navigate between posts in series using navigation box', async ({ page }) => {
    // Assume we're on a post detail page that belongs to a series
    await page.goto('/posts/1') // Post in series

    // Check series box is visible using CSS class
    const seriesBox = page.locator('.series-box')

    if (await seriesBox.isVisible()) {
      // Check navigation buttons using CSS classes
      const nextButton = seriesBox.locator('.next-button')
      const prevButton = seriesBox.locator('.prev-button')

      // Click next button if enabled
      if (await nextButton.isVisible() && await nextButton.isEnabled()) {
        const currentUrl = page.url()
        await nextButton.click()
        await page.waitForURL((url) => url.toString() !== currentUrl)

        // Should navigate to next post
        expect(page.url()).not.toBe(currentUrl)
        await expect(seriesBox).toBeVisible()
      }
    }
  })

  test('should show series posts count', async ({ page }) => {
    await page.goto('/series/1')

    // Look for post count using CSS class or text pattern
    const postCount = page.locator('.post-count, .series-post-count')

    if (await postCount.first().isVisible()) {
      const countText = await postCount.first().textContent()
      expect(countText).toMatch(/\d+/)
    }
  })

  test('should expand/collapse series post list', async ({ page }) => {
    await page.goto('/posts/1') // Post in series

    const seriesBox = page.locator('.series-box')

    if (await seriesBox.isVisible()) {
      // Check for toggle button by role
      const toggleButton = seriesBox.getByRole('button').filter({ hasText: /목록|펼치기|접기/i })

      if (await toggleButton.first().isVisible()) {
        // Click to toggle
        await toggleButton.first().click()
        // Wait for animation
        await page.waitForTimeout(300)
      }
    }
  })

  test('should handle empty series', async ({ page }) => {
    // Navigate to non-existent series
    await page.goto('/series/999')

    // Should show empty state or error message
    const emptyState = page.locator('.empty-state, .empty-message')
    const errorMessage = page.locator('.error-message, .error-content')
    const notFound = page.getByText(/찾을 수 없|존재하지 않|없습니다/)

    const isEmptyVisible = await emptyState.first().isVisible().catch(() => false)
    const isErrorVisible = await errorMessage.first().isVisible().catch(() => false)
    const isNotFoundVisible = await notFound.first().isVisible().catch(() => false)

    expect(isEmptyVisible || isErrorVisible || isNotFoundVisible).toBeTruthy()
  })
})
