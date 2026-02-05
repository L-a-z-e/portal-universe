---
id: components-input-react
title: Input Components
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: documenter
tags: [api, react, input, form]
related:
  - components-button-react
  - components-feedback-react
---

# Input Components

폼 입력 관련 컴포넌트의 API 레퍼런스입니다.

## Input

텍스트 입력 컴포넌트입니다.

### Import

```tsx
import { Input } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string \| number` | - | 입력 값 |
| `onChange` | `(e: ChangeEvent<HTMLInputElement>) => void` | - | 변경 핸들러 |
| `type` | `'text' \| 'email' \| 'password' \| 'number' \| 'tel' \| 'url'` | `'text'` | 입력 타입 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `label` | `string` | - | 레이블 |
| `required` | `boolean` | `false` | 필수 입력 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<input>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
import { Input } from '@portal/design-system-react';
import { useState } from 'react';

function EmailInput() {
  const [email, setEmail] = useState('');

  return (
    <Input
      value={email}
      onChange={(e) => setEmail(e.target.value)}
      type="email"
      label="Email"
      placeholder="Enter your email"
    />
  );
}
```

### With Error

```tsx
function ValidatedInput() {
  const [email, setEmail] = useState('');
  const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  return (
    <Input
      value={email}
      onChange={(e) => setEmail(e.target.value)}
      type="email"
      label="Email"
      error={email.length > 0 && !isValid}
      errorMessage="Invalid email format"
    />
  );
}
```

### Controlled vs Uncontrolled

```tsx
// Controlled
const [value, setValue] = useState('');
<Input value={value} onChange={(e) => setValue(e.target.value)} />

// Uncontrolled with ref
const inputRef = useRef<HTMLInputElement>(null);
<Input ref={inputRef} defaultValue="initial" />
```

---

## Textarea

멀티라인 텍스트 입력 컴포넌트입니다.

### Import

```tsx
import { Textarea } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | - | 입력 값 |
| `onChange` | `(e: ChangeEvent<HTMLTextAreaElement>) => void` | - | 변경 핸들러 |
| `rows` | `number` | `4` | 표시 행 수 |
| `resize` | `'none' \| 'vertical' \| 'horizontal' \| 'both'` | `'vertical'` | 리사이즈 방향 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `label` | `string` | - | 레이블 |
| `required` | `boolean` | `false` | 필수 입력 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<textarea>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
const [content, setContent] = useState('');

<Textarea
  value={content}
  onChange={(e) => setContent(e.target.value)}
  label="Description"
  placeholder="Enter description..."
  rows={5}
/>
```

### With Validation

```tsx
function CommentForm() {
  const [comment, setComment] = useState('');
  const maxLength = 500;

  return (
    <Textarea
      value={comment}
      onChange={(e) => setComment(e.target.value)}
      label="Comment"
      required
      error={comment.length > maxLength}
      errorMessage={`${comment.length}/${maxLength} characters (exceeded limit)`}
      resize="vertical"
    />
  );
}
```

---

## Checkbox

체크박스 컴포넌트입니다.

### Import

```tsx
import { Checkbox } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `checked` | `boolean` | `false` | 체크 상태 |
| `onChange` | `(e: ChangeEvent<HTMLInputElement>) => void` | - | 변경 핸들러 |
| `indeterminate` | `boolean` | `false` | 불확정 상태 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `label` | `string` | - | 레이블 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
const [agreed, setAgreed] = useState(false);

<Checkbox
  checked={agreed}
  onChange={(e) => setAgreed(e.target.checked)}
  label="I agree to the terms"
/>
```

### Multiple Checkboxes

```tsx
const [options, setOptions] = useState({
  email: true,
  sms: false,
  push: true,
});

<div className="space-y-2">
  <Checkbox
    checked={options.email}
    onChange={(e) => setOptions({ ...options, email: e.target.checked })}
    label="Email notifications"
  />
  <Checkbox
    checked={options.sms}
    onChange={(e) => setOptions({ ...options, sms: e.target.checked })}
    label="SMS notifications"
  />
</div>
```

---

## Radio

라디오 버튼 그룹 컴포넌트입니다.

### Import

```tsx
import { Radio } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string \| number` | - | 선택된 값 |
| `onChange` | `(value: string \| number) => void` | - | 변경 핸들러 |
| `options` | `RadioOption[]` | `[]` | 옵션 배열 |
| `name` | `string` | required | name 속성 |
| `direction` | `'horizontal' \| 'vertical'` | `'vertical'` | 배치 방향 |
| `disabled` | `boolean` | `false` | 전체 그룹 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `className` | `string` | - | 추가 CSS 클래스 |

### RadioOption

```ts
interface RadioOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}
```

### 기본 사용법

```tsx
const [selectedPlan, setSelectedPlan] = useState('basic');

