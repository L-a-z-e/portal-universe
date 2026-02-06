---
id: arch-component-matrix
title: 컴포넌트 비교 매트릭스
type: architecture
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [design-system, vue, react, comparison, matrix]
related:
  - arch-vue-components
  - arch-react-components
  - arch-design-system-overview
---

# 컴포넌트 비교 매트릭스

## 개요

Vue 3과 React 18 디자인 시스템 라이브러리 간의 컴포넌트 지원 현황, API 차이, 패턴 비교를 제공한다. 두 라이브러리는 `@portal/design-types`의 공유 타입을 기반으로 일관된 Props 인터페이스를 유지한다.

| 항목 | Vue | React |
|------|-----|-------|
| 총 컴포넌트 | 26 | 30 |
| 공유 컴포넌트 | 25 | 25 |
| 프레임워크 전용 | 1 (ToastContainer) | 5 (Pagination, Popover, Progress, Table, Tooltip) |
| Composable / Hook | 3 | 3 |

## 컴포넌트 지원 매트릭스

### 전체 컴포넌트 (31개)

| # | 컴포넌트 | Vue | React | 카테고리 | 비고 |
|---|---------|-----|-------|---------|------|
| 1 | Alert | O | O | Feedback | |
| 2 | Avatar | O | O | Display | |
| 3 | Badge | O | O | Feedback | |
| 4 | Breadcrumb | O | O | Navigation | |
| 5 | Button | O | O | Form | |
| 6 | Card | O | O | Layout | |
| 7 | Checkbox | O | O | Form | |
| 8 | Container | O | O | Layout | |
| 9 | Divider | O | O | Layout | |
| 10 | Dropdown | O | O | Navigation | |
| 11 | FormField | O | O | Layout | |
| 12 | Input | O | O | Form | |
| 13 | Link | O | O | Navigation | |
| 14 | Modal | O | O | Feedback | |
| 15 | Pagination | - | O | Navigation | React-only |
| 16 | Popover | - | O | Feedback | React-only |
| 17 | Progress | - | O | Feedback | React-only |
| 18 | Radio | O | O | Form | |
| 19 | SearchBar | O | O | Form | |
| 20 | Select | O | O | Form | |
| 21 | Skeleton | O | O | Feedback | |
| 22 | Spinner | O | O | Feedback | |
| 23 | Stack | O | O | Layout | |
| 24 | Switch | O | O | Form | |
| 25 | Table | - | O | Display | React-only |
| 26 | Tabs | O | O | Navigation | |
| 27 | Tag | O | O | Feedback | |
| 28 | Textarea | O | O | Form | |
| 29 | Toast | O | O | Feedback | |
| 30 | ToastContainer | O | - | Utility | Vue-only |
| 31 | Tooltip | - | O | Feedback | React-only |

**요약**: 공유 25, Vue-only 1, React-only 5

### React-only 컴포넌트 사유

| 컴포넌트 | 사유 |
|---------|------|
| Pagination | Shopping 서비스 상품 목록 페이지네이션. Blog는 무한 스크롤 사용. |
| Popover | Prism 서비스 인터랙티브 정보 표시. Vue는 Dropdown으로 대체. |
| Progress | 파일 업로드/로딩 진행률. Shopping/Prism 서비스 요구사항. |
| Table | 데이터 테이블 (정렬/필터). Shopping 상품 관리, Prism 데이터 표시. |
| Tooltip | 아이콘/버튼 호버 설명. Vue는 title 속성이나 Dropdown으로 대체. |

### Vue-only 컴포넌트 사유

| 컴포넌트 | 사유 |
|---------|------|
| ToastContainer | Vue의 Toast 위치 관리 전용 래퍼. React에서는 Toast 내부에서 처리. |

## Composable vs Hook API 비교

### 공통 API (3쌍)

| Vue Composable | React Hook | 기능 |
|---------------|-----------|------|
| `useTheme()` | `useTheme(options?)` | 서비스 테마 + Dark/Light 모드 |
| `useToast()` | `useToast()` | 토스트 알림 관리 |
| `useApiError()` | `useApiError()` | API 에러 → 사용자 메시지 변환 |

### useTheme 상세 비교

| 기능 | Vue | React |
|------|-----|-------|
| 서비스 상태 | `currentService` (Ref) | `service` (state) |
| 테마 상태 | `currentTheme` (Ref) | `mode` (state) |
| System 모드 | 미지원 | `'system'` + `resolvedMode` |
| 초기화 | `initTheme()` 수동 호출 | useEffect 자동 |
| 저장소 | localStorage 저장 | DOM 속성만 |
| ServiceType | `'portal' \| 'blog' \| 'shopping'` | `@portal/design-types` 참조 |
| ThemeMode | `'light' \| 'dark'` | `'light' \| 'dark' \| 'system'` |

