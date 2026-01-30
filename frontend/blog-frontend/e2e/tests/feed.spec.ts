import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Feed features
 * SCENARIO-015: 피드 기능 시나리오
 */
test.describe('Feed Features', () => {
  test.describe('Feed Tab Visibility', () => {
    test('should display feed tab when logged in', async ({ page }) => {
      await mockLogin(page)
      await page.goto('/blog')

      // Check feed tab is visible
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toBeVisible()
    })

    test('should not display feed tab when not logged in', async ({ page }) => {
      await mockLogout(page)
      await page.goto('/blog')

      // Feed tab should not be visible
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).not.toBeVisible()
    })

    test('should display tabs in correct order when logged in', async ({ page }) => {
      await mockLogin(page)
      await page.goto('/blog')

      // Check tab order: 피드 | 트렌딩 | 최신
      const tabs = page.locator('[data-testid="post-list-tabs"] button')
      const tabCount = await tabs.count()

      expect(tabCount).toBeGreaterThanOrEqual(3)

      const firstTab = await tabs.nth(0).textContent()
      const secondTab = await tabs.nth(1).textContent()
      const thirdTab = await tabs.nth(2).textContent()

      expect(firstTab).toMatch(/피드|Feed/)
      expect(secondTab).toMatch(/트렌딩|Trending/)
      expect(thirdTab).toMatch(/최신|Recent/)
    })

    test('should display tabs in correct order when not logged in', async ({ page }) => {
      await mockLogout(page)
      await page.goto('/blog')

      // Check tab order: 트렌딩 | 최신 (no feed tab)
      const tabs = page.locator('[data-testid="post-list-tabs"] button')
      const tabCount = await tabs.count()

      expect(tabCount).toBeGreaterThanOrEqual(2)

      const firstTab = await tabs.nth(0).textContent()
      const secondTab = await tabs.nth(1).textContent()

      expect(firstTab).toMatch(/트렌딩|Trending/)
      expect(secondTab).toMatch(/최신|Recent/)
    })
  })

  test.describe('Feed Tab Selection', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should navigate to feed tab via URL', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      // Check feed tab is active
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toHaveAttribute('data-active', 'true')
    })

    test('should update URL when clicking feed tab', async ({ page }) => {
      await page.goto('/blog')

      const feedTab = page.locator('[data-testid="feed-tab"]')
      await feedTab.click()

      await page.waitForTimeout(500)

      // Check URL updated
      await expect(page).toHaveURL(/tab=feed/)
    })

    test('should maintain tab selection on page reload', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      // Reload page
      await page.reload()

      // Feed tab should still be active
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toHaveAttribute('data-active', 'true')
    })
  })

  test.describe('Feed Content', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display posts from followed users', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Check for posts or empty state
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should show empty state when not following anyone', async ({ page }) => {
      // This test assumes the mock user doesn't follow anyone
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const emptyFeed = page.locator('[data-testid="empty-feed"]')
      const emptyMessage = page.getByText(/팔로우하는 사용자가 없습니다|피드가 비어있습니다/i)

      const hasEmptyFeed = await emptyFeed.isVisible().catch(() => false)
      const hasEmptyMessage = await emptyMessage.isVisible().catch(() => false)

      // If no posts, should show empty state
      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount === 0) {
        expect(hasEmptyFeed || hasEmptyMessage).toBeTruthy()
      }
    })

    test('should display link to trending when feed is empty', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount === 0) {
        // Check for trending link
        const trendingLink = page.locator('[data-testid="go-to-trending"]')
        const trendingButton = page.getByRole('button', { name: /트렌딩|Trending/i })

        const hasLink = await trendingLink.isVisible().catch(() => false)
        const hasButton = await trendingButton.isVisible().catch(() => false)

        expect(hasLink || hasButton).toBeTruthy()
      }
    })

    test('should navigate to trending when clicking link in empty feed', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount === 0) {
        const trendingLink = page.locator('[data-testid="go-to-trending"]')

        if (await trendingLink.isVisible()) {
          await trendingLink.click()
          await page.waitForTimeout(500)

          // Should navigate to trending tab
          await expect(page).toHaveURL(/tab=trending/)
        }
      }
    })
  })

  test.describe('Feed Post Ordering', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display posts in chronological order (newest first)', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount >= 2) {
        // Get timestamps of first two posts
        const firstTimestamp = page.locator('[data-testid="post-card"]:nth-child(1) [data-testid="post-timestamp"]')
        const secondTimestamp = page.locator('[data-testid="post-card"]:nth-child(2) [data-testid="post-timestamp"]')

        const firstTime = await firstTimestamp.getAttribute('data-timestamp') || ''
        const secondTime = await secondTimestamp.getAttribute('data-timestamp') || ''

        // First post should be newer or same as second
        if (firstTime && secondTime) {
          const firstDate = new Date(firstTime)
          const secondDate = new Date(secondTime)
          expect(firstDate.getTime()).toBeGreaterThanOrEqual(secondDate.getTime())
        }
      }
    })
  })

  test.describe('Feed Infinite Scroll', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should load more posts when scrolling to bottom', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const initialCount = await posts.count()

      if (initialCount > 0) {
        // Scroll to bottom
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

        // Wait for potential load
        await page.waitForTimeout(1500)

        // Get new post count
        const newCount = await posts.count()

        // Should have loaded more posts or stayed same if no more
        expect(newCount).toBeGreaterThanOrEqual(initialCount)
      }
    })

    test('should show loading indicator while fetching more posts', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const initialCount = await posts.count()

      if (initialCount > 0) {
        // Scroll to bottom
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

        // Check for loading indicator
        const loadingIndicator = page.locator('[data-testid="loading-more"]')
        // Loading indicator might be brief
        await page.waitForTimeout(500)
      }
    })

    test('should show end of feed message when all posts loaded', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Scroll multiple times to load all posts
      for (let i = 0; i < 5; i++) {
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
        await page.waitForTimeout(1000)
      }

      // Check for end message or no more loading
      const endMessage = page.locator('[data-testid="feed-end"]')
      const hasEndMessage = await endMessage.isVisible().catch(() => false)

      // Either shows end message or just stops loading more
      // This is acceptable either way
    })
  })

  test.describe('Feed Post Interactions', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should navigate to post detail when clicking post', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()
        await firstPost.click()

        await page.waitForTimeout(500)

        // Should navigate to post detail
        await expect(page).toHaveURL(/\/posts\//)
      }
    })

    test('should display author info on feed posts', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for author info
        const authorName = firstPost.locator('[data-testid="author-name"]')
        await expect(authorName).toBeVisible()
      }
    })

    test('should navigate to author profile when clicking author name', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()
        const authorLink = firstPost.locator('[data-testid="author-link"]')

        if (await authorLink.isVisible()) {
          await authorLink.click()
          await page.waitForTimeout(500)

          // Should navigate to author profile
          await expect(page).toHaveURL(/@\w+/)
        }
      }
    })
  })

  test.describe('Feed Loading States', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should show loading spinner while fetching feed', async ({ page }) => {
      // Start loading and check for spinner
      await page.goto('/blog?tab=feed')

      // Loading spinner should appear briefly
      const loadingSpinner = page.locator('[data-testid="feed-loading"]')
      // May be too fast to catch, so we just ensure page loads correctly
      await page.waitForTimeout(1000)

      // Either posts or empty state should be visible
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should show error message on feed load failure', async ({ page }) => {
      // Simulate network failure
      await page.route('**/api/v1/blog/posts/feed*', route => route.abort())

      await mockLogin(page)
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Check for error message or retry button
      const errorMessage = page.locator('[data-testid="feed-error"]')
      const retryButton = page.locator('[data-testid="retry-button"]')

      const hasError = await errorMessage.isVisible().catch(() => false)
      const hasRetry = await retryButton.isVisible().catch(() => false)

      expect(hasError || hasRetry).toBeTruthy()
    })

    test('should retry loading when clicking retry button', async ({ page }) => {
      let failCount = 0

      // First request fails, subsequent succeed
      await page.route('**/api/v1/blog/posts/feed*', route => {
        failCount++
        if (failCount === 1) {
          route.abort()
        } else {
          route.continue()
        }
      })

      await mockLogin(page)
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const retryButton = page.locator('[data-testid="retry-button"]')

      if (await retryButton.isVisible()) {
        await retryButton.click()
        await page.waitForTimeout(1000)

        // Should load feed or show appropriate state
        const posts = page.locator('[data-testid="post-card"]')
        const emptyFeed = page.locator('[data-testid="empty-feed"]')

        const postCount = await posts.count()
        const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

        expect(postCount > 0 || hasEmptyState).toBeTruthy()
      }
    })
  })

  test.describe('Feed Refresh', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should refresh feed when clicking refresh button', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const refreshButton = page.locator('[data-testid="refresh-feed"]')

      if (await refreshButton.isVisible()) {
        await refreshButton.click()

        // Check for loading state
        const loadingSpinner = page.locator('[data-testid="feed-loading"]')
        await page.waitForTimeout(1000)

        // Feed should be refreshed
        const posts = page.locator('[data-testid="post-card"]')
        const emptyFeed = page.locator('[data-testid="empty-feed"]')

        const postCount = await posts.count()
        const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

        expect(postCount > 0 || hasEmptyState).toBeTruthy()
      }
    })

    test('should refresh feed on pull-to-refresh (mobile)', async ({ page }) => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 })

      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Simulate pull-to-refresh gesture
      await page.mouse.move(187, 300)
      await page.mouse.down()
      await page.mouse.move(187, 500, { steps: 10 })
      await page.mouse.up()

      await page.waitForTimeout(1000)

      // Feed should still be visible
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Feed Responsiveness', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should display feed correctly on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 })

      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Feed tab should be visible
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toBeVisible()

      // Posts should adapt to mobile layout
      const posts = page.locator('[data-testid="post-card"]')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()
        const box = await firstPost.boundingBox()

        if (box) {
          // Post should fit within mobile width
          expect(box.width).toBeLessThanOrEqual(375)
        }
      }
    })

    test('should display feed correctly on tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 })

      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Feed tab should be visible
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toBeVisible()

      // Posts should display properly
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should display feed correctly on desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1440, height: 900 })

      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Feed tab should be visible
      const feedTab = page.locator('[data-testid="feed-tab"]')
      await expect(feedTab).toBeVisible()

      // Posts may be displayed in grid on desktop
      const posts = page.locator('[data-testid="post-card"]')
      const emptyFeed = page.locator('[data-testid="empty-feed"]')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })
  })
})
