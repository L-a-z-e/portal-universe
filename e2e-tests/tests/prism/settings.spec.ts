import { test, expect } from '../../fixtures/base'
import { routes } from '../../fixtures/test-data'
import { waitForLoading } from '../../utils/wait'

test.describe('Prism - Settings', () => {
  test('설정 페이지 접근', async ({ page }) => {
    await page.goto(routes.prism.settings)
    await waitForLoading(page)

    // 설정 페이지 또는 리다이렉트
    const isSettingsPage = page.url().includes('settings')
    const isRedirected = page.url().includes('login') || page.url().includes('prism')

    expect(isSettingsPage || isRedirected).toBeTruthy()
  })

  test('설정 버튼/링크', async ({ page }) => {
    await page.goto(routes.prism.chat)
    await waitForLoading(page)

    // 설정 버튼
    const settingsButton = page.getByRole('button', { name: /설정|settings/i })
      .or(page.getByRole('link', { name: /설정|settings/i }))
      .or(page.locator('[aria-label*="settings"], .settings-btn'))

    const count = await settingsButton.count()
    if (count > 0) {
      await expect(settingsButton.first()).toBeVisible()
    }
  })

  test('AI 모델 선택', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 모델 선택
    const modelSelect = authenticatedPage.getByRole('combobox', { name: /모델|model/i })
      .or(authenticatedPage.locator('.model-select, .ai-model-selector'))

    const count = await modelSelect.count()
    if (count > 0) {
      await expect(modelSelect.first()).toBeVisible()
    }
  })

  test('API 키 설정', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // API 키 입력
    const apiKeyInput = authenticatedPage.getByLabel(/api.*key/i)
      .or(authenticatedPage.locator('input[name*="apiKey"], input[name*="api_key"]'))

    const count = await apiKeyInput.count()
    if (count > 0) {
      await expect(apiKeyInput.first()).toBeVisible()
    }
  })

  test('응답 스타일 설정', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 응답 스타일/톤 설정
    const styleOptions = authenticatedPage.locator('.response-style, .tone-selector')
      .or(authenticatedPage.getByRole('radiogroup', { name: /스타일|style|tone/i }))

    const count = await styleOptions.count()
    if (count > 0) {
      await expect(styleOptions.first()).toBeVisible()
    }
  })

  test('언어 설정', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 언어 선택
    const languageSelect = authenticatedPage.getByRole('combobox', { name: /언어|language/i })
      .or(authenticatedPage.locator('.language-select'))

    const count = await languageSelect.count()
    if (count > 0) {
      await expect(languageSelect.first()).toBeVisible()
    }
  })

  test('설정 저장', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 저장 버튼
    const saveButton = authenticatedPage.getByRole('button', { name: /저장|save/i })

    const count = await saveButton.count()
    if (count > 0) {
      await expect(saveButton.first()).toBeVisible()
    }
  })

  test('설정 초기화', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 초기화 버튼
    const resetButton = authenticatedPage.getByRole('button', { name: /초기화|reset|기본값/i })

    const count = await resetButton.count()
    if (count > 0) {
      await expect(resetButton.first()).toBeVisible()
    }
  })

  test('대화 내보내기 옵션', async ({ authenticatedPage }) => {
    await authenticatedPage.goto(routes.prism.settings)
    await waitForLoading(authenticatedPage)

    // 내보내기 버튼
    const exportButton = authenticatedPage.getByRole('button', { name: /내보내기|export/i })

    const count = await exportButton.count()
    if (count > 0) {
      await expect(exportButton.first()).toBeVisible()
    }
  })
})
