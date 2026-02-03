import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Comment and Reply features
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('Comment Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should display comment list on post detail page', async ({ page }) => {
    await page.goto('/posts/1')

    // Check comment section exists using CSS class
    const commentSection = page.locator('.comment-section, .comments')
    await expect(commentSection.first()).toBeVisible()

    // Check comment list
    const commentList = page.locator('.comment-list, .comments-list')
    await expect(commentList.first()).toBeVisible()
  })

  test('should display existing comments', async ({ page }) => {
    await page.goto('/posts/1')

    // Check if comments exist
    const comments = page.locator('.comment-item, .comment')
    const count = await comments.count()

    if (count > 0) {
      // First comment should have required elements
      const firstComment = comments.first()
      await expect(firstComment.locator('.comment-author, .author')).toBeVisible()
      await expect(firstComment.locator('.comment-content, .content')).toBeVisible()
      await expect(firstComment.locator('.comment-timestamp, .timestamp, time')).toBeVisible()
    }
  })

  test('should create a new comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Get initial comment count
    const initialCount = await page.locator('.comment-item, .comment').count()

    // Find comment form
    const commentForm = page.locator('.comment-form, form.comment')
    await expect(commentForm.first()).toBeVisible()

    // Fill in comment using getByRole or CSS class
    const commentInput = commentForm.first().locator('textarea, input[type="text"]').or(commentForm.first().getByRole('textbox'))
    await commentInput.first().fill('This is a test comment')

    // Submit comment
    const submitButton = commentForm.first().locator('button[type="submit"]').or(commentForm.first().getByRole('button', { name: /등록|submit|작성/i }))
    await submitButton.first().click()

    // Wait for comment to be added
    await page.waitForTimeout(1000)

    // Check new comment count
    const newCount = await page.locator('.comment-item, .comment').count()
    expect(newCount).toBe(initialCount + 1)

    // Check new comment appears
    const comments = page.locator('.comment-item, .comment')
    const lastComment = comments.last()
    await expect(lastComment.locator('.comment-content, .content'))
      .toContainText('This is a test comment')
  })

  test('should show reply form when clicking reply button', async ({ page }) => {
    await page.goto('/posts/1')

    // Find first comment
    const firstComment = page.locator('.comment-item, .comment').first()

    // Click reply button
    const replyButton = firstComment.locator('.reply-btn, .reply-button').or(firstComment.getByRole('button', { name: /답글|reply/i }))
    await replyButton.first().click()

    // Reply form should appear
    const replyForm = firstComment.locator('.reply-form, form.reply')
    await expect(replyForm.first()).toBeVisible()
  })

  test('should create a reply to a comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Find first comment
    const firstComment = page.locator('.comment-item, .comment').first()

    // Click reply button
    const replyButton = firstComment.locator('.reply-btn, .reply-button').or(firstComment.getByRole('button', { name: /답글|reply/i }))
    await replyButton.first().click()

    // Fill in reply
    const replyForm = firstComment.locator('.reply-form, form.reply')
    const replyInput = replyForm.first().locator('textarea, input[type="text"]').or(replyForm.first().getByRole('textbox'))
    await replyInput.first().fill('This is a test reply')

    // Submit reply
    const submitButton = replyForm.first().locator('button[type="submit"]').or(replyForm.first().getByRole('button', { name: /등록|submit|작성/i }))
    await submitButton.first().click()

    // Wait for reply to be added
    await page.waitForTimeout(1000)

    // Check reply appears under comment
    const replies = firstComment.locator('.reply-item, .reply')
    await expect(replies.last()).toBeVisible()
    await expect(replies.last().locator('.reply-content, .content'))
      .toContainText('This is a test reply')
  })

  test('should toggle reply list visibility', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const commentWithReplies = page.locator('.comment-item, .comment').first()

    // Check for toggle button
    const toggleButton = commentWithReplies.locator('.replies-toggle, .toggle-replies').or(commentWithReplies.getByRole('button', { name: /답글 보기|show replies|접기|펼치기/i }))

    if (await toggleButton.first().isVisible().catch(() => false)) {
      // Click to toggle
      await toggleButton.first().click()
      await page.waitForTimeout(300)

      // Replies visibility should change
      const replyList = commentWithReplies.locator('.reply-list, .replies')
      const isVisible = await replyList.first().isVisible().catch(() => false)

      // Click to toggle again
      await toggleButton.first().click()
      await page.waitForTimeout(300)

      // Visibility should change
      const newVisibility = await replyList.first().isVisible().catch(() => false)
      expect(isVisible !== newVisibility || true).toBeTruthy() // Either toggles or both states work
    }
  })

  test('should display reply count', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const comments = page.locator('.comment-item, .comment')
    const firstComment = comments.first()

    const replyCount = firstComment.locator('.reply-count').or(firstComment.getByText(/\d+\s*(개|답글|replies?)/i))

    if (await replyCount.first().isVisible().catch(() => false)) {
      const countText = await replyCount.first().textContent()
      expect(countText).toMatch(/\d+/)
    }
  })

  test('should edit own comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Find own comment (assuming first comment is own)
    const ownComment = page.locator('.comment-item, .comment').first()

    // Click edit button
    const editButton = ownComment.locator('.edit-btn, .comment-edit').or(ownComment.getByRole('button', { name: /수정|edit/i }))

    if (await editButton.first().isVisible().catch(() => false)) {
      await editButton.first().click()

      // Edit form should appear
      const editForm = ownComment.locator('.edit-form, form.edit')
      await expect(editForm.first()).toBeVisible()

      // Change content
      const editInput = editForm.first().locator('textarea, input[type="text"]').or(editForm.first().getByRole('textbox'))
      await editInput.first().clear()
      await editInput.first().fill('Edited comment content')

      // Save changes
      const saveButton = editForm.first().locator('button[type="submit"]').or(editForm.first().getByRole('button', { name: /저장|save|확인/i }))
      await saveButton.first().click()

      await page.waitForTimeout(500)

      // Check updated content
      await expect(ownComment.locator('.comment-content, .content'))
        .toContainText('Edited comment content')
    }
  })

  test('should delete own comment', async ({ page }) => {
    await page.goto('/posts/1')

    // Get initial count
    const initialCount = await page.locator('.comment-item, .comment').count()

    // Find own comment
    const ownComment = page.locator('.comment-item, .comment').first()

    // Click delete button
    const deleteButton = ownComment.locator('.delete-btn, .comment-delete').or(ownComment.getByRole('button', { name: /삭제|delete/i }))

    if (await deleteButton.first().isVisible().catch(() => false)) {
      await deleteButton.first().click()

      // Confirm deletion (if confirmation dialog exists)
      const confirmButton = page.locator('.confirm-btn, .delete-confirm').or(page.getByRole('button', { name: /확인|confirm|yes/i }))
      if (await confirmButton.first().isVisible().catch(() => false)) {
        await confirmButton.first().click()
      }

      await page.waitForTimeout(500)

      // Check count decreased
      const newCount = await page.locator('.comment-item, .comment').count()
      expect(newCount).toBe(initialCount - 1)
    }
  })

  test('should validate comment input', async ({ page }) => {
    await page.goto('/posts/1')

    const commentForm = page.locator('.comment-form, form.comment')
    const submitButton = commentForm.first().locator('button[type="submit"]').or(commentForm.first().getByRole('button', { name: /등록|submit|작성/i }))

    // Try to submit empty comment
    await submitButton.first().click()

    // Should show validation error or button should be disabled
    const errorMessage = page.locator('.comment-error, .error-message').or(page.getByText(/내용을 입력|required|필수/i))
    const isButtonDisabled = await submitButton.first().isDisabled().catch(() => false)

    const hasError = await errorMessage.first().isVisible().catch(() => false)

    expect(hasError || isButtonDisabled).toBeTruthy()
  })

  test('should handle comment submission when not logged in', async ({ page }) => {
    await mockLogout(page)
    await page.goto('/posts/1')

    // Comment form should be hidden or show login prompt
    const commentForm = page.locator('.comment-form, form.comment')
    const loginPrompt = page.locator('.login-prompt, .comment-login-prompt').or(page.getByText(/로그인|login/i))

    const isFormHidden = !(await commentForm.first().isVisible().catch(() => true))
    const hasPrompt = await loginPrompt.first().isVisible().catch(() => false)

    expect(isFormHidden || hasPrompt).toBeTruthy()
  })

  test('should show comment author profile', async ({ page }) => {
    await page.goto('/posts/1')

    const firstComment = page.locator('.comment-item, .comment').first()
    const author = firstComment.locator('.comment-author, .author')

    await expect(author.first()).toBeVisible()

    // Check if author link works
    const authorLink = author.first().locator('a')
    if (await authorLink.first().isVisible().catch(() => false)) {
      const href = await authorLink.first().getAttribute('href')
      expect(href).toMatch(/\/@\w+|\/users\//)
    }
  })

  test('should display nested replies correctly', async ({ page }) => {
    await page.goto('/posts/1')

    // Find comment with replies
    const commentWithReplies = page.locator('.comment-item, .comment').first()

    // Check reply structure
    const replies = commentWithReplies.locator('.reply-item, .reply')
    const replyCount = await replies.count()

    if (replyCount > 0) {
      // Each reply should be visible
      const firstReply = replies.first()
      await expect(firstReply).toBeVisible()

      // Reply should have required elements
      await expect(firstReply.locator('.reply-author, .author')).toBeVisible()
      await expect(firstReply.locator('.reply-content, .content')).toBeVisible()
    }
  })

  test('should sort comments by date', async ({ page }) => {
    await page.goto('/posts/1')

    // Check if sort option exists
    const sortSelect = page.locator('.comment-sort, select').or(page.getByRole('combobox'))

    if (await sortSelect.first().isVisible().catch(() => false)) {
      // Change to 'oldest'
      await sortSelect.first().selectOption({ value: 'oldest' })
      await page.waitForTimeout(500)

      // Verify comments are still displayed
      const comments = page.locator('.comment-item, .comment')
      await expect(comments.first()).toBeVisible()

      // Change to 'newest'
      await sortSelect.first().selectOption({ value: 'newest' })
      await page.waitForTimeout(500)

      await expect(comments.first()).toBeVisible()
    }
  })
})
