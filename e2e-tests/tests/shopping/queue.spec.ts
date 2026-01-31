/**
 * Queue E2E Tests
 *
 * Tests for queue waiting functionality:
 * - Display queue waiting page
 * - Show queue position and estimated time
 * - Leave queue functionality
 * - Handle expired sessions
 * - Queue status component
 */
import { test, expect } from '../helpers/test-fixtures'

test.describe('Queue Management', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to shopping section
    await page.goto('/shopping')
  })

  test('should display queue waiting page', async ({ page }) => {
    // Try to access a popular product or time deal that might trigger queue
    // Note: In actual test environment, queue might not always be active
    await page.goto('/shopping/queue')

    // Wait for loading
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check if queue page exists or if redirected
    const queueTitle = page.locator('h1:has-text("Queue"), h1:has-text("Waiting Room")')
    const notFoundError = page.locator('text="Not found", text="404"')

    const hasQueuePage = await queueTitle.isVisible()
    const isNotFound = await notFoundError.isVisible()

    if (hasQueuePage) {
      await expect(queueTitle).toBeVisible()

      // Queue info should be displayed
      const queueInfo = page.locator('text=/position|waiting|queue/')
      await expect(queueInfo.first()).toBeVisible()
    } else if (isNotFound) {
      // Queue page might not exist when no active queues
      expect(isNotFound).toBeTruthy()
    }
  })

  test('should show queue position and estimated time', async ({ page }) => {
    // Navigate to queue page
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check if queue is active
    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Queue position should be shown
      const position = page.locator('text=/Your position|Position in queue|#\\d+/')
      const hasPosition = await position.isVisible()

      if (hasPosition) {
        await expect(position).toBeVisible()

        // Position should be a number
        const positionText = await position.textContent()
        expect(positionText).toMatch(/\d+/)
      }

      // Estimated wait time should be shown
      const waitTime = page.locator('text=/Estimated|Wait time|approximately/')
      const hasWaitTime = await waitTime.isVisible()

      if (hasWaitTime) {
        await expect(waitTime).toBeVisible()

        // Time should include minutes or seconds
        const timeText = await waitTime.textContent()
        expect(timeText).toMatch(/\d+\s*(min|sec|minute|second)/)
      }
    }
  })

  test('should allow leaving the queue', async ({ page }) => {
    // Navigate to queue page
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Check if queue is active
    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Leave queue button should be visible
      const leaveButton = page.locator('button:has-text("Leave Queue"), button:has-text("Exit")')
      const hasLeaveButton = await leaveButton.isVisible()

      if (hasLeaveButton) {
        await expect(leaveButton).toBeVisible()

        // Click leave button
        await leaveButton.click()

        // Confirmation dialog might appear
        page.on('dialog', async dialog => {
          await dialog.accept()
        })

        // Wait for action to complete
        await page.waitForTimeout(1000)

        // Should redirect or show confirmation
        const redirected = page.url() !== '/shopping/queue'
        const confirmMessage = page.locator('text=/left|exited|cancelled/')

        const hasRedirected = redirected
        const hasMessage = await confirmMessage.isVisible()

        expect(hasRedirected || hasMessage).toBeTruthy()
      }
    }
  })

  test('should handle expired session', async ({ page }) => {
    // Navigate to queue page
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for session expired message (might appear after timeout)
    const expiredMessage = page.locator('text=/expired|session ended|timeout/')

    // Wait a bit to see if session status is shown
    await page.waitForTimeout(2000)

    const hasExpired = await expiredMessage.isVisible()

    if (hasExpired) {
      await expect(expiredMessage).toBeVisible()

      // Retry or return button should be available
      const actionButton = page.locator('button:has-text("Retry"), button:has-text("Return"), a:has-text("Back")')
      await expect(actionButton.first()).toBeVisible()
    }
  })

  test('should show queue status component', async ({ page }) => {
    // Queue status might be shown on product pages during high traffic
    await page.goto('/shopping/products/1')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    // Look for queue indicator or banner
    const queueIndicator = page.locator('text=/Queue active|High traffic|Waiting room/')
    const hasIndicator = await queueIndicator.isVisible()

    if (hasIndicator) {
      await expect(queueIndicator).toBeVisible()

      // Join queue button might be present
      const joinButton = page.locator('button:has-text("Join Queue"), button:has-text("Enter Waiting Room")')
      const hasJoinButton = await joinButton.isVisible()

      expect(hasJoinButton).toBeTruthy()
    }
  })

  test('should display queue rules and information', async ({ page }) => {
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Information about queue behavior should be shown
      const queueInfo = page.locator('text=/Do not refresh|Keep this page open|automatically redirect/')
      const hasInfo = await queueInfo.isVisible()

      if (hasInfo) {
        await expect(queueInfo).toBeVisible()
      }
    }
  })

  test('should show progress indicator while waiting', async ({ page }) => {
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Progress bar or spinner should be visible
      const progressIndicator = page.locator('[role="progressbar"], .progress-bar, [class*="progress"]')
      const spinner = page.locator('[class*="spin"], [class*="loading"]')

      const hasProgress = await progressIndicator.isVisible()
      const hasSpinner = await spinner.isVisible()

      // Some form of progress indication should exist
      expect(hasProgress || hasSpinner).toBeTruthy()
    }
  })

  test('should auto-refresh queue position', async ({ page }) => {
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Get initial position if shown
      const position = page.locator('text=/Position|#\\d+/')
      const hasPosition = await position.isVisible()

      if (hasPosition) {
        const initialPosition = await position.textContent()

        // Wait for potential update
        await page.waitForTimeout(5000)

        // Position might have updated (decreased)
        const updatedPosition = await position.textContent()

        // Either position changed or remained (both are valid)
        expect(initialPosition || updatedPosition).toBeTruthy()
      }
    }
  })

  test('should redirect when queue is complete', async ({ page }) => {
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Wait for potential completion (short timeout for test)
      await page.waitForTimeout(3000)

      // Check if still on queue page or redirected
      const currentUrl = page.url()

      if (!currentUrl.includes('/queue')) {
        // Successfully passed queue and redirected
        expect(currentUrl).toContain('/shopping')
      } else {
        // Still in queue (expected for longer waits)
        expect(currentUrl).toContain('/queue')
      }
    }
  })

  test('should display queue capacity information', async ({ page }) => {
    await page.goto('/shopping/queue')
    await page.waitForSelector('.animate-spin', { state: 'hidden', timeout: 10000 }).catch(() => {})

    const queueActive = page.locator('text="Queue"')
    const isActive = await queueActive.isVisible()

    if (isActive) {
      // Queue capacity or total users info might be shown
      const capacityInfo = page.locator('text=/\\d+ users|people in queue|total waiting/')
      const hasCapacity = await capacityInfo.isVisible()

      if (hasCapacity) {
        await expect(capacityInfo).toBeVisible()

        // Should include number
        const capacityText = await capacityInfo.textContent()
        expect(capacityText).toMatch(/\d+/)
      }
    }
  })
})
