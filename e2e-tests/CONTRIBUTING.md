# Contributing to E2E Tests

## Writing New Tests

### 1. Choose the Right Location
- Portal Shell: `tests/portal-shell/`
- Blog: `tests/blog/`
- Shopping: `tests/shopping/`
- Prism: `tests/prism/`
- Admin: `tests/admin/`

### 2. Follow Naming Convention
- File: `feature-name.spec.ts`
- Describe block: Feature name
- Test name: Should + expected behavior

```typescript
test.describe('Feature Name', () => {
  test('should do something specific', async ({ page }) => {
    // ...
  })
})
```

### 3. Use Semantic Selectors

```typescript
// ✅ Good - Semantic selectors
page.getByRole('button', { name: /submit/i })
page.getByRole('heading', { name: 'Title' })
page.getByPlaceholder('Enter email')
page.getByText('Welcome')
page.locator('.comment-section, .comments')

// ❌ Avoid - data-testid or fragile selectors
page.locator('[data-testid="submit"]')
page.locator('#submit-btn')
page.locator('div > button:nth-child(2)')
```

### 4. Use Fixtures

```typescript
import { test, expect } from '../../fixtures/base'

// Unauthenticated test
test('public page test', async ({ page }) => {
  await page.goto('/')
})

// Authenticated test - already logged in
test('authenticated test', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/dashboard')
})
```

### 5. Handle Loading States

```typescript
import { waitForLoading } from '../../utils/wait'

test('test with loading', async ({ page }) => {
  await page.goto('/products')
  await waitForLoading(page)

  // Now safe to interact
  await expect(page.getByRole('heading')).toBeVisible()
})
```

### 6. Use Common Selectors

```typescript
import { blogSelectors, shoppingSelectors } from '../../utils/selectors'

// Blog tests
const postCard = blogSelectors.postCard(page)
const likeButton = blogSelectors.likeButton(page)

// Shopping tests
const productCard = shoppingSelectors.productCard(page)
const addToCartBtn = shoppingSelectors.addToCartButton(page)
```

## Test Structure

```typescript
import { test, expect } from '../../fixtures/base'
import { waitForLoading } from '../../utils/wait'

test.describe('Feature Name', () => {
  // Setup - runs before each test
  test.beforeEach(async ({ page }) => {
    await page.goto('/feature-page')
    await waitForLoading(page)
  })

  test('should display main content', async ({ page }) => {
    await expect(page.getByRole('heading')).toBeVisible()
  })

  test('should handle user interaction', async ({ page }) => {
    await page.getByRole('button', { name: 'Click me' }).click()
    await expect(page.getByText('Success')).toBeVisible()
  })
})
```

## Code Review Checklist

- [ ] Uses semantic selectors (getByRole, getByText, getByPlaceholder)
- [ ] Independent of other tests (no shared state)
- [ ] Has clear, descriptive test names
- [ ] Handles loading states properly
- [ ] Works in both Local and Docker environments
- [ ] Uses fixtures for authentication
- [ ] Includes appropriate timeouts for async operations
- [ ] No hardcoded wait times (use waitFor* utilities)

## Running Tests

```bash
# Run all tests
pnpm test

# Run specific service tests
pnpm test:portal
pnpm test:blog
pnpm test:shopping
pnpm test:prism

# Debug mode
pnpm test:debug

# With UI
pnpm test:ui

# Single test file
npx playwright test tests/blog/comment.spec.ts
```

## Adding New Test Data

Edit `fixtures/test-data.ts`:

```typescript
export const routes = {
  portal: { ... },
  blog: { ... },
  shopping: { ... },
  prism: { ... },
}

export const testPosts = { ... }
export const testProducts = { ... }
```

## Questions?

- Check `TROUBLESHOOTING.md` for common issues
- Review existing tests for patterns
- Run `pnpm test:ui` for interactive debugging
