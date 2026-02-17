import { useToast } from './useToast';
import type { ErrorDetails, FieldError } from '@portal/design-core';

export interface ApiErrorInfo {
  message: string;
  code: string | null;
  details: FieldError[];
}

/**
 * Axios 에러에서 Backend 에러 상세 정보 추출
 */
function extractErrorDetails(error: unknown): ErrorDetails | null {
  if (error && typeof error === 'object' && 'errorDetails' in error) {
    return (error as { errorDetails: ErrorDetails }).errorDetails;
  }
  // axios error.response.data 직접 접근
  if (
    error &&
    typeof error === 'object' &&
    'response' in error &&
    (error as { response?: { data?: { success: boolean; error?: ErrorDetails } } }).response?.data?.error
  ) {
    return (error as { response: { data: { error: ErrorDetails } } }).response.data.error;
  }
  return null;
}

/**
 * API 에러 처리 composable
 *
 * @example
 * ```vue
 * <script setup>
 * import { useApiError } from '@portal/design-vue';
 *
 * const { handleError } = useApiError();
 *
 * async function submit() {
 *   try {
 *     await api.call();
 *   } catch (err) {
 *     handleError(err, '처리에 실패했습니다.');
 *   }
 * }
 * </script>
 * ```
 */
export function useApiError() {
  const toast = useToast();

  /**
   * 에러에서 사용자 친화적 메시지 추출
   */
  function getErrorMessage(error: unknown, fallbackMessage?: string): string {
    const details = extractErrorDetails(error);
    if (details) {
      return details.message;
    }
    if (error instanceof Error) {
      return error.message;
    }
    return fallbackMessage ?? '알 수 없는 오류가 발생했습니다.';
  }

  /**
   * 에러 코드 추출
   */
  function getErrorCode(error: unknown): string | null {
    const details = extractErrorDetails(error);
    return details?.code ?? null;
  }

  /**
   * form validation 에러의 필드별 메시지 추출
   */
  function getFieldErrors(error: unknown): Record<string, string> {
    const details = extractErrorDetails(error);
    if (!details?.details?.length) return {};
    const result: Record<string, string> = {};
    for (const field of details.details) {
      result[field.field] = field.message;
    }
    return result;
  }

  /**
   * 에러 처리 + toast 자동 표시
   * @returns 파싱된 에러 정보
   */
  function handleError(error: unknown, fallbackMessage?: string): ApiErrorInfo {
    const message = getErrorMessage(error, fallbackMessage);
    const code = getErrorCode(error);
    const fieldErrors = extractErrorDetails(error)?.details ?? [];

    const displayMessage = code ? `${message} (${code})` : message;
    toast.error(displayMessage);

    return { message, code, details: fieldErrors };
  }

  return {
    handleError,
    getErrorMessage,
    getErrorCode,
    getFieldErrors,
  };
}

export type UseApiError = ReturnType<typeof useApiError>;
