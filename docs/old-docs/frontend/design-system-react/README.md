---
id: design-system-react-index
title: Design System React
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [design-system, react, components]
related:
  - design-tokens-index
  - design-types-index
  - design-system-vue-index
---

# @portal/design-system-react

Portal Universe Design System의 React 컴포넌트 라이브러리입니다. React 18+와 TypeScript를 기반으로 구현되었습니다.

## 개요

Design System React는 다음을 제공합니다:

- **React 18+**: 최신 React 기능 지원
- **TypeScript**: 완전한 타입 지원
- **Design Tokens 통합**: 일관된 스타일링
- **Storybook**: 인터랙티브 문서화
- **테스트**: Vitest + Testing Library

## 설치

```bash
# 패키지 설치
npm install @portal/design-system-react

# 피어 의존성
npm install @portal/design-tokens @portal/design-types
```

## 빠른 시작

### 1. 스타일 임포트

```tsx
// main.tsx 또는 App.tsx
import '@portal/design-tokens/dist/tokens.css';
import '@portal/design-system-react/dist/style.css';
```

### 2. 컴포넌트 사용

```tsx
import { Button, Input, Card } from '@portal/design-system-react';
import { useState } from 'react';

function LoginForm() {
  const [email, setEmail] = useState('');

  return (
    <Card padding="lg">
      <Input
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        label="Email"
        placeholder="Enter your email"
      />
      <Button variant="primary" onClick={handleSubmit}>
        Submit
      </Button>
    </Card>
  );
}
```

### 3. Toast 시스템 설정

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

// 컴포넌트에서 Toast 사용
import { useToast } from '@portal/design-system-react';

function MyComponent() {
  const toast = useToast();

  const handleSave = () => {
    toast.success('저장되었습니다!');
  };

  return <Button onClick={handleSave}>Save</Button>;
}
```

## 컴포넌트 목록 (30개)

### Form Components (9개)
| Component | Description |
|-----------|-------------|
| `Button` | 버튼 |
| `Input` | 텍스트 입력 |
| `Textarea` | 멀티라인 텍스트 |
| `Checkbox` | 체크박스 |
| `Radio` | 라디오 버튼 그룹 |
| `Switch` | 토글 스위치 |
| `Select` | 셀렉트 드롭다운 |
| `FormField` | 폼 필드 래퍼 |
| `SearchBar` | 검색 바 |

### Feedback Components (6개)
| Component | Description |
|-----------|-------------|
| `Alert` | 알림 메시지 |
| `Toast` | 토스트 알림 (ToastContainer 포함) |
| `Modal` | 모달 다이얼로그 |
| `Spinner` | 로딩 스피너 |
| `Skeleton` | 스켈레톤 로더 |
| `Progress` | 프로그레스 바 |

### Layout Components (4개)
| Component | Description |
|-----------|-------------|
| `Card` | 카드 컨테이너 |
| `Container` | 레이아웃 컨테이너 |
| `Stack` | Flexbox 스택 |
| `Divider` | 구분선 |

### Navigation Components (5개)
| Component | Description |
|-----------|-------------|
| `Breadcrumb` | 브레드크럼 |
| `Tabs` | 탭 네비게이션 |
| `Link` | 링크 |
| `Dropdown` | 드롭다운 메뉴 |
| `Pagination` | 페이지네이션 |

### Data Display Components (5개)
| Component | Description |
|-----------|-------------|
| `Badge` | 뱃지 |
| `Tag` | 태그 |
| `Avatar` | 아바타 |
| `Table` | 테이블 |
| `Tooltip` | 툴팁 |

### Overlay Components (1개)
| Component | Description |
|-----------|-------------|
| `Popover` | 팝오버 |

## Hooks

| Hook | Description |
|------|-------------|
| `useTheme` | 테마 전환 |
| `useToast` | 토스트 알림 |

## 문서 구조

### Architecture
- [system-overview.md](./architecture/system-overview.md) - 시스템 아키텍처

### API Reference
- [components-button.md](./api/components-button.md) - 버튼 컴포넌트
- [components-input.md](./api/components-input.md) - 입력 컴포넌트
- [components-feedback.md](./api/components-feedback.md) - 피드백 컴포넌트
- [hooks.md](./api/hooks.md) - React Hooks

### Guides
- [getting-started.md](./guides/getting-started.md) - 시작 가이드
- [theming.md](./guides/theming.md) - 테마 가이드
- [storybook.md](./guides/storybook.md) - Storybook 가이드

## Vue와의 차이점

| 기능 | Vue | React |
|------|-----|-------|
| v-model | 지원 | `value` + `onChange` |
| ref 전달 | `ref` prop | `forwardRef` |
| 이벤트 | `@click` | `onClick` |
| 조건부 렌더링 | `v-if` | `{condition && ...}` |
| 리스트 렌더링 | `v-for` | `.map()` |

## 관련 패키지

- [@portal/design-tokens](../design-tokens) - 디자인 토큰
- [@portal/design-types](../design-types) - 타입 정의
- [@portal/design-system-vue](../design-system-vue) - Vue 버전
