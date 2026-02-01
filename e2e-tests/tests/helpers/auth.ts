import { Page } from '@playwright/test'

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
  // Check if login modal is showing
  const loginModal = page.locator('h3:has-text("로그인")')
  const isModalVisible = await loginModal.isVisible({ timeout: 2000 }).catch(() => false)

  if (isModalVisible) {
    // Fill and submit login form
    await page.locator('input[placeholder="your@email.com"]').first().fill('test@example.com')
    await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill('password123')
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    // Wait for login to complete
    await page.locator('button:has-text("Logout")').waitFor({ state: 'visible', timeout: 10000 }).catch(() => {})
    return
  }

  // Check if Login button is in sidebar (no modal yet)
  const isLoggedIn = await page.locator('button:has-text("Logout")').isVisible()
  if (!isLoggedIn) {
    const loginButton = page.locator('button:has-text("Login")')
    if (await loginButton.isVisible()) {
      await loginButton.click()

      // Wait for modal
      await page.locator('h3:has-text("로그인")').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})

      // Fill and submit
      await page.locator('input[placeholder="your@email.com"]').first().fill('test@example.com')
      await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill('password123')
      await page.getByRole('button', { name: '로그인', exact: true }).click()

      // Wait for login to complete
      await page.locator('button:has-text("Logout")').waitFor({ state: 'visible', timeout: 10000 }).catch(() => {})
    }
  }
}

/**
 * Navigate to a shopping page with full auth handling and Module Federation loading.
 *
 * Handles:
 * 1. Auth state resolution (waits for refresh token flow)
 * 2. Re-login if refresh token was invalidated (token rotation)
 * 3. Login modal dismissal
 * 4. Module Federation remote content loading
 * 5. Spinner wait
 */
/**
 * Navigate to a blog page with full auth handling and Module Federation loading.
 */
export async function gotoBlogPage(page: Page, urlPath: string, contentSelector?: string): Promise<void> {
  await page.goto(urlPath)

  // Wait for auth state to resolve
  await waitForAuthReady(page)

  // If not authenticated, try to login
  await ensureAuthenticated(page)

  // Wait a moment for the page to re-render after auth change
  await page.waitForTimeout(1000)

  // Wait for Module Federation content
  if (contentSelector) {
    await page.locator(contentSelector).first().waitFor({ timeout: 15000 }).catch(() => {})
  }

  // Wait for spinners to finish
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
}

export async function gotoPrismPage(page: Page, urlPath: string, contentSelector?: string): Promise<void> {
  await page.goto(urlPath)

  // Wait for auth state to resolve
  await waitForAuthReady(page)

  // If not authenticated, try to login
  await ensureAuthenticated(page)

  // Wait a moment for the page to re-render after auth change
  await page.waitForTimeout(1000)

  // Wait for Module Federation content
  if (contentSelector) {
    await page.locator(contentSelector).first().waitFor({ timeout: 15000 }).catch(() => {})
  }

  // Wait for spinners to finish
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
}

export async function gotoShoppingPage(page: Page, urlPath: string, contentSelector?: string): Promise<void> {
  await page.goto(urlPath)

  // Wait for auth state to resolve
  await waitForAuthReady(page)

  // If not authenticated, try to login
  await ensureAuthenticated(page)

  // Wait a moment for the page to re-render after auth change
  await page.waitForTimeout(1000)

  // Wait for Module Federation content
  if (contentSelector) {
    await page.locator(contentSelector).first().waitFor({ timeout: 15000 }).catch(() => {})
  }

  // Wait for spinners to finish
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})
}
