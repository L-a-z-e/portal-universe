import { Page } from '@playwright/test'
import { defaultTestUser } from '../../fixtures/auth'

/**
 * Wait for the Portal Shell auth state to be resolved.
 */
export async function waitForAuthReady(page: Page, timeout = 10000): Promise<boolean> {
  try {
    await Promise.race([
      page.locator('button:has-text("Logout")').waitFor({ state: 'visible', timeout }),
      page.locator('button:has-text("Login")').waitFor({ state: 'visible', timeout }),
    ])
    return await page.locator('button:has-text("Logout")').isVisible()
  } catch {
    return false
  }
}

/**
 * Perform login via the modal if the user is not authenticated.
 * This handles the case where refresh token rotation invalidated the stored token.
 */
async function ensureAuthenticated(page: Page): Promise<void> {
  // Check if already logged in
  const isLoggedIn = await page.locator('button:has-text("Logout")').isVisible().catch(() => false)
  if (isLoggedIn) return

  // Wait a moment for any modal to appear/settle
  await page.waitForTimeout(1000)

  // Try to find login form elements directly (they will be visible if modal is open)
  const emailInput = page.locator('input[placeholder="your@email.com"], input[type="email"]').first()
  const passwordInput = page.locator('input[placeholder="••••••••"], input[type="password"]').first()
  const loginBtn = page.locator('button').filter({ hasText: /^로그인$/ }).first()

  const hasEmailInput = await emailInput.isVisible().catch(() => false)
  const hasPasswordInput = await passwordInput.isVisible().catch(() => false)

  // If login form is visible (modal is showing), fill it
  if (hasEmailInput || hasPasswordInput) {
    if (hasEmailInput) {
      await emailInput.fill(defaultTestUser.email)
    }
    if (hasPasswordInput) {
      await passwordInput.fill(defaultTestUser.password)
    }

    if (await loginBtn.isVisible().catch(() => false)) {
      await loginBtn.click()
      // Wait for login to complete
      await page.locator('button:has-text("Logout")').waitFor({ state: 'visible', timeout: 15000 }).catch(() => {})
    }
    return
  }

  // Check again if logged in (might have auto-logged in during wait)
  const nowLoggedIn = await page.locator('button:has-text("Logout")').isVisible().catch(() => false)
  if (nowLoggedIn) return

  // No modal visible - try clicking Login button in sidebar
  // Use force:true to click even if intercepted, which will trigger modal
  const loginButton = page.locator('button:has-text("Login")')
  if (await loginButton.isVisible().catch(() => false)) {
    try {
      await loginButton.click({ force: true, timeout: 5000 })
    } catch {
      // If click failed due to interception, modal might already be open
      // Wait and retry authentication
      await page.waitForTimeout(500)
      await ensureAuthenticated(page)
      return
    }

    // Wait for modal to appear
    await page.waitForTimeout(1000)

    // Fill and submit login form
    if (await emailInput.isVisible().catch(() => false)) {
      await emailInput.fill(defaultTestUser.email)
    }
    if (await passwordInput.isVisible().catch(() => false)) {
      await passwordInput.fill(defaultTestUser.password)
    }
    if (await loginBtn.isVisible().catch(() => false)) {
      await loginBtn.click()
      await page.locator('button:has-text("Logout")').waitFor({ state: 'visible', timeout: 15000 }).catch(() => {})
    }
  }
}

/**
 * Navigate to a service page with full auth handling and Module Federation loading.
 *
 * Handles:
 * 1. Auth state resolution (waits for refresh token flow)
 * 2. Re-login if refresh token was invalidated (token rotation)
 * 3. Login modal dismissal
 * 4. Module Federation remote content loading
 * 5. Spinner wait
 */
export async function gotoServicePage(page: Page, urlPath: string, contentSelector?: string): Promise<void> {
  await page.goto(urlPath)

  // Wait for auth state to resolve
  await waitForAuthReady(page)

  // If not authenticated, try to login
  await ensureAuthenticated(page)

  // Wait for page to stabilize after auth change (network idle = no pending requests)
  await page.waitForLoadState('networkidle').catch(() => {})

  // Wait for Module Federation content
  if (contentSelector) {
    await page.locator(contentSelector).first().waitFor({ timeout: 25000 }).catch(() => {})
  }

  // Wait for spinners to finish
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
}

// Backward-compatible aliases
export const gotoBlogPage = gotoServicePage
export const gotoPrismPage = gotoServicePage
export const gotoShoppingPage = gotoServicePage
