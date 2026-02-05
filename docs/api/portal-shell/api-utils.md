---
id: api-portal-shell-api-utils
title: Portal Shell API Utils
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Documenter Agent
tags: [api, portal-shell, utils, error-handling, module-federation]
related:
  - api-portal-shell-api-client
---

# Portal Shell API Utils

> API 응답 및 에러 처리 유틸리티 함수

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/api` |
| **Export 함수** | `getData`, `getErrorDetails`, `getErrorMessage`, `getErrorCode` |
| **주요 용도** | ApiResponse 파싱, 에러 처리 간소화 |

---

## 🎯 주요 기능

### 1. ApiResponse data 추출
- `getData<T>` - AxiosResponse<ApiResponse<T>>에서 T 추출

### 2. Backend 에러 정보 추출
- `getErrorDetails` - Axios 에러에서 ErrorDetails 추출
- `getErrorMessage` - 사용자 친화적 에러 메시지 반환
- `getErrorCode` - Backend 에러 코드 추출

---

## 📦 타입 정의

### ApiResponse

```typescript
interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}
```

### ErrorDetails

```typescript
interface ErrorDetails {
  code: string;          // 에러 코드 (예: "B001")
  message: string;       // 에러 메시지
  timestamp: string;     // 발생 시각
  path?: string;         // 요청 경로
  fields?: FieldError[]; // 필드별 에러 (유효성 검사)
}
```

### FieldError

```typescript
interface FieldError {
  field: string;         // 필드 이름
  rejectedValue: any;    // 거부된 값
  message: string;       // 에러 메시지
}
```

---

## 🔹 함수 상세

### getData

```typescript
function getData<T>(response: AxiosResponse<ApiResponse<T>>): T
```

AxiosResponse에서 data를 추출합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `response` | `AxiosResponse<ApiResponse<T>>` | ✅ | Axios 응답 객체 |

**Returns:** `T` - ApiResponse의 data 필드

**예시:**

```typescript
import { apiClient, getData } from 'portal/api';

// Before
const response = await apiClient.get<ApiResponse<Post[]>>('/api/v1/blog/posts');
const posts = response.data.data;  // .data.data 중복

// After
const response = await apiClient.get<ApiResponse<Post[]>>('/api/v1/blog/posts');
const posts = getData(response);  // 깔끔!
```

---

### getErrorDetails

```typescript
function getErrorDetails(error: unknown): ErrorDetails | null
```

Axios 에러에서 Backend 에러 정보를 추출합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `error` | `unknown` | ✅ | catch된 에러 객체 |

**Returns:** `ErrorDetails | null` - Backend 에러 정보 (없으면 null)

**예시:**

```typescript
import { apiClient, getErrorDetails } from 'portal/api';

try {
  await apiClient.post('/api/v1/blog/posts', invalidData);
} catch (error) {
  const details = getErrorDetails(error);

  if (details) {
    console.error('코드:', details.code);
    console.error('메시지:', details.message);
    console.error('필드 에러:', details.fields);
  }
}
```

---

### getErrorMessage

```typescript
function getErrorMessage(error: unknown): string
```

사용자 친화적 에러 메시지를 반환합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `error` | `unknown` | ✅ | catch된 에러 객체 |

**Returns:** `string` - 사용자에게 보여줄 에러 메시지

**우선순위:**
1. Backend ErrorDetails.message
2. Error 객체의 message
3. 기본 메시지: "알 수 없는 오류가 발생했습니다."

**예시:**

```typescript
import { apiClient, getErrorMessage } from 'portal/api';

try {
  await apiClient.post('/api/v1/blog/posts', data);
} catch (error) {
  alert(getErrorMessage(error));  // 사용자에게 표시
}
```

---

### getErrorCode

```typescript
function getErrorCode(error: unknown): string | null
```

Backend 에러 코드를 추출합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `error` | `unknown` | ✅ | catch된 에러 객체 |

**Returns:** `string | null` - 에러 코드 (없으면 null)

**예시:**

```typescript
import { apiClient, getErrorCode } from 'portal/api';

