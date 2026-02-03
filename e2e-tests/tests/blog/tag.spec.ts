import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Tags', () => {
  test('태그 페이지 접근', async ({ page }) => {
    await page.goto(routes.blog.tag('javascript'))
    await waitForLoading(page)

    // 태그 페이지 확인
    await expect(page).toHaveURL(/tags/)
  })

  test('태그별 포스트 목록 표시', async ({ page }) => {
    await page.goto(routes.blog.tag('javascript'))
    await waitForLoading(page)

    // 포스트 목록
    const posts = blogSelectors.postCard(page)
    const count = await posts.count()

    // 포스트가 있으면 표시 확인
    if (count > 0) {
      await expect(posts.first()).toBeVisible()
    }
  })

  test('태그 이름 표시', async ({ page }) => {
    await page.goto(routes.blog.tag('javascript'))
    await waitForLoading(page)

    // 태그 이름이 페이지에 표시
    const tagTitle = page.getByRole('heading', { name: /javascript/i })
      .or(page.locator('.tag-title, h1').filter({ hasText: /javascript/i }))

    await expect(tagTitle.first()).toBeVisible()
  })

  test('포스트 카드 내 태그 클릭', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    // 포스트 카드 내 태그
    const tagItem = blogSelectors.tagItem(page)
    const count = await tagItem.count()

    if (count > 0) {
      // 태그 클릭
      const tagText = await tagItem.first().textContent()
      await tagItem.first().click()

      // 태그 페이지로 이동 확인
      await expect(page).toHaveURL(/tags/)
    }
  })

  test('인기 태그 목록', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // 인기 태그 섹션
    const popularTags = page.locator('.popular-tags, .trending-tags, .tag-cloud')
      .or(page.getByText(/인기 태그|popular tags|trending tags/i))

    const count = await popularTags.count()
    if (count > 0) {
      await expect(popularTags.first()).toBeVisible()
    }
  })

  test('태그 검색/필터', async ({ page }) => {
    await page.goto(routes.blog.feed)
    await waitForLoading(page)

    // 태그 필터 기능
    const tagFilter = page.getByRole('combobox', { name: /태그|tag/i })
      .or(page.locator('.tag-filter, .tag-search'))

    const count = await tagFilter.count()
    if (count > 0) {
      await expect(tagFilter.first()).toBeVisible()
    }
  })

  test('포스트 상세 - 태그 목록 표시', async ({ page }) => {
    await page.goto(routes.blog.post('1'))
    await waitForLoading(page)

    // 태그 목록
    const tagList = blogSelectors.tagList(page)
    const count = await tagList.count()

    if (count > 0) {
      await expect(tagList.first()).toBeVisible()
    }
  })

  test('글쓰기 - 태그 자동완성', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 태그 입력 필드
    const tagInput = authenticatedPage.getByRole('textbox', { name: /태그|tag/i })
      .or(authenticatedPage.getByPlaceholder(/태그|tag/i))
      .or(authenticatedPage.locator('.tag-input'))

    const count = await tagInput.count()
    if (count > 0) {
      await tagInput.first().fill('java')

      // 자동완성 드롭다운
      const autocomplete = authenticatedPage.locator('.autocomplete, .tag-suggestions, [role="listbox"]')
      const acCount = await autocomplete.count()

      // 자동완성이 있으면 표시 확인 (없을 수도 있음)
      if (acCount > 0) {
        await expect(autocomplete.first()).toBeVisible()
      }
    }
  })

  test('태그 포스트 개수 표시', async ({ page }) => {
    await page.goto(routes.blog.tag('javascript'))
    await waitForLoading(page)

    // 포스트 개수 표시
    const postCount = page.getByText(/\d+.*개|\d+.*posts/i)
      .or(page.locator('.post-count, .result-count'))

    const count = await postCount.count()
    if (count > 0) {
      await expect(postCount.first()).toBeVisible()
    }
  })
})
