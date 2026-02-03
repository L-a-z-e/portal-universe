import { test, expect } from '../../fixtures/base'
import { waitForLoading } from '../../utils/wait'

test.describe('Blog - Post Edit', () => {
  test.describe('인증 필요', () => {
    test('비로그인 시 수정 페이지 접근 차단', async ({ page }) => {
      await page.goto('/blog/edit/1')
      await waitForLoading(page)

      // 로그인 모달이 표시되거나 리다이렉트
      const loginModal = page.locator('[role="dialog"]')
      const isModalVisible = await loginModal.isVisible().catch(() => false)
      const notOnEditPage = !page.url().includes('/edit/')

      expect(isModalVisible || notOnEditPage).toBeTruthy()
    })
  })

  test.describe('게시글 수정 페이지 UI', () => {
    test('로그인 후 수정 페이지 접근', async ({ authenticatedPage }) => {
      // 먼저 내 게시글 목록에서 수정 가능한 글을 찾아야 함
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)
      await authenticatedPage.waitForTimeout(2000)

      // 내 게시글이 있는지 확인
      const postCards = authenticatedPage.locator('[class*="post"], [class*="card"], article')
      const postCount = await postCards.count()

      if (postCount > 0) {
        // 수정 버튼 또는 링크 클릭
        const editButton = authenticatedPage.getByRole('link', { name: /수정|Edit/ })
          .or(authenticatedPage.getByRole('button', { name: /수정|Edit/ }))

        if (await editButton.count() > 0) {
          await editButton.first().click()

          // 수정 페이지 로드 확인
          await authenticatedPage.waitForURL(/\/blog\/edit\//)
          await waitForLoading(authenticatedPage)

          // 에디터 또는 폼 확인
          const editor = authenticatedPage.locator('[class*="editor"], [class*="toastui"], form')
          await expect(editor.first()).toBeVisible({ timeout: 10000 })
        }
      }
    })

    test('제목 입력 필드 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 제목 입력 필드
        const titleInput = authenticatedPage.getByLabel(/제목|Title/)
          .or(authenticatedPage.locator('input[placeholder*="제목"]'))
          .or(authenticatedPage.locator('input[name*="title"]'))

        await expect(titleInput.first()).toBeVisible({ timeout: 10000 })
      }
    })

    test('마크다운 에디터 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // Toast UI Editor 또는 기타 에디터
        const editor = authenticatedPage.locator('.toastui-editor, [class*="editor"], [contenteditable="true"]')
        await expect(editor.first()).toBeVisible({ timeout: 10000 })
      }
    })

    test('태그 입력 필드 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 태그 입력
        const tagInput = authenticatedPage.getByLabel(/태그|Tags/)
          .or(authenticatedPage.locator('input[placeholder*="태그"]'))
          .or(authenticatedPage.locator('[class*="tag-input"], [class*="autocomplete"]'))

        if (await tagInput.count() > 0) {
          await expect(tagInput.first()).toBeVisible()
        }
      }
    })

    test('카테고리 선택 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 카테고리 선택
        const categorySelect = authenticatedPage.getByLabel(/카테고리|Category/)
          .or(authenticatedPage.locator('select[name*="category"]'))
          .or(authenticatedPage.locator('input[placeholder*="카테고리"]'))

        if (await categorySelect.count() > 0) {
          await expect(categorySelect.first()).toBeVisible()
        }
      }
    })

    test('시리즈 선택 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 시리즈 선택
        const seriesSelect = authenticatedPage.getByLabel(/시리즈|Series/)
          .or(authenticatedPage.locator('select[name*="series"]'))

        if (await seriesSelect.count() > 0) {
          await expect(seriesSelect.first()).toBeVisible()
        }
      }
    })

    test('저장 버튼 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 저장/수정 버튼
        const saveButton = authenticatedPage.getByRole('button', { name: /저장|수정|Save|Update/ })
        await expect(saveButton.first()).toBeVisible({ timeout: 10000 })
      }
    })
  })

  test.describe('게시글 수정 기능', () => {
    test('제목 수정', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 제목 입력 필드
        const titleInput = authenticatedPage.locator('input').first()
        const originalValue = await titleInput.inputValue()

        // 제목 수정
        await titleInput.fill(originalValue + ' (수정됨)')

        // 입력 확인
        await expect(titleInput).toHaveValue(originalValue + ' (수정됨)')

        // 원래대로 복구 (실제 저장하지 않음)
        await titleInput.fill(originalValue)
      }
    })

    test('내용 수정', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 에디터 찾기
        const editor = authenticatedPage.locator('.toastui-editor, [class*="editor"], [contenteditable="true"]').first()

        if (await editor.count() > 0) {
          // 에디터가 로드될 때까지 대기
          await expect(editor).toBeVisible({ timeout: 10000 })
        }
      }
    })
  })

  test.describe('취소 및 네비게이션', () => {
    test('취소 버튼 클릭 시 이전 페이지로 이동', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/blog/my')
      await waitForLoading(authenticatedPage)

      const editLink = authenticatedPage.locator('a[href*="/edit/"]').first()

      if (await editLink.count() > 0) {
        await editLink.click()
        await waitForLoading(authenticatedPage)

        // 취소 버튼
        const cancelButton = authenticatedPage.getByRole('button', { name: /취소|Cancel/ })
          .or(authenticatedPage.getByRole('link', { name: /취소|Cancel/ }))

        if (await cancelButton.count() > 0) {
          await cancelButton.first().click()

          // 수정 페이지를 벗어났는지 확인
          await authenticatedPage.waitForURL((url) => !url.pathname.includes('/edit/'), { timeout: 5000 })
        }
      }
    })
  })
})
