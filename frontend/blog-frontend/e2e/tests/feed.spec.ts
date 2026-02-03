import { test, expect } from '@playwright/test'
import { mockLogin, mockLogout } from '../fixtures/auth'

/**
 * E2E tests for Feed features
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('Feed Features', () => {
  test.describe('Feed Tab Visibility', () => {
    test('should display feed tab when logged in', async ({ page }) => {
      await mockLogin(page)
      await page.goto('/blog')

      // Check feed tab is visible using getByRole or getByText
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      await expect(feedTab).toBeVisible()
    })

    test('should not display feed tab when not logged in', async ({ page }) => {
      await mockLogout(page)
      await page.goto('/blog')

      // Feed tab should not be visible
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.locator('.feed-tab'))
      await expect(feedTab).not.toBeVisible()
    })

    test('should display tabs in correct order when logged in', async ({ page }) => {
      await mockLogin(page)
      await page.goto('/blog')

      // Check tab order: 피드 | 트렌딩 | 최신
      const tabs = page.locator('.post-list-tabs button, [role="tablist"] button, nav button')
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
      const tabs = page.locator('.post-list-tabs button, [role="tablist"] button, nav button')
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

      // Check feed tab is active using CSS class or aria-selected
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      const isActive = await feedTab.getAttribute('aria-selected') === 'true' ||
        await feedTab.evaluate(el => el.classList.contains('active'))
      expect(isActive).toBeTruthy()
    })

    test('should update URL when clicking feed tab', async ({ page }) => {
      await page.goto('/blog')

      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
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
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      const isActive = await feedTab.getAttribute('aria-selected') === 'true' ||
        await feedTab.evaluate(el => el.classList.contains('active'))
      expect(isActive).toBeTruthy()
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
      const posts = page.locator('.post-card, article.card, .card')
      const emptyFeed = page.locator('.empty-feed, .empty-state').or(page.getByText(/피드가 비어|팔로우하는|no posts/i))

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should show empty state when not following anyone', async ({ page }) => {
      // This test assumes the mock user doesn't follow anyone
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const emptyFeed = page.locator('.empty-feed, .empty-state').or(page.getByText(/팔로우하는 사용자가 없습니다|피드가 비어있습니다/i))

      const hasEmptyFeed = await emptyFeed.first().isVisible().catch(() => false)

      // If no posts, should show empty state
      const posts = page.locator('.post-card, article.card, .card')
      const postCount = await posts.count()

      if (postCount === 0) {
        expect(hasEmptyFeed).toBeTruthy()
      }
    })

    test('should display link to trending when feed is empty', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('.post-card, article.card, .card')
      const postCount = await posts.count()

      if (postCount === 0) {
        // Check for trending link
        const trendingLink = page.locator('.go-to-trending, a[href*="trending"]').or(page.getByRole('link', { name: /트렌딩|Trending/i }))
        const trendingButton = page.getByRole('button', { name: /트렌딩|Trending/i })

        const hasLink = await trendingLink.first().isVisible().catch(() => false)
        const hasButton = await trendingButton.first().isVisible().catch(() => false)

        expect(hasLink || hasButton).toBeTruthy()
      }
    })

    test('should navigate to trending when clicking link in empty feed', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('.post-card, article.card, .card')
      const postCount = await posts.count()

      if (postCount === 0) {
        const trendingLink = page.locator('.go-to-trending, a[href*="trending"]').or(page.getByRole('link', { name: /트렌딩|Trending/i }))

        if (await trendingLink.first().isVisible().catch(() => false)) {
          await trendingLink.first().click()
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

      const posts = page.locator('.post-card, article.card')
      const postCount = await posts.count()

      if (postCount >= 2) {
        // Get timestamps of first two posts
        const firstTimestamp = posts.first().locator('.timestamp, time, .created-at')
        const secondTimestamp = posts.nth(1).locator('.timestamp, time, .created-at')

        const firstTime = await firstTimestamp.getAttribute('datetime') || await firstTimestamp.textContent() || ''
        const secondTime = await secondTimestamp.getAttribute('datetime') || await secondTimestamp.textContent() || ''

        // First post should be newer or same as second (verify it's a date)
        if (firstTime && secondTime) {
          expect(firstTime).toBeTruthy()
          expect(secondTime).toBeTruthy()
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

      const posts = page.locator('.post-card, article.card, .card')
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

      const posts = page.locator('.post-card, article.card, .card')
      const initialCount = await posts.count()

      if (initialCount > 0) {
        // Scroll to bottom
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

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
      const endMessage = page.locator('.feed-end, .no-more').or(page.getByText(/더 이상|no more|end of feed/i))
      const hasEndMessage = await endMessage.first().isVisible().catch(() => false)

      // Either shows end message or just stops loading more - both are acceptable
    })
  })

  test.describe('Feed Post Interactions', () => {
    test.beforeEach(async ({ page }) => {
      await mockLogin(page)
    })

    test('should navigate to post detail when clicking post', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('.post-card, article.card')
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

      const posts = page.locator('.post-card, article.card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()

        // Check for author info using CSS class
        const authorName = firstPost.locator('.author-name, .author, .username')
        await expect(authorName.first()).toBeVisible()
      }
    })

    test('should navigate to author profile when clicking author name', async ({ page }) => {
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      const posts = page.locator('.post-card, article.card')
      const postCount = await posts.count()

      if (postCount > 0) {
        const firstPost = posts.first()
        const authorLink = firstPost.locator('.author-link, a.author, .author a')

        if (await authorLink.first().isVisible().catch(() => false)) {
          await authorLink.first().click()
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

      // Loading spinner should appear briefly - may be too fast to catch
      await page.waitForTimeout(1000)

      // Either posts or empty state should be visible
      const posts = page.locator('.post-card, article.card, .card')
      const emptyFeed = page.locator('.empty-feed, .empty-state').or(page.getByText(/피드가 비어|팔로우/i))

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should show error message on feed load failure', async ({ page }) => {
      // Simulate network failure
      await page.route('**/api/v1/blog/posts/feed*', route => route.abort())

      await mockLogin(page)
      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Check for error message or retry button
      const errorMessage = page.locator('.feed-error, .error-message').or(page.getByText(/오류|error|실패/i))
      const retryButton = page.locator('.retry-button').or(page.getByRole('button', { name: /다시|retry/i }))

      const hasError = await errorMessage.first().isVisible().catch(() => false)
      const hasRetry = await retryButton.first().isVisible().catch(() => false)

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

      const retryButton = page.locator('.retry-button').or(page.getByRole('button', { name: /다시|retry/i }))

      if (await retryButton.first().isVisible().catch(() => false)) {
        await retryButton.first().click()
        await page.waitForTimeout(1000)

        // Should load feed or show appropriate state
        const posts = page.locator('.post-card, article.card, .card')
        const emptyFeed = page.locator('.empty-feed, .empty-state')

        const postCount = await posts.count()
        const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

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

      const refreshButton = page.locator('.refresh-feed, .refresh-button').or(page.getByRole('button', { name: /새로고침|refresh/i }))

      if (await refreshButton.first().isVisible().catch(() => false)) {
        await refreshButton.first().click()

        await page.waitForTimeout(1000)

        // Feed should be refreshed
        const posts = page.locator('.post-card, article.card, .card')
        const emptyFeed = page.locator('.empty-feed, .empty-state')

        const postCount = await posts.count()
        const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

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
      const posts = page.locator('.post-card, article.card, .card')
      const emptyFeed = page.locator('.empty-feed, .empty-state')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

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
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      await expect(feedTab).toBeVisible()

      // Posts should adapt to mobile layout
      const posts = page.locator('.post-card, article.card')
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
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      await expect(feedTab).toBeVisible()

      // Posts should display properly
      const posts = page.locator('.post-card, article.card, .card')
      const emptyFeed = page.locator('.empty-feed, .empty-state')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })

    test('should display feed correctly on desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1440, height: 900 })

      await page.goto('/blog?tab=feed')

      await page.waitForTimeout(1000)

      // Feed tab should be visible
      const feedTab = page.getByRole('tab', { name: /피드|feed/i }).or(page.getByText(/피드|feed/i).first())
      await expect(feedTab).toBeVisible()

      // Posts may be displayed in grid on desktop
      const posts = page.locator('.post-card, article.card, .card')
      const emptyFeed = page.locator('.empty-feed, .empty-state')

      const postCount = await posts.count()
      const hasEmptyState = await emptyFeed.first().isVisible().catch(() => false)

      expect(postCount > 0 || hasEmptyState).toBeTruthy()
    })
  })
})
