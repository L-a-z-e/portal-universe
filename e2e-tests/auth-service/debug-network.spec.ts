import { test, expect } from '@playwright/test';

test.describe('Debug Login Network Traffic', () => {
  test('should log all network requests during a failed login', async ({ page }) => {
    // Intercept and log all network requests and responses
    page.on('request', request => {
      console.log('>> Request:', request.method(), request.url());
      console.log('   Headers:', JSON.stringify(request.headers(), null, 2));
    });

    page.on('response', response => {
      console.log('<< Response:', response.status(), response.url());
      console.log('   Headers:', JSON.stringify(response.headers(), null, 2));
    });

    // 1. Navigate to the login page directly.
    console.log('\n--- Step 1: Navigating to login page ---\n');
    await page.goto('http://localhost:8080/auth-service/login');

    // 2. Fill in incorrect credentials.
    console.log('\n--- Step 2: Filling form ---\n');
    await page.locator('input[name="username"]').fill('test@example.com');
    await page.locator('input[name="password"]').fill('wrongpassword');

    // 3. Click the login button.
    console.log('\n--- Step 3: Clicking Sign In ---\n');
    // Use a try-catch block as navigation might interrupt the click
    try {
      await page.locator('button[type="submit"]').click();
      // Wait for any navigation that might occur after the click
      await page.waitForNavigation({ timeout: 5000 });
    } catch (error) {
      console.log('Error during click/navigation:', error.message);
    }

    // 4. Log the final state
    console.log('\n--- Step 4: Final State ---\n');
    console.log('Final URL:', page.url());
    
    // Keep the browser open for a moment to ensure all logs are captured
    await page.waitForTimeout(1000);
  });
});