### useToast 상세 비교

| 기능 | Vue | React |
|------|-----|-------|
| Provider | 불필요 (composable 내부 관리) | `ToastProvider` 필수 |
| 메서드 | `show()`, `success()`, `error()` | `show()`, `success()`, `error()` |
| 제거 | `remove(id)` | `remove(id)` |

## 패턴 차이

### 상태 바인딩

| 패턴 | Vue | React |
|------|-----|-------|
| 양방향 바인딩 | `v-model="value"` | `value={v} onChange={setV}` |
| Props 타입 | `defineProps<Props>()` | `interface Props extends ...` |
| Ref 전달 | `defineExpose` (필요 시) | `forwardRef` 필수 |
| 기본값 | `withDefaults()` | 구조분해 기본값 |
| 이벤트 | `defineEmits` | callback props |

### 조건부 콘텐츠

| 패턴 | Vue | React |
|------|-----|-------|
| 조건부 렌더링 | `v-if` / `v-show` | `{cond && <Comp />}` |
| 리스트 렌더링 | `v-for` | `.map()` |
| 슬롯 | `<slot>` / `<slot name="x">` | `children` / render props |
| 클래스 바인딩 | `:class="{ active: isActive }"` | `cn('base', isActive && 'active')` |

### 스타일링

| 항목 | Vue | React |
|------|-----|-------|
| 유틸리티 | Tailwind 직접 사용 | `cn()` (clsx + tailwind-merge) |
| Scoped 스타일 | `<style scoped>` 가능 | CSS Modules 또는 Tailwind only |
| 클래스 오버라이드 | `:class` 바인딩 | `className` prop |

## 공유 타입 (`@portal/design-types`)

### 주요 타입

```typescript
// 공통 타입
export type ServiceType = 'portal' | 'blog' | 'shopping' | 'prism'
export type ThemeMode = 'light' | 'dark' | 'system'
export type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'

// Variant 타입
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger'
export type BadgeVariant = 'default' | 'success' | 'error' | 'warning' | 'info'
export type StatusVariant = 'success' | 'error' | 'warning' | 'info'
export type CardVariant = 'default' | 'outlined' | 'elevated'

// 컴포넌트 Props (30+ 인터페이스)
export interface ButtonProps { variant?: ButtonVariant; size?: Size; ... }
export interface InputProps { size?: Size; error?: boolean; ... }
export interface ModalProps { isOpen?: boolean; size?: Size; ... }
export interface TableProps { columns: Column[]; data: Row[]; ... }
// ... 30+ 더
```

### 타입 사용 패턴

**Vue**:
```vue
<script setup lang="ts">
import type { ButtonProps } from '@portal/design-types'
const props = withDefaults(defineProps<ButtonProps>(), { variant: 'primary' })
</script>
```

**React**:
```tsx
import type { ButtonProps } from '@portal/design-types'
export interface ButtonComponentProps
  extends ButtonProps, Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode
}
```

## 새 컴포넌트 추가 체크리스트

### 1. 타입 정의 (`@portal/design-types`)

- [ ] `src/components.ts`에 Props 인터페이스 추가
- [ ] 필요 시 `src/common.ts`에 공통 variant 타입 추가
- [ ] `src/index.ts`에서 export

### 2. Vue 구현 (`@portal/design-system-vue`)

- [ ] `src/components/ComponentName.vue` 생성
- [ ] `<script setup>` + `@portal/design-types` import
- [ ] `src/index.ts`에서 export
- [ ] Storybook story 작성

### 3. React 구현 (`@portal/design-system-react`)

- [ ] `src/components/ComponentName/ComponentName.tsx` 생성
- [ ] `forwardRef` + `@portal/design-types` import
- [ ] `displayName` 설정
- [ ] `src/components/ComponentName/index.ts` barrel export
- [ ] `src/index.ts`에서 export
- [ ] `ComponentName.test.tsx` 테스트 작성
- [ ] `ComponentName.stories.tsx` Storybook story 작성

### 4. 검증

- [ ] Vue/React Props 인터페이스가 `@portal/design-types`와 일치
- [ ] 빌드 성공 (`npm run build`)
- [ ] Storybook에서 렌더링 확인
- [ ] 이 문서의 매트릭스 테이블 업데이트

## 관련 문서

- [Vue Components](./vue-components.md) - Vue 컴포넌트 상세
- [React Components](./react-components.md) - React 컴포넌트 상세
- [System Overview](./system-overview.md) - 전체 아키텍처 개요

**최종 업데이트**: 2026-02-06
