import { test as base, Page } from '@playwright/test'
import { mockLogin, TestUser, defaultTestUser } from './auth'

type TestFixtures = {
  authenticatedPage: Page
  testUser: TestUser
}

export const test = base.extend<TestFixtures>({
  testUser: defaultTestUser,

  authenticatedPage: async ({ page, testUser }, use) => {
    await mockLogin(page, testUser)
    await use(page)
  },
})

export { expect } from '@playwright/test'
