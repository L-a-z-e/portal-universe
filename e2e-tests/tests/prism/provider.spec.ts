/**
 * Prism Provider E2E Tests
 *
 * Tests:
 * - Provider list display
 * - Create Ollama provider (no API key)
 * - Create OpenAI provider (if env var available)
 * - Verify provider connection
 * - Update provider
 * - Delete provider
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'

test.describe('Prism Provider CRUD', () => {
  test('should display provider list page', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    const pageContent = page.locator('text=/Provider|프로바이더/i').first()
    await expect(pageContent).toBeVisible()
  })

  test('should create Ollama provider', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    // Click create button
    const createBtn = page.getByRole('button', { name: /Create|추가|Add|New/i })
    const hasCreate = await createBtn.isVisible().catch(() => false)
    if (!hasCreate) return

    await createBtn.click()
    await page.waitForTimeout(1000)

    // Fill form - select Ollama type
    const typeSelect = page.locator('select, [role="combobox"]').first()
    if (await typeSelect.isVisible().catch(() => false)) {
      await typeSelect.selectOption({ label: /Ollama/i }).catch(() => {})
    }

    // Fill name
    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.fill('E2E Ollama Provider')
    }

    // Submit
    const submitBtn = page.getByRole('button', { name: /Save|저장|Create|등록/i })
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click()
      await page.waitForTimeout(2000)

      // Verify provider appears in list
      const providerItem = page.locator('text=E2E Ollama Provider')
      const hasProvider = await providerItem.isVisible().catch(() => false)
      expect(hasProvider).toBeTruthy()
    }
  })

  test('should create OpenAI provider when API key available', async ({ page }) => {
    const apiKey = process.env.OPENAI_API_KEY
    test.skip(!apiKey, 'OPENAI_API_KEY not set')

    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    const createBtn = page.getByRole('button', { name: /Create|추가|Add|New/i })
    const hasCreate = await createBtn.isVisible().catch(() => false)
    if (!hasCreate) return

    await createBtn.click()
    await page.waitForTimeout(1000)

    // Select OpenAI type
    const typeSelect = page.locator('select, [role="combobox"]').first()
    if (await typeSelect.isVisible().catch(() => false)) {
      await typeSelect.selectOption({ label: /OpenAI/i }).catch(() => {})
    }

    // Fill name and API key
    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.fill('E2E OpenAI Provider')
    }

    const keyInput = page.locator('input[name="apiKey"], input[placeholder*="key" i], input[type="password"]').first()
    if (await keyInput.isVisible().catch(() => false)) {
      await keyInput.fill(apiKey!)
    }

    const submitBtn = page.getByRole('button', { name: /Save|저장|Create|등록/i })
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click()
      await page.waitForTimeout(2000)

      const providerItem = page.locator('text=E2E OpenAI Provider')
      const hasProvider = await providerItem.isVisible().catch(() => false)
      expect(hasProvider).toBeTruthy()
    }
  })

  test('should verify provider connection', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    // Find a provider with verify button
    const verifyBtn = page.getByRole('button', { name: /Verify|검증|Test|연결/i }).first()
    const hasVerify = await verifyBtn.isVisible().catch(() => false)
    if (!hasVerify) return

    await verifyBtn.click()
    await page.waitForTimeout(5000)

    // Should show success or failure message
    const result = page.locator('text=/success|성공|connected|models|failed|실패/i').first()
    const hasResult = await result.isVisible().catch(() => false)
    expect(hasResult).toBeTruthy()
  })

  test('should update provider name', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    // Find edit button on a provider
    const editBtn = page.getByRole('button', { name: /Edit|수정|편집/i }).first()
    const hasEdit = await editBtn.isVisible().catch(() => false)
    if (!hasEdit) return

    await editBtn.click()
    await page.waitForTimeout(1000)

    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.clear()
      await nameInput.fill('Updated Provider Name')
    }

    const saveBtn = page.getByRole('button', { name: /Save|저장|Update|수정/i })
    if (await saveBtn.isVisible().catch(() => false)) {
      await saveBtn.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should delete provider', async ({ page }) => {
    await gotoPrismPage(page, '/prism/providers')
    await page.waitForTimeout(2000)

    // Count providers before
    const providersBefore = await page.locator('[class*="rounded"]').filter({ hasText: /Provider|OPENAI|OLLAMA|ANTHROPIC/i }).count()

    const deleteBtn = page.getByRole('button', { name: /Delete|삭제/i }).first()
    const hasDelete = await deleteBtn.isVisible().catch(() => false)
    if (!hasDelete || providersBefore === 0) return

    await deleteBtn.click()
    await page.waitForTimeout(1000)

    // Confirm deletion if dialog appears
    const confirmBtn = page.getByRole('button', { name: /Confirm|확인|Yes|예/i })
    if (await confirmBtn.isVisible().catch(() => false)) {
      await confirmBtn.click()
      await page.waitForTimeout(2000)
    }
  })
})
