---
id: design-component-003
title: Modal Component - Portal Pattern
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags:
  - design-system
  - modal
  - portal
  - teleport
  - react
  - vue
related:
  - design-component-001
  - design-component-002
---

# Modal Component - Portal Pattern

## 학습 목표

- Modal/Dialog 컴포넌트 구조 이해
- Portal 패턴 (React) / Teleport (Vue) 학습
- Backdrop, Escape Key, Body Scroll Lock 처리 방법 습득
- Accessibility (a11y) 속성 적용 방법 이해
- Animation & Transition 구현 학습

## 1. Modal 개념

### 1.1 Modal이란?

Modal은 사용자의 주의를 끌기 위해 현재 화면 위에 **오버레이로 표시되는 대화상자**입니다.

**특징:**
- ✅ Focus Trap: Modal 내부에만 포커스
- ✅ Backdrop: 배경 어둡게 처리
- ✅ Escape Key: ESC로 닫기
- ✅ Body Scroll Lock: 스크롤 방지
- ✅ Portal: DOM 트리 외부에 렌더링

### 1.2 Modal 사용 케이스

| 케이스 | 예시 |
|--------|------|
| **확인/취소** | 삭제 확인, 로그아웃 확인 |
| **Form** | 회원가입, 로그인, 설정 |
| **상세 정보** | 이미지 상세, 프로필 상세 |
| **알림** | 성공/실패 메시지 |

## 2. Portal Pattern

### 2.1 Portal이 필요한 이유

일반적으로 컴포넌트는 부모의 DOM 구조 내에 렌더링됩니다:

```html
<!-- 문제: z-index, overflow 제약 -->
<div id="app">
  <div class="container" style="overflow: hidden">
    <Modal />  <!-- 여기에 렌더링되면 overflow에 잘림 -->
  </div>
</div>
```

Portal을 사용하면 DOM 트리의 **다른 위치**에 렌더링할 수 있습니다:

```html
<!-- 해결: body 직접 자식으로 렌더링 -->
<div id="app">
  <div class="container" style="overflow: hidden">
    <!-- Modal 논리적 위치 -->
  </div>
</div>

<div id="portal-target">
  <Modal />  <!-- 실제 렌더링 위치 -->
</div>
```

### 2.2 React Portal

```tsx
import { createPortal } from 'react-dom';

const modalContent = (
  <div className="modal">
    Modal Content
  </div>
);

return createPortal(modalContent, document.body);
```

### 2.3 Vue Teleport

```vue
<Teleport to="body">
  <div class="modal">
    Modal Content
  </div>
</Teleport>
```

## 3. React 구현 분석

### 3.1 전체 코드 구조

`frontend/design-system-react/src/components/Modal/Modal.tsx`:

```tsx
import {
  forwardRef,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import type { ModalProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface ModalComponentProps extends Omit<ModalProps, 'open'> {
  open: boolean;
  onClose: () => void;
  children?: ReactNode;
}

const sizeClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
};

export const Modal = forwardRef<HTMLDivElement, ModalComponentProps>(
  (
    {
      open,
      onClose,
      title,
      size = 'md',
      showClose = true,
      closeOnBackdrop = true,
      closeOnEscape = true,
      className,
      children,
      ...props
    },
    ref
  ) => {
    // ESC 키 핸들러
    const handleEscape = useCallback(
      (e: KeyboardEvent) => {
        if (closeOnEscape && e.key === 'Escape') {
          onClose();
        }
      },
      [closeOnEscape, onClose]
    );

    // 마운트/언마운트 시 처리
    useEffect(() => {
      if (open) {
        // ESC 키 리스너 추가
        document.addEventListener('keydown', handleEscape);
        // Body 스크롤 방지
        document.body.style.overflow = 'hidden';
      }

      return () => {
        document.removeEventListener('keydown', handleEscape);
        document.body.style.overflow = '';
      };
    }, [open, handleEscape]);

    if (!open) return null;

    const modalContent = (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'modal-title' : undefined}
      >
        {/* Backdrop */}
        <div
          className="absolute inset-0 bg-black/60 backdrop-blur-sm animate-fade-in"
          onClick={closeOnBackdrop ? onClose : undefined}
          aria-hidden="true"
        />

        {/* Modal - Linear dark mode first */}
        <div
          ref={ref}
          className={cn(
            'relative w-full rounded-xl',
            // Dark mode (기본)
            'bg-[#18191b]',
            'border border-[#2a2a2a]',
            'shadow-[0_16px_48px_rgba(0,0,0,0.6)]',
            // Light mode
            'light:bg-white light:border-gray-200 light:shadow-2xl',
            'animate-scale-in',
            sizeClasses[size],
            className
          )}
          {...props}
        >
          {/* Header */}
          {(title || showClose) && (
            <div className="flex items-center justify-between px-5 py-4 border-b border-[#2a2a2a] light:border-gray-200">
              {title && (
                <h2 id="modal-title" className="text-lg font-semibold text-white light:text-gray-900">
                  {title}
                </h2>
              )}
              {showClose && (
                <button
                  type="button"
                  onClick={onClose}
                  className={cn(
                    'p-1.5 rounded-md',
                    'text-[#6b6b6b] hover:text-[#b4b4b4] hover:bg-white/5',
                    'light:text-gray-400 light:hover:text-gray-600 light:hover:bg-gray-100',
                    'transition-colors duration-100',
                    'focus:outline-none focus:ring-2 focus:ring-[#5e6ad2]',
                    !title && 'ml-auto'
                  )}
                  aria-label="Close"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </div>
          )}

          {/* Content */}
          <div className="px-5 py-5 text-[#b4b4b4] light:text-gray-600">
            {children}
          </div>
        </div>
      </div>
    );

    // Portal to body
    return createPortal(modalContent, document.body);
  }
);

Modal.displayName = 'Modal';
```

