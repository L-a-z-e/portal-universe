# Troubleshooting E2E Tests

## Common Issues

### Test Timeout

**Symptom**: Test fails with timeout error
```
Error: Test timeout of 60000ms exceeded
```

**Causes**:
1. Server not running
2. Slow network/API response
3. Element not found or not visible

**Solutions**:
```bash
# 1. Check server status
curl http://localhost:30000

# 2. Check all services are running
docker compose ps

# 3. Increase timeout for specific test
test.setTimeout(120000)

# 4. Use explicit waits
await page.waitForSelector('.my-element', { timeout: 30000 })
```

---

### Element Not Found

**Symptom**: `locator.click: Target closed` or element not visible

**Causes**:
1. Wrong selector
2. Element not rendered yet
3. Page navigated away
4. Element inside iframe

**Solutions**:
```typescript
// Wait for element to be visible
await expect(page.locator('.my-element')).toBeVisible()

// Use more flexible selector with fallback
page.locator('.btn, button').first()

// Wait for page load
await page.waitForLoadState('domcontentloaded')

// Check if element exists before interacting
if (await page.locator('.optional-element').isVisible()) {
  await page.locator('.optional-element').click()
}
```

---

### Strict Mode Violation

**Symptom**: `strict mode violation: locator resolved to N elements`

**Causes**: Selector matches multiple elements

**Solutions**:
```typescript
// Use .first() for first match
await page.getByText('Submit').first().click()

// Use more specific selector
await page.getByRole('button', { name: 'Submit', exact: true }).click()

// Narrow scope with parent locator
const form = page.locator('form.login-form')
await form.getByRole('button', { name: 'Submit' }).click()
```

---

### Authentication Issues

**Symptom**: Tests fail because user is not logged in

**Causes**:
1. Mock login not working
2. Token expired
3. API mock not set up

**Solutions**:
```typescript
// Use authenticatedPage fixture
test('protected page', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/dashboard')
})

// Check mock login is working
test('verify login', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/')
  await expect(
    authenticatedPage.getByRole('button', { name: /Logout/i })
  ).toBeVisible()
})
```

---

### HTTPS Certificate Error (Docker)

**Symptom**: SSL/TLS errors in Docker environment
```
Error: net::ERR_CERT_AUTHORITY_INVALID
```

**Solution**: Already configured in `playwright.config.ts`
```typescript
use: {
  ignoreHTTPSErrors: isDocker,
}
```

---

### Flaky Tests

**Symptom**: Tests pass sometimes, fail other times

**Causes**:
1. Race conditions
2. Timing issues
3. External dependencies

**Solutions**:
```typescript
// 1. Use explicit waits instead of timeouts
await waitForLoading(page)

// 2. Wait for network to be idle
await page.waitForLoadState('networkidle')

// 3. Wait for specific API response
await page.waitForResponse(resp =>
  resp.url().includes('/api/data') && resp.status() === 200
)

// 4. Use retry in config
// playwright.config.ts
retries: process.env.CI ? 2 : 0,
```

---

### Vue/React Component Not Rendering

**Symptom**: Page loads but components are missing

**Causes**:
1. JavaScript error
2. Module Federation issue
3. API returning error

**Solutions**:
```bash
# Check browser console for errors
pnpm test:headed

# Enable trace for debugging
PWDEBUG=1 pnpm test

# Check network requests
await page.route('**/*', route => {
  console.log(route.request().url())
  route.continue()
})
```

---

### Login Modal Not Appearing

**Symptom**: Protected route doesn't show login modal

**Causes**:
1. Navigation guard not triggered
2. Auth state mismatch

**Solutions**:
```typescript
// Wait for modal to appear
await expect(
  page.getByRole('button', { name: '로그인', exact: true })
).toBeVisible({ timeout: 10000 })

// Or check for redirect
await page.waitForURL(/login/)
```

---

## Debug Commands

```bash
# Debug single test
pnpm test:debug -- -g "test name"

# Run with browser visible
pnpm test:headed

# Open Playwright UI
pnpm test:ui

# Generate trace
PWDEBUG=1 pnpm test

# View trace file
npx playwright show-trace test-results/.../trace.zip

# Record test steps
npx playwright codegen http://localhost:30000
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `TEST_ENV` | local | `local` or `docker` |
| `CI` | - | Set in CI environment |
| `PWDEBUG` | - | Enable debug mode |
| `HEADED` | - | Show browser |

## Checking Service Status

```bash
# Check all services
curl http://localhost:30000          # Portal Shell
curl http://localhost:30001          # Blog Frontend
curl http://localhost:30002          # Shopping Frontend
curl http://localhost:30003          # Prism Frontend
curl http://localhost:8080/actuator/health  # API Gateway

# Check Docker containers
docker compose ps
docker compose logs -f portal-shell
```

## Common Fixes Summary

| Issue | Quick Fix |
|-------|-----------|
| Timeout | Increase timeout, check server |
| Element not found | Use `.first()`, wait for visibility |
| Auth issues | Use `authenticatedPage` fixture |
| Flaky test | Add explicit waits |
| HTTPS error | Set `ignoreHTTPSErrors: true` |
| Strict violation | Use `exact: true` or `.first()` |

## Need More Help?

1. Check test report: `pnpm report`
2. Review error screenshots in `test-results/`
3. Check error context files: `test-results/**/error-context.md`
4. Run single test with debug: `pnpm test:debug -- -g "test name"`