const plans = [
  { label: 'Basic', value: 'basic' },
  { label: 'Pro', value: 'pro' },
  { label: 'Enterprise', value: 'enterprise' },
];

<Radio
  value={selectedPlan}
  onChange={setSelectedPlan}
  options={plans}
  name="plan"
/>
```

### Horizontal Layout

```tsx
<Radio
  value={selected}
  onChange={setSelected}
  options={options}
  name="size"
  direction="horizontal"
  size="sm"
/>
```

---

## Switch

토글 스위치 컴포넌트입니다.

### Import

```tsx
import { Switch } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `checked` | `boolean` | `false` | 토글 상태 |
| `onChange` | `(e: ChangeEvent<HTMLInputElement>) => void` | - | 변경 핸들러 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `label` | `string` | - | 레이블 |
| `labelPosition` | `'left' \| 'right'` | `'right'` | 레이블 위치 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `activeColor` | `'primary' \| 'success' \| 'warning' \| 'error'` | `'primary'` | 활성화 색상 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
const [isDarkMode, setIsDarkMode] = useState(false);

<Switch
  checked={isDarkMode}
  onChange={(e) => setIsDarkMode(e.target.checked)}
  label="Dark Mode"
/>
```

### With Active Color

```tsx
<Switch
  checked={isEnabled}
  onChange={(e) => setIsEnabled(e.target.checked)}
  label="Enable notifications"
  activeColor="success"
  size="lg"
/>
```

---

## Select

셀렉트 드롭다운 컴포넌트입니다. 키보드 네비게이션을 지원합니다.

### Import

```tsx
import { Select } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string \| number \| null` | - | 선택된 값 |
| `onChange` | `(value: string \| number \| null) => void` | - | 변경 핸들러 |
| `options` | `SelectOption[]` | `[]` | 옵션 배열 |
| `placeholder` | `string` | `'Select an option'` | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `clearable` | `boolean` | `false` | 선택 해제 가능 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `label` | `string` | - | 레이블 |
| `required` | `boolean` | `false` | 필수 입력 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `className` | `string` | - | 추가 CSS 클래스 |

### SelectOption

```ts
interface SelectOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}
```

### 기본 사용법

```tsx
const [country, setCountry] = useState<string | number | null>(null);

const countries = [
  { label: 'Korea', value: 'KR' },
  { label: 'United States', value: 'US' },
  { label: 'Japan', value: 'JP' },
];

<Select
  value={country}
  onChange={setCountry}
  options={countries}
  label="Country"
  placeholder="Select a country"
  clearable
/>
```

### Keyboard Navigation

- `Enter` / `Space`: 드롭다운 열기/선택
- `ArrowDown` / `ArrowUp`: 옵션 탐색
- `Escape`: 드롭다운 닫기

---

## FormField

폼 필드 래퍼 컴포넌트입니다. 레이블, 에러 메시지, 도움말 텍스트를 포함합니다.

### Import

```tsx
import { FormField } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `label` | `string` | - | 레이블 |
| `required` | `boolean` | `false` | 필수 표시 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `helperText` | `string` | - | 도움말 텍스트 |
| `disabled` | `boolean` | `false` | 비활성화 (레이블 흐리게) |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 텍스트 크기 |
| `id` | `string` | auto-generated | 폼 필드 ID |
| `children` | `ReactNode` | - | 폼 필드 요소 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
<FormField
  label="Username"
  required
  error={hasError}
  errorMessage={errorMessage}
  helperText="3-20 characters, letters and numbers only"
>
  <Input value={username} onChange={handleChange} />
</FormField>
```

---

## SearchBar

검색 바 컴포넌트입니다. 검색 아이콘과 로딩 스피너를 포함합니다.

### Import

```tsx
import { SearchBar } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | - | 검색어 |
| `onValueChange` | `(value: string) => void` | - | 값 변경 핸들러 |
| `onChange` | `(e: ChangeEvent<HTMLInputElement>) => void` | - | 네이티브 변경 핸들러 |
| `placeholder` | `string` | `'Search...'` | 플레이스홀더 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `autoFocus` | `boolean` | - | 자동 포커스 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<input>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
const [query, setQuery] = useState('');
const [isSearching, setIsSearching] = useState(false);

<SearchBar
  value={query}
  onValueChange={setQuery}
  placeholder="Search posts..."
  loading={isSearching}
/>
```

### With Debounced Search

```tsx
import { useState, useEffect } from 'react';

function SearchPage() {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!query) return;
    setLoading(true);
    const timer = setTimeout(async () => {
      await searchAPI(query);
      setLoading(false);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <SearchBar
      value={query}
      onValueChange={setQuery}
      placeholder="Search..."
      loading={loading}
      autoFocus
    />
  );
}
```
