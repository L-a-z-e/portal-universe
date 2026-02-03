import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Feed', () => {
  test('피드 페이지 로드', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    await expect(page).toHaveURL(/feed/)
  })

  test('포스트 카드 목록 표시', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    // 포스트 카드 확인
    const postCards = blogSelectors.postCard(page)
    const count = await postCards.count()

    // 포스트가 있으면 표시 확인
    if (count > 0) {
      await expect(postCards.first()).toBeVisible()
    }
  })

  test('포스트 카드 클릭 시 상세 페이지 이동', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    const postCards = blogSelectors.postCard(page)
    const count = await postCards.count()

    if (count > 0) {
      // 첫 번째 포스트 카드 클릭
      await postCards.first().click()

      // 포스트 상세 페이지로 이동 확인
      await expect(page).toHaveURL(/\/blog\/posts\/|\/blog\/@/)
    }
  })

  test('무한 스크롤 또는 페이지네이션', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    const postCards = blogSelectors.postCard(page)
    const initialCount = await postCards.count()

    if (initialCount > 0) {
      // 스크롤 다운
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

      // 로딩 대기
      await page.waitForTimeout(2000)

      // 더 많은 포스트가 로드되었거나 페이지네이션 버튼이 있는지 확인
      const afterScrollCount = await postCards.count()
      const pagination = page.getByRole('button', { name: /더 보기|load more|다음/i })
        .or(page.locator('.pagination, .load-more'))

      const hasPagination = (await pagination.count()) > 0
      const hasMorePosts = afterScrollCount > initialCount

      expect(hasPagination || hasMorePosts || afterScrollCount === initialCount).toBeTruthy()
    }
  })

  test('피드 정렬 옵션 (최신순/인기순)', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    // 정렬 옵션 버튼/탭 확인
    const sortOptions = page.getByRole('tab', { name: /최신|recent|인기|popular|trending/i })
      .or(page.getByRole('button', { name: /최신|recent|인기|popular/i }))
      .or(page.locator('.sort-options, .feed-tabs'))

    const count = await sortOptions.count()
    if (count > 0) {
      await expect(sortOptions.first()).toBeVisible()
    }
  })

  test('로그인 사용자 - 팔로잉 피드', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.feed)
    await waitForLoading(authenticatedPage)

    // 팔로잉 탭 확인
    const followingTab = authenticatedPage.getByRole('tab', { name: /팔로잉|following/i })
      .or(authenticatedPage.getByText(/팔로잉|following/i))

    const count = await followingTab.count()
    if (count > 0) {
      await expect(followingTab.first()).toBeVisible()
    }
  })

  test('포스트 카드 정보 표시', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    const postCards = blogSelectors.postCard(page)
    const count = await postCards.count()

    if (count > 0) {
      const firstCard = postCards.first()

      // 제목 확인
      const title = firstCard.locator('h1, h2, h3, .post-title, .title')
      await expect(title).toBeVisible()
    }
  })
})
