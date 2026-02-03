import { test, expect } from '../../fixtures/base'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Dashboard', () => {
  test.describe('인증 필요', () => {
    test('비로그인 시 대시보드 접근 차단', async ({ page }) => {
      await page.goto('/dashboard')
      await waitForLoading(page)

      // 비로그인 시 로그인 모달이 표시됨
      await expect(page.getByRole('button', { name: '로그인', exact: true })).toBeVisible({ timeout: 10000 })
    })
  })

  test.describe('대시보드 UI', () => {
    test('로그인 후 대시보드 페이지 접근', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 대시보드 헤더 확인 (greeting message)
      const greeting = authenticatedPage.getByText(/좋은 (아침|오후|저녁)이에요/)
      await expect(greeting).toBeVisible({ timeout: 10000 })
    })

    test('통계 카드 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 통계 카드들 확인
      await expect(authenticatedPage.getByText('작성한 글')).toBeVisible()
      await expect(authenticatedPage.getByText('주문 건수')).toBeVisible()
      await expect(authenticatedPage.getByText('받은 좋아요')).toBeVisible()
    })

    test('빠른 작업 버튼 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 빠른 작업 섹션
      await expect(authenticatedPage.getByRole('heading', { name: /빠른 작업/ })).toBeVisible()
      // 버튼들은 이모지와 함께 표시됨 (중복 버튼이 있을 수 있으므로 first() 사용)
      await expect(authenticatedPage.getByRole('button', { name: /새 글 작성/ }).first()).toBeVisible()
      await expect(authenticatedPage.getByRole('button', { name: /상품 둘러보기/ }).first()).toBeVisible()
      await expect(authenticatedPage.getByRole('button', { name: /주문 내역/ }).first()).toBeVisible()
    })

    test('최근 활동 섹션 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 최근 활동 섹션
      await expect(authenticatedPage.getByText('최근 활동')).toBeVisible()
    })

    test('서비스 그리드 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 서비스 섹션
      await expect(authenticatedPage.getByRole('heading', { name: /서비스/ })).toBeVisible()
    })

    test('새 글 작성 버튼 클릭 시 블로그 글쓰기로 이동', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      // 헤더의 새 글 작성 버튼 클릭
      const writeButton = authenticatedPage.getByRole('button', { name: /새 글 작성/ }).first()
      await writeButton.click()

      await authenticatedPage.waitForURL(/\/blog\/write/)
    })

    test('빠른 작업 - 상품 둘러보기 클릭', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      const browseButton = authenticatedPage.getByRole('button', { name: /상품 둘러보기/ })
      await browseButton.click()

      await authenticatedPage.waitForURL(/\/shopping/)
    })

    test('빠른 작업 - 주문 내역 클릭', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/dashboard')
      await waitForLoading(authenticatedPage)

      const ordersButton = authenticatedPage.getByRole('button', { name: /주문 내역/ })
      await ordersButton.click()

      await authenticatedPage.waitForURL(/\/shopping\/orders/)
    })
  })
})
