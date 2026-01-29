/**
 * @portal/design-types - API Types
 * Backend ApiResponse와 일치하는 공통 API 타입 정의
 */

/**
 * Validation 에러 필드 상세
 */
export interface FieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

/**
 * 에러 상세 정보
 */
export interface ErrorDetails {
  code: string;
  message: string;
  timestamp?: string;
  path?: string;
  details?: FieldError[];
}

/**
 * API 성공 응답 (axios resolve 시)
 * - Backend ApiResponse 구조와 일치
 * - axios가 4xx/5xx를 reject하므로, resolve된 응답은 항상 성공
 */
export interface ApiResponse<T> {
  success: true;
  data: T;
  error: null;
}

/**
 * API 에러 응답 (axios reject 시 error.response.data)
 * - Backend GlobalExceptionHandler가 반환하는 구조
 */
export interface ApiErrorResponse {
  success: false;
  data: null;
  error: ErrorDetails;
}
