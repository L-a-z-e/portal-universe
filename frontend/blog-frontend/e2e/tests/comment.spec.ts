import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Comment and Reply features
 */
test.describe('Comment Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display comment list on post detail page', async ({ page }) => {
    await page.goto('/posts/1')

    // Check comment section exists
    const commentSection = page.locator('[data-testid="comment-section"]')
    await expect(commentSection).toBeVisible()

    // Check comment list
    const commentList = page.locator('[data-testid="comment-list"]')
    await expect(commentList).toBeVisible()
  })

  test('should display existing comments', async ({ page }) => {
    await page.goto('/posts/1')

    // Check if comments exist
    const comments = page.locator('[data-testid="comment-item"]')
    const count = await comments.count()

    if (count > 0) {
      // First comment should have required elements
      const firstComment = comments.first()
      await expect(firstComment.locator('[data-testid="comment-author"]')).toBeVisible()
      await expect(firstComment.locator('[data-testid="comment-content"]')).toBeVisible()
      await expect(firstComment.locator('[data-testid="comment-timestamp"]')).toBeVisible()
    }
  })

  test('should create a new comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Get initial comment count
    const initialCount = await page.locator('[data-testid="comment-item"]').count()

    // Find comment form
    const commentForm = page.locator('[data-testid="comment-form"]')
    await expect(commentForm).toBeVisible()

    // Fill in comment
    const commentInput = commentForm.locator('[data-testid="comment-input"]')
    await commentInput.fill('This is a test comment')

    // Submit comment
    const submitButton = commentForm.locator('[data-testid="comment-submit-btn"]')
    await submitButton.click()

    // Wait for comment to be added
    await page.waitForTimeout(1000)

    // Check new comment count
    const newCount = await page.locator('[data-testid="comment-item"]').count()
    expect(newCount).toBe(initialCount + 1)

    // Check new comment appears
    const comments = page.locator('[data-testid="comment-item"]')
    const lastComment = comments.last()
    await expect(lastComment.locator('[data-testid="comment-content"]'))
      .toContainText('This is a test comment')
  })

  test('should show reply form when clicking reply button', async ({ page }) => {
    await page.goto('/posts/1')

    // Find first comment
    const firstComment = page.locator('[data-testid="comment-item"]').first()

    // Click reply button
    const replyButton = firstComment.locator('[data-testid="comment-reply-btn"]')
    await replyButton.click()

    // Reply form should appear
    const replyForm = firstComment.locator('[data-testid="reply-form"]')
    await expect(replyForm).toBeVisible()
  })

  test('should create a reply to a comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Find first comment
    const firstComment = page.locator('[data-testid="comment-item"]').first()

    // Click reply button
    const replyButton = firstComment.locator('[data-testid="comment-reply-btn"]')
    await replyButton.click()

    // Fill in reply
    const replyForm = firstComment.locator('[data-testid="reply-form"]')
    const replyInput = replyForm.locator('[data-testid="reply-input"]')
    await replyInput.fill('This is a test reply')

    // Submit reply
    const submitButton = replyForm.locator('[data-testid="reply-submit-btn"]')
    await submitButton.click()

    // Wait for reply to be added
    await page.waitForTimeout(1000)

    // Check reply appears under comment
    const replies = firstComment.locator('[data-testid="reply-item"]')
    await expect(replies.last()).toBeVisible()
    await expect(replies.last().locator('[data-testid="reply-content"]'))
      .toContainText('This is a test reply')
  })

  test('should toggle reply list visibility', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const commentWithReplies = page.locator('[data-testid="comment-item"]').first()

    // Check for toggle button
    const toggleButton = commentWithReplies.locator('[data-testid="replies-toggle-btn"]')

    if (await toggleButton.isVisible()) {
      // Click to collapse
      await toggleButton.click()
      await page.waitForTimeout(300)

      // Replies should be hidden
      const replyList = commentWithReplies.locator('[data-testid="reply-list"]')
      await expect(replyList).not.toBeVisible()

      // Click to expand
      await toggleButton.click()
      await page.waitForTimeout(300)

      // Replies should be visible
      await expect(replyList).toBeVisible()
    }
  })

  test('should display reply count', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const comments = page.locator('[data-testid="comment-item"]')
    const firstComment = comments.first()

    const replyCount = firstComment.locator('[data-testid="reply-count"]')

    if (await replyCount.isVisible()) {
      const countText = await replyCount.textContent()
      expect(countText).toMatch(/\d+/)
    }
  })

  test('should edit own comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Find own comment (assuming first comment is own)
    const ownComment = page.locator('[data-testid="comment-item"]').first()

    // Click edit button
    const editButton = ownComment.locator('[data-testid="comment-edit-btn"]')

    if (await editButton.isVisible()) {
      await editButton.click()

      // Edit form should appear
      const editForm = ownComment.locator('[data-testid="comment-edit-form"]')
      await expect(editForm).toBeVisible()

      // Change content
      const editInput = editForm.locator('[data-testid="comment-edit-input"]')
      await editInput.clear()
      await editInput.fill('Edited comment content')

      // Save changes
      const saveButton = editForm.locator('[data-testid="comment-save-btn"]')
      await saveButton.click()

      await page.waitForTimeout(500)

      // Check updated content
      await expect(ownComment.locator('[data-testid="comment-content"]'))
        .toContainText('Edited comment content')
    }
  })

  test('should delete own comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Get initial count
    const initialCount = await page.locator('[data-testid="comment-item"]').count()

    // Find own comment
    const ownComment = page.locator('[data-testid="comment-item"]').first()

    // Click delete button
    const deleteButton = ownComment.locator('[data-testid="comment-delete-btn"]')

    if (await deleteButton.isVisible()) {
      await deleteButton.click()

      // Confirm deletion (if confirmation dialog exists)
      const confirmButton = page.locator('[data-testid="delete-confirm-btn"]')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
      }

      await page.waitForTimeout(500)

      // Check count decreased
      const newCount = await page.locator('[data-testid="comment-item"]').count()
      expect(newCount).toBe(initialCount - 1)
    }
  })

  test('should validate comment input', async ({ page }) => {
    await page.goto('/posts/1')

    const commentForm = page.locator('[data-testid="comment-form"]')
    const submitButton = commentForm.locator('[data-testid="comment-submit-btn"]')

    // Try to submit empty comment
    await submitButton.click()

    // Should show validation error or button should be disabled
    const errorMessage = page.locator('[data-testid="comment-error"]')
    const isButtonDisabled = await submitButton.isDisabled()

    const hasError = await errorMessage.isVisible().catch(() => false)

    expect(hasError || isButtonDisabled).toBeTruthy()
  })

  test('should handle comment submission when not logged in', async ({ page }) => {
    await mockLogout(page)
    await page.goto('/posts/1')

    // Comment form should be hidden or show login prompt
    const commentForm = page.locator('[data-testid="comment-form"]')
    const loginPrompt = page.locator('[data-testid="comment-login-prompt"]')

    const isFormHidden = !(await commentForm.isVisible().catch(() => true))
    const hasPrompt = await loginPrompt.isVisible().catch(() => false)

    expect(isFormHidden || hasPrompt).toBeTruthy()
  })

  test('should show comment author profile', async ({ page }) => {
    await page.goto('/posts/1')

    const firstComment = page.locator('[data-testid="comment-item"]').first()
    const author = firstComment.locator('[data-testid="comment-author"]')

    await expect(author).toBeVisible()

    // Check if author link works
    const authorLink = author.locator('a')
    if (await authorLink.isVisible()) {
      await expect(authorLink).toHaveAttribute('href', /\/users\/.+/)
    }
  })

  test('should display nested replies correctly', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const commentWithReplies = page.locator('[data-testid="comment-item"]').first()

    // Check reply structure
    const replies = commentWithReplies.locator('[data-testid="reply-item"]')
    const replyCount = await replies.count()

    if (replyCount > 0) {
      // Each reply should be indented or styled differently
      const firstReply = replies.first()
      await expect(firstReply).toBeVisible()

      // Reply should have required elements
      await expect(firstReply.locator('[data-testid="reply-author"]')).toBeVisible()
      await expect(firstReply.locator('[data-testid="reply-content"]')).toBeVisible()
    }
  })

  test('should sort comments by date', async ({ page }) => {
    await page.goto('/posts/1')

    // Check if sort option exists
    const sortSelect = page.locator('[data-testid="comment-sort-select"]')

    if (await sortSelect.isVisible()) {
      // Change to 'oldest'
      await sortSelect.selectOption({ value: 'oldest' })
      await page.waitForTimeout(500)

      // Verify comments are still displayed
      const comments = page.locator('[data-testid="comment-item"]')
      await expect(comments.first()).toBeVisible()

      // Change to 'newest'
      await sortSelect.selectOption({ value: 'newest' })
      await page.waitForTimeout(500)

      await expect(comments.first()).toBeVisible()
    }
  })
})
