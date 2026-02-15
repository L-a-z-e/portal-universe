/**
 * Common Module Barrel Exports
 *
 * 순환 의존성 주의:
 * - module 간 barrel은 만들지 않음 (Task ↔ Execution forwardRef 존재)
 * - 이 파일은 common 모듈 내부의 주요 exports만 re-export
 */

// Filters & Exceptions
export { BusinessException, PrismErrorCode } from './filters/business.exception';
export { GlobalExceptionFilter } from './filters/http-exception.filter';

// DTOs
export { PaginationDto, toPaginatedResult } from './dto/pagination.dto';
export type { PaginatedResult } from './dto/pagination.dto';
export { ApiResponse } from './dto/api-response.dto';

// Decorators
export { CurrentUserId } from './decorators/current-user.decorator';
export { Public } from './decorators/public.decorator';

// Validators
export { NoXss } from './validators/no-xss.validator';
export { NoSqlInjection } from './validators/no-sql-injection.validator';

// Utils
export { findOneOrThrow } from './utils/repository.util';
export { EncryptionUtil } from './utils/encryption.util';

// Guards
export { JwtAuthGuard } from './guards/jwt-auth.guard';

// Interceptors
export { ApiResponseInterceptor } from './interceptors/api-response.interceptor';
export { AuditInterceptor } from './interceptors/audit.interceptor';
