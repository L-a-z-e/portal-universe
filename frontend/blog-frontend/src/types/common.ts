// blog-frontend/src/types/common.ts

/**
 * API 성공 응답
 * - axios가 4xx/5xx를 reject하므로, resolve된 응답은 항상 성공
 * - portal/api 모듈의 타입과 동일한 구조
 */
export interface ApiResponse<T> {
  success: true;
  data: T;
  error: null;
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
