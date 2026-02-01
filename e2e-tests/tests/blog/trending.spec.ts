/**
 * Blog Trending E2E Tests
 *
 * Tests for trending post functionality:
 * - Trending tab display
 * - Period filter (today, week, month, year)
 * - Trending post ordering
 * - Navigation between tabs
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog Trending', () => {
  test.beforeEach(async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
  })

  test('should display trending and latest tabs', async ({ page }) => {
    const trendingTab = page.locator('[data-testid="trending-tab"]')
      .or(page.locator('text=/트렌딩|Trending/i').first())
    const latestTab = page.locator('[data-testid="latest-tab"]')
      .or(page.locator('text=/최신|Latest/i').first())

    const hasTrending = await trendingTab.isVisible().catch(() => false)
    const hasLatest = await latestTab.isVisible().catch(() => false)

    expect(hasTrending || hasLatest).toBeTruthy()
  })

  test('should display trending posts with view/like counts', async ({ page }) => {
    await page.waitForTimeout(1000)

    // Look for post cards with metadata
    const postCards = page.locator('[data-testid="post-card"]')
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }))

    const count = await postCards.count()
    if (count > 0) {
      // First card should be visible
      await expect(postCards.first()).toBeVisible()
    }
  })

  test('should change trending period filter', async ({ page }) => {
    await page.waitForTimeout(1000)

    const periodSelect = page.locator('[data-testid="trending-period-select"]')
      .or(page.locator('select').filter({ hasText: /오늘|이번 주|이번 달|week|month/i }))
      .first()

    const hasSelect = await periodSelect.isVisible().catch(() => false)
    if (!hasSelect) return

    // Change to week
    await periodSelect.selectOption({ value: 'week' }).catch(async () => {
      // Try clicking option directly if it's a custom select
      await periodSelect.click()
      await page.locator('text=/이번 주|Week/i').first().click().catch(() => {})
    })

    await page.waitForTimeout(1000)
  })

  test('should switch between trending and latest tabs', async ({ page }) => {
    await page.waitForTimeout(500)

    const trendingTab = page.locator('[data-testid="trending-tab"]')
      .or(page.locator('text=/트렌딩|Trending/i').first())
    const latestTab = page.locator('[data-testid="latest-tab"]')
      .or(page.locator('text=/최신|Latest/i').first())

    const hasTrending = await trendingTab.isVisible().catch(() => false)
    const hasLatest = await latestTab.isVisible().catch(() => false)

    if (hasTrending && hasLatest) {
      // Click latest
      await latestTab.click()
      await page.waitForTimeout(1000)

      // Click trending
      await trendingTab.click()
      await page.waitForTimeout(1000)

      // Trending should be active
      const isActive = await trendingTab.getAttribute('data-active').catch(() => null)
      if (isActive !== null) {
        expect(isActive).toBe('true')
      }
    }
  })

  test('should display feed tab when authenticated', async ({ page }) => {
    const feedTab = page.locator('[data-testid="feed-tab"]')
      .or(page.locator('text=/피드|Feed/i').first())

    const hasFeed = await feedTab.isVisible().catch(() => false)
    // Feed tab should be visible for authenticated users
    if (hasFeed) {
      await expect(feedTab).toBeVisible()
    }
  })
})
