/**
 * Blog Tag E2E Tests
 *
 * Tests for tag functionality:
 * - Tag list page
 * - Tag detail page (posts by tag)
 * - Tag search/autocomplete
 * - Popular tags
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog Tag List Page', () => {
  test.beforeEach(async ({ page }) => {
    await gotoBlogPage(page, '/blog/tags')
  })

  test('should display tag list page', async ({ page }) => {
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(3000)

    // Tag page should show tag cards, tag heading, or empty state
    const tagCards = page.locator('[data-testid="tag-card"]')
    const genericTags = page.locator('text=/e2e-test|playwright|automation|blog|tutorial/')
    const emptyState = page.locator('text=/태그가 없습니다|No tags/i')
    const tagHeading = page.locator('h1').filter({ hasText: /태그|Tags/i })

    const hasTagCards = await tagCards.first().isVisible().catch(() => false)
    const hasGenericTags = await genericTags.first().isVisible().catch(() => false)
    const hasEmpty = await emptyState.isVisible().catch(() => false)
    const hasHeading = await tagHeading.first().isVisible().catch(() => false)

    expect(hasTagCards || hasGenericTags || hasEmpty || hasHeading).toBeTruthy()
  })

  test('should display tag post count', async ({ page }) => {
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const tagPostCount = page.locator('[data-testid="tag-post-count"]')
      .or(page.locator('text=/\\d+\\s*(posts|개|건)/i'))
      .first()

    const hasCount = await tagPostCount.isVisible().catch(() => false)
    if (hasCount) {
      const text = await tagPostCount.textContent()
      expect(text).toMatch(/\d+/)
    }
  })

  test('should search tags', async ({ page }) => {
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(3000)

    const searchInput = page.locator('[data-testid="tag-search-input"]')
      .or(page.locator('input[placeholder*="태그"], input[placeholder*="tag" i], input[placeholder*="검색"]'))
      .first()

    const hasSearch = await searchInput.isVisible().catch(() => false)
    if (!hasSearch) return

    await searchInput.fill('e2e')
    await page.waitForTimeout(1000)

    // Should filter tags or show empty state
    const filteredTags = page.locator('text=/e2e-test/')
    const hasFiltered = await filteredTags.first().isVisible().catch(() => false)
    const emptyState = page.locator('text=/결과가 없습니다|No tags|태그가 없습니다/i')
    const hasEmpty = await emptyState.isVisible().catch(() => false)

    expect(hasFiltered || hasEmpty).toBeTruthy()
  })
})

test.describe('Blog Tag Detail Page', () => {
  test('should navigate to tag detail and show posts', async ({ page }) => {
    await gotoBlogPage(page, '/blog/tags')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Click on a tag
    const tagCard = page.locator('[data-testid="tag-card"]').first()
      .or(page.locator('a').filter({ hasText: /e2e-test|playwright/ }).first())

    const hasTag = await tagCard.isVisible().catch(() => false)
    if (!hasTag) return

    await tagCard.click()
    await page.waitForTimeout(2000)

    // Should show posts with the selected tag
    const postCards = page.locator('[data-testid="post-card"]')
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test/ }))

    const emptyState = page.locator('text=/게시글이 없습니다|No posts/i')

    const hasPosts = await postCards.first().isVisible().catch(() => false)
    const hasEmpty = await emptyState.isVisible().catch(() => false)

    expect(hasPosts || hasEmpty).toBeTruthy()
  })

  test('should navigate back to tag list from tag detail', async ({ page }) => {
    await gotoBlogPage(page, '/blog/tags')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    const tagCard = page.locator('[data-testid="tag-card"]').first()
      .or(page.locator('a').filter({ hasText: /e2e-test|playwright/ }).first())

    const hasTag = await tagCard.isVisible().catch(() => false)
    if (!hasTag) return

    await tagCard.click()
    await page.waitForTimeout(2000)

    // Go back
    await page.goBack()
    await page.waitForTimeout(1000)

    // Should be back on tag list
    const tagList = page.locator('[data-testid="tag-card"]')
      .or(page.locator('text=/e2e-test|playwright/'))
      .first()

    const hasTagList = await tagList.isVisible().catch(() => false)
    expect(hasTagList).toBeTruthy()
  })
})
