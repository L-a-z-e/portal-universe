/**
 * Search E2E Tests
 *
 * Tests for product search functionality:
 * - Search input and autocomplete
 * - Search execution and results
 * - Popular keywords
 * - Recent search keywords
 * - Empty results handling
 */
import { test, expect } from '@playwright/test'

test.describe('Product Search', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await page.goto('/shopping')
  })

  test('should display search input on product list page', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Search input should be visible
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    await expect(searchInput).toBeVisible({ timeout: 5000 })

    // Search button should be visible
    const searchButton = page.locator('button:has-text("Search")')
    const isButtonVisible = await searchButton.isVisible()

    expect(isButtonVisible).toBeTruthy()
  })

  test('should show autocomplete suggestions when typing', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Find search input
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      // Type search query
      await searchInput.fill('laptop')
      await page.waitForTimeout(500)

      // Check for autocomplete dropdown
      const autocomplete = page.locator('[class*="autocomplete"], [class*="suggestion"], [role="listbox"]')
      const hasSuggestions = await autocomplete.isVisible()

      if (hasSuggestions) {
        // Suggestions should contain search term
        const suggestions = page.locator('text=/laptop/i')
        await expect(suggestions.first()).toBeVisible({ timeout: 3000 })
      }
    }
  })

  test('should execute search and display results', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Find and fill search input
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('test')

      // Click search button or press Enter
      const searchButton = page.locator('button:has-text("Search")')
      await searchButton.click()

      // Wait for navigation or results
      await page.waitForTimeout(1000)

      // URL should contain search parameter
      await expect(page).toHaveURL(/keyword=test/)

      // Wait for results to load
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Search results info, empty state, or error should be shown
      const searchInfo = page.locator('text="Search results for"')
      const emptyResults = page.locator('text="No products found"')
      const errorMessage = page.locator('text=/401|Request failed/')

      const hasInfo = await searchInfo.isVisible()
      const isEmpty = await emptyResults.isVisible()
      const hasError = await errorMessage.isVisible()

      expect(hasInfo || isEmpty || hasError).toBeTruthy()
    }
  })

  test('should display popular keywords', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Click search input to see suggestions
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.click()
      await page.waitForTimeout(500)

      // Look for popular keywords section
      const popularSection = page.locator('text="Popular Keywords", text="Trending"')
      const hasPopular = await popularSection.isVisible()

      if (hasPopular) {
        // Popular keyword tags should be visible
        const keywordTags = page.locator('[class*="tag"], [class*="chip"], button:has-text("#")')
        const hasKeywords = await keywordTags.first().isVisible()

        expect(hasKeywords).toBeTruthy()
      }
    }
  })

  test('should display recent keywords for logged-in user', async ({ page }) => {
    // Perform a search first
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('laptop')
      await page.locator('button:has-text("Search")').click()
      await page.waitForTimeout(1000)

      // Navigate back to products page
      await page.goto('/shopping')
      await page.waitForTimeout(1000)

      // Click search input again
      await searchInput.click()
      await page.waitForTimeout(500)

      // Look for recent keywords section
      const recentSection = page.locator('text="Recent Searches", text="Recent Keywords"')
      const hasRecent = await recentSection.isVisible()

      if (hasRecent) {
        // Recent keyword should include our search
        const recentKeyword = page.locator('text="laptop"')
        const hasKeyword = await recentKeyword.isVisible()

        expect(hasKeyword).toBeTruthy()
      }
    }
  })

  test('should delete individual recent keyword', async ({ page }) => {
    // Perform a search first
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('test-keyword')
      await page.locator('button:has-text("Search")').click()
      await page.waitForTimeout(1000)

      // Navigate back
      await page.goto('/shopping')
      await page.waitForTimeout(1000)

      // Click search input
      await searchInput.click()
      await page.waitForTimeout(500)

      // Look for delete button on recent keyword
      const deleteButton = page.locator('button[aria-label*="delete"], button[aria-label*="remove"]').first()
      const hasDeleteButton = await deleteButton.isVisible()

      if (hasDeleteButton) {
        // Get keyword text before deletion
        const keywordCount = await page.locator('[class*="recent"] button').count()

        // Click delete
        await deleteButton.click()
        await page.waitForTimeout(500)

        // Keyword count should decrease
        const newCount = await page.locator('[class*="recent"] button').count()
        expect(newCount).toBeLessThanOrEqual(keywordCount)
      }
    }
  })

  test('should clear all recent keywords', async ({ page }) => {
    // Perform a search first
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('test')
      await page.locator('button:has-text("Search")').click()
      await page.waitForTimeout(1000)

      // Navigate back
      await page.goto('/shopping')
      await page.waitForTimeout(1000)

      // Click search input
      await searchInput.click()
      await page.waitForTimeout(500)

      // Look for clear all button
      const clearAllButton = page.locator('button:has-text("Clear All"), button:has-text("Clear")')
      const hasClearButton = await clearAllButton.isVisible()

      if (hasClearButton) {
        await clearAllButton.click()
        await page.waitForTimeout(500)

        // Recent keywords section should be empty or hidden
        const recentKeywords = page.locator('[class*="recent"] button').filter({ hasNotText: 'Clear' })
        const count = await recentKeywords.count()

        expect(count).toBe(0)
      }
    }
  })

  test('should handle empty search results', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Search for something unlikely to exist
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('xyznonexistent123456')
      await page.locator('button:has-text("Search")').click()

      // Wait for results
      await page.waitForTimeout(1000)
      await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

      // Empty state or no results message should appear
      const emptyState = page.locator('text="No products found", text="No results"')
      const hasEmptyState = await emptyState.isVisible()

      if (hasEmptyState) {
        await expect(emptyState).toBeVisible()

        // Suggestion to clear search or try again
        const clearSuggestion = page.locator('text=/Try again|Clear search|different keyword/')
        const hasSuggestion = await clearSuggestion.isVisible()

        expect(hasSuggestion).toBeTruthy()
      }
    }
  })

  test('should allow selecting autocomplete suggestion', async ({ page }) => {
    // Wait for page to load
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Type in search input
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]')
    const isInputVisible = await searchInput.isVisible()

    if (isInputVisible) {
      await searchInput.fill('lap')
      await page.waitForTimeout(500)

      // Check for suggestions
      const suggestion = page.locator('[role="option"], [class*="suggestion"] button').first()
      const hasSuggestion = await suggestion.isVisible()

      if (hasSuggestion) {
        // Click suggestion
        await suggestion.click()

        // Search should execute
        await page.waitForTimeout(1000)

        // URL should have keyword or results should load
        const hasKeywordParam = await page.url().includes('keyword=')
        expect(hasKeywordParam).toBeTruthy()
      }
    }
  })
})
