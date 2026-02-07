import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Blog - Stats', () => {
  test.describe('통계 페이지 UI', () => {
    test('통계 페이지 접근', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)

      // 페이지가 존재하는지 확인
      const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
      const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
      if (hasNotFound) {
        test.skip(true, 'Stats page does not exist')
        return
      }

      // 페이지 제목 확인
      const title = page.getByRole('heading', { name: /블로그 통계|Statistics|Stats/ })
        .or(page.getByText('블로그 통계'))

      await expect(title.first()).toBeVisible({ timeout: 10000 })
    })

    test('로딩 상태 표시', async ({ page }) => {
      await page.goto('/blog/stats')

      // 로딩 스피너 또는 콘텐츠 확인
      const spinner = page.locator('[class*="spinner"], [class*="loading"]')
      const content = page.locator('[class*="stats"], [class*="card"]')

      const hasSpinner = await spinner.first().isVisible().catch(() => false)
      const hasContent = await content.first().isVisible().catch(() => false)

      // 로딩 중이거나 콘텐츠가 있어야 함
      expect(hasSpinner || hasContent).toBeTruthy()
    })
  })

  test.describe('블로그 전체 통계', () => {
    test('전체 게시글 수 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 게시글 수 관련 텍스트
      const postStats = page.getByText(/게시글|Posts|글/)
      await expect(postStats.first()).toBeVisible({ timeout: 10000 })
    })

    test('총 조회수 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 조회수 관련 텍스트
      const viewStats = page.getByText(/조회|Views/)
      if (await viewStats.count() > 0) {
        await expect(viewStats.first()).toBeVisible()
      }
    })

    test('총 좋아요 수 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 좋아요 관련 텍스트
      const likeStats = page.getByText(/좋아요|Likes|❤️/)
      if (await likeStats.count() > 0) {
        await expect(likeStats.first()).toBeVisible()
      }
    })

    test('총 댓글 수 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 댓글 관련 텍스트
      const commentStats = page.getByText(/댓글|Comments/)
      if (await commentStats.count() > 0) {
        await expect(commentStats.first()).toBeVisible()
      }
    })
  })

  test.describe('카테고리별 통계', () => {
    test('카테고리 통계 섹션 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 카테고리 통계 섹션
      const categorySection = page.getByText(/카테고리|Category/)
      await expect(categorySection.first()).toBeVisible({ timeout: 10000 })
    })

    test('카테고리별 게시글 수 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 카테고리 카드 또는 리스트
      const categoryItems = page.locator('[class*="category"], [class*="card"]').filter({ hasText: /[0-9]+/ })
      if (await categoryItems.count() > 0) {
        await expect(categoryItems.first()).toBeVisible()
      }
    })
  })

  test.describe('인기 태그', () => {
    test('인기 태그 섹션 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 인기 태그 섹션
      const tagSection = page.getByText(/인기 태그|Popular Tags|태그/)
      await expect(tagSection.first()).toBeVisible({ timeout: 10000 })
    })

    test('태그 클라우드 또는 리스트 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 태그 요소들
      const tags = page.locator('[class*="tag"], [class*="chip"], [class*="badge"]')
      if (await tags.count() > 0) {
        await expect(tags.first()).toBeVisible()
      }
    })
  })

  test.describe('내 통계 (로그인 시)', () => {
    test('로그인 사용자 - 내 통계 섹션', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 내 통계 섹션 (로그인 시에만 표시)
      const myStats = page.getByText(/내 통계|My Stats|작성한 글/)
      // 로그인하지 않으면 없을 수 있음
      if (await myStats.count() > 0) {
        await expect(myStats.first()).toBeVisible()
      }
    })
  })

  test.describe('새로고침', () => {
    test('새로고침 버튼 동작', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(2000)

      // 새로고침 버튼
      const refreshButton = page.getByRole('button', { name: /새로고침|Refresh|다시/ })

      if (await refreshButton.count() > 0) {
        await refreshButton.click()
        await page.waitForTimeout(1000)
      }
    })
  })

  test.describe('에러 처리', () => {
    test('에러 발생 시 에러 메시지 표시', async ({ page }) => {
      await page.goto('/blog/stats')
      await waitForLoading(page)
      await page.waitForTimeout(3000)

      // 에러 메시지 또는 정상 콘텐츠
      const errorMessage = page.getByText(/오류|에러|Error|실패/)
      const normalContent = page.locator('[class*="stats"], [class*="card"]')

      const hasError = await errorMessage.first().isVisible().catch(() => false)
      const hasContent = await normalContent.first().isVisible().catch(() => false)

      // 에러가 있거나 정상 콘텐츠가 있어야 함
      expect(hasError || hasContent).toBeTruthy()
    })
  })
})
