/**
 * Prism Navigation E2E Tests
 *
 * Tests:
 * - Portal Shell sidebar has Prism menu
 * - Prism page loads via Module Federation
 * - Internal routing (boards, agents, providers)
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'

test.describe('Prism Navigation', () => {
  test('should display Prism menu in Portal Shell sidebar', async ({ page }) => {
    await gotoPrismPage(page, '/prism')

    // Sidebar uses <button> elements for navigation (vue-router push)
    const prismButton = page.locator('button').filter({ hasText: /^Prism$/i }).first()
      .or(page.locator('button').filter({ hasText: /🤖/ }).first())

    const hasPrism = await prismButton.isVisible().catch(() => false)
    expect(hasPrism).toBeTruthy()
  })

  test('should load Prism page via Module Federation', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Prism app should be mounted - look for board list or Prism heading
    const prismContent = page.locator('text=/Board|보드|Prism/i').first()
    const hasContent = await prismContent.isVisible().catch(() => false)
    expect(hasContent).toBeTruthy()
  })

  test('should navigate to board detail page', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // If there's a board, click it
    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)

    if (hasBoard) {
      await boardCard.click()
      await page.waitForTimeout(2000)

      // Should see kanban columns
      const kanbanColumn = page.locator('text=/To Do|TODO|In Progress/i').first()
      const hasKanban = await kanbanColumn.isVisible().catch(() => false)
      expect(hasKanban).toBeTruthy()
    }
  })

  test('should navigate to Agents page', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    const agentsContent = page.locator('text=/Agent|에이전트/i').first()
    const hasContent = await agentsContent.isVisible().catch(() => false)
    expect(hasContent).toBeTruthy()
  })

  test('should navigate to Providers page', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    const providersContent = page.locator('text=/Provider|프로바이더|AI Provider/i').first()
    const hasContent = await providersContent.isVisible().catch(() => false)
    expect(hasContent).toBeTruthy()
  })
})
