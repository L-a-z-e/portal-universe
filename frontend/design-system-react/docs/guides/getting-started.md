---
id: getting-started-react
title: Getting Started
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, react, getting-started]
related:
  - system-overview-react
  - theming-react
---

# Getting Started

Design System React를 프로젝트에 설정하고 사용하는 방법을 안내합니다.

## 요구사항

- React 18+
- TypeScript 5.0+ (권장)
- Node.js 18+

## 설치

### 패키지 설치

```bash
# npm
npm install @portal/design-system-react @portal/design-tokens @portal/design-types

# pnpm
pnpm add @portal/design-system-react @portal/design-tokens @portal/design-types

# yarn
yarn add @portal/design-system-react @portal/design-tokens @portal/design-types
```

### Tailwind CSS (선택)

Tailwind를 사용하는 경우:

```bash
npm install -D tailwindcss postcss autoprefixer
```

```js
// tailwind.config.js
module.exports = {
  presets: [require('@portal/design-tokens/tailwind.preset')],
  content: [
    './src/**/*.{tsx,ts,jsx,js}',
    './node_modules/@portal/design-system-react/**/*.{js,jsx}',
  ],
};
```

## 설정

### 1. 스타일 임포트

```tsx
// main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// 스타일 임포트 (순서 중요)
import '@portal/design-tokens/dist/tokens.css';
import '@portal/design-system-react/dist/style.css';
import './index.css'; // 프로젝트 스타일

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

### 2. 테마 설정

```tsx
// App.tsx
import { useEffect } from 'react';
import { ToastProvider, ToastContainer } from '@portal/design-system-react';

function App() {
  useEffect(() => {
    // 기본 테마 설정
    document.documentElement.setAttribute('data-service', 'portal');
    document.documentElement.setAttribute('data-theme', 'dark');
  }, []);

  return (
    <ToastProvider>
      <YourApp />
      <ToastContainer position="top-right" />
    </ToastProvider>
  );
}

export default App;
```

### 3. TypeScript 설정 (선택)

```json
// tsconfig.json
{
  "compilerOptions": {
    "types": ["@portal/design-types"]
  }
}
```

## 컴포넌트 사용

### 개별 임포트 (권장)

트리 쉐이킹을 위해 필요한 컴포넌트만 임포트합니다:

```tsx
import { Button, Input, Card } from '@portal/design-system-react';

function MyComponent() {
  return (
    <Card padding="lg">
      <Input placeholder="Enter text" />
      <Button>Submit</Button>
    </Card>
  );
}
```

## 기본 예제

### 로그인 폼

```tsx
import { useState } from 'react';
import {
  Card,
  Input,
  Checkbox,
  Button,
  Alert,
  useToast,
} from '@portal/design-system-react';

function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const toast = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(email, password);
      toast.success('로그인 성공!');
    } catch (e) {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card padding="lg" className="max-w-md mx-auto">
      <h2 className="text-xl font-semibold mb-6">로그인</h2>

      {error && (
        <Alert variant="error" dismissible onDismiss={() => setError('')}>
          {error}
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="space-y-4 mt-4">
        <Input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          type="email"
          label="이메일"
          placeholder="email@example.com"
          required
        />

        <Input
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          type="password"
          label="비밀번호"
          placeholder="••••••••"
          required
        />

        <Checkbox
          checked={rememberMe}
          onChange={(e) => setRememberMe(e.target.checked)}
          label="로그인 상태 유지"
        />

        <Button type="submit" fullWidth loading={isLoading}>
          로그인
        </Button>
      </form>
    </Card>
  );
}
```

### 데이터 테이블

```tsx
import { useState, useEffect, useMemo } from 'react';
import {
  Card,
  SearchBar,
  Button,
  Skeleton,
  Badge,
  Dropdown,
} from '@portal/design-system-react';

interface User {
  id: string;
  name: string;
  email: string;
  active: boolean;
}

function UserTable() {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchUsers().then((data) => {
      setUsers(data);
      setIsLoading(false);
    });
  }, []);

  const filteredUsers = useMemo(
    () =>
      users.filter((u) =>
        u.name.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    [users, searchQuery]
  );

  const handleAction = (action: string, user: User) => {
    if (action === 'edit') {
      // 편집 로직
    } else if (action === 'delete') {
      // 삭제 로직
    }
  };

  return (
    <Card>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">사용자 목록</h2>
        <Button size="sm">추가</Button>
      </div>

      <SearchBar
        value={searchQuery}
        onChange={setSearchQuery}
        placeholder="사용자 검색..."
        className="mb-4"
      />

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} height="48px" />
          ))}
        </div>
      ) : (
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-default">
              <th className="text-left py-3">이름</th>
              <th className="text-left py-3">이메일</th>
              <th className="text-left py-3">상태</th>
              <th className="text-right py-3">작업</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map((user) => (
              <tr key={user.id} className="border-b border-border-muted">
                <td className="py-3">{user.name}</td>
                <td className="py-3">{user.email}</td>
                <td className="py-3">
                  <Badge variant={user.active ? 'success' : 'default'}>
                    {user.active ? '활성' : '비활성'}
                  </Badge>
                </td>
                <td className="py-3 text-right">
                  <Dropdown
                    items={[
                      { label: '편집', value: 'edit' },
                      { label: '삭제', value: 'delete' },
                    ]}
                    onSelect={(item) => handleAction(item.value, user)}
                  >
                    <Button variant="ghost" size="sm">
                      ⋮
                    </Button>
                  </Dropdown>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  );
}
```

## Next.js 설정

### App Router

```tsx
// app/layout.tsx
import '@portal/design-tokens/dist/tokens.css';
import '@portal/design-system-react/dist/style.css';

import { Providers } from './providers';

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body data-service="portal" data-theme="dark">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
```

```tsx
// app/providers.tsx
'use client';

import { ToastProvider, ToastContainer } from '@portal/design-system-react';

export function Providers({ children }) {
  return (
    <ToastProvider>
      {children}
      <ToastContainer position="top-right" />
    </ToastProvider>
  );
}
```

## 다음 단계

- [System Overview](../architecture/system-overview.md) - 아키텍처 이해
- [Theming Guide](./theming.md) - 테마 커스터마이징
- [Storybook Guide](./storybook.md) - Storybook 사용
- [API Reference](../api/components-button.md) - 컴포넌트 API
