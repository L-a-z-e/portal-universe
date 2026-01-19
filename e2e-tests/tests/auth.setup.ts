/**
 * Authentication Setup for E2E Tests
 *
 * This file handles the OIDC login flow and saves the authenticated state
 * to be reused by all tests requiring authentication.
 */
import { test as setup, expect } from '@playwright/test'

const authFile = './tests/.auth/user.json'

setup('authenticate', async ({ page }) => {
  // Navigate to the portal shell
  await page.goto('/')

  // Wait for the page to load
  await expect(page.locator('body')).toBeVisible()

  // Check if already logged in by looking for user indicators
  const isLoggedIn = await page.locator('[data-testid="user-menu"], .user-avatar, text="Logout"').isVisible()
    .catch(() => false)

  if (!isLoggedIn) {
    // Click on Login button
    const loginButton = page.locator('button:has-text("Login"), a:has-text("Login"), [data-testid="login-button"]')

    if (await loginButton.isVisible()) {
      await loginButton.click()

      // Wait for redirect to auth service login page
      await page.waitForURL(/.*8080.*login.*|.*auth-service.*login.*/, { timeout: 30000 })

      // Fill in the login form
      // Note: Adjust these selectors based on your auth-service login page
      const usernameInput = page.locator('input[name="username"], input[type="email"], #username')
      const passwordInput = page.locator('input[name="password"], input[type="password"], #password')

      await usernameInput.fill('test@example.com')
      await passwordInput.fill('password123')

      // Submit the form
      await page.locator('button[type="submit"], button:has-text("Sign in"), button:has-text("Login")').click()

      // Wait for redirect back to portal shell
      await page.waitForURL(/.*localhost:30000.*/, { timeout: 30000 })

      // Wait for authentication to complete
      await page.waitForTimeout(2000) // Allow time for OIDC callback processing
    }
  }

  // Verify login was successful by checking for authenticated state indicators
  // This could be a user menu, profile icon, or specific text
  await expect(page.locator('body')).toBeVisible()

  // Save storage state (localStorage, sessionStorage, cookies)
  await page.context().storageState({ path: authFile })

  console.log('Authentication setup completed successfully')
})
