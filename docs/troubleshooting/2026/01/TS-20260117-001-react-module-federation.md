---
id: TS-20260117-001
title: React Error #31 - Module Federation과 React Query 호환성 문제
type: troubleshooting
status: resolved
created: 2026-01-17
updated: 2026-01-17
author: Frontend Team
severity: high
resolved: true
affected_services: [shopping-frontend]
tags: [react, module-federation, react-query, context-api]
---

# React Error #31: Module Federation과 React Query 호환성 문제

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-17 |
| **해결일** | 2026-01-17 |
| **영향 서비스** | shopping-frontend (React Remote Module) |

## 증상 (Symptoms)

### 현상
- `/shopping` 페이지 접근 시 React Error #31 발생
- Standalone 모드(localhost:30002)에서도 동일 에러 발생
- 페이지가 빈 화면으로 렌더링됨

### 에러 메시지
```
Objects are not valid as a React child (found: object with keys {$$typeof, type, key, ref, props})
```

## 원인 분석 (Root Cause)

### 초기 추정
- React Router v7 + React.lazy() 호환성 문제로 의심
- route lazy 속성으로 변경 시도했으나 동일 에러 발생

### 실제 원인

**@tanstack/react-query의 QueryClientProvider가 Module Federation 환경에서 React Error #31 발생**

#### 기술적 분석
- React Query v5.90.18과 Module Federation(@originjs/vite-plugin-federation) 사이의 호환성 문제
- QueryClientProvider는 내부적으로 React Context를 사용하는데, Module Federation 환경에서 React 인스턴스 중복 로드로 인한 Context 호환성 문제 발생
- QueryClientProvider 없이 렌더링하면 정상 작동 확인됨

#### 발생 메커니즘
```javascript
// 문제 상황:
// 1. Host(portal-shell)와 Remote(shopping-frontend)가 별도의 React 인스턴스 로드
// 2. QueryClientProvider가 Context API 사용
// 3. Context 불일치로 인한 유효하지 않은 React 엘리먼트 참조
// 4. Error #31: Objects are not valid as a React child
```

### 분석 과정

#### 1단계: 라우팅 문제 추정
```bash
# React Router v7의 route lazy 속성 시도
# → 실패: 동일한 Error #31 발생
```

#### 2단계: 최소화 테스트 진행
```jsx
// App 컴포넌트를 단순 JSX로 축소
function App() {
  return <div>Simple App</div>;
}
// → 성공: 정상 렌더링
```

#### 3단계: 단계별 추가하며 테스트
```jsx
// 1. 라우터 추가 → 정상
// 2. 페이지 컴포넌트 추가 → 정상
// 3. QueryClientProvider 추가 → Error #31 발생!
```

#### 4단계: 원인 특정
- React Query를 shared dependencies로 설정해도 동일 에러
- Context API 기반 라이브러리 모두 의심
- QueryClientProvider 제거하면 완전히 정상 작동

## 해결 방법 (Solution)

### 즉시 조치 (Immediate Fix)

**QueryClientProvider 제거 및 기존 패턴 유지**

`frontend/shopping-frontend/src/App.tsx`

**Before (문제 코드)**
```jsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
```

**After (해결 코드)**
```jsx
import { RouterProvider } from 'react-router-dom';
import router from './router';

function App() {
  return <RouterProvider router={router} />;
}
```

### 영구 조치 (Permanent Fix)

API 호출을 useState + useEffect 기본 패턴으로 변경:

```jsx
import { useState, useEffect } from 'react';
import { apiClient } from '@/api/client';

function ProductListPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await apiClient.get('/api/v1/shopping/products');
        setProducts(response.data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
  }, []);

  // ...
}
```

### 수정된 파일

| 파일 경로 | 수정 내용 |
|----------|----------|
| `src/App.tsx` | QueryClientProvider 제거 |
| `src/pages/admin/ProductListPage.tsx` | useState + useEffect 사용 (이미 적용) |
| `src/pages/admin/ProductDetailPage.tsx` | 필요 시 동일 패턴 적용 |
| `src/pages/admin/AdminDashboard.tsx` | 필요 시 동일 패턴 적용 |

## 재발 방지 (Prevention)

### 프로세스 개선
- Module Federation 환경에서 Context API 기반 라이브러리 사용 시 주의
- 새 라이브러리 도입 전 Module Federation 호환성 테스트

### 향후 대안

1. **Host에서 QueryClientProvider 제공 (권장)** - Host 의존성 증가하지만 Context 일관성 보장
2. **Shared Library로 공유** - singleton 설정 필요
3. **다른 라이브러리 검토** - SWR, RTK Query 등

## 학습 포인트

### Module Federation의 특수성
1. **React 인스턴스 분리**: Host와 Remote가 각각 독립적인 React 인스턴스를 로드할 수 있음
2. **Context 호환성**: Context API는 동일한 React 인스턴스 내에서만 작동
3. **라이브러리 공유**: 반드시 shared 설정으로 공유해야 하는 라이브러리가 있음 (React, React DOM 등)

### 디버깅 팁
1. **최소 단위로 축소**: 컴포넌트를 JSX만 남길 때까지 단순화
2. **단계적 추가**: 한 번에 하나씩 요소 추가하며 테스트
3. **외부 라이브러리 의심**: 라이브러리 제거 후 복구 시도
4. **Context API 의심**: Error #31이 발생하면 Context 사용 라이브러리 확인

## 환경 정보

```
프론트엔드 환경:
├─ React: 18.2.0
├─ React Router: 7.1.5
├─ @tanstack/react-query: 5.90.18 (제거됨)
├─ @originjs/vite-plugin-federation: (Module Federation)
└─ Vite: 7.1.12

배포 환경:
├─ Host: portal-shell (localhost:30000)
├─ Remote: shopping-frontend (localhost:30002)
└─ Design System: @portal/design-system (localhost:30003)
```

## 관련 링크

- [React Error Decoder](https://react.dev/errors)
- [React Query + Module Federation](https://tanstack.com/query/latest/docs/react/community/tkdodo-blog)
- [Module Federation Shared Dependencies](https://webpack.js.org/concepts/module-federation/)
- [Origins vite-plugin-federation](https://github.com/originjs/vite-plugin-federation)

## 관련 이슈

- GitHub Issue #31: React Shopping Module Error (해결됨)
- 관련 PR: shopping-frontend Module Federation 호환성 수정
