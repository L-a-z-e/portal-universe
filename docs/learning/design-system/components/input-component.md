---
id: design-component-002
title: Input Component - Validation & States
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags:
  - design-system
  - input
  - validation
  - form
  - react
  - vue
related:
  - design-component-001
  - design-component-003
---

# Input Component - Validation & States

## 학습 목표

- Input 컴포넌트의 State 관리 (Normal, Focus, Error, Disabled) 이해
- 유효성 검사 UI 패턴 학습
- Label, Placeholder, Helper Text 구조 습득
- Semantic Token을 활용한 Input 스타일링 방법 이해
- Vue와 React에서 v-model / Controlled Component 패턴 비교

## 1. Input States 개념

### 1.1 주요 States

| State | 시각적 특징 | 용도 |
|-------|-------------|------|
| **Normal** | 기본 테두리 | 입력 대기 상태 |
| **Hover** | 테두리 색상 변경 | 마우스 오버 |
| **Focus** | 테두리 + Ring | 입력 중 |
| **Error** | 빨간 테두리 + 에러 메시지 | 유효성 검사 실패 |
| **Disabled** | 회색 배경 + 커서 변경 | 입력 불가 |

### 1.2 Input 구조

```
┌─────────────────────────────────────┐
│ Label (optional) *                  │  ← Label + Required 표시
├─────────────────────────────────────┤
│ Input Field                         │  ← Input Element
│ Placeholder text...                 │
└─────────────────────────────────────┘
  Error message here                     ← Error Message (조건부)
```

## 2. Portal Universe Input 구조

### 2.1 TypeScript 인터페이스

```typescript
// Input.types.ts
export interface InputProps {
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url';
  modelValue?: string | number;
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  size?: 'sm' | 'md' | 'lg';
}
```

### 2.2 Size Variants

| Size | Height | Padding | Font Size |
|------|--------|---------|-----------|
| **sm** | 32px (h-8) | px-3 | text-sm |
| **md** | 36px (h-9) | px-3 | text-sm |
| **lg** | 44px (h-11) | px-4 | text-base |

## 3. Vue 구현 분석

### 3.1 전체 코드

`frontend/design-system-vue/src/components/Input/Input.vue`:

```vue
<script setup lang="ts">
import type { InputProps } from './Input.types';

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  modelValue: '',
  placeholder: '',
  disabled: false,
  error: false,
  errorMessage: '',
  label: '',
  required: false,
  size: 'md'
});

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>();

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
}

const sizeClasses = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-sm',
  lg: 'h-11 px-4 text-base'
};
</script>

<template>
  <div class="input-wrapper w-full">
    <!-- Label -->
    <label
      v-if="label"
      class="block text-sm font-medium text-text-body mb-1.5"
    >
      {{ label }}
      <span v-if="required" class="text-status-error ml-0.5">*</span>
    </label>

    <!-- Input - Using design tokens -->
    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="handleInput"
      :class="[
        'w-full rounded-md',
        // Semantic design tokens (자동 테마 대응)
        'bg-bg-card',
        'text-text-body placeholder:text-text-muted',
        'border border-border-default',
        // Transitions
        'transition-all duration-150 ease-out',
        // Focus state
        'focus:outline-none focus:ring-2 focus:ring-brand-primary/30 focus:border-brand-primary',
        // Hover state
        'hover:border-border-hover',
        // Sizing
        sizeClasses[size],
        // Error state
        error
          ? 'border-status-error focus:border-status-error focus:ring-status-error/30'
          : '',
        // Disabled state
        disabled && 'bg-bg-elevated cursor-not-allowed opacity-50'
      ]"
    />

    <!-- Error Message -->
    <p
      v-if="error && errorMessage"
      class="mt-1.5 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>
```

### 3.2 핵심 특징

#### v-model 양방향 바인딩
```vue
<!-- 부모 컴포넌트 -->
<script setup lang="ts">
const email = ref('');
</script>

<template>
  <Input v-model="email" type="email" label="Email" />
</template>
```

**내부 동작:**
```vue
<!-- 자식 컴포넌트 -->
<input
  :value="modelValue"                           // 부모 → 자식
  @input="emit('update:modelValue', $event)"    // 자식 → 부모
/>
```

#### Semantic Token 사용
```vue
<input
  :class="[
    'bg-bg-card',                    // 배경: 카드 배경 색상
    'text-text-body',                // 텍스트: 본문 색상
    'placeholder:text-text-muted',   // Placeholder: 약한 색상
    'border border-border-default',  // 테두리: 기본 테두리 색상
  ]"
/>
```

**장점:**
- 다크/라이트 모드 자동 대응
- 서비스별 테마 자동 대응

