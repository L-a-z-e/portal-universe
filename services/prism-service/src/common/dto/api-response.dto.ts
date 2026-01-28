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

  static error(code: string, message: string): ApiResponse<null> {
    return new ApiResponse(false, null, { code, message });
  }
}

export interface ErrorInfo {
  code: string;
  message: string;
}
