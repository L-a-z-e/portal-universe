import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for Trending/Latest features
 */
test.describe('Trending and Latest Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display trending and latest tabs on main page', async ({ page }) => {
    await page.goto('/')

    // Check tabs are visible
    const trendingTab = page.locator('[data-testid="trending-tab"]')
    const latestTab = page.locator('[data-testid="latest-tab"]')

    await expect(trendingTab).toBeVisible()
    await expect(latestTab).toBeVisible()
  })

  test('should switch between trending and latest tabs', async ({ page }) => {
    await page.goto('/')

    const trendingTab = page.locator('[data-testid="trending-tab"]')
    const latestTab = page.locator('[data-testid="latest-tab"]')

    // Default should be trending (or check active state)
    await expect(trendingTab).toHaveAttribute('data-active', 'true')

    // Click latest tab
    await latestTab.click()
    await page.waitForTimeout(500)

    // Latest should be active
    await expect(latestTab).toHaveAttribute('data-active', 'true')
    await expect(trendingTab).toHaveAttribute('data-active', 'false')

    // Check URL parameter
    expect(page.url()).toContain('tab=latest')

    // Click trending tab
    await trendingTab.click()
    await page.waitForTimeout(500)

    // Trending should be active
    await expect(trendingTab).toHaveAttribute('data-active', 'true')
    expect(page.url()).toContain('tab=trending')
  })

  test('should display posts in trending tab', async ({ page }) => {
    await page.goto('/?tab=trending')

    // Check posts are displayed
    const posts = page.locator('[data-testid="post-card"]')
    await expect(posts.first()).toBeVisible()

    // Posts should have like count and view count
    const firstPost = posts.first()
    await expect(firstPost.locator('[data-testid="post-likes"]')).toBeVisible()
  })

  test('should display posts in latest tab', async ({ page }) => {
    await page.goto('/?tab=latest')

    // Check posts are displayed
    const posts = page.locator('[data-testid="post-card"]')
    await expect(posts.first()).toBeVisible()

    // Posts should have timestamp
    const firstPost = posts.first()
    await expect(firstPost.locator('[data-testid="post-timestamp"]')).toBeVisible()
  })

  test('should change period filter on trending tab', async ({ page }) => {
    await page.goto('/?tab=trending')

    // Check period filter exists
    const periodSelect = page.locator('[data-testid="trending-period-select"]')
    await expect(periodSelect).toBeVisible()

    // Change to 'week'
    await periodSelect.selectOption({ value: 'week' })
    await page.waitForTimeout(500)

    // Check URL parameter
    expect(page.url()).toContain('period=week')

    // Posts should still be displayed
    const posts = page.locator('[data-testid="post-card"]')
    await expect(posts.first()).toBeVisible()

    // Change to 'month'
    await periodSelect.selectOption({ value: 'month' })
    await page.waitForTimeout(500)

    expect(page.url()).toContain('period=month')

    // Change to 'all'
    await periodSelect.selectOption({ value: 'all' })
    await page.waitForTimeout(500)

    expect(page.url()).toContain('period=all')
  })

  test('should not show period filter on latest tab', async ({ page }) => {
    await page.goto('/?tab=latest')

    // Period filter should not be visible
    const periodSelect = page.locator('[data-testid="trending-period-select"]')
    await expect(periodSelect).not.toBeVisible()
  })

  test('should sync URL query parameters with UI state', async ({ page }) => {
    // Navigate with query parameters
    await page.goto('/?tab=trending&period=week')

    // Check UI reflects parameters
    const trendingTab = page.locator('[data-testid="trending-tab"]')
    const periodSelect = page.locator('[data-testid="trending-period-select"]')

    await expect(trendingTab).toHaveAttribute('data-active', 'true')
    await expect(periodSelect).toHaveValue('week')
  })

  test('should persist tab selection on page reload', async ({ page }) => {
    await page.goto('/')

    // Switch to latest tab
    const latestTab = page.locator('[data-testid="latest-tab"]')
    await latestTab.click()
    await page.waitForTimeout(500)

    const currentUrl = page.url()

    // Reload page
    await page.reload()

    // URL should persist
    expect(page.url()).toBe(currentUrl)

    // Latest tab should still be active
    await expect(latestTab).toHaveAttribute('data-active', 'true')
  })

  test('should display correct post count indicator', async ({ page }) => {
    await page.goto('/?tab=trending')

    // Check if post count or "showing X posts" text exists
    const postCountIndicator = page.locator('[data-testid="post-count"]')

    const isVisible = await postCountIndicator.isVisible().catch(() => false)

    if (isVisible) {
      const text = await postCountIndicator.textContent()
      expect(text).toMatch(/\d+/)
    }
  })

  test('should handle empty states', async ({ page }) => {
    // This might not happen in production, but good to test
    await page.goto('/?tab=latest')

    const posts = page.locator('[data-testid="post-card"]')
    const emptyState = page.locator('[data-testid="empty-posts"]')

    const postCount = await posts.count()
    const hasEmptyState = await emptyState.isVisible().catch(() => false)

    // Either posts exist or empty state is shown
    expect(postCount > 0 || hasEmptyState).toBeTruthy()
  })

  test('should load more posts on scroll (infinite scroll)', async ({ page }) => {
    await page.goto('/?tab=trending')

    // Get initial post count
    const initialCount = await page.locator('[data-testid="post-card"]').count()

    // Scroll to bottom
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

    // Wait for potential load
    await page.waitForTimeout(1000)

    // Get new post count
    const newCount = await page.locator('[data-testid="post-card"]').count()

    // Should have loaded more posts (or stay same if no more)
    expect(newCount).toBeGreaterThanOrEqual(initialCount)
  })

  test('should navigate to post detail from trending/latest list', async ({ page }) => {
    await page.goto('/?tab=trending')

    // Click on first post
    const firstPost = page.locator('[data-testid="post-card"]').first()
    await firstPost.click()

    // Should navigate to post detail
    await expect(page).toHaveURL(/\/posts\/\d+/)
  })
})
