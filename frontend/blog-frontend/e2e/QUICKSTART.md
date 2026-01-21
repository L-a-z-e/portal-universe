# E2E 테스트 빠른 시작 가이드

## 1. 사전 준비

### Playwright 브라우저 설치
```bash
npx playwright install
```

이 명령은 Chromium, Firefox, WebKit 브라우저를 자동으로 다운로드합니다.

## 2. 애플리케이션 빌드

```bash
npm run build:dev
```

## 3. 테스트 실행 (3가지 방법)

### 방법 1: 자동 실행 (권장)
Playwright가 자동으로 프리뷰 서버를 시작하고 테스트를 실행합니다.

```bash
npm run test:e2e
```

### 방법 2: UI 모드 (개발 시 권장)
대화형 UI에서 테스트를 실행하고 디버깅할 수 있습니다.

```bash
npm run test:e2e:ui
```

### 방법 3: 수동 서버 + 테스트
서버를 직접 제어하려는 경우:

```bash
# 터미널 1: 프리뷰 서버 실행
npm run preview

# 터미널 2: 테스트 실행
npm run test:e2e
```

## 4. 특정 테스트만 실행

```bash
# 시리즈 기능만 테스트
npx playwright test series.spec.ts

# 좋아요 기능만 테스트
npx playwright test like.spec.ts

# 브라우저를 보면서 실행
npx playwright test like.spec.ts --headed

# 특정 브라우저만
npx playwright test --project=chromium
```

## 5. 테스트 결과 확인

테스트 실행 후 자동으로 HTML 리포트가 생성됩니다.

```bash
npm run test:e2e:report
```

브라우저에서 테스트 결과, 스크린샷, 비디오를 확인할 수 있습니다.

## 6. 디버깅

### 방법 1: UI 모드 사용
```bash
npm run test:e2e:ui
```

### 방법 2: 디버그 모드
```bash
npm run test:e2e:debug
```

Playwright Inspector가 열리며 단계별로 실행할 수 있습니다.

### 방법 3: 특정 테스트만 디버그
```bash
npx playwright test like.spec.ts --debug
```

## 7. 주요 옵션

```bash
# 실패한 테스트만 재실행
npx playwright test --last-failed

# 특정 브라우저만
npx playwright test --project=chromium --project=firefox

# 병렬 실행 워커 수 지정
npx playwright test --workers=2

# 헤드리스 모드 비활성화
npx playwright test --headed

# 특정 테스트 파일만
npx playwright test e2e/tests/like.spec.ts

# grep으로 테스트 필터링
npx playwright test --grep "should display"
```

## 8. 문제 해결

### 포트가 이미 사용 중
```bash
# 프로세스 찾기
lsof -i :30001

# 종료
kill -9 <PID>
```

### 브라우저 다운로드 오류
```bash
# 재설치
npx playwright install --force

# 의존성 포함 설치
npx playwright install --with-deps
```

### 타임아웃 오류
`playwright.config.ts`에서 타임아웃 조정:

```typescript
use: {
  actionTimeout: 30000,
  navigationTimeout: 30000,
}
```

## 9. 다음 단계

- `e2e/README.md`: 전체 문서 참조
- `e2e/tests/`: 테스트 파일 확인
- `playwright.config.ts`: 설정 파일 커스터마이징

## 10. 유용한 명령어 요약

```bash
# 기본 실행
npm run test:e2e

# UI 모드 (개발 시)
npm run test:e2e:ui

# 디버그
npm run test:e2e:debug

# 헤드 모드
npm run test:e2e:headed

# 결과 보기
npm run test:e2e:report
```

Happy Testing! 🎭
