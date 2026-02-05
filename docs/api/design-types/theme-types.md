---
id: theme-types
title: Theme Types Reference
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: documenter
tags: [api, typescript, theme-types]
related:
  - component-types
  - api-types
---

# Theme Types Reference

Common 타입 정의 레퍼런스입니다. 컴포넌트 Props에서 사용되는 기본 타입들과 서비스별 테마 설정 타입을 정의합니다.

> Source: `frontend/design-types/src/common.ts`, `frontend/design-types/src/index.ts`

## Theme Config Types

### ServiceType

Portal Universe 서비스 식별자입니다. `data-service` HTML 속성과 연동됩니다.

```ts
type ServiceType = 'portal' | 'blog' | 'shopping';
```

| Value | Description | Port |
|-------|-------------|------|
| `portal` | 메인 포털 셸 | 30000 |
| `blog` | 블로그 서비스 | 30001 |
| `shopping` | 쇼핑 서비스 | 30002 |

### ThemeMode

테마 모드입니다.

```ts
type ThemeMode = 'light' | 'dark' | 'system';
```

| Value | Description |
|-------|-------------|
| `light` | 라이트 모드 |
| `dark` | 다크 모드 |
| `system` | OS 설정 따르기 (`prefers-color-scheme`) |

### ThemeConfig

테마 설정 객체입니다.

```ts
interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}
```

| Prop | Type | Description |
|------|------|-------------|
| `service` | `ServiceType` | 현재 활성 서비스 |
| `mode` | `ThemeMode` | 현재 테마 모드 |

## Size Types

### Size

기본 크기 타입으로 대부분의 컴포넌트에서 사용됩니다.

```ts
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
```

| Value | Description | Use Case |
|-------|-------------|----------|
| `xs` | Extra small | Badge, 소형 버튼 |
| `sm` | Small | 밀집된 UI |
| `md` | Medium (기본) | 일반적인 경우 |
| `lg` | Large | 강조된 요소 |
| `xl` | Extra large | 히어로 섹션 |

### ExtendedSize

Avatar 등 더 큰 크기가 필요한 컴포넌트용입니다.

```ts
type ExtendedSize = Size | '2xl';
```

### FormSize

폼 필드 컴포넌트에서 사용되는 3단계 크기입니다.

```ts
type FormSize = 'sm' | 'md' | 'lg';
```

## Variant Types

### ButtonVariant

버튼 스타일 변형입니다.

```ts
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
```

| Value | Description | Use Case |
|-------|-------------|----------|
| `primary` | 주요 액션 | 폼 제출, CTA |
| `secondary` | 보조 액션 | 취소, 보조 버튼 |
| `ghost` | 배경 없음 | 아이콘 버튼, 텍스트 버튼 |
| `outline` | 테두리만 | 덜 중요한 액션 |
| `danger` | 위험 액션 | 삭제, 경고 |

### BadgeVariant

Badge 스타일 변형입니다.

```ts
type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'outline';
```

### StatusVariant

상태 표시용 변형 (Alert, Toast 등)입니다.

```ts
type StatusVariant = 'info' | 'success' | 'warning' | 'error';
```

| Value | Color | Use Case |
|-------|-------|----------|
| `info` | Blue | 정보성 메시지 |
| `success` | Green | 성공 메시지 |
| `warning` | Yellow | 경고 메시지 |
| `error` | Red | 에러 메시지 |

### TagVariant

Tag 컴포넌트 변형입니다.

```ts
type TagVariant = 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info';
```

### CardVariant

Card 컴포넌트 변형입니다.

```ts
type CardVariant = 'elevated' | 'outlined' | 'flat' | 'glass' | 'interactive';
```

| Value | Description |
|-------|-------------|
| `elevated` | 그림자가 있는 카드 |
| `outlined` | 테두리만 있는 카드 |
| `flat` | 배경만 있는 카드 |
| `glass` | 글래스모피즘 효과 |
| `interactive` | hover 효과가 있는 카드 |

## Layout Types

### Orientation

