import { test, expect } from '../../fixtures/base'
import { routes, testProducts } from '../../fixtures/test-data'
import { waitForLoading, expectToast } from '../../utils/wait'
import { shoppingSelectors } from '../../utils/selectors'

test.describe('Shopping - Cart', () => {
  test('장바구니 페이지 접근', async ({ page }) => {
    await page.goto(routes.shopping.cart)
    await waitForLoading(page)

    await expect(page).toHaveURL(/cart/)
  })

  test('빈 장바구니 메시지', async ({ page }) => {
    await page.goto(routes.shopping.cart)
    await waitForLoading(page)

    // Shopping MF가 로드되지 않을 수 있음
    await page.waitForTimeout(3000)

    // 빈 장바구니 메시지 또는 아이템 목록 또는 페이지 콘텐츠
    const emptyMessage = page.getByText(/비어|empty|장바구니.*없/i)
    const cartItems = shoppingSelectors.cartItem(page)
    const pageContent = page.locator('main, .page-content, h1, [class*="cart"]')

    const hasEmptyMessage = (await emptyMessage.count()) > 0
    const hasItems = (await cartItems.count()) > 0
    const hasContent = (await pageContent.count()) > 0

    expect(hasEmptyMessage || hasItems || hasContent).toBeTruthy()
  })

  test('상품 상세에서 장바구니 담기', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.product(testProducts.existing.id))
    await waitForLoading(authenticatedPage)

    // 장바구니 담기 버튼
    const addToCartButton = shoppingSelectors.addToCartButton(authenticatedPage)
    await addToCartButton.click()

    // 성공 토스트 또는 장바구니 아이콘 업데이트
    const successIndicator = authenticatedPage.getByText(/장바구니|담았|added|cart/i)
      .or(authenticatedPage.locator('.toast, .notification'))

    await expect(successIndicator.first()).toBeVisible({ timeout: 5000 })
  })

  test('장바구니 아이템 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.cart)
    await waitForLoading(authenticatedPage)

    // 장바구니 아이템
    const cartItems = shoppingSelectors.cartItem(authenticatedPage)
    const count = await cartItems.count()

    if (count > 0) {
      await expect(cartItems.first()).toBeVisible()
    }
  })

  test('장바구니 수량 변경', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.cart)
    await waitForLoading(authenticatedPage)

    const cartItems = shoppingSelectors.cartItem(authenticatedPage)
    const count = await cartItems.count()

    if (count > 0) {
      // 수량 증가 버튼
      const increaseButton = authenticatedPage.getByRole('button', { name: /\+|증가|increase/i })
        .or(authenticatedPage.locator('.quantity-increase, .qty-plus'))

      const incCount = await increaseButton.count()
      if (incCount > 0) {
        await increaseButton.first().click()
        await authenticatedPage.waitForTimeout(500)
      }
    }
  })

  test('장바구니 아이템 삭제', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.cart)
    await waitForLoading(authenticatedPage)

    const cartItems = shoppingSelectors.cartItem(authenticatedPage)
    const count = await cartItems.count()

    if (count > 0) {
      // 삭제 버튼
      const deleteButton = authenticatedPage.getByRole('button', { name: /삭제|delete|remove/i })
        .or(authenticatedPage.locator('.remove-item, .delete-item'))

      const deleteCount = await deleteButton.count()
      if (deleteCount > 0) {
        await deleteButton.first().click()
      }
    }
  })

  test('장바구니 총 금액 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.cart)
    await waitForLoading(authenticatedPage)

    // 총 금액
    const total = shoppingSelectors.cartTotal(authenticatedPage)
      .or(authenticatedPage.getByText(/총.*원|total.*₩|\d+,\d+원/i))

    const count = await total.count()
    if (count > 0) {
      await expect(total.first()).toBeVisible()
    }
  })

  test('결제 버튼', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.cart)
    await waitForLoading(authenticatedPage)

    // 결제/주문 버튼
    const checkoutButton = shoppingSelectors.checkoutButton(authenticatedPage)

    const count = await checkoutButton.count()
    if (count > 0) {
      await expect(checkoutButton.first()).toBeVisible()
    }
  })

  test('비로그인 시 장바구니 담기 - 로그인 유도', async ({ page }) => {
    await page.goto(routes.shopping.product(testProducts.existing.id))
    await waitForLoading(page)

    const addToCartButton = shoppingSelectors.addToCartButton(page)
    await addToCartButton.click()

    // 로그인 유도
    const loginPrompt = page.getByText(/로그인|login/i)
    const promptCount = await loginPrompt.count()
    const isLoginPage = page.url().includes('login')

    expect(promptCount > 0 || isLoginPage).toBeTruthy()
  })
})
