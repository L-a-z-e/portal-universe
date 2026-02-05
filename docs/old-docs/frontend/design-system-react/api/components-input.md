---
id: components-input-react
title: Input Components
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
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
| `rows` | `number` | `3` | 표시 행 수 |
| `resize` | `'none' \| 'vertical' \| 'horizontal' \| 'both'` | `'vertical'` | 리사이즈 방향 |

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
| `onChange` | `(checked: boolean) => void` | - | 변경 핸들러 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `label` | `string` | - | 레이블 |
| `labelPosition` | `'left' \| 'right'` | `'right'` | 레이블 위치 |

### 기본 사용법

```tsx
const [isDarkMode, setIsDarkMode] = useState(false);

<Switch
  checked={isDarkMode}
  onChange={setIsDarkMode}
  label="Dark Mode"
/>
```

---

## Select

셀렉트 드롭다운 컴포넌트입니다.

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
| `placeholder` | `string` | `'Select...'` | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `clearable` | `boolean` | `false` | 선택 해제 가능 |
| `searchable` | `boolean` | `false` | 검색 가능 |

### 기본 사용법

```tsx
const [country, setCountry] = useState(null);

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

---

## FormField

폼 필드 래퍼 컴포넌트입니다.

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
| `children` | `ReactNode` | - | 폼 필드 요소 |

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

검색 바 컴포넌트입니다.

### Import

```tsx
import { SearchBar } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `string` | - | 검색어 |
| `onChange` | `(value: string) => void` | - | 변경 핸들러 |
| `onSearch` | `(value: string) => void` | - | 검색 실행 (Enter) |
| `onClear` | `() => void` | - | 클리어 핸들러 |
| `placeholder` | `string` | `'Search...'` | 플레이스홀더 |
| `loading` | `boolean` | `false` | 로딩 상태 |

### 기본 사용법

```tsx
const [query, setQuery] = useState('');
const [isSearching, setIsSearching] = useState(false);

const handleSearch = async (q: string) => {
  setIsSearching(true);
  try {
    await searchAPI(q);
  } finally {
    setIsSearching(false);
  }
};

<SearchBar
  value={query}
  onChange={setQuery}
  onSearch={handleSearch}
  placeholder="Search posts..."
  loading={isSearching}
/>
```
