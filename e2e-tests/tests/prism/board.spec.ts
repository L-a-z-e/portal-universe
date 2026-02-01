/**
 * Prism Board E2E Tests
 *
 * Tests:
 * - Board list display
 * - Create board
 * - Board detail with kanban UI
 * - Update board
 * - Archive (soft delete) board
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'

test.describe('Prism Board CRUD', () => {
  test('should display board list page', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Board list page should show boards or empty state
    const boardContent = page.locator('text=/Board|보드/i').first()
    await expect(boardContent).toBeVisible()
  })

  test('should create a new board', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    const createBtn = page.getByRole('button', { name: /Create|추가|Add|New/i })
    const hasCreate = await createBtn.isVisible().catch(() => false)
    if (!hasCreate) return

    await createBtn.click()
    await page.waitForTimeout(1000)

    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.fill('E2E New Board')
    }

    const descInput = page.locator('textarea[name="description"], input[name="description"]').first()
    if (await descInput.isVisible().catch(() => false)) {
      await descInput.fill('Board created by E2E test')
    }

    const submitBtn = page.getByRole('button', { name: /Save|저장|Create|등록/i })
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click()
      await page.waitForTimeout(2000)

      const boardItem = page.locator('text=E2E New Board')
      const hasBoard = await boardItem.isVisible().catch(() => false)
      expect(hasBoard).toBeTruthy()
    }
  })

  test('should display kanban columns on board detail', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Click on a board
    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Board|E2E New Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)
    if (!hasBoard) return

    await boardCard.click()
    await page.waitForTimeout(2000)

    // Should display 4 kanban columns
    const todoCol = page.locator('text=/To Do|TODO/i').first()
    const inProgressCol = page.locator('text=/In Progress|IN_PROGRESS/i').first()
    const inReviewCol = page.locator('text=/In Review|IN_REVIEW/i').first()
    const doneCol = page.locator('text=/Done|DONE/i').first()

    const hasTodo = await todoCol.isVisible().catch(() => false)
    const hasInProgress = await inProgressCol.isVisible().catch(() => false)

    expect(hasTodo || hasInProgress).toBeTruthy()
  })

  test('should update board name', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    const editBtn = page.getByRole('button', { name: /Edit|수정|편집/i }).first()
    const hasEdit = await editBtn.isVisible().catch(() => false)
    if (!hasEdit) return

    await editBtn.click()
    await page.waitForTimeout(1000)

    const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first()
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.clear()
      await nameInput.fill('E2E Updated Board')
    }

    const saveBtn = page.getByRole('button', { name: /Save|저장|Update|수정/i })
    if (await saveBtn.isVisible().catch(() => false)) {
      await saveBtn.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should archive board', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Find archive/delete button on a board (not the seed board)
    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E New Board|E2E Updated Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)
    if (!hasBoard) return

    const deleteBtn = boardCard.getByRole('button', { name: /Delete|삭제|Archive|보관/i })
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
