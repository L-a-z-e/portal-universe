---
id: hooks-react
title: React Hooks & Utilities Reference
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: documenter
tags: [api, react, hooks, utilities]
related:
  - theming-react
  - components-feedback-react
---

# React Hooks & Utilities Reference

Design System React의 커스텀 훅 및 유틸리티 레퍼런스입니다.

## useTheme

테마 전환을 위한 훅입니다.

### Import

```tsx
import { useTheme } from '@portal/design-system-react';
```

### Options

```ts
interface UseThemeOptions {
  defaultService?: ServiceType;  // 'portal' | 'blog' | 'shopping' | 'prism'
  defaultMode?: ThemeMode;       // 'light' | 'dark' | 'system'
}
```

### Returns

| Property | Type | Description |
|----------|------|-------------|
| `service` | `ServiceType` | 현재 서비스 |
| `mode` | `ThemeMode` | 현재 모드 설정 |
| `resolvedMode` | `'light' \| 'dark'` | 실제 적용 모드 |
| `setService` | `(service: ServiceType) => void` | 서비스 설정 |
| `setMode` | `(mode: ThemeMode) => void` | 모드 설정 |
| `toggleMode` | `() => void` | 모드 토글 |

### 기본 사용법

```tsx
import { useTheme } from '@portal/design-system-react';

function ThemeSettings() {
  const { service, mode, resolvedMode, setService, setMode, toggleMode } = useTheme();

  return (
    <div>
      <p>Current theme: {resolvedMode}</p>
      <p>Current service: {service}</p>

      <Button onClick={toggleMode}>
        Toggle Theme
      </Button>

      <Select
        value={service}
        onChange={setService}
        options={[
          { label: 'Portal', value: 'portal' },
          { label: 'Blog', value: 'blog' },
          { label: 'Shopping', value: 'shopping' },
          { label: 'Prism', value: 'prism' },
        ]}
      />
    </div>
  );
}
```

### Theme Switcher 컴포넌트

```tsx
import { useTheme } from '@portal/design-system-react';
import { SunIcon, MoonIcon } from '@heroicons/react/24/outline';

function ThemeToggle() {
  const { resolvedMode, toggleMode } = useTheme();

  return (
    <Button variant="ghost" onClick={toggleMode}>
      {resolvedMode === 'dark' ? (
        <SunIcon className="w-5 h-5" />
      ) : (
        <MoonIcon className="w-5 h-5" />
      )}
    </Button>
  );
}
```

### 시스템 테마 감지

```tsx
function App() {
  const { mode, resolvedMode, setMode } = useTheme({
    defaultMode: 'system', // 시스템 설정 따르기
  });

  // resolvedMode는 실제 적용되는 light/dark 값
  console.log('User setting:', mode);      // 'system'
  console.log('Applied mode:', resolvedMode); // 'light' or 'dark'

  return <div>Theme: {resolvedMode}</div>;
}
```

### 설정 저장

```tsx
import { useTheme } from '@portal/design-system-react';
import { useEffect } from 'react';

function App() {
  const { mode, service, setMode, setService } = useTheme();

  // 저장된 설정 복원
  useEffect(() => {
    const savedMode = localStorage.getItem('theme-mode');
    const savedService = localStorage.getItem('theme-service');

    if (savedMode) setMode(savedMode);
    if (savedService) setService(savedService);
  }, []);

  // 변경 시 저장
  useEffect(() => {
    localStorage.setItem('theme-mode', mode);
    localStorage.setItem('theme-service', service);
  }, [mode, service]);

  return <YourApp />;
}
```

---

## useToast

토스트 알림을 위한 훅입니다. 모듈 레벨 싱글톤으로 구현되어 Provider 없이 동작합니다.

### Import

```tsx
import { useToast } from '@portal/design-system-react';
```

### Setup

`useToast`는 Provider 없이 사용할 수 있습니다. `ToastContainer`만 앱 루트에 배치하세요:

```tsx
// App.tsx
import { ToastContainer, useToast } from '@portal/design-system-react';

function App() {
  const { toasts, removeToast } = useToast();

  return (
    <>
      <YourApp />
      <ToastContainer
        position="top-right"
        toasts={toasts}
        onDismiss={removeToast}
      />
    </>
  );
}
```

### Returns

| Property | Type | Description |
|----------|------|-------------|
| `toasts` | `ToastItem[]` | 현재 토스트 목록 |
| `addToast` | `(toast: Omit<ToastItem, 'id'>) => string` | 토스트 추가 |
| `removeToast` | `(id: string) => void` | 토스트 제거 |
| `clearToasts` | `() => void` | 모든 토스트 제거 |
| `success` | `(message: string, options?) => string` | 성공 토스트 |
| `error` | `(message: string, options?) => string` | 에러 토스트 |
| `warning` | `(message: string, options?) => string` | 경고 토스트 |
| `info` | `(message: string, options?) => string` | 정보 토스트 |

### Toast Options

```ts
interface ToastItem {
  id: string;
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  message: string;
  duration?: number;    // ms, 기본값 5000, 0 = 자동 닫힘 없음
  dismissible?: boolean; // 기본값 true
  action?: {
    label: string;
    onClick: () => void;
  };
}
```

### 기본 사용법

```tsx
import { useToast } from '@portal/design-system-react';

function SaveButton() {
  const toast = useToast();

  const handleSave = async () => {
    try {
      await saveData();
      toast.success('저장되었습니다!');
    } catch (error) {
      toast.error('저장에 실패했습니다.');
    }
  };

  return <Button onClick={handleSave}>Save</Button>;
}
```

### 옵션과 함께