#### Focus State
```vue
'focus:outline-none'                          // 기본 outline 제거
'focus:ring-2'                                // Ring 표시
'focus:ring-brand-primary/30'                 // Ring 색상 (30% 투명도)
'focus:border-brand-primary'                  // 테두리 색상
```

## 4. React 구현 분석

### 4.1 전체 코드

`frontend/design-system-react/src/components/Input/Input.tsx`:

```tsx
import { forwardRef, type InputHTMLAttributes } from 'react';
import type { InputProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface InputComponentProps
  extends Omit<InputProps, 'value'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  value?: string | number;
  onValueChange?: (value: string) => void;
}

const sizeClasses = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-sm',
  lg: 'h-11 px-4 text-base',
};

export const Input = forwardRef<HTMLInputElement, InputComponentProps>(
  (
    {
      type = 'text',
      value,
      placeholder,
      disabled,
      error,
      errorMessage,
      label,
      required,
      size = 'md',
      className,
      onChange,
      onValueChange,
      ...props
    },
    ref
  ) => {
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.(e);
      onValueChange?.(e.target.value);
    };

    return (
      <div className="input-wrapper w-full">
        {/* Label */}
        {label && (
          <label className="block text-sm font-medium text-text-body mb-1.5">
            {label}
            {required && <span className="text-status-error ml-0.5">*</span>}
          </label>
        )}

        {/* Input */}
        <input
          ref={ref}
          type={type}
          value={value}
          placeholder={placeholder}
          disabled={disabled}
          onChange={handleChange}
          className={cn(
            'w-full rounded-md',
            // Design tokens
            'bg-bg-card',
            'text-text-body placeholder:text-text-muted',
            'border border-border-default',
            // Transitions
            'transition-all duration-150 ease-out',
            // Focus
            'focus:outline-none focus:ring-2 focus:ring-brand-primary/30 focus:border-brand-primary',
            // Hover
            'hover:border-border-hover',
            // Size
            sizeClasses[size],
            // Error
            error &&
              'border-status-error focus:border-status-error focus:ring-status-error/30',
            // Disabled
            disabled && 'bg-bg-elevated cursor-not-allowed opacity-50',
            className
          )}
          {...props}
        />

        {/* Error Message */}
        {error && errorMessage && (
          <p className="mt-1.5 text-sm text-status-error">{errorMessage}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
```

### 4.2 Controlled Component 패턴

```tsx
// React
const [email, setEmail] = useState('');

<Input
  type="email"
  value={email}
  onValueChange={setEmail}
  label="Email"
/>
```

**vs Vue v-model:**
```vue
<!-- Vue -->
<script setup lang="ts">
const email = ref('');
</script>

<template>
  <Input v-model="email" type="email" label="Email" />
</template>
```

## 5. 실습 예제

### 예제 1: 기본 사용

```tsx
// React
<Input
  type="text"
  placeholder="Enter your name"
  label="Name"
  required
/>
```

```vue
<!-- Vue -->
<Input
  type="text"
  placeholder="Enter your name"
  label="Name"
  required
/>
```

### 예제 2: 유효성 검사

```tsx
// React
const [email, setEmail] = useState('');
const [error, setError] = useState('');

const validateEmail = (value: string) => {
  if (!value.includes('@')) {
    setError('Please enter a valid email');
  } else {
    setError('');
  }
};

const handleChange = (value: string) => {
  setEmail(value);
  validateEmail(value);
};

<Input
  type="email"
  value={email}
  onValueChange={handleChange}
  label="Email"
  error={!!error}
  errorMessage={error}
  required
/>
```

```vue
<!-- Vue -->
<script setup lang="ts">
const email = ref('');
const error = ref('');

const validateEmail = (value: string) => {
  if (!value.includes('@')) {
    error.value = 'Please enter a valid email';
  } else {
    error.value = '';
  }
};

watch(email, (newValue) => {
  validateEmail(newValue);
});
</script>

<template>
  <Input
    v-model="email"
    type="email"
    label="Email"
    :error="!!error"
    :error-message="error"
    required
  />
</template>
```

### 예제 3: Form 통합

```tsx
// React
interface LoginForm {
  email: string;
  password: string;
}

const [form, setForm] = useState<LoginForm>({
  email: '',
  password: ''
});

const handleSubmit = (e: React.FormEvent) => {
  e.preventDefault();
  console.log('Submit:', form);
};

<form onSubmit={handleSubmit}>
  <Input
    type="email"
    value={form.email}
    onValueChange={(value) => setForm({ ...form, email: value })}
    label="Email"
    required
  />

  <Input
    type="password"
    value={form.password}
    onValueChange={(value) => setForm({ ...form, password: value })}
    label="Password"
    required
  />

  <Button type="submit" variant="primary">
    Login
  </Button>
</form>
```

