# E2E Tests

Portal Universe 통합 E2E 테스트 프로젝트

## 개요

이 프로젝트는 Portal Universe의 모든 프론트엔드 서비스에 대한 E2E 테스트를 통합 관리합니다.

### 지원 서비스

| Service | Tests | Description |
|---------|-------|-------------|
| portal-shell | 5 | 인증, 네비게이션, 테마, 헤더, 챗봇 |
| blog | 11 | 피드, 글쓰기, 댓글, 좋아요, 팔로우 등 |
| shopping | 6 | 상품, 장바구니, 주문, 위시리스트 |
| prism | 3 | 채팅, 히스토리, 설정 |

## 환경 설정

### 1. 의존성 설치

```bash
cd e2e-tests
npm install
npx playwright install
```

### 2. 환경 변수

| 변수 | 값 | 설명 |
|------|-----|------|
| `TEST_ENV` | `local` (기본) | Local 환경 |
| `TEST_ENV` | `docker` | Docker 환경 |

## 테스트 실행

### 전체 테스트

```bash
# Local 환경
npm test

# Docker 환경
TEST_ENV=docker npm test
```

### 서비스별 테스트

```bash
npm run test:portal    # Portal Shell
npm run test:blog      # Blog
npm run test:shopping  # Shopping
npm run test:prism     # Prism
```

### 디버그 모드

```bash
npm run test:debug     # 브라우저 표시 + 단계별 실행
npm run test:ui        # Playwright UI 모드
```

### 특정 테스트 파일

```bash
npx playwright test tests/portal-shell/auth.spec.ts
```

## 디렉토리 구조

```
e2e-tests/
├── fixtures/           # 테스트 픽스처
│   ├── auth.ts         # 인증 헬퍼 (mockLogin)
│   ├── base.ts         # 확장된 test fixture
│   └── test-data.ts    # 테스트 데이터 및 라우트
├── utils/              # 유틸리티
│   ├── selectors.ts    # 공통 셀렉터
│   └── wait.ts         # 대기 유틸리티
├── tests/              # 테스트 파일
│   ├── portal-shell/   # Portal Shell 테스트
│   ├── blog/           # Blog 테스트
│   ├── shopping/       # Shopping 테스트
│   └── prism/          # Prism 테스트
├── playwright.config.ts
└── package.json
```

## 테스트 작성 가이드

### Selector 전략

**권장 (Semantic Selectors)**
```typescript
// Role 기반
page.getByRole('button', { name: /로그인|login/i })
page.getByRole('textbox', { name: /이메일/i })

// Label 기반
page.getByLabel('비밀번호')

// Text 기반
page.getByText('환영합니다')

// CSS 클래스 (폴백)
page.locator('.post-card, .post-item, article')
```

**비권장**
```typescript
// data-testid 사용 금지
page.locator('[data-testid="login-button"]')  // ❌
```

### Mock 로그인

```typescript
import { test, expect } from '../fixtures/base'

test('인증된 사용자 테스트', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/blog/my')
  // authenticatedPage는 이미 로그인된 상태
})
```

### 대기 유틸리티

```typescript
import { waitForLoading, waitForAPI, expectToast } from '../utils/wait'

test('API 호출 테스트', async ({ page }) => {
  await page.click('button')
  await waitForAPI(page, '/api/v1/posts')
  await waitForLoading(page)
  await expectToast(page, '저장되었습니다')
})
```

## 환경별 차이

| 항목 | Local | Docker |
|------|-------|--------|
| Base URL | `http://localhost:30000` | `https://portal-universe:30000` |
| HTTPS | No | Yes (self-signed) |
| Timeout | 60s | 60s |

## 문제 해결

### Playwright 설치 오류

```bash
npx playwright install --with-deps
```

### Docker 환경 SSL 오류

`playwright.config.ts`에서 `ignoreHTTPSErrors: true` 확인

### 테스트 타임아웃

개별 테스트에서 타임아웃 증가:
```typescript
test('느린 테스트', async ({ page }) => {
  test.setTimeout(120000) // 2분
  // ...
})
```

## CI/CD

```yaml
# .github/workflows/e2e.yml 예시
- name: Run E2E Tests
  run: |
    cd e2e-tests
    npm ci
    npx playwright install --with-deps
    npm test
```
