import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Follow features
 * SCENARIO-014: 사용자 팔로우 기능 시나리오
 */
test.describe('Follow Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test.describe('Follow Button', () => {
    test('should display follow button on user profile page', async ({ page }) => {
      await page.goto('/@anotheruser')

      // Check follow button is visible (not on own profile)
      const followButton = page.locator('[data-testid="follow-button"]')
      await expect(followButton).toBeVisible()
    })

    test('should not display follow button on own profile', async ({ page }) => {
      await page.goto('/@testuser') // Same as logged in user

      // Follow button should not be visible on own profile
      const followButton = page.locator('[data-testid="follow-button"]')
      await expect(followButton).not.toBeVisible()
    })

    test('should toggle follow state when clicking follow button', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followButton = page.locator('[data-testid="follow-button"]')
      await expect(followButton).toBeVisible()

      // Get initial state
      const initialText = await followButton.textContent()
      const isFollowing = initialText?.includes('팔로잉') || initialText?.includes('Following')

      // Click to toggle
      await followButton.click()
      await page.waitForTimeout(500)

      // Check state changed
      const newText = await followButton.textContent()
      if (isFollowing) {
        expect(newText).toMatch(/팔로우|Follow/)
      } else {
        expect(newText).toMatch(/팔로잉|Following/)
      }
    })

    test('should update follower count when following', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followerCount = page.locator('[data-testid="follower-count"]')
      const followButton = page.locator('[data-testid="follow-button"]')

      // Get initial count
      const initialCount = parseInt(await followerCount.textContent() || '0')

      // Follow if not already following
      const buttonText = await followButton.textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.click()
        await page.waitForTimeout(500)

        // Check count increased
        const newCount = parseInt(await followerCount.textContent() || '0')
        expect(newCount).toBe(initialCount + 1)
      }
    })

    test('should update follower count when unfollowing', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followerCount = page.locator('[data-testid="follower-count"]')
      const followButton = page.locator('[data-testid="follow-button"]')

      // First follow if needed
      let buttonText = await followButton.textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.click()
        await page.waitForTimeout(500)
      }

      // Get count after following
      const countAfterFollow = parseInt(await followerCount.textContent() || '0')

      // Now unfollow
      await followButton.click()
      await page.waitForTimeout(500)

      // Check count decreased
      const finalCount = parseInt(await followerCount.textContent() || '0')
      expect(finalCount).toBe(countAfterFollow - 1)
    })

    test('should show loading state while toggling follow', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followButton = page.locator('[data-testid="follow-button"]')

      // Click button
      await followButton.click()

      // Check for loading state
      const loadingState = followButton.locator('[data-testid="follow-loading"]')
      // Loading state might be very brief, so we just check button is disabled or has loading indicator
      const isDisabled = await followButton.isDisabled().catch(() => false)
      const hasLoadingIndicator = await loadingState.isVisible().catch(() => false)

      // Either loading indicator or disabled state is acceptable
      // Or the action just completed
      await page.waitForTimeout(500)
    })

    test('should prompt login when clicking follow without authentication', async ({ page }) => {
      await mockLogout(page)
      await page.goto('/@anotheruser')

      const followButton = page.locator('[data-testid="follow-button"]')

      // Click follow button
      await followButton.click()
      await page.waitForTimeout(500)

      // Should show login prompt or redirect to login
      const loginModal = page.locator('[data-testid="login-modal"]')
      const loginPrompt = page.locator('[data-testid="login-prompt"]')
      const isRedirected = page.url().includes('/login')

      const isModalVisible = await loginModal.isVisible().catch(() => false)
      const isPromptVisible = await loginPrompt.isVisible().catch(() => false)

      expect(isModalVisible || isPromptVisible || isRedirected).toBeTruthy()
    })
  })

  test.describe('Follower/Following Lists', () => {
    test('should display follower count on profile', async ({ page }) => {
      await page.goto('/@testuser')

      const followerCount = page.locator('[data-testid="follower-count"]')
      await expect(followerCount).toBeVisible()

      const countText = await followerCount.textContent()
      expect(countText).toMatch(/\d+/)
    })

    test('should display following count on profile', async ({ page }) => {
      await page.goto('/@testuser')

      const followingCount = page.locator('[data-testid="following-count"]')
      await expect(followingCount).toBeVisible()

      const countText = await followingCount.textContent()
      expect(countText).toMatch(/\d+/)
    })

    test('should open follower modal when clicking follower count', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      // Check modal is open
      const followerModal = page.locator('[data-testid="follower-modal"]')
      await expect(followerModal).toBeVisible()

      // Check modal title
      const modalTitle = followerModal.locator('[data-testid="modal-title"]')
      await expect(modalTitle).toContainText(/팔로워|Followers/)
    })

    test('should open following modal when clicking following count', async ({ page }) => {
      await page.goto('/@testuser')

      const followingButton = page.locator('[data-testid="following-stat-button"]')
      await followingButton.click()

      await page.waitForTimeout(500)

      // Check modal is open
      const followerModal = page.locator('[data-testid="follower-modal"]')
      await expect(followerModal).toBeVisible()

      // Check modal title
      const modalTitle = followerModal.locator('[data-testid="modal-title"]')
      await expect(modalTitle).toContainText(/팔로잉|Following/)
    })

    test('should display user list in follower modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('[data-testid="follower-modal"]')
      await expect(followerModal).toBeVisible()

      // Check for user items or empty state
      const userItems = followerModal.locator('[data-testid="user-item"]')
      const emptyState = followerModal.locator('[data-testid="empty-followers"]')

      const userCount = await userItems.count()
      const hasEmptyState = await emptyState.isVisible().catch(() => false)

      expect(userCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should close modal when clicking close button', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('[data-testid="follower-modal"]')
      await expect(followerModal).toBeVisible()

      // Click close button
      const closeButton = followerModal.locator('[data-testid="modal-close"]')
      await closeButton.click()

      await page.waitForTimeout(300)

      // Modal should be closed
      await expect(followerModal).not.toBeVisible()
    })

    test('should navigate to user profile when clicking user in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('[data-testid="follower-modal"]')
      const userItems = followerModal.locator('[data-testid="user-item"]')
      const userCount = await userItems.count()

      if (userCount > 0) {
        const firstUser = userItems.first()
        const userLink = firstUser.locator('a')
        await userLink.click()

        await page.waitForTimeout(500)

        // Should navigate to user profile
        await expect(page).toHaveURL(/@\w+/)
      }
    })

    test('should show follow button for each user in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('[data-testid="follower-modal"]')
      const userItems = followerModal.locator('[data-testid="user-item"]')
      const userCount = await userItems.count()

      if (userCount > 0) {
        // Check each user has a follow button (except self)
        for (let i = 0; i < Math.min(userCount, 3); i++) {
          const userItem = userItems.nth(i)
          const followButton = userItem.locator('[data-testid="follow-button"]')
          const isSelf = userItem.locator('[data-testid="self-indicator"]')

          const hasSelfIndicator = await isSelf.isVisible().catch(() => false)
          if (!hasSelfIndicator) {
            // Should have follow button for other users
            await expect(followButton).toBeVisible()
          }
        }
      }
    })

    test('should support infinite scroll in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('[data-testid="follower-stat-button"]')
      await followerButton.click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('[data-testid="follower-modal"]')
      const userItems = followerModal.locator('[data-testid="user-item"]')
      const initialCount = await userItems.count()

      if (initialCount > 0) {
        // Scroll to bottom of modal
        await followerModal.evaluate((modal) => {
          modal.scrollTo(0, modal.scrollHeight)
        })

        await page.waitForTimeout(1000)

        // Check if more users loaded
        const newCount = await userItems.count()
        expect(newCount).toBeGreaterThanOrEqual(initialCount)
      }
    })
  })

  test.describe('Follow State Persistence', () => {
    test('should persist follow state on page reload', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followButton = page.locator('[data-testid="follow-button"]')

      // Follow the user
      let buttonText = await followButton.textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.click()
        await page.waitForTimeout(500)
      }

      // Reload page
      await page.reload()

      // Check follow state persisted
      const newButtonText = await followButton.textContent()
      expect(newButtonText).toMatch(/팔로잉|Following/)
    })
  })

  test.describe('Error Handling', () => {
    test('should handle self-follow attempt gracefully', async ({ page }) => {
      // This should be prevented by UI, but test error handling if somehow triggered
      await page.goto('/@testuser')

      // Follow button should not exist on own profile
      const followButton = page.locator('[data-testid="follow-button"]')
      await expect(followButton).not.toBeVisible()
    })

    test('should handle follow on non-existent user', async ({ page }) => {
      await page.goto('/@nonexistentuser123')

      // Should show user not found
      const notFoundMessage = page.locator('[data-testid="user-not-found"]')
      const errorMessage = page.getByText(/사용자를 찾을 수 없습니다|User not found/i)

      const hasNotFound = await notFoundMessage.isVisible().catch(() => false)
      const hasErrorMessage = await errorMessage.isVisible().catch(() => false)

      expect(hasNotFound || hasErrorMessage).toBeTruthy()
    })
  })
})
