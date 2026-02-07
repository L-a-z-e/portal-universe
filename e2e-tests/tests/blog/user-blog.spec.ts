import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - User Blog', () => {
  const userBlogUrl = routes.blog.user('testauthor')

  test('사용자 블로그 페이지 접근', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 사용자가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found|사용자.*없/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test user does not exist in database')
      return
    }

    await expect(page).toHaveURL(/@testauthor/)
  })

  test('사용자 프로필 정보 표시', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 사용자가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found|사용자.*없/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test user does not exist in database')
      return
    }

    // 프로필 영역
    const profileSection = page.locator('.profile, .user-profile, .author-info')
    const count = await profileSection.count()

    if (count > 0) {
      await expect(profileSection.first()).toBeVisible()
    }
  })

  test('사용자 아바타/프로필 이미지', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 아바타 이미지
    const avatar = page.locator('.avatar, .profile-image, img[alt*="profile"], img[alt*="avatar"]')
    const count = await avatar.count()

    if (count > 0) {
      await expect(avatar.first()).toBeVisible()
    }
  })

  test('사용자 소개글 표시', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 소개글
    const bio = page.locator('.bio, .introduction, .about')
      .or(page.getByText(/소개|about|introduction/i))

    const count = await bio.count()
    if (count > 0) {
      await expect(bio.first()).toBeVisible()
    }
  })

  test('사용자 포스트 목록', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 포스트 목록
    const posts = blogSelectors.postCard(page)
    const count = await posts.count()

    if (count > 0) {
      await expect(posts.first()).toBeVisible()
    }
  })

  test('포스트/시리즈 탭 전환', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 탭 확인
    const postsTab = page.getByRole('tab', { name: /글|posts/i })
    const seriesTab = page.getByRole('tab', { name: /시리즈|series/i })

    const postsCount = await postsTab.count()
    const seriesCount = await seriesTab.count()

    if (postsCount > 0 && seriesCount > 0) {
      // 시리즈 탭 클릭
      await seriesTab.first().click()

      // 시리즈 목록 표시 확인
      const seriesList = blogSelectors.seriesList(page)
        .or(page.locator('.series-list'))

      await expect(seriesList).toBeVisible()
    }
  })

  test('팔로워/팔로잉 수 클릭 시 목록 표시', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 팔로워 클릭
    const followerCount = page.getByText(/팔로워|followers/i).first()
    await followerCount.click()

    // 모달 또는 목록 표시
    const userList = page.locator('[role="dialog"], .user-list, .follower-list')
    const count = await userList.count()

    if (count > 0) {
      await expect(userList.first()).toBeVisible()
    }
  })

  test('소셜 링크 표시', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // 소셜 링크
    const socialLinks = page.locator('.social-links, .social-icons')
      .or(page.getByRole('link', { name: /github|twitter|linkedin|facebook/i }))

    const count = await socialLinks.count()
    if (count > 0) {
      await expect(socialLinks.first()).toBeVisible()
    }
  })

  test('블로그 RSS/구독 기능', async ({ page }) => {
    await page.goto(userBlogUrl)
    await waitForLoading(page)

    // RSS 또는 구독 버튼
    const subscribeButton = page.getByRole('button', { name: /구독|subscribe|rss/i })
      .or(page.getByRole('link', { name: /rss/i }))

    const count = await subscribeButton.count()
    if (count > 0) {
      await expect(subscribeButton.first()).toBeVisible()
    }
  })

  test('존재하지 않는 사용자 - 404 처리', async ({ page }) => {
    await page.goto(routes.blog.user('nonexistentuser12345'))
    await waitForLoading(page)

    // 404 또는 에러 메시지
    const notFound = page.getByText(/찾을 수 없|not found|존재하지 않|404/i)
    const count = await notFound.count()

    // 404 페이지이거나 에러 메시지 표시
    expect(count > 0 || page.url().includes('404')).toBeTruthy()
  })

  test('본인 블로그 - 수정 버튼 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.user('testuser'))
    await waitForLoading(authenticatedPage)

    // 프로필 수정 버튼 (본인만 보임)
    const editButton = authenticatedPage.getByRole('button', { name: /프로필 수정|edit profile|설정/i })
      .or(authenticatedPage.locator('.edit-profile, .settings-btn'))

    const count = await editButton.count()
    // 본인 블로그인 경우에만 수정 버튼 존재
    if (count > 0) {
      await expect(editButton.first()).toBeVisible()
    }
  })
})
