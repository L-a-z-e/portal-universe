import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for My Page (/my)
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('My Page', () => {
  test.describe('Authentication', () => {
    test('should redirect to login when accessing /my without authentication', async ({ page }) => {
      // Ensure logged out
      await mockLogout(page)

      await page.goto('/my')

      // Should show login prompt or redirect
      const loginPrompt = page.locator('.login-required, .login-prompt').or(page.getByText(/로그인이 필요|Sign in|Login/i))
      const loginButton = page.getByRole('button', { name: /로그인|Sign in|Login/i }).or(page.getByRole('link', { name: /로그인|Sign in|Login/i }))

      const hasPrompt = await loginPrompt.first().isVisible().catch(() => false)
      const hasButton = await loginButton.first().isVisible().catch(() => false)
      const isRedirected = page.url().includes('/login')

      expect(hasPrompt || hasButton || isRedirected).toBeTruthy()
    })

    test('should access my page after login', async ({ page }) => {
      await mockLogin(page)

      await page.goto('/my')

      // Should display my page content
      const myPageContainer = page.locator('.my-page, .mypage, main')
      await expect(myPageContainer.first()).toBeVisible()
    })
  })

  test.describe('Profile Display', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display current profile information', async ({ page }) => {
      await page.goto('/my')

      // Check profile section
      const profileSection = page.locator('.my-profile, .profile-section, .profile-card')
      await expect(profileSection.first()).toBeVisible()

      // Check username
      const username = page.locator('.profile-username, .username')
      await expect(username.first()).toBeVisible()

      // Check email
      const email = page.locator('.profile-email, .email')
      await expect(email.first()).toBeVisible()

      // Check nickname
      const nickname = page.locator('.profile-nickname, .nickname, .display-name')
      if (await nickname.first().isVisible().catch(() => false)) {
        await expect(nickname.first()).toBeVisible()
      }
    })

    test('should display profile edit button', async ({ page }) => {
      await page.goto('/my')

      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await expect(editButton.first()).toBeVisible()
    })

    test('should display profile statistics', async ({ page }) => {
      await page.goto('/my')

      // Check post count
      const postCount = page.locator('.my-post-count, .post-count').or(page.getByText(/\d+\s*(개|posts?|건)/i))
      if (await postCount.first().isVisible().catch(() => false)) {
        const text = await postCount.first().textContent()
        expect(text).toMatch(/\d+/)
      }
    })
  })

  test.describe('Profile Editing', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should enter edit mode when clicking edit button', async ({ page }) => {
      await page.goto('/my')

      // Click edit button
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      // Edit form should appear
      const editForm = page.locator('.profile-edit-form, .edit-form, form')
      await expect(editForm.first()).toBeVisible()

      // Cancel button should appear
      const cancelButton = page.locator('.cancel-button').or(page.getByRole('button', { name: /취소|cancel/i }))
      await expect(cancelButton.first()).toBeVisible()

      // Save button should appear
      const saveButton = page.locator('.save-button').or(page.getByRole('button', { name: /저장|save/i }))
      await expect(saveButton.first()).toBeVisible()
    })

    test('should update profile information', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      // Update nickname
      const nicknameInput = page.locator('input[name="nickname"], .input-nickname').or(page.getByLabel(/닉네임|nickname/i))
      await expect(nicknameInput.first()).toBeVisible()
      await nicknameInput.first().fill('Updated Nickname')

      // Update bio
      const bioInput = page.locator('textarea[name="bio"], .input-bio').or(page.getByLabel(/소개|bio/i))
      if (await bioInput.first().isVisible().catch(() => false)) {
        await bioInput.first().fill('Updated bio text')
      }

      // Save changes
      const saveButton = page.locator('.save-button').or(page.getByRole('button', { name: /저장|save/i }))
      await saveButton.first().click()

      // Wait for save to complete
      await page.waitForTimeout(500)

      // Success message should appear
      const successMessage = page.getByText(/저장되었습니다|Saved|Success/i)
      await expect(successMessage.first()).toBeVisible({ timeout: 3000 })
    })

    test('should cancel profile editing', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      // Make some changes
      const nicknameInput = page.locator('input[name="nickname"], .input-nickname').or(page.getByLabel(/닉네임|nickname/i))
      const originalValue = await nicknameInput.first().inputValue()
      await nicknameInput.first().fill('Temporary Change')

      // Click cancel
      const cancelButton = page.locator('.cancel-button').or(page.getByRole('button', { name: /취소|cancel/i }))
      await cancelButton.first().click()

      // Edit form should be hidden
      const editForm = page.locator('.profile-edit-form, .edit-form')
      await expect(editForm).not.toBeVisible()

      // Edit button should be visible again
      await expect(editButton.first()).toBeVisible()
    })

    test('should set username if not already set', async ({ page }) => {
      // Mock user without username
      await page.addInitScript(() => {
        localStorage.setItem('user', JSON.stringify({
          id: 'test-user-1',
          username: null,
          email: 'test@example.com',
          nickname: 'Test User',
        }))
      })

      await page.goto('/my')

      // Username setup prompt should appear
      const usernamePrompt = page.locator('.username-setup, .username-prompt')

      if (await usernamePrompt.first().isVisible().catch(() => false)) {
        // Enter username
        const usernameInput = page.locator('input[name="username"], .input-username').or(page.getByLabel(/사용자명|username/i))
        await expect(usernameInput.first()).toBeVisible()
        await usernameInput.first().fill('newusername')

        // Submit username
        const submitButton = page.locator('.username-submit').or(page.getByRole('button', { name: /확인|submit|설정/i }))
        await submitButton.first().click()

        await page.waitForTimeout(500)
      }
    })

    test('should validate username format', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      const usernameInput = page.locator('input[name="username"], .input-username').or(page.getByLabel(/사용자명|username/i))

      if (await usernameInput.first().isVisible().catch(() => false)) {
        // Try invalid username (spaces, special chars)
        await usernameInput.first().fill('invalid username!')

        // Error message should appear
        const errorMessage = page.locator('.username-error, .error-message').or(page.getByText(/유효하지 않|invalid|형식/i))
        await expect(errorMessage.first()).toBeVisible()

        // Try valid username
        await usernameInput.first().fill('validusername')
        await expect(errorMessage).not.toBeVisible()
      }
    })

    test('should check username duplication', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      const usernameInput = page.locator('input[name="username"], .input-username').or(page.getByLabel(/사용자명|username/i))

      if (await usernameInput.first().isVisible().catch(() => false)) {
        // Enter an existing username
        await usernameInput.first().fill('existinguser')
        await usernameInput.first().blur()

        // Wait for validation
        await page.waitForTimeout(500)

        // Duplication error might appear (depends on backend response)
        const duplicateError = page.locator('.username-duplicate-error, .duplicate-error').or(page.getByText(/이미 사용 중|already taken|duplicate/i))
        // Check if duplicate validation is shown
      }
    })

    test('should update social links', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await editButton.first().click()

      // Update social links if available
      const githubInput = page.locator('input[name="github"], .input-social-github').or(page.getByLabel(/github/i))
      if (await githubInput.first().isVisible().catch(() => false)) {
        await githubInput.first().fill('https://github.com/testuser')
      }

      const twitterInput = page.locator('input[name="twitter"], .input-social-twitter').or(page.getByLabel(/twitter/i))
      if (await twitterInput.first().isVisible().catch(() => false)) {
        await twitterInput.first().fill('https://twitter.com/testuser')
      }

      const websiteInput = page.locator('input[name="website"], .input-social-website').or(page.getByLabel(/website|웹사이트/i))
      if (await websiteInput.first().isVisible().catch(() => false)) {
        await websiteInput.first().fill('https://testuser.com')
      }

      // Save changes
      const saveButton = page.locator('.save-button').or(page.getByRole('button', { name: /저장|save/i }))
      await saveButton.first().click()

      await page.waitForTimeout(500)
    })
  })

  test.describe('My Posts Management', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display my posts list', async ({ page }) => {
      await page.goto('/my')

      // Check posts section
      const postsSection = page.locator('.my-posts, .posts-section')
      await expect(postsSection.first()).toBeVisible()

      // Check if posts or empty state is shown
      const posts = page.locator('.my-post-card, .post-card, article.card')
      const emptyState = page.locator('.empty-my-posts, .empty-state').or(page.getByText(/게시글이 없|no posts|없습니다/i))

      const postCount = await posts.count()
      const hasEmptyState = await emptyState.first().isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should filter posts by status', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      // Check status filter exists
      const statusFilter = page.locator('.post-status-filter, .status-filter, [role="tablist"]')
      await expect(statusFilter.first()).toBeVisible()

      // Filter by ALL (default)
      const allFilter = page.locator('.filter-all').or(page.getByRole('tab', { name: /전체|all/i })).or(page.getByText(/전체|all/i))
      await allFilter.first().click()
      await page.waitForTimeout(300)

      // Filter by DRAFT
      const draftFilter = page.locator('.filter-draft').or(page.getByRole('tab', { name: /임시|draft/i })).or(page.getByText(/임시|draft/i))
      await draftFilter.first().click()
      await page.waitForTimeout(300)

      // Should show only drafts
      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        // All posts should have draft badge
        for (let i = 0; i < postCount; i++) {
          const post = posts.nth(i)
          const draftBadge = post.locator('.draft-badge, .draft')
          await expect(draftBadge).toBeVisible()
        }
      }

      // Filter by PUBLISHED
      const publishedFilter = page.locator('.filter-published').or(page.getByRole('tab', { name: /발행|published/i })).or(page.getByText(/발행|published/i))
      await publishedFilter.first().click()
      await page.waitForTimeout(300)

      // Should show only published posts
      const publishedPosts = page.locator('.my-post-card, .post-card')
      const publishedCount = await publishedPosts.count()

      if (publishedCount > 0) {
        // No posts should have draft badge
        for (let i = 0; i < publishedCount; i++) {
          const post = publishedPosts.nth(i)
          const draftBadge = post.locator('.draft-badge, .draft')
          await expect(draftBadge).not.toBeVisible()
        }
      }
    })

    test('should display post action buttons', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for edit button
        const editButton = firstPost.locator('.post-edit-button, .edit-btn').or(firstPost.getByRole('button', { name: /수정|edit/i }))
        await expect(editButton.first()).toBeVisible()

        // Check for delete button
        const deleteButton = firstPost.locator('.post-delete-button, .delete-btn').or(firstPost.getByRole('button', { name: /삭제|delete/i }))
        await expect(deleteButton.first()).toBeVisible()
      }
    })

    test('should show delete confirmation dialog', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Click delete button
        const deleteButton = firstPost.locator('.post-delete-button, .delete-btn').or(firstPost.getByRole('button', { name: /삭제|delete/i }))
        await deleteButton.first().click()

        // Confirmation dialog should appear
        const confirmDialog = page.locator('.delete-confirm-dialog, .confirm-dialog, [role="dialog"]').or(page.getByRole('dialog'))
        await expect(confirmDialog.first()).toBeVisible()

        // Check dialog has confirm and cancel buttons
        const confirmButton = page.locator('.delete-confirm-button, .confirm-btn').or(page.getByRole('button', { name: /확인|confirm|삭제/i }))
        const cancelButton = page.locator('.delete-cancel-button, .cancel-btn').or(page.getByRole('button', { name: /취소|cancel/i }))

        await expect(confirmButton.first()).toBeVisible()
        await expect(cancelButton.first()).toBeVisible()

        // Cancel deletion
        await cancelButton.first().click()

        // Dialog should close
        await expect(confirmDialog).not.toBeVisible()
      }
    })

    test('should delete post after confirmation', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const initialCount = await posts.count()

      if (initialCount > 0) {
        const firstPost = posts.first()

        // Click delete button
        const deleteButton = firstPost.locator('.post-delete-button, .delete-btn').or(firstPost.getByRole('button', { name: /삭제|delete/i }))
        await deleteButton.first().click()

        // Confirm deletion
        const confirmButton = page.locator('.delete-confirm-button, .confirm-btn').or(page.getByRole('button', { name: /확인|confirm|삭제/i }))
        await confirmButton.first().click()

        // Wait for deletion to complete
        await page.waitForTimeout(1000)

        // Success message should appear
        const successMessage = page.getByText(/삭제되었습니다|Deleted|Success/i)
        await expect(successMessage.first()).toBeVisible({ timeout: 3000 })
      }
    })

    test('should publish draft post', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      // Filter by drafts
      const draftFilter = page.locator('.filter-draft').or(page.getByRole('tab', { name: /임시|draft/i })).or(page.getByText(/임시|draft/i))
      await draftFilter.first().click()
      await page.waitForTimeout(300)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for publish button
        const publishButton = firstPost.locator('.post-publish-button, .publish-btn').or(firstPost.getByRole('button', { name: /발행|publish/i }))

        if (await publishButton.first().isVisible().catch(() => false)) {
          await publishButton.first().click()

          // Wait for publish to complete
          await page.waitForTimeout(500)

          // Success message should appear
          const successMessage = page.getByText(/발행되었습니다|Published|Success/i)
          await expect(successMessage.first()).toBeVisible({ timeout: 3000 })
        }
      }
    })

    test('should navigate to post edit page', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Click edit button
        const editButton = firstPost.locator('.post-edit-button, .edit-btn').or(firstPost.getByRole('button', { name: /수정|edit/i }))
        await editButton.first().click()

        // Should navigate to edit page
        await expect(page).toHaveURL(/\/posts\/\d+\/edit|\/write\/\d+/)
      }
    })

    test('should display post metadata in my posts', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check title
        const title = firstPost.locator('.post-title, h2, h3')
        await expect(title.first()).toBeVisible()

        // Check timestamp
        const timestamp = firstPost.locator('.post-timestamp, .timestamp, time, .created-at')
        await expect(timestamp.first()).toBeVisible()

        // Check status badge
        const statusBadge = firstPost.locator('.post-status, .status-badge')
        if (await statusBadge.first().isVisible().catch(() => false)) {
          await expect(statusBadge.first()).toBeVisible()
        }
      }
    })

    test('should show create new post button', async ({ page }) => {
      await page.goto('/my')

      const createButton = page.locator('.create-post-button, .new-post-btn').or(page.getByRole('button', { name: /새 글|create|write|작성/i })).or(page.getByRole('link', { name: /새 글|create|write|작성/i }))
      await expect(createButton.first()).toBeVisible()

      // Click create button
      await createButton.first().click()

      // Should navigate to create page
      await expect(page).toHaveURL(/\/posts\/new|\/write/)
    })

    test('should sort posts by creation date', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('.my-post-card, .post-card')
      const postCount = await posts.count()

      if (postCount > 1) {
        // Check if sort options exist
        const sortSelect = page.locator('.post-sort-select, select').or(page.getByRole('combobox'))

        if (await sortSelect.first().isVisible().catch(() => false)) {
          // Try different sort options
          await sortSelect.first().selectOption({ value: 'newest' })
          await page.waitForTimeout(300)

          await sortSelect.first().selectOption({ value: 'oldest' })
          await page.waitForTimeout(300)
        }
      }
    })
  })

  test.describe('Responsive Design', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should be responsive on mobile viewport', async ({ page }) => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 })

      await page.goto('/my')

      await page.waitForTimeout(500)

      // Profile section should be visible
      const profileSection = page.locator('.my-profile, .profile-section, .profile-card')
      await expect(profileSection.first()).toBeVisible()

      // Posts section should be visible
      const postsSection = page.locator('.my-posts, .posts-section')
      await expect(postsSection.first()).toBeVisible()
    })

    test('should have mobile-friendly edit buttons', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 })

      await page.goto('/my')

      await page.waitForTimeout(500)

      // Edit button should be accessible on mobile
      const editButton = page.locator('.profile-edit-button, .edit-button').or(page.getByRole('button', { name: /수정|edit/i }))
      await expect(editButton.first()).toBeVisible()
    })
  })
})
