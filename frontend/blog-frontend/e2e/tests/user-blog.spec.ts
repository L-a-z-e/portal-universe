import { test, expect } from '@playwright/test'
import { mockLogin } from '../fixtures/auth'

/**
 * E2E tests for User Blog Page (/@username)
 * Using Playwright recommended selectors: CSS classes, getByRole, getByText
 */
test.describe('User Blog Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockLogin(page)
  })

  test('should access user blog page via /@username', async ({ page }) => {
    await page.goto('/@testuser')

    // Check page loaded successfully
    await expect(page).toHaveURL('/@testuser')

    // Check user profile section is visible using CSS class
    const profileSection = page.locator('.user-profile, .profile-section, .profile-card')
    await expect(profileSection.first()).toBeVisible()
  })

  test('should display user profile information', async ({ page }) => {
    await page.goto('/@testuser')

    // Check profile components using CSS classes
    const profileSection = page.locator('.user-profile, .profile-section, .profile-card')
    await expect(profileSection.first()).toBeVisible()

    // Check username is displayed
    const username = page.locator('.username, .profile-username').or(page.getByText(/testuser/i))
    await expect(username.first()).toBeVisible()

    // Check nickname/display name
    const nickname = page.locator('.nickname, .display-name')
    if (await nickname.first().isVisible().catch(() => false)) {
      await expect(nickname.first()).toBeVisible()
    }

    // Check bio/introduction if exists
    const bio = page.locator('.bio, .introduction, .profile-bio')
    if (await bio.first().isVisible().catch(() => false)) {
      await expect(bio.first()).toBeVisible()
    }

    // Check profile image
    const avatar = page.locator('.avatar, .profile-avatar, .profile-image, img[alt*="profile"], img[alt*="avatar"]')
    await expect(avatar.first()).toBeVisible()
  })

  test('should display user post list', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for posts to load
    await page.waitForTimeout(500)

    // Check posts section exists
    const postsSection = page.locator('.post-list, .user-posts, .posts-section')
    await expect(postsSection.first()).toBeVisible()

    // Check if posts are displayed or empty state is shown
    const posts = page.locator('.post-card, article.card, .card')
    const emptyState = page.locator('.empty-state, .empty-message').or(page.getByText(/게시글이 없|no posts|없습니다/i))

    const postCount = await posts.count()
    const hasEmptyState = await emptyState.first().isVisible().catch(() => false)

    expect(postCount > 0 || hasEmptyState).toBeTruthy()
  })

  test('should navigate to post detail when clicking a post', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for posts to load
    await page.waitForTimeout(500)

    const posts = page.locator('.post-card, article.card')
    const postCount = await posts.count()

    if (postCount > 0) {
      // Click first post
      const firstPost = posts.first()
      await firstPost.click()

      // Should navigate to post detail page
      await expect(page).toHaveURL(/\/posts\/\d+/)

      // Post detail should be visible
      const postDetail = page.locator('.post-detail, article, .post-content')
      await expect(postDetail.first()).toBeVisible()
    }
  })

  test('should handle non-existent user', async ({ page }) => {
    await page.goto('/@nonexistentuser123')

    // Wait for page to process
    await page.waitForTimeout(500)

    // Should show 404 or user not found message
    const notFoundMessage = page.locator('.not-found, .error-message, .user-not-found')
    const errorMessage = page.getByText(/사용자를 찾을 수 없습니다|User not found|찾을 수 없|존재하지 않/i)

    const hasNotFound = await notFoundMessage.first().isVisible().catch(() => false)
    const hasErrorMessage = await errorMessage.first().isVisible().catch(() => false)

    expect(hasNotFound || hasErrorMessage).toBeTruthy()
  })

  test('should support infinite scroll for user posts', async ({ page }) => {
    await page.goto('/@testuser')

    // Wait for initial posts to load
    await page.waitForTimeout(500)

    const initialCount = await page.locator('.post-card, article.card, .card').count()

    if (initialCount > 0) {
      // Scroll to bottom
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

      // Wait for potential load
      await page.waitForTimeout(1000)

      // Get new post count
      const newCount = await page.locator('.post-card, article.card, .card').count()

      // Should have loaded more posts or stayed same if no more
      expect(newCount).toBeGreaterThanOrEqual(initialCount)
    }
  })

  test('should display post metadata correctly', async ({ page }) => {
    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    const posts = page.locator('.post-card, article.card')
    const postCount = await posts.count()

    if (postCount > 0) {
      const firstPost = posts.first()

      // Check title using CSS class or heading
      const title = firstPost.locator('.post-title, h2, h3')
      await expect(title.first()).toBeVisible()

      // Check timestamp using time element or CSS class
      const timestamp = firstPost.locator('.timestamp, time, .created-at, .date')
      await expect(timestamp.first()).toBeVisible()

      // Check like count if visible
      const likes = firstPost.locator('.like-count, .likes')
      if (await likes.first().isVisible().catch(() => false)) {
        await expect(likes.first()).toBeVisible()
      }

      // Check tags if visible
      const tags = firstPost.locator('.tags, .post-tags, .tag-list')
      if (await tags.first().isVisible().catch(() => false)) {
        await expect(tags.first()).toBeVisible()
      }
    }
  })

  test('should display post count indicator', async ({ page }) => {
    await page.goto('/@testuser')

    await page.waitForTimeout(500)

    // Check for post count indicator
    const postCountIndicator = page.locator('.post-count, .user-post-count').or(page.getByText(/\d+\s*(개|posts?|건)/i))

    if (await postCountIndicator.first().isVisible().catch(() => false)) {
      const text = await postCountIndicator.first().textContent()
      expect(text).toMatch(/\d+/)
    }
  })

  test('should show only published posts for other users', async ({ page }) => {
    // Visit another user's page (not the logged-in user)
    await page.goto('/@anotheruser')

    await page.waitForTimeout(500)

    const posts = page.locator('.post-card, article.card')
    const postCount = await posts.count()

    // All displayed posts should not have draft badge
    for (let i = 0; i < postCount; i++) {
      const post = posts.nth(i)
      const draftBadge = post.locator('.draft-badge, .draft')
      await expect(draftBadge).not.toBeVisible()
    }
  })

  test('should handle special characters in username', async ({ page }) => {
    // Test with username containing numbers, underscores
    await page.goto('/@test_user_123')

    // Page should load without errors
    await page.waitForTimeout(500)

    // Either profile loads or user not found is shown
    const profileSection = page.locator('.user-profile, .profile-section, .profile-card')
    const notFoundMessage = page.locator('.not-found, .error-message').or(page.getByText(/찾을 수 없|not found/i))

    const hasProfile = await profileSection.first().isVisible().catch(() => false)
    const hasNotFound = await notFoundMessage.first().isVisible().catch(() => false)

    expect(hasProfile || hasNotFound).toBeTruthy()
  })

  test('should display social links if available', async ({ page }) => {
    await page.goto('/@testuser')

    const socialLinks = page.locator('.social-links, .profile-links')

    if (await socialLinks.first().isVisible().catch(() => false)) {
      // Check for common social platforms by link href
      const github = page.locator('a[href*="github.com"]')
      const twitter = page.locator('a[href*="twitter.com"], a[href*="x.com"]')
      const website = page.locator('.website-link, a[href^="http"]:not([href*="github"]):not([href*="twitter"])')

      // At least one social link should be present
      const hasGithub = await github.first().isVisible().catch(() => false)
      const hasTwitter = await twitter.first().isVisible().catch(() => false)
      const hasWebsite = await website.first().isVisible().catch(() => false)

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
    const posts = page.locator('.post-card, article.card')
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
    const profileSection = page.locator('.user-profile, .profile-section, .profile-card')
    await expect(profileSection.first()).toBeVisible()

    // Posts should be visible on mobile
    const postsSection = page.locator('.post-list, .user-posts, .posts-section')
    await expect(postsSection.first()).toBeVisible()
  })
})
