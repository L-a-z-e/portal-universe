---
id: api-portal-shell-api-client
title: Portal Shell API Client
type: api
status: current
version: v2
created: 2026-01-18
updated: 2026-02-06
author: Documenter Agent
tags: [api, portal-shell, axios, module-federation, rate-limit, token-refresh]
related:
  - api-portal-shell-auth-store
  - api-portal-shell-api-utils
---

# Portal Shell API Client

> Module Federation을 통해 Remote 모듈에 제공되는 Axios 인스턴스

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/api` |
| **Export 이름** | `apiClient` |
| **Base URL** | `VITE_API_BASE_URL` (환경변수) |
| **Timeout** | 10000ms (10초) |
| **인증** | Bearer Token 자동 주입 및 갱신 |

---

## 🎯 주요 기능

### 1. 자동 인증 토큰 주입 및 갱신
- Request Interceptor를 통해 authService의 accessToken 자동 주입
- 토큰 만료 시 자동으로 `autoRefreshIfNeeded()` 호출
- Authorization Header: `Bearer {token}`

### 2. 401 응답 자동 처리
- Response Interceptor가 401 응답 감지
- 자동으로 토큰 refresh 시도 (1회)
- Refresh 성공 시 원래 요청 재시도
- Refresh 실패 시 로그아웃 처리 및 `/?login=required`로 리다이렉트

### 3. 429 Rate Limit 재시도
- 429 응답 시 자동으로 재시도 (최대 3회)
- `Retry-After` 헤더가 있으면 해당 시간만큼 대기
- 없으면 기본 1초 대기

### 4. Backend 에러 메시지 파싱
- ApiErrorResponse 구조를 파싱하여 `error.errorDetails`에 저장
- `error.message`와 `error.code`를 Backend 에러로 오버라이드

### 5. 공통 설정
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
import { apiClient } from 'portal/api';
```

> ⚠️ **주의**: `default export`가 아닌 **named export**입니다. `{ apiClient }` 형태로 import해야 합니다.

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
import { apiClient, getErrorMessage, getErrorCode } from 'portal/api';

try {
  const response = await apiClient.get('/api/v1/blog/posts');
  console.log(response.data);
} catch (error) {
  console.error('에러 발생:', getErrorMessage(error));

  const code = getErrorCode(error);
  if (code === 'B001') {
    console.error('게시물을 찾을 수 없습니다.');
  }
}
```

### 401 응답 (자동 처리)

```typescript
// 401 응답 시 자동으로 토큰 refresh 시도 → 성공 시 재시도
// Refresh 실패 시에만 로그아웃 처리
// Remote 모듈에서 별도 처리 불필요

await apiClient.get('/api/v1/blog/posts');
// 401 응답 → authService.refresh() 시도
//   성공: 원래 요청 재시도
//   실패: authService.clearTokens() + 리다이렉트 /?login=required
```

### 429 Rate Limit (자동 재시도)

```typescript
// 429 응답 시 자동으로 재시도 (최대 3회)
// Retry-After 헤더가 있으면 해당 시간만큼 대기
await apiClient.get('/api/v1/expensive-operation');
// 429 → 1초 대기 → 재시도
// 429 → 1초 대기 → 재시도
// 429 → 1초 대기 → 재시도
// 429 → 에러 throw
```

### Backend 에러 메시지

```typescript
// Backend에서 반환한 에러 메시지는 자동으로 파싱됨
try {
  await apiClient.post('/api/v1/blog/posts', invalidData);
} catch (error) {
  console.error(error.message);  // Backend 에러 메시지
  console.error((error as any).code);  // Backend 에러 코드
  console.error((error as any).errorDetails);  // 전체 ErrorDetails
}
```

---

## 🔹 사용 예시

### 완전한 API 클라이언트 모듈

```typescript
// blog-frontend/src/api/blogApi.ts
import apiClient from 'portal/api';
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
import apiClient from 'portal/api';
```

**이유**: Shell의 apiClient를 사용해야 인증 토큰이 자동으로 주입됨

### 2. 401/429 에러 처리 중복 금지

```typescript
// ❌ 나쁜 예: Remote에서 401/429 에러 직접 처리
apiClient.get('/api/v1/posts').catch(error => {
  if (error.response?.status === 401) {
    // logout 등의 처리 (중복!)
  }
  if (error.response?.status === 429) {
    // retry 처리 (중복!)
  }
});

// ✅ 좋은 예: Interceptor에 맡기기
apiClient.get('/api/v1/posts').catch(error => {
  // 401, 429는 자동 처리되므로 다른 에러만 처리
  if (error.response?.status === 404) {
    console.error('Not Found');
  }
});
```

**이유**: 401 토큰 refresh와 429 재시도는 Interceptor가 자동으로 처리함

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

**최종 업데이트**: 2026-02-06

---

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | 2026-01-18 | 최초 작성 |
| v2 | 2026-02-06 | 429 재시도 추가, 401 토큰 refresh 추가, Import 경로 수정, API Utils 추가 |
