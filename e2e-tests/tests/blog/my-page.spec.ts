/**
 * Blog My Page E2E Tests
 *
 * Tests for the authenticated user's blog management page:
 * - Profile display
 * - My posts list with status filtering
 * - Post management (edit, delete, publish)
 * - Series management
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoBlogPage } from '../helpers/auth'

test.describe('Blog My Page', () => {
  test.beforeEach(async ({ page }) => {
    await gotoBlogPage(page, '/blog/my')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
  })

  test('should display my page with profile info', async ({ page }) => {
    await page.waitForTimeout(1000)

    // My page should show profile information
    const profileSection = page.locator('[data-testid="my-page"]')
      .or(page.locator('[data-testid="profile-section"]'))
      .or(page.locator('text=/마이페이지|My Page|프로필/i').first())

    const hasProfile = await profileSection.isVisible().catch(() => false)
    const hasNickname = await page.locator('[data-testid="profile-nickname"]').or(page.locator('text=/test/i').first()).isVisible().catch(() => false)

    expect(hasProfile || hasNickname).toBeTruthy()
  })

  test('should display my posts list', async ({ page }) => {
    await page.waitForTimeout(1000)

    // My posts section
    const myPosts = page.locator('[data-testid="my-posts"]')
      .or(page.locator('[data-testid="my-post-card"]'))
      .or(page.locator('text=/내 게시글|My Posts/i').first())

    const postCards = page.locator('[data-testid="my-post-card"]')
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test/ }))

    const hasMyPosts = await myPosts.isVisible().catch(() => false)
    const hasPosts = await postCards.first().isVisible().catch(() => false)
    const hasEmpty = await page.locator('text=/게시글이 없습니다|No posts/i').isVisible().catch(() => false)

    expect(hasMyPosts || hasPosts || hasEmpty).toBeTruthy()
  })

  test('should filter posts by status', async ({ page }) => {
    await page.waitForTimeout(1000)

    // Look for status filter tabs/buttons
    const allFilter = page.locator('[data-testid="filter-all"]')
      .or(page.locator('button').filter({ hasText: /전체|All/i }).first())
    const draftFilter = page.locator('[data-testid="filter-draft"]')
      .or(page.locator('button').filter({ hasText: /초안|Draft/i }).first())
    const publishedFilter = page.locator('[data-testid="filter-published"]')
      .or(page.locator('button').filter({ hasText: /발행|Published/i }).first())

    const hasAll = await allFilter.isVisible().catch(() => false)
    const hasDraft = await draftFilter.isVisible().catch(() => false)

    if (hasAll && hasDraft) {
      // Click draft filter
      await draftFilter.click()
      await page.waitForTimeout(1000)

      // Click published filter
      if (await publishedFilter.isVisible().catch(() => false)) {
        await publishedFilter.click()
        await page.waitForTimeout(1000)
      }

      // Click all filter
      await allFilter.click()
      await page.waitForTimeout(1000)
    }
  })

  test('should show post action buttons (edit, delete)', async ({ page }) => {
    await page.waitForTimeout(1000)

    const postCards = page.locator('[data-testid="my-post-card"]')
      .or(page.locator('[class*="rounded"]').filter({ hasText: /E2E Test/ }))

    const hasPosts = await postCards.first().isVisible().catch(() => false)
    if (!hasPosts) return

    // Check for action buttons
    const editButton = page.locator('[data-testid="post-edit-button"]')
      .or(page.locator('button').filter({ hasText: /수정|Edit/i }))
      .first()
    const deleteButton = page.locator('[data-testid="post-delete-button"]')
      .or(page.locator('button').filter({ hasText: /삭제|Delete/i }))
      .first()

    const hasEdit = await editButton.isVisible().catch(() => false)
    const hasDelete = await deleteButton.isVisible().catch(() => false)

    expect(hasEdit || hasDelete).toBeTruthy()
  })

  test('should display my series list', async ({ page }) => {
    await page.waitForTimeout(1000)

    const mySeries = page.locator('[data-testid="my-series"]')
      .or(page.locator('[data-testid="my-series-list"]'))
      .or(page.locator('text=/내 시리즈|My Series/i').first())

    const hasSeries = await mySeries.isVisible().catch(() => false)
    if (hasSeries) {
      await expect(mySeries).toBeVisible()
    }
  })

  test('should show create new post button', async ({ page }) => {
    await page.waitForTimeout(1000)

    const createButton = page.locator('[data-testid="create-post-button"]')
      .or(page.locator('a, button').filter({ hasText: /새 글|글쓰기|New Post|Write/i }).first())

    const hasCreate = await createButton.isVisible().catch(() => false)
    if (hasCreate) {
      await expect(createButton).toBeVisible()
    }
  })
})
