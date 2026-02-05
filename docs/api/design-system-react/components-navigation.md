---
id: components-navigation-react
title: Navigation Components
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter
tags: [api, react, navigation, tabs, breadcrumb, link, dropdown, pagination]
related:
  - components-button-react
  - components-layout-react
---

# Navigation Components

네비게이션 관련 컴포넌트의 API 레퍼런스입니다.

## Tabs

탭 네비게이션 컴포넌트입니다. 3가지 스타일 variant를 지원합니다.

### Import

```tsx
import { Tabs } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | required | 현재 선택된 탭 값 |
| `items` | `TabItem[]` | required | 탭 항목 배열 |
| `variant` | `'default' \| 'pills' \| 'underline'` | `'default'` | 탭 스타일 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `fullWidth` | `boolean` | `false` | 전체 너비 사용 |
| `onChange` | `(value: string) => void` | - | 탭 변경 핸들러 |
| `className` | `string` | - | 추가 CSS 클래스 |

### TabItem

```ts
interface TabItem {
  label: string;
  value: string;
  disabled?: boolean;
  icon?: string;
}
```

### 기본 사용법

```tsx
const [activeTab, setActiveTab] = useState('overview');

const tabs = [
  { label: 'Overview', value: 'overview' },
  { label: 'Settings', value: 'settings' },
  { label: 'Members', value: 'members' },
];

<Tabs
  value={activeTab}
  items={tabs}
  onChange={setActiveTab}
/>
```

### Variants

```tsx
{/* 기본: 카드 스타일 */}
<Tabs value={tab} items={tabs} onChange={setTab} variant="default" />

{/* 필: 캡슐 형태 */}
<Tabs value={tab} items={tabs} onChange={setTab} variant="pills" />

{/* 언더라인: 하단 테두리 */}
<Tabs value={tab} items={tabs} onChange={setTab} variant="underline" />
```

### Full Width

```tsx
<Tabs
  value={tab}
  items={tabs}
  onChange={setTab}
  fullWidth
/>
```

### Disabled Tab

```tsx
const tabs = [
  { label: 'Active', value: 'active' },
  { label: 'Disabled', value: 'disabled', disabled: true },
  { label: 'Another', value: 'another' },
];

<Tabs value={tab} items={tabs} onChange={setTab} />
```

### Tab Content Pattern

```tsx
function TabPage() {
  const [tab, setTab] = useState('profile');

  return (
    <div>
      <Tabs
        value={tab}
        items={[
          { label: 'Profile', value: 'profile' },
          { label: 'Account', value: 'account' },
          { label: 'Notifications', value: 'notifications' },
        ]}
        onChange={setTab}
        variant="underline"
      />
      <div className="mt-6">
        {tab === 'profile' && <ProfilePanel />}
        {tab === 'account' && <AccountPanel />}
        {tab === 'notifications' && <NotificationsPanel />}
      </div>
    </div>
  );
}
```

---

## Breadcrumb

경로 탐색 컴포넌트입니다. 현재 페이지의 위치를 계층적으로 표시합니다.

### Import

```tsx
import { Breadcrumb } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `items` | `BreadcrumbItem[]` | required | 경로 항목 배열 |
| `separator` | `string` | `'/'` | 구분자 |
| `maxItems` | `number` | - | 최대 표시 개수 (초과 시 축약) |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `className` | `string` | - | 추가 CSS 클래스 |

### BreadcrumbItem

```ts
interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: string;
}
```

### 기본 사용법

```tsx
<Breadcrumb
  items={[
    { label: 'Home', href: '/' },
    { label: 'Products', href: '/products' },
    { label: 'Electronics', href: '/products/electronics' },
    { label: 'Laptops' }, // 마지막 항목은 href 없이 (현재 페이지)
  ]}
/>
```

### Custom Separator

```tsx
<Breadcrumb
  items={items}
  separator=">"
/>

<Breadcrumb
  items={items}
  separator="→"
/>
```

### Max Items (축약)

```tsx
{/* 경로가 길 경우 중간을 "..."으로 축약 */}
<Breadcrumb
  items={[
    { label: 'Home', href: '/' },
    { label: 'Category', href: '/cat' },
    { label: 'Sub Category', href: '/cat/sub' },
    { label: 'Product', href: '/cat/sub/product' },
    { label: 'Detail' },
  ]}
  maxItems={3}
/>
{/* 결과: Home / ... / Product / Detail */}
```

### Sizes

```tsx
<Breadcrumb items={items} size="sm" /> {/* text-xs */}
<Breadcrumb items={items} size="md" /> {/* text-sm (기본) */}
<Breadcrumb items={items} size="lg" /> {/* text-base */}
```

---

## Link

앵커 링크 컴포넌트입니다. 외부 링크 시 자동으로 아이콘을 표시합니다.

### Import

```tsx
import { Link } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `href` | `string` | - | 링크 URL |
| `target` | `'_self' \| '_blank' \| '_parent' \| '_top'` | `'_self'` | 타겟 |
| `variant` | `'default' \| 'primary' \| 'muted' \| 'underline'` | `'default'` | 스타일 |
| `external` | `boolean` | `false` | 외부 링크 여부 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `children` | `ReactNode` | - | 링크 텍스트 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<a>` HTML 속성도 지원합니다 (`href`, `target` 제외).

### 기본 사용법

```tsx
<Link href="/about">About Us</Link>
```