방향 설정용 타입입니다.

```ts
type Orientation = 'horizontal' | 'vertical';
```

### Align

flex/grid align-items 옵션입니다.

```ts
type Align = 'start' | 'center' | 'end' | 'stretch' | 'baseline';
```

### Justify

flex/grid justify-content 옵션입니다.

```ts
type Justify = 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly';
```

### GapSize

Stack 컴포넌트 등의 간격 크기입니다.

```ts
type GapSize = 'none' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
```

### PaddingSize

패딩 크기 옵션입니다.

```ts
type PaddingSize = 'none' | 'sm' | 'md' | 'lg' | 'xl';
```

> **참고**: `ContainerProps.padding`과 `DividerProps.spacing`은 `Exclude<PaddingSize, 'xl'>` 타입으로 `'xl'`을 제외합니다. `CardProps.padding`은 전체 `PaddingSize`를 사용합니다.

### MaxWidth

Container 컴포넌트의 최대 너비입니다.

```ts
type MaxWidth = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';
```

| Value | Width |
|-------|-------|
| `sm` | 640px |
| `md` | 768px |
| `lg` | 1024px |
| `xl` | 1280px |
| `2xl` | 1536px |
| `full` | 100% |

## Element Types

### ContainerElement

Container 컴포넌트의 HTML 요소 타입입니다.

```ts
type ContainerElement = 'div' | 'section' | 'article' | 'main' | 'aside';
```

### StackElement

Stack 컴포넌트의 HTML 요소 타입입니다.

```ts
type StackElement = 'div' | 'section' | 'article' | 'ul' | 'ol' | 'nav';
```

## Navigation Types

### LinkTarget

링크의 target 속성입니다.

```ts
type LinkTarget = '_self' | '_blank' | '_parent' | '_top';
```

### LinkVariant

Link 컴포넌트 스타일입니다.

```ts
type LinkVariant = 'default' | 'primary' | 'muted' | 'underline';
```

### TabVariant

Tabs 컴포넌트 스타일입니다.

```ts
type TabVariant = 'default' | 'pills' | 'underline';
```

## Dropdown Types

### DropdownPlacement

Dropdown/Tooltip 위치 옵션입니다.

```ts
type DropdownPlacement =
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'top' | 'top-start' | 'top-end';
```

### DropdownTrigger

Dropdown 트리거 방식입니다.

```ts
type DropdownTrigger = 'click' | 'hover';
```

## Toast Types

### ToastPosition

Toast 위치 옵션입니다.

```ts
type ToastPosition =
  | 'top-right' | 'top-left' | 'top-center'
  | 'bottom-right' | 'bottom-left' | 'bottom-center';
```

## Animation Types

### SkeletonAnimation

Skeleton 컴포넌트 애니메이션 타입입니다.

```ts
type SkeletonAnimation = 'pulse' | 'wave' | 'none';
```

### SkeletonVariant

Skeleton 컴포넌트 모양 변형입니다.

```ts
type SkeletonVariant = 'text' | 'circular' | 'rectangular' | 'rounded';
```

## Divider Types

### DividerVariant

Divider 스타일입니다.

```ts
type DividerVariant = 'solid' | 'dashed' | 'dotted';
```

### DividerColor

Divider 색상입니다.

```ts
type DividerColor = 'default' | 'muted' | 'strong';
```

## Avatar Types

### AvatarShape

Avatar 모양입니다.

```ts
type AvatarShape = 'circle' | 'square';
```

### AvatarStatus

Avatar 상태 표시입니다.

```ts
type AvatarStatus = 'online' | 'offline' | 'busy' | 'away';
```

## Other Types

### SpinnerColor

Spinner 색상입니다.

```ts
type SpinnerColor = 'primary' | 'current' | 'white';
```

### SwitchColor

Switch 활성화 색상입니다.

```ts
type SwitchColor = 'primary' | 'success' | 'warning' | 'error';
```

### LabelPosition

레이블 위치입니다.

```ts
type LabelPosition = 'left' | 'right';
```
