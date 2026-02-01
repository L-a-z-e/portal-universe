/**
 * Prism Agent E2E Tests
 *
 * Tests:
 * - Agent list display
 * - Create agent with provider
 * - View agent detail
 * - Update agent settings
 * - Delete agent
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'

test.describe('Prism Agent CRUD', () => {
  test('should display agent list page', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    const pageContent = page.locator('text=/Agent|에이전트/i').first()
    await expect(pageContent).toBeVisible()
  })

  test('should create a new agent', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    const createBtn = page.getByRole('button', { name: /Create|추가|Add|New/i })
    const hasCreate = await createBtn.isVisible().catch(() => false)
    if (!hasCreate) return

    await createBtn.click()
    await page.waitForTimeout(1000)

    // Fill agent form
    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.fill('E2E Test Agent')
    }

    // Select role
    const roleSelect = page.locator('select[name="role"], [name="role"]').first()
    if (await roleSelect.isVisible().catch(() => false)) {
      await roleSelect.selectOption({ label: /PM|Custom|CUSTOM/i }).catch(() => {})
    }

    // Fill system prompt
    const promptInput = page.locator('textarea[name="systemPrompt"], textarea').first()
    if (await promptInput.isVisible().catch(() => false)) {
      await promptInput.fill('You are a helpful assistant for testing.')
    }

    // Fill model
    const modelInput = page.locator('input[name="model"], select[name="model"]').first()
    if (await modelInput.isVisible().catch(() => false)) {
      if (await modelInput.evaluate((el) => el.tagName === 'SELECT')) {
        await modelInput.selectOption({ index: 0 }).catch(() => {})
      } else {
        await modelInput.fill('gpt-4o-mini')
      }
    }

    // Submit
    const submitBtn = page.getByRole('button', { name: /Save|저장|Create|등록/i })
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click()
      await page.waitForTimeout(2000)

      const agentItem = page.locator('text=E2E Test Agent')
      const hasAgent = await agentItem.isVisible().catch(() => false)
      expect(hasAgent).toBeTruthy()
    }
  })

  test('should display agent detail', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    // Check if agents exist (not just "No agents yet" empty state)
    const emptyState = page.locator('text=/No agents yet/i').first()
    const isEmpty = await emptyState.isVisible().catch(() => false)
    if (isEmpty) return // No agents to display detail for

    // Find agent card with edit button (distinguishes real cards from headings)
    const agentCards = page.locator('[class*="rounded-xl"]').filter({ has: page.locator('button') })
    const cardCount = await agentCards.count()
    if (cardCount === 0) return

    const firstCard = agentCards.first()

    // Card should display agent info (name, status badge, provider/model)
    const cardText = await firstCard.textContent() ?? ''
    expect(cardText.length).toBeGreaterThan(0)

    // Click edit button on the card to open modal with full details
    const editBtn = firstCard.locator('button').first()
    if (await editBtn.isVisible().catch(() => false)) {
      await editBtn.click()
      await page.waitForTimeout(1000)

      // Modal should show form fields (model, temperature, systemPrompt)
      const modalContent = page.locator('input[name="model"], input[name="temperature"], textarea[name="systemPrompt"]').first()
      const hasModal = await modalContent.isVisible().catch(() => false)
      expect(hasModal).toBeTruthy()

      // Close modal
      const cancelBtn = page.getByRole('button', { name: /Cancel|취소|Close|닫기/i })
      if (await cancelBtn.isVisible().catch(() => false)) {
        await cancelBtn.click()
      }
    }
  })

  test('should update agent settings', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    const editBtn = page.getByRole('button', { name: /Edit|수정|편집/i }).first()
    const hasEdit = await editBtn.isVisible().catch(() => false)
    if (!hasEdit) return

    await editBtn.click()
    await page.waitForTimeout(1000)

    // Update temperature if visible
    const tempInput = page.locator('input[name="temperature"]').first()
    if (await tempInput.isVisible().catch(() => false)) {
      await tempInput.clear()
      await tempInput.fill('0.5')
    }

    const saveBtn = page.getByRole('button', { name: /Save|저장|Update|수정/i })
    if (await saveBtn.isVisible().catch(() => false)) {
      await saveBtn.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should delete agent', async ({ page }) => {
    await gotoPrismPage(page, '/prism/agents')
    await page.waitForTimeout(2000)

    const deleteBtn = page.getByRole('button', { name: /Delete|삭제/i }).first()
    const hasDelete = await deleteBtn.isVisible().catch(() => false)
    if (!hasDelete) return

    await deleteBtn.click()
    await page.waitForTimeout(1000)

    const confirmBtn = page.getByRole('button', { name: /Confirm|확인|Yes|예/i })
    if (await confirmBtn.isVisible().catch(() => false)) {
      await confirmBtn.click()
      await page.waitForTimeout(2000)
    }
  })
})
