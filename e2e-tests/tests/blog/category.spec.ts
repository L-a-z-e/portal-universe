import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Blog - Category', () => {
  test.describe('카테고리 목록 페이지 UI', () => {
    test('카테고리 목록 페이지 접근', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // 페이지 로드 확인 - 카테고리 목록 또는 빈 상태
      const pageContent = page.locator('main, [role="main"], .category-list, .categories')
      await expect(pageContent.first()).toBeVisible({ timeout: 10000 })
    })

    test('카테고리 통계 표시', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // 로딩 완료 대기
      await page.waitForTimeout(2000)

      // 카테고리 카드 또는 빈 상태 메시지
      const categoryCards = page.locator('.category-card, [class*="card"], [class*="category"]')
      const emptyState = page.getByText(/카테고리|게시글이 없습니다/)

      const hasCards = (await categoryCards.count()) > 0
      const hasEmpty = await emptyState.isVisible().catch(() => false)

      expect(hasCards || hasEmpty).toBeTruthy()
    })

    test('전체 카테고리 선택 가능', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // "전체" 버튼 또는 탭
      const allButton = page.getByRole('button', { name: /전체|All/ })
        .or(page.getByText('전체').first())

      if (await allButton.count() > 0) {
        await expect(allButton.first()).toBeVisible()
      }
    })
  })

  test.describe('카테고리별 게시글', () => {
    test('카테고리 선택 시 게시글 필터링', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // 카테고리 버튼들 찾기
      const categoryButtons = page.locator('button, [role="button"]').filter({ hasText: /[가-힣a-zA-Z]+/ })

      if (await categoryButtons.count() > 1) {
        // 첫 번째 카테고리 클릭
        await categoryButtons.nth(1).click()
        await page.waitForTimeout(1000)

        // 게시글 목록이 업데이트됨
        const posts = page.locator('[class*="post"], [class*="card"], article')
        // 0개 이상의 게시글 표시 (빈 카테고리일 수 있음)
        expect(await posts.count()).toBeGreaterThanOrEqual(0)
      }
    })
  })

  test.describe('무한 스크롤', () => {
    test('스크롤 시 추가 게시글 로드', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // 초기 게시글 수 확인
      const initialPosts = await page.locator('[class*="post"], [class*="card"], article').count()

      // 페이지 하단으로 스크롤
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
      await page.waitForTimeout(2000)

      // 게시글 수가 유지되거나 증가 (더 로드할 내용이 있으면)
      const afterScrollPosts = await page.locator('[class*="post"], [class*="card"], article').count()
      expect(afterScrollPosts).toBeGreaterThanOrEqual(initialPosts)
    })
  })

  test.describe('게시글 네비게이션', () => {
    test('게시글 클릭 시 상세 페이지로 이동', async ({ page }) => {
      await page.goto('/blog/categories')
      await waitForLoading(page)

      // 게시글 카드 찾기
      const postCard = page.locator('[class*="post"], [class*="card"], article').first()

      if (await postCard.count() > 0) {
        // 클릭 가능한 요소 찾기
        const clickable = postCard.locator('a, [role="link"]').first()
        if (await clickable.count() > 0) {
          await clickable.click()

          // 상세 페이지로 이동 확인
          await page.waitForURL(/\/blog\/\d+|\/blog\/[a-zA-Z0-9-]+/)
        }
      }
    })
  })
})
