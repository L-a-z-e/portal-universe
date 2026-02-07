/**
 * Admin Authentication Setup for E2E Tests
 *
 * Logs in as admin@test.com (ROLE_SUPER_ADMIN).
 * Always forces a fresh login to capture the access token from the API response.
 */
import { test as setup, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'
import { adminTestUser } from '../fixtures/auth'

const authFile = './tests/.auth/admin.json'
const tokenFile = './tests/.auth/admin-access-token.json'

setup('authenticate as admin', async ({ page, context }) => {
  let capturedAccessToken: string | null = null

  // Intercept login API response to capture the access token
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/auth-service/api/v1/auth/login') && response.status() === 200) {
      try {
        const body = await response.json()
        if (body?.data?.accessToken) {
          capturedAccessToken = body.data.accessToken
        } else if (body?.accessToken) {
          capturedAccessToken = body.accessToken
        }
      } catch {
        // ignore parse errors
      }
    }
  })

  // Clear cookies to force fresh login
  await context.clearCookies()

  // Navigate to the portal shell
  await page.goto('/')
  await expect(page.locator('body')).toBeVisible()
  await page.waitForTimeout(3000)

  // Perform login
  const loginButton = page.locator('button:has-text("Login")')
  const logoutButton = page.locator('button:has-text("Logout")')

  if (await loginButton.isVisible()) {
    await loginButton.click()
    await expect(page.locator('h3:has-text("로그인")')).toBeVisible({ timeout: 5000 })

    await page.locator('input[placeholder="your@email.com"]').first().fill(adminTestUser.email)
    await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill(adminTestUser.password)

    await page.getByRole('button', { name: '로그인', exact: true }).click()
    await page.waitForTimeout(3000)

    await expect(page.locator('button:has-text("Logout")')).toBeVisible({ timeout: 10000 })
  } else if (await logoutButton.isVisible()) {
    // Already logged in as different user - logout first, then login as admin
    await logoutButton.click()
    await page.waitForTimeout(2000)

    const loginBtn = page.locator('button:has-text("Login")')
    await expect(loginBtn).toBeVisible({ timeout: 5000 })
    await loginBtn.click()
    await expect(page.locator('h3:has-text("로그인")')).toBeVisible({ timeout: 5000 })

    await page.locator('input[placeholder="your@email.com"]').first().fill(adminTestUser.email)
    await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill(adminTestUser.password)

    await page.getByRole('button', { name: '로그인', exact: true }).click()
    await page.waitForTimeout(3000)

    await expect(page.locator('button:has-text("Logout")')).toBeVisible({ timeout: 10000 })
  }

  // Try window global
  const windowToken = await page.evaluate(() => {
    return (window as any).__PORTAL_ACCESS_TOKEN__ ||
           (window as any).__PORTAL_GET_ACCESS_TOKEN__?.() ||
           null
  })

  const accessToken = windowToken || capturedAccessToken

  // Save storage state (cookies including refresh token)
  await page.context().storageState({ path: authFile })

  // Save access token
  const tokenDir = path.dirname(tokenFile)
  if (!fs.existsSync(tokenDir)) {
    fs.mkdirSync(tokenDir, { recursive: true })
  }
  fs.writeFileSync(tokenFile, JSON.stringify({ accessToken, timestamp: Date.now() }))

  console.log(`Admin auth setup: Token saved: ${!!accessToken} (window: ${!!windowToken}, captured: ${!!capturedAccessToken})`)
})