```vue
<!-- Vue -->
<script setup lang="ts">
interface LoginForm {
  email: string;
  password: string;
}

const form = reactive<LoginForm>({
  email: '',
  password: ''
});

const handleSubmit = () => {
  console.log('Submit:', form);
};
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <Input
      v-model="form.email"
      type="email"
      label="Email"
      required
    />

    <Input
      v-model="form.password"
      type="password"
      label="Password"
      required
    />

    <Button type="submit" variant="primary">
      Login
    </Button>
  </form>
</template>
```

### 예제 4: Real-time Validation

```tsx
// React
const [username, setUsername] = useState('');
const [isChecking, setIsChecking] = useState(false);
const [isAvailable, setIsAvailable] = useState<boolean | null>(null);

useEffect(() => {
  if (username.length >= 3) {
    setIsChecking(true);
    const timer = setTimeout(async () => {
      const available = await checkUsernameAvailability(username);
      setIsAvailable(available);
      setIsChecking(false);
    }, 500);
    return () => clearTimeout(timer);
  }
}, [username]);

<Input
  type="text"
  value={username}
  onValueChange={setUsername}
  label="Username"
  error={isAvailable === false}
  errorMessage={isAvailable === false ? 'Username already taken' : ''}
  placeholder="Enter username"
/>
{isChecking && <span className="text-text-meta text-sm">Checking...</span>}
{isAvailable && <span className="text-status-success text-sm">✓ Available</span>}
```

### 예제 5: Input with Icon

```tsx
// React
<div className="relative">
  <Input
    type="search"
    placeholder="Search..."
    className="pl-10"
  />
  <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
</div>
```

## 6. 고급 패턴

### 6.1 Debounced Input

```tsx
// React
import { useDebouncedCallback } from 'use-debounce';

const [query, setQuery] = useState('');

const debouncedSearch = useDebouncedCallback(
  (value: string) => {
    // API call
    searchAPI(value);
  },
  500
);

const handleChange = (value: string) => {
  setQuery(value);
  debouncedSearch(value);
};

<Input
  type="search"
  value={query}
  onValueChange={handleChange}
  placeholder="Search..."
/>
```

### 6.2 Auto-focus

```tsx
// React
const inputRef = useRef<HTMLInputElement>(null);

useEffect(() => {
  inputRef.current?.focus();
}, []);

<Input ref={inputRef} />
```

```vue
<!-- Vue -->
<script setup lang="ts">
const inputRef = ref<HTMLInputElement>();

onMounted(() => {
  inputRef.value?.focus();
});
</script>

<template>
  <Input ref="inputRef" />
</template>
```

### 6.3 Masked Input (전화번호)

```tsx
// React
const formatPhoneNumber = (value: string) => {
  const cleaned = value.replace(/\D/g, '');
  const match = cleaned.match(/^(\d{3})(\d{4})(\d{4})$/);
  if (match) {
    return `${match[1]}-${match[2]}-${match[3]}`;
  }
  return value;
};

const [phone, setPhone] = useState('');

const handleChange = (value: string) => {
  const formatted = formatPhoneNumber(value);
  setPhone(formatted);
};

<Input
  type="tel"
  value={phone}
  onValueChange={handleChange}
  label="Phone Number"
  placeholder="010-1234-5678"
/>
```

## 7. 핵심 요약

### ✅ Key Takeaways

1. **Semantic Token 사용**: `bg-bg-card`, `text-text-body`
2. **Focus State**: `focus:ring-2 focus:ring-brand-primary/30`
3. **Error State**: 빨간 테두리 + 에러 메시지
4. **v-model (Vue) vs Controlled Component (React)**
5. **Required 표시**: Label 옆에 `*` 표시

### 🎯 Best Practices

```tsx
// ✅ DO
<Input
  label="Email"
  error={!!error}
  errorMessage={error}
  required
/>

// Validation with debounce
const debouncedValidate = useDebouncedCallback(validate, 500);

// ❌ DON'T
<input className="border p-2" />  // 직접 스타일링

<Input error={true} />  // errorMessage 없이 error만 true
```

### 📋 Validation Checklist

```typescript
// 1. Required 체크
if (!value) return 'This field is required';

// 2. Format 체크
if (type === 'email' && !value.includes('@')) {
  return 'Invalid email format';
}

// 3. Length 체크
if (value.length < 3) return 'Min 3 characters';

// 4. Pattern 체크 (정규식)
if (!/^[a-zA-Z0-9]+$/.test(value)) {
  return 'Only letters and numbers allowed';
}
```

## 8. 관련 문서

- [Button Component](./button-component.md) - Form 제출 버튼
- [Modal Component](./modal-component.md) - Modal 내 Input 사용
- [Design Tokens](../tokens/design-tokens.md) - Input에 사용된 Token
