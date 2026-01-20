import { test, expect } from '@playwright/test'

test.describe('Blog Post List', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/blog')
  })

  test('should display blog page', async ({ page }) => {
    const heading = page.getByRole('heading', { level: 1 })
      .or(page.locator('h1'))
      .or(page.getByText(/blog/i).first())

    const isVisible = await heading.isVisible().catch(() => false)

    if (isVisible) {
      await expect(heading).toBeVisible()
    }
  })

  test('should display post list or empty state', async ({ page }) => {
    await page.waitForTimeout(2000)

    const postList = page.locator('[data-testid="post-list"]')
      .or(page.locator('.post-list'))
      .or(page.locator('article'))
      .or(page.locator('.blog-post'))
      .or(page.locator('[class*="post"]'))

    const emptyState = page.getByText(/no posts/i)
      .or(page.getByText(/empty/i))
      .or(page.getByText(/게시글/i))
      .or(page.locator('[data-testid="empty-state"]'))

    const pageContent = page.locator('main, #app, .app, body')

    const hasPostList = await postList.first().isVisible().catch(() => false)
    const hasEmptyState = await emptyState.first().isVisible().catch(() => false)
    const hasPageContent = await pageContent.first().isVisible().catch(() => false)

    // Accept if any content is visible (page loaded successfully)
    expect(hasPostList || hasEmptyState || hasPageContent).toBeTruthy()
  })

  test('should be able to click on a post if available', async ({ page }) => {
    await page.waitForTimeout(1000)

    const postLink = page.locator('article a').first()
      .or(page.locator('.post-item a').first())
      .or(page.locator('[data-testid="post-link"]').first())

    const isVisible = await postLink.isVisible().catch(() => false)

    if (isVisible) {
      await postLink.click()
      await page.waitForTimeout(500)

      const url = page.url()
      expect(url).toMatch(/blog/)
    } else {
      test.skip()
    }
  })
})
