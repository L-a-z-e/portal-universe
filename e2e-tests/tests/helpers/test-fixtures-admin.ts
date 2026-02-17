import { test as base, type Page, expect as baseExpect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'
import { adminTestUser } from '../../fixtures/auth'

const TOKEN_FILE = path.resolve(__dirname, '../.auth/admin-access-token.json')

export function getSavedAdminAccessToken(): string | null {
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
 * Set up route interception for admin auth on any page.
 */
export async function setupAdminRouteInterception(page: Page): Promise<void> {
  const token = getSavedAdminAccessToken()
  if (!token) return

  // Intercept refresh API to return saved token
  await page.route('**/auth-service/api/v1/auth/refresh', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: token,
          refreshToken: 'e2e-admin-test-refresh-token',
          expiresIn: 900,
        },
      }),
    })
  })

  // Add Authorization header to all backend API requests that lack it
  await page.route('**/*-service/api/**', async (route) => {
    const request = route.request()
    const url = request.url()
    if (url.includes('/auth/refresh')) {
      await route.fallback()
      return
    }
    const headers = request.headers()
    if (!headers['authorization']) {
      await route.fallback({
        headers: { ...headers, 'authorization': `Bearer ${token}` },
      })
    } else {
      await route.fallback()
    }
  })

  // Inject token into window globals before any script runs
  await page.addInitScript((t: string) => {
    (window as any).__PORTAL_ACCESS_TOKEN__ = t
    let _getter = () => t
    Object.defineProperty(window, '__PORTAL_GET_ACCESS_TOKEN__', {
      get() { return _getter },
      set(newFn) {
        const origFn = newFn
        _getter = () => {
          const result = origFn?.()
          return result || t
        }
      },
      configurable: true,
    })
  }, token)
}

/**
 * Handle login modal if it appears due to auth timing.
 * Performs actual login as admin if modal is visible.
 */
export async function handleLoginModalIfVisible(page: Page): Promise<void> {
  try {
    const modal = page.getByRole('heading', { name: '로그인', exact: true })
    if (await modal.isVisible({ timeout: 2000 })) {
      await page.locator('input[placeholder="your@email.com"]').first().fill(adminTestUser.email)
      await page.locator('input[placeholder="••••••••"], input[type="password"]').first().fill(adminTestUser.password)
      await page.getByRole('button', { name: '로그인', exact: true }).click()
      await page.waitForTimeout(3000)
    }
  } catch {
    // No modal, continue
  }
}

/**
 * Navigate to an admin page with auth-ready guarantee.
 *
 * checkAuth()가 완료 시 portal:auth-changed 이벤트를 dispatch하고,
 * RequireAuth가 해당 이벤트를 받아 재동기화하므로 단순 대기로 충분.
 */
export async function navigateToAdminPage(page: Page, adminPath: string): Promise<void> {
  await page.goto(adminPath)
  await page.waitForLoadState('domcontentloaded')

  // Portal Shell auth 완료 대기 (Login 버튼 사라짐 = 인증 완료)
  try {
    await page.locator('button:has-text("Login")').waitFor({ state: 'hidden', timeout: 15000 })
  } catch {
    await handleLoginModalIfVisible(page)
    await page.locator('button:has-text("Login")').waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
  }

  // 로딩 스피너 대기
  await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

  // RequireAuth가 auth-changed 이벤트를 받고 재동기화할 시간 대기
  await page.waitForTimeout(2000)

  // 로그인 모달 처리 (fallback)
  await handleLoginModalIfVisible(page)
}

/**
 * Admin test fixture with route interception for token refresh.
 * Uses admin access token (ROLE_SUPER_ADMIN).
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    await setupAdminRouteInterception(page)
    await use(page)
  },
})

export { expect } from '@playwright/test'
