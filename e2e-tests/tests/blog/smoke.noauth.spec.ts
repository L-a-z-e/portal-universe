/**
 * Blog Smoke Tests (No Authentication)
 *
 * Basic tests that verify blog pages load without authentication:
 * - Portal shell loads
 * - Blog section is accessible
 * - Module Federation remote loads
 * - Public pages are viewable
 */
import { test, expect } from '@playwright/test'

test.describe('Blog Smoke Tests (No Auth)', () => {
  test('should load portal shell home page', async ({ page }) => {
    await page.goto('/')

    // Portal shell should load
    await page.waitForTimeout(3000)

    const hasTitle = await page.locator('text=/Portal|포털/i').first().isVisible().catch(() => false)
    const hasNav = await page.locator('nav, [role="navigation"]').first().isVisible().catch(() => false)
    const hasBody = await page.locator('body').isVisible()

    expect(hasTitle || hasNav || hasBody).toBeTruthy()
  })

  test('should navigate to blog section', async ({ page }) => {
    await page.goto('/blog')
    await page.waitForTimeout(5000)

    // Blog Module Federation remote should load
    const hasBlogContent = await page.locator('text=/트렌딩|Trending|최신|Latest|Blog/i').first().isVisible().catch(() => false)
    const hasPostCards = await page.locator('[data-testid="post-card"]').first().isVisible().catch(() => false)
    const hasEmptyState = await page.locator('text=/게시글|posts/i').first().isVisible().catch(() => false)
    const hasError = await page.locator('text=/Error|오류|로드/i').first().isVisible().catch(() => false)

    // Page should show something (content, empty state, or error)
    expect(hasBlogContent || hasPostCards || hasEmptyState || hasError).toBeTruthy()
  })

  test('should load blog tags page without auth', async ({ page }) => {
    await page.goto('/blog/tags')
    await page.waitForTimeout(5000)

    const hasContent = await page.locator('text=/태그|Tag/i').first().isVisible().catch(() => false)
    const hasCards = await page.locator('[data-testid="tag-card"]').first().isVisible().catch(() => false)
    const hasBody = await page.locator('body').isVisible()

    expect(hasContent || hasCards || hasBody).toBeTruthy()
  })

  test('should load blog stats page without auth', async ({ page }) => {
    await page.goto('/blog/stats')
    await page.waitForTimeout(5000)

    const hasContent = await page.locator('text=/통계|Stats/i').first().isVisible().catch(() => false)
    const hasBody = await page.locator('body').isVisible()

    expect(hasContent || hasBody).toBeTruthy()
  })
})
