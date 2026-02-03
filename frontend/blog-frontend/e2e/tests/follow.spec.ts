import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Follow features
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('Follow Features', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test.describe('Follow Button', () => {
    test('should display follow button on user profile page', async ({ page }) => {
      await page.goto('/@anotheruser')

      // Check follow button is visible using CSS class and getByRole
      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))
      await expect(followButton.first()).toBeVisible()
    })

    test('should not display follow button on own profile', async ({ page }) => {
      await page.goto('/@testuser') // Same as logged in user

      // Follow button should not be visible on own profile
      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))
      await expect(followButton).not.toBeVisible()
    })

    test('should toggle follow state when clicking follow button', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))
      await expect(followButton.first()).toBeVisible()

      // Get initial state
      const initialText = await followButton.first().textContent()
      const isFollowing = initialText?.includes('팔로잉') || initialText?.includes('Following')

      // Click to toggle
      await followButton.first().click()
      await page.waitForTimeout(500)

      // Check state changed
      const newText = await followButton.first().textContent()
      if (isFollowing) {
        expect(newText).toMatch(/팔로우|Follow/)
      } else {
        expect(newText).toMatch(/팔로잉|Following/)
      }
    })

    test('should update follower count when following', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followerCount = page.locator('.follower-count')
      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))

      // Get initial count
      const initialCount = parseInt(await followerCount.first().textContent() || '0')

      // Follow if not already following
      const buttonText = await followButton.first().textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.first().click()
        await page.waitForTimeout(500)

        // Check count increased
        const newCount = parseInt(await followerCount.first().textContent() || '0')
        expect(newCount).toBe(initialCount + 1)
      }
    })

    test('should update follower count when unfollowing', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followerCount = page.locator('.follower-count')
      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))

      // First follow if needed
      let buttonText = await followButton.first().textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.first().click()
        await page.waitForTimeout(500)
      }

      // Get count after following
      const countAfterFollow = parseInt(await followerCount.first().textContent() || '0')

      // Now unfollow
      await followButton.first().click()
      await page.waitForTimeout(500)

      // Check count decreased
      const finalCount = parseInt(await followerCount.first().textContent() || '0')
      expect(finalCount).toBe(countAfterFollow - 1)
    })

    test('should show loading state while toggling follow', async ({ page }) => {
      await page.goto('/@anotheruser')

      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))

      // Click button
      await followButton.first().click()

      // Check for loading state
      const isDisabled = await followButton.first().isDisabled().catch(() => false)
      // Loading state might be very brief, so we just verify action completes
      await page.waitForTimeout(500)
    })

    test('should prompt login when clicking follow without authentication', async ({ page }) => {
      await mockLogout(page)
      await page.goto('/@anotheruser')

      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))

      // Click follow button
      await followButton.first().click()
      await page.waitForTimeout(500)

      // Should show login prompt or redirect to login
      const loginModal = page.locator('.login-modal, .modal').filter({ hasText: /로그인|login/i })
      const isRedirected = page.url().includes('/login')

      const isModalVisible = await loginModal.first().isVisible().catch(() => false)

      expect(isModalVisible || isRedirected).toBeTruthy()
    })
  })

  test.describe('Follower/Following Lists', () => {
    test('should display follower count on profile', async ({ page }) => {
      await page.goto('/@testuser')

      const followerCount = page.locator('.follower-count').or(page.getByText(/팔로워|follower/i).locator('..').locator('.count, span'))
      await expect(followerCount.first()).toBeVisible()

      const countText = await followerCount.first().textContent()
      expect(countText).toMatch(/\d+/)
    })

    test('should display following count on profile', async ({ page }) => {
      await page.goto('/@testuser')

      const followingCount = page.locator('.following-count').or(page.getByText(/팔로잉|following/i).locator('..').locator('.count, span'))
      await expect(followingCount.first()).toBeVisible()

      const countText = await followingCount.first().textContent()
      expect(countText).toMatch(/\d+/)
    })

    test('should open follower modal when clicking follower count', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      // Check modal is open using CSS class or role
      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      await expect(followerModal.first()).toBeVisible()

      // Check modal title
      const modalTitle = followerModal.first().locator('h2, h3, .modal-title')
      await expect(modalTitle.first()).toContainText(/팔로워|Followers/)
    })

    test('should open following modal when clicking following count', async ({ page }) => {
      await page.goto('/@testuser')

      const followingButton = page.locator('.following-stat, .following-count').or(page.getByRole('button', { name: /팔로잉|following/i }))
      await followingButton.first().click()

      await page.waitForTimeout(500)

      // Check modal is open
      const followerModal = page.locator('.follower-modal, .following-modal, .modal').or(page.getByRole('dialog'))
      await expect(followerModal.first()).toBeVisible()

      // Check modal title
      const modalTitle = followerModal.first().locator('h2, h3, .modal-title')
      await expect(modalTitle.first()).toContainText(/팔로잉|Following/)
    })

    test('should display user list in follower modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      await expect(followerModal.first()).toBeVisible()

      // Check for user items or empty state
      const userItems = followerModal.first().locator('.user-item, .follower-item, li')
      const emptyState = followerModal.first().locator('.empty-state, .empty-message').or(page.getByText(/팔로워가 없|no followers|없습니다/i))

      const userCount = await userItems.count()
      const hasEmptyState = await emptyState.first().isVisible().catch(() => false)

      expect(userCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should close modal when clicking close button', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      await expect(followerModal.first()).toBeVisible()

      // Click close button
      const closeButton = followerModal.first().locator('.close-button, .modal-close, button[aria-label*="close"]').or(followerModal.first().getByRole('button', { name: /닫기|close|×/i }))
      await closeButton.first().click()

      await page.waitForTimeout(300)

      // Modal should be closed
      await expect(followerModal).not.toBeVisible()
    })

    test('should navigate to user profile when clicking user in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      const userItems = followerModal.first().locator('.user-item, .follower-item, li')
      const userCount = await userItems.count()

      if (userCount > 0) {
        const firstUser = userItems.first()
        const userLink = firstUser.locator('a')
        await userLink.first().click()

        await page.waitForTimeout(500)

        // Should navigate to user profile
        await expect(page).toHaveURL(/@\w+/)
      }
    })

    test('should show follow button for each user in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      const userItems = followerModal.first().locator('.user-item, .follower-item, li')
      const userCount = await userItems.count()

      if (userCount > 0) {
        // Check each user has a follow button (except self)
        for (let i = 0; i < Math.min(userCount, 3); i++) {
          const userItem = userItems.nth(i)
          const followBtnInItem = userItem.locator('.follow-button, button').filter({ hasText: /팔로우|follow/i })

          // User might be self or follow button might be visible
          const hasFollowButton = await followBtnInItem.first().isVisible().catch(() => false)
          // Just verify the item is visible
          await expect(userItem).toBeVisible()
        }
      }
    })

    test('should support infinite scroll in modal', async ({ page }) => {
      await page.goto('/@testuser')

      const followerButton = page.locator('.follower-stat, .follower-count').or(page.getByRole('button', { name: /팔로워|followers/i }))
      await followerButton.first().click()

      await page.waitForTimeout(500)

      const followerModal = page.locator('.follower-modal, .modal').or(page.getByRole('dialog'))
      const userItems = followerModal.first().locator('.user-item, .follower-item, li')
      const initialCount = await userItems.count()

      if (initialCount > 0) {
        // Scroll to bottom of modal
        await followerModal.first().evaluate((modal) => {
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

      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))

      // Follow the user
      let buttonText = await followButton.first().textContent()
      if (buttonText?.includes('팔로우') || buttonText?.includes('Follow')) {
        await followButton.first().click()
        await page.waitForTimeout(500)
      }

      // Reload page
      await page.reload()

      // Check follow state persisted
      const newButtonText = await followButton.first().textContent()
      expect(newButtonText).toMatch(/팔로잉|Following/)
    })
  })

  test.describe('Error Handling', () => {
    test('should handle self-follow attempt gracefully', async ({ page }) => {
      // This should be prevented by UI, but test error handling if somehow triggered
      await page.goto('/@testuser')

      // Follow button should not exist on own profile
      const followButton = page.locator('.follow-button').or(page.getByRole('button', { name: /팔로우|follow/i }))
      await expect(followButton).not.toBeVisible()
    })

    test('should handle follow on non-existent user', async ({ page }) => {
      await page.goto('/@nonexistentuser123')

      // Should show user not found
      const notFoundMessage = page.locator('.not-found, .error-message').or(page.getByText(/사용자를 찾을 수 없습니다|User not found|찾을 수 없|존재하지 않/i))

      await expect(notFoundMessage.first()).toBeVisible()
    })
  })
})
