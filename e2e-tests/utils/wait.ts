import { Page, expect } from '@playwright/test'

/**
 * 로딩 완료 대기
 */
export async function waitForLoading(page: Page): Promise<void> {
  const loading = page.locator('.loading, .spinner, [data-loading="true"]')
  await expect(loading).toHaveCount(0, { timeout: 10000 })
}

/**
 * 네트워크 안정화 대기
 */
export async function waitForNetworkIdle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle')
}

/**
 * API 응답 대기
 */
export async function waitForAPI(
  page: Page,
  urlPattern: string | RegExp
): Promise<void> {
  await page.waitForResponse(
    (response) => {
      if (typeof urlPattern === 'string') {
        return response.url().includes(urlPattern)
      }
      return urlPattern.test(response.url())
    },
    { timeout: 10000 }
  )
}

/**
 * Toast 메시지 확인
 */
export async function expectToast(
  page: Page,
  text: string | RegExp
): Promise<void> {
  const toast = page.locator('.toast, .notification, [role="status"]')
  await expect(toast).toContainText(text, { timeout: 5000 })
}

/**
 * 요소가 나타날 때까지 대기
 */
export async function waitForElement(
  page: Page,
  selector: string,
  timeout: number = 10000
): Promise<void> {
  await page.waitForSelector(selector, { timeout })
}

/**
 * 페이지 URL 확인
 */
export async function expectURL(
  page: Page,
  urlPattern: string | RegExp
): Promise<void> {
  await expect(page).toHaveURL(urlPattern)
}
