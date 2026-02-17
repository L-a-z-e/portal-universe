import { Page } from '@playwright/test'
import { defaultTestUser } from '../../fixtures/auth'

const LOGIN_BUTTON = 'button:has-text("Login")'

/**
 * Wait for the Portal Shell auth state to be resolved.
 * Returns true if authenticated (Login button absent).
 */
export async function waitForAuthReady(page: Page, timeout = 10000): Promise<boolean> {
  try {
    // Wait for Login button to either appear (not authenticated) or confirm absence (authenticated)
    const loginBtn = page.locator(LOGIN_BUTTON)
    // Give the page time to render auth state
    await page.waitForTimeout(2000)
    const loginVisible = await loginBtn.isVisible({ timeout }).catch(() => false)
    return !loginVisible
  } catch {
    return false
  }
}

/**
 * Perform login via the modal if the user is not authenticated.
 * This handles the case where refresh token rotation invalidated the stored token.
 */
async function ensureAuthenticated(page: Page): Promise<void> {
  // Check if already logged in (Login button not visible = authenticated)
  const loginBtnVisible = await page.locator(LOGIN_BUTTON).isVisible().catch(() => false)
  if (!loginBtnVisible) return

  // Wait for DOM to settle instead of fixed timeout
  await page.waitForLoadState('domcontentloaded')

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
      // Wait for Login button to disappear (auth complete)
      await page.locator(LOGIN_BUTTON).waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})
    }
    return
  }

  // Check again if logged in (might have auto-logged in during wait)
  const stillNeedsLogin = await page.locator(LOGIN_BUTTON).isVisible().catch(() => false)
  if (!stillNeedsLogin) return

  // No modal visible - try clicking Login button to trigger modal
  const loginButton = page.locator(LOGIN_BUTTON)
  if (await loginButton.isVisible().catch(() => false)) {
    try {
      await loginButton.click({ force: true, timeout: 5000 })
    } catch {
      // If click failed due to interception, modal might already be open
      await page.locator('input[type="email"], input[type="password"]').first().waitFor({ timeout: 3000 }).catch(() => {})
      await ensureAuthenticated(page)
      return
    }

    // Wait for modal to appear via selector
    await page.locator('input[type="email"], input[type="password"]').first().waitFor({ timeout: 3000 }).catch(() => {})

    // Fill and submit login form
    if (await emailInput.isVisible().catch(() => false)) {
      await emailInput.fill(defaultTestUser.email)
    }
    if (await passwordInput.isVisible().catch(() => false)) {
      await passwordInput.fill(defaultTestUser.password)
    }
    if (await loginBtn.isVisible().catch(() => false)) {
      await loginBtn.click()
      await page.locator(LOGIN_BUTTON).waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {})
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
  const alreadyAuthed = await waitForAuthReady(page)

  // If not authenticated, try to login
  if (!alreadyAuthed) {
    await ensureAuthenticated(page)
  }

  // Wait for DOM to be ready (networkidle is unreliable in MF environments)
  await page.waitForLoadState('domcontentloaded')

  // Wait for Module Federation content
  if (contentSelector) {
    await page.locator(contentSelector).first().waitFor({ timeout: 15000 }).catch(() => {})
  }

  // Wait for spinners to finish
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 5000 }).catch(() => {})
}

// Backward-compatible aliases
export const gotoBlogPage = gotoServicePage
export const gotoPrismPage = gotoServicePage
export const gotoShoppingPage = gotoServicePage
