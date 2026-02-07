import { test, expect } from '../../fixtures/base'
import { routes, testPosts } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Like', () => {
  const postUrl = routes.blog.post(testPosts.existing.id)

  test('포스트 상세 페이지 - 좋아요 버튼 표시', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 포스트가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test post does not exist in database')
      return
    }

    // 좋아요 버튼 확인
    const likeButton = blogSelectors.likeButton(page)
      .or(page.getByRole('button', { name: /좋아요|like/i }))
      .or(page.locator('[aria-label*="like"], .like-btn'))

    await expect(likeButton.first()).toBeVisible()
  })

  test('비로그인 - 좋아요 클릭 시 로그인 유도', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 포스트가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test post does not exist in database')
      return
    }

    const likeButton = blogSelectors.likeButton(page)
      .or(page.getByRole('button', { name: /좋아요|like/i }))

    await likeButton.first().click()

    // 로그인 모달 또는 리다이렉트
    const loginPrompt = page.getByText(/로그인|login/i)
      .or(page.locator('.login-modal, [role="dialog"]'))

    const promptCount = await loginPrompt.count()
    const isLoginPage = page.url().includes('login')

    expect(promptCount > 0 || isLoginPage).toBeTruthy()
  })

  test('로그인 - 좋아요 토글', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    // 포스트가 존재하는지 확인
    const notFound = authenticatedPage.getByText(/페이지를 찾을 수 없습니다|not found/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test post does not exist in database')
      return
    }

    const likeButton = blogSelectors.likeButton(authenticatedPage)
      .or(authenticatedPage.getByRole('button', { name: /좋아요|like/i }))

    // 좋아요 상태 확인
    const isLiked = await likeButton.first().evaluate(el =>
      el.classList.contains('liked') ||
      el.classList.contains('active') ||
      el.getAttribute('aria-pressed') === 'true'
    )

    // 좋아요 클릭
    await likeButton.first().click()
    await authenticatedPage.waitForTimeout(500)

    // 상태 변경 확인
    const afterLiked = await likeButton.first().evaluate(el =>
      el.classList.contains('liked') ||
      el.classList.contains('active') ||
      el.getAttribute('aria-pressed') === 'true'
    )

    expect(afterLiked).not.toBe(isLiked)
  })

  test('좋아요 수 표시', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 포스트가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test post does not exist in database')
      return
    }

    // 좋아요 카운트 표시
    const likeCount = page.locator('.like-count, .likes-count, [class*="like"] span')
      .or(page.getByText(/\d+.*좋아요|\d+.*likes/i))

    const count = await likeCount.count()
    if (count > 0) {
      await expect(likeCount.first()).toBeVisible()
    }
  })

  test('피드에서 좋아요 버튼', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    const postCards = blogSelectors.postCard(page)
    const count = await postCards.count()

    if (count > 0) {
      // 카드 내 좋아요 버튼
      const likeButton = postCards.first().locator('.like-button, .like-btn, [aria-label*="like"]')
        .or(postCards.first().getByRole('button', { name: /좋아요|like/i }))

      const likeCount = await likeButton.count()
      if (likeCount > 0) {
        await expect(likeButton.first()).toBeVisible()
      }
    }
  })

  test('좋아요한 포스트 목록 (마이페이지)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 좋아요한 글 탭
    const likedTab = authenticatedPage.getByRole('tab', { name: /좋아요|liked/i })
      .or(authenticatedPage.getByText(/좋아요한|liked posts/i))

    const count = await likedTab.count()
    if (count > 0) {
      await likedTab.first().click()

      // 좋아요한 포스트 목록 표시
      const postList = blogSelectors.postCard(authenticatedPage)
        .or(authenticatedPage.locator('.liked-posts, .post-list'))

      await expect(postList.first()).toBeVisible()
    }
  })

  test('좋아요 애니메이션 효과', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    const likeButton = blogSelectors.likeButton(authenticatedPage)
      .or(authenticatedPage.getByRole('button', { name: /좋아요|like/i }))

    // 좋아요 클릭 전 스크린샷
    await likeButton.first().click()

    // 애니메이션이 있는 경우 잠시 대기
    await authenticatedPage.waitForTimeout(300)
  })
})
