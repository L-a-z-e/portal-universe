# E2E Test Refactoring Plan

> Feature: e2e-test-refactoring
> 목표: `@portal/react-bridge` 마이그레이션 후 local 환경에서 Playwright E2E 테스트를 실행하여 실제 문제를 식별하고, 테스트 코드 vs 실제 코드를 구분하여 구조적으로 수정

## 배경

### 최근 변경사항 (authstore-handling PDCA)
- `@portal/react-bridge` 패키지 생성 (useSyncExternalStore 기반)
- Shopping/Prism Frontend: 로컬 `authStore.ts`, `usePortalStore.ts`, `RequireAuth.tsx` 삭제
- 토큰 주입 방식 변경: `isBridgeReady() → getAdapter('auth').getAccessToken()` → window globals fallback
- `PortalBridgeProvider`가 bridge 초기화 보장 (5초 timeout hack 제거)
- Portal Shell `storeAdapter.ts`에 `getAccessToken()`, `requestLogin()` 추가

### E2E 테스트 인프라 현황
- **주요 테스트 스위트**: `e2e-tests/` (33 spec files) — Playwright
- **인증 전략**: auth.setup.ts → storageState + access-token.json 저장 → test-fixtures.ts에서 refresh 가로채기
- **Window globals 의존**: `__PORTAL_ACCESS_TOKEN__`, `__PORTAL_GET_ACCESS_TOKEN__` 으로 토큰 주입
- **MF 로딩 핸들링**: `gotoBlogPage()`, `gotoShoppingPage()`, `gotoPrismPage()` 헬퍼

## 실행 전략

### Phase 1: Local 환경 기동 및 빌드 검증

1. `docker compose -f docker-compose-local.yml up -d` (인프라 서비스)
2. 각 백엔드 서비스 빌드/실행 (`./gradlew bootRun --args='--spring.profiles.active=local'`)
3. `npm run build` → `npm run dev:portal` (프론트엔드)
4. http://localhost:30000 접속 확인

### Phase 2: E2E 테스트 실행 및 실패 수집

1. `e2e-tests/` 디렉토리에서 `npx playwright test` 실행
2. `BASE_URL=http://localhost:30000` 설정 (local은 HTTP)
3. 실패 테스트 목록 수집 및 분류

### Phase 3: 실패 원인 분석 — "테스트 코드 문제" vs "실제 코드 문제" 분류

각 실패에 대해 아래 기준으로 판단:

#### 테스트 코드를 고쳐야 하는 경우
- **Selector 불일치**: 실제 UI 렌더링과 테스트의 CSS selector/text matcher가 다름
- **타이밍 문제**: `waitForTimeout` 하드코딩으로 인한 flaky test
- **삭제된 코드 참조**: 삭제된 컴포넌트/클래스를 테스트가 여전히 참조
- **인증 흐름 변경 미반영**: `RequireAuth` 동작이 변경되었는데 테스트가 구 동작 기대
- **Window globals 의존성**: react-bridge 마이그레이션으로 globals 동작이 달라졌는데 테스트가 구 패턴 사용
- **auth helper 코드 중복**: `gotoBlogPage`, `gotoShoppingPage`, `gotoPrismPage`가 동일 코드 복사

#### 실제 코드를 고쳐야 하는 경우
- **Bridge 초기화 실패**: `PortalBridgeProvider`가 MF 환경에서 정상 초기화 안됨
- **토큰 주입 실패**: `getAccessToken()` 체인에서 실제 토큰이 전달 안됨
- **RequireAuth 리다이렉트 오류**: `requestLogin()` 호출이 Portal Shell에서 동작 안함
- **API 호출 실패**: axios interceptor의 토큰 주입이 실패하여 401/403 발생
- **라우팅 깨짐**: router 변경으로 경로 매칭 실패
- **상태 동기화 실패**: useSyncExternalStore가 adapter 상태를 정상 구독 못함

### Phase 4: 구조적 수정

