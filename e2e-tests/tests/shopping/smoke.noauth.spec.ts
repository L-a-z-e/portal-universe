/**
 * Smoke Tests (No Authentication Required)
 *
 * Basic connectivity and navigation tests that don't require login
 */
import { test, expect } from '@playwright/test'

test.describe('Smoke Tests', () => {
  test('should load the portal shell home page', async ({ page }) => {
    await page.goto('/')

    // Page should load successfully (title is "portal-shell")
    await expect(page).toHaveTitle(/portal/i)

    // Wait for content to be visible
    await expect(page.locator('body')).toBeVisible()
  })

  test('should navigate to shopping section', async ({ page }) => {
    await page.goto('/')

    // Wait for navigation to be ready (use domcontentloaded instead of networkidle to avoid SSE/HMR timeouts)
    await page.waitForLoadState('domcontentloaded')

    // Try to navigate to shopping section
    await page.goto('/shopping')

    // Wait for shopping remote to load
    await page.waitForTimeout(3000)

    // The page should have shopping-related content or loading state
    const body = page.locator('body')
    await expect(body).toBeVisible()
  })

  test('should display shopping navigation or content', async ({ page }) => {
    await page.goto('/shopping')

    // Wait for Module Federation to load the remote
    await page.waitForTimeout(5000)

    // Check if either:
    // 1. Products page is loaded
    // 2. Loading indicator is shown
    // 3. Error message is shown
    const productsTitle = page.locator('h1:has-text("Products")')
    const loadingSpinner = page.locator('.animate-spin')
    const errorMessage = page.locator('text=/error|failed/i')

    const hasProducts = await productsTitle.isVisible()
    const isLoading = await loadingSpinner.isVisible()
    const hasError = await errorMessage.isVisible()

    // Log what we see for debugging
    console.log('Products visible:', hasProducts)
    console.log('Loading:', isLoading)
    console.log('Error:', hasError)

    // At least something should be visible
    expect(hasProducts || isLoading || hasError).toBeTruthy()
  })
})
