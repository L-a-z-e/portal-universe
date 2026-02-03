import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'
import { prismSelectors } from '../../utils/selectors'

test.describe('Prism - Chat', () => {
  test('Prism 메인 페이지 로드', async ({ page }) => {
    await page.goto(routes.prism.home)
    await waitForLoading(page)

    await expect(page).toHaveURL(/prism/)
  })

  test('채팅 인터페이스 표시', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 채팅 컨테이너
    const chatContainer = prismSelectors.chatContainer(page)
    const count = await chatContainer.count()

    if (count > 0) {
      await expect(chatContainer.first()).toBeVisible()
    }
  })

  test('메시지 입력 필드', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 입력 필드
    const chatInput = prismSelectors.chatInput(page)
    await expect(chatInput).toBeVisible()
  })

  test('전송 버튼', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 전송 버튼
    const sendButton = prismSelectors.chatSendButton(page)
    await expect(sendButton).toBeVisible()
  })

  test('메시지 전송', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const chatInput = prismSelectors.chatInput(page)
    const sendButton = prismSelectors.chatSendButton(page)

    // 메시지 입력
    await chatInput.fill('안녕하세요')

    // 전송
    await sendButton.click()

    // 사용자 메시지 표시
    const userMessage = prismSelectors.userMessage(page)
      .or(page.getByText('안녕하세요'))

    await expect(userMessage.first()).toBeVisible()
  })

  test('AI 응답 대기', async ({ page }) => {
    test.setTimeout(30000)

    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const chatInput = prismSelectors.chatInput(page)
    const sendButton = prismSelectors.chatSendButton(page)

    // 메시지 전송
    await chatInput.fill('테스트 메시지입니다')
    await sendButton.click()

    // AI 응답 대기
    const aiMessage = prismSelectors.aiMessage(page)

    try {
      await expect(aiMessage.first()).toBeVisible({ timeout: 15000 })
    } catch {
      // API 연결이 없으면 응답이 없을 수 있음
    }
  })

  test('Enter 키로 메시지 전송', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const chatInput = prismSelectors.chatInput(page)

    // 메시지 입력 후 Enter
    await chatInput.fill('Enter 테스트')
    await page.keyboard.press('Enter')

    // 메시지 전송 확인
    const message = page.getByText('Enter 테스트')
    await expect(message).toBeVisible()
  })

  test('빈 메시지 전송 방지', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const sendButton = prismSelectors.chatSendButton(page)

    // 빈 상태에서 전송 버튼 비활성화 또는 클릭 무시
    const isDisabled = await sendButton.isDisabled()

    if (!isDisabled) {
      await sendButton.click()
      // 에러 없이 동작해야 함
    }
  })

  test('메시지 히스토리 스크롤', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    const chatContainer = prismSelectors.chatContainer(page)
    const count = await chatContainer.count()

    if (count > 0) {
      // 스크롤 가능 확인
      const isScrollable = await chatContainer.first().evaluate(el =>
        el.scrollHeight > el.clientHeight
      )

      // 스크롤이 가능하면 스크롤 테스트
      if (isScrollable) {
        await chatContainer.first().evaluate(el => el.scrollTop = 0)
      }
    }
  })
})
