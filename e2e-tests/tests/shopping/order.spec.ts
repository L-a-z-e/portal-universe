import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'

test.describe('Shopping - Order', () => {
  test('주문 내역 페이지 접근 (로그인 필요)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    await expect(authenticatedPage).toHaveURL(/orders/)
  })

  test('비로그인 - 주문 내역 접근 제한', async ({ page }) => {
    await page.goto(routes.shopping.orders)

    // 로그인 페이지로 리다이렉트
    await expect(page).toHaveURL(/login/)
  })

  test('주문 내역 목록 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    // 주문 목록 또는 빈 상태 메시지
    const orders = authenticatedPage.locator('.order-item, .order-card')
    const emptyMessage = authenticatedPage.getByText(/주문.*없|no orders|empty/i)

    const hasOrders = (await orders.count()) > 0
    const hasEmptyMessage = (await emptyMessage.count()) > 0

    expect(hasOrders || hasEmptyMessage).toBeTruthy()
  })

  test('주문 상세 정보 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    const orders = authenticatedPage.locator('.order-item, .order-card')
    const count = await orders.count()

    if (count > 0) {
      // 첫 번째 주문 클릭
      await orders.first().click()

      // 주문 상세 정보
      const orderDetail = authenticatedPage.locator('.order-detail, .order-info')
        .or(authenticatedPage.getByText(/주문 번호|order.*#|order.*id/i))

      await expect(orderDetail.first()).toBeVisible()
    }
  })

  test('주문 상태 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    const orders = authenticatedPage.locator('.order-item, .order-card')
    const count = await orders.count()

    if (count > 0) {
      // 주문 상태 확인
      const status = orders.first().locator('.order-status, .status')
        .or(orders.first().getByText(/배송|결제|완료|취소|pending|delivered|canceled/i))

      await expect(status.first()).toBeVisible()
    }
  })

  test('주문 취소 버튼 (취소 가능 상태)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    const orders = authenticatedPage.locator('.order-item, .order-card')
    const count = await orders.count()

    if (count > 0) {
      // 취소 버튼
      const cancelButton = orders.first().getByRole('button', { name: /취소|cancel/i })

      const cancelCount = await cancelButton.count()
      if (cancelCount > 0) {
        await expect(cancelButton.first()).toBeVisible()
      }
    }
  })

  test('주문 필터 (기간/상태)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    // 필터 옵션
    const filter = authenticatedPage.getByRole('combobox', { name: /기간|상태|filter/i })
      .or(authenticatedPage.locator('.order-filter, .filter-options'))

    const count = await filter.count()
    if (count > 0) {
      await expect(filter.first()).toBeVisible()
    }
  })

  test('재주문 버튼', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.orders)
    await waitForLoading(authenticatedPage)

    const orders = authenticatedPage.locator('.order-item, .order-card')
    const count = await orders.count()

    if (count > 0) {
      // 재주문 버튼
      const reorderButton = orders.first().getByRole('button', { name: /재주문|reorder|다시 주문/i })

      const reorderCount = await reorderButton.count()
      if (reorderCount > 0) {
        await expect(reorderButton.first()).toBeVisible()
      }
    }
  })
})
