---
id: api-types
title: API Types Reference
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, typescript, api-types]
related:
  - component-types
  - theme-types
---

# API Types Reference

Backend `ApiResponse` 구조와 일치하는 공통 API 타입 정의입니다.

> Source: `frontend/design-types/src/api.ts`

## FieldError

Validation 에러 필드 상세 정보입니다. `@Valid` 어노테이션 기반 검증 실패 시 각 필드별 에러를 나타냅니다.

```ts
interface FieldError {
  /** 에러가 발생한 필드명 */
  field: string;
  /** 에러 메시지 */
  message: string;
  /** 거부된 값 (optional) */
  rejectedValue?: unknown;
}
```

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `field` | `string` | Yes | 에러가 발생한 필드명 (e.g. `"email"`) |
| `message` | `string` | Yes | 에러 메시지 (e.g. `"이메일 형식이 올바르지 않습니다"`) |
| `rejectedValue` | `unknown` | No | 거부된 입력값 |

## ErrorDetails

에러 상세 정보입니다. Backend `GlobalExceptionHandler`가 반환하는 에러 구조의 내부 객체입니다.

```ts
interface ErrorDetails {
  /** 에러 코드 (e.g. "AUTH_001", "VALIDATION_ERROR") */
  code: string;
  /** 에러 메시지 */
  message: string;
  /** 에러 발생 시각 (ISO 8601) */
  timestamp?: string;
  /** 요청 경로 */
  path?: string;
  /** Validation 에러 필드 목록 */
  details?: FieldError[];
}
```

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `code` | `string` | Yes | 서비스별 에러 코드 |
| `message` | `string` | Yes | 사람이 읽을 수 있는 에러 메시지 |
| `timestamp` | `string` | No | 에러 발생 시각 (ISO 8601) |
| `path` | `string` | No | 요청 경로 (e.g. `"/api/v1/users"`) |
| `details` | `FieldError[]` | No | Validation 에러 시 각 필드별 상세 |

## ApiResponse\<T\>

API 성공 응답 타입입니다. axios가 4xx/5xx를 reject하므로, resolve된 응답은 항상 성공입니다.

```ts
interface ApiResponse<T> {
  /** 항상 true */
  success: true;
  /** 응답 데이터 */
  data: T;
  /** 항상 null */
  error: null;
}
```

| Prop | Type | Value | Description |
|------|------|-------|-------------|
| `success` | `true` | 고정 | 성공 여부 (literal type) |
| `data` | `T` | - | 제네릭 응답 데이터 |
| `error` | `null` | 고정 | 성공 시 항상 null |

### Backend 대응

```java
// services/*/common/ApiResponse.java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetails error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
}
```

### 사용 예시

```ts
import type { ApiResponse } from '@portal/design-types';

interface User {
  id: number;
  name: string;
}

// API 호출 결과 타입
const response: ApiResponse<User> = {
  success: true,
  data: { id: 1, name: 'John' },
  error: null,
};

// axios 사용 시
const { data } = await apiClient.get<ApiResponse<User>>('/api/v1/users/1');
console.log(data.data.name); // 'John'
```

## ApiErrorResponse

API 에러 응답 타입입니다. axios reject 시 `error.response.data`에 해당합니다.

```ts
interface ApiErrorResponse {
  /** 항상 false */
  success: false;
  /** 항상 null */
  data: null;
  /** 에러 상세 정보 */
  error: ErrorDetails;
}
```

| Prop | Type | Value | Description |
|------|------|-------|-------------|
| `success` | `false` | 고정 | 실패 여부 (literal type) |
| `data` | `null` | 고정 | 에러 시 항상 null |
| `error` | `ErrorDetails` | - | 에러 상세 정보 |

### Backend 대응

```java
// services/*/exception/GlobalExceptionHandler.java
@ExceptionHandler(CustomBusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(
    CustomBusinessException e) {
    return ResponseEntity.status(e.getStatus())
        .body(ApiResponse.error(e.getCode(), e.getMessage()));
}
```

### 사용 예시

```ts
import type { ApiErrorResponse } from '@portal/design-types';
import { AxiosError } from 'axios';

try {
  await apiClient.post('/api/v1/users', userData);
} catch (err) {
  const axiosError = err as AxiosError<ApiErrorResponse>;
  const errorData = axiosError.response?.data;

  if (errorData?.error) {
    console.error(errorData.error.code);    // 'AUTH_001'
    console.error(errorData.error.message);  // '인증이 필요합니다'

    // Validation 에러 처리
    if (errorData.error.details) {
      errorData.error.details.forEach(({ field, message }) => {
        setFieldError(field, message);
      });
    }
  }
}
```

## 타입 관계도

```
ApiResponse<T>              ApiErrorResponse
├── success: true           ├── success: false
├── data: T                 ├── data: null
└── error: null             └── error: ErrorDetails
                                  ├── code: string
                                  ├── message: string
                                  ├── timestamp?: string
                                  ├── path?: string
                                  └── details?: FieldError[]
                                        ├── field: string
                                        ├── message: string
                                        └── rejectedValue?: unknown
```