#### 테스트 인프라 개선 (테스트 코드 수정)
1. **auth helper 중복 제거**: `gotoServicePage(page, service, urlPath, contentSelector)` 제네릭 함수로 통합
2. **waitForTimeout → waitFor 기반으로 전환**: 하드코딩된 timeout 대신 조건 기반 대기
3. **test-fixtures.ts 업데이트**: react-bridge의 토큰 흐름에 맞게 intercept 로직 조정
4. **Selector 현대화**: 삭제/변경된 컴포넌트에 맞게 selector 업데이트

#### 실제 코드 수정 (필요 시)
1. **window globals fallback 보장**: react-bridge가 standalone/embedded 양쪽에서 동작
2. **API client 토큰 체인**: bridge → globals → localStorage 순서 정상 동작 확인
3. **PortalBridgeProvider 에러 핸들링**: MF 로드 실패 시 graceful degradation

### Phase 5: 재실행 및 검증

1. 수정 후 전체 E2E 테스트 재실행
2. 남은 실패 분석 및 2차 수정
3. 최종 결과 정리

## 핵심 원칙

### 시니어 개발자 관점의 수정 기준
1. **하드코딩 금지**: `waitForTimeout(3000)` 같은 magic number 대신 조건 기반 대기
2. **땜질 금지**: `try-catch(() => {})` 로 에러를 삼키는 패턴 → 명시적 에러 처리
3. **구조적 접근**: 동일 패턴 반복 → 헬퍼/fixture로 추출
4. **원인-결과 추적**: "왜 실패하는가" → "어디를 고칠 것인가" 명확히 구분
5. **Backward compatible**: 기존 docker/k8s 환경 테스트도 깨지지 않게

## 예상 영향 범위

| 영역 | 파일 | 예상 변경 |
|------|------|----------|
| Test Helper | `e2e-tests/tests/helpers/auth.ts` | 중복 제거, 조건 기반 대기 |
| Test Fixture | `e2e-tests/tests/helpers/test-fixtures.ts` | react-bridge 토큰 흐름 반영 |
| Test Config | `e2e-tests/playwright.config.ts` | BASE_URL default 조정 가능 |
| Auth Setup | `e2e-tests/tests/auth.setup.ts` | window globals 캡처 로직 검증 |
| Shopping Tests | `e2e-tests/tests/shopping/*.spec.ts` | selector/assertion 업데이트 |
| Prism Tests | `e2e-tests/tests/prism/*.spec.ts` | selector/assertion 업데이트 |
| Blog Tests | `e2e-tests/tests/blog/*.spec.ts` | 영향 최소 (Vue, react-bridge 무관) |
| react-bridge | `frontend/react-bridge/src/` | 실제 버그 발견 시만 수정 |
| Shopping FE | `frontend/shopping-frontend/src/` | 실제 버그 발견 시만 수정 |
| Prism FE | `frontend/prism-frontend/src/` | 실제 버그 발견 시만 수정 |

## 판단 기준 요약

```
실패 발견
  ├── UI가 정상 렌더링되는데 테스트가 못 찾음 → 테스트 selector 수정
  ├── UI가 렌더링 안 됨 → 실제 코드 버그
  ├── 인증이 필요한데 토큰이 없음
  │     ├── Portal Shell 토큰이 bridge로 전달 안됨 → 실제 코드 (bridge/adapter)
  │     └── 테스트가 토큰 주입을 못함 → 테스트 fixture 수정
  ├── 타이밍으로 flaky → 테스트 대기 조건 개선
  └── API 404/500 → 백엔드 문제 (이 PDCA 스코프 외)
```

## 스코프 제외

- 백엔드 서비스 버그 수정 (API 500 등)
- blog-frontend 테스트 (Vue + Pinia 직접, react-bridge 무관)
- `frontend/blog-frontend/e2e/` 별도 테스트 스위트
- `frontend/e2e/` Portal Shell 단독 테스트 스위트
- CI/CD 환경 설정
- Docker/Kubernetes 환경 테스트