### 3.2 핵심 특징

#### 1. createPortal
```tsx
return createPortal(modalContent, document.body);
```
- Modal을 `document.body`에 직접 렌더링
- z-index, overflow 문제 해결

#### 2. Escape Key Handler
```tsx
const handleEscape = useCallback(
  (e: KeyboardEvent) => {
    if (closeOnEscape && e.key === 'Escape') {
      onClose();
    }
  },
  [closeOnEscape, onClose]
);

useEffect(() => {
  if (open) {
    document.addEventListener('keydown', handleEscape);
  }
  return () => {
    document.removeEventListener('keydown', handleEscape);
  };
}, [open, handleEscape]);
```

#### 3. Body Scroll Lock
```tsx
useEffect(() => {
  if (open) {
    document.body.style.overflow = 'hidden';
  }
  return () => {
    document.body.style.overflow = '';
  };
}, [open]);
```

#### 4. Backdrop Click
```tsx
<div
  className="absolute inset-0 bg-black/60"
  onClick={closeOnBackdrop ? onClose : undefined}
/>
```

#### 5. Accessibility
```tsx
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby={title ? 'modal-title' : undefined}
>
  <h2 id="modal-title">{title}</h2>
</div>
```

## 4. Vue 구현 분석

### 4.1 전체 코드 구조

`frontend/design-system-vue/src/components/Modal/Modal.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue';
import type { ModalProps } from './Modal.types';

const props = withDefaults(defineProps<ModalProps>(), {
  modelValue: false,
  title: '',
  size: 'md',
  showClose: true,
  closeOnBackdrop: true
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'close': []
}>();

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl'
};

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

function close() {
  isOpen.value = false;
  emit('close');
}

function handleBackdropClick() {
  if (props.closeOnBackdrop) {
    close();
  }
}

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape' && isOpen.value) {
    close();
  }
}

// Body scroll lock
watch(isOpen, (value) => {
  if (value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

onMounted(() => {
  document.addEventListener('keydown', handleEscape);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape);
  document.body.style.overflow = '';
});
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-[160ms]"
      leave-active-class="transition-opacity duration-[100ms]"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
        @click.self="handleBackdropClick"
      >
        <Transition
          enter-active-class="transition-all duration-150 ease-out"
          leave-active-class="transition-all duration-100 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="isOpen"
            :class="[
              'bg-[#18191b] rounded-xl w-full',
              'border border-[#2a2a2a]',
              'shadow-[0_16px_48px_rgba(0,0,0,0.6)]',
              'light:bg-white light:border-gray-200 light:shadow-2xl',
              sizeClasses[size]
            ]"
            @click.stop
          >
            <!-- Header -->
            <div
              v-if="title || showClose"
              class="flex items-center justify-between px-5 py-4 border-b border-[#2a2a2a] light:border-gray-200"
            >
              <h3 v-if="title" class="text-lg font-semibold text-white light:text-gray-900">
                {{ title }}
              </h3>
              <button
                v-if="showClose"
                @click="close"
                class="p-1.5 hover:bg-white/5 rounded-md transition-colors duration-100 text-[#6b6b6b] hover:text-[#b4b4b4]"
                aria-label="Close"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-5 py-5 text-[#b4b4b4] light:text-gray-600">
              <slot />
            </div>

            <!-- Footer (optional) -->
            <div
              v-if="$slots.footer"
              class="px-5 py-4 bg-[#0f1011] rounded-b-xl border-t border-[#2a2a2a] light:bg-gray-50 light:border-gray-200"
            >
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
```

### 4.2 핵심 특징

#### 1. Teleport
```vue
<Teleport to="body">
  <!-- Modal content -->
</Teleport>
```

#### 2. v-model 바인딩
```vue
const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
```

#### 3. Nested Transition
```vue
<!-- Backdrop Fade -->
<Transition
  enter-active-class="transition-opacity duration-[160ms]"
  enter-from-class="opacity-0"
>
  <!-- Modal Scale + Fade -->
  <Transition
    enter-active-class="transition-all duration-150"
    enter-from-class="opacity-0 scale-95 translate-y-2"
  >
    <div>Modal</div>
  </Transition>
</Transition>
```

