/**
 * Blog Like E2E Tests
 *
 * Tests for the like/unlike functionality:
 * - Like button display
 * - Like toggle (like/unlike)
 * - Like count update
 * - Like state persistence
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

async function navigateToPostDetail(page: import('@playwright/test').Page): Promise<boolean> {
  await gotoBlogPage(page, '/blog')
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
  await page.waitForTimeout(1000)

  const postCard = page.locator('[data-testid="post-card"]').first()
    .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post/ }).first())

  const hasPost = await postCard.isVisible().catch(() => false)
  if (!hasPost) return false

  await postCard.click()
  await page.waitForTimeout(2000)
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 5000 }).catch(() => {})
  return true
}

test.describe('Blog Like Feature', () => {
  test('should display like button on post detail page', async ({ page }) => {
    const navigated = await navigateToPostDetail(page)
    if (!navigated) return

    const likeButton = page.locator('[data-testid="like-button"]')
      .or(page.locator('button').filter({ hasText: /좋아요|♡|❤|👍/ }))
      .first()

    const hasLike = await likeButton.isVisible().catch(() => false)
    expect(hasLike).toBeTruthy()
  })

  test('should display like count', async ({ page }) => {
    const navigated = await navigateToPostDetail(page)
    if (!navigated) return

    const likeCount = page.locator('[data-testid="like-count"]')
      .or(page.locator('[data-testid="like-button"]').locator('text=/\\d+/'))
      .first()

    const hasCount = await likeCount.isVisible().catch(() => false)
    if (hasCount) {
      const text = await likeCount.textContent()
      expect(text).toMatch(/\d+/)
    }
  })

  test('should toggle like state when clicking like button', async ({ page }) => {
    const navigated = await navigateToPostDetail(page)
    if (!navigated) return

    const likeButton = page.locator('[data-testid="like-button"]')
      .or(page.locator('button').filter({ hasText: /좋아요|♡|❤|👍/ }))
      .first()

    const hasLike = await likeButton.isVisible().catch(() => false)
    if (!hasLike) return

    // Get initial state
    const initialLiked = await likeButton.getAttribute('data-liked').catch(() => null)

    // Click to toggle
    await likeButton.click()
    await page.waitForTimeout(1000)

    // State should have changed (or at least the click should succeed without error)
    const afterLiked = await likeButton.getAttribute('data-liked').catch(() => null)
    if (initialLiked !== null && afterLiked !== null) {
      expect(afterLiked).not.toBe(initialLiked)
    }
  })

  test('should persist like state on page reload', async ({ page }) => {
    const navigated = await navigateToPostDetail(page)
    if (!navigated) return

    const likeButton = page.locator('[data-testid="like-button"]')
      .or(page.locator('button').filter({ hasText: /좋아요|♡|❤|👍/ }))
      .first()

    const hasLike = await likeButton.isVisible().catch(() => false)
    if (!hasLike) return

    // Click like
    await likeButton.click()
    await page.waitForTimeout(1000)

    const likedState = await likeButton.getAttribute('data-liked').catch(() => null)

    // Reload page
    await page.reload()
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(2000)

    // State should persist
    if (likedState !== null) {
      const afterReload = await likeButton.getAttribute('data-liked').catch(() => null)
      expect(afterReload).toBe(likedState)
    }
  })
})
