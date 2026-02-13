# ADR-040: Frontend Error Handling Standardization

**Status**: Accepted
**Date**: 2026-02-14
**Author**: Laze
**Supersedes**: 개별 앱의 로컬 에러 핸들러 패턴

## Context

6개 프론트엔드 앱(portal-shell, blog-frontend, shopping-frontend, prism-frontend, drive-frontend, admin-frontend)이 각자 다른 방식으로 에러를 처리하고 있었다. shopping-frontend와 prism-frontend는 로컬 `ErrorBoundary` 컴포넌트를 구현했고, portal-shell은 15줄의 인라인 `errorHandler`를 사용했으며, 나머지 3개 앱은 에러 핸들링이 누락되었다. 로깅 방식도 앱마다 달라 개발/운영 환경에서 일관된 에러 추적이 어려웠다.

## Decision

Design System에 에러 핸들링 및 로깅 유틸리티를 추가하여 전체 프론트엔드 앱에서 일관된 에러 처리 패턴을 적용한다. Framework-agnostic 로거(`@portal/design-types`)와 Framework-specific 에러 핸들러(React: `ErrorBoundary`, Vue: `setupErrorHandler`)를 제공한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 앱별 로컬 구현 유지 | 앱별 커스터마이징 자유 | 에러 처리 정책 불일치, 코드 중복 |
| ② 별도 npm 패키지 분리 | Design System과 독립적 버전 관리 | 의존성 추가, Design System과의 타입 통합 복잡 |
| ③ Design System 통합 (채택) | 타입 시스템 재사용, 단일 소스, 버전 통합 | Design System이 무거워질 수 있음 |
| ④ Sentry 같은 외부 서비스만 의존 | 강력한 분석 도구 | 외부 의존성, 개발 환경 노이즈 |

## Rationale

- **타입 시스템 재사용**: `@portal/design-types`에 로거를 추가하여 기존 컴포넌트 Props 타입 패턴과 동일하게 관리. Vue/React 양쪽에서 동일한 Logger 인터페이스 사용.
- **단일 소스**: 로깅/에러 핸들링 정책이 Design System에 집중되어 업데이트 시 모든 앱에 자동 적용.
- **개발/프로덕션 분리**: `NODE_ENV` 기반으로 로그 레벨 자동 조정. 프로덕션에서는 `console.log`/`debug` 제거 (Vite `esbuild.pure` 설정).
- **확장 가능**: `ErrorReporter` 인터페이스로 Sentry/Datadog 같은 APM 도구 플러그인 가능. 기본 구현은 console만 사용.

## Trade-offs

✅ **장점**:
- 6개 앱에서 동일한 에러 핸들링 패턴 (일관성)
- production 빌드에서 자동 로그 제거 (번들 크기 감소, 보안 개선)
- 로컬 에러 핸들러 코드 60+ 줄 → 1-3줄로 감소 (유지보수성)
- ErrorReporter 인터페이스로 APM 통합 가능 (확장성)

⚠️ **단점 및 완화**:
- Design System 패키지가 무거워짐 → (완화: `@portal/design-types`의 logger는 단일 파일 80줄, tree-shaking 가능)
- 앱별 커스터마이징 제한 → (완화: `createLogger({ reporter: customReporter })` 인터페이스로 플러그인 가능)
- 기존 로컬 ErrorBoundary 제거 → (완화: 동일 Props 인터페이스로 마이그레이션 비용 최소화)

## Implementation

### 1. @portal/design-types (logger 유틸리티)
- `frontend/design-types/src/logger.ts` (신규)
  - `LogLevel`, `ErrorReporter` 인터페이스 정의
  - `createLogger({ moduleName, level?, reporter? })` 팩토리 함수
  - `NODE_ENV=production` 시 `log`/`debug` 레벨 자동 no-op

### 2. design-system-react (에러 핸들링)
- `frontend/design-system-react/src/components/ErrorBoundary/ErrorBoundary.tsx` (신규)
  - React Error Boundary 컴포넌트 (fallback UI 지원)
  - `useLogger()` hook으로 에러 로깅
- `frontend/design-system-react/src/hooks/useLogger.ts` (신규)
  - React 환경에서 logger 인스턴스 제공

### 3. design-system-vue (에러 핸들링)
- `frontend/design-system-vue/src/composables/setupErrorHandler.ts` (신규)
  - Vue `app.config.errorHandler` 설정 유틸리티
  - 전역 에러 핸들러 등록, router/vuex 에러 캐치
- `frontend/design-system-vue/src/composables/useLogger.ts` (신규)
  - Vue 환경에서 logger 인스턴스 제공

### 4. 6개 앱 적용
- **React apps** (shopping, prism): 로컬 ErrorBoundary 삭제 → `<ErrorBoundary>` import
- **Vue apps** (portal-shell, blog, drive, admin):
  - portal-shell의 15줄 인라인 errorHandler → `setupErrorHandler()` 1줄로 교체
  - blog/drive/admin에 `setupErrorHandler()` 적용 (기존 에러 핸들러 없음)

### 5. Production console stripping
- 6개 앱 `vite.config.ts`에 `esbuild.pure` 설정 추가:
  ```typescript
  esbuild: {
    pure: ['console.log', 'console.debug'],
  }
  ```
- production 빌드 시 `console.log`/`debug` 제거, `console.warn`/`error` 유지

## References

- [Design System Overview](../architecture/design-system/system-overview.md)
- [React Components](../architecture/design-system/react-components.md)
- [Vue Components](../architecture/design-system/vue-components.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-14 | 초안 작성 | Laze |
