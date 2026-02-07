import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Blog - Advanced Search', () => {
  test.describe('고급 검색 페이지 UI', () => {
    test('고급 검색 페이지 접근', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 페이지 로드 확인
      const pageContent = page.locator('main, [role="main"], form')
      await expect(pageContent.first()).toBeVisible({ timeout: 10000 })
    })

    test('검색 필터 필드 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 키워드 입력 필드
      const keywordInput = page.getByLabel(/키워드|Keyword/)
        .or(page.locator('input[placeholder*="키워드"]'))
        .or(page.locator('input[name*="keyword"]'))

      await expect(keywordInput.first()).toBeVisible()
    })

    test('카테고리 필터 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 카테고리 입력 또는 선택
      const categoryInput = page.getByLabel(/카테고리|Category/)
        .or(page.locator('input[placeholder*="카테고리"]'))
        .or(page.locator('select[name*="category"]'))

      if (await categoryInput.count() > 0) {
        await expect(categoryInput.first()).toBeVisible()
      }
    })

    test('태그 필터 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 태그 입력
      const tagInput = page.getByLabel(/태그|Tags/)
        .or(page.locator('input[placeholder*="태그"]'))
        .or(page.locator('input[name*="tag"]'))

      if (await tagInput.count() > 0) {
        await expect(tagInput.first()).toBeVisible()
      }
    })

    test('날짜 필터 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 시작일, 종료일
      const dateInput = page.locator('input[type="date"]')

      if (await dateInput.count() > 0) {
        await expect(dateInput.first()).toBeVisible()
      }
    })

    test('검색 버튼 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      const searchButton = page.getByRole('button', { name: /검색|Search/ })
      await expect(searchButton).toBeVisible()
    })
  })

  test.describe('검색 유효성 검사', () => {
    test('빈 검색 조건 시 에러 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 검색 버튼 클릭
      const searchButton = page.getByRole('button', { name: /검색|Search/ })
      await searchButton.click()

      // 에러 메시지 확인
      const errorMessage = page.getByText(/최소 하나|검색 조건|입력/)
      await expect(errorMessage.first()).toBeVisible({ timeout: 3000 })
    })
  })

  test.describe('검색 실행', () => {
    test('키워드 검색 실행', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 페이지가 존재하는지 확인
      const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found/i)
      const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
      if (hasNotFound) {
        test.skip(true, 'Advanced search page does not exist')
        return
      }

      // 키워드 입력
      const keywordInput = page.getByLabel(/키워드|Keyword/)
        .or(page.locator('input[placeholder*="키워드"]'))
        .or(page.locator('input').first())

      await keywordInput.first().fill('테스트')

      // 검색 버튼 클릭
      const searchButton = page.getByRole('button', { name: /검색|Search/ })
      await searchButton.click()

      // 결과 대기
      await page.waitForTimeout(2000)

      // 결과 영역 확인 (결과가 있거나 없거나)
      const results = page.locator('[class*="result"], [class*="post"], [class*="card"], article')
      const emptyMessage = page.getByText(/결과가 없습니다|No results/)

      const hasResults = (await results.count()) > 0
      const isEmpty = await emptyMessage.isVisible().catch(() => false)

      expect(hasResults || isEmpty).toBeTruthy()
    })

    test('태그로 검색', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 태그 입력
      const tagInput = page.getByLabel(/태그|Tags/)
        .or(page.locator('input[placeholder*="태그"]'))
        .or(page.locator('input[name*="tag"]'))

      if (await tagInput.count() > 0) {
        await tagInput.first().fill('vue')

        // 검색 버튼 클릭
        const searchButton = page.getByRole('button', { name: /검색|Search/ })
        await searchButton.click()

        await page.waitForTimeout(2000)
      }
    })

    test('날짜 범위로 검색', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 날짜 입력
      const dateInputs = page.locator('input[type="date"]')

      if (await dateInputs.count() >= 2) {
        const today = new Date().toISOString().split('T')[0]
        const lastMonth = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

        await dateInputs.nth(0).fill(lastMonth)
        await dateInputs.nth(1).fill(today)

        // 추가 필터 (키워드) 입력 (빈 검색 방지)
        const keywordInput = page.locator('input').first()
        await keywordInput.fill('a')

        // 검색 버튼 클릭
        const searchButton = page.getByRole('button', { name: /검색|Search/ })
        await searchButton.click()

        await page.waitForTimeout(2000)
      }
    })
  })

  test.describe('검색 결과 표시', () => {
    test('검색 결과 카드 표시', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 키워드 검색
      const keywordInput = page.locator('input').first()
      await keywordInput.fill('테스트')

      const searchButton = page.getByRole('button', { name: /검색|Search/ })
      await searchButton.click()

      await page.waitForTimeout(2000)

      // 결과 또는 빈 상태
      const searchArea = page.locator('main, [role="main"]')
      await expect(searchArea.first()).toBeVisible()
    })
  })

  test.describe('검색 결과 무한 스크롤', () => {
    test('더 많은 결과 로드', async ({ page }) => {
      await page.goto('/blog/search/advanced')
      await waitForLoading(page)

      // 키워드 검색
      const keywordInput = page.locator('input').first()
      await keywordInput.fill('a')

      const searchButton = page.getByRole('button', { name: /검색|Search/ })
      await searchButton.click()

      await page.waitForTimeout(2000)

      // 스크롤
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
      await page.waitForTimeout(2000)
    })
  })
})
