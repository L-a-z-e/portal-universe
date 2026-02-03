import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Service Status', () => {
  test.describe('서비스 상태 페이지 UI', () => {
    test('서비스 상태 페이지 접근', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      // 페이지 제목 확인
      await expect(page.getByRole('heading', { name: 'Service Status' })).toBeVisible()
      await expect(page.getByText('Monitor the health of Portal Universe services')).toBeVisible()
    })

    test('전체 상태 배너 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      // 전체 상태 메시지 중 하나가 표시됨 (정규식으로 확인)
      const statusBanner = page.getByRole('heading', {
        name: /All systems operational|Major outage|systems experiencing issues|Checking systems/i
      })
      await expect(statusBanner).toBeVisible({ timeout: 10000 })
    })

    test('Auto Refresh 토글 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await expect(page.getByText('Auto Refresh')).toBeVisible()
    })

    test('Refresh 버튼 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await expect(page.getByRole('button', { name: 'Refresh' })).toBeVisible()
    })

    test('Last checked 시간 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await expect(page.getByText(/Last checked:/)).toBeVisible()
    })
  })

  test.describe('서비스 목록', () => {
    test('서비스 카드들 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      // 잠시 대기하여 서비스 상태 로드
      await page.waitForTimeout(2000)

      // 서비스 카드가 최소 1개 이상 있는지 확인
      const serviceCards = page.locator('.bg-bg-card').filter({ hasText: /Healthy|Down|Degraded|Unknown/ })
      const count = await serviceCards.count()
      expect(count).toBeGreaterThan(0)
    })

    test('Response Time 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await page.waitForTimeout(2000)

      // Response Time 레이블 확인
      const responseTimeLabels = page.getByText('Response Time')
      const count = await responseTimeLabels.count()
      expect(count).toBeGreaterThan(0)
    })
  })

  test.describe('상태 범례', () => {
    test('Status Legend 섹션 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await expect(page.getByText('Status Legend')).toBeVisible()
    })

    test('상태 아이콘 설명 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      // 상태 레이블들 (범례 섹션 내에서 확인)
      const legend = page.locator('text=Status Legend').locator('..')
      await expect(legend.getByText('Healthy').first()).toBeVisible()
      await expect(legend.getByText('Down').first()).toBeVisible()
      await expect(legend.getByText('Degraded').first()).toBeVisible()
      await expect(legend.getByText('Unknown').first()).toBeVisible()
    })
  })

  test.describe('새로고침 기능', () => {
    test('Refresh 버튼 클릭', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      const refreshButton = page.getByRole('button', { name: 'Refresh' })
      await expect(refreshButton).toBeEnabled()

      await refreshButton.click()

      // 로딩 상태 확인 또는 완료 대기
      await page.waitForTimeout(1000)
    })

    test('Auto Refresh 토글 클릭', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      // Auto Refresh 스위치 찾기
      const autoRefreshSection = page.locator('div').filter({ hasText: 'Auto Refresh' })
      const switchElement = autoRefreshSection.locator('button[role="switch"], input[type="checkbox"]').first()

      if (await switchElement.count() > 0) {
        await switchElement.click()
        await page.waitForTimeout(500)
      }
    })
  })

  test.describe('정보 노트', () => {
    test('하단 정보 노트 표시', async ({ page }) => {
      await page.goto('/status')
      await waitForLoading(page)

      await expect(page.getByText(/Services are checked every/)).toBeVisible()
    })
  })
})
