import { Page } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:30001'

/**
 * Ensure page is on a valid URL before accessing localStorage
 */
async function ensureValidPage(page: Page): Promise<void> {
  const currentUrl = page.url()
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto(BASE_URL)
    await page.waitForLoadState('domcontentloaded')
  }
}

/**
 * Mock authentication for tests
 */
export async function mockLogin(page: Page) {
  // Set mock auth token in localStorage via addInitScript (runs before page load)
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'mock-jwt-token')
    localStorage.setItem('user', JSON.stringify({
      id: 'test-user-1',
      username: 'testuser',
      email: 'test@example.com',
      nickname: 'Test User',
    }))
  })
}

export async function mockLogout(page: Page) {
  // Ensure we're on a valid page before accessing localStorage
  await ensureValidPage(page)

  await page.evaluate(() => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user')
  })
}

export async function isLoggedIn(page: Page): Promise<boolean> {
  // Ensure we're on a valid page before accessing localStorage
  await ensureValidPage(page)

  return await page.evaluate(() => {
    return !!localStorage.getItem('auth_token')
  })
}
