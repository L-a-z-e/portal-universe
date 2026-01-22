// portal-shell/src/api/utils.ts

import type { AxiosResponse } from 'axios';
import type { ApiResponse, ErrorDetails } from './types';

/**
 * AxiosResponse에서 data 추출
 * @example
 * const tags = getData(await apiClient.get<ApiResponse<Tag[]>>('/api/tags'));
 */
export function getData<T>(response: AxiosResponse<ApiResponse<T>>): T {
  return response.data.data;
}

/**
 * Axios 에러에서 Backend 에러 정보 추출
 * - apiClient 인터셉터에서 error.errorDetails에 저장됨
 */
export function getErrorDetails(error: unknown): ErrorDetails | null {
  if (error && typeof error === 'object' && 'errorDetails' in error) {
    return (error as { errorDetails: ErrorDetails }).errorDetails;
  }
  return null;
}

/**
 * 사용자 친화적 에러 메시지 반환
 * @example
 * try {
 *   await fetchData();
 * } catch (error) {
 *   alert(getErrorMessage(error));
 * }
 */
export function getErrorMessage(error: unknown): string {
  const details = getErrorDetails(error);
  if (details) {
    return details.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '알 수 없는 오류가 발생했습니다.';
}

/**
 * 에러 코드 추출
 * @example
 * if (getErrorCode(error) === 'B001') {
 *   // 게시물 없음 처리
 * }
 */
export function getErrorCode(error: unknown): string | null {
  const details = getErrorDetails(error);
  return details?.code ?? null;
}