#### 4. Named Slots
```vue
<!-- Body -->
<slot />

<!-- Footer (optional) -->
<slot name="footer" />
```

## 5. 실습 예제

### 예제 1: 기본 사용

```tsx
// React
const [open, setOpen] = useState(false);

<Button onClick={() => setOpen(true)}>Open Modal</Button>

<Modal
  open={open}
  onClose={() => setOpen(false)}
  title="Confirm Action"
  size="md"
>
  <p>Are you sure you want to continue?</p>
  <div className="flex gap-2 mt-4">
    <Button variant="primary" onClick={handleConfirm}>
      Confirm
    </Button>
    <Button variant="secondary" onClick={() => setOpen(false)}>
      Cancel
    </Button>
  </div>
</Modal>
```

```vue
<!-- Vue -->
<script setup lang="ts">
const isOpen = ref(false);
</script>

<template>
  <Button @click="isOpen = true">Open Modal</Button>

  <Modal v-model="isOpen" title="Confirm Action" size="md">
    <p>Are you sure you want to continue?</p>
    <div class="flex gap-2 mt-4">
      <Button variant="primary" @click="handleConfirm">Confirm</Button>
      <Button variant="secondary" @click="isOpen = false">Cancel</Button>
    </div>
  </Modal>
</template>
```

### 예제 2: Form Modal

```tsx
// React
const [open, setOpen] = useState(false);
const [form, setForm] = useState({ name: '', email: '' });

const handleSubmit = async () => {
  await api.submit(form);
  setOpen(false);
};

<Modal
  open={open}
  onClose={() => setOpen(false)}
  title="Add User"
  size="md"
>
  <Input
    label="Name"
    value={form.name}
    onValueChange={(v) => setForm({ ...form, name: v })}
  />
  <Input
    label="Email"
    type="email"
    value={form.email}
    onValueChange={(v) => setForm({ ...form, email: v })}
  />
  <div className="flex gap-2 mt-4">
    <Button variant="primary" onClick={handleSubmit}>
      Submit
    </Button>
    <Button variant="secondary" onClick={() => setOpen(false)}>
      Cancel
    </Button>
  </div>
</Modal>
```

### 예제 3: Vue Footer Slot

```vue
<template>
  <Modal v-model="isOpen" title="User Details">
    <div>
      <p>Name: John Doe</p>
      <p>Email: john@example.com</p>
    </div>

    <template #footer>
      <div class="flex gap-2 justify-end">
        <Button variant="secondary" @click="isOpen = false">
          Close
        </Button>
        <Button variant="primary" @click="handleEdit">
          Edit
        </Button>
      </div>
    </template>
  </Modal>
</template>
```

### 예제 4: Non-closable Modal

```tsx
// React
<Modal
  open={open}
  onClose={() => {}}
  title="Processing..."
  closeOnBackdrop={false}
  closeOnEscape={false}
  showClose={false}
>
  <Spinner />
  <p>Please wait while we process your request...</p>
</Modal>
```

### 예제 5: Nested Modals

```tsx
// React
const [mainOpen, setMainOpen] = useState(false);
const [confirmOpen, setConfirmOpen] = useState(false);

<Modal open={mainOpen} onClose={() => setMainOpen(false)} title="Main Modal">
  <p>Main content</p>
  <Button onClick={() => setConfirmOpen(true)}>Delete</Button>
</Modal>

<Modal
  open={confirmOpen}
  onClose={() => setConfirmOpen(false)}
  title="Confirm Delete"
  size="sm"
>
  <p>Are you sure?</p>
  <Button variant="danger" onClick={handleDelete}>Delete</Button>
</Modal>
```

## 6. 핵심 요약

### ✅ Key Takeaways

1. **Portal/Teleport**: DOM 트리 외부 렌더링
2. **Body Scroll Lock**: Modal 열릴 때 스크롤 방지
3. **Escape Key**: ESC로 닫기
4. **Backdrop Click**: 배경 클릭 시 닫기 (옵션)
5. **Accessibility**: `role="dialog"`, `aria-modal="true"`

### 🎯 Best Practices

```tsx
// ✅ DO
<Modal
  open={open}
  onClose={handleClose}
  title="Modal Title"
  closeOnEscape={true}
  closeOnBackdrop={true}
>
  <Content />
</Modal>

// ❌ DON'T
// 1. Portal 없이 일반 div로 Modal 구현
<div className="fixed inset-0">  // ❌ z-index 문제

// 2. Body scroll lock 누락
// 3. Escape key 핸들러 누락
// 4. Accessibility 속성 누락
```

### 📋 Checklist

- [ ] Portal/Teleport 사용
- [ ] Body scroll lock 구현
- [ ] ESC key 핸들러
- [ ] Backdrop click 처리
- [ ] `role="dialog"` 추가
- [ ] `aria-modal="true"` 추가
- [ ] Animation/Transition 추가

## 7. 관련 문서

- [Button Component](./button-component.md) - Modal 내 버튼 사용
- [Input Component](./input-component.md) - Modal Form
- [Design Tokens](../tokens/design-tokens.md) - Modal 스타일링
