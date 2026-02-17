/**
 * Authentication Setup for E2E Tests
 *
 * This file handles the modal-based login flow and saves:
 * 1. Browser storage state (cookies) for the refresh token
 * 2. Access token to a separate file for route interception in tests
 *
 * Since the backend uses refresh token rotation, the saved refresh token
 * becomes invalid after first use. Tests intercept the refresh API to
 * return the saved access token directly.
 */
import { test as setup, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'
import { defaultTestUser } from '../fixtures/auth'

const authFile = './tests/.auth/user.json'
const tokenFile = './tests/.auth/access-token.json'

setup('authenticate', async ({ page }) => {
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

  // Navigate to the portal shell
  await page.goto('/')

  // Wait for the page to load
  await expect(page.locator('body')).toBeVisible()

  // Wait for auth check to complete
  await page.waitForTimeout(2000)

  // Check if already logged in (Login button absent = authenticated)
  const loginButton = page.locator('button:has-text("Login")')
  const isLoggedIn = !(await loginButton.isVisible({ timeout: 3000 }).catch(() => false))

  if (!isLoggedIn) {
    await loginButton.click()

    // Wait for login modal to appear
    await expect(page.getByRole('heading', { name: '로그인', exact: true })).toBeVisible({ timeout: 5000 })

    // Fill in the login form in the modal
    await page.locator('input[placeholder="your@email.com"]').first().fill(defaultTestUser.email)
    await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill(defaultTestUser.password)

    // Click login submit button in modal (exact match to avoid OAuth buttons)
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    // Wait for login to complete — Login button should disappear
    await page.waitForTimeout(3000)
    await expect(page.locator('button:has-text("Login")')).toBeHidden({ timeout: 10000 })
  }

  // Try window global first, then use captured token
  const windowToken = await page.evaluate(() => {
    return (window as any).__PORTAL_ACCESS_TOKEN__ ||
           (window as any).__PORTAL_GET_ACCESS_TOKEN__?.() ||
           null
  })

  const accessToken = windowToken || capturedAccessToken

  // Save storage state (cookies including refresh token)
  await page.context().storageState({ path: authFile })

  // Save access token to a separate file for test injection
  const tokenDir = path.dirname(tokenFile)
  if (!fs.existsSync(tokenDir)) {
    fs.mkdirSync(tokenDir, { recursive: true })
  }
  fs.writeFileSync(tokenFile, JSON.stringify({ accessToken, timestamp: Date.now() }))

  console.log(`Authentication setup completed. Token saved: ${!!accessToken} (window: ${!!windowToken}, captured: ${!!capturedAccessToken})`)
})
