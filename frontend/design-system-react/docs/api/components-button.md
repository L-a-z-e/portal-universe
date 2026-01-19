---
id: components-button-react
title: Button Components
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [api, react, button]
related:
  - components-input-react
  - components-feedback-react
---

# Button Components

버튼 관련 컴포넌트의 API 레퍼런스입니다.

## Button

기본 버튼 컴포넌트입니다.

### Import

```tsx
import { Button } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'primary' \| 'secondary' \| 'ghost' \| 'outline' \| 'danger'` | `'primary'` | 버튼 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 버튼 크기 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `fullWidth` | `boolean` | `false` | 전체 너비 |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | HTML 타입 |
| `className` | `string` | - | 추가 CSS 클래스 |
| `children` | `ReactNode` | - | 버튼 내용 |

모든 표준 `<button>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
import { Button } from '@portal/design-system-react';

function MyComponent() {
  const handleClick = () => {
    console.log('Button clicked!');
  };

  return (
    <Button onClick={handleClick}>
      Click me
    </Button>
  );
}
```

### Variants

```tsx
<div className="flex gap-4">
  <Button variant="primary">Primary</Button>
  <Button variant="secondary">Secondary</Button>
  <Button variant="ghost">Ghost</Button>
  <Button variant="outline">Outline</Button>
  <Button variant="danger">Danger</Button>
</div>
```

### Sizes

```tsx
<div className="flex items-center gap-4">
  <Button size="xs">Extra Small</Button>
  <Button size="sm">Small</Button>
  <Button size="md">Medium</Button>
  <Button size="lg">Large</Button>
</div>
```

### Loading State

```tsx
import { Button } from '@portal/design-system-react';
import { useState } from 'react';

function SubmitButton() {
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async () => {
    setIsLoading(true);
    try {
      await saveData();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Button loading={isLoading} onClick={handleSubmit}>
      {isLoading ? 'Saving...' : 'Save'}
    </Button>
  );
}
```

### Full Width

```tsx
<Button fullWidth>
  Full Width Button
</Button>
```

### With Icon

```tsx
import { PlusIcon } from '@heroicons/react/24/outline';

<Button variant="primary">
  <PlusIcon className="w-4 h-4" />
  Add Item
</Button>
```

### Form Submit

```tsx
function LoginForm() {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    // 로그인 로직
  };

  return (
    <form onSubmit={handleSubmit}>
      <Input name="email" label="Email" />
      <Button type="submit" loading={isSubmitting}>
        Sign In
      </Button>
    </form>
  );
}
```

### Button Group

```tsx
<div className="flex">
  <Button variant="outline" className="rounded-r-none border-r-0">
    Left
  </Button>
  <Button variant="outline" className="rounded-none">
    Center
  </Button>
  <Button variant="outline" className="rounded-l-none border-l-0">
    Right
  </Button>
</div>
```

### Disabled State

```tsx
<div className="flex gap-4">
  <Button disabled>Disabled Primary</Button>
  <Button variant="secondary" disabled>Disabled Secondary</Button>
  <Button variant="danger" disabled>Disabled Danger</Button>
</div>
```

### Ref 전달

```tsx
import { useRef, useEffect } from 'react';

function FocusButton() {
  const buttonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    buttonRef.current?.focus();
  }, []);

  return <Button ref={buttonRef}>Auto Focused</Button>;
}
```

### 커스텀 스타일

```tsx
<Button
  variant="primary"
  className="bg-purple-600 hover:bg-purple-700"
>
  Custom Styled
</Button>
```

### TypeScript

```tsx
import type { ButtonComponentProps } from '@portal/design-system-react';

const MyButton: React.FC<ButtonComponentProps> = (props) => {
  return <Button {...props} />;
};
```

### CSS Classes

| Class | Description |
|-------|-------------|
| Base | `inline-flex items-center justify-center font-medium rounded-md` |
| Primary | `bg-brand-primary text-text-inverse hover:bg-brand-primaryHover` |
| Secondary | `bg-bg-muted text-text-body hover:bg-bg-hover` |
| Ghost | `bg-transparent text-text-body hover:bg-bg-hover` |
| Outline | `bg-transparent border border-border-default hover:bg-bg-hover` |
| Danger | `bg-status-error text-white hover:bg-red-600` |

### Accessibility

- `disabled` 또는 `loading` 상태에서 `disabled` 속성 설정
- `focus-visible` 링 스타일 제공
- 키보드 네비게이션 지원 (Tab, Enter, Space)
- 적절한 contrast ratio 보장