### Variants

```tsx
<Link href="/page" variant="default">Default link</Link>
<Link href="/page" variant="primary">Primary link</Link>
<Link href="/page" variant="muted">Muted link</Link>
<Link href="/page" variant="underline">Underline link</Link>
```

### External Link

```tsx
{/* external=true 또는 target="_blank" 시 외부 링크 아이콘 자동 표시 */}
<Link href="https://example.com" external>
  External Link
</Link>

{/* rel="noopener noreferrer" 자동 추가 */}
<Link href="https://example.com" target="_blank">
  Opens in new tab
</Link>
```

### Disabled

```tsx
<Link href="/page" disabled>
  Cannot click this link
</Link>
```

---

## Dropdown

드롭다운 메뉴 컴포넌트입니다. 클릭 또는 호버로 트리거할 수 있습니다.

### Import

```tsx
import { Dropdown } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `items` | `DropdownItem[]` | required | 메뉴 항목 배열 |
| `trigger` | `'click' \| 'hover'` | `'click'` | 트리거 방식 |
| `placement` | `DropdownPlacement` | `'bottom-start'` | 메뉴 위치 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `closeOnSelect` | `boolean` | `true` | 선택 시 닫기 |
| `width` | `'auto' \| 'trigger' \| string` | `'auto'` | 메뉴 너비 |
| `onSelect` | `(item: DropdownItem) => void` | - | 선택 핸들러 |
| `onOpen` | `() => void` | - | 열기 핸들러 |
| `onClose` | `() => void` | - | 닫기 핸들러 |
| `children` | `ReactNode` | - | 트리거 요소 |
| `className` | `string` | - | 추가 CSS 클래스 |

### DropdownPlacement

```ts
type DropdownPlacement =
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'top' | 'top-start' | 'top-end';
```

### DropdownItem

```ts
interface DropdownItem {
  label: string;
  value?: string | number;
  icon?: string;
  disabled?: boolean;
  divider?: boolean;  // true이면 구분선으로 렌더링
}
```

### 기본 사용법

```tsx
const menuItems = [
  { label: 'Edit', value: 'edit' },
  { label: 'Duplicate', value: 'duplicate' },
  { divider: true, label: '' },
  { label: 'Delete', value: 'delete' },
];

<Dropdown items={menuItems} onSelect={(item) => handleAction(item.value)}>
  <Button variant="ghost">Actions</Button>
</Dropdown>
```

### Hover Trigger

```tsx
<Dropdown items={items} trigger="hover" onSelect={handleSelect}>
  <span>Hover me</span>
</Dropdown>
```

### Placement

```tsx
<Dropdown items={items} placement="bottom-end">
  <Button>Bottom End</Button>
</Dropdown>

<Dropdown items={items} placement="top-start">
  <Button>Top Start</Button>
</Dropdown>
```

### Width Options

```tsx
{/* 트리거와 동일한 너비 */}
<Dropdown items={items} width="trigger">
  <Button fullWidth>Select Action</Button>
</Dropdown>

{/* 고정 너비 */}
<Dropdown items={items} width="200px">
  <Button>Fixed Width</Button>
</Dropdown>
```

### Disabled Items & Dividers

```tsx
const items = [
  { label: 'View', value: 'view' },
  { label: 'Edit', value: 'edit' },
  { divider: true, label: '' },
  { label: 'Archive', value: 'archive', disabled: true },
  { label: 'Delete', value: 'delete' },
];

<Dropdown items={items} onSelect={handleSelect}>
  <Button>Menu</Button>
</Dropdown>
```

---

## Pagination

페이지네이션 컴포넌트입니다. 이전/다음 버튼과 페이지 번호를 표시합니다.

### Import

```tsx
import { Pagination } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `page` | `number` | required | 현재 페이지 |
| `totalPages` | `number` | required | 전체 페이지 수 |
| `siblingCount` | `number` | `1` | 현재 페이지 양옆에 표시할 페이지 수 |
| `showFirstLast` | `boolean` | `true` | 첫/마지막 페이지 항상 표시 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `onChange` | `(page: number) => void` | - | 페이지 변경 핸들러 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
const [page, setPage] = useState(1);
const totalPages = 10;

<Pagination
  page={page}
  totalPages={totalPages}
  onChange={setPage}
/>
```

### Sibling Count

```tsx
{/* 현재 페이지 양옆에 2개씩 표시 */}
<Pagination
  page={5}
  totalPages={20}
  siblingCount={2}
  onChange={setPage}
/>
{/* 결과: < 1 ... 3 4 [5] 6 7 ... 20 > */}
```

### 리스트와 함께

```tsx
function ProductList() {
  const [page, setPage] = useState(1);
  const { data, totalPages, loading } = useProducts({ page, limit: 20 });

  return (
    <div>
      {loading ? (
        <Spinner />
      ) : (
        <div className="grid grid-cols-3 gap-4">
          {data.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
      <div className="mt-8 flex justify-center">
        <Pagination
          page={page}
          totalPages={totalPages}
          onChange={setPage}
        />
      </div>
    </div>
  );
}
```

### Sizes

```tsx
<Pagination page={1} totalPages={10} onChange={setPage} size="sm" />
<Pagination page={1} totalPages={10} onChange={setPage} size="md" />
<Pagination page={1} totalPages={10} onChange={setPage} size="lg" />
```
