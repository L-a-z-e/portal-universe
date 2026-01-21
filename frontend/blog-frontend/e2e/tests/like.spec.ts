import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Like features
 */
test.describe('Like Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display like button on post detail page', async ({ page }) => {
    await page.goto('/posts/1')

    // Check like button is visible
    const likeButton = page.locator('[data-testid="like-button"]')
    await expect(likeButton).toBeVisible()

    // Check like count is displayed
    const likeCount = likeButton.locator('[data-testid="like-count"]')
    await expect(likeCount).toBeVisible()
  })

  test('should increase like count when clicking like button', async ({ page }) => {
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')
    const likeCount = likeButton.locator('[data-testid="like-count"]')

    // Get initial count
    const initialCount = parseInt(await likeCount.textContent() || '0')

    // Click like button
    await likeButton.click()

    // Wait for update
    await page.waitForTimeout(500)

    // Check count increased
    const newCount = parseInt(await likeCount.textContent() || '0')
    expect(newCount).toBe(initialCount + 1)

    // Check button state changed (liked)
    await expect(likeButton).toHaveAttribute('data-liked', 'true')
  })

  test('should decrease like count when clicking liked button', async ({ page }) => {
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')
    const likeCount = likeButton.locator('[data-testid="like-count"]')

    // First like
    await likeButton.click()
    await page.waitForTimeout(500)

    const likedCount = parseInt(await likeCount.textContent() || '0')

    // Click again to unlike
    await likeButton.click()
    await page.waitForTimeout(500)

    // Check count decreased
    const finalCount = parseInt(await likeCount.textContent() || '0')
    expect(finalCount).toBe(likedCount - 1)

    // Check button state changed (unliked)
    await expect(likeButton).toHaveAttribute('data-liked', 'false')
  })

  test('should show visual feedback on like button hover', async ({ page }) => {
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')

    // Hover over button
    await likeButton.hover()

    // Check for hover styles (this depends on your implementation)
    await page.waitForTimeout(100)
    // Visual check - button should be visible and interactive
    await expect(likeButton).toBeVisible()
  })

  test('should handle like button when not logged in', async ({ page }) => {
    await mockLogout(page)
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')

    // Click like button
    await likeButton.click()

    // Should show login prompt or redirect to login
    // This depends on your implementation
    const loginModal = page.locator('[data-testid="login-modal"]')
    const loginPrompt = page.locator('[data-testid="login-prompt"]')

    const isModalVisible = await loginModal.isVisible().catch(() => false)
    const isPromptVisible = await loginPrompt.isVisible().catch(() => false)
    const isRedirected = page.url().includes('/login')

    expect(isModalVisible || isPromptVisible || isRedirected).toBeTruthy()
  })

  test('should display like count correctly', async ({ page }) => {
    await page.goto('/posts/1')

    const likeCount = page.locator('[data-testid="like-count"]')
    const countText = await likeCount.textContent()

    // Should be a number
    expect(countText).toMatch(/^\d+$/)
  })

  test('should persist like state on page reload', async ({ page }) => {
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')

    // Like the post
    await likeButton.click()
    await page.waitForTimeout(500)

    // Reload page
    await page.reload()

    // Check like state persisted
    await expect(likeButton).toHaveAttribute('data-liked', 'true')
  })

  test('should handle rapid clicking (debounce)', async ({ page }) => {
    await page.goto('/posts/1')

    const likeButton = page.locator('[data-testid="like-button"]')
    const likeCount = likeButton.locator('[data-testid="like-count"]')

    const initialCount = parseInt(await likeCount.textContent() || '0')

    // Rapidly click multiple times
    await likeButton.click()
    await likeButton.click()
    await likeButton.click()

    await page.waitForTimeout(1000)

    // Count should only change by 1 (or odd number if toggle)
    const finalCount = parseInt(await likeCount.textContent() || '0')
    const diff = Math.abs(finalCount - initialCount)

    // Should be 1 if properly debounced
    expect(diff).toBeLessThanOrEqual(1)
  })
})
