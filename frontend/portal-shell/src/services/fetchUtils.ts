import type { ApiErrorResponse, ErrorDetails } from '@portal/design-types';

/**
 * fetch API용 에러 클래스.
 * errorDetails 프로퍼티로 useApiError의 extractErrorDetails()와 자동 호환.
 */
export class ApiError extends Error {
  readonly code: string | null;
  readonly errorDetails: ErrorDetails | null;

  constructor(message: string, code?: string, details?: ErrorDetails) {
    super(message);
    this.name = 'ApiError';
    this.code = code ?? null;
    this.errorDetails = details ?? null;
  }
}

/**
 * fetch Response가 ok가 아니면 body를 파싱하여 ApiError를 throw.
 * Backend ApiResponse의 error.message를 우선 사용하고,
 * 파싱 실패 시 fallbackMessage + status code 사용.
 */
export async function throwIfNotOk(response: Response, fallbackMessage: string): Promise<void> {
  if (response.ok) return;
  const body = await response.json().catch(() => ({}));
  const errorDetail = (body as Partial<ApiErrorResponse>).error;
  throw new ApiError(
    errorDetail?.message || `${fallbackMessage}: ${response.status}`,
    errorDetail?.code,
    errorDetail ?? undefined,
  );
}
