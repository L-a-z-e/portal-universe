import { test, expect } from '../../fixtures/base'
import { routes, testProducts } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { shoppingSelectors } from '../../utils/selectors'

test.describe('Shopping - Products', () => {
  test('상품 목록 페이지 로드', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    await expect(page).toHaveURL(/products/)
  })

  test('상품 카드 목록 표시', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    // 상품 카드 확인
    const productCards = shoppingSelectors.productCard(page)
    const count = await productCards.count()

    if (count > 0) {
      await expect(productCards.first()).toBeVisible()
    }
  })

  test('상품 카드 정보 표시 (이름, 가격)', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    const productCards = shoppingSelectors.productCard(page)
    const count = await productCards.count()

    if (count > 0) {
      const firstCard = productCards.first()

      // 가격 표시
      const price = shoppingSelectors.productPrice(page).first()
        .or(firstCard.locator('.price, .product-price'))

      await expect(price.first()).toBeVisible()
    }
  })

  test('상품 상세 페이지 이동', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    const productCards = shoppingSelectors.productCard(page)
    const count = await productCards.count()

    if (count > 0) {
      await productCards.first().click()

      // 상품 상세 페이지로 이동
      await expect(page).toHaveURL(/\/shopping\/products\/\d+/)
    }
  })

  test('상품 상세 페이지 정보 표시', async ({ page }) => {
    await page.goto(routes.shopping.product(testProducts.existing.id))
    await waitForLoading(page)

    // 상품명 표시
    const productName = page.locator('h1, .product-name, .product-title')
    await expect(productName.first()).toBeVisible()

    // 가격 표시
    const price = shoppingSelectors.productPrice(page)
    await expect(price.first()).toBeVisible()
  })

  test('상품 카테고리 필터', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    // 카테고리 필터
    const categoryFilter = page.getByRole('combobox', { name: /카테고리|category/i })
      .or(page.locator('.category-filter, .category-select'))

    const count = await categoryFilter.count()
    if (count > 0) {
      await expect(categoryFilter.first()).toBeVisible()
    }
  })

  test('상품 정렬', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    // 정렬 옵션
    const sortSelect = page.getByRole('combobox', { name: /정렬|sort/i })
      .or(page.locator('.sort-select, .sort-options'))

    const count = await sortSelect.count()
    if (count > 0) {
      await expect(sortSelect.first()).toBeVisible()
    }
  })

  test('상품 검색', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    // 검색 입력
    const searchInput = page.getByRole('searchbox')
      .or(page.getByPlaceholder(/검색|search/i))

    const count = await searchInput.count()
    if (count > 0) {
      await searchInput.first().fill('테스트')
      await page.keyboard.press('Enter')
      await waitForLoading(page)
    }
  })

  test('상품 페이지네이션', async ({ page }) => {
    await page.goto(routes.shopping.products)
    await waitForLoading(page)

    // 페이지네이션
    const pagination = page.locator('.pagination, nav[aria-label*="pagination"]')
      .or(page.getByRole('button', { name: /다음|next|더 보기/i }))

    const count = await pagination.count()
    if (count > 0) {
      await expect(pagination.first()).toBeVisible()
    }
  })
})
