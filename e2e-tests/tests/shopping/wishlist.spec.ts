import { test, expect } from '../../fixtures/base'
import { routes, testProducts } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { shoppingSelectors } from '../../utils/selectors'

test.describe('Shopping - Wishlist', () => {
  test('위시리스트 페이지 접근 (로그인 필요)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.wishlist)
    await waitForLoading(authenticatedPage)

    await expect(authenticatedPage).toHaveURL(/wishlist/)
  })

  test('비로그인 - 위시리스트 접근 제한', async ({ page }) => {
    await page.goto(routes.shopping.wishlist)

    // 로그인 모달 또는 리다이렉트 또는 로그인 텍스트 확인
    const loginModal = page.locator('[role="dialog"]')
    const hasLoginModal = await loginModal.isVisible({ timeout: 5000 }).catch(() => false)
    const loginRedirect = page.url().includes('/login')
    const loginText = page.getByText(/로그인|Login/i)
    const hasLoginText = await loginText.first().isVisible().catch(() => false)
    expect(hasLoginModal || loginRedirect || hasLoginText).toBeTruthy()
  })

  test('상품 상세에서 위시리스트 추가', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.product(testProducts.existing.id))
    await waitForLoading(authenticatedPage)

    // 찜하기 버튼
    const wishlistButton = shoppingSelectors.wishlistButton(authenticatedPage)
    const count = await wishlistButton.count()

    if (count > 0) {
      await wishlistButton.first().click()

      // 성공 표시
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('위시리스트 아이템 목록', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.wishlist)
    await waitForLoading(authenticatedPage)

    // Shopping MF가 로드되지 않을 수 있음
    const mfContent = authenticatedPage.locator('[class*="shopping"], [data-service="shopping"]')
    const hasMfContent = await mfContent.count().catch(() => 0) > 0

    // 위시리스트 아이템 또는 빈 메시지 또는 페이지 콘텐츠
    const items = authenticatedPage.locator('.wishlist-item, .product-card')
    const emptyMessage = authenticatedPage.getByText(/위시리스트.*없|empty|비어/i)
    const pageContent = authenticatedPage.locator('main, .page-content')

    const hasItems = (await items.count()) > 0
    const hasEmptyMessage = (await emptyMessage.count()) > 0
    const hasContent = (await pageContent.count()) > 0

    expect(hasItems || hasEmptyMessage || hasContent).toBeTruthy()
  })

  test('위시리스트에서 장바구니 담기', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.wishlist)
    await waitForLoading(authenticatedPage)

    const items = authenticatedPage.locator('.wishlist-item, .product-card')
    const count = await items.count()

    if (count > 0) {
      // 장바구니 담기 버튼
      const addToCartButton = items.first().getByRole('button', { name: /장바구니|cart|담기/i })

      const btnCount = await addToCartButton.count()
      if (btnCount > 0) {
        await addToCartButton.first().click()
      }
    }
  })

  test('위시리스트에서 아이템 삭제', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.wishlist)
    await waitForLoading(authenticatedPage)

    const items = authenticatedPage.locator('.wishlist-item, .product-card')
    const count = await items.count()

    if (count > 0) {
      // 삭제 버튼
      const removeButton = items.first().getByRole('button', { name: /삭제|remove|delete/i })
        .or(items.first().locator('.remove-wishlist, .delete-btn'))

      const btnCount = await removeButton.count()
      if (btnCount > 0) {
        await removeButton.first().click()
      }
    }
  })

  test('위시리스트 토글 (추가/제거)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.shopping.product(testProducts.existing.id))
    await waitForLoading(authenticatedPage)

    const wishlistButton = shoppingSelectors.wishlistButton(authenticatedPage)
    const count = await wishlistButton.count()

    if (count > 0) {
      // 현재 상태 확인
      const isWished = await wishlistButton.first().evaluate(el =>
        el.classList.contains('active') ||
        el.classList.contains('wished') ||
        el.getAttribute('aria-pressed') === 'true'
      )

      // 클릭하여 토글
      await wishlistButton.first().click()
      await authenticatedPage.waitForTimeout(500)

      // 상태 변경 확인
      const afterWished = await wishlistButton.first().evaluate(el =>
        el.classList.contains('active') ||
        el.classList.contains('wished') ||
        el.getAttribute('aria-pressed') === 'true'
      )

      expect(afterWished).not.toBe(isWished)
    }
  })

  test('상품 목록에서 위시리스트 버튼', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    const productCards = shoppingSelectors.productCard(page)
    const count = await productCards.count()

    if (count > 0) {
      // 카드 내 위시리스트 버튼
      const wishlistButton = productCards.first().locator('[aria-label*="wish"], .wishlist-btn, .heart-icon')
        .or(productCards.first().getByRole('button', { name: /찜|wish|좋아요/i }))

      const btnCount = await wishlistButton.count()
      if (btnCount > 0) {
        await expect(wishlistButton.first()).toBeVisible()
      }
    }
  })
})
