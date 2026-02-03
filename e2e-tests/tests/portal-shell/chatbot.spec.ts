import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Chatbot Widget', () => {
  test('챗봇 위젯 버튼 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 플로팅 버튼 (title="Chat Assistant")
    const chatButton = page.getByTitle('Chat Assistant')
    await expect(chatButton).toBeVisible()
  })

  test('챗봇 위젯 열기', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 플로팅 버튼 클릭
    const chatButton = page.getByTitle('Chat Assistant')
    await chatButton.click()

    // Chat Assistant 패널 표시 확인
    const chatPanel = page.getByText('Chat Assistant').first()
    await expect(chatPanel).toBeVisible()
  })

  test('비로그인 - 로그인 요청 메시지 표시', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 플로팅 버튼 클릭
    const chatButton = page.getByTitle('Chat Assistant')
    await chatButton.click()
    await page.waitForTimeout(500) // 위젯 열림 대기

    // "Please log in" 또는 관련 메시지 확인
    const loginMessage = page.getByText(/Please log in|log in to use/i)
    await expect(loginMessage.first()).toBeVisible()
  })

  test('챗봇 위젯 닫기', async ({ page }) => {
    await page.goto(routes.portal.home)
    await waitForLoading(page)

    // 플로팅 버튼 클릭하여 열기
    const chatButton = page.getByTitle('Chat Assistant')
    await chatButton.click()
    await page.waitForTimeout(300) // 위젯 열림 대기

    // Chat Assistant 패널 표시 확인
    const chatPanel = page.getByText('Chat Assistant').first()
    await expect(chatPanel).toBeVisible()

    // 닫기 버튼 클릭 (같은 플로팅 버튼이 X 아이콘으로 변경됨)
    await chatButton.click()
    await page.waitForTimeout(300)

    // 패널이 사라졌는지 확인 (제목 텍스트가 안 보이면 됨)
    await expect(chatPanel).not.toBeVisible()
  })

  test('Prism 서비스 페이지 접근', async ({ page }) => {
    await page.goto(routes.prism.home)
    await waitForLoading(page)

    // Prism 페이지 로드 확인
    await expect(page).toHaveURL(/\/prism/)
  })

  test('로그인 상태 - 채팅 패널 표시 (Mock)', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.portal.home)
    await waitForLoading(authenticatedPage)

    // 플로팅 버튼 클릭
    const chatButton = authenticatedPage.getByTitle('Chat Assistant')
    await chatButton.click()
    await authenticatedPage.waitForTimeout(500) // 위젯 열림 대기

    // Chat Assistant 패널 확인
    const chatPanel = authenticatedPage.getByText('Chat Assistant').first()
    await expect(chatPanel).toBeVisible()
  })
})
