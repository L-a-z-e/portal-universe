# E-commerce Admin 상품 관리 UI/UX 설계

## 목차
1. [개요](#개요)
2. [레이아웃 구조](#레이아웃-구조)
3. [페이지별 와이어프레임](#페이지별-와이어프레임)
4. [컴포넌트 상세 스펙](#컴포넌트-상세-스펙)
5. [TailwindCSS 스타일 가이드](#tailwindcss-스타일-가이드)
6. [반응형 브레이크포인트](#반응형-브레이크포인트)
7. [인터랙션 정의](#인터랙션-정의)
8. [상태별 UI](#상태별-ui)

---

## 개요

### 목적
관리자가 상품을 등록, 수정, 삭제, 조회할 수 있는 Admin 전용 상품 관리 인터페이스

### 디자인 원칙
- **일관성**: 기존 디자인 시스템 토큰 준수 (`data-service="shopping"`)
- **효율성**: 빠른 작업 처리를 위한 테이블 기반 레이아웃
- **명확성**: 액션 버튼과 상태를 명확하게 구분
- **접근성**: WCAG 2.1 AA 레벨 준수

### 기존 디자인 토큰 활용
```css
/* Shopping Service Theme */
[data-service="shopping"] {
  --semantic-brand-primary: #FD7E14 (Orange)
  --semantic-brand-primaryHover: #F76707
  --semantic-brand-secondary: #FFA94D
  --semantic-text-heading: #212529
  --semantic-text-body: #212529
  --semantic-text-meta: #868E96
  --semantic-bg-page: #ffffff
  --semantic-bg-card: #ffffff
  --semantic-bg-hover: #FFF4E6
  --semantic-border-default: #E9ECEF
}
```

---

## 레이아웃 구조

### Admin 전체 레이아웃 (AdminLayout)

```
┌─────────────────────────────────────────────────────────────┐
│  Header (Fixed Top)                                         │
│  [Logo] [Welcome Admin] [Logout]                            │
├──────────┬──────────────────────────────────────────────────┤
│          │                                                  │
│ Sidebar  │  Main Content Area                               │
│ (Fixed)  │  <Outlet />                                      │
│          │                                                  │
│ Dashboard│  - Product List                                  │
│ Products │  - Product Form                                  │
│ Orders   │  - etc.                                          │
│ Users    │                                                  │
│ Settings │                                                  │
│          │                                                  │
│          │                                                  │
└──────────┴──────────────────────────────────────────────────┘
```

#### Header 컴포넌트
```tsx
<header className="
  fixed top-0 left-0 right-0 z-50 h-16
  bg-bg-card border-b border-border-default
  flex items-center justify-between px-6
  shadow-sm
">
  {/* Logo */}
  <div className="flex items-center gap-3">
    <div className="w-8 h-8 bg-brand-primary rounded-lg" />
    <h1 className="text-xl font-bold text-text-heading">
      Portal Admin
    </h1>
  </div>

  {/* User Menu */}
  <div className="flex items-center gap-4">
    <span className="text-sm text-text-meta">
      Welcome, Admin
    </span>
    <button className="
      px-4 py-2 text-sm font-medium
      text-status-error hover:text-status-error/80
      transition-colors
    ">
      Logout
    </button>
  </div>
</header>
```

#### Sidebar 컴포넌트
```tsx
<aside className="
  fixed left-0 top-16 bottom-0 w-64
  bg-bg-card border-r border-border-default
  overflow-y-auto
  hidden lg:block
">
  <nav className="p-4 space-y-2">
    <NavItem icon="dashboard" label="Dashboard" to="/admin" />
    <NavItem icon="products" label="Products" to="/admin/products" active />
    <NavItem icon="orders" label="Orders" to="/admin/orders" />
    <NavItem icon="users" label="Users" to="/admin/users" />
    <NavItem icon="settings" label="Settings" to="/admin/settings" />
  </nav>
</aside>

{/* NavItem 스타일 */}
<Link className="
  flex items-center gap-3 px-4 py-3 rounded-lg
  text-text-body hover:bg-bg-hover
  transition-colors

  /* Active State */
  data-[active=true]:bg-brand-primary/10
  data-[active=true]:text-brand-primary
  data-[active=true]:font-medium
">
  {icon}
  {label}
</Link>
```

#### Main Content
```tsx
<main className="
  lg:ml-64 mt-16 min-h-screen
  p-6 lg:p-8
  bg-bg-page
">
  <Outlet />
</main>
```

### 반응형 레이아웃
**Mobile (< 1024px)**
- Sidebar 숨김
- 햄버거 메뉴 버튼 표시 (Header 왼쪽)
- Sidebar를 Overlay 형태로 표시 (열림/닫힘)

**Desktop (>= 1024px)**
- Sidebar 고정 표시
- Main Content 왼쪽 여백 확보 (`ml-64`)

---

## 페이지별 와이어프레임

### 1. 상품 목록 페이지 (AdminProductListPage)

```
┌──────────────────────────────────────────────────────────────┐
│ Header: "Products" [+ New Product Button]                   │
├──────────────────────────────────────────────────────────────┤
│ Search & Filter Bar                                          │
│ [Search Input] [Category Filter] [Status Filter] [Search Btn]│
├──────────────────────────────────────────────────────────────┤
│ Data Table                                                   │
│ ┌────┬────────┬─────────────┬─────────┬──────┬────────┬────┐│
│ │ ID │ Image  │ Product Name│  Price  │ Stock│ Status │Act.││
│ ├────┼────────┼─────────────┼─────────┼──────┼────────┼────┤│
│ │ 1  │ [IMG]  │ Product A   │ $100.00 │  50  │ Active │ ED ││
│ │ 2  │ [IMG]  │ Product B   │  $50.00 │   5  │ Active │ ED ││
│ │ 3  │ [IMG]  │ Product C   │  $75.00 │   0  │ OOS    │ ED ││
│ └────┴────────┴─────────────┴─────────┴──────┴────────┴────┘│
├──────────────────────────────────────────────────────────────┤
│ Pagination: [< Prev] [1] [2] [3] [Next >]                   │
└──────────────────────────────────────────────────────────────┘

Legend:
- ED: Edit (연필 아이콘), Delete (휴지통 아이콘) 버튼
- OOS: Out of Stock
```

#### 컴포넌트 구조
```tsx
<AdminProductListPage>
  <PageHeader title="Products">
    <Button variant="primary" icon="plus">New Product</Button>
  </PageHeader>

  <SearchBar>
    <Input placeholder="Search products..." />
    <Select placeholder="Category" />
    <Select placeholder="Status" />
    <Button>Search</Button>
  </SearchBar>

  <DataTable>
    <TableHeader />
    <TableBody>
      {products.map(product => (
        <TableRow key={product.id} onClick={handleRowClick}>
          <TableCell>{product.id}</TableCell>
          <TableCell><ProductImage src={product.imageUrl} /></TableCell>
          <TableCell>{product.name}</TableCell>
          <TableCell>{formatPrice(product.price)}</TableCell>
          <TableCell>
            <StockBadge quantity={product.stock} />
          </TableCell>
          <TableCell>
            <StatusBadge status={product.status} />
          </TableCell>
          <TableCell>
            <ActionButtons>
              <IconButton icon="edit" onClick={handleEdit} />
              <IconButton icon="delete" onClick={handleDelete} />
            </ActionButtons>
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
  </DataTable>

  <Pagination />
</AdminProductListPage>
```

### 2. 상품 등록/수정 폼 (AdminProductFormPage)

```
┌──────────────────────────────────────────────────────────────┐
│ Header: "Edit Product" / "New Product"          [Back Button]│
├──────────────────────────────────────────────────────────────┤
│ Form Card                                                    │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Product Name *                                           │ │
│ │ [Input Field                                            ]│ │
│ │                                                          │ │
│ │ Description                                              │ │
│ │ [Textarea Field                                         ]│ │
│ │ [                                                       ]│ │
│ │ [                                                       ]│ │
│ │                                                          │ │
│ │ Price *              Category *          Stock *         │ │
│ │ [$ Input    ]        [Select   v]        [Number ]      │ │
│ │                                                          │ │
│ │ Image URL                                                │ │
│ │ [Input Field                                            ]│ │
│ │ [Image Preview]                                          │ │
│ │                                                          │ │
│ │ Status                                                   │ │
│ │ [o] Active  [ ] Inactive                                 │ │
│ │                                                          │ │
│ │ [Cancel Button] [Save Product Button]                   │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

#### 컴포넌트 구조
```tsx
<AdminProductFormPage>
  <PageHeader title={isEdit ? "Edit Product" : "New Product"}>
    <Button variant="ghost" onClick={goBack}>Back</Button>
  </PageHeader>

  <FormCard>
    <form onSubmit={handleSubmit}>
      <FormField label="Product Name" required error={errors.name}>
        <Input name="name" value={form.name} onChange={handleChange} />
      </FormField>

      <FormField label="Description">
        <Textarea
          name="description"
          rows={5}
          value={form.description}
          onChange={handleChange}
        />
      </FormField>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <FormField label="Price" required error={errors.price}>
          <Input
            type="number"
            name="price"
            prefix="$"
            value={form.price}
            onChange={handleChange}
          />
        </FormField>

        <FormField label="Category" required error={errors.category}>
          <Select name="category" value={form.category} onChange={handleChange}>
            <option>Electronics</option>
            <option>Clothing</option>
            <option>Books</option>
          </Select>
        </FormField>

        <FormField label="Stock" required error={errors.stock}>
          <Input
            type="number"
            name="stock"
            min="0"
            value={form.stock}
            onChange={handleChange}
          />
        </FormField>
      </div>

      <FormField label="Image URL">
        <Input name="imageUrl" value={form.imageUrl} onChange={handleChange} />
        {form.imageUrl && <ImagePreview src={form.imageUrl} />}
      </FormField>

      <FormField label="Status">
        <RadioGroup name="status" value={form.status} onChange={handleChange}>
          <Radio value="ACTIVE" label="Active" />
          <Radio value="INACTIVE" label="Inactive" />
        </RadioGroup>
      </FormField>

      <FormActions>
        <Button variant="ghost" type="button" onClick={handleCancel}>
          Cancel
        </Button>
        <Button variant="primary" type="submit" loading={loading}>
          {isEdit ? "Update Product" : "Create Product"}
        </Button>
      </FormActions>
    </form>
  </FormCard>
</AdminProductFormPage>
```

---

## 컴포넌트 상세 스펙

### 1. DataTable 컴포넌트

#### Props
```typescript
interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  loading?: boolean
  onRowClick?: (row: T) => void
  emptyMessage?: string
}

interface Column<T> {
  key: string
  label: string
  width?: string
  sortable?: boolean
  render?: (value: any, row: T) => React.ReactNode
}
```

#### 스타일
```tsx
<div className="
  bg-bg-card border border-border-default rounded-lg
  overflow-hidden shadow-sm
">
  <div className="overflow-x-auto">
    <table className="w-full">
      <thead className="
        bg-bg-subtle border-b border-border-default
      ">
        <tr>
          <th className="
            px-6 py-4 text-left text-xs font-medium
            text-text-meta uppercase tracking-wider
          ">
            Column Name
          </th>
        </tr>
      </thead>
      <tbody className="divide-y divide-border-default">
        <tr className="
          hover:bg-bg-hover transition-colors cursor-pointer
        ">
          <td className="px-6 py-4 text-sm text-text-body">
            Cell Content
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

#### 정렬 기능
```tsx
{/* Sortable Column Header */}
<th
  className="cursor-pointer hover:bg-bg-hover"
  onClick={() => handleSort('name')}
>
  <div className="flex items-center gap-2">
    <span>Product Name</span>
    {sortColumn === 'name' && (
      <svg className="w-4 h-4">
        {sortDirection === 'asc' ? '↑' : '↓'}
      </svg>
    )}
  </div>
</th>
```

### 2. FormField 컴포넌트

#### Props
```typescript
interface FormFieldProps {
  label: string
  required?: boolean
  error?: string
  hint?: string
  children: React.ReactNode
}
```

#### 스타일
```tsx
<div className="space-y-2">
  {/* Label */}
  <label className="
    block text-sm font-medium text-text-heading
  ">
    {label}
    {required && (
      <span className="text-status-error ml-1">*</span>
    )}
  </label>

  {/* Input */}
  {children}

  {/* Error Message */}
  {error && (
    <p className="text-sm text-status-error flex items-center gap-1">
      <svg className="w-4 h-4">!</svg>
      {error}
    </p>
  )}

  {/* Hint */}
  {hint && !error && (
    <p className="text-xs text-text-meta">
      {hint}
    </p>
  )}
</div>
```

### 3. Input 컴포넌트

#### Props
```typescript
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean
  prefix?: string
  suffix?: string
}
```

#### 스타일
```tsx
<div className="relative">
  {prefix && (
    <div className="
      absolute left-3 top-1/2 -translate-y-1/2
      text-text-meta pointer-events-none
    ">
      {prefix}
    </div>
  )}

  <input className="
    w-full px-4 py-3
    border border-border-default rounded-lg
    bg-bg-card text-text-body
    placeholder:text-text-placeholder

    focus:outline-none focus:ring-2
    focus:ring-brand-primary/20 focus:border-brand-primary

    disabled:bg-bg-muted disabled:text-text-muted
    disabled:cursor-not-allowed

    data-[error=true]:border-status-error
    data-[error=true]:focus:ring-status-error/20
  " />

  {suffix && (
    <div className="
      absolute right-3 top-1/2 -translate-y-1/2
      text-text-meta
    ">
      {suffix}
    </div>
  )}
</div>
```

### 4. Button 컴포넌트

#### Variants
```tsx
// Primary (CTA)
<button className="
  px-6 py-3 rounded-lg font-medium
  bg-brand-primary text-white
  hover:bg-brand-primaryHover
  active:scale-[0.98]
  disabled:opacity-50 disabled:cursor-not-allowed
  transition-all duration-200
  shadow-sm hover:shadow-md
">
  Primary Button
</button>

// Secondary
<button className="
  px-6 py-3 rounded-lg font-medium
  border-2 border-brand-primary
  bg-transparent text-brand-primary
  hover:bg-brand-primary/5
  active:scale-[0.98]
  disabled:opacity-50 disabled:cursor-not-allowed
  transition-all duration-200
">
  Secondary Button
</button>

// Ghost
<button className="
  px-6 py-3 rounded-lg font-medium
  bg-transparent text-text-body
  hover:bg-bg-hover
  active:scale-[0.98]
  disabled:opacity-50 disabled:cursor-not-allowed
  transition-all duration-200
">
  Ghost Button
</button>

// Danger
<button className="
  px-6 py-3 rounded-lg font-medium
  bg-status-error text-white
  hover:bg-status-error/90
  active:scale-[0.98]
  disabled:opacity-50 disabled:cursor-not-allowed
  transition-all duration-200
  shadow-sm hover:shadow-md
">
  Delete Button
</button>
```

#### Loading State
```tsx
<button disabled className="
  px-6 py-3 rounded-lg font-medium
  bg-brand-primary text-white
  cursor-wait
">
  <div className="flex items-center gap-2">
    <svg className="w-5 h-5 animate-spin" viewBox="0 0 24 24">
      <circle
        className="opacity-25"
        cx="12" cy="12" r="10"
        stroke="currentColor"
        strokeWidth="4"
        fill="none"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
    <span>Loading...</span>
  </div>
</button>
```

### 5. Badge 컴포넌트

#### StockBadge
```tsx
<span className={`
  inline-flex items-center px-2.5 py-1 rounded-full
  text-xs font-medium
  ${quantity > 10
    ? 'bg-status-success-bg text-status-success'
    : quantity > 0
      ? 'bg-status-warning-bg text-status-warning'
      : 'bg-status-error-bg text-status-error'
  }
`}>
  {quantity > 0 ? `${quantity} in stock` : 'Out of stock'}
</span>
```

#### StatusBadge
```tsx
<span className={`
  inline-flex items-center px-2.5 py-1 rounded-full
  text-xs font-medium
  ${status === 'ACTIVE'
    ? 'bg-status-success-bg text-status-success'
    : 'bg-bg-muted text-text-muted'
  }
`}>
  <span className="w-1.5 h-1.5 rounded-full mr-1.5 bg-current" />
  {status === 'ACTIVE' ? 'Active' : 'Inactive'}
</span>
```

### 6. ConfirmModal 컴포넌트

#### Props
```typescript
interface ConfirmModalProps {
  open: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: 'info' | 'danger'
  onConfirm: () => void
  onCancel: () => void
}
```

#### 스타일
```tsx
{/* Backdrop */}
<div className="
  fixed inset-0 z-50
  bg-gray-900/50 backdrop-blur-sm
  flex items-center justify-center p-4
  animate-in fade-in duration-200
">
  {/* Modal */}
  <div className="
    bg-bg-card rounded-lg shadow-xl
    max-w-md w-full p-6
    animate-in zoom-in-95 duration-200
  ">
    {/* Icon */}
    <div className={`
      w-12 h-12 rounded-full mx-auto mb-4
      flex items-center justify-center
      ${variant === 'danger'
        ? 'bg-status-error-bg text-status-error'
        : 'bg-status-info-bg text-status-info'
      }
    `}>
      <svg className="w-6 h-6">!</svg>
    </div>

    {/* Title */}
    <h3 className="text-lg font-bold text-text-heading text-center mb-2">
      {title}
    </h3>

    {/* Message */}
    <p className="text-sm text-text-meta text-center mb-6">
      {message}
    </p>

    {/* Actions */}
    <div className="flex items-center gap-3">
      <button
        onClick={onCancel}
        className="flex-1 px-4 py-3 rounded-lg font-medium
          bg-bg-subtle text-text-body
          hover:bg-bg-hover transition-colors
        "
      >
        {cancelText || 'Cancel'}
      </button>
      <button
        onClick={onConfirm}
        className={`flex-1 px-4 py-3 rounded-lg font-medium
          text-white transition-colors
          ${variant === 'danger'
            ? 'bg-status-error hover:bg-status-error/90'
            : 'bg-brand-primary hover:bg-brand-primaryHover'
          }
        `}
      >
        {confirmText || 'Confirm'}
      </button>
    </div>
  </div>
</div>
```

### 7. Toast 알림 컴포넌트

#### 위치
```tsx
{/* Toast Container (Fixed) */}
<div className="
  fixed bottom-6 right-6 z-50
  flex flex-col gap-3
  max-w-sm w-full
">
  {toasts.map(toast => (
    <ToastItem key={toast.id} {...toast} />
  ))}
</div>
```

#### ToastItem
```tsx
<div className={`
  bg-bg-card border rounded-lg p-4 shadow-lg
  flex items-start gap-3
  animate-in slide-in-from-right duration-300
  ${variant === 'success' && 'border-status-success'}
  ${variant === 'error' && 'border-status-error'}
  ${variant === 'info' && 'border-status-info'}
`}>
  {/* Icon */}
  <div className={`
    w-5 h-5 flex-shrink-0
    ${variant === 'success' && 'text-status-success'}
    ${variant === 'error' && 'text-status-error'}
    ${variant === 'info' && 'text-status-info'}
  `}>
    {icon}
  </div>

  {/* Content */}
  <div className="flex-1 pt-0.5">
    <p className="text-sm font-medium text-text-heading">
      {title}
    </p>
    {message && (
      <p className="text-sm text-text-meta mt-1">
        {message}
      </p>
    )}
  </div>

  {/* Close Button */}
  <button
    onClick={onClose}
    className="text-text-meta hover:text-text-body transition-colors"
  >
    <svg className="w-5 h-5">×</svg>
  </button>
</div>
```

### 8. Pagination 컴포넌트

#### Props
```typescript
interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}
```

#### 스타일
```tsx
<div className="flex items-center justify-center gap-2 mt-6">
  {/* Previous Button */}
  <button
    onClick={() => onPageChange(currentPage - 1)}
    disabled={currentPage === 0}
    className="
      px-4 py-2 rounded-lg font-medium
      bg-bg-subtle text-text-body
      hover:bg-bg-hover
      disabled:opacity-50 disabled:cursor-not-allowed
      transition-colors
    "
  >
    Previous
  </button>

  {/* Page Numbers */}
  <div className="flex items-center gap-1">
    {pageNumbers.map((page) => (
      <button
        key={page}
        onClick={() => onPageChange(page)}
        className={`
          w-10 h-10 rounded-lg font-medium
          transition-all
          ${page === currentPage
            ? 'bg-brand-primary text-white shadow-sm'
            : 'bg-bg-subtle text-text-body hover:bg-bg-hover'
          }
        `}
      >
        {page + 1}
      </button>
    ))}
  </div>

  {/* Next Button */}
  <button
    onClick={() => onPageChange(currentPage + 1)}
    disabled={currentPage === totalPages - 1}
    className="
      px-4 py-2 rounded-lg font-medium
      bg-bg-subtle text-text-body
      hover:bg-bg-hover
      disabled:opacity-50 disabled:cursor-not-allowed
      transition-colors
    "
  >
    Next
  </button>
</div>
```

---

## TailwindCSS 스타일 가이드

### Color System (Shopping Theme)

#### Primary Actions
```css
bg-brand-primary         /* #FD7E14 - Orange */
text-brand-primary
border-brand-primary
hover:bg-brand-primaryHover  /* #F76707 */
```

#### Text Hierarchy
```css
text-text-heading        /* #212529 - Dark Gray */
text-text-body          /* #212529 */
text-text-meta          /* #868E96 - Medium Gray */
text-text-muted         /* #ADB5BD - Light Gray */
```

#### Backgrounds
```css
bg-bg-page              /* #ffffff - White */
bg-bg-card              /* #ffffff */
bg-bg-hover             /* #FFF4E6 - Light Orange */
bg-bg-muted             /* #FFF4E6 */
```

#### Borders
```css
border-border-default   /* #E9ECEF - Light Gray */
border-border-hover     /* #FFC078 - Light Orange */
border-border-focus     /* #FD7E14 - Orange */
```

#### Status Colors
```css
/* Success */
bg-status-success-bg text-status-success
/* #E6FCF5 background, #12B886 text */

/* Error */
bg-status-error-bg text-status-error
/* #FFE3E3 background, #F03E3E text */

/* Warning */
bg-status-warning-bg text-status-warning
/* #FFF9DB background, #FAB005 text */

/* Info */
bg-status-info-bg text-status-info
/* #D0EBFF background, #228BE6 text */
```

### Spacing Scale
```css
gap-2        /* 0.5rem - 8px */
gap-3        /* 0.75rem - 12px */
gap-4        /* 1rem - 16px */
gap-6        /* 1.5rem - 24px */

p-4          /* 1rem - 16px */
p-6          /* 1.5rem - 24px */
p-8          /* 2rem - 32px */

space-y-2    /* vertical spacing */
space-y-4
space-y-6
```

### Typography
```css
/* Headings */
text-2xl font-bold text-text-heading     /* Page Title */
text-xl font-bold text-text-heading      /* Section Title */
text-lg font-medium text-text-heading    /* Subsection Title */

/* Body */
text-base text-text-body                 /* Normal Text */
text-sm text-text-meta                   /* Small Text */
text-xs text-text-meta                   /* Extra Small */

/* Interactive */
font-medium                              /* Buttons, Labels */
font-semibold                            /* Emphasis */
```

### Border Radius
```css
rounded-lg      /* 0.5rem - 8px - Default */
rounded-md      /* 0.375rem - 6px */
rounded-full    /* Pills, Badges */
```

### Shadows
```css
shadow-sm       /* Subtle elevation */
shadow-md       /* Cards */
shadow-lg       /* Modals, Dropdowns */
shadow-xl       /* Heavy emphasis */
```

### Common Patterns

#### Card
```css
bg-bg-card border border-border-default rounded-lg shadow-sm p-6
```

#### Hover Effect
```css
hover:bg-bg-hover hover:border-brand-primary/30 transition-colors
```

#### Focus Ring
```css
focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary
```

#### Disabled State
```css
disabled:opacity-50 disabled:cursor-not-allowed
```

---

## 반응형 브레이크포인트

### TailwindCSS Breakpoints
```css
/* Mobile First Approach */
sm: 640px    /* Small devices (landscape phones) */
md: 768px    /* Medium devices (tablets) */
lg: 1024px   /* Large devices (desktops) */
xl: 1280px   /* Extra large devices (large desktops) */
2xl: 1536px  /* 2X large devices */
```

### Layout Breakpoints

#### Sidebar
```css
/* Mobile (< 1024px) */
hidden

/* Desktop (>= 1024px) */
lg:block
```

#### Main Content
```css
/* Mobile */
mt-16 p-6

/* Desktop */
lg:ml-64 lg:p-8
```

#### Data Table
```css
/* Mobile */
overflow-x-auto   /* Horizontal scroll */

/* Desktop */
table            /* Normal table layout */
```

#### Form Grid
```css
/* Mobile */
grid-cols-1

/* Tablet */
md:grid-cols-2

/* Desktop */
lg:grid-cols-3
```

### Component Responsive Patterns

#### Page Header
```tsx
<div className="
  flex flex-col sm:flex-row items-start sm:items-center
  justify-between gap-4
">
  <h1 className="text-xl sm:text-2xl font-bold">Products</h1>
  <Button>New Product</Button>
</div>
```

#### Search Bar
```tsx
<div className="
  flex flex-col sm:flex-row items-stretch sm:items-center
  gap-3
">
  <Input className="flex-1" />
  <Select className="sm:w-48" />
  <Button className="w-full sm:w-auto">Search</Button>
</div>
```

#### Form Actions
```tsx
<div className="
  flex flex-col-reverse sm:flex-row items-stretch sm:items-center
  justify-between gap-3
">
  <Button className="w-full sm:w-auto">Cancel</Button>
  <Button className="w-full sm:w-auto">Save</Button>
</div>
```

---

## 인터랙션 정의

### Hover States

#### Table Row
```css
/* Initial */
bg-bg-card

/* Hover */
hover:bg-bg-hover cursor-pointer transition-colors
```

#### Button
```css
/* Initial */
bg-brand-primary scale-100

/* Hover */
hover:bg-brand-primaryHover hover:shadow-md

/* Active (Click) */
active:scale-[0.98] transition-transform duration-100
```

#### Input
```css
/* Initial */
border-border-default

/* Hover */
hover:border-border-hover

/* Focus */
focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20
```

#### Link/NavItem
```css
/* Initial */
text-text-body

/* Hover */
hover:text-brand-primary hover:bg-bg-hover
```

### Focus States

#### Keyboard Navigation
```css
focus-visible:outline-none
focus-visible:ring-2
focus-visible:ring-brand-primary
focus-visible:ring-offset-2
```

#### Form Fields
```css
focus:outline-none
focus:ring-2
focus:ring-brand-primary/20
focus:border-brand-primary
```

### Active States

#### Button Click
```css
active:scale-[0.98]
active:shadow-inner
transition-all duration-100
```

#### NavItem Active
```css
data-[active=true]:bg-brand-primary/10
data-[active=true]:text-brand-primary
data-[active=true]:font-medium
data-[active=true]:border-l-4
data-[active=true]:border-brand-primary
```

### Loading States

#### Button Loading
```tsx
{loading ? (
  <>
    <Spinner className="w-5 h-5 animate-spin" />
    <span>Loading...</span>
  </>
) : (
  'Save Product'
)}
```

#### Table Loading
```tsx
<div className="flex items-center justify-center py-20">
  <div className="flex flex-col items-center gap-4">
    <Spinner className="w-8 h-8 animate-spin" />
    <p className="text-text-meta">Loading products...</p>
  </div>
</div>
```

### Animation Classes

#### Fade In
```css
@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.animate-in.fade-in {
  animation: fade-in 200ms ease-out;
}
```

#### Slide In (Toast)
```css
@keyframes slide-in-from-right {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.animate-in.slide-in-from-right {
  animation: slide-in-from-right 300ms ease-out;
}
```

#### Zoom In (Modal)
```css
@keyframes zoom-in-95 {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.animate-in.zoom-in-95 {
  animation: zoom-in-95 200ms ease-out;
}
```

---

## 상태별 UI

### 1. Loading State

#### Full Page Loading
```tsx
<div className="flex items-center justify-center min-h-screen">
  <div className="flex flex-col items-center gap-4">
    <Spinner className="w-12 h-12" />
    <p className="text-lg text-text-meta">Loading...</p>
  </div>
</div>
```

#### Inline Loading (Table)
```tsx
<div className="flex items-center justify-center py-20">
  <div className="flex flex-col items-center gap-4">
    <Spinner className="w-8 h-8" />
    <p className="text-text-meta">Loading products...</p>
  </div>
</div>
```

#### Skeleton Loader (Optional)
```tsx
<div className="space-y-4">
  {[1, 2, 3].map(i => (
    <div key={i} className="animate-pulse">
      <div className="h-20 bg-bg-muted rounded-lg" />
    </div>
  ))}
</div>
```

### 2. Empty State

#### No Products
```tsx
<div className="
  bg-bg-card border border-border-default rounded-lg
  p-12 text-center
">
  {/* Icon */}
  <div className="w-16 h-16 mx-auto mb-4 text-text-placeholder">
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
        d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
      />
    </svg>
  </div>

  {/* Title */}
  <h3 className="text-lg font-medium text-text-heading mb-2">
    No products found
  </h3>

  {/* Description */}
  <p className="text-sm text-text-meta mb-6">
    Get started by creating a new product.
  </p>

  {/* CTA */}
  <Button variant="primary" icon="plus">
    Create Product
  </Button>
</div>
```

#### No Search Results
```tsx
<div className="
  bg-bg-card border border-border-default rounded-lg
  p-12 text-center
">
  <p className="text-lg text-text-heading mb-2">
    No results found for "{searchQuery}"
  </p>
  <p className="text-sm text-text-meta mb-4">
    Try adjusting your search or filter criteria
  </p>
  <Button variant="ghost" onClick={clearSearch}>
    Clear Search
  </Button>
</div>
```

### 3. Error State

#### API Error
```tsx
<div className="
  bg-status-error-bg border border-status-error/20 rounded-lg
  p-8 text-center
">
  {/* Icon */}
  <div className="
    w-12 h-12 mx-auto mb-4
    bg-status-error/10 rounded-full
    flex items-center justify-center
  ">
    <svg className="w-6 h-6 text-status-error">!</svg>
  </div>

  {/* Title */}
  <h3 className="text-lg font-medium text-text-heading mb-2">
    Failed to load products
  </h3>

  {/* Message */}
  <p className="text-sm text-status-error mb-6">
    {error.message || 'An unexpected error occurred'}
  </p>

  {/* Actions */}
  <Button variant="primary" onClick={retry}>
    Try Again
  </Button>
</div>
```

#### Form Validation Error
```tsx
<div className="
  bg-status-error-bg border border-status-error/20 rounded-lg
  p-4 mb-6
">
  <div className="flex items-start gap-3">
    <svg className="w-5 h-5 text-status-error flex-shrink-0 mt-0.5">
      !
    </svg>
    <div className="flex-1">
      <h4 className="text-sm font-medium text-status-error mb-1">
        Validation Error
      </h4>
      <ul className="text-sm text-status-error space-y-1">
        {errors.map((error, i) => (
          <li key={i}>• {error}</li>
        ))}
      </ul>
    </div>
  </div>
</div>
```

### 4. Success State

#### Action Success (Toast)
```tsx
// Toast appears in bottom-right
showToast({
  variant: 'success',
  title: 'Product created',
  message: 'The product has been successfully created.'
})
```

#### Inline Success Message
```tsx
<div className="
  bg-status-success-bg border border-status-success/20 rounded-lg
  p-4 mb-6
">
  <div className="flex items-center gap-3">
    <svg className="w-5 h-5 text-status-success">✓</svg>
    <p className="text-sm font-medium text-status-success">
      Product updated successfully!
    </p>
  </div>
</div>
```

### 5. Confirmation State

#### Delete Confirmation Modal
```tsx
<ConfirmModal
  open={showDeleteModal}
  variant="danger"
  title="Delete Product?"
  message="This action cannot be undone. Are you sure you want to delete this product?"
  confirmText="Delete"
  cancelText="Cancel"
  onConfirm={handleDelete}
  onCancel={() => setShowDeleteModal(false)}
/>
```

#### Unsaved Changes Warning
```tsx
<ConfirmModal
  open={showUnsavedModal}
  variant="info"
  title="Unsaved Changes"
  message="You have unsaved changes. Do you want to leave without saving?"
  confirmText="Leave"
  cancelText="Stay"
  onConfirm={handleLeave}
  onCancel={() => setShowUnsavedModal(false)}
/>
```

---

## 구현 체크리스트

### Phase 1: 레이아웃 & 네비게이션
- [ ] AdminLayout 컴포넌트 구현
- [ ] Header 컴포넌트 구현
- [ ] Sidebar 컴포넌트 구현
- [ ] 반응형 네비게이션 (햄버거 메뉴)
- [ ] 라우팅 설정 (`/admin/products`)

### Phase 2: 기본 컴포넌트
- [ ] Button 컴포넌트 (4개 variants)
- [ ] Input 컴포넌트
- [ ] Select 컴포넌트
- [ ] Textarea 컴포넌트
- [ ] FormField 컴포넌트
- [ ] Badge 컴포넌트 (Stock, Status)

### Phase 3: 상품 목록 페이지
- [ ] DataTable 컴포넌트
- [ ] SearchBar 컴포넌트
- [ ] Pagination 컴포넌트
- [ ] ProductImage 컴포넌트
- [ ] ActionButtons (Edit, Delete)
- [ ] 정렬 기능
- [ ] 필터링 기능

### Phase 4: 상품 폼 페이지
- [ ] AdminProductFormPage 구현
- [ ] Form 상태 관리 (React Hook Form 권장)
- [ ] 유효성 검사
- [ ] ImagePreview 컴포넌트
- [ ] Radio/Checkbox 컴포넌트

### Phase 5: 모달 & 알림
- [ ] ConfirmModal 컴포넌트
- [ ] Toast 시스템 구현
- [ ] Modal 열림/닫힘 애니메이션

### Phase 6: 상태 관리 & API 연동
- [ ] API 엔드포인트 연동
- [ ] Loading 상태 처리
- [ ] Error 상태 처리
- [ ] Empty 상태 처리
- [ ] Success 피드백

### Phase 7: 접근성 & 최적화
- [ ] 키보드 네비게이션
- [ ] ARIA 속성 추가
- [ ] Focus 관리
- [ ] 성능 최적화 (Memoization)

---

## 참고 자료

### 기존 코드 참고
- `frontend/shopping-frontend/src/pages/ProductListPage.tsx` - 그리드 레이아웃, 페이징
- `frontend/shopping-frontend/src/pages/CartPage.tsx` - 리스트 + 사이드바 레이아웃
- `frontend/shopping-frontend/src/pages/CheckoutPage.tsx` - 스텝 인디케이터, 폼 구조
- `frontend/shopping-frontend/src/components/ProductCard.tsx` - 카드 스타일

### 디자인 토큰
- `frontend/design-system/tailwind.preset.js` - TailwindCSS 설정
- `frontend/design-system/src/styles/index.css` - CSS Variables
- `frontend/design-system/src/styles/themes/shopping.css` - Shopping 테마

### 외부 참고
- [Tailwind UI Components](https://tailwindui.com/components)
- [Headless UI](https://headlessui.com/) - Accessible components
- [Radix UI](https://www.radix-ui.com/) - Primitives
- [React Hook Form](https://react-hook-form.com/) - Form management
