import { test, expect } from '@playwright/test';
import { defaultTestUser } from '../fixtures/auth';

test.describe('Login Failure Redirect', () => {
  test('should redirect to /login on failed login attempt, not /auth-service/login', async ({ page }) => {
    // 1. Navigate to the login page via the gateway.
    await page.goto('http://localhost:30000/auth-service/login');

    // 2. Fill in incorrect credentials.
    await page.locator('input[name="username"]').fill(defaultTestUser.email);
    await page.locator('input[name="password"]').fill('wrongpassword');

    let redirectUrl = '';
    // 3. Intercept the redirection response.
    page.on('response', response => {
      // Check for a redirect status (302) and the login POST request.
      if (response.status() === 302 && response.request().method() === 'POST') {
        const locationHeader = response.headers()['location'];
        if (locationHeader) {
          console.log(`Intercepted a 302 redirect. Location header: ${locationHeader}`);
          redirectUrl = locationHeader;
        }
      }
    });

    // 4. Click the login button to submit the form.
    await page.locator('button[type="submit"]').click();

    // 5. Wait for navigation/redirection to complete.
    // We expect the final URL to be the incorrect one.
    await page.waitForURL('**/login?error', { timeout: 5000 }).catch(() => {
        console.log(`Final URL was not the expected one. Current URL: ${page.url()}`);
    });

    // 6. Assert that the intercepted redirect URL was indeed '/login'.
    // This confirms the auth-service is sending the wrong redirect path.
    expect(redirectUrl).toBe('/login');

    // 7. Also assert that the final URL the browser tried to navigate to is the incorrect one.
    // This confirms the end-to-end behavior.
    const finalUrl = new URL(page.url());
    expect(finalUrl.pathname).toBe('/login');
    expect(finalUrl.search).toBe('?error');
    expect(finalUrl.origin).toBe('http://localhost:30000'); // It should be the gateway's origin
  });
});
