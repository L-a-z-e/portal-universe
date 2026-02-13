export class ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ErrorInfo | null;

  private constructor(
    success: boolean,
    data: T | null,
    error: ErrorInfo | null,
  ) {
    this.success = success;
    this.data = data;
    this.error = error;
  }

  static success<T>(data: T): ApiResponse<T> {
    return new ApiResponse(true, data, null);
  }

  static error(
    code: string,
    message: string,
    options?: ErrorInfoOptions,
  ): ApiResponse<null> {
    const error: ErrorInfo = {
      code,
      message,
      timestamp: options?.timestamp ?? new Date().toISOString(),
      ...(options?.path && { path: options.path }),
      ...(options?.details?.length && { details: options.details }),
    };
    return new ApiResponse(false, null, error);
  }
}

export interface ErrorInfo {
  code: string;
  message: string;
  timestamp: string;
  path?: string;
  details?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

interface ErrorInfoOptions {
  timestamp?: string;
  path?: string;
  details?: FieldError[];
}
