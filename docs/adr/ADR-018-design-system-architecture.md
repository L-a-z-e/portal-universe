# ADR-018: Design System Architecture - Single Source of Truth 구조

**Status**: Accepted
**Date**: 2026-02-01
**Source**: PDCA archive (design-system)

## Context

Portal Universe는 4개 계층의 디자인 시스템을 운영 중이었다: design-types(타입), design-tokens(토큰), design-system-vue(Vue 컴포넌트), design-system-react(React 컴포넌트). 그러나 CSS 변수가 tokens, vue, react 3곳에 중복 정의되고, Tailwind preset도 3중 복사되어 있었다. 토큰 값 변경 시 3곳을 모두 수동 동기화해야 하는 유지보수 문제가 발생했다.

## Decision

**Single Source of Truth 원칙**을 적용하여 CSS 변수와 Tailwind preset을 `design-tokens` 모듈에만 정의하고, Vue/React 모듈은 이를 import하여 사용한다.

## Rationale

- **CSS 변수 통합**: `design-tokens/dist/tokens.css`를 유일한 소스로 하고, Vue/React의 `src/styles/index.css`에서 `@import '@portal/design-tokens/css'` 사용
- **Tailwind preset 통합**: `design-tokens/tailwind.preset.js`를 유일한 소스로 하고, Vue/React의 `tailwind.config.js`에서 `presets: [@portal/design-tokens/tailwind]` 참조
- **타입 통합**: Vue 컴포넌트도 `@portal/design-types`에서 Props를 import하도록 변경 (기존 24개 로컬 `.types.ts` 제거)
- **React CSS 번들링**: Vue와 동일하게 `dist/design-system.css`로 번들하여 소비자 앱에서 일관된 방식으로 사용

## Trade-offs

✅ **장점**:
- 토큰 변경 시 1곳(design-tokens)만 수정하면 전체 전파
- light variant 플러그인 불일치 해소 (tokens에만 있던 플러그인이 Vue/React에 자동 적용)
- CSS 번들 크기 감소 (중복 제거로 373KB → 최적화)
- 프레임워크 간 타입 일관성 확보

⚠️ **단점 및 완화**:
- CSS import 경로 변경으로 소비자 앱 빌드 실패 가능 → 변경 전/후 빌드 검증, 단계적 적용
- Module Federation에서 CSS 로딩 순서 변경 → Host/Remote 모두 E2E 테스트
- Vue 타입 마이그레이션 시간 소요 → 패턴화된 일괄 변환 스크립트 사용

## Implementation

- **토큰 빌드**: `frontend/design-tokens/scripts/build-tokens.js` - `@tailwind` directives 제거
- **Vue 스타일**: `design-system-vue/src/styles/index.css` - 인라인 CSS 변수 제거, `@import` 추가
- **React 스타일**: `design-system-react/src/styles/index.css` - 동일 패턴
- **React 번들링**: `design-system-react/vite.config.ts` - `cssCodeSplit: false`, `assetFileNames` 설정
- **Vue 타입**: 24개 `.types.ts` → `@portal/design-types` import로 교체

## References

- PDCA: `pdca/archive/2026-02/design-system/`
- 관련 규칙: `.claude/rules/tailwind.md`
- 빌드 스크립트: `frontend/package.json` - `build:design`
