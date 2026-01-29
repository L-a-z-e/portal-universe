import { useCallback } from 'react';
import { useToast } from './useToast';
import type { ErrorDetails, FieldError } from '@portal/design-types';

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
 * 에러에서 사용자 친화적 메시지 추출 (standalone utility)
 */
export function getApiErrorMessage(error: unknown, fallbackMessage?: string): string {
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
 * 에러 코드 추출 (standalone utility)
 */
export function getApiErrorCode(error: unknown): string | null {
  const details = extractErrorDetails(error);
  return details?.code ?? null;
}

/**
 * form validation 에러의 필드별 메시지 추출 (standalone utility)
 */
export function getApiFieldErrors(error: unknown): Record<string, string> {
  const details = extractErrorDetails(error);
  if (!details?.details?.length) return {};
  const result: Record<string, string> = {};
  for (const field of details.details) {
    result[field.field] = field.message;
  }
  return result;
}

export interface UseApiErrorReturn {
  handleError: (error: unknown, fallbackMessage?: string) => ApiErrorInfo;
  getErrorMessage: (error: unknown, fallbackMessage?: string) => string;
  getErrorCode: (error: unknown) => string | null;
  getFieldErrors: (error: unknown) => Record<string, string>;
}

/**
 * API 에러 처리 hook
 *
 * @example
 * ```tsx
 * const { handleError } = useApiError();
 *
 * const handleSubmit = async () => {
 *   try {
 *     await api.call();
 *   } catch (err) {
 *     handleError(err, '처리에 실패했습니다.');
 *   }
 * };
 * ```
 *
 * @note React hook이므로 컴포넌트/hook 내에서만 호출 가능.
 *       Store에서는 에러를 throw하고, 컴포넌트의 catch에서 handleError()를 호출하세요.
 */
export function useApiError(): UseApiErrorReturn {
  const { error: showError } = useToast();

  const handleError = useCallback(
    (error: unknown, fallbackMessage?: string): ApiErrorInfo => {
      const message = getApiErrorMessage(error, fallbackMessage);
      const code = getApiErrorCode(error);
      const fieldErrors = extractErrorDetails(error)?.details ?? [];

      const displayMessage = code ? `${message} (${code})` : message;
      showError(displayMessage);

      return { message, code, details: fieldErrors };
    },
    [showError]
  );

  const getErrorMessage = useCallback(
    (error: unknown, fallbackMessage?: string) => getApiErrorMessage(error, fallbackMessage),
    []
  );

  const getErrorCode = useCallback(
    (error: unknown) => getApiErrorCode(error),
    []
  );

  const getFieldErrors = useCallback(
    (error: unknown) => getApiFieldErrors(error),
    []
  );

  return {
    handleError,
    getErrorMessage,
    getErrorCode,
    getFieldErrors,
  };
}
