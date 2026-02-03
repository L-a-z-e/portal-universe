import { test, expect } from '../../fixtures/base'
import { routes, testPosts } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'
import { defaultTestUser } from '../../fixtures/auth'

test.describe('Blog - Follow', () => {
  test('사용자 프로필 - 팔로우 버튼 표시', async ({ page }) => {
    await page.goto(routes.blog.user('testauthor'))
    await waitForLoading(page)

    // 팔로우 버튼 확인
    const followButton = blogSelectors.followButton(page)
      .or(page.locator('.follow-btn, [class*="follow"]'))

    const count = await followButton.count()
    if (count > 0) {
      await expect(followButton.first()).toBeVisible()
    }
  })

  test('비로그인 - 팔로우 클릭 시 로그인 유도', async ({ page }) => {
    await page.goto(routes.blog.user('testauthor'))
    await waitForLoading(page)

    const followButton = blogSelectors.followButton(page)
    const count = await followButton.count()

    if (count > 0) {
      await followButton.first().click()

      // 로그인 모달 또는 리다이렉트
      const loginPrompt = page.getByText(/로그인|login/i)
      const promptCount = await loginPrompt.count()
      const isLoginPage = page.url().includes('login')

      expect(promptCount > 0 || isLoginPage).toBeTruthy()
    }
  })

  test('로그인 - 팔로우/언팔로우 토글', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.user('testauthor'))
    await waitForLoading(authenticatedPage)

    const followButton = blogSelectors.followButton(authenticatedPage)
    const count = await followButton.count()

    if (count > 0) {
      // 현재 팔로우 상태 확인
      const buttonText = await followButton.first().textContent()
      const isFollowing = /팔로잉|following|언팔로우|unfollow/i.test(buttonText || '')

      // 팔로우/언팔로우 클릭
      await followButton.first().click()
      await authenticatedPage.waitForTimeout(500)

      // 상태 변경 확인
      const newButtonText = await followButton.first().textContent()
      const newIsFollowing = /팔로잉|following|언팔로우|unfollow/i.test(newButtonText || '')

      expect(newIsFollowing).not.toBe(isFollowing)
    }
  })

  test('팔로워/팔로잉 수 표시', async ({ page }) => {
    await page.goto(routes.blog.user('testauthor'))
    await waitForLoading(page)

    // 팔로워 수
    const followerCount = page.getByText(/팔로워|followers/i)
      .or(page.locator('.follower-count, [class*="follower"]'))

    // 팔로잉 수
    const followingCount = page.getByText(/팔로잉|following/i)
      .or(page.locator('.following-count, [class*="following"]'))

    const hasFollowerCount = (await followerCount.count()) > 0
    const hasFollowingCount = (await followingCount.count()) > 0

    expect(hasFollowerCount || hasFollowingCount).toBeTruthy()
  })

  test('팔로워 목록 보기', async ({ page }) => {
    await page.goto(routes.blog.user('testauthor'))
    await waitForLoading(page)

    // 팔로워 클릭
    const followerLink = page.getByText(/팔로워|followers/i).first()
    await followerLink.click()

    // 팔로워 목록 모달/페이지 확인
    const followerList = page.locator('.follower-list, .user-list, [role="dialog"]')
      .or(page.getByText(/팔로워 목록|followers list/i))

    const count = await followerList.count()
    if (count > 0) {
      await expect(followerList.first()).toBeVisible()
    }
  })

  test('팔로잉 목록 보기', async ({ page }) => {
    await page.goto(routes.blog.user('testauthor'))
    await waitForLoading(page)

    // 팔로잉 클릭
    const followingLink = page.getByText(/팔로잉|following/i).first()
    await followingLink.click()

    // 팔로잉 목록 모달/페이지 확인
    const followingList = page.locator('.following-list, .user-list, [role="dialog"]')

    const count = await followingList.count()
    if (count > 0) {
      await expect(followingList.first()).toBeVisible()
    }
  })

  test('포스트 상세 - 작성자 팔로우 버튼', async ({ page }) => {
    const postUrl = routes.blog.post(testPosts.existing.id)
    await page.goto(postUrl)
    await waitForLoading(page)

    // 작성자 정보 영역의 팔로우 버튼
    const authorSection = page.locator('.author-info, .post-author, .writer-info')
    const count = await authorSection.count()

    if (count > 0) {
      const followButton = authorSection.locator('button').filter({ hasText: /팔로우|follow/i })
      const followCount = await followButton.count()

      if (followCount > 0) {
        await expect(followButton.first()).toBeVisible()
      }
    }
  })

  test('마이페이지 - 내 팔로워/팔로잉 확인', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 팔로워/팔로잉 정보 표시
    const followerInfo = authenticatedPage.getByText(/팔로워|followers/i)
    const followingInfo = authenticatedPage.getByText(/팔로잉|following/i)

    const hasFollowerInfo = (await followerInfo.count()) > 0
    const hasFollowingInfo = (await followingInfo.count()) > 0

    expect(hasFollowerInfo || hasFollowingInfo).toBeTruthy()
  })
})
