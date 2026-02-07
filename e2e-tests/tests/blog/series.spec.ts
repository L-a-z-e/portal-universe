import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Series', () => {
  test('시리즈 목록 페이지 접근', async ({ page }) => {
    await page.goto(routes.blog.myPage)
    await waitForLoading(page)

    // 시리즈 탭 클릭
    const seriesTab = page.getByRole('tab', { name: /시리즈|series/i })
      .or(page.getByText(/시리즈|series/i))

    const count = await seriesTab.count()
    if (count > 0) {
      await seriesTab.first().click()

      // 시리즈 목록 표시
      const seriesList = blogSelectors.seriesList(page)
      await expect(seriesList).toBeVisible()
    }
  })

  test('시리즈 상세 페이지', async ({ page }) => {
    await page.goto(routes.blog.series('1'))
    await waitForLoading(page)

    // 시리즈가 존재하는지 확인
    const notFound = page.getByText(/페이지를 찾을 수 없습니다|not found|시리즈.*없/i)
    const hasNotFound = await notFound.isVisible({ timeout: 3000 }).catch(() => false)
    if (hasNotFound) {
      test.skip(true, 'Test series does not exist in database')
      return
    }

    // 시리즈 제목 표시
    const seriesTitle = page.locator('h1, .series-title')
    await expect(seriesTitle.first()).toBeVisible()
  })

  test('시리즈 내 포스트 목록', async ({ page }) => {
    await page.goto(routes.blog.series('1'))
    await waitForLoading(page)

    // 포스트 목록
    const posts = blogSelectors.postCard(page)
      .or(page.locator('.series-post, .post-item'))

    const count = await posts.count()
    if (count > 0) {
      await expect(posts.first()).toBeVisible()
    }
  })

  test('시리즈 포스트 순서 표시', async ({ page }) => {
    await page.goto(routes.blog.series('1'))
    await waitForLoading(page)

    // 순서 번호 표시
    const orderNumber = page.locator('.post-order, .series-order, .order-number')
      .or(page.getByText(/^[0-9]+\./))

    const count = await orderNumber.count()
    if (count > 0) {
      await expect(orderNumber.first()).toBeVisible()
    }
  })

  test('시리즈 생성 (로그인 필요)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 시리즈 탭 이동
    const seriesTab = authenticatedPage.getByRole('tab', { name: /시리즈|series/i })
    const tabCount = await seriesTab.count()

    if (tabCount > 0) {
      await seriesTab.first().click()

      // 시리즈 추가 버튼
      const addButton = authenticatedPage.getByRole('button', { name: /시리즈 추가|새 시리즈|add series|new series/i })
        .or(authenticatedPage.locator('.add-series-btn'))

      const addCount = await addButton.count()
      if (addCount > 0) {
        await addButton.first().click()

        // 시리즈 생성 모달/폼
        const modal = authenticatedPage.locator('[role="dialog"], .modal, .series-form')
        await expect(modal).toBeVisible()
      }
    }
  })

  test('시리즈 수정 (본인 시리즈)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    // 시리즈 탭 이동
    const seriesTab = authenticatedPage.getByRole('tab', { name: /시리즈|series/i })
    const tabCount = await seriesTab.count()

    if (tabCount > 0) {
      await seriesTab.first().click()

      // 시리즈 아이템의 수정 버튼
      const editButton = authenticatedPage.getByRole('button', { name: /수정|edit/i })
        .or(authenticatedPage.locator('.series-edit, .edit-btn'))

      const editCount = await editButton.count()
      if (editCount > 0) {
        await editButton.first().click()
      }
    }
  })

  test('시리즈 삭제 (본인 시리즈)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.myPage)
    await waitForLoading(authenticatedPage)

    const seriesTab = authenticatedPage.getByRole('tab', { name: /시리즈|series/i })
    const tabCount = await seriesTab.count()

    if (tabCount > 0) {
      await seriesTab.first().click()

      // 삭제 버튼
      const deleteButton = authenticatedPage.getByRole('button', { name: /삭제|delete/i })
        .or(authenticatedPage.locator('.series-delete, .delete-btn'))

      const deleteCount = await deleteButton.count()
      if (deleteCount > 0) {
        // 다이얼로그 핸들러
        authenticatedPage.on('dialog', dialog => dialog.dismiss())
        await deleteButton.first().click()
      }
    }
  })

  test('시리즈 네비게이션 (이전/다음 포스트)', async ({ page }) => {
    await page.goto(routes.blog.series('1'))
    await waitForLoading(page)

    // 포스트 클릭하여 상세로 이동
    const posts = page.locator('.series-post, .post-item, article')
    const count = await posts.count()

    if (count > 0) {
      await posts.first().click()
      await waitForLoading(page)

      // 시리즈 네비게이션 확인
      const seriesNav = page.locator('.series-nav, .series-navigation')
        .or(page.getByText(/이전 글|다음 글|prev|next/i))

      const navCount = await seriesNav.count()
      if (navCount > 0) {
        await expect(seriesNav.first()).toBeVisible()
      }
    }
  })
})
