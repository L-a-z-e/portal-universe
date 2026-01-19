---
id: api-portal-shell-api-client
title: Portal Shell API Client
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [api, portal-shell, axios, module-federation]
related:
  - api-portal-shell-auth-store
---

# Portal Shell API Client

> Module Federation을 통해 Remote 모듈에 제공되는 Axios 인스턴스

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal-shell/apiClient` |
| **Base URL** | `VITE_API_BASE_URL` (환경변수) |
| **Timeout** | 10000ms (10초) |
| **인증** | Bearer Token 자동 주입 |

---

## 🎯 주요 기능

### 1. 자동 인증 토큰 주입
- Request Interceptor를 통해 authStore의 accessToken 자동 주입
- Authorization Header: `Bearer {token}`

### 2. 401 응답 자동 처리
- Response Interceptor가 401 응답 감지
- 자동으로 authStore.logout() 호출

### 3. 공통 설정
- Content-Type: application/json
- Timeout: 10000ms

---

## 📦 타입 정의

```typescript
import type { AxiosInstance } from 'axios';

// Axios 인스턴스
const apiClient: AxiosInstance;
```

---

## 🔹 Remote 모듈에서 사용하기

### 1. Import

```typescript
// blog-frontend/src/api/blogApi.ts
import apiClient from 'portal-shell/apiClient';
```

### 2. GET 요청

```typescript
// 블로그 게시물 목록 조회
export const getPosts = async (page: number = 0, size: number = 20) => {
  const response = await apiClient.get('/api/v1/blog/posts', {
    params: { page, size }
  });
  return response.data;
};
```

### 3. POST 요청

```typescript
// 블로그 게시물 생성
export const createPost = async (data: CreatePostRequest) => {
  const response = await apiClient.post('/api/v1/blog/posts', data);
  return response.data;
};
```

### 4. PUT 요청

```typescript
// 블로그 게시물 수정
export const updatePost = async (id: string, data: UpdatePostRequest) => {
  const response = await apiClient.put(`/api/v1/blog/posts/${id}`, data);
  return response.data;
};
```

### 5. DELETE 요청

```typescript
// 블로그 게시물 삭제
export const deletePost = async (id: string) => {
  const response = await apiClient.delete(`/api/v1/blog/posts/${id}`);
  return response.data;
};
```

---

## 🔹 에러 처리

### 기본 에러 처리

```typescript
import apiClient from 'portal-shell/apiClient';

try {
  const response = await apiClient.get('/api/v1/blog/posts');
  console.log(response.data);
} catch (error) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) {
      console.error('게시물을 찾을 수 없습니다.');
    } else if (error.response?.status === 500) {
      console.error('서버 오류가 발생했습니다.');
    } else {
      console.error('요청 실패:', error.message);
    }
  }
}
```

### 401 응답 (자동 처리)

```typescript
// 401 응답 시 자동으로 logout() 호출됨
// Remote 모듈에서 별도 처리 불필요
await apiClient.get('/api/v1/blog/posts');
// 401 응답 → Response Interceptor가 authStore.logout() 호출
```

---

## 🔹 사용 예시

### 완전한 API 클라이언트 모듈

```typescript
// blog-frontend/src/api/blogApi.ts
import apiClient from 'portal-shell/apiClient';
import type { AxiosError } from 'axios';

export interface Post {
  id: string;
  title: string;
  content: string;
  author: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  tags?: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// 게시물 목록 조회
export const getPosts = async (
  page: number = 0,
  size: number = 20
): Promise<ApiResponse<PageResponse<Post>>> => {
  const response = await apiClient.get('/api/v1/blog/posts', {
    params: { page, size }
  });
  return response.data;
};

// 게시물 상세 조회
export const getPost = async (id: string): Promise<ApiResponse<Post>> => {
  const response = await apiClient.get(`/api/v1/blog/posts/${id}`);
  return response.data;
};

// 게시물 생성
export const createPost = async (
  data: CreatePostRequest
): Promise<ApiResponse<Post>> => {
  const response = await apiClient.post('/api/v1/blog/posts', data);
  return response.data;
};

// 게시물 수정
export const updatePost = async (
  id: string,
  data: Partial<CreatePostRequest>
): Promise<ApiResponse<Post>> => {
  const response = await apiClient.put(`/api/v1/blog/posts/${id}`, data);
  return response.data;
};

// 게시물 삭제
export const deletePost = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/v1/blog/posts/${id}`);
};

// 에러 처리 헬퍼
export const handleApiError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiResponse<null>>;
    return axiosError.response?.data?.message || '알 수 없는 오류가 발생했습니다.';
  }
  return '네트워크 오류가 발생했습니다.';
};
```

---

## ⚙️ 설정

### 환경변수

```env
# .env
VITE_API_BASE_URL=http://localhost:8080
```

### 개발 환경별 URL

| 환경 | Base URL |
|------|----------|
| Local | `http://localhost:8080` |
| Docker | `http://api-gateway:8080` |
| Kubernetes | `http://api-gateway-service:8080` |

---

## ⚠️ 주의사항

### 1. Remote 모듈에서 새로운 Axios 인스턴스 생성 금지

```typescript
// ❌ 나쁜 예: Remote에서 독자적인 axios 인스턴스 생성
import axios from 'axios';
const myClient = axios.create({ baseURL: '...' });

// ✅ 좋은 예: Shell의 apiClient 사용
import apiClient from 'portal-shell/apiClient';
```

**이유**: Shell의 apiClient를 사용해야 인증 토큰이 자동으로 주입됨

### 2. 401 에러 처리 중복 금지

```typescript
// ❌ 나쁜 예: Remote에서 401 에러 직접 처리
apiClient.get('/api/v1/posts').catch(error => {
  if (error.response?.status === 401) {
    // logout 등의 처리 (중복!)
  }
});

// ✅ 좋은 예: Interceptor에 맡기기
apiClient.get('/api/v1/posts').catch(error => {
  // 401은 자동 처리되므로 다른 에러만 처리
  if (error.response?.status === 404) {
    console.error('Not Found');
  }
});
```

### 3. Timeout 조정 필요 시

```typescript
// 특정 요청에만 timeout 조정
await apiClient.get('/api/v1/large-data', {
  timeout: 30000 // 30초
});
```

---

## 🔗 관련 문서

- [Auth Store API](./auth-store.md) - 인증 상태 관리
- [Theme Store API](./theme-store.md) - 테마 상태 관리

---

**최종 업데이트**: 2026-01-18
