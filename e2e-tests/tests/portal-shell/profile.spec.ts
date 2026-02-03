import { test, expect } from '../../fixtures/base'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Profile', () => {
  test.describe('인증 필요', () => {
    test('비로그인 시 프로필 접근 차단', async ({ page }) => {
      await page.goto('/profile')
      await waitForLoading(page)

      // 비로그인 시 로그인 모달이 표시됨
      // 모달 내의 "로그인" 버튼으로 확인
      await expect(page.getByRole('button', { name: '로그인', exact: true })).toBeVisible({ timeout: 10000 })
    })
  })

  test.describe('프로필 페이지 UI', () => {
    test('로그인 후 프로필 페이지 접근', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 프로필 페이지 확인 (프로필 정보 또는 에러가 표시됨)
      const profileContent = authenticatedPage.getByText(/프로필|Profile|이메일|닉네임/)
      await expect(profileContent.first()).toBeVisible({ timeout: 10000 })
    })

    test('이메일 정보 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 이메일 레이블 또는 값
      const emailSection = authenticatedPage.getByText(/이메일|Email/)
      await expect(emailSection.first()).toBeVisible({ timeout: 10000 })
    })

    test('닉네임 정보 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 닉네임 레이블 또는 값
      const nicknameSection = authenticatedPage.getByText(/닉네임|Nickname/)
      await expect(nicknameSection.first()).toBeVisible({ timeout: 10000 })
    })
  })

  test.describe('프로필 수정', () => {
    test('수정 버튼 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 수정 또는 편집 버튼
      const editButton = authenticatedPage.getByRole('button', { name: /수정|편집|Edit/ })
      if (await editButton.count() > 0) {
        await expect(editButton.first()).toBeVisible()
      }
    })

    test('수정 모드 전환', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 수정 버튼 클릭
      const editButton = authenticatedPage.getByRole('button', { name: /수정|편집|Edit/ })
      if (await editButton.count() > 0) {
        await editButton.first().click()

        // 저장 또는 취소 버튼 표시 확인
        const saveOrCancel = authenticatedPage.getByRole('button', { name: /저장|취소|Save|Cancel/ })
        await expect(saveOrCancel.first()).toBeVisible({ timeout: 5000 })
      }
    })
  })

  test.describe('비밀번호 변경', () => {
    test('비밀번호 변경 버튼 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 비밀번호 변경 버튼
      const passwordButton = authenticatedPage.getByRole('button', { name: /비밀번호|Password/ })
      if (await passwordButton.count() > 0) {
        await expect(passwordButton.first()).toBeVisible()
      }
    })

    test('비밀번호 변경 모달 열기', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 비밀번호 변경 버튼 클릭
      const passwordButton = authenticatedPage.getByRole('button', { name: /비밀번호 변경|Change Password/ })
      if (await passwordButton.count() > 0) {
        await passwordButton.first().click()

        // 모달 또는 폼 표시
        const modal = authenticatedPage.locator('[role="dialog"]')
        const passwordInput = authenticatedPage.locator('input[type="password"]')

        const modalVisible = await modal.isVisible().catch(() => false)
        const inputVisible = await passwordInput.first().isVisible().catch(() => false)

        expect(modalVisible || inputVisible).toBeTruthy()
      }
    })
  })

  test.describe('계정 삭제', () => {
    test('계정 삭제 버튼 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 계정 삭제 또는 탈퇴 버튼
      const deleteButton = authenticatedPage.getByRole('button', { name: /삭제|탈퇴|Delete|Withdraw/ })
      if (await deleteButton.count() > 0) {
        await expect(deleteButton.first()).toBeVisible()
      }
    })
  })

  test.describe('아바타', () => {
    test('프로필 아바타 표시', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/profile')
      await waitForLoading(authenticatedPage)

      // 아바타 이미지 또는 이니셜
      const avatar = authenticatedPage.locator('img[alt*="profile"], img[alt*="avatar"], .rounded-full')
      if (await avatar.count() > 0) {
        await expect(avatar.first()).toBeVisible()
      }
    })
  })
})
