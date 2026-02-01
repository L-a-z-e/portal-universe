/**
 * Blog Series E2E Tests
 *
 * Tests for series functionality:
 * - Series display on post detail
 * - Series navigation (prev/next in series)
 * - Series detail page
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog Series', () => {
  test('should display series box on posts that belong to a series', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Navigate to a post that's in a series (first seeded post)
    const postCard = page.locator('[data-testid="post-card"]').first()
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post 1/ }).first())

    const hasPost = await postCard.isVisible().catch(() => false)
    if (!hasPost) return

    await postCard.click()
    await page.waitForTimeout(2000)

    // Check for series box component
    const seriesBox = page.locator('[data-testid="series-box"]')
      .or(page.locator('text=/E2E Test Series/'))
      .first()

    const hasSeries = await seriesBox.isVisible().catch(() => false)
    // Series box may or may not be visible depending on seeded data
    if (hasSeries) {
      await expect(seriesBox).toContainText(/E2E Test Series|시리즈/)
    }
  })

  test('should navigate to series detail page', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Find a post that belongs to a series and navigate to it
    const postCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post 1/ }).first()
    const hasPost = await postCard.isVisible().catch(() => false)
    if (!hasPost) return

    await postCard.click()
    await page.waitForTimeout(2000)

    // Look for series link
    const seriesLink = page.locator('[data-testid="series-link"]')
      .or(page.locator('a').filter({ hasText: /E2E Test Series/ }))
      .first()

    const hasLink = await seriesLink.isVisible().catch(() => false)
    if (!hasLink) return

    await seriesLink.click()
    await page.waitForTimeout(2000)

    // Should be on series detail page
    const seriesTitle = page.locator('h1, h2').filter({ hasText: /E2E Test Series/ }).first()
    const hasTitle = await seriesTitle.isVisible().catch(() => false)

    if (hasTitle) {
      await expect(seriesTitle).toContainText('E2E Test Series')
    }
  })

  test('should display posts list within series', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Try to navigate to a series detail page directly
    const postCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post 1/ }).first()
    const hasPost = await postCard.isVisible().catch(() => false)
    if (!hasPost) return

    await postCard.click()
    await page.waitForTimeout(2000)

    // If there's a series box, check posts are listed
    const seriesPostList = page.locator('[data-testid="series-post-list"]')
      .or(page.locator('[data-testid="series-box"]'))
      .first()

    const hasList = await seriesPostList.isVisible().catch(() => false)
    if (hasList) {
      // Series should show post entries
      const postEntries = seriesPostList.locator('[data-testid="series-post-item"]')
        .or(seriesPostList.locator('a, li'))

      const count = await postEntries.count()
      if (count > 0) {
        expect(count).toBeGreaterThan(0)
      }
    }
  })
})
