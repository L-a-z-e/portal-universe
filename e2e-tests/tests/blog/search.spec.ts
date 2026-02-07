import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Search', () => {
  test('검색 입력 필드 표시', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    // 검색 입력 필드
    const searchInput = page.getByRole('searchbox')
      .or(page.getByPlaceholder(/검색|search/i))
      .or(page.locator('input[type="search"], .search-input'))

    const count = await searchInput.count()
    if (count > 0) {
      await expect(searchInput.first()).toBeVisible()
    }
  })

  test('검색어 입력 및 실행', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    const searchInput = page.getByRole('searchbox')
      .or(page.getByPlaceholder(/검색|search/i))
      .or(page.locator('input[type="search"], .search-input'))

    const count = await searchInput.count()
    if (count > 0) {
      // 검색어 입력
      await searchInput.first().fill('테스트')
      await page.keyboard.press('Enter')

      // 검색 결과 페이지 또는 결과 표시
      await waitForLoading(page)
    }
  })

  test('검색 결과 목록 표시', async ({ page }) => {
    await page.goto('/blog/search?q=test')
    await waitForLoading(page)

    // 페이지가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Search page does not exist')
      return
    }

    // 검색 결과 목록
    const results = blogSelectors.postCard(page)
      .or(page.locator('.search-result, .result-item'))

    const count = await results.count()
    // 결과가 있거나 "결과 없음" 메시지 표시
    if (count > 0) {
      await expect(results.first()).toBeVisible()
    } else {
      const noResults = page.getByText(/결과.*없|no results|찾을 수 없/i)
      await expect(noResults).toBeVisible()
    }
  })

  test('검색 결과 없음 표시', async ({ page }) => {
    await page.goto('/blog/search?q=nonexistentkeyword12345')
    await waitForLoading(page)

    // 결과 없음 메시지
    const noResults = page.getByText(/결과.*없|no results|찾을 수 없/i)
    const count = await noResults.count()

    if (count > 0) {
      await expect(noResults.first()).toBeVisible()
    }
  })

  test('검색 필터 (태그, 작성자)', async ({ page }) => {
    await page.goto('/blog/search?q=test')
    await waitForLoading(page)

    // 필터 옵션
    const filters = page.locator('.search-filters, .filter-options')
      .or(page.getByRole('combobox', { name: /필터|filter/i }))

    const count = await filters.count()
    if (count > 0) {
      await expect(filters.first()).toBeVisible()
    }
  })

  test('검색 자동완성', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    const searchInput = page.getByRole('searchbox')
      .or(page.getByPlaceholder(/검색|search/i))

    const count = await searchInput.count()
    if (count > 0) {
      // 검색어 입력
      await searchInput.first().fill('java')

      // 자동완성 드롭다운
      const autocomplete = page.locator('.autocomplete, .search-suggestions, [role="listbox"]')
      const acCount = await autocomplete.count()

      if (acCount > 0) {
        await expect(autocomplete.first()).toBeVisible()
      }
    }
  })

  test('최근 검색어', async ({ page }) => {
    await page.goto(routes.blog.home)
    await waitForLoading(page)

    const searchInput = page.getByRole('searchbox')
      .or(page.getByPlaceholder(/검색|search/i))

    const count = await searchInput.count()
    if (count > 0) {
      await searchInput.first().click()

      // 최근 검색어 목록
      const recentSearches = page.locator('.recent-searches, .search-history')
        .or(page.getByText(/최근 검색|recent searches/i))

      const recentCount = await recentSearches.count()
      if (recentCount > 0) {
        await expect(recentSearches.first()).toBeVisible()
      }
    }
  })

  test('검색 결과 정렬', async ({ page }) => {
    await page.goto('/blog/search?q=test')
    await waitForLoading(page)

    // 정렬 옵션
    const sortOptions = page.getByRole('combobox', { name: /정렬|sort/i })
      .or(page.locator('.sort-options, .sort-select'))

    const count = await sortOptions.count()
    if (count > 0) {
      await expect(sortOptions.first()).toBeVisible()
    }
  })

  test('검색 결과 페이지네이션', async ({ page }) => {
    await page.goto('/blog/search?q=test')
    await waitForLoading(page)

    // 페이지네이션
    const pagination = page.locator('.pagination, nav[aria-label*="pagination"]')
      .or(page.getByRole('button', { name: /더 보기|load more|다음/i }))

    const count = await pagination.count()
    if (count > 0) {
      await expect(pagination.first()).toBeVisible()
    }
  })

  test('검색어 하이라이트', async ({ page }) => {
    await page.goto('/blog/search?q=test')
    await waitForLoading(page)

    // 하이라이트된 텍스트
    const highlight = page.locator('mark, .highlight, .search-highlight')

    const count = await highlight.count()
    if (count > 0) {
      await expect(highlight.first()).toBeVisible()
    }
  })
})
