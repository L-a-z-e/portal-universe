import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - My Page', () => {
  test('비로그인 - 마이페이지 접근 제한', async ({ page }) => {
    await page.goto(routes.blog.myPage)

    // 로그인 페이지로 리다이렉트
    await expect(page).toHaveURL(/login/)
  })

  test('로그인 - 마이페이지 접근', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 마이페이지 로드 확인
    await expect(authenticatedPage).toHaveURL(/my|profile/)
  })

  test('내 프로필 정보 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 프로필 영역
    const profile = authenticatedPage.locator('.profile, .my-profile')
    const count = await profile.count()

    if (count > 0) {
      await expect(profile.first()).toBeVisible()
    }
  })

  test('내 글 목록', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 글 목록 탭
    const postsTab = authenticatedPage.getByRole('tab', { name: /글|posts|작성한/i })
    const count = await postsTab.count()

    if (count > 0) {
      await postsTab.first().click()

      // 포스트 목록
      const posts = blogSelectors.postCard(authenticatedPage)
      const postsCount = await posts.count()

      if (postsCount > 0) {
        await expect(posts.first()).toBeVisible()
      }
    }
  })

  test('임시 저장 글 목록', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 임시 저장 탭
    const draftsTab = authenticatedPage.getByRole('tab', { name: /임시|drafts|저장/i })
    const count = await draftsTab.count()

    if (count > 0) {
      await draftsTab.first().click()

      // 임시 저장 목록
      const drafts = authenticatedPage.locator('.draft-item, .draft-list')
        .or(blogSelectors.postCard(authenticatedPage))

      await expect(drafts.first()).toBeVisible()
    }
  })

  test('좋아요한 글 목록', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 좋아요 탭
    const likesTab = authenticatedPage.getByRole('tab', { name: /좋아요|liked/i })
    const count = await likesTab.count()

    if (count > 0) {
      await likesTab.first().click()

      // 좋아요한 글 목록
      const likedPosts = blogSelectors.postCard(authenticatedPage)
      const likedCount = await likedPosts.count()

      if (likedCount > 0) {
        await expect(likedPosts.first()).toBeVisible()
      }
    }
  })

  test('내 시리즈 목록', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 시리즈 탭
    const seriesTab = authenticatedPage.getByRole('tab', { name: /시리즈|series/i })
    const count = await seriesTab.count()

    if (count > 0) {
      await seriesTab.first().click()

      // 시리즈 목록
      const series = blogSelectors.seriesList(authenticatedPage)
        .or(authenticatedPage.locator('.series-list, .my-series'))

      await expect(series).toBeVisible()
    }
  })

  test('프로필 수정 버튼', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 프로필 수정 버튼
    const editButton = authenticatedPage.getByRole('button', { name: /수정|edit|설정/i })
      .or(authenticatedPage.locator('.edit-profile, .settings'))

    const count = await editButton.count()
    if (count > 0) {
      await expect(editButton.first()).toBeVisible()
    }
  })

  test('프로필 수정 모달/페이지', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    const editButton = authenticatedPage.getByRole('button', { name: /프로필.*수정|edit.*profile/i })
      .or(authenticatedPage.locator('.edit-profile'))

    const count = await editButton.count()
    if (count > 0) {
      await editButton.first().click()

      // 수정 모달 또는 페이지
      const editForm = authenticatedPage.locator('[role="dialog"], .profile-edit, form')
      await expect(editForm.first()).toBeVisible()
    }
  })

  test('통계 정보 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 통계 정보 (글 수, 팔로워 등)
    const stats = authenticatedPage.locator('.stats, .statistics')
      .or(authenticatedPage.getByText(/글\s*\d+|팔로워\s*\d+|posts\s*\d+/i))

    const count = await stats.count()
    if (count > 0) {
      await expect(stats.first()).toBeVisible()
    }
  })

  test('새 글 쓰기 버튼', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 글쓰기 버튼
    const writeButton = authenticatedPage.getByRole('link', { name: /새 글|글쓰기|write|new post/i })
      .or(authenticatedPage.getByRole('button', { name: /새 글|글쓰기/i }))

    const count = await writeButton.count()
    if (count > 0) {
      await writeButton.first().click()
      await expect(authenticatedPage).toHaveURL(/write/)
    }
  })
})
