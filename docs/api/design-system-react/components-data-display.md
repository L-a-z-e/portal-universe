---
id: components-data-display-react
title: Data Display & Overlay Components
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, react, data-display, overlay, badge, tag, avatar, table, tooltip, popover]
related:
  - components-button-react
  - components-layout-react
---

# Data Display & Overlay Components

데이터 표시 및 오버레이 관련 컴포넌트의 API 레퍼런스입니다.

## Badge

상태 표시용 배지 컴포넌트입니다.

### Import

```tsx
import { Badge } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'default' \| 'primary' \| 'success' \| 'warning' \| 'danger' \| 'info' \| 'outline'` | `'default'` | 배지 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `children` | `ReactNode` | - | 배지 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<span>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
<Badge>Default</Badge>
<Badge variant="primary">Primary</Badge>
<Badge variant="success">Active</Badge>
<Badge variant="warning">Pending</Badge>
<Badge variant="danger">Error</Badge>
<Badge variant="info">Info</Badge>
<Badge variant="outline">Outline</Badge>
```

### Sizes

```tsx
<Badge size="xs">XS</Badge>
<Badge size="sm">SM</Badge>
<Badge size="md">MD</Badge>
<Badge size="lg">LG</Badge>
```

### 실전 예시

```tsx
function UserStatus({ status }: { status: string }) {
  const variants: Record<string, BadgeVariant> = {
    active: 'success',
    inactive: 'default',
    suspended: 'danger',
    pending: 'warning',
  };

  return <Badge variant={variants[status]}>{status}</Badge>;
}
```

---

## Tag

태그/칩 컴포넌트입니다. 삭제 가능하고 클릭 가능합니다.

### Import

```tsx
import { Tag } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'default' \| 'primary' \| 'success' \| 'error' \| 'warning' \| 'info'` | `'default'` | 태그 스타일 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `removable` | `boolean` | `false` | 삭제 버튼 표시 |
| `clickable` | `boolean` | `false` | 클릭 가능 |
| `onClick` | `(e: MouseEvent) => void` | - | 클릭 핸들러 (clickable 시) |
| `onRemove` | `() => void` | - | 삭제 핸들러 (removable 시) |
| `children` | `ReactNode` | - | 태그 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<span>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
<Tag>Default Tag</Tag>
<Tag variant="primary">Primary</Tag>
<Tag variant="success">Success</Tag>
```

### Removable

```tsx
function TagList() {
  const [tags, setTags] = useState(['React', 'TypeScript', 'Tailwind']);

  return (
    <div className="flex gap-2">
      {tags.map((tag) => (
        <Tag
          key={tag}
          removable
          onRemove={() => setTags(tags.filter((t) => t !== tag))}
        >
          {tag}
        </Tag>
      ))}
    </div>
  );
}
```

### Clickable

```tsx
<Tag clickable onClick={() => navigate('/tags/react')}>
  React
</Tag>
```

### Sizes

```tsx
<Tag size="sm">Small</Tag>
<Tag size="md">Medium</Tag>
<Tag size="lg">Large</Tag>
```

---

## Avatar

사용자 아바타 컴포넌트입니다. 이미지, 이니셜, 상태 표시를 지원합니다.

### Import

```tsx
import { Avatar } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `src` | `string` | - | 이미지 URL |
| `alt` | `string` | - | 대체 텍스트 |
| `name` | `string` | - | 사용자 이름 (이니셜 생성용) |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl'` | `'md'` | 크기 |
| `status` | `'online' \| 'offline' \| 'busy' \| 'away'` | - | 상태 표시 |
| `shape` | `'circle' \| 'square'` | `'circle'` | 모양 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<img>` HTML 속성도 지원합니다 (`src`, `alt` 제외).

### Size Scale

| Size | Dimension |
|------|-----------|
| `xs` | 24px |
| `sm` | 32px |
| `md` | 40px |
| `lg` | 48px |
| `xl` | 64px |
| `2xl` | 80px |

### 기본 사용법

```tsx
{/* 이미지 아바타 */}
<Avatar src="/avatars/user.jpg" alt="John Doe" />

