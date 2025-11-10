// blog-frontend/src/types/common.ts

/**
 * 공통 API 응답 래퍼
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  message?: string;
  timestamp?: string;
}

/**
 * Spring Page 응답 (완전한 정의)
 */
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}

/**
 * 에러 응답
 */
export interface ErrorResponse {
  success: false;
  code: string;
  message: string;
  timestamp: string;
}