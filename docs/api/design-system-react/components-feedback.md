---
id: components-feedback-react
title: Feedback Components
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: Laze
tags: [api, react, feedback, alert, toast, modal]
related:
  - components-button-react
  - components-input-react
  - hooks-react
---

# Feedback Components

사용자 피드백 관련 컴포넌트의 API 레퍼런스입니다.

## Alert

인라인 알림 메시지 컴포넌트입니다.

### Import

```tsx
import { Alert } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'info' \| 'success' \| 'warning' \| 'error'` | `'info'` | 알림 유형 |
| `title` | `string` | - | 제목 |
| `dismissible` | `boolean` | `false` | 닫기 버튼 표시 |
| `onDismiss` | `() => void` | - | 닫기 핸들러 |
| `showIcon` | `boolean` | `true` | 아이콘 표시 |
| `bordered` | `boolean` | `false` | 테두리 표시 |
| `children` | `ReactNode` | - | 알림 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
<Alert variant="info" title="Information">
  This is an informational message.
</Alert>
```

### All Variants

```tsx
<div className="space-y-4">
  <Alert variant="info" title="Info">
    Informational message
  </Alert>

  <Alert variant="success" title="Success">
    Operation completed successfully
  </Alert>

  <Alert variant="warning" title="Warning">
    Please review before proceeding
  </Alert>

  <Alert variant="error" title="Error">
    An error occurred
  </Alert>
</div>
```

### Dismissible

```tsx
function DismissibleAlert() {
  const [show, setShow] = useState(true);

  if (!show) return null;

  return (
    <Alert
      variant="success"
      title="Saved!"
      dismissible
      onDismiss={() => setShow(false)}
    >
      Your changes have been saved.
    </Alert>
  );
}
```

---

## Toast & ToastContainer

토스트 알림 시스템입니다. 모듈 레벨 싱글톤으로 구현되어 Provider 없이 사용할 수 있습니다.

### Import

```tsx
import { ToastContainer, useToast } from '@portal/design-system-react';
```

### 설정

```tsx
// App.tsx - ToastProvider 불필요, ToastContainer만 배치
function App() {
  const { toasts, removeToast } = useToast();

  return (
    <>
      <YourApp />
      <ToastContainer
        position="top-right"
        maxToasts={5}
        toasts={toasts}
        onDismiss={removeToast}
      />
    </>
  );
}
```

### ToastContainer Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `position` | `ToastPosition` | `'top-right'` | 토스트 위치 |
| `maxToasts` | `number` | `5` | 최대 표시 개수 |
| `toasts` | `ToastItem[]` | required | 토스트 목록 |
| `onDismiss` | `(id: string) => void` | required | 닫기 핸들러 |

### ToastPosition

```ts
type ToastPosition =
  | 'top-right'
  | 'top-left'
  | 'top-center'
  | 'bottom-right'
  | 'bottom-left'
  | 'bottom-center';
