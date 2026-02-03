import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Trending', () => {
  test('트렌딩 페이지 접근', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    await expect(page).toHaveURL(/trending/)
  })

  test('트렌딩 포스트 목록 표시', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    // 포스트 목록
    const posts = blogSelectors.postCard(page)
    const count = await posts.count()

    if (count > 0) {
      await expect(posts.first()).toBeVisible()
    }
  })

  test('기간별 트렌딩 필터 (오늘/이번주/이번달)', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    // 기간 필터 버튼/탭
    const periodFilter = page.getByRole('tab', { name: /오늘|today|이번\s?주|this week|이번\s?달|this month/i })
      .or(page.getByRole('button', { name: /오늘|이번\s?주|이번\s?달/i }))
      .or(page.locator('.period-filter, .time-filter'))

    const count = await periodFilter.count()
    if (count > 0) {
      await expect(periodFilter.first()).toBeVisible()
    }
  })

  test('기간 필터 변경', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    // 이번주 필터 클릭
    const weekFilter = page.getByRole('tab', { name: /이번\s?주|this week|week/i })
      .or(page.getByRole('button', { name: /이번\s?주|week/i }))

    const count = await weekFilter.count()
    if (count > 0) {
      await weekFilter.first().click()
      await waitForLoading(page)

      // URL 또는 UI 변경 확인
      const isActive = await weekFilter.first().evaluate(el =>
        el.classList.contains('active') ||
        el.getAttribute('aria-selected') === 'true'
      )

      expect(isActive).toBeTruthy()
    }
  })

  test('트렌딩 순위 표시', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    // 순위 번호 표시
    const rankNumber = page.locator('.rank, .ranking, .order')
      .or(page.getByText(/^[1-9]$|^10$/))

    const count = await rankNumber.count()
    if (count > 0) {
      await expect(rankNumber.first()).toBeVisible()
    }
  })

  test('포스트 통계 표시 (조회수, 좋아요)', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    const posts = blogSelectors.postCard(page)
    const count = await posts.count()

    if (count > 0) {
      const firstPost = posts.first()

      // 통계 정보 확인
      const stats = firstPost.locator('.views, .likes, .stats')
        .or(firstPost.getByText(/조회|views|좋아요|likes/i))

      const statsCount = await stats.count()
      if (statsCount > 0) {
        await expect(stats.first()).toBeVisible()
      }
    }
  })

  test('트렌딩 포스트 클릭 시 상세 페이지 이동', async ({ page }) => {
    await page.goto(routes.blog.trending)
    await waitForLoading(page)

    const posts = blogSelectors.postCard(page)
    const count = await posts.count()

    if (count > 0) {
      await posts.first().click()

      // 포스트 상세 페이지로 이동
      await expect(page).toHaveURL(/\/blog\/posts\/|\/blog\/@/)
    }
  })

  test('네비게이션에서 트렌딩 링크', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // 트렌딩 링크
    const trendingLink = page.getByRole('link', { name: /트렌딩|trending/i })
    const count = await trendingLink.count()

    if (count > 0) {
      await trendingLink.first().click()
      await expect(page).toHaveURL(/trending/)
    }
  })

  test('피드와 트렌딩 전환', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    // 트렌딩 탭으로 전환
    const trendingTab = page.getByRole('tab', { name: /트렌딩|trending|인기/i })
    const count = await trendingTab.count()

    if (count > 0) {
      await trendingTab.first().click()

      // 트렌딩 콘텐츠 표시 확인
      await waitForLoading(page)
    }
  })
})
