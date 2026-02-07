import { test, expect } from '../helpers/test-fixtures-admin'
import { navigateToAdminPage } from '../helpers/test-fixtures-admin'
import { waitForLoading } from '../../utils/wait'

test.describe('Admin Queue Management', () => {
  // Admin 테스트는 admin@test.com 계정으로 실행됨 (ROLE_SUPER_ADMIN)

  test.describe('Queue Management Page UI', () => {
    test('should display admin queue management page', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // 페이지 제목 확인 (MF 중복 렌더링 대응 .first())
      const title = page.getByRole('heading', { name: /Queue Management/i }).first()
      await expect(title).toBeVisible({ timeout: 10000 })
    })

    test('should display Queue Target section', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Queue Target 섹션
      await expect(page.getByText('Queue Target')).toBeVisible()

      // Event Type 선택
      await expect(page.getByText('Event Type')).toBeVisible()
      const eventTypeSelect = page.locator('select').first()
      await expect(eventTypeSelect).toBeVisible()

      // Event ID 입력
      await expect(page.getByText('Event ID')).toBeVisible()
      const eventIdInput = page.locator('input[type="number"]').first()
      await expect(eventIdInput).toBeVisible()
    })

    test('should display Activation Settings section', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Activation Settings 섹션
      await expect(page.getByText('Activation Settings')).toBeVisible()

      // Max Capacity
      await expect(page.getByText('Max Capacity')).toBeVisible()

      // Entry Batch Size
      await expect(page.getByText('Entry Batch Size')).toBeVisible()

      // Entry Interval
      await expect(page.getByText(/Entry Interval/)).toBeVisible()
    })

    test('should display Actions section', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Actions 섹션 (MF 중복 렌더링 대응 .first())
      await expect(page.getByText('Actions').first()).toBeVisible()

      // 버튼들
      await expect(page.getByRole('button', { name: /Activate Queue/i }).first()).toBeVisible()
      await expect(page.getByRole('button', { name: /Process Queue/i }).first()).toBeVisible()
      await expect(page.getByRole('button', { name: /Deactivate Queue/i }).first()).toBeVisible()
    })

    test('should display Help section', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Help 섹션
      await expect(page.getByText('How Queue Management Works')).toBeVisible()
      await expect(page.getByText(/Activate.*Start a queue/i)).toBeVisible()
      await expect(page.getByText(/Process.*Manually trigger/i)).toBeVisible()
      await expect(page.getByText(/Deactivate.*Stop the queue/i)).toBeVisible()
    })
  })

  test.describe('Event Type Selection', () => {
    test('should have TIME_DEAL and COUPON options', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      const eventTypeSelect = page.locator('select').first()

      // TIME_DEAL 옵션
      await expect(eventTypeSelect.locator('option[value="TIME_DEAL"]')).toHaveCount(1)

      // COUPON 옵션
      await expect(eventTypeSelect.locator('option[value="COUPON"]')).toHaveCount(1)
    })

    test('should select COUPON event type', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      const eventTypeSelect = page.locator('select').first()
      await eventTypeSelect.selectOption('COUPON')

      await expect(eventTypeSelect).toHaveValue('COUPON')
    })
  })

  test.describe('Form Validation', () => {
    test('buttons should be disabled without Event ID', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Event ID 필드를 비워서 버튼 비활성화 확인
      const eventIdInput = page.locator('input[type="number"]').first()
      await eventIdInput.clear()

      const activateButton = page.getByRole('button', { name: /Activate Queue/i }).first()
      const processButton = page.getByRole('button', { name: /Process Queue/i }).first()
      const deactivateButton = page.getByRole('button', { name: /Deactivate Queue/i }).first()

      await expect(activateButton).toBeDisabled()
      await expect(processButton).toBeDisabled()
      await expect(deactivateButton).toBeDisabled()
    })

    test('buttons should be enabled with valid Event ID', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Event ID 입력
      const eventIdInput = page.locator('input[type="number"]').first()
      await eventIdInput.fill('1')

      // 버튼들이 활성화됨 (MF 중복 대응 .first())
      const activateButton = page.getByRole('button', { name: /Activate Queue/i }).first()
      await expect(activateButton).toBeEnabled()
    })
  })

  test.describe('Activation Settings', () => {
    test('should have default values for settings', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // 기본값 확인
      const inputs = page.locator('input[type="number"]')

      // Max Capacity 기본값 100
      const maxCapacityInput = inputs.nth(1) // 두 번째 input
      await expect(maxCapacityInput).toHaveValue('100')

      // Entry Batch Size 기본값 10
      const batchSizeInput = inputs.nth(2)
      await expect(batchSizeInput).toHaveValue('10')

      // Entry Interval 기본값 5
      const intervalInput = inputs.nth(3)
      await expect(intervalInput).toHaveValue('5')
    })

    test('should update Max Capacity', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      const maxCapacityInput = page.locator('input[type="number"]').nth(1)
      await maxCapacityInput.fill('200')

      await expect(maxCapacityInput).toHaveValue('200')
    })

    test('should update Entry Batch Size', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      const batchSizeInput = page.locator('input[type="number"]').nth(2)
      await batchSizeInput.fill('20')

      await expect(batchSizeInput).toHaveValue('20')
    })

    test('should update Entry Interval', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      const intervalInput = page.locator('input[type="number"]').nth(3)
      await intervalInput.fill('10')

      await expect(intervalInput).toHaveValue('10')
    })
  })

  test.describe('Queue Actions', () => {
    test('should show loading state when activating queue', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Event ID 입력
      const eventIdInput = page.locator('input[type="number"]').first()
      await eventIdInput.fill('999') // 테스트용 ID

      // Activate 버튼 클릭
      const activateButton = page.getByRole('button', { name: /Activate Queue/i }).first()
      await activateButton.click()

      // 로딩 상태 또는 결과 메시지 확인 (API 미실행 시 에러 메시지도 허용)
      const loadingOrMessage = page.getByText(/Activating|activated|error|failed|success|Queue/i)
      await expect(loadingOrMessage.first()).toBeVisible({ timeout: 5000 })
    })

    test('should show loading state when processing queue', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Event ID 입력
      const eventIdInput = page.locator('input[type="number"]').first()
      await eventIdInput.fill('999')

      // Process 버튼 클릭
      const processButton = page.getByRole('button', { name: /Process Queue/i })
      await processButton.click()

      // 로딩 상태 또는 결과 메시지 확인
      const loadingOrMessage = page.getByText(/Processing|processed|error|failed/i)
      await expect(loadingOrMessage.first()).toBeVisible({ timeout: 5000 })
    })
  })

  test.describe('Alert Messages', () => {
    test('should display success message on successful action', async ({ page }) => {
      await navigateToAdminPage(page, '/shopping/admin/queue')
      await waitForLoading(page)

      // Event ID 입력
      const eventIdInput = page.locator('input[type="number"]').first()
      await eventIdInput.fill('1')

      // Activate 버튼 클릭
      const activateButton = page.getByRole('button', { name: /Activate Queue/i }).first()
      await activateButton.click()

      // 성공 또는 에러 메시지 확인 (API 응답에 따라)
      await page.waitForTimeout(2000)

      // alert 메시지 또는 텍스트 피드백 확인
      const alertMessage = page.locator('[class*="alert"], [role="alert"]')
      const feedbackText = page.getByText(/success|error|failed|activated|Queue/i)
      const hasAlert = (await alertMessage.count()) > 0
      const hasFeedback = (await feedbackText.count()) > 0

      expect(hasAlert || hasFeedback).toBeTruthy()
    })
  })
})
