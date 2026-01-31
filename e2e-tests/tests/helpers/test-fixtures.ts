import { test as base } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const TOKEN_FILE = path.resolve(__dirname, '../.auth/access-token.json')

/**
 * Read the saved access token from auth setup.
 */
function getSavedAccessToken(): string | null {
  try {
    if (!fs.existsSync(TOKEN_FILE)) return null
    const data = JSON.parse(fs.readFileSync(TOKEN_FILE, 'utf-8'))
    if (Date.now() - data.timestamp > 14 * 60 * 1000) return null
    return data.accessToken || null
  } catch {
    return null
  }
}

/**
 * Custom test fixture that intercepts the token refresh API.
 *
 * Problem: Backend uses refresh token rotation, making the storageState's
 * refresh token invalid after first use. This causes 401 errors and
 * login modals in tests.
 *
 * Solution: Intercept the refresh endpoint and return the saved access token.
 * This makes the Portal Shell's authService think the refresh succeeded,
 * setting up proper user state without needing a valid refresh token.
 *
 * Security: No tokens are stored in localStorage. The interception only
 * affects the test browser context.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    const token = getSavedAccessToken()

    if (token) {
      // Intercept the token refresh endpoint
      await page.route('**/auth-service/api/v1/auth/refresh', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              accessToken: token,
              refreshToken: 'e2e-test-refresh-token',
              expiresIn: 900,
            },
          }),
        })
      })
    }

    await use(page)
  },
})

export { expect } from '@playwright/test'
