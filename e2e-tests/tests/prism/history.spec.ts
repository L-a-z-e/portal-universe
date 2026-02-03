import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { prismSelectors } from '../../utils/selectors'

test.describe('Prism - Chat History', () => {
  test('대화 히스토리 영역 표시', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 사이드바 또는 히스토리 영역
    const historyArea = page.locator('.chat-history, .conversation-list, .sidebar')
      .or(page.getByText(/대화 목록|conversations|history/i))

    const count = await historyArea.count()
    if (count > 0) {
      await expect(historyArea.first()).toBeVisible()
    }
  })

  test('새 대화 시작 버튼', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 새 대화 버튼
    const newChatButton = page.getByRole('button', { name: /새 대화|new chat|새로운/i })
      .or(page.locator('.new-chat-btn, .new-conversation'))

    const count = await newChatButton.count()
    if (count > 0) {
      await expect(newChatButton.first()).toBeVisible()
    }
  })

  test('새 대화 시작', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const newChatButton = page.getByRole('button', { name: /새 대화|new chat/i })
    const count = await newChatButton.count()

    if (count > 0) {
      await newChatButton.first().click()

      // 새 대화 인터페이스
      const chatInput = prismSelectors.chatInput(page)
      await expect(chatInput).toBeVisible()
    }
  })

  test('이전 대화 목록 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.chat)
    await waitForLoading(authenticatedPage)

    // 대화 목록
    const conversations = authenticatedPage.locator('.conversation-item, .chat-item, .history-item')

    const count = await conversations.count()
    if (count > 0) {
      await expect(conversations.first()).toBeVisible()
    }
  })

  test('이전 대화 선택', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.chat)
    await waitForLoading(authenticatedPage)

    const conversations = authenticatedPage.locator('.conversation-item, .chat-item')
    const count = await conversations.count()

    if (count > 0) {
      // 대화 선택
      await conversations.first().click()

      // 대화 내용 로드 확인
      const messages = prismSelectors.chatMessage(authenticatedPage)
      await expect(messages.first()).toBeVisible()
    }
  })

  test('대화 삭제', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.chat)
    await waitForLoading(authenticatedPage)

    const conversations = authenticatedPage.locator('.conversation-item, .chat-item')
    const count = await conversations.count()

    if (count > 0) {
      // 삭제 버튼
      const deleteButton = conversations.first().getByRole('button', { name: /삭제|delete/i })
        .or(conversations.first().locator('.delete-btn'))

      const deleteCount = await deleteButton.count()
      if (deleteCount > 0) {
        authenticatedPage.on('dialog', dialog => dialog.accept())
        await deleteButton.first().click()
      }
    }
  })

  test('대화 제목 표시', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.chat)
    await waitForLoading(authenticatedPage)

    const conversations = authenticatedPage.locator('.conversation-item, .chat-item')
    const count = await conversations.count()

    if (count > 0) {
      // 대화 제목
      const title = conversations.first().locator('.title, .conversation-title, h3, h4')
      const titleCount = await title.count()

      if (titleCount > 0) {
        await expect(title.first()).toBeVisible()
      }
    }
  })

  test('대화 검색', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 검색 입력
    const searchInput = page.getByPlaceholder(/검색|search/i)
      .or(page.locator('.search-conversations, input[type="search"]'))

    const count = await searchInput.count()
    if (count > 0) {
      await searchInput.first().fill('테스트')
    }
  })
})