{/* 이니셜 아바타 (이미지 없을 때) */}
<Avatar name="John Doe" />
{/* "JD"로 표시 */}

{/* 이미지 로드 실패 시 자동으로 이니셜 fallback */}
<Avatar src="/invalid.jpg" name="Jane Smith" />
```

### Status

```tsx
<Avatar src="/avatar.jpg" status="online" />
<Avatar src="/avatar.jpg" status="offline" />
<Avatar src="/avatar.jpg" status="busy" />
<Avatar src="/avatar.jpg" status="away" />
```

### Shapes

```tsx
<Avatar name="John" shape="circle" />  {/* 원형 (기본) */}
<Avatar name="John" shape="square" />  {/* 둥근 사각형 */}
```

### Sizes

```tsx
<Avatar name="J" size="xs" />
<Avatar name="JD" size="sm" />
<Avatar name="JD" size="md" />
<Avatar name="JD" size="lg" />
<Avatar name="JD" size="xl" />
<Avatar name="JD" size="2xl" />
```

### User Profile Pattern

```tsx
function UserProfile({ user }: { user: User }) {
  return (
    <div className="flex items-center gap-3">
      <Avatar
        src={user.avatarUrl}
        name={user.name}
        size="lg"
        status={user.isOnline ? 'online' : 'offline'}
      />
      <div>
        <p className="font-semibold">{user.name}</p>
        <p className="text-sm text-text-muted">{user.email}</p>
      </div>
    </div>
  );
}
```

---

## Table

데이터 테이블 컴포넌트입니다. Generic 타입을 지원하며, 로딩/빈 상태를 내장합니다.

### Import

```tsx
import { Table } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `columns` | `TableColumn<T>[]` | required | 컬럼 정의 배열 |
| `data` | `T[]` | required | 데이터 배열 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `emptyText` | `string` | `'No data available'` | 빈 데이터 메시지 |
| `striped` | `boolean` | `false` | 줄무늬 배경 |
| `hoverable` | `boolean` | `true` | 행 호버 효과 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<table>` HTML 속성도 지원합니다.

### TableColumn

```ts
interface TableColumn<T = unknown> {
  key: string;             // 데이터 객체의 키
  label: string;           // 컬럼 헤더 텍스트
  sortable?: boolean;      // 정렬 가능 여부
  width?: string;          // 컬럼 너비 (CSS 값)
  align?: 'left' | 'center' | 'right';  // 텍스트 정렬
  render?: (value: unknown, row: T) => unknown;  // 커스텀 렌더러
}
```

### 기본 사용법

```tsx
interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

