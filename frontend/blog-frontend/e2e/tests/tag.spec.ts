import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for Tag features
 */
test.describe('Tag Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display tag list page', async ({ page }) => {
    await page.goto('/tags')

    // Check page title
    await expect(page).toHaveTitle(/Tags/)

    // Check tag cards are displayed
    const tagCards = page.locator('[data-testid="tag-card"]')
    await expect(tagCards.first()).toBeVisible()

    // Check tag card contains required elements
    await expect(tagCards.first().locator('[data-testid="tag-name"]')).toBeVisible()
    await expect(tagCards.first().locator('[data-testid="tag-post-count"]')).toBeVisible()
  })

  test('should change sort option on tag list', async ({ page }) => {
    await page.goto('/tags')

    // Check sort dropdown exists
    const sortSelect = page.locator('[data-testid="tag-sort-select"]')
    await expect(sortSelect).toBeVisible()

    // Get initial first tag
    const firstTagBefore = await page.locator('[data-testid="tag-card"]').first()
      .locator('[data-testid="tag-name"]').textContent()

    // Change to 'popular' sort
    await sortSelect.selectOption({ value: 'popular' })
    await page.waitForTimeout(500)

    // Check URL parameter changed
    expect(page.url()).toContain('sort=popular')

    // Change to 'name' sort
    await sortSelect.selectOption({ value: 'name' })
    await page.waitForTimeout(500)

    expect(page.url()).toContain('sort=name')

    // First tag might be different now
    const firstTagAfter = await page.locator('[data-testid="tag-card"]').first()
      .locator('[data-testid="tag-name"]').textContent()

    // Just verify tags are still displayed (order may change)
    expect(firstTagAfter).toBeTruthy()
  })

  test('should filter tags by search query', async ({ page }) => {
    await page.goto('/tags')

    // Check search input exists
    const searchInput = page.locator('[data-testid="tag-search-input"]')
    await expect(searchInput).toBeVisible()

    // Get initial count
    const initialCount = await page.locator('[data-testid="tag-card"]').count()

    // Type search query
    await searchInput.fill('test')
    await page.waitForTimeout(500)

    // Get filtered count
    const filteredCount = await page.locator('[data-testid="tag-card"]').count()

    // Should show only matching tags (or show empty state)
    expect(filteredCount).toBeLessThanOrEqual(initialCount)

    // Check URL has search parameter
    expect(page.url()).toContain('search=test')
  })

  test('should navigate to tag detail page', async ({ page }) => {
    await page.goto('/tags')

    // Click on first tag card
    const firstTag = page.locator('[data-testid="tag-card"]').first()
    const tagName = await firstTag.locator('[data-testid="tag-name"]').textContent()

    await firstTag.click()

    // Should navigate to tag detail page
    await expect(page).toHaveURL(/\/tags\/[^/]+/)

    // Check tag detail elements
    await expect(page.locator('[data-testid="tag-detail-name"]')).toBeVisible()
    await expect(page.locator('[data-testid="tag-detail-name"]')).toHaveText(tagName!)
  })

  test('should display posts for selected tag', async ({ page }) => {
    await page.goto('/tags/test-tag')

    // Check posts are displayed
    const posts = page.locator('[data-testid="post-card"]')
    await expect(posts.first()).toBeVisible()

    // Each post should have tag badge
    const tagBadges = posts.first().locator('[data-testid="post-tag"]')
    await expect(tagBadges.first()).toBeVisible()
  })

  test('should show post count for each tag', async ({ page }) => {
    await page.goto('/tags')

    const tagCards = page.locator('[data-testid="tag-card"]')
    const firstCard = tagCards.first()

    const postCount = firstCard.locator('[data-testid="tag-post-count"]')
    await expect(postCount).toBeVisible()

    const countText = await postCount.textContent()
    expect(countText).toMatch(/\d+/)
  })

  test('should handle empty search results', async ({ page }) => {
    await page.goto('/tags')

    const searchInput = page.locator('[data-testid="tag-search-input"]')
    await searchInput.fill('nonexistenttag12345')
    await page.waitForTimeout(500)

    // Should show empty state
    const emptyState = page.locator('[data-testid="empty-tags"]')
    const tagCards = page.locator('[data-testid="tag-card"]')

    const hasEmptyState = await emptyState.isVisible().catch(() => false)
    const tagCount = await tagCards.count()

    expect(hasEmptyState || tagCount === 0).toBeTruthy()
  })

  test('should persist sort and search in URL', async ({ page }) => {
    await page.goto('/tags')

    // Set sort and search
    const sortSelect = page.locator('[data-testid="tag-sort-select"]')
    const searchInput = page.locator('[data-testid="tag-search-input"]')

    await sortSelect.selectOption({ value: 'popular' })
    await searchInput.fill('test')
    await page.waitForTimeout(500)

    const currentUrl = page.url()

    // Reload page
    await page.reload()

    // URL parameters should persist
    expect(page.url()).toBe(currentUrl)

    // Sort and search values should persist
    await expect(sortSelect).toHaveValue('popular')
    await expect(searchInput).toHaveValue('test')
  })

  test('should navigate between tag list and detail pages', async ({ page }) => {
    await page.goto('/tags')

    // Go to tag detail
    const firstTag = page.locator('[data-testid="tag-card"]').first()
    await firstTag.click()

    await expect(page).toHaveURL(/\/tags\/[^/]+/)

    // Go back to tag list
    await page.goBack()

    await expect(page).toHaveURL(/\/tags$/)
    await expect(page.locator('[data-testid="tag-card"]').first()).toBeVisible()
  })

  test('should display tag description on detail page', async ({ page }) => {
    await page.goto('/tags/test-tag')

    const description = page.locator('[data-testid="tag-description"]')

    // Description should be visible (if exists)
    const isVisible = await description.isVisible().catch(() => false)

    if (isVisible) {
      const text = await description.textContent()
      expect(text).toBeTruthy()
    }
  })
})
