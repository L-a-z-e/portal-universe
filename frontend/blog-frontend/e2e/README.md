# Blog Frontend E2E Tests

Playwright를 사용한 blog-frontend Phase 1-A 기능에 대한 E2E 테스트입니다.

## 테스트 구조

```
e2e/
├── fixtures/          # 테스트 데이터 및 헬퍼
│   ├── test-data.ts   # Mock 데이터
│   └── auth.ts        # 인증 헬퍼
└── tests/             # 테스트 파일
    ├── series.spec.ts    # 시리즈 기능
    ├── like.spec.ts      # 좋아요 기능
    ├── tag.spec.ts       # 태그 페이지
    ├── trending.spec.ts  # 트렌딩/최신 탭
    └── comment.spec.ts   # 대댓글 기능
```

## 테스트 시나리오

### 1. series.spec.ts - 시리즈 기능
- ✅ 시리즈 목록 페이지 표시
- ✅ 시리즈 상세 페이지 접근
- ✅ 시리즈 내 포스트 순서 확인
- ✅ 시리즈 박스에서 이전/다음 네비게이션
- ✅ 시리즈 포스트 목록 펼치기/접기
- ✅ 빈 시리즈 처리

### 2. like.spec.ts - 좋아요 기능
- ✅ 좋아요 버튼 표시
- ✅ 좋아요 클릭 시 카운트 증가
- ✅ 좋아요 재클릭 시 카운트 감소
- ✅ 비로그인 시 좋아요 처리
- ✅ 페이지 리로드 시 상태 유지
- ✅ 연속 클릭 방지 (debounce)

### 3. tag.spec.ts - 태그 페이지
- ✅ 태그 목록 페이지 표시
- ✅ 정렬 옵션 변경 (인기순, 이름순, 최신순)
- ✅ 태그 검색 필터
- ✅ 태그 상세 페이지 접근
- ✅ 태그별 포스트 목록
- ✅ URL 쿼리 파라미터 동기화

### 4. trending.spec.ts - 트렌딩/최신 탭
- ✅ 메인 페이지 탭 표시
- ✅ 탭 전환
- ✅ 기간 필터 변경 (트렌딩 탭)
- ✅ URL 쿼리 파라미터 동기화
- ✅ 무한 스크롤
- ✅ 포스트 상세 페이지 이동

### 5. comment.spec.ts - 대댓글 기능
- ✅ 댓글 목록 표시
- ✅ 새 댓글 작성
- ✅ 댓글에 답글 작성
- ✅ 답글 접기/펼치기
- ✅ 댓글 수정/삭제
- ✅ 댓글 유효성 검사
- ✅ 비로그인 시 처리

## 실행 방법

### 1. Playwright 브라우저 설치
```bash
npx playwright install
```

### 2. 빌드 및 프리뷰 서버 실행
테스트는 `http://localhost:30001`에서 실행됩니다.

```bash
# 빌드
npm run build:dev

# 프리뷰 서버 실행 (별도 터미널)
npm run preview
```

### 3. 테스트 실행

#### 기본 실행 (headless)
```bash
npm run test:e2e
```

#### UI 모드 (추천)
```bash
npm run test:e2e:ui
```

#### 브라우저를 보면서 실행
```bash
npm run test:e2e:headed
```

#### 디버그 모드
```bash
npm run test:e2e:debug
```

#### 특정 테스트만 실행
```bash
npx playwright test series.spec.ts
npx playwright test like.spec.ts --headed
```

#### 특정 브라우저만 실행
```bash
npx playwright test --project=chromium
npx playwright test --project=webkit
```

### 4. 테스트 결과 보기
```bash
npm run test:e2e:report
```

## 테스트 데이터

### Mock 인증
테스트는 `fixtures/auth.ts`의 `mockLogin()` 함수를 사용하여 인증 상태를 시뮬레이션합니다.

```typescript
await mockLogin(page)  // 로그인 상태
await mockLogout(page) // 로그아웃 상태
```

### Mock 데이터
`fixtures/test-data.ts`에 정의된 Mock 데이터:
- `mockUser`: 테스트 사용자
- `mockPost`: 테스트 포스트
- `mockSeries`: 테스트 시리즈
- `mockTag`: 테스트 태그
- `mockComment`: 테스트 댓글

## 주의사항

### 1. data-testid 속성
모든 테스트는 `data-testid` 속성을 사용합니다. 컴포넌트에 다음과 같이 추가하세요:

```vue
<template>
  <div data-testid="series-card">
    <h3 data-testid="series-title">{{ series.name }}</h3>
    <p data-testid="series-post-count">{{ series.postCount }} posts</p>
  </div>
</template>
```

### 2. API 응답
테스트는 실제 API 또는 Mock API를 사용할 수 있습니다. 프로덕션 환경에서는:
- Mock Service Worker (MSW) 설정
- 테스트 데이터베이스 사용
- API stubbing/mocking

### 3. 비동기 처리
- `await page.waitForTimeout()`은 임시 방편입니다
- 실제로는 `await expect().toBeVisible()` 등 자동 대기를 활용하세요
- 네트워크 응답 대기: `await page.waitForResponse()`

### 4. 테스트 격리
각 테스트는 독립적으로 실행되어야 합니다:
- `test.beforeEach()`에서 초기 상태 설정
- 테스트 간 상태 공유 금지
- 테스트 데이터 클린업

## CI/CD 통합

### GitHub Actions 예제
```yaml
- name: Install dependencies
  run: npm ci

- name: Install Playwright Browsers
  run: npx playwright install --with-deps

- name: Build
  run: npm run build:dev

- name: Run E2E tests
  run: npm run test:e2e

- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: playwright-report
    path: playwright-report/
```

## 문제 해결

### Port 충돌
다른 프로세스가 30001 포트를 사용 중인 경우:
```bash
lsof -i :30001
kill -9 <PID>
```

### 브라우저 설치 오류
```bash
npx playwright install --force
```

### 타임아웃 오류
`playwright.config.ts`에서 타임아웃 증가:
```typescript
use: {
  actionTimeout: 30000,
  navigationTimeout: 30000,
}
```

## 추가 개선 사항

### Phase 1-B 이후
- [ ] 포스트 작성/수정 테스트
- [ ] 이미지 업로드 테스트
- [ ] SEO 메타 태그 검증
- [ ] 성능 테스트 (Lighthouse)
- [ ] 접근성 테스트 (axe)

### Mock Service Worker 통합
```bash
npm install -D msw
```

### Visual Regression Testing
```bash
npm install -D @playwright/test-snapshots
```

## 참고 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [Vue Testing Guide](https://vuejs.org/guide/scaling-up/testing.html)
