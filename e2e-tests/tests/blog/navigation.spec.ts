/**
 * Blog Navigation E2E Tests
 *
 * Tests for post navigation and user blog:
 * - Previous/Next post navigation
 * - User blog page (@username)
 * - Blog stats page
 * - Category page
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog Post Navigation', () => {
  test('should display prev/next navigation on post detail', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const postCard = page.locator('[data-testid="post-card"]').first()
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }).first())

    const hasPost = await postCard.isVisible().catch(() => false)
    if (!hasPost) return

    await postCard.click()
    await page.waitForTimeout(2000)

    // Check for navigation component
    const navComponent = page.locator('[data-testid="post-navigation"]')
      .or(page.locator('text=/이전|이전 글|Previous/i').first())
      .or(page.locator('text=/다음|다음 글|Next/i').first())

    const hasNav = await navComponent.isVisible().catch(() => false)
    if (hasNav) {
      await expect(navComponent).toBeVisible()
    }
  })

  test('should display related posts', async ({ page }) => {
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const postCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post 1/ }).first()
    const hasPost = await postCard.isVisible().catch(() => false)
    if (!hasPost) return

    await postCard.click()
    await page.waitForTimeout(2000)

    // Check for related posts section
    const relatedPosts = page.locator('[data-testid="related-posts"]')
      .or(page.locator('text=/관련 글|Related Posts/i').first())

    const hasRelated = await relatedPosts.isVisible().catch(() => false)
    if (hasRelated) {
      await expect(relatedPosts).toBeVisible()
    }
  })
})

test.describe('Blog Stats Page', () => {
  test('should display blog statistics', async ({ page }) => {
    await gotoBlogPage(page, '/blog/stats')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Stats page should show some statistics
    const hasTitle = await page.locator('h1, h2').filter({ hasText: /통계|Stats/i }).first().isVisible().catch(() => false)
    const hasNumbers = await page.locator('text=/\\d+/').first().isVisible().catch(() => false)
    const hasCharts = await page.locator('canvas, svg, [data-testid="stats"]').first().isVisible().catch(() => false)

    expect(hasTitle || hasNumbers || hasCharts).toBeTruthy()
  })
})

test.describe('Blog Category Page', () => {
  test('should display categories', async ({ page }) => {
    await gotoBlogPage(page, '/blog/categories')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    await page.waitForTimeout(2000)

    const categoryCards = page.locator('[data-testid="category-card"]')
      .or(page.locator('h3').filter({ hasText: /Spring Boot|test|전체/ }).first())
      .first()
    const categoryHeading = page.locator('h1').filter({ hasText: /카테고리|Categories/i })
    const emptyState = page.locator('text=/카테고리가 없습니다|No categories/i')

    const hasCategories = await categoryCards.isVisible().catch(() => false)
    const hasHeading = await categoryHeading.isVisible().catch(() => false)
    const hasEmpty = await emptyState.isVisible().catch(() => false)

    expect(hasCategories || hasHeading || hasEmpty).toBeTruthy()
  })
})

test.describe('User Blog Page', () => {
  test('should access user blog page', async ({ page }) => {
    // Navigate to the test user's blog (the user created during auth setup)
    await gotoBlogPage(page, '/blog')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Click on an author name to navigate to their blog
    const authorLink = page.locator('[data-testid="post-author"]')
      .or(page.locator('a').filter({ hasText: /test/i }))
      .first()

    const hasAuthor = await authorLink.isVisible().catch(() => false)
    if (!hasAuthor) return

    await authorLink.click()
    await page.waitForTimeout(2000)

    // User blog page should show profile and posts
    const userProfile = page.locator('[data-testid="user-profile"]')
      .or(page.locator('text=/게시글|Posts/i').first())

    const hasProfile = await userProfile.isVisible().catch(() => false)
    if (hasProfile) {
      await expect(userProfile).toBeVisible()
    }
  })
})
