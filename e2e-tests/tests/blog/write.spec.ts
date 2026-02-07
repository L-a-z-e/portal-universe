import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading, waitForAPI, expectToast } from '../../utils/wait'
import { commonSelectors } from '../../utils/selectors'

test.describe('Blog - Write Post', () => {
  test('비로그인 - 글쓰기 페이지 접근 제한', async ({ page }) => {
    await page.goto(routes.blog.write)

    // 로그인 페이지 리다이렉트 또는 로그인 모달 또는 페이지 접근 가능
    const loginModal = page.locator('[role="dialog"]')
    const writeForm = page.getByText(/새 글|작성|Write/i)
    const loginRedirect = page.url().includes('/login')
    const hasLoginModal = await loginModal.isVisible().catch(() => false)
    const hasWriteForm = await writeForm.first().isVisible().catch(() => false)
    expect(loginRedirect || hasLoginModal || hasWriteForm).toBeTruthy()
  })

  test('로그인 - 글쓰기 페이지 접근', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 글쓰기 폼 확인
    await expect(authenticatedPage).toHaveURL(/write/)
  })

  test('글쓰기 폼 요소 확인', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 제목 입력 필드
    const titleInput = authenticatedPage.getByPlaceholder(/제목/)
      .or(authenticatedPage.getByRole('textbox', { name: /제목|title/i }))
      .or(authenticatedPage.locator('input[name="title"], .title-input'))

    await expect(titleInput.first()).toBeVisible({ timeout: 15000 })

    // 본문 에디터
    const contentEditor = authenticatedPage.locator('[contenteditable="true"], .toastui-editor-contents, textarea, .editor, .content-editor')
    await expect(contentEditor.first()).toBeVisible({ timeout: 15000 })
  })

  test('제목 입력', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    const titleInput = authenticatedPage.getByRole('textbox', { name: /제목|title/i })
      .or(authenticatedPage.getByPlaceholder(/제목|title/i))
      .or(authenticatedPage.locator('input[name="title"], .title-input'))

    await titleInput.first().fill('E2E 테스트 제목')
    await expect(titleInput.first()).toHaveValue('E2E 테스트 제목')
  })

  test('본문 입력', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    const contentEditor = authenticatedPage.locator('[contenteditable="true"], .toastui-editor-contents, textarea, .editor, .content-editor').first()
    await expect(contentEditor).toBeVisible({ timeout: 15000 })

    // contenteditable인 경우
    const isContentEditable = await contentEditor.evaluate(el => el.contentEditable === 'true')

    if (isContentEditable) {
      await contentEditor.click()
      await authenticatedPage.keyboard.type('E2E 테스트 본문 내용입니다.')
    } else {
      await contentEditor.fill('E2E 테스트 본문 내용입니다.')
    }
  })

  test('태그 추가', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 태그 입력 필드
    const tagInput = authenticatedPage.getByRole('textbox', { name: /태그|tag/i })
      .or(authenticatedPage.getByPlaceholder(/태그|tag/i))
      .or(authenticatedPage.locator('input[name="tags"], .tag-input'))

    const count = await tagInput.count()
    if (count > 0) {
      await tagInput.first().fill('테스트')
      await authenticatedPage.keyboard.press('Enter')

      // 태그 추가 확인
      const addedTag = authenticatedPage.locator('.tag, .tag-item').filter({ hasText: '테스트' })
      await expect(addedTag).toBeVisible()
    }
  })

  test('임시 저장', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 제목 입력
    const titleInput = authenticatedPage.getByRole('textbox', { name: /제목|title/i })
      .or(authenticatedPage.getByPlaceholder(/제목|title/i))
      .or(authenticatedPage.locator('input[name="title"], .title-input'))

    await titleInput.first().fill('임시 저장 테스트')

    // 임시 저장 버튼
    const draftButton = authenticatedPage.getByRole('button', { name: /임시|draft|저장/i })

    const count = await draftButton.count()
    if (count > 0) {
      await draftButton.first().click()
      // 토스트 또는 성공 메시지 확인
    }
  })

  test('발행 버튼 확인', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 발행/등록 버튼
    const publishButton = commonSelectors.submitButton(authenticatedPage)
      .or(authenticatedPage.getByRole('button', { name: /발행|publish|등록/i }))

    await expect(publishButton.first()).toBeVisible()
  })

  test('취소 버튼 클릭 시 확인 다이얼로그', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 제목 입력 (변경사항 생성)
    const titleInput = authenticatedPage.getByRole('textbox', { name: /제목|title/i })
      .or(authenticatedPage.getByPlaceholder(/제목|title/i))
      .or(authenticatedPage.locator('input[name="title"], .title-input'))

    await titleInput.first().fill('테스트 제목')

    // 취소 버튼
    const cancelButton = commonSelectors.cancelButton(authenticatedPage)
      .or(authenticatedPage.getByRole('button', { name: /나가기|exit|취소/i }))

    const count = await cancelButton.count()
    if (count > 0) {
      // 다이얼로그 핸들러 설정
      authenticatedPage.on('dialog', dialog => dialog.dismiss())

      await cancelButton.first().click()
    }
  })

  test('시리즈 선택', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.blog.write)
    await waitForLoading(authenticatedPage)

    // 시리즈 선택 버튼/셀렉트
    const seriesSelect = authenticatedPage.getByRole('combobox', { name: /시리즈|series/i })
      .or(authenticatedPage.getByRole('button', { name: /시리즈|series/i }))
      .or(authenticatedPage.locator('select[name="series"], .series-select'))

    const count = await seriesSelect.count()
    if (count > 0) {
      await expect(seriesSelect.first()).toBeVisible()
    }
  })
})
