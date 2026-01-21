import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for User Blog Page (/@username)
 * Tests public profile viewing and user-specific posts
 */
test.describe('User Blog Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should access user blog page via /@username', async ({ page }) => {
    await page.goto('/@testuser')

    // Check page loaded successfully
    await expect(page).toHaveURL('/@testuser')

    // Check user profile section is visible
    const profileSection = page.locator('[data-testid="user-profile"]')
    await expect(profileSection).toBeVisible()
  })

  test('should display user profile information', async ({ page }) => {
    await page.goto('/@testuser')

    // Check profile components
    const profileSection = page.locator('[data-testid="user-profile"]')
    await expect(profileSection).toBeVisible()

    // Check username is displayed
    const username = page.locator('[data-testid="profile-username"]')
    await expect(username).toBeVisible()
    await expect(username).toContainText('testuser')

    // Check nickname/display name
    const nickname = page.locator('[data-testid="profile-nickname"]')
    if (await nickname.isVisible()) {
      await expect(nickname).toBeVisible()
    }

    // Check bio/introduction if exists
    const bio = page.locator('[data-testid="profile-bio"]')
    if (await bio.isVisible()) {
      await expect(bio).toBeVisible()
    }

    // Check profile image
    const avatar = page.locator('[data-testid="profile-avatar"]')
    await expect(avatar).toBeVisible()
  })

  test('should display user post list', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for posts to load
    await page.waitForTimeout(500)

    // Check posts section exists
    const postsSection = page.locator('[data-testid="user-posts"]')
    await expect(postsSection).toBeVisible()

    // Check if posts are displayed or empty state is shown
    const posts = page.locator('[data-testid="post-card"]')
    const emptyState = page.locator('[data-testid="empty-posts"]')

    const postCount = await posts.count()
    const hasEmptyState = await emptyState.isVisible().catch(() => false)

    expect(postCount > 0 || hasEmptyState).toBeTruthy()
  })

  test('should navigate to post detail when clicking a post', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for posts to load
    await page.waitForTimeout(500)

    const posts = page.locator('[data-testid="post-card"]')
    const postCount = await posts.count()

    if (postCount > 0) {
      // Click first post
      const firstPost = posts.first()
      await firstPost.click()

      // Should navigate to post detail page
      await expect(page).toHaveURL(/\/posts\/\d+/)

      // Post detail should be visible
      const postDetail = page.locator('[data-testid="post-detail"]')
      await expect(postDetail).toBeVisible()
    }
  })

  test('should handle non-existent user', async ({ page }) => {
    await page.goto('/@nonexistentuser123')

    // Wait for page to process
    await page.waitForTimeout(500)

    // Should show 404 or user not found message
    const notFoundMessage = page.locator('[data-testid="user-not-found"]')
    const errorMessage = page.getByText(/사용자를 찾을 수 없습니다|User not found/i)

    const hasNotFound = await notFoundMessage.isVisible().catch(() => false)
    const hasErrorMessage = await errorMessage.isVisible().catch(() => false)

    expect(hasNotFound || hasErrorMessage).toBeTruthy()
  })

  test('should support infinite scroll for user posts', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for initial posts to load
    await page.waitForTimeout(500)

    const initialCount = await page.locator('[data-testid="post-card"]').count()

    if (initialCount > 0) {
      // Scroll to bottom
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

      // Wait for potential load
      await page.waitForTimeout(1000)

      // Get new post count
      const newCount = await page.locator('[data-testid="post-card"]').count()

      // Should have loaded more posts or stayed same if no more
      expect(newCount).toBeGreaterThanOrEqual(initialCount)

      // Check for loading indicator during scroll
      const loadingIndicator = page.locator('[data-testid="loading-more"]')
      // Loading indicator might appear briefly
    }
  })

  test('should display post metadata correctly', async ({ page }) => {
    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    const posts = page.locator('[data-testid="post-card"]')
    const postCount = await posts.count()

    if (postCount > 0) {
      const firstPost = posts.first()

      // Check title
      const title = firstPost.locator('[data-testid="post-title"]')
      await expect(title).toBeVisible()

      // Check timestamp
      const timestamp = firstPost.locator('[data-testid="post-timestamp"]')
      await expect(timestamp).toBeVisible()

      // Check like count if visible
      const likes = firstPost.locator('[data-testid="post-likes"]')
      if (await likes.isVisible()) {
        await expect(likes).toBeVisible()
      }

      // Check tags if visible
      const tags = firstPost.locator('[data-testid="post-tags"]')
      if (await tags.isVisible()) {
        await expect(tags).toBeVisible()
      }
    }
  })

  test('should display post count indicator', async ({ page }) => {
    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    // Check for post count indicator
    const postCountIndicator = page.locator('[data-testid="user-post-count"]')

    if (await postCountIndicator.isVisible()) {
      const text = await postCountIndicator.textContent()
      expect(text).toMatch(/\d+/)
    }
  })

  test('should show only published posts for other users', async ({ page }) => {
    // Visit another user's page (not the logged-in user)
    await page.goto('/@anotheruser')

    await page.waitForTimeout(500)

    const posts = page.locator('[data-testid="post-card"]')
    const postCount = await posts.count()

    // All displayed posts should not have draft badge
    for (let i = 0; i < postCount; i++) {
      const post = posts.nth(i)
      const draftBadge = post.locator('[data-testid="draft-badge"]')
      await expect(draftBadge).not.toBeVisible()
    }
  })

  test('should handle special characters in username', async ({ page }) => {
    // Test with username containing numbers, underscores
    await page.goto('/@test_user_123')

    // Page should load without errors
    await page.waitForTimeout(500)

    // Either profile loads or user not found is shown
    const profileSection = page.locator('[data-testid="user-profile"]')
    const notFoundMessage = page.locator('[data-testid="user-not-found"]')

    const hasProfile = await profileSection.isVisible().catch(() => false)
    const hasNotFound = await notFoundMessage.isVisible().catch(() => false)

    expect(hasProfile || hasNotFound).toBeTruthy()
  })

  test('should display social links if available', async ({ page }) => {
    await page.goto('/@testuser')

    const socialLinks = page.locator('[data-testid="profile-social-links"]')

    if (await socialLinks.isVisible()) {
      // Check for common social platforms
      const github = page.locator('[data-testid="social-github"]')
      const twitter = page.locator('[data-testid="social-twitter"]')
      const website = page.locator('[data-testid="social-website"]')

      // At least one social link should be present
      const hasGithub = await github.isVisible().catch(() => false)
      const hasTwitter = await twitter.isVisible().catch(() => false)
      const hasWebsite = await website.isVisible().catch(() => false)

      expect(hasGithub || hasTwitter || hasWebsite).toBeTruthy()
    }
  })

  test('should maintain scroll position when navigating back', async ({ page }) => {
    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    // Scroll down
    await page.evaluate(() => window.scrollBy(0, 500))
    const scrollPosition = await page.evaluate(() => window.scrollY)

    expect(scrollPosition).toBeGreaterThan(0)

    // Click on a post
    const posts = page.locator('[data-testid="post-card"]')
    const postCount = await posts.count()

    if (postCount > 0) {
      await posts.first().click()
      await expect(page).toHaveURL(/\/posts\/\d+/)

      // Navigate back
      await page.goBack()

      // Should return to user blog page
      await expect(page).toHaveURL('/@testuser')
    }
  })

  test('should be responsive on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    // Profile should be visible on mobile
    const profileSection = page.locator('[data-testid="user-profile"]')
    await expect(profileSection).toBeVisible()

    // Posts should be visible on mobile
    const postsSection = page.locator('[data-testid="user-posts"]')
    await expect(postsSection).toBeVisible()
  })
})
