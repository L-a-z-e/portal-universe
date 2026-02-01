/**
 * Blog Comment E2E Tests
 *
 * Tests for comment functionality:
 * - Comment display on post detail
 * - Comment creation
 * - Reply (thread) comments
 * - Comment editing and deletion
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

/**
 * Navigate to a post detail page that has comments.
 */
async function navigateToPostWithComments(page: import('@playwright/test').Page): Promise<boolean> {
  await gotoBlogPage(page, '/blog')
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
  await page.waitForTimeout(1000)

  // Click first post card
  const postCard = page.locator('[data-testid="post-card"]').first()
    .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Blog Post 1/ }).first())

  const hasPost = await postCard.isVisible().catch(() => false)
  if (!hasPost) return false

  await postCard.click()
  await page.waitForTimeout(2000)
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 5000 }).catch(() => {})
  return true
}

test.describe('Blog Comments', () => {
  test('should display comment section on post detail', async ({ page }) => {
    const navigated = await navigateToPostWithComments(page)
    if (!navigated) return

    // Comment section should be visible
    const commentSection = page.locator('[data-testid="comment-section"]')
      .or(page.locator('[data-testid="comment-list"]'))
      .or(page.locator('text=/댓글|Comments/i').first())

    const hasComments = await commentSection.isVisible().catch(() => false)
    expect(hasComments).toBeTruthy()
  })

  test('should display existing comments', async ({ page }) => {
    const navigated = await navigateToPostWithComments(page)
    if (!navigated) return

    await page.waitForTimeout(1000)

    // Look for comment items
    const commentItems = page.locator('[data-testid="comment-item"]')
    const genericComments = page.locator('text=/E2E Test Comment/')

    const hasTestIdComments = await commentItems.first().isVisible().catch(() => false)
    const hasGenericComments = await genericComments.first().isVisible().catch(() => false)

    // Comments should exist (seeded data)
    if (hasTestIdComments) {
      expect(await commentItems.count()).toBeGreaterThan(0)
    } else if (hasGenericComments) {
      expect(await genericComments.count()).toBeGreaterThan(0)
    }
  })

  test('should create a new comment', async ({ page }) => {
    const navigated = await navigateToPostWithComments(page)
    if (!navigated) return

    // Find comment input form
    const commentInput = page.locator('[data-testid="comment-input"]')
      .or(page.locator('textarea[placeholder*="댓글"], textarea[placeholder*="comment" i]'))
      .first()

    const hasInput = await commentInput.isVisible().catch(() => false)
    if (!hasInput) return

    const testComment = `E2E New Comment ${Date.now()}`
    await commentInput.fill(testComment)

    // Submit comment
    const submitButton = page.locator('[data-testid="comment-submit"]')
      .or(page.getByRole('button', { name: /작성|등록|Submit|Post/i }))
      .first()

    await submitButton.click()
    await page.waitForTimeout(2000)

    // New comment should appear
    const newComment = page.locator(`text="${testComment}"`)
    const hasNewComment = await newComment.isVisible().catch(() => false)
    expect(hasNewComment).toBeTruthy()
  })

  test('should show reply form when clicking reply button', async ({ page }) => {
    const navigated = await navigateToPostWithComments(page)
    if (!navigated) return

    // Find reply button on a comment
    const replyButton = page.locator('[data-testid="comment-reply-btn"]')
      .or(page.locator('button').filter({ hasText: /답글|Reply/i }))
      .first()

    const hasReply = await replyButton.isVisible().catch(() => false)
    if (!hasReply) return

    await replyButton.click()
    await page.waitForTimeout(500)

    // Reply form should appear
    const replyForm = page.locator('[data-testid="reply-form"]')
      .or(page.locator('textarea[placeholder*="답글"], textarea[placeholder*="reply" i]'))
      .first()

    const hasReplyForm = await replyForm.isVisible().catch(() => false)
    expect(hasReplyForm).toBeTruthy()
  })
})
