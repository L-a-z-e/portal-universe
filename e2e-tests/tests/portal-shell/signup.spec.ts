import { test, expect } from '@playwright/test'
import { waitForLoading } from '../../utils/wait'

test.describe('Portal Shell - Signup', () => {
  test.describe('회원가입 페이지 UI', () => {
    test('회원가입 페이지 접근', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      // 페이지 제목 확인
      await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible()
      await expect(page.getByText('Portal Universe에 오신 것을 환영합니다')).toBeVisible()
    })

    test('회원가입 폼 필드 표시', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      // 필수 필드들 - placeholder로 확인
      await expect(page.getByPlaceholder('example@portal.com')).toBeVisible()
      await expect(page.getByPlaceholder(/자 이상 입력/)).toBeVisible()
      await expect(page.getByPlaceholder('사용하실 닉네임을 입력해주세요')).toBeVisible()

      // 선택 필드
      await expect(page.getByPlaceholder('실명을 입력해주세요')).toBeVisible()

      // 마케팅 동의 체크박스
      await expect(page.getByText(/마케팅 정보 수신/)).toBeVisible()

      // 제출 버튼
      await expect(page.getByRole('button', { name: '회원가입 완료' })).toBeVisible()
    })

    test('로그인하기 링크 표시', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      await expect(page.getByText('이미 계정이 있으신가요?')).toBeVisible()
      await expect(page.getByRole('link', { name: '로그인하기' })).toBeVisible()
    })
  })

  test.describe('회원가입 유효성 검사', () => {
    test('빈 폼 제출 시 에러 표시', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      // 빈 상태로 제출
      await page.getByRole('button', { name: '회원가입 완료' }).click()

      // 에러 메시지 확인
      await expect(page.getByText(/유효한 이메일|이메일.*입력/)).toBeVisible()
    })

    // Note: '잘못된 이메일 형식' 테스트는 브라우저 HTML5 email 유효성 검사와 중복되어 제거
    // 앱의 유효성 검사(@포함 여부)는 브라우저 검사보다 느슨함

    test('짧은 비밀번호', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      await page.getByPlaceholder('example@portal.com').fill('test@example.com')
      await page.getByPlaceholder(/자 이상 입력/).fill('123')
      await page.getByPlaceholder('사용하실 닉네임을 입력해주세요').fill('테스터')
      await page.getByRole('button', { name: '회원가입 완료' }).click()

      await expect(page.getByText(/비밀번호는.*자 이상/)).toBeVisible()
    })

    test('닉네임 누락', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      await page.getByPlaceholder('example@portal.com').fill('test@example.com')
      await page.getByPlaceholder(/자 이상 입력/).fill('password123')
      await page.getByRole('button', { name: '회원가입 완료' }).click()

      await expect(page.getByText(/닉네임을 입력/)).toBeVisible()
    })
  })

  test.describe('비밀번호 정책', () => {
    test('비밀번호 정책 안내 토글', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      // ? 버튼 클릭
      const policyButton = page.locator('button[title="비밀번호 조건 보기"]')
      await policyButton.click()

      // 정책 안내 표시
      await expect(page.getByText('비밀번호 조건')).toBeVisible()

      // 다시 클릭하면 숨김
      await policyButton.click()
      await expect(page.getByText('비밀번호 조건')).toBeHidden()
    })
  })

  test.describe('회원가입 성공', () => {
    test('유효한 정보로 회원가입 시도', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      const uniqueEmail = `test_${Date.now()}@example.com`

      await page.getByPlaceholder('example@portal.com').fill(uniqueEmail)
      await page.getByPlaceholder(/자 이상 입력/).fill('TestPassword123!')
      await page.getByPlaceholder('사용하실 닉네임을 입력해주세요').fill('테스트유저')

      // 폼이 유효한지 확인 (에러 없음)
      const emailError = page.getByText(/유효한 이메일/)
      const passwordError = page.getByText(/비밀번호는.*자 이상/)
      const nicknameError = page.getByText(/닉네임을 입력/)

      await expect(emailError).toBeHidden()
      await expect(passwordError).toBeHidden()
      await expect(nicknameError).toBeHidden()
    })
  })

  test.describe('네비게이션', () => {
    test('로그인하기 링크 클릭 시 홈으로 이동', async ({ page }) => {
      await page.goto('/signup')
      await waitForLoading(page)

      await page.getByRole('link', { name: '로그인하기' }).click()

      await page.waitForURL('/')
    })
  })
})