```

### useToast Hook

```tsx
function MyComponent() {
  const toast = useToast();

  // 기본 사용
  const showSuccess = () => toast.success('저장되었습니다!');
  const showError = () => toast.error('오류가 발생했습니다.');

  // 옵션과 함께
  const showWithOptions = () => {
    toast.success('저장되었습니다!', {
      title: '성공',
      duration: 3000,
      dismissible: true,
    });
  };

  // 커스텀 토스트
  const showCustom = () => {
    toast.addToast({
      variant: 'info',
      message: '새 버전이 있습니다.',
      action: {
        label: '업데이트',
        onClick: () => window.location.reload(),
      },
    });
  };

  return (
    <div>
      <Button onClick={showSuccess}>Success</Button>
      <Button onClick={showError}>Error</Button>
    </div>
  );
}
```

---

## Modal

모달 다이얼로그 컴포넌트입니다. Portal을 사용하여 렌더링됩니다.

### Import

```tsx
import { Modal } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | `false` | 모달 표시 여부 |
| `onClose` | `() => void` | - | 닫기 핸들러 |
| `title` | `string` | - | 모달 제목 |
| `size` | `'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | 모달 크기 |
| `showClose` | `boolean` | `true` | 닫기 버튼 표시 |
| `closeOnBackdrop` | `boolean` | `true` | 배경 클릭 시 닫기 |
| `closeOnEscape` | `boolean` | `true` | ESC 키 닫기 |
| `children` | `ReactNode` | - | 모달 본문 |
| `className` | `string` | - | 추가 CSS 클래스 |

> Modal은 `footer` prop이 없습니다. footer 영역은 `children` 내부에서 직접 구성합니다.

### 기본 사용법

```tsx
function ConfirmModal() {
  const [open, setOpen] = useState(false);

  const handleConfirm = () => {
    // confirm logic
    setOpen(false);
  };

  return (
    <>
      <Button onClick={() => setOpen(true)}>Open Modal</Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title="Confirmation"
      >
        <p>Are you sure you want to proceed?</p>
        <div className="flex justify-end gap-3 mt-4">
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={handleConfirm}>
            Confirm
          </Button>
        </div>
      </Modal>
    </>
  );
}
```

### Form in Modal

```tsx
function EditProfileModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [form, setForm] = useState({ name: '', email: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      await updateProfile(form);
      onClose();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Edit Profile">
      <div className="space-y-4">
        <Input
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          label="Name"
        />
        <Input
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          label="Email"
          type="email"
        />
      </div>
      <div className="flex justify-end gap-3 mt-6">
        <Button variant="ghost" onClick={onClose}>
          Cancel
        </Button>
        <Button loading={isSubmitting} onClick={handleSubmit}>
          Save Changes
        </Button>
      </div>
    </Modal>
  );
}
```

---

## Spinner

로딩 스피너 컴포넌트입니다.

### Import

```tsx
import { Spinner } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | 스피너 크기 |
| `color` | `'primary' \| 'current' \| 'white'` | `'primary'` | 스피너 색상 |
| `label` | `string` | - | 접근성 레이블 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
<Spinner />
<Spinner size="lg" color="white" />
<Spinner size="sm" label="Loading..." />
```

### Loading State

```tsx
function DataLoader() {
  const [isLoading, setIsLoading] = useState(true);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return <DataContent />;
}
```

---

## Skeleton

스켈레톤 로더 컴포넌트입니다.

### Import

```tsx
import { Skeleton } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'text' \| 'circular' \| 'rectangular' \| 'rounded'` | `'text'` | 모양 |
| `width` | `string` | - | 너비 |
| `height` | `string` | - | 높이 |
| `animation` | `'pulse' \| 'wave' \| 'none'` | `'pulse'` | 애니메이션 |
| `lines` | `number` | `1` | 텍스트 라인 수 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
// 텍스트 스켈레톤
<Skeleton variant="text" width="200px" />

// 원형 (아바타)
<Skeleton variant="circular" width="48px" height="48px" />

// 사각형 (이미지)
<Skeleton variant="rectangular" width="100%" height="200px" />

// 여러 줄 텍스트
<Skeleton variant="text" lines={3} />
```

### Card Skeleton

```tsx
function PostCardSkeleton() {
  return (
    <Card>
      <div className="flex gap-4">
        <Skeleton variant="circular" width="48px" height="48px" />
        <div className="flex-1">
          <Skeleton variant="text" width="60%" />
          <Skeleton variant="text" width="40%" />
        </div>
      </div>
      <Skeleton variant="rectangular" width="100%" height="200px" className="mt-4" />
      <Skeleton variant="text" lines={3} className="mt-4" />
    </Card>
  );
}
```

---

## Progress

프로그레스 바 컴포넌트입니다.

### Import

```tsx
import { Progress } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `number` | `0` | 현재 값 |
| `max` | `number` | `100` | 최대 값 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `showLabel` | `boolean` | `false` | 레이블 표시 |
| `variant` | `'default' \| 'info' \| 'success' \| 'warning' \| 'error'` | `'default'` | 색상 |
| `className` | `string` | - | 추가 CSS 클래스 |

### 기본 사용법

```tsx
<Progress value={60} />
<Progress value={80} showLabel />
<Progress value={100} variant="success" />
```

### Upload Progress

```tsx
function FileUpload() {
  const [progress, setProgress] = useState(0);

  return (
    <div className="space-y-2">
      <p className="text-sm">Uploading...</p>
      <Progress value={progress} showLabel />
    </div>
  );
}
```
