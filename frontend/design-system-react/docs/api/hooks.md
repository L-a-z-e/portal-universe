---
id: hooks-react
title: React Hooks Reference
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [api, react, hooks]
related:
  - theming-react
  - components-feedback-react
---

# React Hooks Reference

Design System React의 커스텀 훅 레퍼런스입니다.

## useTheme

테마 전환을 위한 훅입니다.

### Import

```tsx
import { useTheme } from '@portal/design-system-react';
```

### Options

```ts
interface UseThemeOptions {
  defaultService?: 'portal' | 'blog' | 'shopping';
  defaultMode?: 'light' | 'dark' | 'system';
}
```

### Returns

| Property | Type | Description |
|----------|------|-------------|
| `service` | `'portal' \| 'blog' \| 'shopping'` | 현재 서비스 |
| `mode` | `'light' \| 'dark' \| 'system'` | 현재 모드 설정 |
| `resolvedMode` | `'light' \| 'dark'` | 실제 적용 모드 |
| `setService` | `(service: string) => void` | 서비스 설정 |
| `setMode` | `(mode: string) => void` | 모드 설정 |
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

토스트 알림을 위한 훅입니다.

### Import

```tsx
import { useToast, ToastProvider } from '@portal/design-system-react';
```

### Setup

`useToast`는 `ToastProvider` 내부에서만 사용할 수 있습니다:

```tsx
// App.tsx
import { ToastProvider, ToastContainer } from '@portal/design-system-react';

function App() {
  return (
    <ToastProvider>
      <YourApp />
      <ToastContainer position="top-right" />
    </ToastProvider>
  );
}
```

### Returns

| Property | Type | Description |
|----------|------|-------------|
| `show` | `(options: ToastOptions) => string` | 토스트 표시 |
| `success` | `(message: string, options?) => string` | 성공 토스트 |
| `error` | `(message: string, options?) => string` | 에러 토스트 |
| `warning` | `(message: string, options?) => string` | 경고 토스트 |
| `info` | `(message: string, options?) => string` | 정보 토스트 |
| `remove` | `(id: string) => void` | 토스트 제거 |
| `clear` | `() => void` | 모든 토스트 제거 |

### Toast Options

```ts
interface ToastOptions {
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  message: string;
  duration?: number;  // ms, 0 = 자동 닫힘 없음
  dismissible?: boolean;
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
toast.show({
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

  toast.show({
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
    toast.remove(toastId);
    toast.success('업로드 완료!');
  } catch (error) {
    toast.remove(toastId);
    toast.error('업로드 실패');
  }
}
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

### 3. Context 분리

대규모 앱에서는 토스트와 테마 context를 분리하세요:

```tsx
// providers/index.tsx
function Providers({ children }) {
  return (
    <ThemeProvider>
      <ToastProvider>
        {children}
      </ToastProvider>
    </ThemeProvider>
  );
}
```

### 4. 커스텀 훅 조합

```tsx
// hooks/useApi.ts
function useApi() {
  const toast = useToast();

  const mutate = async (fn: () => Promise<void>, options?: MutateOptions) => {
    try {
      await fn();
      if (options?.successMessage) {
        toast.success(options.successMessage);
      }
    } catch (error) {
      toast.error(options?.errorMessage || '오류가 발생했습니다.');
      throw error;
    }
  };

  return { mutate };
}
```
