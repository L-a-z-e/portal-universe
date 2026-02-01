import { defineConfig, devices } from '@playwright/test'

/**
 * Portal Universe E2E Test Configuration
 *
 * Shopping frontend E2E tests for Phase 1 e-commerce features:
 * - Product browsing
 * - Cart management
 * - Checkout flow
 * - Order tracking
 */
export default defineConfig({
  testDir: './tests',

  // Run tests in files in parallel
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the code
  forbidOnly: !!process.env.CI,

  // Retry on CI only
  retries: process.env.CI ? 2 : 1,

  // Opt out of parallel tests on CI
  workers: process.env.CI ? 1 : undefined,

  // Reporter to use
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],

  // Shared settings for all projects
  use: {
    // Base URL for portal-shell (Module Federation host)
    // Docker: https://portal-universe:30000, Local: https://localhost:30000
    baseURL: process.env.BASE_URL || 'https://localhost:30000',

    // Ignore HTTPS errors (Docker uses self-signed certificates)
    ignoreHTTPSErrors: true,

    // Collect trace when retrying the failed test
    trace: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Video on failure
    video: 'retain-on-failure',

    // Default timeout for actions
    actionTimeout: 10000,

    // Default navigation timeout
    navigationTimeout: 30000,
  },

  // Global timeout for each test
  timeout: 60000,

  // Expect timeout
  expect: {
    timeout: 10000,
  },

  // Configure projects for browsers
  projects: [
    // Setup project for user authentication
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },

    // Setup project for admin authentication
    {
      name: 'admin-setup',
      testMatch: /auth-admin\.setup\.ts/,
    },

    // Seed test data via Admin & User API (products, coupons, time-deals, blog posts)
    {
      name: 'data-seed',
      testMatch: /data-seed\.setup\.ts/,
      dependencies: ['setup', 'admin-setup'],
    },

    // Main test project with user authentication state (excludes admin/ and noauth)
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/user.json',
      },
      testIgnore: [/tests\/admin\//, /.*\.noauth\.spec\.ts/],
      dependencies: ['setup'],
    },

    // Admin test project with admin authentication state
    {
      name: 'chromium-admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/admin.json',
      },
      testMatch: /tests\/admin\//,
      dependencies: ['admin-setup', 'data-seed'],
    },

    // Tests that don't require authentication
    {
      name: 'chromium-no-auth',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /.*\.noauth\.spec\.ts/,
    },
  ],
})
