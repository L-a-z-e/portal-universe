# React Error #31: Module Federation과 React Query 호환성 문제

**작성일**: 2026-01-17
**상태**: 해결됨
**영향 범위**: shopping-frontend (React Remote Module)

---

## 문제 현상

### 증상
- `/shopping` 페이지 접근 시 React Error #31 발생
- Standalone 모드(localhost:30002)에서도 동일 에러 발생
- 페이지가 빈 화면으로 렌더링됨

### 에러 메시지
```
Objects are not valid as a React child (found: object with keys {$$typeof, type, key, ref, props})
```

---

## 원인 분석

### 초기 잘못된 추정
- React Router v7 + React.lazy() 호환성 문제로 의심
- route lazy 속성으로 변경 시도했으나 동일 에러 발생

### 실제 근본 원인

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

---

## 디버깅 과정

### 1단계: 라우팅 문제 추정
```bash
# React Router v7의 route lazy 속성 시도
# → 실패: 동일한 Error #31 발생
```

### 2단계: 최소화 테스트 진행
```jsx
// App 컴포넌트를 단순 JSX로 축소
function App() {
  return <div>Simple App</div>;
}
// → 성공: 정상 렌더링
```

### 3단계: 단계별 추가하며 테스트
```jsx
// 1. 라우터 추가 → 정상
// 2. 페이지 컴포넌트 추가 → 정상
// 3. QueryClientProvider 추가 → Error #31 발생!
```

### 4단계: 원인 특정
- React Query를 shared dependencies로 설정해도 동일 에러
- Context API 기반 라이브러리 모두 의심
- QueryClientProvider 제거하면 완전히 정상 작동

---

## 해결 방법

### 적용된 솔루션

**QueryClientProvider 제거 및 기존 패턴 유지**

#### 수정된 파일
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

### API 호출 패턴 변경

**React Query 제거 후 기존 패턴으로 변경**

```jsx
// ProductListPage.tsx 패턴 (이미 사용 중)
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

  if (loading) return <div>로딩 중...</div>;
  if (error) return <div>에러 발생: {error.message}</div>;

  return (
    <div>
      {products.map(product => (
        <div key={product.id}>{product.name}</div>
      ))}
    </div>
  );
}
```

### 영향받는 파일 목록

| 파일 경로 | 수정 내용 |
|---------|---------|
| `src/App.tsx` | QueryClientProvider 제거 |
| `src/pages/admin/ProductListPage.tsx` | useState + useEffect 사용 (이미 적용) |
| `src/pages/admin/ProductDetailPage.tsx` | 필요 시 동일 패턴 적용 |
| `src/pages/admin/AdminDashboard.tsx` | 필요 시 동일 패턴 적용 |

---

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

---

## 향후 대안

Module Federation 환경에서 React Query를 사용해야 하는 경우, 다음 대안을 고려하세요:

### 대안 1: Host에서 QueryClientProvider 제공 (권장)
```jsx
// portal-shell (Host)
export const QueryClientProvider = QueryClientProvider;
export const queryClient = queryClient;

// shopping-frontend (Remote) - bootstrap.tsx
import { QueryClientProvider, queryClient } from 'portal-shell/context';

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
```

**장점:**
- 전체 애플리케이션에서 단일 QueryClient 인스턴스 사용
- Context 불일치 해결
- 더 나은 캐시 일관성

**단점:**
- Host에 의존성 증가
- Remote의 독립적 실행이 어려워짐

### 대안 2: QueryClient를 Shared Library로 공유
```javascript
// vite.config.ts (host & remote)
federation({
  shared: {
    '@tanstack/react-query': {
      singleton: true,
      requiredVersion: '^5.90.18'
    }
  }
})
```

**장점:**
- 선택적으로 공유 가능
- 이전 호환성 설정 가능

**단점:**
- 여전히 Context 문제 가능성
- 추가 설정 복잡도

### 대안 3: 다른 데이터 페칭 라이브러리 검토

| 라이브러리 | 특징 | Module Federation 호환성 |
|----------|------|------------------------|
| SWR | 가벼움, React Hooks 기반 | ★★★★☆ (권장) |
| TanStack Query | 강력함, Context 기반 | ★★☆☆☆ (현재 문제) |
| RTK Query | Redux 통합, 안정적 | ★★★★★ (Redux 사용 시) |
| Apollo Client | GraphQL 특화 | ★★★★☆ |

---

## 검증 방법

### 로컬 테스트

#### 1. Standalone 모드 확인
```bash
cd frontend/shopping-frontend
npm run dev
# http://localhost:30002 접근 → 정상 렌더링 확인
```

#### 2. Module Federation 모드 확인
```bash
cd frontend
npm run dev
# portal-shell에서 shopping 모듈 로드 → Error #31 없음 확인
```

#### 3. 네트워크 요청 확인
```javascript
// 브라우저 개발자 도구 - Network 탭
// /api/v1/shopping/products 요청 성공 확인
// 응답 데이터가 UI에 정상 렌더링되는지 확인
```

---

## 학습 포인트

### Module Federation의 특수성
1. **React 인스턴스 분리**: Host와 Remote가 각각 독립적인 React 인스턴스를 로드할 수 있음
2. **Context 호환성**: Context API는 동일한 React 인스턴스 내에서만 작동
3. **라이브러리 공유**: 반드시 shared 설정으로 공유해야 하는 라이브러리가 있음 (React, React DOM 등)

### React Query와 Module Federation
- React Query v5+는 Context API 강의존도
- Module Federation 환경에서는 singleton 패턴이나 Host 제공 방식 권장
- 간단한 데이터 페칭은 기본 useState + useEffect로 충분

### 디버깅 팁
1. **최소 단위로 축소**: 컴포넌트를 JSX만 남길 때까지 단순화
2. **단계적 추가**: 한 번에 하나씩 요소 추가하며 테스트
3. **외부 라이브러리 의심**: 라이브러리 제거 후 복구 시도
4. **Context API 의심**: Error #31이 발생하면 Context 사용 라이브러리 확인

---

## 참고 링크

- [React Error Decoder](https://react.dev/errors)
- [React Query + Module Federation](https://tanstack.com/query/latest/docs/react/community/tkdodo-blog)
- [Module Federation Shared Dependencies](https://webpack.js.org/concepts/module-federation/)
- [Origins vite-plugin-federation](https://github.com/originjs/vite-plugin-federation)

---

## 관련 이슈

- GitHub Issue #31: React Shopping Module Error (해결됨)
- 관련 PR: shopping-frontend Module Federation 호환성 수정

---

**마지막 업데이트**: 2026-01-17
**담당자**: Frontend Team
**상태**: 해결 완료 및 검증됨
