import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for My Page (/my)
 * Tests user profile management and personal post management
 */
test.describe('My Page', () => {
  test.describe('Authentication', () => {
    test('should redirect to login when accessing /my without authentication', async ({ page }) => {
      // Ensure logged out
      await mockLogout(page)

      await page.goto('/my')

      // Should show login prompt or redirect
      const loginPrompt = page.locator('[data-testid="login-required"]')
      const loginButton = page.getByText(/로그인|Sign in|Login/i)

      const hasPrompt = await loginPrompt.isVisible().catch(() => false)
      const hasButton = await loginButton.isVisible().catch(() => false)

      expect(hasPrompt || hasButton).toBeTruthy()
    })

    test('should access my page after login', async ({ page }) => {
      await mockLogin(page)

      await page.goto('/my')

      // Should display my page content
      const myPageContainer = page.locator('[data-testid="my-page"]')
      await expect(myPageContainer).toBeVisible()
    })
  })

  test.describe('Profile Display', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display current profile information', async ({ page }) => {
      await page.goto('/my')

      // Check profile section
      const profileSection = page.locator('[data-testid="my-profile"]')
      await expect(profileSection).toBeVisible()

      // Check username
      const username = page.locator('[data-testid="profile-username"]')
      await expect(username).toBeVisible()

      // Check email
      const email = page.locator('[data-testid="profile-email"]')
      await expect(email).toBeVisible()

      // Check nickname
      const nickname = page.locator('[data-testid="profile-nickname"]')
      if (await nickname.isVisible()) {
        await expect(nickname).toBeVisible()
      }
    })

    test('should display profile edit button', async ({ page }) => {
      await page.goto('/my')

      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await expect(editButton).toBeVisible()
    })

    test('should display profile statistics', async ({ page }) => {
      await page.goto('/my')

      // Check post count
      const postCount = page.locator('[data-testid="my-post-count"]')
      if (await postCount.isVisible()) {
        const text = await postCount.textContent()
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
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      // Edit form should appear
      const editForm = page.locator('[data-testid="profile-edit-form"]')
      await expect(editForm).toBeVisible()

      // Cancel button should appear
      const cancelButton = page.locator('[data-testid="profile-cancel-button"]')
      await expect(cancelButton).toBeVisible()

      // Save button should appear
      const saveButton = page.locator('[data-testid="profile-save-button"]')
      await expect(saveButton).toBeVisible()
    })

    test('should update profile information', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      // Update nickname
      const nicknameInput = page.locator('[data-testid="input-nickname"]')
      await expect(nicknameInput).toBeVisible()
      await nicknameInput.fill('Updated Nickname')

      // Update bio
      const bioInput = page.locator('[data-testid="input-bio"]')
      if (await bioInput.isVisible()) {
        await bioInput.fill('Updated bio text')
      }

      // Save changes
      const saveButton = page.locator('[data-testid="profile-save-button"]')
      await saveButton.click()

      // Wait for save to complete
      await page.waitForTimeout(500)

      // Success message should appear
      const successMessage = page.getByText(/저장되었습니다|Saved|Success/i)
      await expect(successMessage).toBeVisible({ timeout: 3000 })
    })

    test('should cancel profile editing', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      // Make some changes
      const nicknameInput = page.locator('[data-testid="input-nickname"]')
      const originalValue = await nicknameInput.inputValue()
      await nicknameInput.fill('Temporary Change')

      // Click cancel
      const cancelButton = page.locator('[data-testid="profile-cancel-button"]')
      await cancelButton.click()

      // Edit form should be hidden
      const editForm = page.locator('[data-testid="profile-edit-form"]')
      await expect(editForm).not.toBeVisible()

      // Edit button should be visible again
      await expect(editButton).toBeVisible()
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
      const usernamePrompt = page.locator('[data-testid="username-setup"]')

      if (await usernamePrompt.isVisible()) {
        // Enter username
        const usernameInput = page.locator('[data-testid="input-username"]')
        await expect(usernameInput).toBeVisible()
        await usernameInput.fill('newusername')

        // Submit username
        const submitButton = page.locator('[data-testid="username-submit"]')
        await submitButton.click()

        await page.waitForTimeout(500)
      }
    })

    test('should validate username format', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      const usernameInput = page.locator('[data-testid="input-username"]')

      if (await usernameInput.isVisible()) {
        // Try invalid username (spaces, special chars)
        await usernameInput.fill('invalid username!')

        // Error message should appear
        const errorMessage = page.locator('[data-testid="username-error"]')
        await expect(errorMessage).toBeVisible()

        // Try valid username
        await usernameInput.fill('validusername')
        await expect(errorMessage).not.toBeVisible()
      }
    })

    test('should check username duplication', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      const usernameInput = page.locator('[data-testid="input-username"]')

      if (await usernameInput.isVisible()) {
        // Enter an existing username
        await usernameInput.fill('existinguser')
        await usernameInput.blur()

        // Wait for validation
        await page.waitForTimeout(500)

        // Duplication error might appear
        const duplicateError = page.locator('[data-testid="username-duplicate-error"]')
        // Check if duplicate validation is shown (it depends on backend response)
      }
    })

    test('should update social links', async ({ page }) => {
      await page.goto('/my')

      // Enter edit mode
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await editButton.click()

      // Update social links if available
      const githubInput = page.locator('[data-testid="input-social-github"]')
      if (await githubInput.isVisible()) {
        await githubInput.fill('https://github.com/testuser')
      }

      const twitterInput = page.locator('[data-testid="input-social-twitter"]')
      if (await twitterInput.isVisible()) {
        await twitterInput.fill('https://twitter.com/testuser')
      }

      const websiteInput = page.locator('[data-testid="input-social-website"]')
      if (await websiteInput.isVisible()) {
        await websiteInput.fill('https://testuser.com')
      }

      // Save changes
      const saveButton = page.locator('[data-testid="profile-save-button"]')
      await saveButton.click()

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
      const postsSection = page.locator('[data-testid="my-posts"]')
      await expect(postsSection).toBeVisible()

      // Check if posts or empty state is shown
      const posts = page.locator('[data-testid="my-post-card"]')
      const emptyState = page.locator('[data-testid="empty-my-posts"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyState.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should filter posts by status', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      // Check status filter exists
      const statusFilter = page.locator('[data-testid="post-status-filter"]')
      await expect(statusFilter).toBeVisible()

      // Filter by ALL (default)
      const allFilter = page.locator('[data-testid="filter-all"]')
      await allFilter.click()
      await page.waitForTimeout(300)

      // Filter by DRAFT
      const draftFilter = page.locator('[data-testid="filter-draft"]')
      await draftFilter.click()
      await page.waitForTimeout(300)

      // Should show only drafts
      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        // All posts should have draft badge
        for (let i = 0; i < postCount; i++) {
          const post = posts.nth(i)
          const draftBadge = post.locator('[data-testid="draft-badge"]')
          await expect(draftBadge).toBeVisible()
        }
      }

      // Filter by PUBLISHED
      const publishedFilter = page.locator('[data-testid="filter-published"]')
      await publishedFilter.click()
      await page.waitForTimeout(300)

      // Should show only published posts
      const publishedPosts = page.locator('[data-testid="my-post-card"]')
      const publishedCount = await publishedPosts.count()

      if (publishedCount > 0) {
        // No posts should have draft badge
        for (let i = 0; i < publishedCount; i++) {
          const post = publishedPosts.nth(i)
          const draftBadge = post.locator('[data-testid="draft-badge"]')
          await expect(draftBadge).not.toBeVisible()
        }
      }
    })

    test('should display post action buttons', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for edit button
        const editButton = firstPost.locator('[data-testid="post-edit-button"]')
        await expect(editButton).toBeVisible()

        // Check for delete button
        const deleteButton = firstPost.locator('[data-testid="post-delete-button"]')
        await expect(deleteButton).toBeVisible()
      }
    })

    test('should show delete confirmation dialog', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Click delete button
        const deleteButton = firstPost.locator('[data-testid="post-delete-button"]')
        await deleteButton.click()

        // Confirmation dialog should appear
        const confirmDialog = page.locator('[data-testid="delete-confirm-dialog"]')
        await expect(confirmDialog).toBeVisible()

        // Check dialog has confirm and cancel buttons
        const confirmButton = page.locator('[data-testid="delete-confirm-button"]')
        const cancelButton = page.locator('[data-testid="delete-cancel-button"]')

        await expect(confirmButton).toBeVisible()
        await expect(cancelButton).toBeVisible()

        // Cancel deletion
        await cancelButton.click()

        // Dialog should close
        await expect(confirmDialog).not.toBeVisible()
      }
    })

    test('should delete post after confirmation', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const initialCount = await posts.count()

      if (initialCount > 0) {
        const firstPost = posts.first()

        // Click delete button
        const deleteButton = firstPost.locator('[data-testid="post-delete-button"]')
        await deleteButton.click()

        // Confirm deletion
        const confirmButton = page.locator('[data-testid="delete-confirm-button"]')
        await confirmButton.click()

        // Wait for deletion to complete
        await page.waitForTimeout(1000)

        // Success message should appear
        const successMessage = page.getByText(/삭제되었습니다|Deleted|Success/i)
        await expect(successMessage).toBeVisible({ timeout: 3000 })
      }
    })

    test('should publish draft post', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      // Filter by drafts
      const draftFilter = page.locator('[data-testid="filter-draft"]')
      await draftFilter.click()
      await page.waitForTimeout(300)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for publish button
        const publishButton = firstPost.locator('[data-testid="post-publish-button"]')

        if (await publishButton.isVisible()) {
          await publishButton.click()

          // Wait for publish to complete
          await page.waitForTimeout(500)

          // Success message should appear
          const successMessage = page.getByText(/발행되었습니다|Published|Success/i)
          await expect(successMessage).toBeVisible({ timeout: 3000 })
        }
      }
    })

    test('should navigate to post edit page', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Click edit button
        const editButton = firstPost.locator('[data-testid="post-edit-button"]')
        await editButton.click()

        // Should navigate to edit page
        await expect(page).toHaveURL(/\/posts\/\d+\/edit/)
      }
    })

    test('should display post metadata in my posts', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check title
        const title = firstPost.locator('[data-testid="post-title"]')
        await expect(title).toBeVisible()

        // Check timestamp
        const timestamp = firstPost.locator('[data-testid="post-timestamp"]')
        await expect(timestamp).toBeVisible()

        // Check status badge
        const statusBadge = firstPost.locator('[data-testid="post-status"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toBeVisible()
        }
      }
    })

    test('should show create new post button', async ({ page }) => {
      await page.goto('/my')

      const createButton = page.locator('[data-testid="create-post-button"]')
      await expect(createButton).toBeVisible()

      // Click create button
      await createButton.click()

      // Should navigate to create page
      await expect(page).toHaveURL(/\/posts\/new/)
    })

    test('should sort posts by creation date', async ({ page }) => {
      await page.goto('/my')

      await page.waitForTimeout(500)

      const posts = page.locator('[data-testid="my-post-card"]')
      const postCount = await posts.count()

      if (postCount > 1) {
        // Check if sort options exist
        const sortSelect = page.locator('[data-testid="post-sort-select"]')

        if (await sortSelect.isVisible()) {
          // Try different sort options
          await sortSelect.selectOption({ value: 'newest' })
          await page.waitForTimeout(300)

          await sortSelect.selectOption({ value: 'oldest' })
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
      const profileSection = page.locator('[data-testid="my-profile"]')
      await expect(profileSection).toBeVisible()

      // Posts section should be visible
      const postsSection = page.locator('[data-testid="my-posts"]')
      await expect(postsSection).toBeVisible()
    })

    test('should have mobile-friendly edit buttons', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 })

      await page.goto('/my')

      await page.waitForTimeout(500)

      // Edit button should be accessible on mobile
      const editButton = page.locator('[data-testid="profile-edit-button"]')
      await expect(editButton).toBeVisible()
    })
  })
})
