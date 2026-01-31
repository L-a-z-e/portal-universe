/**
 * Authentication Setup for E2E Tests
 *
 * This file handles the modal-based login flow and saves:
 * 1. Browser storage state (cookies) for the refresh token
 * 2. Access token to a separate file for injection via addInitScript
 *
 * Since the backend uses refresh token rotation, the saved refresh token
 * becomes invalid after first use. Tests inject the access token directly
 * into window.__PORTAL_ACCESS_TOKEN__ to avoid depending on token refresh.
 */
import { test as setup, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const authFile = './tests/.auth/user.json'
const tokenFile = './tests/.auth/access-token.json'

setup('authenticate', async ({ page }) => {
  // Navigate to the portal shell
  await page.goto('/')

  // Wait for the page to load
  await expect(page.locator('body')).toBeVisible()

  // Wait for auth check to complete
  await page.waitForTimeout(2000)

  // Check if already logged in
  const isLoggedIn = await page.locator('button:has-text("Logout")').isVisible()
    .catch(() => false)

  if (!isLoggedIn) {
    // Click on Login button in sidebar
    const loginButton = page.locator('button:has-text("Login")')

    if (await loginButton.isVisible()) {
      await loginButton.click()

      // Wait for login modal to appear
      await expect(page.locator('h3:has-text("로그인")')).toBeVisible({ timeout: 5000 })

      // Fill in the login form in the modal
      await page.locator('input[placeholder="your@email.com"]').first().fill('test@example.com')
      await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill('password123')

      // Click login submit button in modal (exact match to avoid OAuth buttons)
      await page.getByRole('button', { name: '로그인', exact: true }).click()

      // Wait for login to complete
      await page.waitForTimeout(3000)

      // Verify login was successful
      await expect(page.locator('button:has-text("Logout")')).toBeVisible({ timeout: 10000 })
    }
  }

  // Extract the access token from the window global (set by authService after login)
  const accessToken = await page.evaluate(() => {
    return (window as any).__PORTAL_ACCESS_TOKEN__ || null
  })

  // Save storage state (cookies including refresh token)
  await page.context().storageState({ path: authFile })

  // Save access token to a separate file for test injection
  const tokenDir = path.dirname(tokenFile)
  if (!fs.existsSync(tokenDir)) {
    fs.mkdirSync(tokenDir, { recursive: true })
  }
  fs.writeFileSync(tokenFile, JSON.stringify({ accessToken, timestamp: Date.now() }))

  console.log(`Authentication setup completed. Token saved: ${!!accessToken}`)
})
