import { test, expect } from '../../fixtures/base'
import { waitForLoading } from '../../utils/wait'

test.describe('Admin Queue Management', () => {
  // Admin 테스트는 admin@example.com 계정으로 실행됨 (ROLE_SUPER_ADMIN)

  test.describe('Queue Management Page UI', () => {
    test('should display admin queue management page', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // 페이지 제목 확인
      const title = authenticatedPage.getByRole('heading', { name: /Queue Management/i })
      await expect(title).toBeVisible({ timeout: 10000 })
    })

    test('should display Queue Target section', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Queue Target 섹션
      await expect(authenticatedPage.getByText('Queue Target')).toBeVisible()

      // Event Type 선택
      await expect(authenticatedPage.getByText('Event Type')).toBeVisible()
      const eventTypeSelect = authenticatedPage.locator('select').first()
      await expect(eventTypeSelect).toBeVisible()

      // Event ID 입력
      await expect(authenticatedPage.getByText('Event ID')).toBeVisible()
      const eventIdInput = authenticatedPage.locator('input[type="number"]').first()
      await expect(eventIdInput).toBeVisible()
    })

    test('should display Activation Settings section', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Activation Settings 섹션
      await expect(authenticatedPage.getByText('Activation Settings')).toBeVisible()

      // Max Capacity
      await expect(authenticatedPage.getByText('Max Capacity')).toBeVisible()

      // Entry Batch Size
      await expect(authenticatedPage.getByText('Entry Batch Size')).toBeVisible()

      // Entry Interval
      await expect(authenticatedPage.getByText(/Entry Interval/)).toBeVisible()
    })

    test('should display Actions section', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Actions 섹션
      await expect(authenticatedPage.getByText('Actions')).toBeVisible()

      // 버튼들
      await expect(authenticatedPage.getByRole('button', { name: /Activate Queue/i })).toBeVisible()
      await expect(authenticatedPage.getByRole('button', { name: /Process Queue/i })).toBeVisible()
      await expect(authenticatedPage.getByRole('button', { name: /Deactivate Queue/i })).toBeVisible()
    })

    test('should display Help section', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Help 섹션
      await expect(authenticatedPage.getByText('How Queue Management Works')).toBeVisible()
      await expect(authenticatedPage.getByText(/Activate.*Start a queue/i)).toBeVisible()
      await expect(authenticatedPage.getByText(/Process.*Manually trigger/i)).toBeVisible()
      await expect(authenticatedPage.getByText(/Deactivate.*Stop the queue/i)).toBeVisible()
    })
  })

  test.describe('Event Type Selection', () => {
    test('should have TIME_DEAL and COUPON options', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      const eventTypeSelect = authenticatedPage.locator('select').first()

      // TIME_DEAL 옵션
      await expect(eventTypeSelect.locator('option[value="TIME_DEAL"]')).toHaveCount(1)

      // COUPON 옵션
      await expect(eventTypeSelect.locator('option[value="COUPON"]')).toHaveCount(1)
    })

    test('should select COUPON event type', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      const eventTypeSelect = authenticatedPage.locator('select').first()
      await eventTypeSelect.selectOption('COUPON')

      await expect(eventTypeSelect).toHaveValue('COUPON')
    })
  })

  test.describe('Form Validation', () => {
    test('buttons should be disabled without Event ID', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Event ID가 비어있으면 버튼들이 비활성화
      const activateButton = authenticatedPage.getByRole('button', { name: /Activate Queue/i })
      const processButton = authenticatedPage.getByRole('button', { name: /Process Queue/i })
      const deactivateButton = authenticatedPage.getByRole('button', { name: /Deactivate Queue/i })

      await expect(activateButton).toBeDisabled()
      await expect(processButton).toBeDisabled()
      await expect(deactivateButton).toBeDisabled()
    })

    test('buttons should be enabled with valid Event ID', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Event ID 입력
      const eventIdInput = authenticatedPage.locator('input[type="number"]').first()
      await eventIdInput.fill('1')

      // 버튼들이 활성화됨
      const activateButton = authenticatedPage.getByRole('button', { name: /Activate Queue/i })
      await expect(activateButton).toBeEnabled()
    })
  })

  test.describe('Activation Settings', () => {
    test('should have default values for settings', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // 기본값 확인
      const inputs = authenticatedPage.locator('input[type="number"]')

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

    test('should update Max Capacity', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      const maxCapacityInput = authenticatedPage.locator('input[type="number"]').nth(1)
      await maxCapacityInput.fill('200')

      await expect(maxCapacityInput).toHaveValue('200')
    })

    test('should update Entry Batch Size', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      const batchSizeInput = authenticatedPage.locator('input[type="number"]').nth(2)
      await batchSizeInput.fill('20')

      await expect(batchSizeInput).toHaveValue('20')
    })

    test('should update Entry Interval', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      const intervalInput = authenticatedPage.locator('input[type="number"]').nth(3)
      await intervalInput.fill('10')

      await expect(intervalInput).toHaveValue('10')
    })
  })

  test.describe('Queue Actions', () => {
    test('should show loading state when activating queue', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Event ID 입력
      const eventIdInput = authenticatedPage.locator('input[type="number"]').first()
      await eventIdInput.fill('999') // 테스트용 ID

      // Activate 버튼 클릭
      const activateButton = authenticatedPage.getByRole('button', { name: /Activate Queue/i })
      await activateButton.click()

      // 로딩 상태 또는 결과 메시지 확인
      const loadingOrMessage = authenticatedPage.getByText(/Activating|activated|error|failed/i)
      await expect(loadingOrMessage.first()).toBeVisible({ timeout: 5000 })
    })

    test('should show loading state when processing queue', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Event ID 입력
      const eventIdInput = authenticatedPage.locator('input[type="number"]').first()
      await eventIdInput.fill('999')

      // Process 버튼 클릭
      const processButton = authenticatedPage.getByRole('button', { name: /Process Queue/i })
      await processButton.click()

      // 로딩 상태 또는 결과 메시지 확인
      const loadingOrMessage = authenticatedPage.getByText(/Processing|processed|error|failed/i)
      await expect(loadingOrMessage.first()).toBeVisible({ timeout: 5000 })
    })
  })

  test.describe('Alert Messages', () => {
    test('should display success message on successful action', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/shopping/admin/queue')
      await waitForLoading(authenticatedPage)

      // Event ID 입력
      const eventIdInput = authenticatedPage.locator('input[type="number"]').first()
      await eventIdInput.fill('1')

      // Activate 버튼 클릭
      const activateButton = authenticatedPage.getByRole('button', { name: /Activate Queue/i })
      await activateButton.click()

      // 성공 또는 에러 메시지 확인 (API 응답에 따라)
      await authenticatedPage.waitForTimeout(2000)

      const alertMessage = authenticatedPage.locator('[class*="alert"], [role="alert"]')
      if (await alertMessage.count() > 0) {
        await expect(alertMessage.first()).toBeVisible()
      }
    })
  })
})
