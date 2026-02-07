import { defineConfig, devices } from '@playwright/test'

const isDocker = process.env.TEST_ENV === 'docker'

export default defineConfig({
  testDir: './tests',
  outputDir: './test-results',

  /* 병렬 실행 */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  /* 리포터 */
  reporter: [
    ['list'],
    ['html', { outputFolder: './playwright-report' }],
  ],

  /* 전역 설정 */
  use: {
    baseURL: isDocker
      ? 'https://portal-universe:30000'
      : 'http://localhost:30000',

    /* 디버깅 */
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',

    /* 타임아웃 */
    actionTimeout: 10000,
    navigationTimeout: 30000,

    /* HTTPS 인증서 무시 (Docker 환경) */
    ignoreHTTPSErrors: isDocker,
  },

  /* 브라우저 설정 */
  projects: [
    // Auth setup projects
    {
      name: 'admin-auth-setup',
      testMatch: /auth-admin\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    // Browser projects
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      testIgnore: /\.setup\.ts/,
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
      testIgnore: /\.setup\.ts/,
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
      testIgnore: /\.setup\.ts/,
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
      testIgnore: /\.setup\.ts/,
    },
    // Admin frontend tests (depends on admin auth)
    {
      name: 'admin-frontend',
      testDir: './tests/admin-frontend',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['admin-auth-setup'],
    },
  ],

  /* 글로벌 타임아웃 */
  timeout: 60000,
  expect: {
    timeout: 5000,
  },
})
