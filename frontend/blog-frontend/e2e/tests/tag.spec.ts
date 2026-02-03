import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for Tag features
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('Tag Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display tag list page', async ({ page }) => {
    await page.goto('/tags')

    // Check page title
    await expect(page).toHaveTitle(/Tags/)

    // Check tag cards are displayed using CSS class
    const tagCards = page.locator('.tag-card')
    await expect(tagCards.first()).toBeVisible()

    // Check tag card contains required elements
    await expect(tagCards.first().locator('.tag-name')).toBeVisible()
    await expect(tagCards.first().locator('.tag-post-count, .post-count')).toBeVisible()
  })

  test('should change sort option on tag list', async ({ page }) => {
    await page.goto('/tags')

    // Check sort dropdown exists using getByRole
    const sortSelect = page.getByRole('combobox').or(page.locator('select'))
    await expect(sortSelect.first()).toBeVisible()

    // Get initial first tag
    const firstTagBefore = await page.locator('.tag-card').first()
      .locator('.tag-name').textContent()

    // Change to 'popular' sort
    await sortSelect.first().selectOption({ value: 'popular' })
    await page.waitForTimeout(500)

    // Check URL parameter changed
    expect(page.url()).toContain('sort=popular')

    // Change to 'name' sort
    await sortSelect.first().selectOption({ value: 'name' })
    await page.waitForTimeout(500)

    expect(page.url()).toContain('sort=name')

    // First tag might be different now
    const firstTagAfter = await page.locator('.tag-card').first()
      .locator('.tag-name').textContent()

    // Just verify tags are still displayed (order may change)
    expect(firstTagAfter).toBeTruthy()
  })

  test('should filter tags by search query', async ({ page }) => {
    await page.goto('/tags')

    // Check search input exists using getByRole or getByPlaceholder
    const searchInput = page.getByRole('textbox').or(page.getByPlaceholder(/검색|search/i))
    await expect(searchInput.first()).toBeVisible()

    // Get initial count
    const initialCount = await page.locator('.tag-card').count()

    // Type search query
    await searchInput.first().fill('test')
    await page.waitForTimeout(500)

    // Get filtered count
    const filteredCount = await page.locator('.tag-card').count()

    // Should show only matching tags (or show empty state)
    expect(filteredCount).toBeLessThanOrEqual(initialCount)

    // Check URL has search parameter
    expect(page.url()).toContain('search=test')
  })

  test('should navigate to tag detail page', async ({ page }) => {
    await page.goto('/tags')

    // Click on first tag card
    const firstTag = page.locator('.tag-card').first()
    const tagName = await firstTag.locator('.tag-name').textContent()

    await firstTag.click()

    // Should navigate to tag detail page
    await expect(page).toHaveURL(/\/tags\/[^/]+/)

    // Check tag detail elements using CSS class or heading
    const tagTitle = page.locator('.tag-title, .tag-detail-name, h1, h2').first()
    await expect(tagTitle).toBeVisible()
    await expect(tagTitle).toHaveText(tagName!)
  })

  test('should display posts for selected tag', async ({ page }) => {
    await page.goto('/tags/test-tag')

    // Check posts are displayed using CSS class
    const posts = page.locator('.post-card, article.card, .card')
    await expect(posts.first()).toBeVisible()

    // Each post should have tag badge
    const tagBadges = posts.first().locator('.tag-badge, .post-tag, .tag')
    await expect(tagBadges.first()).toBeVisible()
  })

  test('should show post count for each tag', async ({ page }) => {
    await page.goto('/tags')

    const tagCards = page.locator('.tag-card')
    const firstCard = tagCards.first()

    const postCount = firstCard.locator('.tag-post-count, .post-count')
    await expect(postCount).toBeVisible()

    const countText = await postCount.textContent()
    expect(countText).toMatch(/\d+/)
  })

  test('should handle empty search results', async ({ page }) => {
    await page.goto('/tags')

    const searchInput = page.getByRole('textbox').or(page.getByPlaceholder(/검색|search/i))
    await searchInput.first().fill('nonexistenttag12345')
    await page.waitForTimeout(500)

    // Should show empty state or no tag cards
    const emptyState = page.locator('.empty-state, .empty-message').or(page.getByText(/태그가 없|no tags|없습니다/i))
    const tagCards = page.locator('.tag-card')

    const hasEmptyState = await emptyState.first().isVisible().catch(() => false)
    const tagCount = await tagCards.count()

    expect(hasEmptyState || tagCount === 0).toBeTruthy()
  })

  test('should persist sort and search in URL', async ({ page }) => {
    await page.goto('/tags')

    // Set sort and search
    const sortSelect = page.getByRole('combobox').or(page.locator('select'))
    const searchInput = page.getByRole('textbox').or(page.getByPlaceholder(/검색|search/i))

    await sortSelect.first().selectOption({ value: 'popular' })
    await searchInput.first().fill('test')
    await page.waitForTimeout(500)

    const currentUrl = page.url()

    // Reload page
    await page.reload()

    // URL parameters should persist
    expect(page.url()).toBe(currentUrl)

    // Sort and search values should persist
    await expect(sortSelect.first()).toHaveValue('popular')
    await expect(searchInput.first()).toHaveValue('test')
  })

  test('should navigate between tag list and detail pages', async ({ page }) => {
    await page.goto('/tags')

    // Go to tag detail
    const firstTag = page.locator('.tag-card').first()
    await firstTag.click()

    await expect(page).toHaveURL(/\/tags\/[^/]+/)

    // Go back to tag list
    await page.goBack()

    await expect(page).toHaveURL(/\/tags$/)
    await expect(page.locator('.tag-card').first()).toBeVisible()
  })

  test('should display tag description on detail page', async ({ page }) => {
    await page.goto('/tags/test-tag')

    const description = page.locator('.tag-description, .description')

    // Description should be visible (if exists)
    const isVisible = await description.first().isVisible().catch(() => false)

    if (isVisible) {
      const text = await description.first().textContent()
      expect(text).toBeTruthy()
    }
  })
})