try {
  await apiClient.get(`/api/v1/blog/posts/${id}`);
} catch (error) {
  const code = getErrorCode(error);

  if (code === 'B001') {
    console.error('게시물을 찾을 수 없습니다.');
  } else if (code === 'B002') {
    console.error('접근 권한이 없습니다.');
  } else {
    console.error('알 수 없는 오류:', code);
  }
}
```

---

## 🔹 사용 예시

### 1. API 호출 + 에러 처리

```typescript
import { apiClient, getData, getErrorMessage, getErrorCode } from 'portal/api';

async function fetchPosts() {
  try {
    const response = await apiClient.get<ApiResponse<Post[]>>('/api/v1/blog/posts');
    const posts = getData(response);
    return posts;
  } catch (error) {
    console.error('에러 발생:', getErrorMessage(error));

    const code = getErrorCode(error);
    if (code === 'AUTH001') {
      // 인증 에러 처리
    }

    throw error;
  }
}
```

---

### 2. Vue Composable에서 사용

```typescript
// blog-frontend/src/composables/usePosts.ts
import { ref } from 'vue';
import { apiClient, getData, getErrorMessage } from 'portal/api';
import type { ApiResponse, Post } from '@/types';

export function usePosts() {
  const posts = ref<Post[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const fetchPosts = async () => {
    loading.value = true;
    error.value = null;

    try {
      const response = await apiClient.get<ApiResponse<Post[]>>('/api/v1/blog/posts');
      posts.value = getData(response);
    } catch (e) {
      error.value = getErrorMessage(e);
    } finally {
      loading.value = false;
    }
  };

  return { posts, loading, error, fetchPosts };
}
```

---

### 3. 필드 에러 표시 (Form Validation)

```typescript
import { apiClient, getErrorDetails } from 'portal/api';

async function submitForm(data: FormData) {
  try {
    await apiClient.post('/api/v1/users', data);
  } catch (error) {
    const details = getErrorDetails(error);

    if (details?.fields) {
      // 필드별 에러 표시
      details.fields.forEach(fieldError => {
        console.error(`${fieldError.field}: ${fieldError.message}`);
        // UI에 에러 표시 로직
      });
    }
  }
}
```

---

### 4. Toast 알림과 함께 사용

```typescript
import { apiClient, getData, getErrorMessage } from 'portal/api';
import { toast } from '@/utils/toast';

async function deletePost(id: string) {
  try {
    await apiClient.delete(`/api/v1/blog/posts/${id}`);
    toast.success('게시물이 삭제되었습니다.');
  } catch (error) {
    toast.error(getErrorMessage(error));
  }
}
```

---

## ⚠️ 주의사항

### 1. apiClient Interceptor와 연동

```typescript
// apiClient의 Response Interceptor가 자동으로 errorDetails를 주입함
// 따라서 getErrorDetails()는 항상 최신 Backend 에러 정보를 반환

// apiClient.ts (내부 동작)
apiClient.interceptors.response.use(
  response => response,
  error => {
    const backendError = error.response?.data?.error;
    if (backendError) {
      error.errorDetails = backendError;  // 주입!
    }
    return Promise.reject(error);
  }
);
```

### 2. TypeScript 제네릭 활용

```typescript
// ✅ 좋은 예: 타입 안전성 확보
const response = await apiClient.get<ApiResponse<Post[]>>('/api/v1/blog/posts');
const posts = getData(response);  // posts는 Post[] 타입

// ❌ 나쁜 예: 타입 안전성 상실
const response = await apiClient.get('/api/v1/blog/posts');
const posts = response.data.data;  // posts는 any 타입
```

### 3. null 체크

```typescript
// getErrorDetails는 null을 반환할 수 있음
const details = getErrorDetails(error);
if (details) {
  console.error(details.code);
}

// Optional chaining 사용
console.error(getErrorDetails(error)?.code);
```

---

## 🔗 관련 문서

- [API Client](./api-client.md) - HTTP 요청 클라이언트
- [Auth Store API](./auth-store.md) - 인증 상태 관리

---

**최종 업데이트**: 2026-02-06
