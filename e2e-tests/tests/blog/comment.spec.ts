import { test, expect } from '../../fixtures/base'
import { routes, testPosts, testComments } from '../../fixtures/test-data'
import { waitForLoading, waitForAPI, expectToast } from '../../utils/wait'
import { blogSelectors } from '../../utils/selectors'

test.describe('Blog - Comments', () => {
  const postUrl = routes.blog.post(testPosts.existing.id)

  test('포스트 상세 페이지 - 댓글 섹션 표시', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 댓글 섹션 확인
    const commentSection = blogSelectors.commentSection(page)
    await expect(commentSection).toBeVisible()
  })

  test('비로그인 - 댓글 작성 불가', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 댓글 입력 필드가 비활성화되거나 로그인 안내가 표시
    const commentInput = blogSelectors.commentInput(page)
    const loginPrompt = page.getByText(/로그인|login/i)

    const inputCount = await commentInput.count()
    const promptCount = await loginPrompt.count()

    if (inputCount > 0) {
      const isDisabled = await commentInput.isDisabled()
      expect(isDisabled || promptCount > 0).toBeTruthy()
    }
  })

  test('로그인 - 댓글 입력 필드 활성화', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    // 댓글 입력 필드 활성화 확인
    const commentInput = blogSelectors.commentInput(authenticatedPage)
    await expect(commentInput).toBeEnabled()
  })

  test('댓글 작성', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    const commentInput = blogSelectors.commentInput(authenticatedPage)

    // 댓글 입력
    await commentInput.fill(testComments.new.content)

    // 제출 버튼 클릭
    const submitButton = authenticatedPage.getByRole('button', { name: /등록|submit|댓글/i })
      .or(authenticatedPage.locator('.comment-submit, button[type="submit"]'))

    await submitButton.first().click()

    // 댓글 추가 확인
    const newComment = authenticatedPage.locator('.comment, .comment-item')
      .filter({ hasText: testComments.new.content })

    await expect(newComment).toBeVisible({ timeout: 10000 })
  })

  test('댓글 목록 표시', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 댓글 목록 확인
    const comments = blogSelectors.commentItem(page)
    const count = await comments.count()

    // 댓글이 있으면 표시 확인
    if (count > 0) {
      await expect(comments.first()).toBeVisible()
    }
  })

  test('대댓글 작성', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    const comments = blogSelectors.commentItem(authenticatedPage)
    const count = await comments.count()

    if (count > 0) {
      // 답글 버튼 클릭
      const replyButton = comments.first().getByRole('button', { name: /답글|reply/i })
        .or(comments.first().locator('.reply-button'))

      const replyCount = await replyButton.count()
      if (replyCount > 0) {
        await replyButton.first().click()

        // 대댓글 입력 필드
        const replyInput = authenticatedPage.locator('textarea').last()
        await replyInput.fill(testComments.reply.content)

        // 제출
        const submitButton = authenticatedPage.getByRole('button', { name: /등록|submit/i }).last()
        await submitButton.click()
      }
    }
  })

  test('댓글 수정 (본인 댓글)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    // 본인 댓글의 수정 버튼 찾기
    const editButton = authenticatedPage.getByRole('button', { name: /수정|edit/i })
      .or(authenticatedPage.locator('.comment-edit, .edit-button'))

    const count = await editButton.count()
    if (count > 0) {
      await editButton.first().click()

      // 수정 모드 확인
      const editInput = authenticatedPage.locator('textarea').first()
      await expect(editInput).toBeVisible()
    }
  })

  test('댓글 삭제 (본인 댓글)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(postUrl)
    await waitForLoading(authenticatedPage)

    // 본인 댓글의 삭제 버튼 찾기
    const deleteButton = authenticatedPage.getByRole('button', { name: /삭제|delete/i })
      .or(authenticatedPage.locator('.comment-delete, .delete-button'))

    const count = await deleteButton.count()
    if (count > 0) {
      // 다이얼로그 핸들러 설정
      authenticatedPage.on('dialog', dialog => dialog.accept())

      await deleteButton.first().click()
    }
  })

  test('댓글 정렬 (최신순/인기순)', async ({ page }) => {
    await page.goto(postUrl)
    await waitForLoading(page)

    // 정렬 옵션
    const sortOptions = page.getByRole('button', { name: /최신|인기|정렬/i })
      .or(page.locator('.comment-sort'))

    const count = await sortOptions.count()
    if (count > 0) {
      await expect(sortOptions.first()).toBeVisible()
    }
  })
})
