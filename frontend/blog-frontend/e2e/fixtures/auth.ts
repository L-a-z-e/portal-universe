import { Page } from '@playwright/test'

/**
 * Mock authentication for tests
 */
export async function mockLogin(page: Page) {
  // Set mock auth token in localStorage
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
  await page.evaluate(() => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user')
  })
}

export async function isLoggedIn(page: Page): Promise<boolean> {
  return await page.evaluate(() => {
    return !!localStorage.getItem('auth_token')
  })
}
