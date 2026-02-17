# ADR-043: Design System 패키지 통합 (4→3)

**Status**: Accepted
**Date**: 2026-02-17
**Author**: Laze
**Supersedes**: 기존 4패키지 구조 (design-tokens + design-types + design-system-vue + design-system-react)

## Context

기존 디자인 시스템은 4개 패키지로 분리되어 있었다: `design-tokens` (토큰 정의 + 빌드), `design-types` (공유 타입), `design-system-vue` (Vue 컴포넌트), `design-system-react` (React 컴포넌트). 패키지 간 의존성 관리가 복잡하고, 컴포넌트 variant 클래스가 각 프레임워크 패키지에 중복 정의되어 있어 불일치 위험이 있었다. 또한 빌드 순서가 4단계(tokens → types → vue/react)로 느렸다.

## Decision

4개 패키지를 3개로 통합한다: `design-core` (토큰 + 타입 + variant 정의), `design-vue` (Vue 컴포넌트), `design-react` (React 컴포넌트).

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현행 유지 (4패키지) | 변경 불필요 | 중복 variant, 복잡한 의존성, 느린 빌드 |
| ② 3패키지 통합 (채택) | variant SSOT, 빌드 단순화, 의존성 감소 | 마이그레이션 비용 |
| ③ 모노 패키지 (1개) | 최단 의존성 | 프레임워크별 tree-shaking 불가, 번들 비대화 |

## Rationale

- **Variant Single Source of Truth**: 버튼, 탭 등의 Tailwind 클래스 정의를 `design-core/src/variants/`에 한 곳에서 관리. Vue/React 양쪽에서 import하여 불일치 제거
- **빌드 단계 축소**: 4단계(tokens → types → vue + react) → 2단계(core → vue + react)
- **패키지 수 감소**: `design-tokens`와 `design-types`가 항상 함께 사용되므로 하나로 통합이 자연스러움
- **Tailwind content scan 단순화**: variant 파일이 한 패키지에 모여있어 content 경로 관리 용이
- **타입 colocate**: 토큰과 타입이 같은 패키지에 있어 import 경로가 단순해짐

## Trade-offs

✅ **장점**:
- Variant 정의 중복 제거 (버튼 사이즈, 탭 사이즈 등 한 곳에서 정의)
- 빌드 파이프라인 단순화 (`build:design` 한 명령어로 3패키지 순차 빌드)
- import 경로 통일 (`@portal/design-core`에서 토큰, 타입, variant 모두 제공)

⚠️ **단점 및 완화**:
- 기존 import 경로 변경 필요 → (완화: 일괄 마이그레이션으로 한 번에 처리)
- Tailwind content scan에 `design-core` 경로 명시 필요 → (완화: 각 앱의 tailwind.config.js에 표준 경로 추가)

## Implementation

- `frontend/design-core/` — 토큰 JSON, 빌드 스크립트, TypeScript 타입, variant 정의, Tailwind preset
- `frontend/design-vue/` — Vue 3 컴포넌트, composable, 스타일
- `frontend/design-react/` — React 18 컴포넌트, hook, 유틸리티
- `frontend/*/tailwind.config.js` — design-core content 경로 추가 (9개 앱)

### 패키지 매핑

| Before | After |
|--------|-------|
| `@portal/design-tokens` | `@portal/design-core` |
| `@portal/design-types` | `@portal/design-core` |
| `@portal/design-system-vue` | `@portal/design-vue` |
| `@portal/design-system-react` | `@portal/design-react` |

### Tailwind Config 패턴

각 소비자 앱의 `tailwind.config.js`에 다음 content 경로 추가 필수:

```js
content: [
  // ... 기존 경로
  '../design-core/src/variants/**/*.ts',
  '../design-core/src/styles/**/*.css',
  '../design-vue/src/**/*.{vue,js,ts}',   // Vue 앱
  // 또는
  '../design-react/src/**/*.{js,ts,jsx,tsx}', // React 앱
]
```

## References

- [Design System System Overview](../architecture/design-system/system-overview.md)
- [Token System](../architecture/design-system/token-system.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-17 | 초안 작성 (구현 완료, Accepted) | Laze |
