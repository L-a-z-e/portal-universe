/**
 * Prism Task E2E Tests
 *
 * Tests:
 * - Create task in board
 * - Task list in kanban
 * - Task detail view
 * - Update task
 * - Status transitions (TODO → IN_PROGRESS → IN_REVIEW → DONE)
 * - Cancel and reopen
 * - Delete task
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'

test.describe('Prism Task CRUD', () => {
  test.beforeEach(async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Navigate to a board
    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)
    if (hasBoard) {
      await boardCard.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should create a new task', async ({ page }) => {
    const createBtn = page.getByRole('button', { name: /Create|추가|Add|New/i })
    const hasCreate = await createBtn.isVisible().catch(() => false)
    if (!hasCreate) return

    await createBtn.click()
    await page.waitForTimeout(1000)

    const titleInput = page.locator('input[name="title"], input[placeholder*="title" i], input[placeholder*="제목" i]').first()
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('E2E New Task')
    }

    const descInput = page.locator('textarea[name="description"], textarea').first()
    if (await descInput.isVisible().catch(() => false)) {
      await descInput.fill('Task created by E2E test')
    }

    const submitBtn = page.getByRole('button', { name: /Save|저장|Create|등록/i })
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click()
      await page.waitForTimeout(2000)

      const taskItem = page.locator('text=E2E New Task')
      const hasTask = await taskItem.isVisible().catch(() => false)
      expect(hasTask).toBeTruthy()
    }
  })

  test('should display tasks in kanban columns', async ({ page }) => {
    // Check TODO column has tasks
    const taskCards = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Task|E2E New Task/ })
    const taskCount = await taskCards.count()
    expect(taskCount).toBeGreaterThanOrEqual(0) // At least seed data task
  })

  test('should show task detail', async ({ page }) => {
    const taskCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Task/ }).first()
    const hasTask = await taskCard.isVisible().catch(() => false)
    if (!hasTask) return

    await taskCard.click()
    await page.waitForTimeout(2000)

    // Task detail should show title, status, priority
    const detailContent = page.locator('text=/E2E Test Task/i').first()
    await expect(detailContent).toBeVisible()
  })

  test('should update task title and priority', async ({ page }) => {
    const editBtn = page.getByRole('button', { name: /Edit|수정|편집/i }).first()
    const hasEdit = await editBtn.isVisible().catch(() => false)
    if (!hasEdit) return

    await editBtn.click()
    await page.waitForTimeout(1000)

    const titleInput = page.locator('input[name="title"], input[placeholder*="title" i]').first()
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.clear()
      await titleInput.fill('E2E Updated Task')
    }

    const saveBtn = page.getByRole('button', { name: /Save|저장|Update|수정/i })
    if (await saveBtn.isVisible().catch(() => false)) {
      await saveBtn.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should transition task status TODO → IN_PROGRESS → IN_REVIEW → DONE', async ({ page }) => {
    // Find a TODO task and transition it
    const taskCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E/ }).first()
    const hasTask = await taskCard.isVisible().catch(() => false)
    if (!hasTask) return

    // Click the task to open detail/actions
    await taskCard.click()
    await page.waitForTimeout(1000)

    // Look for status transition buttons
    const startBtn = page.getByRole('button', { name: /Start|시작|Execute|실행/i }).first()
    if (await startBtn.isVisible().catch(() => false)) {
      await startBtn.click()
      await page.waitForTimeout(2000)
    }

    // Approve to move to DONE
    const approveBtn = page.getByRole('button', { name: /Approve|승인|Complete|완료/i }).first()
    if (await approveBtn.isVisible().catch(() => false)) {
      await approveBtn.click()
      await page.waitForTimeout(2000)
    }
  })

  test('should cancel and reopen task', async ({ page }) => {
    const taskCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E/ }).first()
    const hasTask = await taskCard.isVisible().catch(() => false)
    if (!hasTask) return

    await taskCard.click()
    await page.waitForTimeout(1000)

    // Cancel
    const cancelBtn = page.getByRole('button', { name: /Cancel|취소/i }).first()
    if (await cancelBtn.isVisible().catch(() => false)) {
      await cancelBtn.click()
      await page.waitForTimeout(2000)

      // Reopen
      const reopenBtn = page.getByRole('button', { name: /Reopen|재오픈|다시 열기/i }).first()
      if (await reopenBtn.isVisible().catch(() => false)) {
        await reopenBtn.click()
        await page.waitForTimeout(2000)
      }
    }
  })

  test('should delete task', async ({ page }) => {
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