```tsx
const toast = useToast();

// 제목 포함
toast.success('프로필이 업데이트되었습니다.', {
  title: '성공',
});

// 커스텀 지속시간
toast.info('곧 세션이 만료됩니다.', {
  duration: 10000, // 10초
});

// 자동 닫힘 없음
toast.warning('주의가 필요합니다.', {
  duration: 0,
  dismissible: true,
});
```

### 액션 버튼 포함

```tsx
const toast = useToast();

// 업데이트 알림
toast.addToast({
  variant: 'info',
  message: '새 버전이 있습니다.',
  action: {
    label: '업데이트',
    onClick: () => window.location.reload(),
  },
});

// 실행취소 패턴
function deleteItem(id: string) {
  const item = items.find((i) => i.id === id);
  setItems(items.filter((i) => i.id !== id));

  toast.addToast({
    variant: 'info',
    message: '항목이 삭제되었습니다.',
    action: {
      label: '실행취소',
      onClick: () => setItems([...items, item]),
    },
  });
}
```

### 프로미스와 함께

```tsx
const toast = useToast();

async function uploadFile(file: File) {
  const toastId = toast.info('업로드 중...', { duration: 0 });

  try {
    await upload(file);
    toast.removeToast(toastId);
    toast.success('업로드 완료!');
  } catch (error) {
    toast.removeToast(toastId);
    toast.error('업로드 실패');
  }
}
```

---

## useApiError

API 에러 처리를 위한 훅입니다. 백엔드의 `ApiResponse` 에러 형식을 파싱하고 토스트로 표시합니다.

### Import

```tsx
import { useApiError } from '@portal/design-system-react';
```

### Returns

| Property | Type | Description |
|----------|------|-------------|
| `handleError` | `(error: unknown, fallbackMessage?: string) => ApiErrorInfo` | 에러 처리 및 토스트 표시 |
| `getErrorMessage` | `(error: unknown, fallbackMessage?: string) => string` | 에러 메시지 추출 |
| `getErrorCode` | `(error: unknown) => string \| null` | 에러 코드 추출 |
| `getFieldErrors` | `(error: unknown) => Record<string, string>` | 필드별 에러 추출 |

### Types

```ts
interface ApiErrorInfo {
  message: string;
  code: string | null;
  details: FieldError[];
}

interface FieldError {
  field: string;
  message: string;
}
```

### 기본 사용법

```tsx
import { useApiError } from '@portal/design-system-react';

function MyForm() {
  const { handleError } = useApiError();

  const handleSubmit = async () => {
    try {
      await api.submitForm(data);
    } catch (err) {
      handleError(err, '폼 제출에 실패했습니다.');
    }
  };

  return <Button onClick={handleSubmit}>Submit</Button>;
}
```

### Field Errors 처리

```tsx
function SignupForm() {
  const { handleError, getFieldErrors } = useApiError();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const handleSubmit = async () => {
    try {
      await api.signup(formData);
    } catch (err) {
      handleError(err);
      setFieldErrors(getFieldErrors(err));
    }
  };

  return (
    <form>
      <Input
        error={!!fieldErrors.email}
        errorMessage={fieldErrors.email}
        label="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Input
        error={!!fieldErrors.password}
        errorMessage={fieldErrors.password}
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <Button onClick={handleSubmit}>Sign Up</Button>
    </form>
  );
}
```

### Standalone Utilities

컴포넌트 외부(스토어, 유틸 함수 등)에서 사용할 수 있는 독립 함수들:

```tsx
import {
  getApiErrorMessage,
  getApiErrorCode,
  getApiFieldErrors,
} from '@portal/design-system-react';

// Zustand 스토어 등에서 사용
try {
  await api.call();
} catch (error) {
  const message = getApiErrorMessage(error, 'Default message');
  const code = getApiErrorCode(error);
  console.error(`Error ${code}: ${message}`);
}
```

---

## cn() Utility

Tailwind CSS 클래스를 병합하는 유틸리티 함수입니다. 내부적으로 `clsx` + `tailwind-merge`를 사용합니다.

### Import

```tsx
import { cn } from '@portal/design-system-react';
```

### Signature

```ts
function cn(...inputs: ClassValue[]): string
```

### 기본 사용법

```tsx
import { cn } from '@portal/design-system-react';

// 조건부 클래스
<div className={cn(
  'base-class',
  isActive && 'active-class',
  isDisabled && 'disabled-class'
)} />

// 객체 스타일
<div className={cn({
  'text-blue-500': isPrimary,
  'text-red-500': isDanger,
  'opacity-50': isDisabled,
})} />

// Tailwind 충돌 해결 (tailwind-merge)
cn('px-4', 'px-6')  // → 'px-6' (후자가 우선)
cn('text-red-500', 'text-blue-500')  // → 'text-blue-500'
```

---

## 모범 사례

### 1. 토스트 메시지 가이드

```tsx
// Good: 구체적이고 액션 가능
toast.success('프로필 이미지가 업데이트되었습니다.');
toast.error('파일 크기는 5MB를 초과할 수 없습니다.');

// Bad: 모호하고 불명확
toast.success('완료');
toast.error('오류');
```

### 2. 적절한 지속시간

```tsx
// 성공 메시지: 짧게
toast.success('저장됨', { duration: 3000 });

// 에러: 길게 또는 수동 닫기
toast.error('결제 실패. 다시 시도해주세요.', {
  duration: 0,
  dismissible: true,
});
```

### 3. API 에러와 토스트 조합

```tsx
function useApi() {
  const { handleError } = useApiError();

  const mutate = async (fn: () => Promise<void>, options?: { successMessage?: string }) => {
    const toast = useToast();
    try {
      await fn();
      if (options?.successMessage) {
        toast.success(options.successMessage);
      }
    } catch (error) {
      handleError(error);
      throw error;
    }
  };

  return { mutate };
}
```
