---
id: component-types
title: Component Types Reference
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: documenter
tags: [api, typescript, component-types]
related:
  - theme-types
  - api-types
---

# Component Types Reference

모든 컴포넌트의 Props 타입 정의 레퍼런스입니다.

> Source: `frontend/design-types/src/components.ts`
> 공유 타입 참조: [theme-types.md](./theme-types.md) 참고

## Form Components

### ButtonProps

```ts
interface ButtonProps {
  variant?: ButtonVariant;
  size?: Exclude<Size, 'xl'>;
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
  type?: 'button' | 'submit' | 'reset';
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`ButtonVariant`](./theme-types.md#buttonvariant) | `'primary'` | 버튼 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 크기 (`'xl'` 제외) |
| `disabled` | `boolean` | `false` | 비활성화 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `fullWidth` | `boolean` | `false` | 전체 너비 |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | HTML 타입 |

### InputProps

```ts
interface InputProps {
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url';
  value?: string | number;
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  size?: FormSize;
  name?: string;
  id?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `type` | `'text' \| 'email' \| 'password' \| 'number' \| 'tel' \| 'url'` | `'text'` | 입력 타입 |
| `value` | `string \| number` | - | 입력 값 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `label` | `string` | - | 라벨 텍스트 |
| `required` | `boolean` | `false` | 필수 입력 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `name` | `string` | - | HTML name 속성 |
| `id` | `string` | - | HTML id 속성 |

### TextareaProps

`InputProps`를 확장하며 `type` 속성을 제외합니다.

```ts
interface TextareaProps extends Omit<InputProps, 'type'> {
  rows?: number;
  resize?: 'none' | 'vertical' | 'horizontal' | 'both';
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `rows` | `number` | - | 표시 행 수 |
| `resize` | `'none' \| 'vertical' \| 'horizontal' \| 'both'` | - | 크기 조절 방향 |
| *(+ InputProps의 모든 속성, `type` 제외)* | | | |

### CheckboxProps

```ts
interface CheckboxProps {
  checked?: boolean;
  indeterminate?: boolean;
  disabled?: boolean;
  label?: string;
  error?: boolean;
  errorMessage?: string;
  size?: FormSize;
  value?: string | number;
  name?: string;
  id?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `checked` | `boolean` | `false` | 체크 상태 |
| `indeterminate` | `boolean` | `false` | 불확정 상태 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `label` | `string` | - | 라벨 텍스트 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `value` | `string \| number` | - | 값 |
| `name` | `string` | - | HTML name 속성 |
| `id` | `string` | - | HTML id 속성 |

### RadioProps

```ts
interface RadioOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

interface RadioProps {
  value?: string | number;
  options: RadioOption[];
  name: string;
  direction?: Orientation;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  size?: FormSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string \| number` | - | 선택된 값 |
| `options` | `RadioOption[]` | **필수** | 라디오 옵션 목록 |
| `name` | `string` | **필수** | HTML name 속성 |
| `direction` | [`Orientation`](./theme-types.md#orientation) | `'vertical'` | 배치 방향 |
| `disabled` | `boolean` | `false` | 전체 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |

### SwitchProps

```ts
interface SwitchProps {
  checked?: boolean;
  disabled?: boolean;
  label?: string;
  labelPosition?: LabelPosition;
  size?: FormSize;
  activeColor?: SwitchColor;
  name?: string;
  id?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `checked` | `boolean` | `false` | 활성 상태 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `label` | `string` | - | 라벨 텍스트 |
| `labelPosition` | [`LabelPosition`](./theme-types.md#labelposition) | `'right'` | 라벨 위치 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `activeColor` | [`SwitchColor`](./theme-types.md#switchcolor) | `'primary'` | 활성화 색상 |
| `name` | `string` | - | HTML name 속성 |
| `id` | `string` | - | HTML id 속성 |

### SelectProps

```ts
interface SelectOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

interface SelectProps {
  value?: string | number | null;
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  clearable?: boolean;
  searchable?: boolean;
  size?: FormSize;
  name?: string;
  id?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string \| number \| null` | - | 선택된 값 |
| `options` | `SelectOption[]` | **필수** | 옵션 목록 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `label` | `string` | - | 라벨 텍스트 |
| `required` | `boolean` | `false` | 필수 선택 |
| `clearable` | `boolean` | `false` | 선택 초기화 허용 |
| `searchable` | `boolean` | `false` | 검색 기능 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `name` | `string` | - | HTML name 속성 |
| `id` | `string` | - | HTML id 속성 |

### FormFieldProps

폼 필드 래퍼 컴포넌트용 타입입니다.

```ts
interface FormFieldProps {
  label?: string;
  required?: boolean;
  error?: boolean;
  errorMessage?: string;
  helperText?: string;
  id?: string;
  disabled?: boolean;
  size?: FormSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `label` | `string` | - | 필드 라벨 |
| `required` | `boolean` | `false` | 필수 표시 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `helperText` | `string` | - | 도움말 텍스트 |
| `id` | `string` | - | 연결할 입력 요소 id |
| `disabled` | `boolean` | `false` | 비활성화 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |

### SearchBarProps

```ts
interface SearchBarProps {
  value: string;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
  autofocus?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | **필수** | 검색어 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `loading` | `boolean` | `false` | 검색 중 상태 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `autofocus` | `boolean` | `false` | 자동 포커스 |

## Feedback Components

### AlertProps

```ts
interface AlertProps {
  variant?: StatusVariant;
  title?: string;
  dismissible?: boolean;
  showIcon?: boolean;
  bordered?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`StatusVariant`](./theme-types.md#statusvariant) | `'info'` | 알림 유형 |
| `title` | `string` | - | 알림 제목 |
| `dismissible` | `boolean` | `false` | 닫기 버튼 표시 |
| `showIcon` | `boolean` | `true` | 아이콘 표시 |
| `bordered` | `boolean` | `false` | 테두리 표시 |

### ToastItem / ToastProps / ToastContainerProps

```ts
interface ToastItem {
  id: string;
  variant?: StatusVariant;
  title?: string;
  message: string;
  duration?: number;
  dismissible?: boolean;
  action?: {
    label: string;
    onClick: () => void;
  };
}

interface ToastProps extends ToastItem {}

interface ToastContainerProps {
  position?: ToastPosition;
  maxToasts?: number;
}
```

**ToastItem:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `id` | `string` | **필수** | 고유 식별자 |
| `variant` | [`StatusVariant`](./theme-types.md#statusvariant) | `'info'` | 토스트 유형 |
| `title` | `string` | - | 토스트 제목 |
| `message` | `string` | **필수** | 토스트 메시지 |
| `duration` | `number` | `5000` | 표시 시간 (ms) |
| `dismissible` | `boolean` | `true` | 닫기 버튼 |
| `action` | `{ label, onClick }` | - | 액션 버튼 |

**ToastContainerProps:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `position` | [`ToastPosition`](./theme-types.md#toastposition) | `'top-right'` | 표시 위치 |
| `maxToasts` | `number` | `5` | 최대 표시 수 |

### ModalProps

```ts
interface ModalProps {
  open: boolean;
  title?: string;
  size?: Exclude<Size, 'xs'>;
  showClose?: boolean;
  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | **필수** | 열림 상태 |
| `title` | `string` | - | 모달 제목 |
| `size` | `'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | 모달 크기 (`'xs'` 제외) |
| `showClose` | `boolean` | `true` | 닫기 버튼 |
| `closeOnBackdrop` | `boolean` | `true` | 배경 클릭으로 닫기 |
| `closeOnEscape` | `boolean` | `true` | ESC 키로 닫기 |

> **Vue 참고**: Vue 컴포넌트에서는 `open` 대신 `modelValue`를 사용하여 `v-model` 지원

### SpinnerProps

```ts
interface SpinnerProps {
  size?: Size;
  color?: SpinnerColor;
  label?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `size` | [`Size`](./theme-types.md#size) | `'md'` | 크기 (xs~xl 전체) |
| `color` | [`SpinnerColor`](./theme-types.md#spinnercolor) | `'primary'` | 색상 |
| `label` | `string` | - | 접근성 라벨 |

### SkeletonProps

```ts
interface SkeletonProps {
  variant?: SkeletonVariant;
  width?: string;
  height?: string;
  animation?: SkeletonAnimation;
  lines?: number;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`SkeletonVariant`](./theme-types.md#skeletonvariant) | `'text'` | 모양 |
| `width` | `string` | `'100%'` | 너비 |
| `height` | `string` | - | 높이 |
| `animation` | [`SkeletonAnimation`](./theme-types.md#skeletonanimation) | `'pulse'` | 애니메이션 |
| `lines` | `number` | `1` | 텍스트 라인 수 |

### ProgressProps

```ts
interface ProgressProps {
  value: number;
  max?: number;
  size?: FormSize;
  showLabel?: boolean;
  variant?: StatusVariant | 'default';
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `number` | **필수** | 현재 값 |
| `max` | `number` | `100` | 최대 값 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `showLabel` | `boolean` | `false` | 퍼센트 라벨 표시 |
| `variant` | [`StatusVariant`](./theme-types.md#statusvariant) `\| 'default'` | `'default'` | 색상 변형 |

## Layout Components

### CardProps

```ts
interface CardProps {
  variant?: CardVariant;
  hoverable?: boolean;
  padding?: PaddingSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`CardVariant`](./theme-types.md#cardvariant) | `'elevated'` | 카드 스타일 |
| `hoverable` | `boolean` | `false` | hover 효과 |
| `padding` | [`PaddingSize`](./theme-types.md#paddingsize) | `'md'` | 내부 여백 (none~xl) |

### ContainerProps

```ts
interface ContainerProps {
  maxWidth?: MaxWidth;
  centered?: boolean;
  padding?: Exclude<PaddingSize, 'xl'>;
  as?: ContainerElement;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `maxWidth` | [`MaxWidth`](./theme-types.md#maxwidth) | `'xl'` | 최대 너비 |
| `centered` | `boolean` | `true` | 중앙 정렬 |
| `padding` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 좌우 패딩 (`'xl'` 제외) |
| `as` | [`ContainerElement`](./theme-types.md#containerelement) | `'div'` | HTML 요소 |

### StackProps

```ts
interface StackProps {
  direction?: Orientation;
  gap?: GapSize;
  align?: Align;
  justify?: Justify;
  wrap?: boolean;
  as?: StackElement;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `direction` | [`Orientation`](./theme-types.md#orientation) | `'vertical'` | 배치 방향 |
| `gap` | [`GapSize`](./theme-types.md#gapsize) | `'md'` | 간격 크기 |
| `align` | [`Align`](./theme-types.md#align) | `'stretch'` | 교차축 정렬 |
| `justify` | [`Justify`](./theme-types.md#justify) | `'start'` | 주축 정렬 |
| `wrap` | `boolean` | `false` | 줄바꿈 허용 |
| `as` | [`StackElement`](./theme-types.md#stackelement) | `'div'` | HTML 요소 |

### DividerProps

```ts
interface DividerProps {
  orientation?: Orientation;
  variant?: DividerVariant;
  color?: DividerColor;
  label?: string;
  spacing?: Exclude<PaddingSize, 'xl'>;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `orientation` | [`Orientation`](./theme-types.md#orientation) | `'horizontal'` | 방향 |
| `variant` | [`DividerVariant`](./theme-types.md#dividervariant) | `'solid'` | 선 스타일 |
| `color` | [`DividerColor`](./theme-types.md#dividercolor) | `'default'` | 선 색상 |
| `label` | `string` | - | 구분선 내 텍스트 |
| `spacing` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 상하 여백 (`'xl'` 제외) |

## Navigation Components

### BreadcrumbProps

```ts
interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  separator?: string;
  maxItems?: number;
  size?: FormSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `items` | `BreadcrumbItem[]` | **필수** | 경로 항목 목록 |
| `separator` | `string` | `'/'` | 구분 문자 |
| `maxItems` | `number` | - | 최대 표시 항목 수 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |

### TabsProps

```ts
interface TabItem {
  label: string;
  value: string;
  disabled?: boolean;
  icon?: string;
}

interface TabsProps {
  value: string;
  items: TabItem[];
  variant?: TabVariant;
  size?: FormSize;
  fullWidth?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | **필수** | 선택된 탭 값 |
| `items` | `TabItem[]` | **필수** | 탭 항목 목록 |
| `variant` | [`TabVariant`](./theme-types.md#tabvariant) | `'default'` | 탭 스타일 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `fullWidth` | `boolean` | `false` | 전체 너비 |

### LinkProps

```ts
interface LinkProps {
  href?: string;
  target?: LinkTarget;
  variant?: LinkVariant;
  external?: boolean;
  disabled?: boolean;
  size?: FormSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `href` | `string` | - | 링크 URL |
| `target` | [`LinkTarget`](./theme-types.md#linktarget) | `'_self'` | 열기 대상 |
| `variant` | [`LinkVariant`](./theme-types.md#linkvariant) | `'default'` | 링크 스타일 |
| `external` | `boolean` | `false` | 외부 링크 아이콘 표시 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |

### DropdownProps

```ts
interface DropdownItem {
  label: string;
  value?: string | number;
  icon?: string;
  disabled?: boolean;
  divider?: boolean;
}

interface DropdownProps {
  items: DropdownItem[];
  trigger?: DropdownTrigger;
  placement?: DropdownPlacement;
  disabled?: boolean;
  closeOnSelect?: boolean;
  width?: 'auto' | 'trigger' | string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `items` | `DropdownItem[]` | **필수** | 메뉴 항목 목록 |
| `trigger` | [`DropdownTrigger`](./theme-types.md#dropdowntrigger) | `'click'` | 트리거 방식 |
| `placement` | [`DropdownPlacement`](./theme-types.md#dropdownplacement) | `'bottom'` | 위치 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `closeOnSelect` | `boolean` | `true` | 선택 시 닫기 |
| `width` | `'auto' \| 'trigger' \| string` | `'auto'` | 너비 |

### PaginationProps

```ts
interface PaginationProps {
  page: number;
  totalPages: number;
  siblingCount?: number;
  showFirstLast?: boolean;
  size?: FormSize;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `page` | `number` | **필수** | 현재 페이지 |
| `totalPages` | `number` | **필수** | 전체 페이지 수 |
| `siblingCount` | `number` | `1` | 현재 페이지 양쪽 표시 수 |
| `showFirstLast` | `boolean` | `true` | 처음/마지막 버튼 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |

## Data Display Components

### BadgeProps

```ts
interface BadgeProps {
  variant?: BadgeVariant;
  size?: Exclude<Size, 'xl'>;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`BadgeVariant`](./theme-types.md#badgevariant) | `'default'` | 배지 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'sm'` | 크기 (`'xl'` 제외) |

### TagProps

```ts
interface TagProps {
  variant?: TagVariant;
  size?: FormSize;
  removable?: boolean;
  clickable?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | [`TagVariant`](./theme-types.md#tagvariant) | `'default'` | 태그 스타일 |
| `size` | [`FormSize`](./theme-types.md#formsize) | `'md'` | 크기 |
| `removable` | `boolean` | `false` | 제거 버튼 표시 |
| `clickable` | `boolean` | `false` | 클릭 가능 |

### AvatarProps

```ts
interface AvatarProps {
  src?: string;
  alt?: string;
  name?: string;
  size?: ExtendedSize;
  status?: AvatarStatus;
  shape?: AvatarShape;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `src` | `string` | - | 이미지 URL |
| `alt` | `string` | - | 대체 텍스트 |
| `name` | `string` | - | 이니셜 표시용 이름 |
| `size` | [`ExtendedSize`](./theme-types.md#extendedsize) | `'md'` | 크기 (xs~2xl) |
| `status` | [`AvatarStatus`](./theme-types.md#avatarstatus) | - | 온라인 상태 표시 |
| `shape` | [`AvatarShape`](./theme-types.md#avatarshape) | `'circle'` | 모양 |

### TooltipProps

```ts
interface TooltipProps {
  content: string;
  placement?: DropdownPlacement;
  delay?: number;
  disabled?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `content` | `string` | **필수** | 툴팁 텍스트 |
| `placement` | [`DropdownPlacement`](./theme-types.md#dropdownplacement) | `'top'` | 위치 |
| `delay` | `number` | `200` | 표시 지연 (ms) |
| `disabled` | `boolean` | `false` | 비활성화 |

### TableProps

```ts
interface TableColumn<T = unknown> {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (value: unknown, row: T) => unknown;
}

interface TableProps<T = unknown> {
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  emptyText?: string;
  striped?: boolean;
  hoverable?: boolean;
}
```

**TableColumn\<T\>:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `key` | `string` | **필수** | 데이터 필드 키 |
| `label` | `string` | **필수** | 헤더 라벨 |
| `sortable` | `boolean` | `false` | 정렬 가능 |
| `width` | `string` | - | 열 너비 |
| `align` | `'left' \| 'center' \| 'right'` | `'left'` | 정렬 |
| `render` | `(value, row) => unknown` | - | 커스텀 렌더러 |

**TableProps\<T\>:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `columns` | `TableColumn<T>[]` | **필수** | 열 정의 |
| `data` | `T[]` | **필수** | 행 데이터 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `emptyText` | `string` | - | 데이터 없을 때 텍스트 |
| `striped` | `boolean` | `false` | 줄무늬 행 |
| `hoverable` | `boolean` | `false` | hover 효과 |

## Overlay Components

### PopoverProps

```ts
interface PopoverProps {
  open: boolean;
  placement?: DropdownPlacement;
  trigger?: DropdownTrigger;
  closeOnClickOutside?: boolean;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | **필수** | 열림 상태 |
| `placement` | [`DropdownPlacement`](./theme-types.md#dropdownplacement) | `'bottom'` | 위치 |
| `trigger` | [`DropdownTrigger`](./theme-types.md#dropdowntrigger) | `'click'` | 트리거 방식 |
| `closeOnClickOutside` | `boolean` | `true` | 외부 클릭 시 닫기 |