const columns: TableColumn<User>[] = [
  { key: 'name', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Role', align: 'center' },
];

const users: User[] = [
  { id: 1, name: 'John', email: 'john@example.com', role: 'Admin' },
  { id: 2, name: 'Jane', email: 'jane@example.com', role: 'User' },
];

<Table columns={columns} data={users} />
```

### Custom Render

```tsx
const columns: TableColumn<User>[] = [
  { key: 'name', label: 'Name' },
  {
    key: 'role',
    label: 'Role',
    render: (value) => (
      <Badge variant={value === 'Admin' ? 'primary' : 'default'}>
        {value as string}
      </Badge>
    ),
  },
  {
    key: 'id',
    label: 'Actions',
    align: 'right',
    render: (_, row) => (
      <Button size="xs" variant="ghost" onClick={() => editUser(row)}>
        Edit
      </Button>
    ),
  },
];
```

### Loading & Empty States

```tsx
{/* 로딩 중: Spinner 표시 */}
<Table columns={columns} data={[]} loading />

{/* 빈 데이터: 커스텀 메시지 */}
<Table columns={columns} data={[]} emptyText="검색 결과가 없습니다." />
```

### Striped & Hoverable

```tsx
<Table
  columns={columns}
  data={data}
  striped         {/* 홀수/짝수 행 배경색 다르게 */}
  hoverable={false} {/* 호버 효과 비활성화 */}
/>
```

---

## Tooltip

마우스 호버 시 짧은 정보를 표시하는 툴팁 컴포넌트입니다.

### Import

```tsx
import { Tooltip } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `content` | `string` | required | 툴팁 내용 |
| `placement` | `DropdownPlacement` | `'top'` | 표시 위치 |
| `delay` | `number` | `200` | 표시 지연 시간 (ms) |
| `disabled` | `boolean` | `false` | 비활성화 |
| `children` | `ReactNode` | required | 트리거 요소 |
| `className` | `string` | - | 추가 CSS 클래스 |

### Placement

```ts
type DropdownPlacement =
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'top' | 'top-start' | 'top-end';
```

### 기본 사용법

```tsx
<Tooltip content="Edit this item">
  <Button variant="ghost">
    <PencilIcon className="w-4 h-4" />
  </Button>
</Tooltip>
```

### Placement

```tsx
<Tooltip content="Top tooltip" placement="top">
  <Button>Top</Button>
</Tooltip>

<Tooltip content="Bottom tooltip" placement="bottom">
  <Button>Bottom</Button>
</Tooltip>

<Tooltip content="Bottom end" placement="bottom-end">
  <Button>Bottom End</Button>
</Tooltip>
```

### Delay

```tsx
{/* 즉시 표시 */}
<Tooltip content="No delay" delay={0}>
  <span>Hover me</span>
</Tooltip>

{/* 느리게 표시 */}
<Tooltip content="Slow tooltip" delay={500}>
  <span>Hover me</span>
</Tooltip>
```

### Disabled

```tsx
<Tooltip content="This won't show" disabled>
  <Button>No tooltip</Button>
</Tooltip>
```

---

## Popover

클릭 또는 호버로 Rich Content를 표시하는 팝오버 컴포넌트입니다. Controlled/Uncontrolled 모드를 지원합니다.

### Import

```tsx
import { Popover } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | - | 열림 상태 (controlled) |
| `onOpenChange` | `(open: boolean) => void` | - | 상태 변경 핸들러 |
| `placement` | `DropdownPlacement` | `'bottom'` | 표시 위치 |
| `trigger` | `'click' \| 'hover'` | `'click'` | 트리거 방식 |
| `closeOnClickOutside` | `boolean` | `true` | 외부 클릭 시 닫기 |
| `children` | `ReactNode` | required | 트리거 요소 |
| `content` | `ReactNode` | required | 팝오버 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

### Uncontrolled (기본)

```tsx
<Popover
  content={
    <div className="w-64">
      <h4 className="font-semibold mb-2">Settings</h4>
      <p className="text-sm text-text-muted">
        Manage your account settings and preferences.
      </p>
    </div>
  }
>
  <Button variant="ghost">Settings</Button>
</Popover>
```

### Controlled

```tsx
function ControlledPopover() {
  const [open, setOpen] = useState(false);

  return (
    <Popover
      open={open}
      onOpenChange={setOpen}
      content={
        <div>
          <p>Controlled popover content</p>
          <Button size="sm" onClick={() => setOpen(false)}>
            Close
          </Button>
        </div>
      }
    >
      <Button>Toggle</Button>
    </Popover>
  );
}
```

### Hover Trigger

```tsx
<Popover
  trigger="hover"
  placement="top"
  content={
    <div className="flex items-center gap-3">
      <Avatar name="John Doe" size="lg" />
      <div>
        <p className="font-semibold">John Doe</p>
        <p className="text-sm text-text-muted">Software Engineer</p>
      </div>
    </div>
  }
>
  <Link href="/users/john">John Doe</Link>
</Popover>
```

### Placement

```tsx
<Popover placement="top" content={<div>Top popover</div>}>
  <Button>Top</Button>
</Popover>

<Popover placement="bottom-end" content={<div>Bottom end</div>}>
  <Button>Bottom End</Button>
</Popover>
```
