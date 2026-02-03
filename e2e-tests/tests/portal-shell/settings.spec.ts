import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Settings', () => {
  test.describe('설정 페이지 UI', () => {
    test('설정 페이지 접근', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      // 페이지 제목 확인
      await expect(page.getByRole('heading', { name: 'Settings', exact: true })).toBeVisible()
      await expect(page.getByText('Customize your Portal Universe experience')).toBeVisible()
    })

    test('Appearance 섹션 표시', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await expect(page.getByRole('heading', { name: /Appearance/ })).toBeVisible()
      await expect(page.getByText('Theme')).toBeVisible()
      await expect(page.getByText('Compact Mode')).toBeVisible()
    })

    test('Language 섹션 표시', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await expect(page.getByRole('heading', { name: /Language/ })).toBeVisible()
      await expect(page.getByText('한국어')).toBeVisible()
      await expect(page.getByText('English')).toBeVisible()
    })

    test('Notifications 섹션 표시', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await expect(page.getByRole('heading', { name: /Notifications/ })).toBeVisible()
      await expect(page.getByText('Email Notifications')).toBeVisible()
      await expect(page.getByText('Push Notifications')).toBeVisible()
      await expect(page.getByText('Marketing Emails')).toBeVisible()
    })

    test('Reset Settings 섹션 표시', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await expect(page.getByRole('heading', { name: /Reset Settings/ })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Reset to Defaults' })).toBeVisible()
    })
  })

  test.describe('테마 변경', () => {
    test('테마 옵션 표시 (Dark, Light, System)', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await expect(page.getByRole('button', { name: 'Dark' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Light' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'System' })).toBeVisible()
    })

    test('Light 테마 선택 시 Saved 메시지 표시', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await page.getByRole('button', { name: 'Light' }).click()

      // Saved 메시지 확인
      await expect(page.getByText('Saved')).toBeVisible()
    })

    test('Dark 테마 선택', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await page.getByRole('button', { name: 'Dark' }).click()
      await expect(page.getByText('Saved')).toBeVisible()
    })

    test('System 테마 선택', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await page.getByRole('button', { name: 'System' }).click()
      await expect(page.getByText('Saved')).toBeVisible()
    })
  })

  test.describe('언어 변경', () => {
    test('한국어 선택', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await page.getByRole('button', { name: /한국어/ }).click()
      await expect(page.getByText('Saved')).toBeVisible()
    })

    test('English 선택', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      await page.getByRole('button', { name: /English/ }).click()
      await expect(page.getByText('Saved')).toBeVisible()
    })
  })

  test.describe('알림 설정', () => {
    test('Email Notifications 토글', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      // Email Notifications 스위치 찾기 및 토글
      const emailSection = page.locator('div').filter({ hasText: /^Email Notifications/ }).first()
      const emailSwitch = emailSection.locator('button[role="switch"], input[type="checkbox"]').first()

      if (await emailSwitch.count() > 0) {
        await emailSwitch.click()
        await expect(page.getByText('Saved')).toBeVisible()
      }
    })

    test('Compact Mode 토글', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      // Compact Mode 스위치 찾기
      const compactSection = page.locator('div').filter({ hasText: 'Compact Mode' }).first()
      const compactSwitch = compactSection.locator('button[role="switch"], input[type="checkbox"]').first()

      if (await compactSwitch.count() > 0) {
        await compactSwitch.click()
        await expect(page.getByText('Saved')).toBeVisible()
      }
    })
  })

  test.describe('설정 초기화', () => {
    test('Reset to Defaults 버튼 존재', async ({ page }) => {
      await page.goto('/settings')
      await waitForLoading(page)

      const resetButton = page.getByRole('button', { name: 'Reset to Defaults' })
      await expect(resetButton).toBeVisible()
      await expect(resetButton).toBeEnabled()
    })
  })
})
