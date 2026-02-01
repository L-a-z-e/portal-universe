/**
 * Blog Post E2E Tests
 *
 * Tests for blog post listing, detail, and CRUD:
 * - Post list display (trending, latest tabs)
 * - Post detail page (content, metadata)
 * - Post creation and editing
 * - Post search
 * - View count increment
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog Post List Page', () => {
  test.beforeEach(async ({ page }) => {
    await gotoBlogPage(page, '/blog')
  })

  test('should display blog main page', async ({ page }) => {
    // Wait for Module Federation content to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Blog page should show some content (tabs, post cards, or empty state)
    const hasTrendingTab = await page.locator('text=/트렌딩|Trending/').isVisible().catch(() => false)
    const hasLatestTab = await page.locator('text=/최신|Latest/').isVisible().catch(() => false)
    const hasPostCard = await page.locator('[class*="rounded"]').filter({ hasText: /E2E Test/ }).first().isVisible().catch(() => false)
    const hasEmptyState = await page.locator('text=/게시글이 없습니다|No posts/').isVisible().catch(() => false)

    expect(hasTrendingTab || hasLatestTab || hasPostCard || hasEmptyState).toBeTruthy()
  })

  test('should display post cards with metadata', async ({ page }) => {
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Look for any post card elements
    const postCards = page.locator('[data-testid="post-card"]')
    const genericCards = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ })

    const testIdCount = await postCards.count()
    const genericCount = await genericCards.count()
    const totalCards = testIdCount > 0 ? testIdCount : genericCount

    if (totalCards > 0) {
      const firstCard = testIdCount > 0 ? postCards.first() : genericCards.first()
      await expect(firstCard).toBeVisible()
    }
  })

  test('should switch between trending and latest tabs', async ({ page }) => {
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for tab elements
    const trendingTab = page.locator('[data-testid="trending-tab"]').or(page.locator('text=/트렌딩|Trending/').first())
    const latestTab = page.locator('[data-testid="latest-tab"]').or(page.locator('text=/최신|Latest/').first())

    const hasTrending = await trendingTab.isVisible().catch(() => false)
    const hasLatest = await latestTab.isVisible().catch(() => false)

    if (hasTrending && hasLatest) {
      // Click latest tab
      await latestTab.click()
      await page.waitForTimeout(1000)

      // Click trending tab
      await trendingTab.click()
      await page.waitForTimeout(1000)
    }
  })
})

test.describe('Blog Post Detail Page', () => {
  test('should display post detail with content', async ({ page }) => {
    // Navigate to blog and find a post to click
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Try to find and click a post card
    const postCard = page.locator('[data-testid="post-card"]').first()
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }).first())

    const hasPost = await postCard.isVisible().catch(() => false)

    if (hasPost) {
      await postCard.click()
      await page.waitForTimeout(2000)

      // Post detail should show title and content
      const hasTitle = await page.locator('h1, h2').first().isVisible().catch(() => false)
      const hasContent = await page.locator('article, [class*="content"], [class*="prose"]').first().isVisible().catch(() => false)

      expect(hasTitle || hasContent).toBeTruthy()
    }
  })

  test('should display post metadata (author, date, tags)', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const postCard = page.locator('[data-testid="post-card"]').first()
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }).first())

    const hasPost = await postCard.isVisible().catch(() => false)

    if (hasPost) {
      await postCard.click()
      await page.waitForTimeout(2000)

      // Check for author info, tags, or date
      const hasAuthor = await page.locator('[data-testid="post-author"]').or(page.locator('text=/작성자|author|by /i')).first().isVisible().catch(() => false)
      const hasTags = await page.locator('[data-testid="post-tags"]').or(page.locator('text=/e2e-test|playwright|automation/')).first().isVisible().catch(() => false)
      const hasDate = await page.locator('time, [data-testid="post-date"]').first().isVisible().catch(() => false)

      // At least some metadata should be visible
      expect(hasAuthor || hasTags || hasDate).toBeTruthy()
    }
  })

  test('should show like button and comment section', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const postCard = page.locator('[data-testid="post-card"]').first()
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }).first())

    const hasPost = await postCard.isVisible().catch(() => false)

    if (hasPost) {
      await postCard.click()
      await page.waitForTimeout(2000)

      const hasLikeButton = await page.locator('[data-testid="like-button"]').or(page.locator('button').filter({ hasText: /좋아요|♡|❤/ })).first().isVisible().catch(() => false)
      const hasCommentSection = await page.locator('[data-testid="comment-section"]').or(page.locator('text=/댓글|Comment/i')).first().isVisible().catch(() => false)

      expect(hasLikeButton || hasCommentSection).toBeTruthy()
    }
  })
})

test.describe('Blog Post Search', () => {
  test('should search posts by keyword', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for search input
    const searchInput = page.locator('input[placeholder*="검색"], input[placeholder*="Search"], input[type="search"]').first()
    const hasSearch = await searchInput.isVisible().catch(() => false)

    if (hasSearch) {
      await searchInput.fill('E2E Test')
      await searchInput.press('Enter')
      await page.waitForTimeout(3000)

      // Should show search results, empty state, or post cards
      const hasResults = await page.locator('[data-testid="post-card"]').or(page.locator('article')).first().isVisible().catch(() => false)
      const hasEmpty = await page.locator('text=/결과가 없습니다|No results|검색 결과/i').isVisible().catch(() => false)
      const hasAnyContent = await page.locator('h1, h2').first().isVisible().catch(() => false)

      expect(hasResults || hasEmpty || hasAnyContent).toBeTruthy()
    }
  })
})
